package com.example.mborzenkov.readlaterlist.activity.main;

import android.app.Dialog;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import com.example.mborzenkov.readlaterlist.R;
import com.example.mborzenkov.readlaterlist.adt.ReadLaterItem;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainListConflictFragment extends DialogFragment {

    /** Формат поля описания текущего конфликта (remoteId). */
    private static final String FORMAT_DESCRIPTION = "ID: %s";
    /** Название кнопки, если элемент всего 1. */
    private static String BUTTON_SAVE = null;
    /** Название кнопки, если элементов несколько. */
    private static String BUTTON_NEXT = null;

    public interface ConflictsCallback {
        void onConflictsMerged();
    }

    /** Список конфликтов. */
    private @NonNull List<ReadLaterItem[]> mConflictsList = new ArrayList<>();
    /** Текущая редактируемая пара. */
    private ReadLaterItem[] mCurrentConflict;
    /** Колбек для оповещений о ходе синхронизации. */
    private @Nullable ConflictsCallback mConflictsCallback = null;

    // Элементы layout
    private RadioGroup mChosenOption;
    private TextView mConflictDescriptionTextView;
    private TextView mConflictLeftTextView;
    private TextView mConflictRightTextView;
    private Button mProceedButton;

    public static MainListConflictFragment getInstance(@NonNull List<ReadLaterItem[]> conflicts) {
        MainListConflictFragment conflictFragment = new MainListConflictFragment();
        conflictFragment.mConflictsList = conflicts;
        conflictFragment.mCurrentConflict = conflicts.get(0);
        return conflictFragment;
    }

    @Override
    public void onDestroyView() {
        Dialog dialog = getDialog();
        if (dialog != null && getRetainInstance()) {
            dialog.setDismissMessage(null);
        }
        super.onDestroyView();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View parentView = inflater.inflate(R.layout.fragment_conflict, container, false);
        mChosenOption = (RadioGroup) parentView.findViewById(R.id.rg_conflict_chosen);
        mConflictDescriptionTextView = (TextView) parentView.findViewById(R.id.tv_conflict_description);
        mConflictLeftTextView = (TextView) parentView.findViewById(R.id.tv_conflict_item_left);
        mConflictLeftTextView.setOnClickListener(this::toggleSelection);
        mConflictRightTextView = (TextView) parentView.findViewById(R.id.tv_conflict_item_right);
        mConflictRightTextView.setOnClickListener(this::toggleSelection);
        mProceedButton = (Button) parentView.findViewById(R.id.button_conflict_next);
        mProceedButton.setOnClickListener((View v) -> saveSelectedData());
        fillFragmentWithData();
        setCancelable(false);
        setRetainInstance(true);
        return parentView;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (BUTTON_NEXT == null) {
            BUTTON_SAVE = context.getString(R.string.mainlist_conflict_button_save);
            BUTTON_NEXT = context.getString(R.string.mainlist_conflict_button_next);
        }
        mConflictsCallback = (ConflictsCallback) context;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mConflictsCallback = null;
    }

    private void fillFragmentWithData() {
        // Если есть конфликты
        if (mConflictsList.isEmpty()) {
            return;
        }

        mChosenOption.check(R.id.rb_conflict_item_left);

        // Читаем левый и правый элемент
        ReadLaterItem leftItem  = mCurrentConflict[0];
        ReadLaterItem rightItem = mCurrentConflict[1];

        // Устанавливаем заголовок и кнопку
        mConflictDescriptionTextView.setText(String.format(FORMAT_DESCRIPTION, leftItem.getRemoteId()));
        final String buttonText = mConflictsList.size() == 1
                ? BUTTON_SAVE
                : BUTTON_NEXT;
        mProceedButton.setText(buttonText);

        // Устанавливаем сравнение
        mConflictLeftTextView.setText(leftItem.toString());
        mConflictRightTextView.setText(rightItem.toString());
    }

    private void saveSelectedData() {
        ReadLaterItem savingItem;
        if (mChosenOption.getCheckedRadioButtonId() == R.id.rb_conflict_item_left) {
            savingItem = mCurrentConflict[0];
            // TODO: save item
        } else {
            savingItem = mCurrentConflict[1];
            // TODO: save item
        }
        mConflictsList.remove(0);
        if (mConflictsList.isEmpty()) {
            if (mConflictsCallback != null) {
                mConflictsCallback.onConflictsMerged();
            }
        } else {
            mCurrentConflict = mConflictsList.get(0);
            fillFragmentWithData();
        }
    }

    private void toggleSelection(View view) {
        switch (view.getId()) {
            case R.id.tv_conflict_item_left:
                mChosenOption.check(R.id.rb_conflict_item_left);
                break;
            case R.id.tv_conflict_item_right:
                mChosenOption.check(R.id.rb_conflict_item_right);
                break;
            default:
                break;
        }
    }

}
