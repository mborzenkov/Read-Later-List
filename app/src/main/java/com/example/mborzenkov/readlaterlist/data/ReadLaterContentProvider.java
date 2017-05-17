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
import android.util.Log;

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

    /** Запрос для отдельного элемента по remoteId. */
    private static final String QUERY_REMOTE_ID = ReadLaterEntry.COLUMN_REMOTE_ID + "=?";

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

        if (itemDeleted != 0) {
            // mContext.getContentResolver().notifyChange(uri, null);
        }

        return itemDeleted;
    }

    @Override
    public Uri insert(@NonNull Uri uri, ContentValues values) {
        // Обработчик зарпосов insert
        Uri returnUri;

        switch (sUriMatcher.match(uri)) {
            case CODE_READLATER_ITEMS:
                SQLiteDatabase db = mReadLaterDbHelper.getWritableDatabase();
                long id = db.insert(ReadLaterEntry.TABLE_NAME, null, values);
                ContentValues ftsValues = new ContentValues();
                ftsValues.put("docid", id);
                ftsValues.put(ReadLaterEntry.COLUMN_LABEL, values.getAsString(ReadLaterEntry.COLUMN_LABEL));
                ftsValues.put(ReadLaterEntry.COLUMN_DESCRIPTION, values.getAsString(ReadLaterEntry.COLUMN_DESCRIPTION));
                db.insert(ReadLaterEntry.TABLE_NAME_FTS, null, ftsValues);
                if (id > 0) {
                    returnUri =  ContentUris.withAppendedId(ReadLaterEntry.CONTENT_URI, id);
                } else {
                    throw new android.database.SQLException(mContext.getString(R.string.db_error_insert) + uri);
                }
                break;
            default:
                throw new UnsupportedOperationException(mContext.getString(R.string.db_error_uriunknown) + uri);
        }

        // mContext.getContentResolver().notifyChange(uri, null);

        return returnUri;
    }

    @Override
    public int bulkInsert(@NonNull Uri uri, @NonNull ContentValues[] values) {
        // Обработчик зарпосов bullk insert
        int inserted = 0;
        Log.d("INSERTING", String.valueOf(values.length));

        switch (sUriMatcher.match(uri)) {
            case CODE_READLATER_ITEMS:
                SQLiteDatabase db = mReadLaterDbHelper.getWritableDatabase();
                db.beginTransaction();
                try {
                    for (ContentValues value : values) {
                        long id = db.insert(ReadLaterEntry.TABLE_NAME, null, value);
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

    @Override
    public int update(@NonNull Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        // Обработчик запросов update
        int itemUpdated;

        // if (null == selection) selection = "1";

        switch (sUriMatcher.match(uri)) {
            case CODE_READLATER_ITEMS_WITH_ID:
                String[] id = new String[] {uri.getPathSegments().get(1)};
                SQLiteDatabase db = mReadLaterDbHelper.getWritableDatabase();
                itemUpdated = db.update(ReadLaterEntry.TABLE_NAME, values, "_id=?", id);
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
                    db.update(ReadLaterEntry.TABLE_NAME_FTS, ftsValues, "docid=?", id);
                }
                break;
            default:
                throw new UnsupportedOperationException(mContext.getString(R.string.db_error_uriunknown) + uri);
        }

        if (itemUpdated != 0) {
           // mContext.getContentResolver().notifyChange(uri, null);
        }

        return itemUpdated;
    }
}
