package com.example.mborzenkov.readlaterlist.utility;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

import com.example.mborzenkov.readlaterlist.adt.ReadLaterItem;
import com.example.mborzenkov.readlaterlist.adt.ReadLaterItemDbAdapter;
import com.example.mborzenkov.readlaterlist.data.ReadLaterContract;
import com.example.mborzenkov.readlaterlist.data.ReadLaterContract.ReadLaterEntry;

import java.util.List;
import java.util.Locale;

/** Класс для упрощения работы с базой данных.
 * Представляет собой набор static методов
 */
public class ReadLaterDbUtils {

    /** Запрос на диапзаон. */
    private static final String QUERY_RANGE = "_ID LIMIT %s OFFSET %s";


    private ReadLaterDbUtils() {
        throw new UnsupportedOperationException("Класс ReadLaterDbUtils - static util, не может иметь экземпляров");
    }

    /** Добавляет новый элемент в базу данных.
     *
     * @param context Контекст
     * @param item Элемент в виде ReadLaterItem
     * @return True, если добавление было выполнено успешно
     */
    public static boolean insertItem(Context context, ReadLaterItem item) {
        ReadLaterItemDbAdapter dbAdapter = new ReadLaterItemDbAdapter();
        ContentValues contentValues = dbAdapter.contentValuesFromItem(item);
        Uri uri = context.getContentResolver().insert(ReadLaterEntry.CONTENT_URI, contentValues);
        return uri != null;
    }

    /** Производит массовое добавление данных. При этом все даты задаются равными текущему времени.
     *
     * @param context Контекст
     * @param itemList Данные в формате ReadLaterItem.
     */
    public static void bulkInsertItems(Context context, List<ReadLaterItem> itemList) {
        ReadLaterItemDbAdapter dbAdapter = new ReadLaterItemDbAdapter();
        ContentValues[] values = new ContentValues[itemList.size()];
        for (int i = 0; i < itemList.size(); i++) {
            values[i] = dbAdapter.contentValuesFromItem(itemList.get(i));
        }
        context.getContentResolver().bulkInsert(ReadLaterEntry.CONTENT_URI, values);
    }

    /** Обновляет элемент в базе данных с uid.
     *
     * @param context Контекст
     * @param item Элемент в виде ReadLaterItem
     * @param uid _id элемента для изменения
     * @return True, если изменение было выполнено успешно
     */
    public static boolean updateItem(Context context, ReadLaterItem item, int uid) {
        ReadLaterItemDbAdapter dbAdapter = new ReadLaterItemDbAdapter();
        ContentValues contentValues = dbAdapter.contentValuesFromItem(item);
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
    @SuppressWarnings("UnusedReturnValue")
    public static boolean updateItemViewDate(Context context, int uid) {
        final long currentTime = System.currentTimeMillis();
        ContentValues contentValues = new ContentValues();
        contentValues.put(ReadLaterEntry.COLUMN_DATE_LAST_VIEW, currentTime);
        int updated = context.getContentResolver()
                .update(ReadLaterEntry.buildUriForOneItem(uid), contentValues, null, null);
        return updated > 0;
    }

    /** Производит удаление объекта из базы.
     *
     * @param context Контекст
     * @param uid Уникальный id объекта
     * @return true, если удаление выполнено успешно
     */
    public static boolean deleteItem(Context context, int uid) {
        return context.getContentResolver()
                .delete(ReadLaterContract.ReadLaterEntry.buildUriForOneItem(uid), null, null) > 0;
    }

    /** Производит удаление всех данных из базы.
     *
     * @param context Контекст
     */
    public static void deleteAll(Context context) {
        context.getContentResolver()
                .delete(ReadLaterEntry.CONTENT_URI, null, null);
    }

    /** Выполяняет запрос count количества данных из базы с from позиции.
     *
     * @param context контекст
     * @param from начало, с которого запрашивать данные
     * @param count колчичество
     * @return курсор, указывающий на данные
     */
    public static Cursor queryRange(Context context, int from, int count) {
        return context.getContentResolver().query(
                ReadLaterContract.ReadLaterEntry.CONTENT_URI,
                null,
                null,
                null,
                String.format(Locale.US, QUERY_RANGE, count, from));
    }

}
