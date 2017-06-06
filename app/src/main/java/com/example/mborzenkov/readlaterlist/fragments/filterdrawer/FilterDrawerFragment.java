package com.example.mborzenkov.readlaterlist.fragments.filterdrawer;

import android.app.DatePickerDialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.DrawableContainer;
import android.graphics.drawable.GradientDrawable;
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
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.LinearLayout;

import com.example.mborzenkov.readlaterlist.R;
import com.example.mborzenkov.readlaterlist.adt.MainListFilter;
import com.example.mborzenkov.readlaterlist.adt.UserInfo;
import com.example.mborzenkov.readlaterlist.utility.view.ActivityUtils;
import com.example.mborzenkov.readlaterlist.utility.FavoriteColorsUtils;
import com.example.mborzenkov.readlaterlist.utility.MainListFilterUtils;
import com.example.mborzenkov.readlaterlist.utility.UserInfoUtils;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Фрагмент, представляющий собой Drawer с фильтрами.
 * Оповещает о выборе нового пользователя и о нажатиях на кнопки.
 */
public class FilterDrawerFragment extends Fragment {

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
    private FilterDrawerViewHolder mViewHolder;

    // Хэлперы
    /** Редактируемое поле даты. */
    private @Nullable EditText mDateEditor = null;
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

        loaded = false;

        View rootView = inflater.inflate(R.layout.fragment_drawer_filter, container, false);

