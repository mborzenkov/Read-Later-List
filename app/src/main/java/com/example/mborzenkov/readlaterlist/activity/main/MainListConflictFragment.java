package com.example.mborzenkov.readlaterlist.activity.main;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.TextView;

import com.example.mborzenkov.readlaterlist.R;
import com.example.mborzenkov.readlaterlist.adt.ReadLaterItem;

import java.util.ArrayList;
import java.util.List;

public class MainListConflictFragment extends DialogFragment {

    /** Формат поля описания текущего конфликта (remoteId). */
    private static final String FORMAT_DESCRIPTION = "ID: %s";
    /** Название кнопки, если элемент всего 1. */
    private static String BUTTON_SAVE = null;
    /** Название кнопки, если элементов несколько. */
    private static String BUTTON_NEXT = null;

    /** Возможные для выбора варианты: Левый и Правый. */
    private enum TypesOfConflicts { LEFT, RIGHT }

    public interface ConflictsCallback {
        void onConflictsMerged();
    }

    /** Список конфликтов. */
    private @NonNull List<ReadLaterItem[]> mConflictsList = new ArrayList<>();
    /** Текущая редактируемая пара. */
    private ReadLaterItem[] mCurrentConflict;
    /** Выбранный для сохранения вариант. */
    private TypesOfConflicts mChosenOption = TypesOfConflicts.LEFT;
    /** Колбек для оповещений о ходе синхронизации. */
    private @Nullable ConflictsCallback mConflictsCallback = null;

    // Элементы layout
    private TextView mConflictDescription;
    private GridLayout mConflictContent;
    private Button mButtonProceed;

    public static MainListConflictFragment getInstance(@NonNull List<ReadLaterItem[]> conflicts) {
        MainListConflictFragment conflictFragment = new MainListConflictFragment();
        conflictFragment.mConflictsList = conflicts;
        conflictFragment.mCurrentConflict = conflicts.get(0);
        return conflictFragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View parentView = inflater.inflate(R.layout.fragment_conflict, container, false);
        mConflictDescription = (TextView) parentView.findViewById(R.id.tv_conflict_description);
        mConflictContent = (GridLayout) parentView.findViewById(R.id.grid_conflict_items);
        mConflictContent.setOnClickListener(this::toggleSelection);
        mButtonProceed = (Button) parentView.findViewById(R.id.button_conflict_next);
        mButtonProceed.setOnClickListener((View v) -> saveSelectedData());
        fillFragmentWithData();
        setCancelable(false);
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
        if (mConflictsList.isEmpty()) {
            return;
        }
        mConflictDescription.setText(String.format(FORMAT_DESCRIPTION, mCurrentConflict[0].getRemoteId()));
        final String buttonText = mConflictsList.size() == 0
                ? BUTTON_SAVE
                : BUTTON_NEXT;
        mButtonProceed.setText(buttonText);
        // TODO: Заполнить mConflictContent, адаптер, tag
    }

    private void saveSelectedData() {
        // TODO: Выполнить сохранение данных
    }

    private void toggleSelection(View v) {
        if (v.getTag() != null) {
            TypesOfConflicts clickedOption = (TypesOfConflicts) v.getTag();
            if (clickedOption != mChosenOption) {
                // TODO: Выполнить selection
                switch (clickedOption) {
                    case LEFT:
                        break;
                    case RIGHT:
                        break;
                    default:
                        break;
                }
            }
        }
    }

}
