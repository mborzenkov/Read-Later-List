package com.example.mborzenkov.readlaterlist.activity.main;

import android.app.DatePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.widget.DatePicker;
import android.widget.EditText;

import java.util.Calendar;

/** Вспомогательный класс для диалоговых окон. */
public class DialogUtils {

    private DialogUtils() {
        throw new UnsupportedOperationException("Класс DialogUtils - static util, не может иметь экземпляров");
    }

    public interface OnClickWithTextInput {
        /** Вызывается при нажатии на кнопку в диалоге с вводом текста.
         *
         * @param input ввод пользователя, может быть пустым
         */
        void onClick(@NonNull String input);
    }

    /** Показывает окно AlertDialog с двумя кнопками: подтверждение и отмена.
     * В зависимости от параметров, окно обладает заголовком и сообщением и выполняет действия при нажатии на кнопки.
     *
     * @param context контекст, в котором запускается AlertDialog
     * @param title заголовок окна или null, если заголовок не нужен
     * @param message сообщение в окне или null, если сообщение не нужно
     * @param positiveAction интерфейс для оповещения о нажатии на кнопку подтверждения или null, если действие не нужно
     * @param negativeAction интерфейс для оповещения о нажатии на кнопку отмены или null, если действие не нужно
     */
    public static void showAlertDialog(@NonNull  final Context context,
                                       @Nullable final String title,
                                       @Nullable final String message,
                                       @Nullable final DialogInterface.OnClickListener positiveAction,
                                       @Nullable final DialogInterface.OnClickListener negativeAction) {

        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(context);
        if (title != null) {
            dialogBuilder.setTitle(title);
            dialogBuilder.setIcon(android.R.drawable.ic_dialog_alert);
        }
        if (message != null) {
            dialogBuilder.setMessage(message);
        }
        if (positiveAction != null) {
            dialogBuilder.setPositiveButton(android.R.string.yes, positiveAction);
        }
        if (negativeAction != null) {
            dialogBuilder.setNegativeButton(android.R.string.no, negativeAction);
        }
        dialogBuilder.show();

    }

    /** Показывает окно AlertDialog с полем для ввода текста и двумя кнопками: подтверждение и отмена.
     * В зависимости от параметров, окно обладает заголовком, сообщением и выполняет действия при нажатии на кнопки.
     *
     * @param context контекст, в котором запускается AlertDialog
     * @param editText элемент EditText для ввода текста
     * @param title заголовок окна или null, если заголовок не нужен
     * @param message сообщение в окне или null, если сообщение не нужно
     * @param positiveAction интерфейс для оповещения о нажатии на кнопку подтверждения или null, если действие не нужно
     * @param negativeAction интерфейс для оповещения о нажатии на кнопку отмены или null, если действие не нужно
     */
    public static void showInputTextDialog(@NonNull  final Context context,
                                           @NonNull  final EditText editText,
                                           @Nullable final String title,
                                           @Nullable final String message,
                                           @Nullable final OnClickWithTextInput positiveAction,
                                           @Nullable final DialogInterface.OnClickListener negativeAction) {

        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(context);
        dialogBuilder.setTitle(title);
        dialogBuilder.setView(editText);
        if (title != null) {
            dialogBuilder.setTitle(title);
            dialogBuilder.setIcon(android.R.drawable.ic_dialog_alert);
        }
        if (message != null) {
            dialogBuilder.setMessage(message);
        }
        if (positiveAction != null) {
            dialogBuilder.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    positiveAction.onClick(editText.getText().toString().trim());
                }
            });
        }
        if (negativeAction != null) {
            dialogBuilder.setNegativeButton(android.R.string.no, negativeAction);
        }
        dialogBuilder.show();

    }

    /** Открывает диалог выбора даты и оповещает о выборе.
     *
     * @param context контекст, в котором нужно открыть диалог
     * @param timeSelected время по умолчанию в формате timestamp
     * @param dateFromBorder левая граница дат в формате timestamp
     * @param dateToBorder права граница дат в формате timestamp
     * @param onDateSetListener интерфейс для оповещения о выбранном времени
     */
    public static void openDatePickerDialog(@NonNull Context context,
                                     long timeSelected,
                                     long dateFromBorder,
                                     long dateToBorder,
                                     @NonNull DatePickerDialog.OnDateSetListener onDateSetListener) {

        // Открываем Dialog, установив заранее выбранную дату и границы
        Calendar calendar = Calendar.getInstance();
        if (timeSelected > 0) {
            calendar.setTimeInMillis(timeSelected);
        }

        DatePickerDialog dialog = new DatePickerDialog(
                context, // где открываем
                onDateSetListener, // что делать после выбора
                calendar.get(Calendar.YEAR), // текущее значение
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH));

        DatePicker picker = dialog.getDatePicker();

        picker.setMinDate(dateFromBorder);
        picker.setMaxDate(dateToBorder);
        dialog.show();

    }

}
