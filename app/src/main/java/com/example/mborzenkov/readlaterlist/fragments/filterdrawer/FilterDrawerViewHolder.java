package com.example.mborzenkov.readlaterlist.fragments.filterdrawer;

import android.support.annotation.NonNull;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import com.example.mborzenkov.readlaterlist.R;

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
    @NonNull TextView mUrlChangeUser;
    @NonNull Button mBackupSaveButton;
    @NonNull Button mBackupRestoreButton;
    @NonNull Button mFillWithPlaceHoldersButton;
    @NonNull Button mDeleteAllButton;

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
    }

}
