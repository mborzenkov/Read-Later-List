package com.example.mborzenkov.readlaterlist.activity.main;

import android.app.DatePickerDialog;
import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.text.InputFilter;
import android.text.InputType;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
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
import com.example.mborzenkov.readlaterlist.utility.DebugUtils;
import com.example.mborzenkov.readlaterlist.utility.FavoriteColorsUtils;
import com.example.mborzenkov.readlaterlist.utility.LongTaskNotifications;
import com.example.mborzenkov.readlaterlist.utility.MainListBackupUtils;
import com.example.mborzenkov.readlaterlist.utility.MainListFilterUtils;
import com.example.mborzenkov.readlaterlist.utility.ReadLaterDbUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Класс помощник для DrawerLayout в MainActivity. */
class MainListDrawerHelper implements View.OnClickListener {

    // Элементы Layout
    private final MainListActivity mActivity;
    private final DrawerLayout mDrawerLayout;
    private final LinearLayout mFavLinearLayout;
    private final Spinner mSavedFiltersSpinner;
    private final Spinner mDateFiltersSpinner;
    private final EditText mDateFromEditText;
    private final EditText mDateToEditText;
    private final Button mSortByLabelButton;
    private final Button mSortByDateCreatedButton;
    private final Button mSortByDateModifiedButton;
    private final Button mSortByDateViewedButton;
    private final TextView mCurrentUser;

    /** Адаптер для SavedFilters. */
    private ArrayAdapter<String> mSavedFiltersAdapter = null;
    /** Календарь для выбора. */
    private final Calendar mCalendar = Calendar.getInstance();
    /** Редактируемое поле даты. */
    private EditText mDateEditor = null;
    /** Оригинальные названия кнопок сортировки. */
    private final Map<MainListFilter.SortType, String> mSortButtonsNames = new HashMap<>();
    /** Добавляемый символ сортировки. */
    private final Map<MainListFilter.SortOrder, String> mSortOrderSymbols = new HashMap<>();
    /** Избранные цвета. */
    private int[] favColors = null;


    MainListDrawerHelper(MainListActivity activity) {

        mActivity = activity;

        // Объекты layout
        mFavLinearLayout = (LinearLayout) mActivity.findViewById(R.id.linearlayout_drawermainlist_favorites);
        mDrawerLayout = (DrawerLayout) mActivity.findViewById(R.id.drawerlayout_mainlist);
        mSavedFiltersSpinner = (Spinner) mActivity.findViewById(R.id.spinner_drawermainlist_filter);
        mDateFiltersSpinner = (Spinner) mActivity.findViewById(R.id.spinner_drawermainlist_datefilter);
        mDateFromEditText = (EditText) mActivity.findViewById(R.id.edittext_drawermainlist_datefrom);
        mDateToEditText = (EditText) mActivity.findViewById(R.id.edittext_drawermainlist_dateto);
        mSortByLabelButton = (Button) mActivity.findViewById(R.id.button_drawermainlist_sortname);
        mSortByDateCreatedButton = (Button) mActivity.findViewById(R.id.button_drawermainlist_sortcreate);
        mSortByDateModifiedButton = (Button) mActivity.findViewById(R.id.button_drawermainlist_sortmodified);
        mSortByDateViewedButton = (Button) mActivity.findViewById(R.id.button_drawermainlist_sortview);
        mCurrentUser = (TextView) mActivity.findViewById(R.id.tv_drawermainlist_user_value);

        // Инициализируем поле смены пользователя
        TextView urlChangeUser = (TextView) mActivity.findViewById(R.id.tv_drawermainlist_user_change);
        urlChangeUser.setOnClickListener((View v) -> {
            // Нажатие на "сменить пользователя"
            EditText inputNumber = new EditText(mActivity);
            inputNumber.setInputType(InputType.TYPE_CLASS_NUMBER);
            inputNumber.setFilters(new InputFilter[] {new InputFilter.LengthFilter(8)}); // Не более 8 цифр
            inputNumber.setText(mCurrentUser.getText().toString());
            ActivityUtils.showInputTextDialog(
                    mActivity,
                    inputNumber,
                    mActivity.getString(R.string.mainlist_menu_add_placeholders_question_title),
                    mActivity.getString(R.string.mainlist_menu_add_placeholders_question_text),
                    (input) -> {
                        try {
                            // Смотрим введенное значение
                            int number = Integer.parseInt(input);
                            UserInfo.changeCurrentUser(mActivity, number);
                            mCurrentUser.setText(String.valueOf(UserInfo.getCurentUser(mActivity).getUserId()));
                            mActivity.toggleSync();
                        } catch (ClassCastException e) {
                            Log.e("CAST ERROR", "Ошибка преобразования ввода пользователя в число");
                        }
                    },
                    null);
        });


        // Обработчик открытия и закрытия Drawer
        ActionBarDrawerToggle drawerToggle = new ActionBarDrawerToggle(mActivity, mDrawerLayout,
                R.string.mainlist_drawer_title, R.string.mainlist_drawer_title) {

            @Override
            public void onDrawerClosed(View view) {
                // При закрытии - устанавливаем фильтр
                super.onDrawerClosed(view);
                mActivity.reloadData();
            }

            @Override
            public void onDrawerOpened(View drawerView) {
                //  При открытии - обновляем Drawer на основании фильтра
                super.onDrawerOpened(drawerView);
                updateDrawerWithCurrentFilter();
            }
        };
        mDrawerLayout.addDrawerListener(drawerToggle);

        // Устанавливаем текущего пользователя
        if (UserInfo.userInfoNotSet()) {
            mActivity.toggleSync();
        }
        mCurrentUser.setText(String.valueOf(UserInfo.getCurentUser(mActivity).getUserId()));

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
        FavoriteColorsUtils.inflateFavLayout(mActivity, mFavLinearLayout);


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

        mSortOrderSymbols.put(MainListFilter.SortOrder.ASC,
                mActivity.getString(R.string.mainlist_drawer_sort_symb_asc));
        mSortOrderSymbols.put(MainListFilter.SortOrder.DESC,
                mActivity.getString(R.string.mainlist_drawer_sort_symb_desc));


        // Ставим клик листенер на кнопки бэкап
        mDrawerLayout.findViewById(R.id.button_drawermainlist_backupsave).setOnClickListener(this);
        mDrawerLayout.findViewById(R.id.button_drawermainlist_backuprestore).setOnClickListener(this);


        // Специальные возможности создаются только в DEBUG
        Button fillPlaceholdersButton =
                (Button) mDrawerLayout.findViewById(R.id.button_drawermainlist_fillplaceholders);
        Button deleteAllButton =
                (Button) mDrawerLayout.findViewById(R.id.button_drawermainlist_deleteall);
        fillPlaceholdersButton.setOnClickListener(this);
        deleteAllButton.setOnClickListener(this);
        if (!BuildConfig.DEBUG) {
            mDrawerLayout.findViewById(R.id.textview_drawermainlist_debug).setVisibility(View.INVISIBLE);
            fillPlaceholdersButton.setVisibility(View.INVISIBLE);
            deleteAllButton.setVisibility(View.INVISIBLE);
        }

    }