        // Создаем AdapterView.OnItemSelectedListener
        AdapterView.OnItemSelectedListener onSavedFilterSelectedListener = new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long l) {
                // При запуске возникает onItemSelected, нужно первый отсеять
                if (!loaded) {
                    loaded = true;
                    return;
                }
                int indexSavedAdd = MainListFilterUtils.getIndexSavedAdd();
                int indexSavedDelete = MainListFilterUtils.getIndexSavedDelete();
                if (position == indexSavedAdd) {
                    // Вариант 1: Клик на кнопку "+ Добавить"
                    // Показываем окно ввода текста, сохраняем при успешном вводе
                    Context context = getContext();
                    final EditText editText = new EditText(context);
                    ActivityUtils.showInputTextDialog(
                            context,
                            editText,
                            context.getString(R.string.mainlist_drawer_filters_save_question_title),
                            null,
                            new ActivityUtils.Consumer<String>() {
                                @Override
                                public void accept(String param) {
                                    saveFilter(param);
                                }
                            },
                            new Runnable() {
                                @Override
                                public void run() {
                                    resetSavedFilterSelection();
                                }
                            });

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
                            new Runnable() {
                                @Override
                                public void run() {
                                    removeSavedFilter();
                                }
                            },
                            new Runnable() {
                                @Override
                                public void run() {
                                    resetSavedFilterSelection();
                                }
                            });
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
        };


        // Создаем новый ViewHolder
        mViewHolder = new FilterDrawerViewHolder(rootView);

        // Заполняем все значениями по умолчанию
        mViewHolder.initializeWithDefaults(
                getContext(),
                inflater,
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        onUserChangeClickListener(v);
                    }
                },
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        onDateClickListener(v);
                    }
                },
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        onSortButtonClickListener(v);
                    }
                },
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        onActionButtonClickListener(v);
                    }
                },
                onSavedFilterSelectedListener);

        // Запоминаем символы сортировок
        mSortOrderSymbols.put(MainListFilter.SortOrder.ASC, getString(R.string.mainlist_drawer_sort_symb_asc));
        mSortOrderSymbols.put(MainListFilter.SortOrder.DESC, getString(R.string.mainlist_drawer_sort_symb_desc));

        // Читаем текущий фильтр
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
        favColors = updateFavLayoutFromSharedPreferences(getContext(), mViewHolder.mFavLinearLayout,
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        onFavoriteColorClickListener(v);
                    }
                }, currentFilter.getColorFilter());

        mViewHolder.resetButtons();
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
        selectedSortButton.setActivated(true); // NOTNULL всегда, кроме если добавят SortType и тут не поменять
        if (selectedSortButton != mViewHolder.mSortByManualOrderButton) {
            selectedSortButton.setText(selectedSortButton.getText().toString()
                    + " " + mSortOrderSymbols.get(currentFilter.getSortOrder()));
        }

    }


    /////////////////////////
    // Колбеки View.onClickListener и обработчики нажатий на различные кнопки

    /** Обработчик нажатия на кнопку сортировки.
     *
     * @param v Button кнопка сортировки
     */
    private void onSortButtonClickListener(@NonNull View v) {
        switch (v.getId()) {
            case R.id.button_filterdrawer_sortmanual:
                if (v.isActivated()) { // Если уже активирована, то тут нет второго режима
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
            default:
                break;
        }
    }

    /** Обработчик нажатия на кнопку действия.
     *
     * @param v Button кнопка действия
     */
    private void onActionButtonClickListener(@NonNull View v) {
        switch (v.getId()) {
            case R.id.button_filterdrawer_backupsave:
                // fall through
            case R.id.button_filterdrawer_backuprestore:
                // fall through
            case R.id.button_filterdrawer_fillplaceholders:
                // fall through
            case R.id.button_filterdrawer_deleteall:
                if ((mCallbacks != null) && (v.getTag() instanceof DrawerActions)) {
                    mCallbacks.onActionToggled((DrawerActions) v.getTag());
                }
                break;
            default:
                break;
        }
    }

    /** Обработчик нажатия на один из любимых цветов.
     *
     * @param v ImageButton любимый цвет
     */
    private void onFavoriteColorClickListener(@NonNull View v) {
        if (v.getId() != R.id.imageButton_favorite_color) {
            return;
        }
        // Нажатие на круг фильтра по цвету меняет его статус активированности и применяет фильтр
        v.setActivated(!v.isActivated());
        toggleColorFilter((int) v.getTag(), v.isActivated());
    }

    /** Обработчик нажатия на кнопку "Сменить пользователя".
     *
     * @param v View - кнопка "Сменить пользователя"
     */
    private void onUserChangeClickListener(@NonNull View v) {
        if (v.getId() != R.id.tv_filterdrawer_user_change) {
            return;
        }
        // Нажатие на "сменить пользователя"
        final Context context = getContext();
        EditText inputNumber = new EditText(getActivity());
        inputNumber.setInputType(InputType.TYPE_CLASS_NUMBER);
        inputNumber.setFilters(new InputFilter[] {new InputFilter.LengthFilter(UserInfo.USER_ID_MAX_LENGTH)});
        inputNumber.setText(mViewHolder.mCurrentUserTextView.getText().toString());
        ActivityUtils.showInputTextDialog(
                getContext(),
                inputNumber,
                getString(R.string.mainlist_user_change_question_title),
                getString(R.string.mainlist_user_change_question_text),
                new ActivityUtils.Consumer<String>() {
                    @Override
                    public void accept(String param) {
                        try {
                            // Смотрим введенное значение
                            int number = Integer.parseInt(param);
                            if (number != UserInfoUtils.getCurentUser(context).getUserId()) {
                                UserInfoUtils.changeCurrentUser(context, number);
                                mViewHolder.mCurrentUserTextView.setText(String.valueOf(
                                        UserInfoUtils.getCurentUser(context).getUserId()));
                                if (mCallbacks != null) {
                                    mCallbacks.onUserChanged();
                                }
                            }
                        } catch (ClassCastException e) {
                            Log.e("CAST ERROR", "Ошибка преобразования ввода пользователя в число");
                        }
                    }
                },
                null);
    }

    /** Обработчик нажатия на кнопку "Сменить пользователя".
     *
     * @param v EditText с датой
     */
    private void onDateClickListener(@NonNull View v) {
        switch (v.getId()) {
            case R.id.edittext_filterdrawer_datefrom:
                // fall through
            case R.id.edittext_filterdrawer_dateto:
                mDateEditor = (EditText) v;
                final long timeSelected = (long) mDateEditor.getTag();
                long leftDateBorder;
                long rightDateBorder;
                if (mDateEditor.getId() == R.id.edittext_filterdrawer_dateto) {
                    leftDateBorder = (long) mViewHolder.mDateFromEditText.getTag();
                    rightDateBorder = System.currentTimeMillis();
                } else {
                    leftDateBorder = 0;
                    rightDateBorder = (long) mViewHolder.mDateToEditText.getTag(); // Вернет 0, если не установлена
                    if (rightDateBorder <= 0) {
                        rightDateBorder = System.currentTimeMillis();
                    }
                }
                ActivityUtils.openDatePickerDialog(getContext(), timeSelected, leftDateBorder, rightDateBorder,
                        new DatePickerDialog.OnDateSetListener() {
                            @Override
                            public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                                setDate(year, month, dayOfMonth);
                            }
                        });
                break;
            default:
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
     * @param year год
     * @param month месяц
     * @param day день
     */
    private void setDate(int year, int month, int day) {
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


    /////////////////////////
    // Вспомогательные методы для работы с предопределенными фильтрами

    /** Удаляет текущий выбранный фильтр из списка сохраненных. */
    private void removeSavedFilter() {
        MainListFilterUtils.removeCurrentFilter(getContext());
        mViewHolder.resetSavedFilter(getContext());
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
            mViewHolder.resetSavedFilter(getContext());
            mViewHolder.mSavedFiltersSpinner.setSelection(MainListFilterUtils.getIndexSavedCurrent());
        }
        resetSavedFilterSelection();
    }

    /** Обновляет layout с любимыми кругами на основании данных в Shared Preferences.
     *
     * @param context Контекст
     * @param layout Layout
     * @param clickListener Ссылка на OnClickListener, который устанавливается для кругов
     * @param colorFilter Фильтр цвета, если указан, то круги будут помечены .active
     * @return Список любимых цветов, как getFavoriteColorsFromSharedPreferences(...)
     */
    private static int[] updateFavLayoutFromSharedPreferences(Context context,
                                                              LinearLayout layout,
                                                              @Nullable View.OnClickListener clickListener,
                                                              @Nullable Set<Integer> colorFilter) {

        int[] result = FavoriteColorsUtils.getFavoriteColorsFromSharedPreferences(context, null);

        for (int i = 0; i < result.length; i++) {
            int savedColor = result[i];
            View favCircle = layout.getChildAt(i).findViewById(R.id.imageButton_favorite_color);
            if (savedColor != Color.TRANSPARENT) {
                DrawableContainer.DrawableContainerState containerState = ((DrawableContainer.DrawableContainerState) (
                        favCircle.getBackground()).getConstantState());
                if (containerState != null) {
                    Drawable[] children = containerState.getChildren();
                    ((GradientDrawable) children[0]).setColor(savedColor);
                    ((GradientDrawable) children[1]).setColor(savedColor);
                    ((GradientDrawable) children[2]).setColor(savedColor);
                    favCircle.setOnClickListener(clickListener);
                    favCircle.setClickable(true);
                }
                if (colorFilter != null) {
                    favCircle.setActivated(colorFilter.contains(savedColor));
                }
            } else {
                favCircle.setOnClickListener(null);
                favCircle.setClickable(false);
                favCircle.setActivated(false);
            }
        }
        return result;
    }

}
