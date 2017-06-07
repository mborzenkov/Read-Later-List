package com.example.mborzenkov.readlaterlist.fragments.filterdrawer;

import android.app.DatePickerDialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.DrawableContainer;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
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
import com.example.mborzenkov.readlaterlist.activity.main.DialogUtils;
import com.example.mborzenkov.readlaterlist.adt.MainListFilter;

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

    // Объекты Layout
    private LinearLayout mFavLinearLayout;
    private Spinner mSavedFiltersSpinner;
    private Spinner mDateFiltersSpinner;
    private EditText mDateFromEditText;
    private EditText mDateToEditText;
    private Button mSortByManualOrderButton;
    private Button mSortByLabelButton;
    private Button mSortByDateCreatedButton;
    private Button mSortByDateModifiedButton;
    private Button mSortByDateViewedButton;
    private TextView mCurrentUserTextView;
    private TextView mUrlChangeUser;
    private Button mBackupSaveButton;
    private Button mBackupRestoreButton;
    private Button mFillWithPlaceHoldersButton;
    private Button mDeleteAllButton;
    private TextView mDebugTextView;

    // Хэлперы
    /** Оригинальные названия кнопок сортировки. */
    private final Map<MainListFilter.SortType, String> mSortButtonsNames = new HashMap<>();
    /** Редактируемое поле даты. */
    private @Nullable EditText mDateEditor = null;
    /** Добавляемый символ сортировки. */
    private final Map<MainListFilter.SortOrder, String> mSortOrderSymbols = new HashMap<>();
    /** Избранные цвета. */
    private @Nullable int[] mFavColors = null;
    /** Признак выполнения загрузки. */
    private boolean loaded;


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

        mFavLinearLayout            = (LinearLayout) rootView.findViewById(R.id.linearlayout_filterdrawer_favorites);
        mSavedFiltersSpinner        = (Spinner) rootView.findViewById(R.id.spinner_filterdrawer_filter);
        mDateFiltersSpinner         = (Spinner) rootView.findViewById(R.id.spinner_filterdrawer_datefilter);
        mDateFromEditText           = (EditText) rootView.findViewById(R.id.edittext_filterdrawer_datefrom);
        mDateToEditText             = (EditText) rootView.findViewById(R.id.edittext_filterdrawer_dateto);
        mSortByManualOrderButton    = (Button) rootView.findViewById(R.id.button_filterdrawer_sortmanual);
        mSortByLabelButton          = (Button) rootView.findViewById(R.id.button_filterdrawer_sortname);
        mSortByDateCreatedButton    = (Button) rootView.findViewById(R.id.button_filterdrawer_sortcreate);
        mSortByDateModifiedButton   = (Button) rootView.findViewById(R.id.button_filterdrawer_sortmodified);
        mSortByDateViewedButton     = (Button) rootView.findViewById(R.id.button_filterdrawer_sortview);
        mCurrentUserTextView        = (TextView) rootView.findViewById(R.id.tv_filterdrawer_user_value);
        mUrlChangeUser              = (TextView) rootView.findViewById(R.id.tv_filterdrawer_user_change);
        mBackupSaveButton           = (Button) rootView.findViewById(R.id.button_filterdrawer_backupsave);
        mBackupRestoreButton        = (Button) rootView.findViewById(R.id.button_filterdrawer_backuprestore);
        mFillWithPlaceHoldersButton = (Button) rootView.findViewById(R.id.button_filterdrawer_fillplaceholders);
        mDeleteAllButton            = (Button) rootView.findViewById(R.id.button_filterdrawer_deleteall);
        mDebugTextView              = (TextView) rootView.findViewById(R.id.textview_filterdrawer_debug);

        // Заполняем все значениями по умолчанию
        initializeWithDefaults();

        // Читаем текущий фильтр
        if (mCallbacks != null) {

            // Добавляем Favorites на Drawer Layout
            mFavColors = mCallbacks.getFavoriteColors();
            inflateFavLayout(inflater, mFavColors.length);

            // Запоминаем символы сортировок
            mSortOrderSymbols.put(MainListFilter.SortOrder.ASC, getString(R.string.mainlist_drawer_sort_symb_asc));
            mSortOrderSymbols.put(MainListFilter.SortOrder.DESC, getString(R.string.mainlist_drawer_sort_symb_desc));

            onFilterChanged(mCallbacks.getCurrentFilter());
        }

        return rootView;

    }

    /** Заполняет все объекты layout значениями по умолчанию и устанавливает onClickListener'ы.
     * Все объекты должны быть уже инициализированы.
     */
    private void initializeWithDefaults() {

        // Преезагружаем выпадающие списки
        reloadSavedFiltersList();
        reloadDateFiltersList();

        // Инициализируем поле смены пользователя
        mUrlChangeUser.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mCallbacks != null) {
                    mCallbacks.onChangeUserClick();
                }
            }
        });

        // Устанавливаем текущего пользователя пустым
        if (mCallbacks != null) {
            mCurrentUserTextView.setText(mCallbacks.getCurrentUser());
        }


        // Создаем OnDateClickListener
        View.OnClickListener onDateClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onDateClickListener(v);
            }
        };

        // DatePicker на полях с датами
        final long zeroLong = 0; // В таги лучше сразу записать long, чтобы потом не конвертировать
        mDateFromEditText.setOnClickListener(onDateClickListener);
        mDateFromEditText.setTag(zeroLong);
        mDateToEditText.setOnClickListener(onDateClickListener);
        mDateToEditText.setTag(zeroLong);


        // Создаем OnSortClickListener
        View.OnClickListener onSortButtonClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
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
                        if ((mCallbacks != null) && (v.getTag() instanceof MainListFilter.SortType)) {
                            mCallbacks.onSortButtonClick((MainListFilter.SortType) v.getTag());
                        }
                        break;
                    default:
                        break;
                }
            }
        };

        // Инициализируем кнопки SortBy
        mSortByManualOrderButton.setTag(MainListFilter.SortType.MANUAL);
        mSortByManualOrderButton.setOnClickListener(onSortButtonClickListener);
        mSortButtonsNames.put(MainListFilter.SortType.MANUAL, mSortByManualOrderButton.getText().toString());

        mSortByLabelButton.setTag(MainListFilter.SortType.LABEL);
        mSortByLabelButton.setOnClickListener(onSortButtonClickListener);
        mSortButtonsNames.put(MainListFilter.SortType.LABEL,
                mSortByLabelButton.getText().toString());

        mSortByDateCreatedButton.setTag(MainListFilter.SortType.DATE_CREATED);
        mSortByDateCreatedButton.setOnClickListener(onSortButtonClickListener);
        mSortButtonsNames.put(MainListFilter.SortType.DATE_CREATED,
                mSortByDateCreatedButton.getText().toString());

        mSortByDateModifiedButton.setTag(MainListFilter.SortType.DATE_MODIFIED);
        mSortByDateModifiedButton.setOnClickListener(onSortButtonClickListener);
        mSortButtonsNames.put(MainListFilter.SortType.DATE_MODIFIED,
                mSortByDateModifiedButton.getText().toString());

        mSortByDateViewedButton.setTag(MainListFilter.SortType.DATE_VIEWED);
        mSortByDateViewedButton.setOnClickListener(onSortButtonClickListener);
        mSortButtonsNames.put(MainListFilter.SortType.DATE_VIEWED,
                mSortByDateViewedButton.getText().toString());


        // Создаем onActionButtonClickListener
        View.OnClickListener onActionButtonClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
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
        };

        // Ставим клик листенер и таги на кнопки бэкап
        mBackupSaveButton.setOnClickListener(onActionButtonClickListener);
        mBackupSaveButton.setTag(FilterDrawerFragment.DrawerActions.BACKUP_SAVE);
        mBackupRestoreButton.setOnClickListener(onActionButtonClickListener);
        mBackupRestoreButton.setTag(FilterDrawerFragment.DrawerActions.BACKUP_RESTORE);

        // Специальные возможности создаются только в DEBUG, ставим клик листенеры и таги
        mFillWithPlaceHoldersButton.setOnClickListener(onActionButtonClickListener);
        mFillWithPlaceHoldersButton.setTag(FilterDrawerFragment.DrawerActions.FILL_PLACEHOLDERS);
        mDeleteAllButton.setOnClickListener(onActionButtonClickListener);
        mDeleteAllButton.setTag(FilterDrawerFragment.DrawerActions.DELETE_ALL);
        if (!BuildConfig.DEBUG) {
            mDebugTextView.setVisibility(View.INVISIBLE);
            mFillWithPlaceHoldersButton.setVisibility(View.INVISIBLE);
            mDeleteAllButton.setVisibility(View.INVISIBLE);
        }

    }

    /** Добавляет Favorite кружки на favorite layout.
     * Все объекты должны быть уже инициализированы.
     *
     * @param inflater инфлейтер для инфлейтинга
     * @param numberOfFavorites количество кружков
     */
    private void inflateFavLayout(@NonNull LayoutInflater inflater,
                                  int numberOfFavorites) {

        Context context = getContext();

        for (int i = 0; i < numberOfFavorites; i++) {
            StateListDrawable circle =
                    (StateListDrawable) ContextCompat.getDrawable(context, R.drawable.circle_default);
            View favCircle = inflater.inflate(R.layout.fragment_drawer_filter_favorites, mFavLinearLayout, false);
            View circleButton = favCircle.findViewById(R.id.imageButton_favorite_color);
            circleButton.setBackground(circle);
            circleButton.setTag(i);

            // + Видимо activated состояние получается не сразу при инфлейтинге, по какой то причине цвет потом
            // не соответствует. Этот костыль позволяет добиться желаемого результата, но нужно поправить
            // на более элегантное решение.
            circleButton.setActivated(true);
            circleButton.setActivated(false);
            // -

            mFavLinearLayout.addView(favCircle);
        }

    }

    /** Перезагружает адаптер списка сохраненных фильтров. */
    private void reloadSavedFiltersList() {

        // Создаем AdapterView.OnItemSelectedListener для списка сохраненных фильтров
        AdapterView.OnItemSelectedListener onSavedFilterSelectedListener = new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long l) {
                // При запуске возникает onItemSelected, нужно первый отсеять
                if (!loaded) {
                    loaded = true;
                    return;
                }
                if (mCallbacks != null) {
                    mCallbacks.onSavedFilterClick(position);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) { }
        };

        Context context = getContext();

        /* Адаптер для SavedFilters. */
        ArrayAdapter<String> savedFiltersAdapter = new ArrayAdapter<>(context, android.R.layout.simple_spinner_item);
        savedFiltersAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mSavedFiltersSpinner.setAdapter(savedFiltersAdapter);

        // Устанавливаем онклик слушатель
        mSavedFiltersSpinner.setOnItemSelectedListener(onSavedFilterSelectedListener);

        if (mCallbacks != null) {
            savedFiltersAdapter.clear();
            savedFiltersAdapter.addAll(mCallbacks.getSavedFiltersList());
            savedFiltersAdapter.notifyDataSetChanged();
        }

    }

    /** Перезагружает адаптер списка фильтров по датам. */
    private void reloadDateFiltersList() {

        if (mCallbacks != null) {

            Context context = getContext();

            ArrayAdapter<String> dateFiltersAdapter =
                    new ArrayAdapter<>(context, android.R.layout.simple_spinner_item);
            dateFiltersAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            dateFiltersAdapter.addAll(mCallbacks.getDateFiltersList());
            mDateFiltersSpinner.setAdapter(dateFiltersAdapter);

            // Создаем AdapterView.OnItemSelectedListener для списка фильтров по датам
            AdapterView.OnItemSelectedListener onDateFilterSelectedListener = new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> adapterView, View view, int position, long l) {
                    if (mCallbacks != null) {
                        mCallbacks.onDateFilterClick(position);
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> adapterView) {
                }
            };

            // Устанавливаем онклик слушатель
            mDateFiltersSpinner.setOnItemSelectedListener(onDateFilterSelectedListener);

        }

    }

    @Override
    public void onDetach() {
        super.onDetach();
        mCallbacks = null;
    }


    /////////////////////////
    // Методы для перезагрузки данных в layout

    /** Обновляет Drawer в соответствии с выбранным фильтром. */
    public void onFilterChanged(MainListFilter currentFilter) {

        if (mCallbacks == null) {
            return;
        }

        mDateFiltersSpinner.setSelection(currentFilter.getSelection().getPosition(), false);

        // Даты выключены, если фильтр по датам не установлен
        mDateFromEditText.setText(currentFilter.getDateFrom());
        mDateToEditText.setText(currentFilter.getDateTo());
        switch (mDateFiltersSpinner.getSelectedItemPosition()) {
            case MainListFilter.INDEX_DATE_ALL:
                mDateFromEditText.setVisibility(View.GONE);
                mDateToEditText.setVisibility(View.GONE);
                break;
            default:
                mDateFromEditText.setVisibility(View.VISIBLE);
                mDateToEditText.setVisibility(View.VISIBLE);
                break;
        }

        mFavColors = mCallbacks.getFavoriteColors();
        updateFavLayout(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if ((v.getId() == R.id.imageButton_favorite_color) && (mCallbacks != null) && (mFavColors != null)) {
                    int color = (int) v.getTag();
                    mCallbacks.onFavoriteColorClick(mFavColors[color]);
                }
            }
        },
            currentFilter.getColorFilter());

        resetButtons();
        Button selectedSortButton = null;
        switch (currentFilter.getSortType()) {
            case MANUAL:
                selectedSortButton = mSortByManualOrderButton;
                break;
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
        selectedSortButton.setActivated(true); // NOTNULL всегда, кроме если добавят SortType и тут не поменять
        if (selectedSortButton != mSortByManualOrderButton) {
            selectedSortButton.setText(selectedSortButton.getText().toString()
                    + " " + mSortOrderSymbols.get(currentFilter.getSortOrder()));
        }

    }

    /** Обновляет mFavLinearLayout с любимыми кругами на основании mFavColors.
     * Все объекты должны быть инициализированы, mFavLinearLayout должен быть заполнен.
     *
     * @param clickListener Ссылка на OnClickListener, который устанавливается для кругов
     * @param colorFilter Фильтр цвета, если указан, то круги будут помечены .active
     */
    private void updateFavLayout(@Nullable View.OnClickListener clickListener,
                                 @Nullable Set<Integer> colorFilter) {

        if (mFavColors != null) {
            for (int i = 0; i < mFavColors.length; i++) {
                int savedColor = mFavColors[i];
                View favCircle = mFavLinearLayout.getChildAt(i).findViewById(R.id.imageButton_favorite_color);
                DrawableContainer.DrawableContainerState containerState =
                        ((DrawableContainer.DrawableContainerState) (favCircle.getBackground()).getConstantState());
                if (containerState != null) {
                    Drawable[] children = containerState.getChildren();
                    ((GradientDrawable) children[0]).setColor(savedColor);
                    ((GradientDrawable) children[1]).setColor(savedColor);
                    ((GradientDrawable) children[2]).setColor(savedColor);
                }
                if (savedColor != Color.TRANSPARENT) {
                    favCircle.setOnClickListener(clickListener);
                    favCircle.setClickable(true);
                    if (colorFilter != null) {
                        favCircle.setActivated(colorFilter.contains(savedColor));
                    }
                } else {
                    favCircle.setOnClickListener(null);
                    favCircle.setClickable(false);
                    favCircle.setActivated(false);
                }
            }
        }

    }

    /** Сбрасывает все кнопки SortBy к виду по умолчанию. */
    private void resetButtons() {
        mSortByManualOrderButton.setActivated(false);
        // set text не нужен, так как не меняется
        mSortByLabelButton.setActivated(false);
        mSortByLabelButton.setText(mSortButtonsNames.get(MainListFilter.SortType.LABEL));
        mSortByDateCreatedButton.setActivated(false);
        mSortByDateCreatedButton.setText(mSortButtonsNames.get(MainListFilter.SortType.DATE_CREATED));
        mSortByDateModifiedButton.setActivated(false);
        mSortByDateModifiedButton.setText(mSortButtonsNames.get(MainListFilter.SortType.DATE_MODIFIED));
        mSortByDateViewedButton.setActivated(false);
        mSortByDateViewedButton.setText(mSortButtonsNames.get(MainListFilter.SortType.DATE_VIEWED));
    }

    /** Устанавливает текущего пользователя в текстовое поле. */
    public void setCurrentUser(String user) {
        mCurrentUserTextView.setText(user);
    }

    /////////////////////////
    // Колбеки View.onClickListener и обработчики нажатий на различные кнопки


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
                    leftDateBorder = (long) mDateFromEditText.getTag();
                    rightDateBorder = System.currentTimeMillis();
                } else {
                    leftDateBorder = 0;
                    rightDateBorder = (long) mDateToEditText.getTag(); // Вернет 0, если не установлена
                    if (rightDateBorder <= 0) {
                        rightDateBorder = System.currentTimeMillis();
                    }
                }
                DialogUtils.openDatePickerDialog(getContext(), timeSelected, leftDateBorder, rightDateBorder,
                        new DatePickerDialog.OnDateSetListener() {
                            @Override
                            public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                                if ((mDateEditor != null) && (mCallbacks != null)) {
                                    // Получаем новый календарь и устанавливаем в нем выбранную дату
                                    Calendar calendar = Calendar.getInstance();
                                    calendar.set(year, month, dayOfMonth);

                                    // Правим время на начало (если from) или конец (если to) и правим фильтр
                                    long date;
                                    if (mDateEditor.getId() == R.id.edittext_filterdrawer_datefrom) {
                                        calendar.set(Calendar.HOUR_OF_DAY, 0);
                                        calendar.set(Calendar.MINUTE, 0);
                                        calendar.set(Calendar.SECOND, 0);
                                        date = calendar.getTimeInMillis();
                                        mCallbacks.onDateFromSet(date);
                                    } else {
                                        calendar.set(Calendar.HOUR_OF_DAY, LAST_HOUR);
                                        calendar.set(Calendar.MINUTE, LAST_MINUTE);
                                        calendar.set(Calendar.SECOND, LAST_SECOND);
                                        date = calendar.getTimeInMillis();
                                        mCallbacks.onDateToSet(date);
                                    }

                                    // Ставим выбранную дату в нужное поле и запоминаем таг, чтобы на парсить потом
                                    SimpleDateFormat sdf = new SimpleDateFormat(FORMAT_DATE, Locale.US);
                                    mDateEditor.setText(sdf.format(calendar.getTime()));
                                    mDateEditor.setTag(date);
                                }
                            }
                        });
                break;
            default:
                break;
        }
    }

    /** Устанавливает выбор текущего фильтра. */
    public void setSavedFilterSelection(int position, boolean reload) {
        if (reload) {
            reloadSavedFiltersList();
        }
        mSavedFiltersSpinner.setSelection(position);
    }

}
