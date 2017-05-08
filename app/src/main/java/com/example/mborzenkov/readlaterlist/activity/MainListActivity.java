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
import com.example.mborzenkov.readlaterlist.adt.ReadLaterItemDbAdapter;
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
        SearchView.OnQueryTextListener {

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
        ReadLaterContract.ReadLaterEntry.COLUMN_COLOR,
        ReadLaterContract.ReadLaterEntry.COLUMN_DATE_CREATED,
        ReadLaterContract.ReadLaterEntry.COLUMN_DATE_LAST_MODIFIED,
        ReadLaterContract.ReadLaterEntry.COLUMN_DATE_LAST_VIEW
    };
    /** Индексы для колонок из MAIN_LIST_PROJECTION, для упрощения. */
    private static final int INDEX_COLUMN_ID = 0;
    static final int INDEX_COLUMN_LABEL = 1;
    static final int INDEX_COLUMN_DESCRIPTION = 2;
    static final int INDEX_COLUMN_COLOR = 3;
    static final int INDEX_COLUMN_DATE_CREATED = 4;
    static final int INDEX_COLUMN_DATE_LAST_MODIFIED = 5;
    static final int INDEX_COLUMN_DATE_LAST_VIEW = 6;
    /** ID Используемого LoadManager'а. */
    public static final int ITEM_LOADER_ID = 13;

    // Элементы layout
    private ItemListAdapter mItemListAdapter;
    private ListView mItemListView;
    private ProgressBar mLoadingIndicator;
    private LinearLayout mEmptyList;
    private MainListDrawerHelper mDrawerHelper;

    /** Cursor с данными. */
    private Cursor mDataCursor;
    /** ID текущего редактируемого элемента. */
    private int mEditItemId = UID_EMPTY;
    /** Запрос поиска. */
    private String mSearchQuery = "";

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
        mDrawerHelper = new MainListDrawerHelper(this);

        // Показать иконку загрузки
        showLoading();

        // Начать загрузку данных
        getSupportLoaderManager().initLoader(ITEM_LOADER_ID, null, this);
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
                mDrawerHelper.openDrawer();
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
        ReadLaterItemDbAdapter dbAdapter = new ReadLaterItemDbAdapter();
        ReadLaterItem data = dbAdapter.itemFromCursor(mDataCursor);
        editItemIntent.putExtra(ReadLaterItemParcelable.KEY_EXTRA, new ReadLaterItemParcelable(data));
        startActivityForResult(editItemIntent, ITEM_EDIT_REQUEST);
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
