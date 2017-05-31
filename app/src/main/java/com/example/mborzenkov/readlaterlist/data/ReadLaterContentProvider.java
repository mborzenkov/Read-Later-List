package com.example.mborzenkov.readlaterlist.data;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.example.mborzenkov.readlaterlist.data.ReadLaterContract.ReadLaterEntry;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Контент провайдер для работы с базой данных. */
public class ReadLaterContentProvider extends ContentProvider {

    /** Код запроса всех данных. */
    private static final int CODE_READLATER_ITEMS = 100;
    /** Код запроса отдельного элемента. */
    private static final int CODE_READLATER_ITEMS_WITH_ID = 101;
    /** Код запроса отдельного элемента по remoteId. */
    private static final int CODE_READLATER_ITEMS_WITH_REMOTE_ID = 102;
    /** Код запроса обновления порядка у элемента. */
    private static final int CODE_READLATER_ITEMS_UPDATE_ORDER = 103;

    /** Запрос для отдельного элемента по remoteId. */
    private static final String QUERY_REMOTE_ID = ReadLaterEntry.COLUMN_REMOTE_ID + "=?";
    /** Запрос для максимального значения порядка. */
    private static final String QUERY_MAX_ORDER_POSITION =
            "SELECT MAX(" + ReadLaterEntry.COLUMN_ORDER + ") FROM " + ReadLaterEntry.TABLE_NAME;
    /** Запрос для отдельного элемента по id. */
    private static final String QUERY_ID = "_id=?";

    /** Матчер для сравнения запрашиваемых uri. */
    private static final UriMatcher sUriMatcher = buildUriMatcher();
    /** DbHelper для работы с базой. */
    private ReadLaterDbHelper mReadLaterDbHelper;

    /** Создает новый UriMatcher.
     *
     * @return Матчер для сравнения запрашиваемых uri
     */
    private static UriMatcher buildUriMatcher() {

        final UriMatcher matcher = new UriMatcher(UriMatcher.NO_MATCH);
        final String authority = ReadLaterContract.CONTENT_AUTHORITY;
        // Uri для доступа ко всем данным
        matcher.addURI(authority, ReadLaterContract.PATH_ITEMS, CODE_READLATER_ITEMS);
        // Uri для доступа к отдельному элементу
        matcher.addURI(authority, ReadLaterContract.PATH_ITEMS + "/#",
                CODE_READLATER_ITEMS_WITH_ID);
        matcher.addURI(authority, ReadLaterContract.PATH_ITEMS + "/" + ReadLaterContract.PATH_NOTE + "/#",
                CODE_READLATER_ITEMS_WITH_REMOTE_ID);
        matcher.addURI(authority, ReadLaterContract.PATH_ITEMS + "/#/" + ReadLaterContract.PATH_ORDER + "/#",
                CODE_READLATER_ITEMS_UPDATE_ORDER);

        return matcher;
    }

    @Override
    public boolean onCreate() {
        // Создание DbHelper
        mReadLaterDbHelper = new ReadLaterDbHelper(getContext());
        return true;
    }

    @Override
    public String getType(@NonNull Uri uri) {
        // Не нужен в этой версии
        return null;
    }

    /** {@inheritDoc}
     *
     * @throws UnsupportedOperationException если uri не соответствует разрешенным
     */
    @Override
    public Cursor query(@NonNull Uri uri, @Nullable String[] projection, @Nullable String selection,
                        @Nullable String[] selectionArgs, @Nullable String sortOrder) {
        // Обработчик запросов select
        Cursor cursor;
        switch (sUriMatcher.match(uri)) {
            case CODE_READLATER_ITEMS:
                cursor = mReadLaterDbHelper.getReadableDatabase().query(
                        ReadLaterEntry.TABLE_NAME,
                        projection,
                        selection,
                        selectionArgs,
                        null,
                        null,
                        sortOrder);
                break;
            case CODE_READLATER_ITEMS_WITH_ID:
                String[] id = new String[] {uri.getPathSegments().get(1)};
                cursor = mReadLaterDbHelper.getReadableDatabase().query(
                        ReadLaterEntry.TABLE_NAME,
                        projection,
                        QUERY_ID,
                        id,
                        null,
                        null,
                        null);
                break;
            case CODE_READLATER_ITEMS_WITH_REMOTE_ID:
                StringBuilder newSelection = new StringBuilder(QUERY_REMOTE_ID);
                List<String> newSelectionArgs = new ArrayList<>();
                newSelectionArgs.add(uri.getPathSegments().get(2));
                if ((selection != null) && (selectionArgs != null) && (!selection.isEmpty())) {
                    newSelection.append(" AND ").append(selection);
                    newSelectionArgs.addAll(Arrays.asList(selectionArgs));
                }
                cursor = mReadLaterDbHelper.getReadableDatabase().query(
                        ReadLaterEntry.TABLE_NAME,
                        projection,
                        newSelection.toString(),
                        newSelectionArgs.toArray(new String[newSelectionArgs.size()]),
                        null,
                        null,
                        sortOrder);
                break;
            default:
                throw new UnsupportedOperationException("Error @ ReadLaterContentProvider.query: unknown uri " + uri);
        }

        return cursor;
    }

