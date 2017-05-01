package com.example.mborzenkov.readlaterlist.data;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.net.Uri;
import android.support.annotation.NonNull;

import com.example.mborzenkov.readlaterlist.R;

/**
 * Контент провайдер для работы с базой данных
 */
public class ReadLaterContentProvider extends ContentProvider {

    /** Код запроса всех данных */
    private static final int CODE_READLATER_ITEMS = 100;
    /** Код запроса отдельного элемента */
    private static final int CODE_READLATER_ITEMS_WITH_ID = 101;

    /** Матчер для сравнения запрашиваемых uri */
    private static final UriMatcher sUriMatcher = buildUriMatcher();
    /** DbHelper для работы с базой */
    private ReadLaterDbHelper mReadLaterDbHelper;

    /**
     * Создает новый UriMatcher
     * @return Матчер для сравнения запрашиваемых uri
     */
    public static UriMatcher buildUriMatcher() {

        final UriMatcher matcher = new UriMatcher(UriMatcher.NO_MATCH);
        final String authority = ReadLaterContract.CONTENT_AUTHORITY;
        // Uri для доступа ко всем данным
        matcher.addURI(authority, ReadLaterContract.PATH_ITEMS, CODE_READLATER_ITEMS);
        // Uri для доступа к отдельному элементу
        matcher.addURI(authority, ReadLaterContract.PATH_ITEMS + "/#", CODE_READLATER_ITEMS_WITH_ID);

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

    @Override
    public Cursor query(@NonNull Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {
        // Обработчик запросов select
        Cursor cursor;
        switch (sUriMatcher.match(uri)) {
            case CODE_READLATER_ITEMS:
                cursor = mReadLaterDbHelper.getReadableDatabase().query(
                        ReadLaterContract.ReadLaterEntry.TABLE_NAME,
                        projection,
                        selection,
                        selectionArgs,
                        null,
                        null,
                        sortOrder);
                break;
            default:
                throw new UnsupportedOperationException(getContext().getString(R.string.db_error_uriunknown) + uri);
        }

        cursor.setNotificationUri(getContext().getContentResolver(), uri);
        return cursor;
    }

    @Override
    public int delete(@NonNull Uri uri, String selection, String[] selectionArgs) {
        // Обработчик запросов delete
        int itemDeleted;

        if (null == selection) selection = "1";

        switch (sUriMatcher.match(uri)) {
            case CODE_READLATER_ITEMS_WITH_ID:
                String id = uri.getPathSegments().get(1);
                itemDeleted = mReadLaterDbHelper.getWritableDatabase().delete(
                        ReadLaterContract.ReadLaterEntry.TABLE_NAME,
                        "_id=?",
                        new String[] {id});

                break;
            default:
                throw new UnsupportedOperationException(getContext().getString(R.string.db_error_uriunknown) + uri);
        }

        if (itemDeleted != 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }

        return itemDeleted;
    }

    @Override
    public Uri insert(@NonNull Uri uri, ContentValues values) {
        // Обработчик зарпосов insert
        Uri returnUri;

        switch (sUriMatcher.match(uri)) {
            case CODE_READLATER_ITEMS:
                long id = mReadLaterDbHelper.getWritableDatabase().insert(ReadLaterContract.ReadLaterEntry.TABLE_NAME, null, values);
                if ( id > 0 ) {
                    returnUri =  ContentUris.withAppendedId(ReadLaterContract.ReadLaterEntry.CONTENT_URI, id);
                } else {
                    throw new android.database.SQLException(getContext().getString(R.string.db_error_insert) + uri);
                }
                break;
            default:
                throw new UnsupportedOperationException(getContext().getString(R.string.db_error_uriunknown) + uri);
        }

        getContext().getContentResolver().notifyChange(uri, null);

        return returnUri;
    }

    @Override
    public int update(@NonNull Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        // Обработчик запросов update
        int itemUpdated;

        if (null == selection) selection = "1";

        switch (sUriMatcher.match(uri)) {
            case CODE_READLATER_ITEMS_WITH_ID:
                String id = uri.getPathSegments().get(1);
                itemUpdated = mReadLaterDbHelper.getWritableDatabase().update(
                        ReadLaterContract.ReadLaterEntry.TABLE_NAME,
                        values, "_id=?",
                        new String[] {id});

                break;
            default:
                throw new UnsupportedOperationException(getContext().getString(R.string.db_error_uriunknown) + uri);
        }

        if (itemUpdated != 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }

        return itemUpdated;
    }
}
