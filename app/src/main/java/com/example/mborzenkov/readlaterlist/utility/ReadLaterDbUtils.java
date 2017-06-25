package com.example.mborzenkov.readlaterlist.utility;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.example.mborzenkov.readlaterlist.adt.ReadLaterItem;
import com.example.mborzenkov.readlaterlist.adt.ReadLaterItemDbAdapter;
import com.example.mborzenkov.readlaterlist.data.ReadLaterContract;
import com.example.mborzenkov.readlaterlist.data.ReadLaterContract.ReadLaterEntry;

import java.util.List;
import java.util.Locale;

/** Класс для упрощения работы с базой данных.
 * Представляет собой набор static методов
 */
public final class ReadLaterDbUtils {

    /** Запрос на диапзаон. */
    private static final String QUERY_RANGE = "_ID LIMIT %s OFFSET %s";


    private ReadLaterDbUtils() {
        throw new UnsupportedOperationException("Класс ReadLaterDbUtils - static util, не может иметь экземпляров");
    }

    /** Возвращает заметку с указанным remoteId.
     * Если заметок с указанным remoteId больше одной, случайную из них.
     *
     * @param context контекст, не null
     * @param userId идентификатор пользователя, >= 0
     * @param remoteId внешний идентификатор заметки, >= 0
     *
     * @return заметка с указанным remoteId у указанного пользователя или null, если такой нет
     *
     * @throws NullPointerException если context == null
     * @throws UnsupportedOperationException если remoteId < 0 или userId < 0
     */
    public static @Nullable ReadLaterItem getItemByRemoteId(@NonNull Context context,
                                                            @IntRange(from = 0) int userId,
                                                            @IntRange(from = 0) int remoteId) {
        ReadLaterItem result = null;
        Cursor queryCursor = context.getContentResolver().query(
                ReadLaterEntry.buildUriForRemoteId(userId, remoteId), null, null, null, null);
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
     * @param context контекст, не null
     * @param userId идентификатор пользователя, >= 0
     * @param remoteId внешний идентификатор заметки, >= 0
     *
     * @return _id для заметки с указанным remoteId или -1, если у пользователя нет заметки с указанным remoteId
     *
     * @throws NullPointerException если context == null
     * @throws UnsupportedOperationException если remoteId < 0 или userId < 0
     */
    private static int getItemLocalIdByRemoteId(@NonNull Context context,
                                                @IntRange(from = 0) int userId,
                                                @IntRange(from = 0) int remoteId) {
        Cursor queryCursor = context.getContentResolver().query(
                ReadLaterEntry.buildUriForRemoteId(userId, remoteId), null, null, null, null);

        if (queryCursor == null) {
            return -1;
        } else if (queryCursor.getCount() == 0) {
            queryCursor.close();
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
     * @param context контекст, не null
     * @param userId идентификатор пользователя, >= 0
     *
     * @return курсор со всеми заметками указанного пользователя
     *
     * @throws NullPointerException если context == null
     * @throws UnsupportedOperationException если userId < 0
     */
    public static @Nullable Cursor queryAllItems(@NonNull Context context,
                                                 @IntRange(from = 0) int userId) {
        return context.getContentResolver().query(ReadLaterEntry.buildUriForUserItems(userId), null, null, null, null);
    }

    /** Добавляет новый элемент в базу данных.
     * Элемент добавляется с USER_ID = UserInfoUtiils.getCurrentUser().getUserId().
     *
     * @param context Контекст, не null
     * @param item Объект ReadLaterItem, не null
     *
     * @throws NullPointerException если context == null или item == null
     */
    public static void insertItem(@NonNull Context context, @NonNull ReadLaterItem item) {
        final int currentUser = UserInfoUtils.getCurentUser(context).getUserId();
        ReadLaterItemDbAdapter dbAdapter = new ReadLaterItemDbAdapter();
        context.getContentResolver().insert(
                ReadLaterEntry.buildUriForUserItems(currentUser),
                dbAdapter.contentValuesFromItem(item));
    }

    /** Производит массовое добавление данных. При этом все даты задаются равными текущему времени.
     * Данные добавляются с USER_ID = UserInfoUtiils.getCurrentUser().getUserId().
     *
     * @param context Контекст, не null
     * @param itemList Список с объектами ReadLaterItem, не null
     *
     * @throws NullPointerException если context == null или itemList == null
     */
    public static void bulkInsertItems(Context context, @NonNull List<ReadLaterItem> itemList) {
        final int currentUser = UserInfoUtils.getCurentUser(context).getUserId();
        ReadLaterItemDbAdapter dbAdapter = new ReadLaterItemDbAdapter();
        ContentValues[] values = new ContentValues[itemList.size()];
        for (int i = 0; i < itemList.size(); i++) {
            values[i] = dbAdapter.contentValuesFromItem(itemList.get(i));
        }
        context.getContentResolver().bulkInsert(ReadLaterEntry.buildUriForUserItems(currentUser), values);
    }

    /** Обновляет заметку с указанным remoteId.
     *
     * @param context контекст, не null
     * @param item заметка с содержимым, которое нужно положить на место обновляемой заметки, не null
     * @param userId идентификатор пользователя, >= 0
     * @param remoteId внешний идентификатор заметки, >= 0
     *
     * @throws NullPointerException если context == null или item == null
     * @throws UnsupportedOperationException если remoteId < 0 или userId < 0
     */
    public static void updateItem(@NonNull Context context,
                                  @NonNull ReadLaterItem item,
                                  @IntRange(from = 0) int userId,
                                  @IntRange(from = 0) int remoteId) {
        int localId = getItemLocalIdByRemoteId(context, userId, remoteId);
        //noinspection SimplifiableIfStatement
        if (localId < 0) {
            return;
        }
        updateItem(context, item, localId);
    }

    /** Обновляет элемент в базе данных с uid.
     *
     * @param context Контекст, не null
     * @param item Элемент в виде ReadLaterItem, не null
     * @param uid _id элемента для изменения, >= 0
     *
     * @throws NullPointerException если context == null или item == null
     * @throws UnsupportedOperationException если uid < 0
     */
    public static void updateItem(@NonNull Context context, @NonNull ReadLaterItem item, @IntRange(from = 0) int uid) {
        final int currentUser = UserInfoUtils.getCurentUser(context).getUserId();
        ReadLaterItemDbAdapter dbAdapter = new ReadLaterItemDbAdapter();
        ContentValues contentValues = dbAdapter.contentValuesFromItem(item);
        context.getContentResolver().update(
                ReadLaterEntry.buildUriForOneItem(currentUser, uid), contentValues, null, null);
    }

    /** Обновляет дату просмотра элемента в базе данных с uid.
     *
     * @param context Контекст, не null
     * @param uid _id элемента для изменения, >= 0
     *
     * @throws NullPointerException если context == null
     * @throws UnsupportedOperationException если uid < 0
     */
    public static void updateItemViewDate(@NonNull Context context, @IntRange(from = 0) int uid) {
        final int currentUser = UserInfoUtils.getCurentUser(context).getUserId();
        final long currentTime = System.currentTimeMillis();
        ContentValues contentValues = new ContentValues();
        contentValues.put(ReadLaterEntry.COLUMN_DATE_LAST_VIEW, currentTime);
        context.getContentResolver().update(
                ReadLaterEntry.buildUriForOneItem(currentUser, uid), contentValues, null, null);
    }

    /** Обновляет внешний идентификатор заметки с указанным _id.
     *
     * @param context контекст, не null
     * @param uid внутренний идентификатор заметки, >= 0
     * @param remoteId внешний идентификатор заметки, >= 0
     *
     * @throws NullPointerException если context == null
     * @throws UnsupportedOperationException если remoteId < 0 или uid < 0
     */
    public static void updateItemRemoteId(@NonNull Context context,
                                          @IntRange(from = 0) int uid,
                                          @IntRange(from = 0) int remoteId) {
        final int currentUser = UserInfoUtils.getCurentUser(context).getUserId();
        ContentValues contentValues = new ContentValues();
        contentValues.put(ReadLaterEntry.COLUMN_REMOTE_ID, remoteId);
        context.getContentResolver().update(
                ReadLaterEntry.buildUriForOneItem(currentUser, uid), contentValues, null, null);
    }


    /** Производит удаление объекта из базы.
     *
     * @param context Контекст, не null
     * @param uid Уникальный id объекта, >= 0
     *
     * @throws NullPointerException если context == null
     * @throws UnsupportedOperationException если uid < 0
     */
    public static void deleteItem(@NonNull Context context, @IntRange(from = 0) int uid) {
        final int currentUser = UserInfoUtils.getCurentUser(context).getUserId();
        context.getContentResolver().delete(
                ReadLaterContract.ReadLaterEntry.buildUriForOneItem(currentUser, uid), null, null);
    }

    /** Производит удаление всех данных из базы для всех пользователей.
     *
     * @param context Контекст, не null
     *
     * @throws NullPointerException если context == null
     */
    public static void deleteAll(@NonNull Context context) {
        final int currentUser = UserInfoUtils.getCurentUser(context).getUserId();
        context.getContentResolver()
                .delete(ReadLaterEntry.buildUriForUserItems(currentUser), null, null);
    }

    /** Выполяняет запрос count количества данных из базы с from позиции.
     * Данные возвращаются для текущего пользователя, UserInfoUtiils.getCurrentUser().getUserId().
     * После окончания работы с курсором его необходимо закрыть.
     *
     * @param context контекст, не null
     * @param from начало, с которого запрашивать данные, >= 0
     * @param count колчичество, >= 0
     *
     * @return курсор, указывающий на данные
     *
     * @throws NullPointerException если context == null
     */
    public static Cursor queryRange(@NonNull Context context,
                                    @IntRange(from = 0) int from,
                                    @IntRange(from = 0) int count) {
        final int currentUser = UserInfoUtils.getCurentUser(context).getUserId();
        return context.getContentResolver().query(
                ReadLaterContract.ReadLaterEntry.buildUriForUserItems(currentUser),
                null,
                null,
                null,
                String.format(Locale.US, QUERY_RANGE, count, from));
    }

    /** Устанавливает позицию у элемента, изменяя позиции всех промежуточных элементов.
     *
     * @param context контекст для обращения к контент провайдеру, не null
     * @param itemLocalId внутренний идентификатор элемента, >= 0
     * @param newPosition новая позиция элемента, item_order, >= 0
     *
     * @throws NullPointerException если context == null
     * @throws UnsupportedOperationException если itemLocalId < 0 или newPosition < 0
     * @throws IllegalArgumentException если newPosition > MAX среди заметок пользователя
     */
    public static void changeItemOrder(@NonNull Context context,
                                       @IntRange(from = 0) int itemLocalId,
                                       @IntRange(from = 0) int newPosition) {
        final int currentUser = UserInfoUtils.getCurentUser(context).getUserId();
        context.getContentResolver()
                .update(ReadLaterEntry.buildUriForUpdateOrder(currentUser, itemLocalId, newPosition), null, null, null);
    }

}
