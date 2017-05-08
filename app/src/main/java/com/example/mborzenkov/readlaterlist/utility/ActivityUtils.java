package com.example.mborzenkov.readlaterlist.utility;

import android.content.Context;
import android.content.DialogInterface;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.EditText;

import java.util.function.Consumer;
import java.util.function.Function;

/** Вспомогательный класс для Activity. */
public class ActivityUtils {

    private ActivityUtils() {
        throw new UnsupportedOperationException("Класс ActivityUtils - static util, не может иметь экземпляров");
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

}
