package com.example.mborzenkov.readlaterlist.utility;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.support.v4.content.CursorLoader;

import com.example.mborzenkov.readlaterlist.data.MainListFilter;
import com.example.mborzenkov.readlaterlist.adt.ReadLaterItem;
import com.example.mborzenkov.readlaterlist.data.ReadLaterContract;
import com.example.mborzenkov.readlaterlist.data.ReadLaterContract.ReadLaterEntry;
import com.example.mborzenkov.readlaterlist.data.ReadLaterDbJson;

import java.util.Arrays;
import java.util.List;

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

    private static ContentValues getContentValuesForInsert(ReadLaterItem item) {
        final long currentTime = System.currentTimeMillis();
        ContentValues contentValues = new ContentValues();
        contentValues.put(ReadLaterEntry.COLUMN_LABEL, item.getLabel());
        contentValues.put(ReadLaterEntry.COLUMN_DESCRIPTION, item.getDescription());
        contentValues.put(ReadLaterEntry.COLUMN_COLOR, item.getColor());
        contentValues.put(ReadLaterEntry.COLUMN_DATE_CREATED, currentTime);
        contentValues.put(ReadLaterEntry.COLUMN_DATE_LAST_MODIFIED, currentTime);
        contentValues.put(ReadLaterEntry.COLUMN_DATE_LAST_VIEW, currentTime);
        return contentValues;
    }

    private static ContentValues getContentValuesForInsertFromJson(ReadLaterDbJson itemJson) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(ReadLaterContract.ReadLaterEntry.COLUMN_LABEL, itemJson.getTitle());
        contentValues.put(ReadLaterContract.ReadLaterEntry.COLUMN_DESCRIPTION, itemJson.getDescription());
        contentValues.put(ReadLaterContract.ReadLaterEntry.COLUMN_COLOR, itemJson.getColor());
        contentValues.put(ReadLaterContract.ReadLaterEntry.COLUMN_DATE_CREATED, itemJson.getDateCreated());
        contentValues.put(ReadLaterContract.ReadLaterEntry.COLUMN_DATE_LAST_MODIFIED, itemJson.getDateModified());
        contentValues.put(ReadLaterContract.ReadLaterEntry.COLUMN_DATE_LAST_VIEW, itemJson.getDateViewed());
        return contentValues;
    }

    /** Добавляет новый элемент в базу данных.
     *
     * @param context Контекст
     * @param item Элемент в виде ReadLaterItem
     * @return True, если добавление было выполнено успешно
     */
    public static boolean insertItem(Context context, ReadLaterItem item) {
        Uri uri = context.getContentResolver().insert(ReadLaterEntry.CONTENT_URI, getContentValuesForInsert(item));
        return uri != null;
    }

    public static boolean bulkInsertJson(Context context, List<ReadLaterDbJson> dataJson) {
        ContentValues[] values = new ContentValues[dataJson.size()];
        for (int i = 0; i < dataJson.size(); i++) {
            values[i] = getContentValuesForInsertFromJson(dataJson.get(i));
        }
        return context.getContentResolver().bulkInsert(ReadLaterEntry.CONTENT_URI, values) > 0;
    }

    public static boolean bulkInsertItems(Context context, List<ReadLaterItem> itemList) {
        ContentValues[] values = new ContentValues[itemList.size()];
        for (int i = 0; i < itemList.size(); i++) {
            values[i] = getContentValuesForInsert(itemList.get(i));
        }
        return context.getContentResolver().bulkInsert(ReadLaterEntry.CONTENT_URI, values) > 0;
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

    public static boolean deleteItem(Context context, int uid) {
        return context.getContentResolver()
                .delete(ReadLaterContract.ReadLaterEntry.buildUriForOneItem(uid), null, null) > 0;
    }

    public static boolean deleteAll(Context context) {
        return context.getContentResolver()
                .delete(ReadLaterEntry.CONTENT_URI, null, null) > 0;
    }
}
