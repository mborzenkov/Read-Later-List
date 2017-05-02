package com.example.mborzenkov.readlaterlist.utility;

import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.support.v4.content.CursorLoader;

import com.example.mborzenkov.readlaterlist.data.MainListFilter;
import com.example.mborzenkov.readlaterlist.adt.ReadLaterItem;
import com.example.mborzenkov.readlaterlist.data.ReadLaterContract;
import com.example.mborzenkov.readlaterlist.data.ReadLaterContract.ReadLaterEntry;

import java.util.Arrays;

/** Класс для упрощения работы с базой данных.
 * Представляет собой набор static методов
 */
public class ReadLaterDbUtils {

    private ReadLaterDbUtils() {
        throw new UnsupportedOperationException("Класс ReadLaterDbUtils - static util, не может иметь экземпляров");
    }

    /** Возвращает строку selection для использования в поиске по строке.
     * Строка selection это: SELECT * FROM table WHERE (вот эта часть) ORDER BY _id ASC)
     * Параметр запроса отмечен как "?"
     *
     * @return Строка selection, например "_id IN (SELECT docid FROM table_fts WHERE table_fts MATCH ?)"
     */
    public static CursorLoader getNewCursorLoader(Context context, String[] projection,
                                                  String searchQuery, MainListFilter filter) {

        Uri itemsQueryUri = ReadLaterContract.ReadLaterEntry.CONTENT_URI;
        StringBuilder selection = new StringBuilder();
        String[] selectionArgs = new String[0];
        String sortOrder = "";
        if (filter != null) {
            sortOrder = filter.getSqlSortOrder();
            selection.append(filter.getSqlSelection());
            selectionArgs = filter.getSqlSelectionArgs();
        }
        if (!searchQuery.isEmpty()) {
            if (!selection.toString().trim().isEmpty()) {
                selection.append(" AND ");
            }
            selection.append(String.format("_id IN (SELECT docid FROM %s WHERE %s MATCH ?)",
                    ReadLaterEntry.TABLE_NAME_FTS, ReadLaterEntry.TABLE_NAME_FTS));
            selectionArgs = Arrays.copyOf(selectionArgs, selectionArgs.length + 1);
            selectionArgs[selectionArgs.length - 1] = searchQuery;
        }
        return new CursorLoader(context, itemsQueryUri, projection, selection.toString(), selectionArgs, sortOrder);
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

}
