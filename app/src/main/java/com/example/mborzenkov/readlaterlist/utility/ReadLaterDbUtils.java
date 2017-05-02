package com.example.mborzenkov.readlaterlist.utility;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;

import com.example.mborzenkov.readlaterlist.R;
import com.example.mborzenkov.readlaterlist.adt.ReadLaterItem;
import com.example.mborzenkov.readlaterlist.data.ReadLaterContract.ReadLaterEntry;

import java.util.Random;

/** Класс для упрощения работы с базой данных.
 * Представляет собой набор static методов
 */
public class ReadLaterDbUtils {

    /** Количество плейсхолдеров, которое создается. */
    private static final int PLACEHOLDERS_COUNT = 100;
    /** Количество строк в description для автоматического создания. */
    private static final int DESCRIPTION_LINES = 3;

    private ReadLaterDbUtils() {
        throw new UnsupportedOperationException("Класс ReadLaterDbUtils - static util, не может иметь экземпляров");
    }

    /** Возвращает строку selection для использования в поиске по строке.
     * Строка selection это: SELECT * FROM table WHERE (вот эта часть) ORDER BY _id ASC)
     * Параметр запроса отмечен как "?"
     *
     * @return Строка selection, например "_id IN (SELECT docid FROM table_fts WHERE table_fts MATCH ?)"
     */
    public static String getSelectionForTextQuery() {
        return String.format("_id IN (SELECT docid FROM %s WHERE %s MATCH ?)",
                ReadLaterEntry.TABLE_NAME_FTS, ReadLaterEntry.TABLE_NAME_FTS);
    }

    /** Добавляет новый элемент в базу данных.
     *
     * @param context Контекст
     * @param item Элемент в виде ReadLaterItem
     * @return True, если добавление было выполнено успешно
     */
    public static boolean insertItem(Context context, ReadLaterItem item) {
        final long currentTime = System.currentTimeMillis();
        ContentValues contentValues = new ContentValues();
        contentValues.put(ReadLaterEntry.COLUMN_LABEL, item.getLabel());
        contentValues.put(ReadLaterEntry.COLUMN_DESCRIPTION, item.getDescription());
        contentValues.put(ReadLaterEntry.COLUMN_COLOR, item.getColor());
        contentValues.put(ReadLaterEntry.COLUMN_DATE_CREATED, currentTime);
        contentValues.put(ReadLaterEntry.COLUMN_DATE_LAST_MODIFIED, currentTime);
        contentValues.put(ReadLaterEntry.COLUMN_DATE_LAST_VIEW, currentTime);
        Uri uri = context.getContentResolver().insert(ReadLaterEntry.CONTENT_URI, contentValues);
        return uri != null;
    }

    /** Обновляет элемент в базе данных с uid.
     *
     * @param context Контекст
     * @param item Элемент в виде ReadLaterItem
     * @param uid _id элемента для изменения
     * @return True, если изменение было выполнено успешно
     */
    public static boolean updateItem(Context context, ReadLaterItem item, int uid) {
        final long currentTime = System.currentTimeMillis();
        ContentValues contentValues = new ContentValues();
        contentValues.put(ReadLaterEntry.COLUMN_LABEL, item.getLabel());
        contentValues.put(ReadLaterEntry.COLUMN_DESCRIPTION, item.getDescription());
        contentValues.put(ReadLaterEntry.COLUMN_COLOR, item.getColor());
        contentValues.put(ReadLaterEntry.COLUMN_DATE_LAST_MODIFIED, currentTime);
        contentValues.put(ReadLaterEntry.COLUMN_DATE_LAST_VIEW, currentTime);
        int updated = context.getContentResolver()
                .update(ReadLaterEntry.buildUriForOneItem(uid), contentValues, null, null);
        return updated > 0;
    }

    /** Обновляет дату просмотра элемента в базе данных с uid.
     *
     * @param context Контекст
     * @param uid _id элемента для изменения
     * @return True, если изменение было выполнено успешно
     */
    public static boolean updateItemViewDate(Context context, int uid) {
        final long currentTime = System.currentTimeMillis();
        ContentValues contentValues = new ContentValues();
        contentValues.put(ReadLaterEntry.COLUMN_DATE_LAST_VIEW, currentTime);
        int updated = context.getContentResolver()
                .update(ReadLaterEntry.buildUriForOneItem(uid), contentValues, null, null);
        return updated > 0;
    }

    /** Заполняет базу данных плейсхолдерами (случайными данными).
     *  Добавляет PLACEHOLDERS_COUNT штук записей с заранее определенными
     *  Label, случайными description и случайными цветами
     *
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
                description.append(text[randomizer.nextInt(textRows)]).append('\n');
            }
            // Конвертация int в HSV и обратно нужна, чтобы ColorPickerActivity красиво работал
            // (не каждый int без потерь конвертируется в HSV)
            // Это допущение используется только в этом тестовом методе, во всех остальных местах используются цвета,
            // конвертируемые в обе стороны без потерь
            float[] colorHsv = new float[3];
            Color.colorToHSV(randomizer.nextInt(), colorHsv);
            insertItem(context,
                    new ReadLaterItem(label + " " + i, description.toString().trim(), Color.HSVToColor(colorHsv)));
        }

    }

    /** Удаляет данные из базы данных (на основании предоставленного cursor).
     *
     * @param context Контекст
     * @param cursor Cursor, если указывает на все данные, то будут удалены все данные
     * @param indexColumnId Индекс колонки с _id, по которым удаляются данные
     */
    public static void deleteItemsFromDatabase(Context context, Cursor cursor, int indexColumnId) {

        // Массовое удаление умышленно не было реализован, так как нигде не используется
        // кроме этого метода, предназначенного для тестирования
        // А также с целью безопасности, отсутствие массового удаления снижает вероятность ошибочного стирания всего

        for (int i = 0; i < cursor.getCount(); i++) {
            cursor.moveToPosition(i);
            int uid = cursor.getInt(indexColumnId);
            context.getContentResolver().delete(ReadLaterEntry.buildUriForOneItem(uid), null, null);
        }
    }

}
