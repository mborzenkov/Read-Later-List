package com.example.mborzenkov.readlaterlist.fragments.filterdrawer;

import android.app.DatePickerDialog;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;

import com.example.mborzenkov.readlaterlist.BuildConfig;
import com.example.mborzenkov.readlaterlist.R;
import com.example.mborzenkov.readlaterlist.adt.MainListFilter;
import com.example.mborzenkov.readlaterlist.utility.ActivityUtils;
import com.example.mborzenkov.readlaterlist.utility.FavoriteColorsUtils;
import com.example.mborzenkov.readlaterlist.utility.MainListFilterUtils;
import com.example.mborzenkov.readlaterlist.utility.UserInfoUtils;

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

    // Константы времени
    private static final int LAST_HOUR = 23;
    private static final int LAST_MINUTE = 59;
    private static final int LAST_SECOND = 59;

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
    private @Nullable FilterDrawerCallbacks mCallbacks = null;

    /** ViewHolder. */
    FilterDrawerViewHolder mViewHolder;

    // Хэлперы
    /** Адаптер для SavedFilters. */
    private @Nullable ArrayAdapter<String> mSavedFiltersAdapter = null;
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
        if (context instanceof FilterDrawerCallbacks) {
            mCallbacks = (FilterDrawerCallbacks) context;
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_drawer_filter, container, false);
        loaded = false;

        mViewHolder = new FilterDrawerViewHolder(rootView);

        // Инициализируем поле смены пользователя
        mViewHolder.mUrlChangeUser.setOnClickListener((View v) -> {
            UserInfoUtils.showDialogAndChangeUser(getActivity(), () -> {
                mViewHolder.mCurrentUserTextView.setText(String.valueOf(
                        UserInfoUtils.getCurentUser(getContext()).getUserId()));
                if (mCallbacks != null) {
                    mCallbacks.onUserChanged();
                }
            });
        });

        // Устанавливаем текущего пользователя
        mViewHolder.mCurrentUserTextView.setText(String.valueOf(UserInfoUtils.getCurentUser(getContext()).getUserId()));

        // Заполняем варианты запомненных фильтров
        reloadSavedFiltersList();

        // Заполняем варианты фильтров по дате
        reloadDateFiltersList();

        // DatePicker на полях с датами
        View.OnClickListener onDateClick = this::openDatePickerDialog;
        long zeroLong = 0; // В таги лучше сразу записать long, чтобы потом не конвертировать
        mViewHolder.mDateFromEditText.setOnClickListener(onDateClick);
        mViewHolder.mDateFromEditText.setTag(zeroLong);
        mViewHolder.mDateToEditText.setOnClickListener(onDateClick);
        mViewHolder.mDateToEditText.setTag(zeroLong);

        // Добавляем Favorites на Drawer Layout
        FavoriteColorsUtils.inflateFavLayout(getContext(), inflater, mViewHolder.mFavLinearLayout);

        // Инициализируем кнопки SortBy
        mViewHolder.mSortByManualOrderButton.setTag(MainListFilter.SortType.MANUAL);
        mViewHolder.mSortByManualOrderButton.setOnClickListener(this);
        mSortButtonsNames.put(MainListFilter.SortType.MANUAL,
                mViewHolder.mSortByManualOrderButton.getText().toString());

        mViewHolder.mSortByLabelButton.setTag(MainListFilter.SortType.LABEL);
        mViewHolder.mSortByLabelButton.setOnClickListener(this);
        mSortButtonsNames.put(MainListFilter.SortType.LABEL,
                mViewHolder.mSortByLabelButton.getText().toString());

        mViewHolder.mSortByDateCreatedButton.setTag(MainListFilter.SortType.DATE_CREATED);
        mViewHolder.mSortByDateCreatedButton.setOnClickListener(this);
        mSortButtonsNames.put(MainListFilter.SortType.DATE_CREATED,
                mViewHolder.mSortByDateCreatedButton.getText().toString());

        mViewHolder.mSortByDateModifiedButton.setTag(MainListFilter.SortType.DATE_MODIFIED);
        mViewHolder.mSortByDateModifiedButton.setOnClickListener(this);
        mSortButtonsNames.put(MainListFilter.SortType.DATE_MODIFIED,
                mViewHolder.mSortByDateModifiedButton.getText().toString());

        mViewHolder.mSortByDateViewedButton.setTag(MainListFilter.SortType.DATE_VIEWED);
        mViewHolder.mSortByDateViewedButton.setOnClickListener(this);
        mSortButtonsNames.put(MainListFilter.SortType.DATE_VIEWED,
                mViewHolder.mSortByDateViewedButton.getText().toString());

        mSortOrderSymbols.put(MainListFilter.SortOrder.ASC, getString(R.string.mainlist_drawer_sort_symb_asc));
        mSortOrderSymbols.put(MainListFilter.SortOrder.DESC, getString(R.string.mainlist_drawer_sort_symb_desc));

        // Ставим клик листенер и таги на кнопки бэкап
        mViewHolder.mBackupSaveButton.setOnClickListener(this);
        mViewHolder.mBackupSaveButton.setTag(DrawerActions.BACKUP_SAVE);
        mViewHolder.mBackupRestoreButton.setOnClickListener(this);
        mViewHolder.mBackupRestoreButton.setTag(DrawerActions.BACKUP_RESTORE);

        // Специальные возможности создаются только в DEBUG, ставим клик листенеры и таги
        mViewHolder.mFillWithPlaceHoldersButton.setOnClickListener(this);
        mViewHolder.mFillWithPlaceHoldersButton.setTag(DrawerActions.FILL_PLACEHOLDERS);
        mViewHolder.mDeleteAllButton.setOnClickListener(this);
        mViewHolder.mDeleteAllButton.setTag(DrawerActions.DELETE_ALL);
        if (!BuildConfig.DEBUG) {
            rootView.findViewById(R.id.textview_filterdrawer_debug).setVisibility(View.INVISIBLE);
            mViewHolder.mFillWithPlaceHoldersButton.setVisibility(View.INVISIBLE);
            mViewHolder.mDeleteAllButton.setVisibility(View.INVISIBLE);
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
    private void reloadDataFromCurrentFilter() {

        MainListFilter currentFilter = MainListFilterUtils.getCurrentFilter();

        Log.d("CURRENT_FILTER", currentFilter.toString());

        mViewHolder.mDateFiltersSpinner.setSelection(currentFilter.getSelection().getPosition(), false);
        mViewHolder.mDateFromEditText.setText(currentFilter.getDateFrom());
        mViewHolder.mDateToEditText.setText(currentFilter.getDateTo());
        switch (mViewHolder.mDateFiltersSpinner.getSelectedItemPosition()) {
            case MainListFilterUtils.INDEX_DATE_ALL:
                mViewHolder.mDateFromEditText.setVisibility(View.GONE);
                mViewHolder.mDateToEditText.setVisibility(View.GONE);
                break;
            default:
                mViewHolder.mDateFromEditText.setVisibility(View.VISIBLE);
                mViewHolder.mDateToEditText.setVisibility(View.VISIBLE);
                break;
        }
        favColors = FavoriteColorsUtils.updateFavLayoutFromSharedPreferences(getContext(), mViewHolder.mFavLinearLayout,
                null, this, currentFilter.getColorFilter());

        resetButtons();
        Button selectedSortButton = null;
        switch (currentFilter.getSortType()) {
            case MANUAL:
                selectedSortButton = mViewHolder.mSortByManualOrderButton;
                break;
            case LABEL:
                selectedSortButton = mViewHolder.mSortByLabelButton;
                break;
            case DATE_CREATED:
                selectedSortButton = mViewHolder.mSortByDateCreatedButton;
                break;
            case DATE_MODIFIED:
                selectedSortButton = mViewHolder.mSortByDateModifiedButton;
                break;
            case DATE_VIEWED:
                selectedSortButton = mViewHolder.mSortByDateViewedButton;
                break;
            default:
                break;
        }
        selectedSortButton.setActivated(true);
        if (selectedSortButton != mViewHolder.mSortByManualOrderButton) {
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
        mViewHolder.mSavedFiltersSpinner.setAdapter(mSavedFiltersAdapter);

        // Устанавливаем онклик слушатель
        mViewHolder.mSavedFiltersSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
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
                        (String input) -> saveFilter(input),
                        () -> resetSavedFilterSelection());

                } else if (position == indexSavedDelete) {
                    // Вариант 2: Клик на кнопку "- Удалить"
                    // Показываем окно подтверждения, удаляем при положительном ответе
                    final int currentIndex = MainListFilterUtils.getIndexSavedCurrent();
                    if (currentIndex == MainListFilterUtils.INDEX_SAVED_DEFAULT) {
                        mViewHolder.mSavedFiltersSpinner.setSelection(currentIndex);
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
        mViewHolder.mDateFiltersSpinner.setAdapter(dateFiltersAdapter);

        // Устанавливаем онклик слушатель
        mViewHolder.mDateFiltersSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long l) {
                // Скрываем или показываем поле выбора дат
                MainListFilter filter = MainListFilterUtils.getCurrentFilter();
                filter.setSelection(MainListFilterUtils.getDateFilterSelection(position));
                switch (position) {
                    case MainListFilterUtils.INDEX_DATE_ALL:
                        mViewHolder.mDateFromEditText.setVisibility(View.GONE);
                        mViewHolder.mDateToEditText.setVisibility(View.GONE);
                        break;
                    default:
                        mViewHolder.mDateFromEditText.setVisibility(View.VISIBLE);
                        mViewHolder.mDateToEditText.setVisibility(View.VISIBLE);
                        break;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) { }
        });

    }

    /** Сбрасывает все кнопки SortBy. */
    private void resetButtons() {
        mViewHolder.mSortByManualOrderButton.setActivated(false);
        // set text не нужен, так как не меняется
        mViewHolder.mSortByLabelButton.setActivated(false);
        mViewHolder.mSortByLabelButton.setText(mSortButtonsNames.get(MainListFilter.SortType.LABEL));
        mViewHolder.mSortByDateCreatedButton.setActivated(false);
        mViewHolder.mSortByDateCreatedButton.setText(mSortButtonsNames.get(MainListFilter.SortType.DATE_CREATED));
        mViewHolder.mSortByDateModifiedButton.setActivated(false);
        mViewHolder.mSortByDateModifiedButton.setText(mSortButtonsNames.get(MainListFilter.SortType.DATE_MODIFIED));
        mViewHolder.mSortByDateViewedButton.setActivated(false);
        mViewHolder.mSortByDateViewedButton.setText(mSortButtonsNames.get(MainListFilter.SortType.DATE_VIEWED));
    }


    /////////////////////////
    // Колбеки View.onClickListener и обработчики нажатий на различные кнопки

    @Override
    public void onClick(@NonNull View v) {

        switch (v.getId()) {
            case R.id.button_filterdrawer_sortmanual:
                if (v.isActivated()) {
                    // Если уже активирована, то тут нет второго режима
                    return;
                }
                // fall through
            case R.id.button_filterdrawer_sortname:
                // fall through
            case R.id.button_filterdrawer_sortcreate:
                // fall through
            case R.id.button_filterdrawer_sortmodified:
                // fall through
            case R.id.button_filterdrawer_sortview:
                // Нажатие на кнопку сортировки устанавливает новый тип сортировки или меняет порядок текущей
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
            // Получаем новый календарь и устанавливаем в нем выбранную дату
            Calendar calendar = Calendar.getInstance();
            calendar.set(year, month, day);

            // Правим время на начало (если from) или конец (если to) и правим фильтр
            MainListFilter filter = MainListFilterUtils.getCurrentFilter();
            long date;
            if (mDateEditor.getId() == R.id.edittext_filterdrawer_datefrom) {
                calendar.set(Calendar.HOUR_OF_DAY, 0);
                calendar.set(Calendar.MINUTE, 0);
                calendar.set(Calendar.SECOND, 0);
                date = calendar.getTimeInMillis();
                filter.setDateFrom(date);
            } else {
                calendar.set(Calendar.HOUR_OF_DAY, LAST_HOUR);
                calendar.set(Calendar.MINUTE, LAST_MINUTE);
                calendar.set(Calendar.SECOND, LAST_SECOND);
                date = calendar.getTimeInMillis();
                filter.setDateTo(date);
            }

            // Ставим выбранную дату в нужное поле и запоминаем таг, чтобы на парсить потом
            SimpleDateFormat sdf = new SimpleDateFormat(FORMAT_DATE, Locale.US);
            mDateEditor.setText(sdf.format(calendar.getTime()));
            mDateEditor.setTag(date);

            // Обновляем данные
            reloadDataFromCurrentFilter();
            if (mCallbacks != null) {
                mCallbacks.onFilterChanged();
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
        Calendar calendar = Calendar.getInstance();
        if (timeSelected > 0) {
            calendar.setTimeInMillis(timeSelected);
        }

        DatePickerDialog dialog = new DatePickerDialog(
                getContext(), // где открываем
                this::setDate, // что делать после выбора
                calendar.get(Calendar.YEAR), // текущее значение
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH));

        DatePicker picker = dialog.getDatePicker();

        // Устанавливаем ограничители дат
        if (mDateEditor.getId() == R.id.edittext_filterdrawer_dateto) {
            picker.setMinDate((long) mViewHolder.mDateFromEditText.getTag());
            picker.setMaxDate(System.currentTimeMillis());
        } else {
            long timeAfter = (long) mViewHolder.mDateToEditText.getTag();
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
        mViewHolder.mSavedFiltersSpinner.setSelection(MainListFilterUtils.getIndexSavedCurrent());
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
            mViewHolder.mSavedFiltersSpinner.setSelection(MainListFilterUtils.getIndexSavedCurrent());
        }
        resetSavedFilterSelection();
    }

}
