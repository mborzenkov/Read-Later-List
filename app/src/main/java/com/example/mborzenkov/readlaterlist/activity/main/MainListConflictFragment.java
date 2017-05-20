package com.example.mborzenkov.readlaterlist.activity.main;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.example.mborzenkov.readlaterlist.BuildConfig;
import com.example.mborzenkov.readlaterlist.R;
import com.example.mborzenkov.readlaterlist.adt.ReadLaterItem;

import java.util.ArrayList;
import java.util.List;

/** Фрагмент для обработки конфликтов.
 *  Новый экземпляр обязательно должен создаваться через getInstance.
 */
public class MainListConflictFragment extends DialogFragment {

    /** Формат поля описания текущего конфликта (remoteId). */
    private static final String FORMAT_DESCRIPTION = "ID: %s";
    /** Название кнопки, если элемент всего 1. */
    private static String BUTTON_SAVE = null;
    /** Название кнопки, если элементов несколько. */
    private static String BUTTON_NEXT = null;

    /** Коллбэк для оповещения о результатах обработки конфликтов. */
    interface ConflictsCallback {

        /** Вызывается, когда пользователь выбрал вариант, который нужно сохранить.
         *
         * @param item вариант для сохранения, содержит remoteId
         */
        void saveConflict(@NonNull ReadLaterItem item);

        /** Вызывается, когда пользователь разобрал все конфликты. */
        void onConflictsMerged();
    }

    // TODO: issue #29
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

    /** Создает новый instance MainListConflictFragment.
     *
     * @param conflicts список конфликтов, не null и все элементы не null
     * @return всегда новый объект MainListConflictFragment
     * @throws NullPointerException если conflicts - null или любой из элементов null
     */
    public static MainListConflictFragment getInstance(@NonNull List<ReadLaterItem[]> conflicts) {
        MainListConflictFragment conflictFragment = new MainListConflictFragment();
        conflictFragment.mConflictsList = conflicts;
        conflictFragment.mCurrentConflict = conflicts.get(0);
        return conflictFragment;
    }

    @Override
    public void onDestroyView() {
        // Предотвращает закрытие DialogFragment при повороте экрана
        Dialog dialog = getDialog();
        if (dialog != null && getRetainInstance()) {
            dialog.setDismissMessage(null);
        }
        super.onDestroyView();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        // Ленивая инициализация строк из ресурсов и спасает от лишней переменной с контекстом.
        if (BUTTON_NEXT == null) {
            BUTTON_SAVE = context.getString(R.string.mainlist_conflict_button_save);
            BUTTON_NEXT = context.getString(R.string.mainlist_conflict_button_next);
        }
        mConflictsCallback = (ConflictsCallback) context;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @NonNull ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        // Инфлейтим все элементы layout
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

        // Нельзя закрыть
        setCancelable(false);
        // Восстанавливает себя после поворота экрана
        setRetainInstance(true);

        return parentView;

    }

    @Override
    public void onDetach() {
        super.onDetach();
        mConflictsCallback = null;
    }

    /** Заполняет фрагмент данными из mCurrentConflict. */
    private void fillFragmentWithData() {
        // Если есть конфликты
        if (mConflictsList.isEmpty()) {
            this.dismiss();
            return;
        }

        mChosenOption.check(R.id.rb_conflict_item_left);

        // Читаем левый и правый элемент
        ReadLaterItem leftItem  = mCurrentConflict[0];

        // Устанавливаем заголовок и кнопку
        mConflictDescriptionTextView.setText(String.format(FORMAT_DESCRIPTION, leftItem.getRemoteId()));
        final String buttonText = mConflictsList.size() == 1
                ? BUTTON_SAVE
                : BUTTON_NEXT;
        mProceedButton.setText(buttonText);


        ReadLaterItem rightItem = mCurrentConflict[1];
        // Устанавливаем сравнение
        mConflictLeftTextView.setText(leftItem.toString());
        mConflictRightTextView.setText(rightItem.toString());
    }

    /** Сохраняет выбранный вариант. */
    private void saveSelectedData() {
        if (mConflictsCallback != null) {
            ReadLaterItem savingItem;
            if (mChosenOption.getCheckedRadioButtonId() == R.id.rb_conflict_item_left) {
                savingItem = mCurrentConflict[0];
            } else {
                savingItem = mCurrentConflict[1];
            }
            mConflictsCallback.saveConflict(savingItem);
            mConflictsList.remove(0);
            if (mConflictsList.isEmpty()) {
                mConflictsCallback.onConflictsMerged();
                this.dismiss();
            } else {
                mCurrentConflict = mConflictsList.get(0);
                fillFragmentWithData();
            }
        } else {
            this.dismiss();
        }
    }

    /** Выбирает новый вариант.
     *
     * @param view вариант, на который кликнули: R.id.tv_conflict_item_(left или right)
     */
    private void toggleSelection(@NonNull View view) {
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