    @Override
    public void onClick(View v) {

        switch (v.getId()) {
            case R.id.button_drawermainlist_sortname:
                // drop down
            case R.id.button_drawermainlist_sortcreate:
                // drop down
            case R.id.button_drawermainlist_sortmodified:
                // drop down
            case R.id.button_drawermainlist_sortview:
                // Нажатие на кнопку сортировки устанавливает меняет порядок сортировки или устанавливает новую
                if (v.getTag() != null) {
                    MainListFilter filter = MainListFilterUtils.getCurrentFilter();
                    if (v.isActivated()) {
                        filter.nextSortOrder();
                    } else {
                        filter.setSortType((MainListFilter.SortType) v.getTag());
                    }
                    updateDrawerWithCurrentFilter();
                }
                break;
            case R.id.imageButton_favorite_color:
                // Нажатие на круг фильтра по цвету меняет его статус активированности и применяет фильтр
                v.setActivated(!v.isActivated());
                toggleColorFilter((int) v.getTag(), v.isActivated());
                break;
            default:
                // Нажатия на кнопки действий обрабатываются другой функцией
                clickOnActions(v);
                return;
        }

    }

    /** Обрабатывает нажатия на кнопки действий.
     * Управление передается из onClick, если onClick ничего не удалось обработать.
     *
     * @param v view, на которую нажали
     */
    private void clickOnActions(View v) {

        if (MainListLongTask.isActive()) {
            // Если выполняется какая-то работа, кнопки не работают, показывается предупреждение.
            ActivityUtils.showAlertDialog(mActivity,
                    mActivity.getString(R.string.mainlist_longloading_title),
                    mActivity.getString(R.string.mainlist_longloading_text),
                    null,
                    null);
            return;
        }

        switch (v.getId()) {
            case R.id.button_drawermainlist_backupsave:
                // Действие "Сохранить бэкап" открывает окно подтверждения и по положительному ответу
                // вызывает функцию для сохранения
                ActivityUtils.showAlertDialog(mActivity,
                    mActivity.getString(R.string.mainlist_drawer_backup_save_question_title),
                    mActivity.getString(R.string.mainlist_drawer_backup_save_question_text),
                    () -> handleBackupTask(true),
                    null);
                break;
            case R.id.button_drawermainlist_backuprestore:
                // Действие "Восстановить из бэкапа" открывает окно подтверждения и по положительному ответу
                // вызывает функцию для восстановления
                ActivityUtils.showAlertDialog(mActivity,
                    mActivity.getString(R.string.mainlist_drawer_backup_restore_question_title),
                    mActivity.getString(R.string.mainlist_drawer_backup_restore_question_text),
                    () -> handleBackupTask(false),
                    null);
                break;
            case R.id.button_drawermainlist_fillplaceholders:
                // Действие "Заполнить данными" открывает окно подтверждения и по положительному ответу
                // вызывает функцию для заполнения
                if (BuildConfig.DEBUG) {
                    EditText inputNumber = new EditText(mActivity);
                    inputNumber.setInputType(InputType.TYPE_CLASS_NUMBER);
                    inputNumber.setFilters(new InputFilter[] {new InputFilter.LengthFilter(1)}); // Не более 9
                    ActivityUtils.showInputTextDialog(
                        mActivity,
                        inputNumber,
                        mActivity.getString(R.string.mainlist_menu_add_placeholders_question_title),
                        mActivity.getString(R.string.mainlist_menu_add_placeholders_question_text),
                        (input) -> {
                            try {
                                // Смотрим введенное значение
                                int number = Integer.parseInt(input);
                                MainListLongTask.startLongBackgroundTask(
                                    () -> DebugUtils.addPlaceholdersToDatabase(mActivity, number),
                                        mActivity
                                );
                            } catch (ClassCastException e) {
                                Log.e("CAST ERROR", "Ошибка преобразования ввода пользователя в число");
                            }
                        },
                        null);
                }
                break;
            case R.id.button_drawermainlist_deleteall:
                // Действие "Удалить все" открывает окно подтверждения и по положительному ответу
                // вызывает функцию для очистки
                if (BuildConfig.DEBUG) {
                    ActivityUtils.showAlertDialog(
                        mActivity,
                        mActivity.getString(R.string.mainlist_menu_delete_all_question_title),
                        mActivity.getString(R.string.mainlist_menu_delete_all_question_text),
                        () -> {
                            // Запускаем таск, показываем нотификейшены
                            MainListLongTask.startLongBackgroundTask(
                                () -> {
                                    ReadLaterDbUtils.deleteAll(mActivity);
                                    LongTaskNotifications.cancelNotification();
                                },
                                mActivity
                            );
                            // Это быстро, обойдемся без нотификейшенов
                        },
                        null);
                }
                break;
            default:
                break;
        }
        mDrawerLayout.closeDrawer(Gravity.END);

    }

