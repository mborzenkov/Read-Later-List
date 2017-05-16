package com.example.mborzenkov.readlaterlist.utility;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.support.annotation.Nullable;
import android.util.Log;

import com.example.mborzenkov.readlaterlist.activity.main.MainListActivity;
import com.example.mborzenkov.readlaterlist.adt.ReadLaterItem;
import com.example.mborzenkov.readlaterlist.adt.ReadLaterItemDbAdapter;
import com.example.mborzenkov.readlaterlist.adt.UserInfo;
import com.example.mborzenkov.readlaterlist.data.ReadLaterContract;
import com.example.mborzenkov.readlaterlist.data.ReadLaterContract.ReadLaterEntry;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/** Класс для упрощения работы с базой данных.
 * Представляет собой набор static методов
 */
public class ReadLaterDbUtils {

    /** Запрос на диапзаон. */
    private static final String QUERY_RANGE = "_ID LIMIT %s OFFSET %s";
    /** Запрос на отдельную заметку по remoteId. */
    private static final String QUERY_REMOTE_ID = String.format("%s = ? AND %s = ?",
            ReadLaterEntry.COLUMN_USER_ID, ReadLaterEntry.COLUMN_REMOTE_ID);
    /** Запрос на заметки пользователя. */
    private static final String QUERY_USER_ID = String.format("%s = ?", ReadLaterEntry.COLUMN_USER_ID);


    private ReadLaterDbUtils() {
        throw new UnsupportedOperationException("Класс ReadLaterDbUtils - static util, не может иметь экземпляров");
    }

    public static @Nullable ReadLaterItem getItemByRemoteId(Context context, int userId, int remoteId) {
        ReadLaterItem result = null;
        Cursor queryCursor = context.getContentResolver().query(
                ReadLaterEntry.CONTENT_URI,
                null,
                QUERY_REMOTE_ID,
                new String[] { String.valueOf(userId), String.valueOf(remoteId) },
                null);
        if (queryCursor != null) {
            if (queryCursor.getCount() > 0) {
                queryCursor.moveToPosition(0);
                ReadLaterItemDbAdapter dbAdapter = new ReadLaterItemDbAdapter();
                result = dbAdapter.itemFromCursor(queryCursor);
            }
            queryCursor.close();
        }
        return result;
    }

    public static List<ReadLaterItem> getAllItems(Context context, int userId) {
        List<ReadLaterItem> result = new ArrayList<>();
        StringBuilder selectionBuilder = new StringBuilder(QUERY_USER_ID);
        List<String> selectionArgs = new ArrayList<>();
        selectionArgs.add(String.valueOf(userId));

        Cursor queryCursor = context.getContentResolver().query(
                ReadLaterEntry.CONTENT_URI,
                null,
                selectionBuilder.toString(),
                selectionArgs.toArray(new String[selectionArgs.size()]),
                null);

        if (queryCursor != null) {
            if (queryCursor.getCount() > 0) {
                ReadLaterItemDbAdapter dbAdapter = new ReadLaterItemDbAdapter();
                result = dbAdapter.allItemsFromCursor(queryCursor);
            }
            queryCursor.close();
        }
        return result;
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
        contentValues.put(ReadLaterEntry.COLUMN_USER_ID, UserInfo.getCurentUser().getUserId());
        Uri uri = context.getContentResolver().insert(ReadLaterEntry.CONTENT_URI, contentValues);
        return uri != null;
    }

    /** Производит массовое добавление данных. При этом все даты задаются равными текущему времени.
     *
     * @param context Контекст
     * @param itemList Данные в формате ReadLaterItem.
     */
    public static void bulkInsertItems(Context context, List<ReadLaterItem> itemList) {
        final int currentUser = UserInfo.getCurentUser().getUserId();
        ReadLaterItemDbAdapter dbAdapter = new ReadLaterItemDbAdapter();
        ContentValues[] values = new ContentValues[itemList.size()];
        for (int i = 0; i < itemList.size(); i++) {
            values[i] = dbAdapter.contentValuesFromItem(itemList.get(i));
            values[i].put(ReadLaterEntry.COLUMN_USER_ID, currentUser);
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
                QUERY_USER_ID,
                new String[] { String.valueOf(UserInfo.getCurentUser().getUserId()) },
                String.format(Locale.US, QUERY_RANGE, count, from));
    }

}
