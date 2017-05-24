package com.example.mborzenkov.readlaterlist.utility;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.support.annotation.Nullable;

import com.example.mborzenkov.readlaterlist.adt.ReadLaterItem;
import com.example.mborzenkov.readlaterlist.adt.ReadLaterItemDbAdapter;
import com.example.mborzenkov.readlaterlist.adt.UserInfo;
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
    /** Запрос на заметки пользователя. */
    private static final String QUERY_USER_ID = String.format("%s = ?", ReadLaterEntry.COLUMN_USER_ID);


    private ReadLaterDbUtils() {
        throw new UnsupportedOperationException("Класс ReadLaterDbUtils - static util, не может иметь экземпляров");
    }

    /** Возвращает заметку с указанным remoteId.
     *
     * @param context контекст
     * @param userId идентификатор пользователя
     * @param remoteId внешний идентификатор заметки
     * @return заметка с указанным remoteId у указанного пользователя или null, если такой нет
     */
    public static @Nullable ReadLaterItem getItemByRemoteId(Context context, int userId, int remoteId) {
        ReadLaterItem result = null;
        Cursor queryCursor = context.getContentResolver().query(
                ReadLaterEntry.buildUriForRemoteId(remoteId),
                null,
                QUERY_USER_ID,
                new String[] { String.valueOf(userId) },
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

    /** Возвращает _id для заметки с указанным remoteId.
     *
     * @param context контекст
     * @param userId идентификатор пользователя
     * @param remoteId внешний идентификатор заметки
     * @return _id для заметки с указанным remoteId или -1, если у указанного пользователя нет заметки с указанным
     *                  remoteId
     */
    private static int getItemLocalIdByRemoteId(Context context, int userId, int remoteId) {
        Cursor queryCursor = context.getContentResolver().query(
                ReadLaterEntry.buildUriForRemoteId(remoteId),
                null,
                QUERY_USER_ID,
                new String[] { String.valueOf(userId) },
                null);

        if (queryCursor == null || queryCursor.getCount() == 0) {
            return -1;
        }

        queryCursor.moveToPosition(0);
        int localId = queryCursor.getInt(queryCursor.getColumnIndexOrThrow(ReadLaterEntry._ID));
        queryCursor.close();

        return localId;
    }

    /** Возвращает Cursor со всеми заметками указанного пользователя.
     *  После окончания работы с курсором его необходимо закрыть.
     *
     * @param context контекст
     * @param userId идентификатор пользователя
     * @return курсор со всеми заметками указанного пользователя
     */
    public static @Nullable Cursor queryAllItems(Context context, int userId) {
        return context.getContentResolver().query(
                ReadLaterEntry.CONTENT_URI,
                null,
                QUERY_USER_ID,
                new String[] { String.valueOf(userId) },
                null);
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
        contentValues.put(ReadLaterEntry.COLUMN_USER_ID, UserInfo.getCurentUser(context).getUserId());
        Uri uri = context.getContentResolver().insert(ReadLaterEntry.CONTENT_URI, contentValues);
        return uri != null;
    }

    /** Производит массовое добавление данных. При этом все даты задаются равными текущему времени.
     *
     * @param context Контекст
     * @param itemList Данные в формате ReadLaterItem.
     */
    public static void bulkInsertItems(Context context, List<ReadLaterItem> itemList) {
        final int currentUser = UserInfo.getCurentUser(context).getUserId();
        ReadLaterItemDbAdapter dbAdapter = new ReadLaterItemDbAdapter();
        ContentValues[] values = new ContentValues[itemList.size()];
        for (int i = 0; i < itemList.size(); i++) {
            values[i] = dbAdapter.contentValuesFromItem(itemList.get(i));
            values[i].put(ReadLaterEntry.COLUMN_USER_ID, currentUser);
        }
        context.getContentResolver().bulkInsert(ReadLaterEntry.CONTENT_URI, values);
    }

    /** Обновляет заметку с указанным remoteId.
     *
     * @param context контекст
     * @param item заметка с содержимым, которое нужно положить на место обновляемой заметки
     * @param userId идентификатор пользователя
     * @param remoteId внешний идентификатор заметки
     *
     * @return true - если обновление прошло успешно, иначе false
     */
    public static boolean updateItem(Context context, ReadLaterItem item, int userId, int remoteId) {
        int localId = getItemLocalIdByRemoteId(context, userId, remoteId);
        //noinspection SimplifiableIfStatement
        if (localId < 0) {
            return false;
        }
        return updateItem(context, item, localId);
    }

    /** Обновляет элемент в базе данных с uid.
     *
     * @param context Контекст
     * @param item Элемент в виде ReadLaterItem
     * @param uid _id элемента для изменения
     *
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

    /** Обновляет внешний идентификатор заметки с указанным _id.
     *
     * @param context контекст
     * @param uid внутренний идентификатор заметки
     * @param remoteId внешний идентификатор заметки
     * @return true - если обновление прошло успешно, иначе false
     */
    public static boolean updateItemRemoteId(Context context, int uid, int remoteId) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(ReadLaterEntry.COLUMN_REMOTE_ID, remoteId);
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
     *  После окончания работы с курсором его необходимо закрыть.
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
                new String[] { String.valueOf(UserInfo.getCurentUser(context).getUserId()) },
                String.format(Locale.US, QUERY_RANGE, count, from));
    }

    /** Устанавливает позицию у элемента, изменяя позиции всех промежуточных элементов.
     *
     * @param context контекст для обращения к контент провайдеру
     * @param itemLocalId внутренний идентификатор элемента
     * @param newPosition новая позиция элемента, item_order
     */
    public static void changeItemOrder(Context context, int itemLocalId, int newPosition) {
        context.getContentResolver()
                .update(ReadLaterEntry.buildUriForUpdateOrder(itemLocalId, newPosition), null, null, null);
    }

}
