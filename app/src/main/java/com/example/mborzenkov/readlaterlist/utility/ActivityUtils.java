package com.example.mborzenkov.readlaterlist.utility;

import android.app.DatePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.DatePicker;
import android.widget.EditText;

import com.example.mborzenkov.readlaterlist.R;

import java.util.Calendar;

/** Вспомогательный класс для Activity. */
public class ActivityUtils {

    private ActivityUtils() {
        throw new UnsupportedOperationException("Класс ActivityUtils - static util, не может иметь экземпляров");
    }

    // Замена java.util.function.Consumer, совместимая с API 16
    public interface Consumer<T> {
        void accept(String param);
    }

    /** Показывает окно AlertDialog с двумя кнопками: подтверждение и отмена.
     * В зависимости от параметров, окно обладает заголовком и сообщением и выполняет действия при нажатии на кнопки.
     *
     * @param context контекст, в котором запускается AlertDialog
     * @param title заголовок окна или null, если заголовок не нужен
     * @param message сообщение в окне или null, если сообщение не нужно
     * @param positiveAction действие при нажатии на кнопку подтверждения или null, если действие не нужно
     * @param negativeAction действие при нажатии на кнопку отмены или null, если действие не нужно
     */
    public static void showAlertDialog(@NonNull  final Context context,
                                       @Nullable final String title,
                                       @Nullable final String message,
                                       @Nullable final Runnable positiveAction,
                                       @Nullable final Runnable negativeAction) {

        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(context);
        if (title != null) {
            dialogBuilder.setTitle(title);
            dialogBuilder.setIcon(android.R.drawable.ic_dialog_alert);
        }
        if (message != null) {
            dialogBuilder.setMessage(message);
        }
        dialogBuilder.setPositiveButton(android.R.string.yes, (DialogInterface dialog, int which) -> {
            if (positiveAction != null) {
                positiveAction.run();
            }
        });
        dialogBuilder.setNegativeButton(android.R.string.no, (DialogInterface dialog, int which) -> {
            if (negativeAction != null) {
                negativeAction.run();
            }
        });
        dialogBuilder.show();

    }

    /** Показывает окно AlertDialog с полем для ввода текста и двумя кнопками: подтверждение и отмена.
     * В зависимости от параметров, окно обладает заголовком, сообщением и выполняет действия при нажатии на кнопки.
     *
     * @param context контекст, в котором запускается AlertDialog
     * @param editText элемент EditText для ввода текста
     * @param title заголовок окна или null, если заголовок не нужен
     * @param message сообщение в окне или null, если сообщение не нужно
     * @param positiveAction действие при нажатии на кнопку подтверждения или null, если действие не нужно
     *                       в действие передается введенный пользователем текст (даже пустой) без крайних пробелов
     * @param negativeAction действие при нажатии на кнопку отмены или null, если действие не нужно
     */
    public static void showInputTextDialog(@NonNull  final Context context,
                                           @NonNull  final EditText editText,
                                           @Nullable final String title,
                                           @Nullable final String message,
                                           @Nullable final Consumer<String> positiveAction,
                                           @Nullable final Runnable negativeAction) {

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
        dialogBuilder.setPositiveButton(android.R.string.ok, (DialogInterface dialog, int which) -> {
            if (positiveAction != null) {
                positiveAction.accept(editText.getText().toString().trim());
            }
        });
        dialogBuilder.setNegativeButton(android.R.string.no, (DialogInterface dialog, int which) -> {
            if (negativeAction != null) {
                negativeAction.run();
            }
        });
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