    /** Выполняет сохранение или восстановление бэкапов в фоновом потоке.
     *
     * @param savingMode true - режим сохранения данных, false - режим восстановления
     */
    private void handleBackupTask(boolean savingMode) {

        // Пробуем заблокировать интерфейс
        if (!MainListLongTask.startAnotherLongTask(mActivity)) {
            return; // не удалось, что то уже происходит
        }

        // Показываем индикатор загрузки
        mActivity.runOnUiThread(mActivity::showLoading);

        // Запускаем поток
        /* Имя хэндлер треда для бэкапа. */
        HandlerThread handlerThread = new HandlerThread("BackupHandlerThread");
        handlerThread.start();
        Looper looper = handlerThread.getLooper();
        Handler handler = new Handler(looper);

        // Выполняем работу
        if (savingMode) {
            handler.post(() -> {
                MainListBackupUtils.saveEverythingAsJsonFile(mActivity);
                if (MainListLongTask.stopAnotherLongTask()) {
                    mActivity.runOnUiThread(mActivity::showDataView);
                }
            });
        } else {
            handler.post(() -> {
                MainListBackupUtils.restoreEverythingFromJsonFile(mActivity);
                if (MainListLongTask.stopAnotherLongTask()) {
                    mActivity.runOnUiThread(mActivity::reloadData);
                }
            });
        }

    }

    /** Открывает этот Drawer. */
    void openDrawer() {
        mDrawerLayout.openDrawer(Gravity.END);
    }