    /** {@inheritDoc}
     *
     * @throws UnsupportedOperationException если uri не соответствует разрешенным
     */
    @Override
    public int delete(@NonNull Uri uri, @Nullable String selection, @Nullable String[] selectionArgs) {
        // Обработчик запросов delete
        int itemDeleted;
        SQLiteDatabase db = mReadLaterDbHelper.getWritableDatabase();

        // if (null == selection) selection = "1";

        switch (sUriMatcher.match(uri)) {
            case CODE_READLATER_ITEMS:
                itemDeleted = db.delete(ReadLaterEntry.TABLE_NAME, null, null);
                db.delete(ReadLaterEntry.TABLE_NAME_FTS, null, null);

                /* Возможно, пересбор таблиц - не лучшее решение, но по какой-то причине ни вызов VACUUM, ни
                 * PRAGMA auto_vacuum = FULL не уменьшают размер базы данных. Это приводит к тому, что добавление
                 * тысяч строк несколько раз превращает базу в 2Гб и последующим ошибкам окончания доступной памяти.
                 * Сброс таблиц и создание их заново решает эту проблему, плюс работает быстро.
                 */
                // mReadLaterDbHelper.resetDb(db);
                /* Пересбор таблиц действительно было не лучшим решением, так как при быстром тестировании частые
                 * уничтожения таблиц приводят вообще к непредвиденным результатам. Все крашится.
                 * Рост базы в таком случае уже не так страшен.
                 */
                break;
            case CODE_READLATER_ITEMS_WITH_ID:
                String[] id = new String[] {uri.getPathSegments().get(1)};
                itemDeleted = db.delete(ReadLaterEntry.TABLE_NAME, QUERY_ID, id);
                db.delete(ReadLaterEntry.TABLE_NAME_FTS, "docid=?", id);
                break;
            default:
                throw new UnsupportedOperationException("Error @ ReadLaterContentProvider.delete: unknown uri " + uri);
        }

        return itemDeleted;
    }

