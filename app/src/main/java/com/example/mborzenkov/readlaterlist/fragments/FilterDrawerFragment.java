package com.example.mborzenkov.readlaterlist.fragments;

import android.app.DatePickerDialog;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.text.InputFilter;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import com.example.mborzenkov.readlaterlist.BuildConfig;
import com.example.mborzenkov.readlaterlist.R;
import com.example.mborzenkov.readlaterlist.adt.MainListFilter;
import com.example.mborzenkov.readlaterlist.adt.UserInfo;
import com.example.mborzenkov.readlaterlist.utility.ActivityUtils;
import com.example.mborzenkov.readlaterlist.utility.FavoriteColorsUtils;
import com.example.mborzenkov.readlaterlist.utility.MainListFilterUtils;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/** Фрагмент, представляющий собой Drawer с фильтрами.
 * Оповещает о выборе нового пользователя и о нажатиях на кнопки.
 */
public class FilterDrawerFragment extends Fragment implements View.OnClickListener {

    /////////////////////////
    // Константы

    /** TAG фрагмента для фрагмент менеджера. */
    public static final String TAG = "fragment_drawer_filter";
    /** Формат даты для вывода на формах Drawer. */
    private static final String FORMAT_DATE = "dd/MM/yy";


    /////////////////////////
    // Static

    /** Возвращает уже созданный ранее объект FilterDrawerFragment или создает новый, если такого нет.
     * Для создания объектов следует всегда использовать этот метод.
     * Не помещает объект в FragmentManager.
     * При помещении объекта в FragmentManager, следует использовать тэг TAG.
     *
     * @param fragmentManager менеджер для поиска фрагментов по тэгу
     * @return новый объект FilterDrawerFragment
     */
    public static FilterDrawerFragment getInstance(FragmentManager fragmentManager) {

        FilterDrawerFragment fragment = (FilterDrawerFragment) fragmentManager.findFragmentByTag(TAG);

        if (fragment == null) {
            fragment = new FilterDrawerFragment();
        }

        return fragment;

    }

    /** Интерфейс для оповещений о событиях в Drawer. */
    public interface DrawerCallbacks {

        /** Вызывается, когда выбран новый пользователь.
         * Если выбран тот же самый пользователь, не вызывается.
         */
        void onUserChanged();

        /** Вызывается при нажатии на одну из кнопок действий.
         * Нажатие на кнопки только вызывает этот колбек, не показывает окон и не закрывает Drawer.
         *
          * @param action действие
         */
        void onActionToggled(DrawerActions action);

        /** Оповещает об изменениях фильтра. */
        void onFilterChanged();

    }

    /** Перечисление действий в Drawer. */
    public enum DrawerActions {

        BACKUP_SAVE("Save backup"),
        BACKUP_RESTORE("Restore from backup"),
        FILL_PLACEHOLDERS("Fill with placeholders"),
        DELETE_ALL("Delete all");

        private final String title;

        DrawerActions(String title) {
            this.title = title;
        }

        @Override
        public String toString() {
            return title;
        }

    }


    /////////////////////////
    // Поля объекта

    /** Объект для колбеков о событиях во фрагменте. */
    private @Nullable DrawerCallbacks mCallbacks = null;

    // Объекты Layout
    private LinearLayout mFavLinearLayout;
    private Spinner mSavedFiltersSpinner;
    private Spinner mDateFiltersSpinner;
    private EditText mDateFromEditText;
    private EditText mDateToEditText;
    private Button mSortByLabelButton;
    private Button mSortByDateCreatedButton;
    private Button mSortByDateModifiedButton;
    private Button mSortByDateViewedButton;
    private TextView mCurrentUser;

    // Хэлперы
    /** Адаптер для SavedFilters. */
    private @Nullable ArrayAdapter<String> mSavedFiltersAdapter = null;
    /** Календарь для выбора. */
    private final Calendar mCalendar = Calendar.getInstance();
    /** Редактируемое поле даты. */
    private @Nullable EditText mDateEditor = null;
    /** Оригинальные названия кнопок сортировки. */
    private final Map<MainListFilter.SortType, String> mSortButtonsNames = new HashMap<>();
    /** Добавляемый символ сортировки. */
    private final Map<MainListFilter.SortOrder, String> mSortOrderSymbols = new HashMap<>();
    /** Избранные цвета. */
    private @Nullable int[] favColors = null;
    /** Признак выполнения загрузки. */
    private boolean loaded = false;

