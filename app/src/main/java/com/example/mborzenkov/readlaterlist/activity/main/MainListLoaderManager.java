package com.example.mborzenkov.readlaterlist.activity.main;

import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.util.Log;

import com.example.mborzenkov.readlaterlist.data.MainListFilter;
import com.example.mborzenkov.readlaterlist.data.ReadLaterContract;
import com.example.mborzenkov.readlaterlist.utility.MainListFilterUtils;

import java.util.Arrays;

/** Главная Activity, представляющая собой список. */
class MainListLoaderManager implements LoaderManager.LoaderCallbacks<Cursor> {

    /** ID Используемого LoadManager'а. */
    private static final int ITEM_LOADER_ID = 13;

    /** Используемые колонки базы данных. */
    private static final String[] MAIN_LIST_PROJECTION = {
        ReadLaterContract.ReadLaterEntry._ID,
        ReadLaterContract.ReadLaterEntry.COLUMN_LABEL,
        ReadLaterContract.ReadLaterEntry.COLUMN_DESCRIPTION,
        ReadLaterContract.ReadLaterEntry.COLUMN_COLOR,
        ReadLaterContract.ReadLaterEntry.COLUMN_DATE_CREATED,
        ReadLaterContract.ReadLaterEntry.COLUMN_DATE_LAST_MODIFIED,
        ReadLaterContract.ReadLaterEntry.COLUMN_DATE_LAST_VIEW,
        ReadLaterContract.ReadLaterEntry.COLUMN_IMAGE_URL
    };

    // Индексы для колонок из MAIN_LIST_PROJECTION, для упрощения
    static final int INDEX_COLUMN_ID = 0;
    static final int INDEX_COLUMN_LABEL = 1;
    static final int INDEX_COLUMN_DESCRIPTION = 2;
    static final int INDEX_COLUMN_COLOR = 3;
    // static final int INDEX_COLUMN_DATE_CREATED = 4;
    static final int INDEX_COLUMN_DATE_LAST_MODIFIED = 5;
    // static final int INDEX_COLUMN_DATE_LAST_VIEW = 6;
    // static final int INDEX_COLUMN_IMAGE_URL = 7;

    /** Ссылка на MainListActivity. */
    private final MainListActivity mActivity;
    /** Поисковый запрос. */
    private String mSearchQuery = "";


    MainListLoaderManager(MainListActivity activity) {
        mActivity = activity;
    }

    /** Возвращает CursorLoader для указанного запроса, добавляя к нему поисковый запрос и фильтр, если имеются.
     *
     * @return новый CursorLoader
     */
    private CursorLoader getNewCursorLoader() {

        MainListFilter filter = MainListFilterUtils.getCurrentFilter();
        StringBuilder selection = new StringBuilder();
        String[] selectionArgs = new String[0];
        String sortOrder = "";
        if (filter != null) {
            sortOrder = filter.getSqlSortOrder();
            selection.append(filter.getSqlSelection(mActivity));
            selectionArgs = filter.getSqlSelectionArgs(mActivity);
        }
        if (!mSearchQuery.isEmpty()) {
            if (!selection.toString().trim().isEmpty()) {
                selection.append(" AND ");
            }
            selection.append(String.format("_id IN (SELECT docid FROM %s WHERE %s MATCH ?)",
                    ReadLaterContract.ReadLaterEntry.TABLE_NAME_FTS, ReadLaterContract.ReadLaterEntry.TABLE_NAME_FTS));
            selectionArgs = Arrays.copyOf(selectionArgs, selectionArgs.length + 1);
            selectionArgs[selectionArgs.length - 1] = mSearchQuery;
        }
        Log.d("SELECTION", String.format("%s, %s", selection.toString(), Arrays.toString(selectionArgs)));
        Log.d("ORDERING", sortOrder);
        return new CursorLoader(mActivity, ReadLaterContract.ReadLaterEntry.CONTENT_URI,
                MAIN_LIST_PROJECTION, selection.toString(), selectionArgs, sortOrder);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int loaderId, final Bundle args) {
        switch (loaderId) {
            case ITEM_LOADER_ID:
                // Создаем новый CursorLoader, нужно все имеющееся в базе данных
                return getNewCursorLoader();
            default:
                throw new IllegalArgumentException("Loader Not Implemented: " + loaderId);
        }
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        // По завершению загрузки, подменяем Cursor в адаптере и показываем данные
        mActivity.changeCursorInAdapter(data);
        mActivity.showDataView();
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        // При сбросе загрузчика данных, сбрасываем данные
        mActivity.changeCursorInAdapter(null);
    }

    /** Устанавливает поисковый запрос и применяет поиск.
     * Вызывает reloadData() самостоятельно, если не выполняется длительная загрузка.
     *
     * @param query поисковый запрос
     */
    void setSearchQuery(String query) {
        mSearchQuery = query;
        reloadData();
    }

    /** Обновляет данные. */
    void reloadData() {
        if (mActivity.getSupportLoaderManager().getLoader(ITEM_LOADER_ID) != null) {
            mActivity.getSupportLoaderManager().restartLoader(ITEM_LOADER_ID, null, this);
        } else {
            mActivity.getSupportLoaderManager().initLoader(MainListLoaderManager.ITEM_LOADER_ID, null, this);
        }
    }

}
