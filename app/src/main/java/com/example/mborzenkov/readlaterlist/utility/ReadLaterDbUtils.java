package com.example.mborzenkov.readlaterlist.utility;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;

import com.example.mborzenkov.readlaterlist.ADT.ReadLaterItem;
import com.example.mborzenkov.readlaterlist.R;
import com.example.mborzenkov.readlaterlist.data.ReadLaterContract;

import java.util.Random;

/**
 * Класс для упрощения работы с базой данных
 * Представляет собой набор static методов
 */
public class ReadLaterDbUtils {

    /** Количество плейсхолдеров, которое создается */
    private static final int PLACEHOLDERS_COUNT = 100;
    /** Количество строк в description для автоматического создания */
    private static final int DESCRIPTION_LINES = 3;

    private ReadLaterDbUtils() { throw new UnsupportedOperationException("Класс ReadLaterDbUtils - static util, не может иметь экземпляров"); }

    /**
     * Добавляет новый элемент в базу данных
     * @param context Контекст
     * @param item Элемент в виде ReadLaterItem
     * @return True, если добавление было выполнено успешно
     */
    public static boolean insertItem(Context context, ReadLaterItem item) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(ReadLaterContract.ReadLaterEntry.COLUMN_LABEL, item.getLabel());
        contentValues.put(ReadLaterContract.ReadLaterEntry.COLUMN_DESCRIPTION, item.getDescription());
        contentValues.put(ReadLaterContract.ReadLaterEntry.COLUMN_COLOR, item.getColor());
        Uri uri = context.getContentResolver().insert(ReadLaterContract.ReadLaterEntry.CONTENT_URI, contentValues);
        return uri != null;
    }

    /**
     * Обновляет элемент в базе данных с uid
     * @param context Контекст
     * @param item Элемент в виде ReadLaterItem
     * @param uid _id элемента для изменения
     * @return True, если изменение было выполнено успешно
     */
    public static boolean updateItem(Context context, ReadLaterItem item, int uid) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(ReadLaterContract.ReadLaterEntry.COLUMN_LABEL, item.getLabel());
        contentValues.put(ReadLaterContract.ReadLaterEntry.COLUMN_DESCRIPTION, item.getDescription());
        contentValues.put(ReadLaterContract.ReadLaterEntry.COLUMN_COLOR, item.getColor());
        int updated = context.getContentResolver().update(ReadLaterContract.ReadLaterEntry.buildUriForOneItem(uid), contentValues, null, null);
        return updated > 0;
    }

    /**
     * Заполняет базу данных плейсхолдерами (случайными данными)
     *      Добавляет PLACEHOLDERS_COUNT штук записей с заранее определенными Label, случайными description и случайными цветами
     * @param context Контекст
     */
    public static void addPlaceholdersToDatabase(Context context) {

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
                description.append(text[randomizer.nextInt(text.length)] + "\n");
            }
            // Конвертация int в HSV и обратно нужна, чтобы ColorPicker красиво работал (не каждый int без потерь конвертируется в HSV)
            // Это допущение используется только в этом тестовом методе, во всех остальных местах используются цвета, конвертируемые в обе стороны без потерь
            float[] colorHSV = new float[3];
            Color.colorToHSV(randomizer.nextInt(), colorHSV);
            ContentValues contentValues = new ContentValues();
            contentValues.put(ReadLaterContract.ReadLaterEntry.COLUMN_LABEL, label + " " + i);
            contentValues.put(ReadLaterContract.ReadLaterEntry.COLUMN_DESCRIPTION, description.toString().trim());
            contentValues.put(ReadLaterContract.ReadLaterEntry.COLUMN_COLOR, Color.HSVToColor(colorHSV));
            Uri uri = context.getContentResolver().insert(ReadLaterContract.ReadLaterEntry.CONTENT_URI, contentValues);
        }
    }

    /**
     * Удаляет данные из базы данных (на основании предоставленного cursor)
     * @param context Контекст
     * @param cursor Cursor, если указывает на все данные, то будут удалены все данные
     * @param indexColumnId Индекс колонки с _id, по которым удаляются данные
     */
    public static void deleteItemsFromDatabase(Context context, Cursor cursor, int indexColumnId) {

        // Массовое удаление умышленно не было реализован, так как нигде не используется
        // кроме этого метода, предназначенного для тестирования
        // А также с целью безопасности, отсутствие массового удаления снижает вероятность ошибочного стирания всех данных

        for (int i = 0; i < cursor.getCount(); i++) {
            cursor.moveToPosition(i);
            int uid = cursor.getInt(indexColumnId);
            context.getContentResolver().delete(ReadLaterContract.ReadLaterEntry.buildUriForOneItem(uid), null, null);
        }
    }

}
