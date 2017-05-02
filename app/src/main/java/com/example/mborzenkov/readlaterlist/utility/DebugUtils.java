package com.example.mborzenkov.readlaterlist.utility;

import android.app.Activity;
import android.app.LoaderManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.graphics.Color;
import android.support.v7.app.AlertDialog;

import com.example.mborzenkov.readlaterlist.R;
import com.example.mborzenkov.readlaterlist.activity.MainListActivity;
import com.example.mborzenkov.readlaterlist.adt.ReadLaterItem;
import com.example.mborzenkov.readlaterlist.data.ReadLaterContract;

import java.util.Random;

public class DebugUtils {

    /** Количество плейсхолдеров, которое создается. */
    private static final int PLACEHOLDERS_COUNT = 100;
    /** Количество строк в description для автоматического создания. */
    private static final int DESCRIPTION_LINES = 3;

    private DebugUtils() {
        throw new UnsupportedOperationException("Класс DebugUtils - static util, не может иметь экземпляров");
    }

    public static void showAlertAndAddPlaceholders(final Context context, final MainListActivity activity) {

        new AlertDialog.Builder(context)
                .setTitle(context.getString(R.string.mainlist_menu_add_placeholders_question_title))
                .setMessage(context.getString(R.string.mainlist_menu_add_placeholders_question_text))
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        addPlaceholdersToDatabase(context);
                        activity.getSupportLoaderManager().restartLoader(MainListActivity.ITEM_LOADER_ID, null,
                                activity);
                    }
                })
                .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // do nothing
                    }
                })
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();

    }

    public static void showAlertAndDeleteItems(final Context context, final MainListActivity activity) {

        new AlertDialog.Builder(context)
                .setTitle(context.getString(R.string.mainlist_menu_delete_all_question_title))
                .setMessage(context.getString(R.string.mainlist_menu_delete_all_question_text))
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        deleteItemsFromDatabase(context);
                        activity.getSupportLoaderManager().restartLoader(MainListActivity.ITEM_LOADER_ID, null,
                                activity);
                    }
                })
                .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // do nothing
                    }
                })
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    /** Заполняет базу данных плейсхолдерами (случайными данными).
     *  Добавляет PLACEHOLDERS_COUNT штук записей с заранее определенными
     *  Label, случайными description и случайными цветами
     *
     * @param context Контекст
     */
    private static void addPlaceholdersToDatabase(Context context) {

        // bulkInsert умышленно не был реализован, так как нигде не используется
        // кроме этого метода, предназначенного для тестирования

        String[] text = context.getString(R.string.debug_large_text).split("\n");
        int textRows = text.length;
        String label = context.getString(R.string.mainlist_menu_add_placeholders_label);
        Random randomizer = new Random();
        for (int i = 0; i < PLACEHOLDERS_COUNT; i++) {
            // Description создается из случайных строк large_text
            StringBuilder description = new StringBuilder();
            for (int j = 0; j < DESCRIPTION_LINES; j++) {
                description.append(text[randomizer.nextInt(textRows)]).append('\n');
            }
            // Конвертация int в HSV и обратно нужна, чтобы ColorPickerActivity красиво работал
            // (не каждый int без потерь конвертируется в HSV)
            // Это допущение используется только в этом тестовом методе, во всех остальных местах используются цвета,
            // конвертируемые в обе стороны без потерь
            float[] colorHsv = new float[3];
            Color.colorToHSV(randomizer.nextInt(), colorHsv);
            ReadLaterDbUtils.insertItem(context,
                    new ReadLaterItem(label + " " + i, description.toString().trim(), Color.HSVToColor(colorHsv)));
        }

    }

    /** Удаляет данные из базы данных (на основании предоставленного cursor).
     *
     * @param context Контекст
     * @param cursor Cursor, если указывает на все данные, то будут удалены все данные
     * @param indexColumnId Индекс колонки с _id, по которым удаляются данные
     */
    private static void deleteItemsFromDatabase(Context context) {

        // Массовое удаление умышленно не было реализован, так как нигде не используется
        // кроме этого метода, предназначенного для тестирования
        // А также с целью безопасности, отсутствие массового удаления снижает вероятность ошибочного стирания всего

        ContentResolver contentResolver = context.getContentResolver();
        Cursor allData = contentResolver.query(ReadLaterContract.ReadLaterEntry.CONTENT_URI, new String[] {"_id"}, null, null, null);

        for (int i = 0; i < allData.getCount(); i++) {
            allData.moveToPosition(i);
            int uid = allData.getInt(0);
            context.getContentResolver().delete(ReadLaterContract.ReadLaterEntry.buildUriForOneItem(uid), null, null);
        }
    }


}
