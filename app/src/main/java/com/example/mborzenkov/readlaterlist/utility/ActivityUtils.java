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

public class ActivityUtils {

    private ActivityUtils() {
        throw new UnsupportedOperationException("Класс ActivityUtils - static util, не может иметь экземпляров");
    }

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

    public static void showInputTextDialog(@NonNull  final Context context,
                                           @NonNull  final EditText view,
                                           @Nullable final String title,
                                           @Nullable final String message,
                                           @Nullable final Consumer<String> positiveAction,
                                           @Nullable final Runnable negativeAction) {

        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(context);
        dialogBuilder.setTitle(title);
        dialogBuilder.setView(view);
        if (title != null) {
            dialogBuilder.setTitle(title);
            dialogBuilder.setIcon(android.R.drawable.ic_dialog_alert);
        }
        if (message != null) {
            dialogBuilder.setMessage(message);
        }
        dialogBuilder.setPositiveButton(android.R.string.ok, (DialogInterface dialog, int which) -> {
            if (positiveAction != null) {
                positiveAction.accept(view.getText().toString().trim());
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
