package com.example.mborzenkov.readlaterlist.data;

import android.annotation.TargetApi;
import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.net.Uri;
import android.support.annotation.NonNull;

// TODO: Javadoc
public class ReadLaterContentProvider extends ContentProvider {

    // TODO: комментарии и строки

    public static final int CODE_READLATER_ITEMS = 100;
    public static final int CODE_READLATER_ITEMS_WITH_ID = 101;

    private static final UriMatcher sUriMatcher = buildUriMatcher();
    private ReadLaterDbHelper mReadLaterDbHelper;

    public static UriMatcher buildUriMatcher() {

        final UriMatcher matcher = new UriMatcher(UriMatcher.NO_MATCH);
        final String authority = ReadLaterContract.CONTENT_AUTHORITY;
        matcher.addURI(authority, ReadLaterContract.PATH_ITEMS, CODE_READLATER_ITEMS);
        matcher.addURI(authority, ReadLaterContract.PATH_ITEMS + "/#", CODE_READLATER_ITEMS_WITH_ID);

        return matcher;
    }

    @Override
    public boolean onCreate() {
        mReadLaterDbHelper = new ReadLaterDbHelper(getContext());
        return true;
    }

    // TODO: implement or not?
    @Override
    public String getType(@NonNull Uri uri) {
        throw new RuntimeException("Not supported operation");
    }

    @Override
    public Cursor query(@NonNull Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {

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
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }

        cursor.setNotificationUri(getContext().getContentResolver(), uri);
        return cursor;
    }

    @Override
    public int delete(@NonNull Uri uri, String selection, String[] selectionArgs) {

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
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }

        if (itemDeleted != 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }

        return itemDeleted;
    }

    @Override
    public Uri insert(@NonNull Uri uri, ContentValues values) {

        Uri returnUri;

        switch (sUriMatcher.match(uri)) {
            case CODE_READLATER_ITEMS:
                long id = mReadLaterDbHelper.getWritableDatabase().insert(ReadLaterContract.ReadLaterEntry.TABLE_NAME, null, values);
                if ( id > 0 ) {
                    returnUri =  ContentUris.withAppendedId(ReadLaterContract.ReadLaterEntry.CONTENT_URI, id);
                } else {
                    throw new android.database.SQLException("Failed to insert row into " + uri);
                }
                break;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }

        getContext().getContentResolver().notifyChange(uri, null);

        return returnUri;
    }

    @Override
    public int update(@NonNull Uri uri, ContentValues values, String selection, String[] selectionArgs) {
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
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }

        if (itemUpdated != 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }

        return itemUpdated;
    }
}
