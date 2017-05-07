package com.example.mborzenkov.readlaterlist.activity;

import android.app.DatePickerDialog;
import android.app.SearchManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.Loader;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Spinner;

import com.example.mborzenkov.readlaterlist.BuildConfig;
import com.example.mborzenkov.readlaterlist.R;
import com.example.mborzenkov.readlaterlist.adt.ReadLaterItem;
import com.example.mborzenkov.readlaterlist.adt.ReadLaterItemParcelable;
import com.example.mborzenkov.readlaterlist.data.MainListFilter;
import com.example.mborzenkov.readlaterlist.data.ReadLaterContract;
import com.example.mborzenkov.readlaterlist.utility.ActivityUtils;
import com.example.mborzenkov.readlaterlist.utility.DebugUtils;
import com.example.mborzenkov.readlaterlist.utility.FavoriteColorsUtils;
import com.example.mborzenkov.readlaterlist.utility.MainListBackupUtils;
import com.example.mborzenkov.readlaterlist.utility.MainListFilterUtils;
import com.example.mborzenkov.readlaterlist.utility.ReadLaterDbUtils;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/** Главная Activity, представляющая собой список. */
public class MainListActivity extends AppCompatActivity implements
        LoaderManager.LoaderCallbacks<Cursor>,
        ItemListAdapter.ItemListAdapterOnClickHandler,
        SearchView.OnQueryTextListener,
        View.OnClickListener {

    // Константы
    /** Формат даты для вывода на формах редактирования дат. */
    public static final String FORMAT_DATE = "dd/MM/yy";

    // Intent
    /** Константа, обозначающая пустой UID. */
    private static final int UID_EMPTY = -1;
    /** ID запроса для создания нового элемента. */
    private static final int ITEM_ADD_NEW_REQUEST = 1;
    /** ID запроса для редактирования элемента. */
    private static final int ITEM_EDIT_REQUEST = 2;

    // Database
    /** Используемые в MainListActivity колонки базы данных. */
    private static final String[] MAIN_LIST_PROJECTION = {
        ReadLaterContract.ReadLaterEntry._ID,
        ReadLaterContract.ReadLaterEntry.COLUMN_LABEL,
        ReadLaterContract.ReadLaterEntry.COLUMN_DESCRIPTION,
        ReadLaterContract.ReadLaterEntry.COLUMN_COLOR
    };
    /** Индексы для колонок из MAIN_LIST_PROJECTION, для упрощения. */
    private static final int INDEX_COLUMN_ID = 0;
    static final int INDEX_COLUMN_LABEL = 1;
    static final int INDEX_COLUMN_DESCRIPTION = 2;
    static final int INDEX_COLUMN_COLOR = 3;
    /** ID Используемого LoadManager'а. */
    public static final int ITEM_LOADER_ID = 13;

    // Элементы layout
    private ItemListAdapter mItemListAdapter;
    private ListView mItemListView;
    private ProgressBar mLoadingIndicator;
    private LinearLayout mEmptyList;

    // Элементы Drawer Layout
    private DrawerLayout mDrawerLayout;
    private LinearLayout mFavLinearLayout;
    private Spinner mSavedFiltersSpinner;
    private ArrayAdapter<String> mSavedFiltersAdapter;
    private Spinner mDateFiltersSpinner;
    private EditText mDateFromEditText;
    private EditText mDateToEditText;
    private Button mSortByLabelButton;
    private Button mSortByDateCreatedButton;
    private Button mSortByDateModifiedButton;
    private Button mSortByDateViewedButton;

    /** Cursor с данными. */
    private Cursor mDataCursor;
    /** ID текущего редактируемого элемента. */
    private int mEditItemId = UID_EMPTY;
    /** Запрос поиска. */
    private String mSearchQuery = "";
    /** Календарь для выбора. */
    private Calendar mCalendar = Calendar.getInstance();
    /** Редактируемое поле даты. */
    private EditText mDateEditor;
    /** Оригинальные названия кнопок сортировки. */
    private Map<MainListFilter.SortType, String> mSortButtonsNames = new HashMap<>();
    /** Добавляемый символ сортировки. */
    private Map<MainListFilter.SortOrder, String> mSortOrderSymbols = new HashMap<>();
    /** Избранные цвета. */
    private int[] favColors;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mainlist);

        // Инициализация Toolbar
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_main_list);
        toolbar.setTitleTextColor(ContextCompat.getColor(this, R.color.icons));
        setSupportActionBar(toolbar);
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab_item_add);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Создание нового элемента
                Intent newItemIntent = new Intent(MainListActivity.this, EditItemActivity.class);
                startActivityForResult(newItemIntent, ITEM_ADD_NEW_REQUEST);
            }
        });

        // Инициализация объектов layout
        mItemListAdapter = new ItemListAdapter(this, this);
        mItemListView = (ListView) findViewById(R.id.listview_main_list);
        mItemListView.setAdapter(mItemListAdapter);
        mLoadingIndicator = (ProgressBar) findViewById(R.id.pb_main_loading);
        mEmptyList = (LinearLayout) findViewById(R.id.linearLayout_emptylist);

        // Инициализация Drawer Layout
        inflateDrawerLayout();

        // Показать иконку загрузки
        showLoading();

        // Начать загрузку данных
        getSupportLoaderManager().initLoader(ITEM_LOADER_ID, null, this);
    }

    /** Вызывается один раз в onCreate для создания всего связанного с DrawerLayout. */
    private void inflateDrawerLayout() {

        // Объекты layout
        mFavLinearLayout = (LinearLayout) findViewById(R.id.linearlayout_drawermainlist_favorites);
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawerlayout_mainlist);
        ActionBarDrawerToggle drawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout,
                R.string.mainlist_drawer_title, R.string.mainlist_drawer_title) {

            @Override
            public void onDrawerClosed(View view) {
                // При закрытии - устанавливаем фильтр
                super.onDrawerClosed(view);
                getSupportLoaderManager().restartLoader(ITEM_LOADER_ID, null, MainListActivity.this);
            }

            @Override
            public void onDrawerOpened(View drawerView) {
                //  При открытии - обновляем Drawer на основании фильтра
                super.onDrawerOpened(drawerView);
                updateDrawerWithCurrentFilter();
            }
        };
        mDrawerLayout.addDrawerListener(drawerToggle);

        // Заполняем варианты запомненных фильтров
        mSavedFiltersSpinner = (Spinner) findViewById(R.id.spinner_drawermainlist_filter);
        mSavedFiltersAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item);
        mSavedFiltersAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mSavedFiltersAdapter.addAll(MainListFilterUtils.getSavedFiltersList(this));
        mSavedFiltersSpinner.setAdapter(mSavedFiltersAdapter);
        mSavedFiltersSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long l) {
                int indexSavedAdd = MainListFilterUtils.getIndexSavedAdd();
                int indexSavedDelete = MainListFilterUtils.getIndexSavedDelete();
                if (position == indexSavedAdd) {
                    // Вариант 1: Клик на кнопку "+ Добавить"
                    // Показываем окно ввода текста, сохраняем при успешном вводе
                    final EditText editText = new EditText(MainListActivity.this);
                    ActivityUtils.showInputTextDialog(MainListActivity.this,
                        getString(R.string.mainlist_drawer_filters_save_question_title),
                        null,
                        editText,
                        (String input) -> saveFilter(input),
                        () -> resetSavedFilterSelection());

                } else if (position == indexSavedDelete) {
                    // Вариант 2: Клик на кнопку "- Удалить"
                    // Показываем окно подтверждения, удаляем при положительном ответе
                    final int currentIndex = MainListFilterUtils.getIndexSavedCurrent();
                    if (currentIndex == MainListFilterUtils.INDEX_SAVED_DEFAULT) {
                        mSavedFiltersSpinner.setSelection(currentIndex);
                        return;
                    }
                    ActivityUtils.showAlertDialog(MainListActivity.this,
                        getString(R.string.mainlist_drawer_filters_remove_question_title),
                        getString(R.string.mainlist_drawer_filters_remove_question_text),
                        () -> removeSavedFilter(),
                        () -> resetSavedFilterSelection());
                } else {
                    // Остальные варианты - выбираем
                    MainListFilterUtils.clickOnSavedFilter(MainListActivity.this, position);
                    updateDrawerWithCurrentFilter();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) { }
        });

        // Заполняем варианты фильтров по дате
        mDateFiltersSpinner = (Spinner) findViewById(R.id.spinner_drawermainlist_datefilter);
        ArrayAdapter<String> dateFiltersAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item);
        dateFiltersAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        dateFiltersAdapter.addAll(MainListFilterUtils.getsDateFiltersList(this));
        mDateFiltersSpinner.setAdapter(dateFiltersAdapter);
        mDateFiltersSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long l) {
                MainListFilter filter = MainListFilterUtils.getCurrentFilter();
                filter.setSelection(MainListFilterUtils.getDateFilterSelection(position));
                switch (position) {
                    case MainListFilterUtils.INDEX_DATE_ALL:
                        mDateFromEditText.setVisibility(View.GONE);
                        mDateToEditText.setVisibility(View.GONE);
                        break;
                    default:
                        mDateFromEditText.setVisibility(View.VISIBLE);
                        mDateToEditText.setVisibility(View.VISIBLE);
                        break;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) { }
        });

        // DatePicker на полях с датами
        final DatePickerDialog.OnDateSetListener date = new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int monthOfYear,
                                  int dayOfMonth) {
                mCalendar.set(Calendar.YEAR, year);
                mCalendar.set(Calendar.MONTH, monthOfYear);
                mCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                SimpleDateFormat sdf = new SimpleDateFormat(FORMAT_DATE, Locale.US);
                mDateEditor.setText(sdf.format(mCalendar.getTime()));
                MainListFilter filter = MainListFilterUtils.getCurrentFilter();
                if (mDateEditor.getId() == R.id.edittext_drawermainlist_datefrom) {
                    mCalendar.set(Calendar.HOUR_OF_DAY, 0);
                    mCalendar.set(Calendar.MINUTE, 0);
                    mCalendar.set(Calendar.SECOND, 0);
                    filter.setDateFrom(mCalendar.getTimeInMillis());
                } else {
                    mCalendar.set(Calendar.HOUR_OF_DAY, 23);
                    mCalendar.set(Calendar.MINUTE, 59);
                    mCalendar.set(Calendar.SECOND, 59);
                    filter.setDateTo(mCalendar.getTimeInMillis());
                }
            }

        };
        View.OnClickListener onDateClick = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Сохраняем view, который сейчас редактируем
                mDateEditor = (EditText) v;

                // Открываем Dialog, установив заранее выбранную дату и границы
                long timeSelected = (long) v.getTag();
                if (timeSelected > 0) {
                    mCalendar.setTimeInMillis(timeSelected);
                }
                DatePickerDialog dialog = new DatePickerDialog(MainListActivity.this, date, mCalendar
                        .get(Calendar.YEAR), mCalendar.get(Calendar.MONTH),
                        mCalendar.get(Calendar.DAY_OF_MONTH));
                DatePicker picker = dialog.getDatePicker();
                if (mDateEditor.getId() == R.id.edittext_drawermainlist_dateto) {
                    picker.setMinDate((long) mDateFromEditText.getTag());
                    picker.setMaxDate(System.currentTimeMillis());
                } else {
                    long timeAfter = (long) mDateToEditText.getTag();
                    if (timeAfter > 0) {
                        picker.setMaxDate(timeAfter);
                    } else {
                        picker.setMaxDate(System.currentTimeMillis());
                    }
                }
                dialog.show();
            }
        };
        long zeroLong = 0;
        mDateFromEditText = (EditText) findViewById(R.id.edittext_drawermainlist_datefrom);
        mDateFromEditText.setOnClickListener(onDateClick);
        mDateFromEditText.setTag(zeroLong);
        mDateToEditText = (EditText) findViewById(R.id.edittext_drawermainlist_dateto);
        mDateToEditText.setOnClickListener(onDateClick);
        mDateToEditText.setTag(zeroLong);

        // Добавляем Favorites на Drawer Layout
        FavoriteColorsUtils.inflateFavLayout(this, mFavLinearLayout);

        // Запоминаем кнопки sortBy
        mSortByLabelButton = (Button) findViewById(R.id.button_drawermainlist_sortname);
        mSortByLabelButton.setTag(MainListFilter.SortType.LABEL);
        mSortButtonsNames.put(MainListFilter.SortType.LABEL, mSortByLabelButton.getText().toString());

        mSortByDateCreatedButton = (Button) findViewById(R.id.button_drawermainlist_sortcreate);
        mSortByDateCreatedButton.setTag(MainListFilter.SortType.DATE_CREATED);
        mSortButtonsNames.put(MainListFilter.SortType.DATE_CREATED, mSortByDateCreatedButton.getText().toString());

        mSortByDateModifiedButton = (Button) findViewById(R.id.button_drawermainlist_sortmodified);
        mSortByDateModifiedButton.setTag(MainListFilter.SortType.DATE_MODIFIED);
        mSortButtonsNames.put(MainListFilter.SortType.DATE_MODIFIED, mSortByDateModifiedButton.getText().toString());

        mSortByDateViewedButton = (Button) findViewById(R.id.button_drawermainlist_sortview);
        mSortByDateViewedButton.setTag(MainListFilter.SortType.DATE_VIEWED);
        mSortButtonsNames.put(MainListFilter.SortType.DATE_VIEWED, mSortByDateViewedButton.getText().toString());

        mSortOrderSymbols.put(MainListFilter.SortOrder.ASC, getString(R.string.mainlist_drawer_sort_symb_asc));
        mSortOrderSymbols.put(MainListFilter.SortOrder.DESC, getString(R.string.mainlist_drawer_sort_symb_desc));

        // Специальные возможности создаются только в DEBUG
        if (!BuildConfig.DEBUG) {
            mDrawerLayout.findViewById(R.id.textview_drawermainlist_debug).setVisibility(View.INVISIBLE);
            mDrawerLayout.findViewById(R.id.button_drawermainlist_fillplaceholders).setVisibility(View.INVISIBLE);
            mDrawerLayout.findViewById(R.id.button_drawermainlist_deleteall).setVisibility(View.INVISIBLE);
        }

    }

    private void reloadSavedFiltersList() {
        mSavedFiltersAdapter.clear();
        mSavedFiltersAdapter.addAll(MainListFilterUtils
                .getSavedFiltersList(MainListActivity.this));
        mSavedFiltersAdapter.notifyDataSetChanged();
    }

    private void removeSavedFilter() {
        MainListFilterUtils.removeCurrentFilter(MainListActivity.this);
        reloadSavedFiltersList();
        updateDrawerWithCurrentFilter();
    }

    private void resetSavedFilterSelection() {
        mSavedFiltersSpinner.setSelection(MainListFilterUtils.getIndexSavedCurrent());
    }

    private void saveFilter(String input) {
        // pos
        if (!input.isEmpty()
                && !input.equals(getString(R.string.mainlist_drawer_filters_default))) {
            MainListFilterUtils.saveFilter(MainListActivity.this, input);
            reloadSavedFiltersList();
            mSavedFiltersSpinner.setSelection(MainListFilterUtils.getIndexSavedCurrent());
        }
        resetSavedFilterSelection();
    }

    /** Обновляет Drawer в соответствии с выбранным фильтром. */
    private void updateDrawerWithCurrentFilter() {
        MainListFilter currentFilter = MainListFilterUtils.getCurrentFilter();
        mDateFiltersSpinner.setSelection(currentFilter.getSelection().getPosition());
        mDateFromEditText.setText(currentFilter.getDateFrom());
        mDateToEditText.setText(currentFilter.getDateTo());
        favColors = FavoriteColorsUtils.updateFavLayoutFromSharedPreferences(this, mFavLinearLayout, null,
                MainListActivity.this, currentFilter.getColorFilter());
        resetButtons();
        Button selectedSortButton = null;
        switch (currentFilter.getSortType()) {
            case LABEL:
                selectedSortButton = mSortByLabelButton;
                break;
            case DATE_CREATED:
                selectedSortButton = mSortByDateCreatedButton;
                break;
            case DATE_MODIFIED:
                selectedSortButton = mSortByDateModifiedButton;
                break;
            case DATE_VIEWED:
                selectedSortButton = mSortByDateViewedButton;
                break;
            default:
                break;
        }
        if (selectedSortButton != null) {
            selectedSortButton.setActivated(true);
            selectedSortButton.setText(selectedSortButton.getText().toString()
                    + " " + mSortOrderSymbols.get(currentFilter.getSortOrder()));
        }
    }

    /** Сбрасывает все кнопки SortBy. */
    private void resetButtons() {
        mSortByLabelButton.setActivated(false);
        mSortByLabelButton.setText(mSortButtonsNames.get(MainListFilter.SortType.LABEL));
        mSortByDateCreatedButton.setActivated(false);
        mSortByDateCreatedButton.setText(mSortButtonsNames.get(MainListFilter.SortType.DATE_CREATED));
        mSortByDateModifiedButton.setActivated(false);
        mSortByDateModifiedButton.setText(mSortButtonsNames.get(MainListFilter.SortType.DATE_MODIFIED));
        mSortByDateViewedButton.setActivated(false);
        mSortByDateViewedButton.setText(mSortButtonsNames.get(MainListFilter.SortType.DATE_VIEWED));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        getMenuInflater().inflate(R.menu.menu_mainlist, menu);

        // Создание меню поиска
        SearchManager searchManager = (SearchManager) getSystemService(SEARCH_SERVICE);
        SearchView searchView = (SearchView) menu.findItem(R.id.mainlist_action_search).getActionView();
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        searchView.setSubmitButtonEnabled(true);
        searchView.setOnQueryTextListener(this);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.mainlist_settings:
                mDrawerLayout.openDrawer(Gravity.RIGHT);
                return true;
            default:
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        mSearchQuery = query;
        getSupportLoaderManager().restartLoader(ITEM_LOADER_ID, null, this);
        return false;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        mSearchQuery = newText;
        getSupportLoaderManager().restartLoader(ITEM_LOADER_ID, null, this);
        return false;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int loaderId, final Bundle args) {
        switch (loaderId) {
            case ITEM_LOADER_ID:
                // Создаем новый CursorLoader, нужно все имеющееся в базе данных
                return ReadLaterDbUtils.getNewCursorLoader(this, MAIN_LIST_PROJECTION, mSearchQuery,
                        MainListFilterUtils.getCurrentFilter());
            default:
                throw new IllegalArgumentException("Loader Not Implemented: " + loaderId);
        }
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        // По завершению загрузки, подменяем Cursor в адаптере и показываем данные
        mDataCursor = data;
        mItemListAdapter.changeCursor(mDataCursor);
        // mItemListView.smoothScrollToPosition(mPosition);
        showDataView();
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        // При сбросе загрузчика данных, сбрасываем данные
        mDataCursor = null;
        mItemListAdapter.changeCursor(null);
    }

    @Override
    public void onClick(int position) {
        // При нажатии на элемент, открываем EditItemActivity Activity для его редактирования
        mDataCursor.moveToPosition(position);
        mEditItemId = mDataCursor.getInt(INDEX_COLUMN_ID);
        Intent editItemIntent = new Intent(MainListActivity.this, EditItemActivity.class);
        ReadLaterItem data = new ReadLaterItem(mDataCursor.getString(INDEX_COLUMN_LABEL),
                mDataCursor.getString(INDEX_COLUMN_DESCRIPTION), mDataCursor.getInt(INDEX_COLUMN_COLOR));
        editItemIntent.putExtra(ReadLaterItemParcelable.KEY_EXTRA, new ReadLaterItemParcelable(data));
        startActivityForResult(editItemIntent, ITEM_EDIT_REQUEST);
    }

    @Override
    public void onClick(View v) {
        // Click на drawer favorite
        MainListFilter filter = MainListFilterUtils.getCurrentFilter();
        v.setActivated(!v.isActivated());
        int color = favColors[(int) v.getTag()];
        if (v.isActivated()) {
            filter.addColorFilter(color);
        } else {
            filter.removeColorFilter(color);
        }
    }

    /** Обработчик нажатия Sort button. */
    public void clickOnSortButton(View view) {
        // Click на кнопку сортировки
        Button button = (Button) view;
        if (button.getTag() != null) {
            MainListFilter filter = MainListFilterUtils.getCurrentFilter();
            MainListFilter.SortType buttonType = (MainListFilter.SortType) button.getTag();
            if (button.isActivated()) {
                filter.nextSortOrder();
            } else {
                filter.setSortType(buttonType);
            }
            updateDrawerWithCurrentFilter();
        }
    }

    /** Обработчик нажатия на Backup button. */
    public void clickOnBackupButton(View button) {
        // Click на кнопку бэкап
        switch (button.getId()) {
            case R.id.button_drawermainlist_backupsave:
                ActivityUtils.showAlertDialog(MainListActivity.this,
                    getString(R.string.mainlist_drawer_backup_save_question_title),
                    getString(R.string.mainlist_drawer_backup_save_question_text),
                    () -> new BackupAsyncTask().execute(true),
                    null);
                break;
            case R.id.button_drawermainlist_backuprestore:
                ActivityUtils.showAlertDialog(MainListActivity.this,
                    getString(R.string.mainlist_drawer_backup_restore_question_title),
                    getString(R.string.mainlist_drawer_backup_restore_question_text),
                    () -> new BackupAsyncTask().execute(false),
                    null);
                break;
            default:
                break;
        }
        mDrawerLayout.closeDrawer(Gravity.RIGHT);
    }

    /** AsyncTask для загрузки данных из бэкапа в многопоточном режиме. */
    private class BackupAsyncTask extends AsyncTask<Boolean, Void, Boolean> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            showLoading();
        }

        @Override
        protected Boolean doInBackground(Boolean... saving) {
            boolean saveMode = saving[0];
            if (saveMode) {
                MainListBackupUtils.saveEverythingAsJsonFile(MainListActivity.this);
            } else {
                MainListBackupUtils.restoreEverythingFromJsonFile(MainListActivity.this);
            }
            return saveMode;
        }

        @Override
        protected void onPostExecute(Boolean saveMode) {
            super.onPostExecute(saveMode);
            showDataView();
            if (!saveMode) {
                getSupportLoaderManager().restartLoader(ITEM_LOADER_ID, null, MainListActivity.this);
            }
        }
    }

    /** Обрабатывает нажатия на кнопки специальных возможностей. */
    public void clickOnDebugButton(View button) {
        if (!BuildConfig.DEBUG) {
            return;
        }
        switch (button.getId()) {
            case R.id.button_drawermainlist_fillplaceholders:
                // Действие "Заполнить данными" открывает окно подтверждения и по положительному ответу
                // вызывает функцию для заполнения
                DebugUtils.showAlertAndAddPlaceholders(this, this);
                break;
            case R.id.button_drawermainlist_deleteall:
                // Действие "Удалить все" открывает окно подтверждения и по положительному ответу
                // вызывает функцию для очистки
                DebugUtils.showAlertAndDeleteItems(this, this);
                break;
            default:
                break;
        }
        mDrawerLayout.closeDrawer(Gravity.RIGHT);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        // Каждый раз при окончании редактирования, сбрасываем переменную mEditItemId
        int uid = mEditItemId;
        mEditItemId = UID_EMPTY;

        // Обрабатывает возврат от EditItemActivity
        if (resultCode == RESULT_OK && data != null && data.hasExtra(ReadLaterItemParcelable.KEY_EXTRA)) {
            // Возвращенные данные в формате ReadLaterItem
            ReadLaterItemParcelable parcelableData =
                    (ReadLaterItemParcelable) data.getParcelableExtra(ReadLaterItemParcelable.KEY_EXTRA);
            ReadLaterItem resultData = parcelableData == null ? null : parcelableData.getItem();
            switch (requestCode) {
                case ITEM_ADD_NEW_REQUEST:
                    if (resultData != null && ReadLaterDbUtils.insertItem(MainListActivity.this, resultData)) {
                        // Добавляет новый элемент в базу, показывает снэкбар
                        Snackbar.make(mItemListView, getString(R.string.snackbar_item_added),
                                Snackbar.LENGTH_LONG).show();
                        getSupportLoaderManager().restartLoader(ITEM_LOADER_ID, null, this);
                        return;
                    }
                    break;
                case ITEM_EDIT_REQUEST:
                    if (uid != UID_EMPTY) {
                        if (resultData == null) {
                            // Удаляет элемент, показывает снэкбар
                            if (ReadLaterDbUtils.deleteItem(this, uid)) {
                                Snackbar.make(mItemListView,
                                        getString(R.string.snackbar_item_removed), Snackbar.LENGTH_LONG).show();
                                showDataView();
                            }
                        } else {
                            // Изменяет элемент
                            if (ReadLaterDbUtils.updateItem(MainListActivity.this, resultData, uid)) {
                                Snackbar.make(mItemListView,
                                        getString(R.string.snackbar_item_edited), Snackbar.LENGTH_LONG).show();
                            }
                        }
                        getSupportLoaderManager().restartLoader(ITEM_LOADER_ID, null, this);
                        return;
                    }
                    break;
                default:
                    break;
            }
        }

        // Этот блок вызывается при простом просмотре, тк при успешном случае с ADD_NEW или EDIT, уже был вызван return
        if (uid != UID_EMPTY) {
            ReadLaterDbUtils.updateItemViewDate(MainListActivity.this, uid);
        }

    }

    /** Показывает онбординг, если список пуст или список, если он не пуст. */
    private void showDataView() {
        mLoadingIndicator.setVisibility(View.INVISIBLE);
        if (mDataCursor.getCount() > 0) {
            mEmptyList.setVisibility(View.INVISIBLE);
            mItemListView.setVisibility(View.VISIBLE);
        } else {
            mItemListView.setVisibility(View.INVISIBLE);
            mEmptyList.setVisibility(View.VISIBLE);
        }
    }

    /** Показывает индикатор загрузки, скрывая все лишнее. */
    private void showLoading() {
        mItemListView.setVisibility(View.INVISIBLE);
        mEmptyList.setVisibility(View.INVISIBLE);
        mLoadingIndicator.setVisibility(View.VISIBLE);
    }

}