    /////////////////////////
    // Колбеки Fragment

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof DrawerCallbacks) {
            mCallbacks = (DrawerCallbacks) context;
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_drawer_filter, container, false);
        loaded = false;

        // Объекты layout
        mFavLinearLayout = (LinearLayout) rootView.findViewById(R.id.linearlayout_filterdrawer_favorites);
        mSavedFiltersSpinner = (Spinner) rootView.findViewById(R.id.spinner_filterdrawer_filter);
        mDateFiltersSpinner = (Spinner) rootView.findViewById(R.id.spinner_filterdrawer_datefilter);
        mDateFromEditText = (EditText) rootView.findViewById(R.id.edittext_filterdrawer_datefrom);
        mDateToEditText = (EditText) rootView.findViewById(R.id.edittext_filterdrawer_dateto);
        mSortByLabelButton = (Button) rootView.findViewById(R.id.button_filterdrawer_sortname);
        mSortByDateCreatedButton = (Button) rootView.findViewById(R.id.button_filterdrawer_sortcreate);
        mSortByDateModifiedButton = (Button) rootView.findViewById(R.id.button_filterdrawer_sortmodified);
        mSortByDateViewedButton = (Button) rootView.findViewById(R.id.button_filterdrawer_sortview);
        mCurrentUser = (TextView) rootView.findViewById(R.id.tv_filterdrawer_user_value);

        // Инициализируем поле смены пользователя
        TextView urlChangeUser = (TextView) rootView.findViewById(R.id.tv_filterdrawer_user_change);
        urlChangeUser.setOnClickListener((View v) -> {
            // Нажатие на "сменить пользователя"
            EditText inputNumber = new EditText(getActivity());
            inputNumber.setInputType(InputType.TYPE_CLASS_NUMBER);
            inputNumber.setFilters(new InputFilter[] {new InputFilter.LengthFilter(8)}); // Не более 8 цифр
            inputNumber.setText(mCurrentUser.getText().toString());
            ActivityUtils.showInputTextDialog(
                    getContext(),
                    inputNumber,
                    getString(R.string.mainlist_user_change_question_title),
                    getString(R.string.mainlist_user_change_question_text),
                (input) -> {
                    try {
                        // Смотрим введенное значение
                        int number = Integer.parseInt(input);
                        if (number != UserInfo.getCurentUser(getContext()).getUserId()) {
                            UserInfo.changeCurrentUser(getContext(), number);
                            mCurrentUser.setText(String.valueOf(UserInfo.getCurentUser(getContext()).getUserId()));
                            if (mCallbacks != null) {
                                mCallbacks.onUserChanged();
                            }
                        }
                    } catch (ClassCastException e) {
                        Log.e("CAST ERROR", "Ошибка преобразования ввода пользователя в число");
                    }
                },
                    null);
        });

        // Устанавливаем текущего пользователя
        mCurrentUser.setText(String.valueOf(UserInfo.getCurentUser(getContext()).getUserId()));

        // Заполняем варианты запомненных фильтров
        reloadSavedFiltersList();

        // Заполняем варианты фильтров по дате
        reloadDateFiltersList();

        // DatePicker на полях с датами
        View.OnClickListener onDateClick = this::openDatePickerDialog;
        long zeroLong = 0; // В таги лучше сразу записать long, чтобы потом не конвертировать
        mDateFromEditText.setOnClickListener(onDateClick);
        mDateFromEditText.setTag(zeroLong);
        mDateToEditText.setOnClickListener(onDateClick);
        mDateToEditText.setTag(zeroLong);

        // Добавляем Favorites на Drawer Layout
        FavoriteColorsUtils.inflateFavLayout(getContext(), inflater, mFavLinearLayout);

        // Инициализируем кнопки SortBy
        mSortByLabelButton.setTag(MainListFilter.SortType.LABEL);
        mSortByLabelButton.setOnClickListener(this);
        mSortButtonsNames.put(MainListFilter.SortType.LABEL, mSortByLabelButton.getText().toString());

        mSortByDateCreatedButton.setTag(MainListFilter.SortType.DATE_CREATED);
        mSortByDateCreatedButton.setOnClickListener(this);
        mSortButtonsNames.put(MainListFilter.SortType.DATE_CREATED, mSortByDateCreatedButton.getText().toString());

        mSortByDateModifiedButton.setTag(MainListFilter.SortType.DATE_MODIFIED);
        mSortByDateModifiedButton.setOnClickListener(this);
        mSortButtonsNames.put(MainListFilter.SortType.DATE_MODIFIED, mSortByDateModifiedButton.getText().toString());

        mSortByDateViewedButton.setTag(MainListFilter.SortType.DATE_VIEWED);
        mSortByDateViewedButton.setOnClickListener(this);
        mSortButtonsNames.put(MainListFilter.SortType.DATE_VIEWED, mSortByDateViewedButton.getText().toString());

        mSortOrderSymbols.put(MainListFilter.SortOrder.ASC, getString(R.string.mainlist_drawer_sort_symb_asc));
        mSortOrderSymbols.put(MainListFilter.SortOrder.DESC, getString(R.string.mainlist_drawer_sort_symb_desc));

        // Ставим клик листенер и таги на кнопки бэкап
        Button backupSaveButton = (Button) rootView.findViewById(R.id.button_filterdrawer_backupsave);
        backupSaveButton.setOnClickListener(this);
        backupSaveButton.setTag(DrawerActions.BACKUP_SAVE);
        Button backupRestoreButton = (Button) rootView.findViewById(R.id.button_filterdrawer_backuprestore);
        backupRestoreButton.setOnClickListener(this);
        backupRestoreButton.setTag(DrawerActions.BACKUP_RESTORE);

        // Специальные возможности создаются только в DEBUG, ставим клик листенеры и таги
        Button fillPlaceholdersButton = (Button) rootView.findViewById(R.id.button_filterdrawer_fillplaceholders);
        fillPlaceholdersButton.setOnClickListener(this);
        fillPlaceholdersButton.setTag(DrawerActions.FILL_PLACEHOLDERS);
        Button deleteAllButton = (Button) rootView.findViewById(R.id.button_filterdrawer_deleteall);
        deleteAllButton.setOnClickListener(this);
        deleteAllButton.setTag(DrawerActions.DELETE_ALL);
        if (!BuildConfig.DEBUG) {
            rootView.findViewById(R.id.textview_filterdrawer_debug).setVisibility(View.INVISIBLE);
            fillPlaceholdersButton.setVisibility(View.INVISIBLE);
            deleteAllButton.setVisibility(View.INVISIBLE);
        }

        reloadDataFromCurrentFilter();

        return rootView;

    }

    @Override
    public void onDetach() {
        super.onDetach();
        mCallbacks = null;
    }


    /////////////////////////
    // Методы для перезагрузки данных в layout

    /** Обновляет Drawer в соответствии с выбранным фильтром. */
    public void reloadDataFromCurrentFilter() {

        MainListFilter currentFilter = MainListFilterUtils.getCurrentFilter();

        Log.d("CURRENT_FILTER", currentFilter.toString());

        mDateFiltersSpinner.setSelection(currentFilter.getSelection().getPosition(), false);
        mDateFromEditText.setText(currentFilter.getDateFrom());
        mDateToEditText.setText(currentFilter.getDateTo());
        switch (mDateFiltersSpinner.getSelectedItemPosition()) {
            case MainListFilterUtils.INDEX_DATE_ALL:
                mDateFromEditText.setVisibility(View.GONE);
                mDateToEditText.setVisibility(View.GONE);
                break;
            default:
                mDateFromEditText.setVisibility(View.VISIBLE);
                mDateToEditText.setVisibility(View.VISIBLE);
                break;
        }
        favColors = FavoriteColorsUtils.updateFavLayoutFromSharedPreferences(getContext(), mFavLinearLayout, null,
                this, currentFilter.getColorFilter());

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

    /** Перезагружает адаптер списка сохраненных фильтров.
     *  Если адаптер еще не был создан, создает новый.
     */
    private void reloadSavedFiltersList() {

        if (mSavedFiltersAdapter == null) {
            mSavedFiltersAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item);
        }
        mSavedFiltersAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mSavedFiltersSpinner.setAdapter(mSavedFiltersAdapter);

        // Устанавливаем онклик слушатель
        mSavedFiltersSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long l) {
                if (!loaded) {
                    loaded = true;
                    return;
                }
                int indexSavedAdd = MainListFilterUtils.getIndexSavedAdd();
                int indexSavedDelete = MainListFilterUtils.getIndexSavedDelete();
                if (position == indexSavedAdd) {
                    // Вариант 1: Клик на кнопку "+ Добавить"
                    // Показываем окно ввода текста, сохраняем при успешном вводе
                    final EditText editText = new EditText(getContext());
                    ActivityUtils.showInputTextDialog(
                            getContext(),
                            editText,
                            getString(R.string.mainlist_drawer_filters_save_question_title),
                            null,
                        (String input) -> {
                            saveFilter(input);
                        },
                        () -> resetSavedFilterSelection());

                } else if (position == indexSavedDelete) {
                    // Вариант 2: Клик на кнопку "- Удалить"
                    // Показываем окно подтверждения, удаляем при положительном ответе
                    final int currentIndex = MainListFilterUtils.getIndexSavedCurrent();
                    if (currentIndex == MainListFilterUtils.INDEX_SAVED_DEFAULT) {
                        mSavedFiltersSpinner.setSelection(currentIndex);
                        return;
                    }
                    ActivityUtils.showAlertDialog(
                            getContext(),
                            getString(R.string.mainlist_drawer_filters_remove_question_title),
                            getString(R.string.mainlist_drawer_filters_remove_question_text),
                            () -> removeSavedFilter(),
                            () -> resetSavedFilterSelection());
                } else {
                    Log.d("FILTER", "CREATION w position: " + position);
                    // Остальные варианты - выбираем
                    MainListFilterUtils.clickOnSavedFilter(position);
                    reloadDataFromCurrentFilter();
                    if (mCallbacks != null) {
                        mCallbacks.onFilterChanged();
                    }
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) { }
        });

        mSavedFiltersAdapter.clear();
        mSavedFiltersAdapter.addAll(MainListFilterUtils.getSavedFiltersList(getContext()));
        mSavedFiltersAdapter.notifyDataSetChanged();

    }

    /** Перезагружает адаптер списка фильтров по датам.
     * Всегда создает новый адаптер.
     */
    private void reloadDateFiltersList() {

        ArrayAdapter<String> dateFiltersAdapter =
                new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item);
        dateFiltersAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        dateFiltersAdapter.addAll(MainListFilterUtils.getsDateFiltersList(getContext()));
        mDateFiltersSpinner.setAdapter(dateFiltersAdapter);

        // Устанавливаем онклик слушатель
        mDateFiltersSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long l) {
                // Скрываем или показываем поле выбора дат
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


    /////////////////////////
    // Колбеки View.onClickListener и обработчики нажатий на различные кнопки

    @Override
    public void onClick(@NonNull View v) {

        switch (v.getId()) {
            case R.id.button_filterdrawer_sortname:
                // drop down
            case R.id.button_filterdrawer_sortcreate:
                // drop down
            case R.id.button_filterdrawer_sortmodified:
                // drop down
            case R.id.button_filterdrawer_sortview:
                // Нажатие на кнопку сортировки устанавливает меняет порядок сортировки или устанавливает новую
                if (v.getTag() != null) {
                    MainListFilter filter = MainListFilterUtils.getCurrentFilter();
                    if (v.isActivated()) {
                        filter.nextSortOrder();
                    } else {
                        filter.setSortType((MainListFilter.SortType) v.getTag());
                    }
                    reloadDataFromCurrentFilter();
                    if (mCallbacks != null) {
                        mCallbacks.onFilterChanged();
                    }
                }
                break;
            case R.id.imageButton_favorite_color:
                // Нажатие на круг фильтра по цвету меняет его статус активированности и применяет фильтр
                v.setActivated(!v.isActivated());
                toggleColorFilter((int) v.getTag(), v.isActivated());
                break;
            default:
                if ((mCallbacks != null) && (v.getTag() instanceof DrawerActions)) {
                    mCallbacks.onActionToggled((DrawerActions) v.getTag());
                }
                break;
        }

    }

    /** Включает или выключает фильтр по цвету из списка любимых.
     *
     * @param position позиция в списке любимых
     * @param activate признак, включить или выключить фильтр
     */
    private void toggleColorFilter(int position, boolean activate) {
        if (favColors != null) {
            MainListFilter filter = MainListFilterUtils.getCurrentFilter();
            int color = favColors[position];
            if (activate) {
                filter.addColorFilter(color);
            } else {
                filter.removeColorFilter(color);
            }
            reloadDataFromCurrentFilter();
            if (mCallbacks != null) {
                mCallbacks.onFilterChanged();
            }
        }
    }


    /////////////////////////
    // Вспомогательные методы для выбора дат

    /** Устанавливает в mDateEditor выбраную дату.
     *
     * @param picker диалог выбора даты
     * @param year год
     * @param month месяц
     * @param day день
     */
    @SuppressWarnings("UnusedParameters") // Используется как лямбда
    private void setDate(@Nullable DatePicker picker, int year, int month, int day) {
        if (mDateEditor != null) {
            mCalendar.set(Calendar.YEAR, year);
            mCalendar.set(Calendar.MONTH, month);
            mCalendar.set(Calendar.DAY_OF_MONTH, day);
            SimpleDateFormat sdf = new SimpleDateFormat(FORMAT_DATE, Locale.US);
            mDateEditor.setText(sdf.format(mCalendar.getTime()));
            MainListFilter filter = MainListFilterUtils.getCurrentFilter();
            long date = mCalendar.getTimeInMillis();
            mDateEditor.setTag(date);
            if (mDateEditor.getId() == R.id.edittext_filterdrawer_datefrom) {
                mCalendar.set(Calendar.HOUR_OF_DAY, 0);
                mCalendar.set(Calendar.MINUTE, 0);
                mCalendar.set(Calendar.SECOND, 0);
                filter.setDateFrom(date);
            } else {
                mCalendar.set(Calendar.HOUR_OF_DAY, 23);
                mCalendar.set(Calendar.MINUTE, 59);
                mCalendar.set(Calendar.SECOND, 59);
                filter.setDateTo(date);
            }
        }
    }

    /** Открывает диалог выбора даты.
     *
     * @param v view, которая вызвала открытие диалога, должна быть EditText
     */
    private void openDatePickerDialog(@NonNull View v) {

        // Сохраняем view, который сейчас редактируем
        mDateEditor = (EditText) v;

        // Открываем Dialog, установив заранее выбранную дату и границы
        long timeSelected = (long) mDateEditor.getTag();
        if (timeSelected > 0) {
            mCalendar.setTimeInMillis(timeSelected);
        }

        DatePickerDialog dialog = new DatePickerDialog(
                getContext(), // где открываем
                this::setDate, // что делать после выбора
                mCalendar.get(Calendar.YEAR), // текущее значение
                mCalendar.get(Calendar.MONTH),
                mCalendar.get(Calendar.DAY_OF_MONTH));

        DatePicker picker = dialog.getDatePicker();

        // Устанавливаем ограничители дат
        if (mDateEditor.getId() == R.id.edittext_filterdrawer_dateto) {
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


    /////////////////////////
    // Вспомогательные методы для работы с предопределенными фильтрами

    /** Удаляет текущий выбранный фильтр из списка сохраненных. */
    private void removeSavedFilter() {
        MainListFilterUtils.removeCurrentFilter(getContext());
        reloadSavedFiltersList();
        reloadDataFromCurrentFilter();
        if (mCallbacks != null) {
            mCallbacks.onFilterChanged();
        }
    }

    /** Сбрасывает текущий выбор фильтра в списке сохраненных. */
    private void resetSavedFilterSelection() {
        mSavedFiltersSpinner.setSelection(MainListFilterUtils.getIndexSavedCurrent());
    }

    /** Сохраняет текущий фильтр в список сохраненных.
     *
     * @param input название для нового фильтра
     */
    private void saveFilter(@NonNull String input) {
        // pos
        if (!input.isEmpty()
                && !input.equals(getString(R.string.mainlist_drawer_filters_default))) {
            MainListFilterUtils.saveFilter(getContext(), input);
            reloadSavedFiltersList();
            mSavedFiltersSpinner.setSelection(MainListFilterUtils.getIndexSavedCurrent());
        }
        resetSavedFilterSelection();
    }

}
