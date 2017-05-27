package com.example.mborzenkov.readlaterlist.fragments.filterdrawer;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import com.example.mborzenkov.readlaterlist.BuildConfig;
import com.example.mborzenkov.readlaterlist.R;
import com.example.mborzenkov.readlaterlist.adt.MainListFilter;
import com.example.mborzenkov.readlaterlist.utility.ActivityUtils;
import com.example.mborzenkov.readlaterlist.utility.FavoriteColorsUtils;
import com.example.mborzenkov.readlaterlist.utility.MainListFilterUtils;
import com.example.mborzenkov.readlaterlist.utility.UserInfoUtils;

import java.util.HashMap;
import java.util.Map;

/** ViewHolder for FilderDrawerFragment. */
class FilterDrawerViewHolder {

    // Объекты Layout
    @NonNull LinearLayout mFavLinearLayout;
    @NonNull Spinner mSavedFiltersSpinner;
    @NonNull Spinner mDateFiltersSpinner;
    @NonNull EditText mDateFromEditText;
    @NonNull EditText mDateToEditText;
    @NonNull Button mSortByManualOrderButton;
    @NonNull Button mSortByLabelButton;
    @NonNull Button mSortByDateCreatedButton;
    @NonNull Button mSortByDateModifiedButton;
    @NonNull Button mSortByDateViewedButton;
    @NonNull TextView mCurrentUserTextView;
    private @NonNull TextView mUrlChangeUser;
    private @NonNull Button mBackupSaveButton;
    private @NonNull Button mBackupRestoreButton;
    private @NonNull Button mFillWithPlaceHoldersButton;
    private @NonNull Button mDeleteAllButton;
    private @NonNull TextView mDebugTextView;

    // Хэлперы
    /** Оригинальные названия кнопок сортировки. */
    private final Map<MainListFilter.SortType, String> mSortButtonsNames = new HashMap<>();
    /** Адаптер для SavedFilters. */
    private @Nullable ArrayAdapter<String> mSavedFiltersAdapter = null;

    /** Создает новый объект FilterDrawerViewHolder.
     * Заполняет все поля ссылками на элементы layout с использованием rootView.
     *
     * @param rootView корневой элемент, который содержит все объекты, указанные в этом классе
     *
     * @throws IllegalArgumentException если не удалось найти какой либо из объектов в rootView
     */
    FilterDrawerViewHolder(View rootView) {
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
    }

    /** Инициализирует все объекты layout значениями по умолчанию и устанавливает onClickListener'ы.
     *
     * @param context контекст
     * @param inflater инфлейтер для инфлейтинга любимых цветов
     * @param onChangeUserClickListener интерфейс для оповещения о смене пользователя
     * @param onDateClickListener интерфейс для оповещения о нажатии на одну из дат
     * @param onSortButtonClickListener интерфейс для оповещения о нажатии на кнопку сортировки
     * @param onActionButtonClickListener интерфейс для оповещения о нажатии на кнопку действия
     * @param onSavedFilterSelectedListener интерфейс для оповещений о выборе в выпадающем списке сохраненных фильтров
     *
     * @throws NullPointerException если какой либо из параметров null
     */
    void initializeWithDefaults(@NonNull Context context,
                                @NonNull LayoutInflater inflater,
                                @NonNull View.OnClickListener onChangeUserClickListener,
                                @NonNull View.OnClickListener onDateClickListener,
                                @NonNull View.OnClickListener onSortButtonClickListener,
                                @NonNull View.OnClickListener onActionButtonClickListener,
                                @NonNull AdapterView.OnItemSelectedListener onSavedFilterSelectedListener) {

        // Преезагружаем выпадающие списки
        reloadSavedFiltersList(context, onSavedFilterSelectedListener);
        reloadDateFiltersList(context);

        // Инициализируем поле смены пользователя
        mUrlChangeUser.setOnClickListener(onChangeUserClickListener);

        // Устанавливаем текущего пользователя
        mCurrentUserTextView.setText(String.valueOf(UserInfoUtils.getCurentUser(context).getUserId()));

        // DatePicker на полях с датами
        final long zeroLong = 0; // В таги лучше сразу записать long, чтобы потом не конвертировать
        mDateFromEditText.setOnClickListener(onDateClickListener);
        mDateFromEditText.setTag(zeroLong);
        mDateToEditText.setOnClickListener(onDateClickListener);
        mDateToEditText.setTag(zeroLong);

        // Добавляем Favorites на Drawer Layout
        FavoriteColorsUtils.inflateFavLayout(context, inflater, mFavLinearLayout);

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

    /** Сбрасывает все кнопки SortBy к виду по умолчанию. */
    void resetButtons() {
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

    /** Очищает адаптер выпадающего списка сохраненных фильтров и заново заполняет его данными.
     *
     * @param context контекст
     */
    void resetSavedFilter(Context context) {
        if (mSavedFiltersAdapter != null) {
            mSavedFiltersAdapter.clear();
            mSavedFiltersAdapter.addAll(MainListFilterUtils.getSavedFiltersList(context));
            mSavedFiltersAdapter.notifyDataSetChanged();
        }
    }

    /** Перезагружает адаптер списка сохраненных фильтров.
     *
     * @param context контекст
     * @param onSavedFilterSelectedListener интерфейс для оповещений о выборе в выпадающем списке сохраненных фильтров
     */
    private void reloadSavedFiltersList(Context context,
                                        AdapterView.OnItemSelectedListener onSavedFilterSelectedListener) {

        mSavedFiltersAdapter = new ArrayAdapter<>(context, android.R.layout.simple_spinner_item);
        mSavedFiltersAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mSavedFiltersSpinner.setAdapter(mSavedFiltersAdapter);

        // Устанавливаем онклик слушатель
        mSavedFiltersSpinner.setOnItemSelectedListener(onSavedFilterSelectedListener);

        resetSavedFilter(context);

    }

    /** Перезагружает адаптер списка фильтров по датам.
     *
     * @param context контекст
     */
    private void reloadDateFiltersList(Context context) {

        ArrayAdapter<String> dateFiltersAdapter =
                new ArrayAdapter<>(context, android.R.layout.simple_spinner_item);
        dateFiltersAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        dateFiltersAdapter.addAll(MainListFilterUtils.getsDateFiltersList(context));
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


}