    /** {@inheritDoc}
     *
     * @throws UnsupportedOperationException если uri не соответствует разрешенным
     * @throws IllegalArgumentException если values == null или не содержит всех необходимых данных
     */
    @Override
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues values) {

        if (values == null) {
            throw new IllegalArgumentException("Error @ ReadLaterContentProvider.insert: ContentValues == null");
        }

        // Обработчик зарпосов insert
        Uri returnUri;

        switch (sUriMatcher.match(uri)) {
            case CODE_READLATER_ITEMS:
                SQLiteDatabase db = mReadLaterDbHelper.getWritableDatabase();

                // SELECT MAX ORDER
                Cursor maxOrderCursor = db.rawQuery(QUERY_MAX_ORDER_POSITION, null);
                maxOrderCursor.moveToFirst();
                int maxOrder = maxOrderCursor.getInt(0);
                maxOrderCursor.close();
                values.put(ReadLaterEntry.COLUMN_ORDER, maxOrder + 1);

                db.beginTransaction();
                try {
                    // INSERT INTO TABLE_ITEMS
                    long id = db.insert(ReadLaterEntry.TABLE_NAME, null, values);
                    if (id > 0) {
                        returnUri =  ContentUris.withAppendedId(ReadLaterEntry.CONTENT_URI, id);
                    } else {
                        throw new IllegalArgumentException("Error @ ReadLaterContentProvider.insert: when inserting "
                                + values.toString());
                    }

                    // INSERT INTO TABLE_FTS
                    ContentValues ftsValues = new ContentValues();
                    ftsValues.put("docid", id);
                    ftsValues.put(ReadLaterEntry.COLUMN_LABEL, values.getAsString(ReadLaterEntry.COLUMN_LABEL));
                    ftsValues.put(ReadLaterEntry.COLUMN_DESCRIPTION,
                            values.getAsString(ReadLaterEntry.COLUMN_DESCRIPTION));
                    db.insert(ReadLaterEntry.TABLE_NAME_FTS, null, ftsValues);

                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                }
                break;
            default:
                throw new UnsupportedOperationException("Error @ ReadLaterContentProvider.insert: unknown uri " + uri);
        }

        return returnUri;
    }

    /** {@inheritDoc}
     *
     * @throws UnsupportedOperationException если uri не соответствует разрешенным
     * @throws IllegalArgumentException если values == null
     */
    @Override
    public int bulkInsert(@NonNull Uri uri, @Nullable ContentValues[] values) {

        if (values == null) {
            throw new IllegalArgumentException("Error @ ReadLaterContentProvider.bulkInsert: ContentValues == null");
        }

        // Обработчик зарпосов bullk insert
        int inserted = 0;

        switch (sUriMatcher.match(uri)) {
            case CODE_READLATER_ITEMS:
                SQLiteDatabase db = mReadLaterDbHelper.getWritableDatabase();

                // SELECT MAX ORDER
                Cursor maxOrderCursor = db.rawQuery(QUERY_MAX_ORDER_POSITION, null);
                maxOrderCursor.moveToFirst();
                int maxOrder = maxOrderCursor.getInt(0);
                maxOrderCursor.close();

                db.beginTransaction();
                try {
                    for (ContentValues value : values) {
                        maxOrder++;

                        // INSERT INTO TABLE_ITEMS
                        value.put(ReadLaterEntry.COLUMN_ORDER, maxOrder);
                        long id = db.insert(ReadLaterEntry.TABLE_NAME, null, value);
                        if (id < 0) {
                            throw new IllegalArgumentException("Error @ ReadLaterContentProvider.bulkInsert: inserting "
                                    + value.toString());
                        }

                        // INSERT INTO TABLE_FTS
                        ContentValues ftsValues = new ContentValues();
                        ftsValues.put("docid", id);
                        ftsValues.put(ReadLaterEntry.COLUMN_LABEL, value.getAsString(ReadLaterEntry.COLUMN_LABEL));
                        ftsValues.put(ReadLaterEntry.COLUMN_DESCRIPTION,
                                value.getAsString(ReadLaterEntry.COLUMN_DESCRIPTION));
                        db.insert(ReadLaterEntry.TABLE_NAME_FTS, null, ftsValues);
                        inserted++;
                    }
                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                }
                break;
            default:
                throw new UnsupportedOperationException("Error @ ReadLaterContentProvider.bulkInsert: unknown uri "
                        + uri);
        }

        return inserted;
    }

    /** {@inheritDoc}
     *
     * @throws UnsupportedOperationException если uri не соответствует разрешенным
     * @throws UnsupportedOperationException если uri == CODE_READLATER_ITEMS_UPDATE_ORDER и позиция < -1
     * @throws IllegalArgumentException если uri != CODE_READLATER_ITEMS_UPDATE_ORDER и values == null
     * @throws IllegalArgumentException если values не соответствуют контракту
     * @throws IllegalArgumentException если uri == CODE_READLATER_ITEMS_UPDATE_ORDER и новая позиция > максимальной
     */
    @Override
    public int update(@NonNull Uri uri, @Nullable ContentValues values,
                      @Nullable String selection, @Nullable String[] selectionArgs) {

        // Обработчик запросов update
        int itemUpdated = 0;
        SQLiteDatabase db = mReadLaterDbHelper.getWritableDatabase();
        String itemIdString;

        switch (sUriMatcher.match(uri)) {
            case CODE_READLATER_ITEMS_WITH_ID:

                if (values == null) {
                    throw new IllegalArgumentException(
                            "Error @ ReadLaterContentProvider.bulkInsert: ContentValues == null");
                }

                itemIdString = uri.getPathSegments().get(1);
                db.beginTransaction();
                try {
                    // INSERT INTO TABLE_ITEMS
                    itemUpdated = db.update(ReadLaterEntry.TABLE_NAME, values, QUERY_ID, new String[] { itemIdString });

                    // INSERT INTO TABLE_FTS
                    boolean updateFts = false;
                    ContentValues ftsValues = new ContentValues();
                    if (values.containsKey(ReadLaterEntry.COLUMN_LABEL)) {
                        ftsValues.put(ReadLaterEntry.COLUMN_LABEL,
                                values.getAsString(ReadLaterEntry.COLUMN_LABEL));
                        updateFts = true;
                    }
                    if (values.containsKey(ReadLaterEntry.COLUMN_DESCRIPTION)) {
                        ftsValues.put(ReadLaterEntry.COLUMN_DESCRIPTION,
                                values.getAsString(ReadLaterEntry.COLUMN_DESCRIPTION));
                        updateFts = true;
                    }
                    if (updateFts) {
                        db.update(ReadLaterEntry.TABLE_NAME_FTS, ftsValues, "docid=?", new String[] { itemIdString });
                    }

                    db.setTransactionSuccessful();
                } catch (SQLiteConstraintException e) {
                    throw new IllegalArgumentException("Error @ ReadLaterContentProvider.update: when inserting "
                        + values.toString());
                } finally {
                    db.endTransaction();
                }
                break;
            case CODE_READLATER_ITEMS_UPDATE_ORDER:
                itemIdString = uri.getPathSegments().get(1);
                final int newPosition = Integer.valueOf(uri.getPathSegments().get(3));

                // SELECT MAX ORDER
                Cursor maxOrderCursor = db.rawQuery(QUERY_MAX_ORDER_POSITION, null);
                maxOrderCursor.moveToFirst();
                int maxOrder = maxOrderCursor.getInt(0);
                maxOrderCursor.close();

                if (newPosition > maxOrder) {
                    throw new IllegalArgumentException("Error @ ReadLaterContentProvider.update: position > MAX :: "
                            + newPosition + ">" + maxOrder);
                }

                db.beginTransaction();
                try {

                    final String[] columnOrder = new String[] {ReadLaterEntry.COLUMN_ORDER};

                    // Позиция элемента itemId
                    Cursor curPosCursor = db.query(ReadLaterEntry.TABLE_NAME, columnOrder, QUERY_ID,
                            new String[] { itemIdString }, null, null, null);
                    if (curPosCursor.moveToFirst()) {
                        final int oldPosition = curPosCursor.getInt(0);
                        curPosCursor.close();

                        // Если позиция изменилась, UPDATE
                        if (oldPosition != newPosition) {
                            db.execSQL(
                                    getRawQueryForUpdateOrder(oldPosition, newPosition));
                            ContentValues updateOrderVal = new ContentValues();
                            updateOrderVal.put(ReadLaterEntry.COLUMN_ORDER, newPosition);
                            db.update(ReadLaterEntry.TABLE_NAME,
                                    updateOrderVal, QUERY_ID, new String[] { itemIdString });
                            itemUpdated = Math.abs(oldPosition - newPosition) + 1;
                        }
                    }
                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                }
                break;
            default:
                throw new UnsupportedOperationException("Error @ ReadLaterContentProvider.update: unknown uri " + uri);
        }

        return itemUpdated;
    }

    /** Возвращает запрос на изменение порядка элемента.
     * Пример:
     *      getRawQueryForUpdateOrder(4, 1)
     *      UPDATE items SET item_order = item_order + 1 WHERE item_order >= 1 AND item_order < 4;
     * Если oldPosition == newPosition, возвращает пустую строку
     *
     * @param oldPosition старая позиция элемента
     * @param newPosition новая позиция элемента
     */
    private @NonNull String getRawQueryForUpdateOrder(int oldPosition, int newPosition) {

        StringBuilder builder = new StringBuilder();
        builder.append("UPDATE ").append(ReadLaterEntry.TABLE_NAME)
                .append(" SET ").append(ReadLaterEntry.COLUMN_ORDER).append('=').append(ReadLaterEntry.COLUMN_ORDER);
        if (oldPosition < newPosition) {
            builder.append("-1");
        } else {
            builder.append("+1");
        }
        builder.append(" WHERE ").append(ReadLaterEntry.COLUMN_ORDER);
        if (oldPosition < newPosition) {
            builder.append('>').append(oldPosition);
        } else {
            builder.append(">=").append(newPosition);
        }
        builder.append(" AND ").append(ReadLaterEntry.COLUMN_ORDER);
        if (oldPosition < newPosition) {
            builder.append("<=").append(newPosition);
        } else {
            builder.append('<').append(oldPosition);
        }
        return builder.toString();

    }

}
