package com.example.mborzenkov.readlaterlist.data;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.support.annotation.NonNull;

import com.example.mborzenkov.readlaterlist.R;
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

    /** Матчер для сравнения запрашиваемых uri. */
    private static final UriMatcher sUriMatcher = buildUriMatcher();
    /** DbHelper для работы с базой. */
    private ReadLaterDbHelper mReadLaterDbHelper;
    /** Контекст. */
    private Context mContext;

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
        mContext = getContext();
        mReadLaterDbHelper = new ReadLaterDbHelper(mContext);
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
    public Cursor query(@NonNull Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {
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
            case CODE_READLATER_ITEMS_WITH_REMOTE_ID:
                StringBuilder newSelection = new StringBuilder(QUERY_REMOTE_ID);
                List<String> newSelectionArgs = new ArrayList<>();
                newSelectionArgs.add(uri.getPathSegments().get(2));
                if (!selection.isEmpty()) {
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
                throw new UnsupportedOperationException(mContext.getString(R.string.db_error_uriunknown) + uri);
        }

        // cursor.setNotificationUri(mContext.getContentResolver(), uri);
        return cursor;
    }

    /** {@inheritDoc}
     *
     * @throws UnsupportedOperationException если uri не соответствует разрешенным
     */
    @Override
    public int delete(@NonNull Uri uri, String selection, String[] selectionArgs) {
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
                mReadLaterDbHelper.resetDb(db);
                break;
            case CODE_READLATER_ITEMS_WITH_ID:
                String[] id = new String[] {uri.getPathSegments().get(1)};
                itemDeleted = db.delete(ReadLaterEntry.TABLE_NAME, "_id=?", id);
                db.delete(ReadLaterEntry.TABLE_NAME_FTS, "docid=?", id);
                break;
            default:
                throw new UnsupportedOperationException(mContext.getString(R.string.db_error_uriunknown) + uri);
        }

        //        if (itemDeleted != 0) {
        //             mContext.getContentResolver().notifyChange(uri, null);
        //        }

        return itemDeleted;
    }

    /** {@inheritDoc}
     *
     * @throws UnsupportedOperationException если uri не соответствует разрешенным
     */
    @Override
    public Uri insert(@NonNull Uri uri, ContentValues values) {
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
                        throw new android.database.SQLException(mContext.getString(R.string.db_error_insert) + uri);
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
                throw new UnsupportedOperationException(mContext.getString(R.string.db_error_uriunknown) + uri);
        }

        // mContext.getContentResolver().notifyChange(uri, null);

        return returnUri;
    }

    /** {@inheritDoc}
     *
     * @throws UnsupportedOperationException если uri не соответствует разрешенным
     */
    @Override
    public int bulkInsert(@NonNull Uri uri, @NonNull ContentValues[] values) {
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
                            throw new android.database.SQLException(mContext.getString(R.string.db_error_insert) + uri);
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
                throw new UnsupportedOperationException(mContext.getString(R.string.db_error_uriunknown) + uri);
        }

        // mContext.getContentResolver().notifyChange(uri, null);

        return inserted;
    }

    /** {@inheritDoc}
     *
     * @throws UnsupportedOperationException если uri не соответствует разрешенным
     */
    @Override
    public int update(@NonNull Uri uri, ContentValues values, String selection, String[] selectionArgs) {

        // Обработчик запросов update
        int itemUpdated = 0;
        SQLiteDatabase db = mReadLaterDbHelper.getWritableDatabase();
        String itemIdString = uri.getPathSegments().get(1);

        // if (null == selection) selection = "1";

        switch (sUriMatcher.match(uri)) {
            case CODE_READLATER_ITEMS_WITH_ID:
                db.beginTransaction();
                try {
                    // INSERT INTO TABLE_ITEMS
                    itemUpdated = db.update(ReadLaterEntry.TABLE_NAME, values, "_id=?", new String[] { itemIdString });

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
                } finally {
                    db.endTransaction();
                }
                break;
            case CODE_READLATER_ITEMS_UPDATE_ORDER:
                final int newPosition = Integer.valueOf(uri.getPathSegments().get(3));
                db.beginTransaction();
                try {

                    final String[] columnOrder = new String[] {ReadLaterEntry.COLUMN_ORDER};

                    // Позиция элемента itemId
                    Cursor curPosCursor = db.query(ReadLaterEntry.TABLE_NAME, columnOrder, "_id=?",
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
                                    updateOrderVal, "_id=?", new String[] { itemIdString });
                            itemUpdated = Math.abs(oldPosition - newPosition);
                        }
                    }
                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                }
                break;
            default:
                throw new UnsupportedOperationException(mContext.getString(R.string.db_error_uriunknown) + uri);
        }

        //        if (itemUpdated != 0) {
        //           mContext.getContentResolver().notifyChange(uri, null);
        //        }

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
        if (oldPosition == newPosition) {
            return "";
        }

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