    /** Перезагружает адаптер списка сохраненных фильтров.
     * Если адаптер еще не был создан, создает новый.
     */
    private void reloadSavedFiltersList() {
        if (mSavedFiltersAdapter == null) {
            mSavedFiltersAdapter = new ArrayAdapter<>(mActivity, android.R.layout.simple_spinner_item);
            mSavedFiltersAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            mSavedFiltersSpinner.setAdapter(mSavedFiltersAdapter);

            // Устанавливаем онклик слушатель
            mSavedFiltersSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> adapterView, View view, int position, long l) {
                    int indexSavedAdd = MainListFilterUtils.getIndexSavedAdd();
                    int indexSavedDelete = MainListFilterUtils.getIndexSavedDelete();
                    if (position == indexSavedAdd) {
                        // Вариант 1: Клик на кнопку "+ Добавить"
                        // Показываем окно ввода текста, сохраняем при успешном вводе
                        final EditText editText = new EditText(mActivity);
                        ActivityUtils.showInputTextDialog(mActivity,
                            editText,
                            mActivity.getString(R.string.mainlist_drawer_filters_save_question_title),
                            null,
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
                        ActivityUtils.showAlertDialog(mActivity,
                            mActivity.getString(R.string.mainlist_drawer_filters_remove_question_title),
                            mActivity.getString(R.string.mainlist_drawer_filters_remove_question_text),
                            () -> removeSavedFilter(),
                            () -> resetSavedFilterSelection());
                    } else {
                        // Остальные варианты - выбираем
                        MainListFilterUtils.clickOnSavedFilter(position);
                        updateDrawerWithCurrentFilter();
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> adapterView) { }
            });
        }

        mSavedFiltersAdapter.clear();
        mSavedFiltersAdapter.addAll(MainListFilterUtils.getSavedFiltersList(mActivity));
        mSavedFiltersAdapter.notifyDataSetChanged();
    }

    /** Перезагружает адаптер списка фильтров по датам.
     * Всегда создает новый адаптер.
     */
    private void reloadDateFiltersList() {
        ArrayAdapter<String> dateFiltersAdapter = new ArrayAdapter<>(mActivity, android.R.layout.simple_spinner_item);
        dateFiltersAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        dateFiltersAdapter.addAll(MainListFilterUtils.getsDateFiltersList(mActivity));
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

    /** Устанавливает в mDateEditor выбраную дату.
     *
     * @param picker диалог выбора даты
     * @param year год
     * @param month месяц
     * @param day день
     */
    @SuppressWarnings("UnusedParameters") // Используется как лямбда
    private void setDate(DatePicker picker, int year, int month, int day) {
        if (mDateEditor != null) {
            mCalendar.set(Calendar.YEAR, year);
            mCalendar.set(Calendar.MONTH, month);
            mCalendar.set(Calendar.DAY_OF_MONTH, day);
            SimpleDateFormat sdf = new SimpleDateFormat(MainListActivity.FORMAT_DATE, Locale.US);
            mDateEditor.setText(sdf.format(mCalendar.getTime()));
            MainListFilter filter = MainListFilterUtils.getCurrentFilter();
            long date = mCalendar.getTimeInMillis();
            mDateEditor.setTag(date);
            if (mDateEditor.getId() == R.id.edittext_drawermainlist_datefrom) {
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
    private void openDatePickerDialog(View v) {

        // Сохраняем view, который сейчас редактируем
        mDateEditor = (EditText) v;

        // Открываем Dialog, установив заранее выбранную дату и границы
        long timeSelected = (long) mDateEditor.getTag();
        if (timeSelected > 0) {
            mCalendar.setTimeInMillis(timeSelected);
        }

        DatePickerDialog dialog = new DatePickerDialog(
                mActivity, // где открываем
                this::setDate, // что делать после выбора
                mCalendar.get(Calendar.YEAR), // текущее значение
                mCalendar.get(Calendar.MONTH),
                mCalendar.get(Calendar.DAY_OF_MONTH));

        DatePicker picker = dialog.getDatePicker();

        // Устанавливаем ограничители дат
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

    /** Удаляет текущий выбранный фильтр из списка сохраненных. */
    private void removeSavedFilter() {
        MainListFilterUtils.removeCurrentFilter(mActivity);
        reloadSavedFiltersList();
        updateDrawerWithCurrentFilter();
    }

    /** Сбрасывает текущий выбор фильтра в списке сохраненных. */
    private void resetSavedFilterSelection() {
        mSavedFiltersSpinner.setSelection(MainListFilterUtils.getIndexSavedCurrent());
    }

    /** Сохраняет текущий фильтр в список сохраненных.
     *
     * @param input название для нового фильтра
     */
    private void saveFilter(String input) {
        // pos
        if (!input.isEmpty()
                && !input.equals(mActivity.getString(R.string.mainlist_drawer_filters_default))) {
            MainListFilterUtils.saveFilter(mActivity, input);
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
        favColors = FavoriteColorsUtils.updateFavLayoutFromSharedPreferences(mActivity, mFavLinearLayout, null,
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

    /** Включает или выключает фильтр по цвету из списка любимых.
     *
     * @param position позиция в списке любимых
     * @param activate признак, включить или выключить фильтр
     */
    private void toggleColorFilter(int position, boolean activate) {
        MainListFilter filter = MainListFilterUtils.getCurrentFilter();
        int color = favColors[position];
        if (activate) {
            filter.addColorFilter(color);
        } else {
            filter.removeColorFilter(color);
        }
    }

}
