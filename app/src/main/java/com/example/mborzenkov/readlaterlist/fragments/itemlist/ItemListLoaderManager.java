package com.example.mborzenkov.readlaterlist.fragments.itemlist;

import android.support.annotation.NonNull;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.util.Log;

import com.example.mborzenkov.readlaterlist.activity.main.MainActivityLongTask;
import com.example.mborzenkov.readlaterlist.adt.MainListFilter;
import com.example.mborzenkov.readlaterlist.adt.UserInfo;
import com.example.mborzenkov.readlaterlist.data.ReadLaterContract;
import com.example.mborzenkov.readlaterlist.utility.MainListFilterUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** {@link LoaderManager} для {@link ItemListFragment}.
 */
class ItemListLoaderManager  {

    /** ID Используемого LoadManager'а. */
    static final int ITEM_LOADER_ID = 13;
    /** Запрос на заметки пользователя. */
    private static final String QUERY_USER_ID = String.format("%s = ?",
            ReadLaterContract.ReadLaterEntry.COLUMN_USER_ID);

    /** Используемые колонки базы данных. */
    private static final String[] MAIN_LIST_PROJECTION = {
        ReadLaterContract.ReadLaterEntry._ID,
        ReadLaterContract.ReadLaterEntry.COLUMN_LABEL,
        ReadLaterContract.ReadLaterEntry.COLUMN_DESCRIPTION,
        ReadLaterContract.ReadLaterEntry.COLUMN_COLOR,
        ReadLaterContract.ReadLaterEntry.COLUMN_DATE_CREATED,
        ReadLaterContract.ReadLaterEntry.COLUMN_DATE_LAST_MODIFIED,
        ReadLaterContract.ReadLaterEntry.COLUMN_DATE_LAST_VIEW,
        ReadLaterContract.ReadLaterEntry.COLUMN_IMAGE_URL,
        ReadLaterContract.ReadLaterEntry.COLUMN_REMOTE_ID
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
    // static final int INDEX_COLUMN_REMOTE_ID = 8;

    /** Ссылка на ItemListFragment. */
    private final @NonNull ItemListFragment mItemListFragment;
    /** Поисковый запрос. */
    private String mSearchQuery = "";

    /** Создает и инициализирует новый объект ItemListLoaderManager.
     *
     *  @param itemListFragment ссылка на ItemListFragment для контекста, обращений к activity и колбеков
     */
    ItemListLoaderManager(@NonNull ItemListFragment itemListFragment) {
        mItemListFragment   = itemListFragment;
    }

    /** Возвращает CursorLoader для указанного запроса, добавляя к нему поисковый запрос и фильтр, если имеются.
     *
     * @return новый CursorLoader
     */
    CursorLoader getNewCursorLoader() {

        MainListFilter filter = MainListFilterUtils.getCurrentFilter();
        StringBuilder selection = new StringBuilder(QUERY_USER_ID);
        List<String> selectionArgs = new ArrayList<>();
        selectionArgs.add(String.valueOf(UserInfo.getCurentUser(mItemListFragment.getContext()).getUserId()));
        String sortOrder = "";
        if (filter != null) {
            sortOrder = filter.getSqlSortOrder();
            String filterSelection = filter.getSqlSelection(mItemListFragment.getContext());
            if (!filterSelection.isEmpty()) {
                selection.append(" AND ").append(filterSelection);
                selectionArgs.addAll(Arrays.asList(filter.getSqlSelectionArgs(mItemListFragment.getContext())));
            }
        }
        if (!mSearchQuery.isEmpty()) {
            if (!selection.toString().trim().isEmpty()) {
                selection.append(" AND ");
            }
            selection.append(String.format("_id IN (SELECT docid FROM %s WHERE %s MATCH ?)",
                    ReadLaterContract.ReadLaterEntry.TABLE_NAME_FTS, ReadLaterContract.ReadLaterEntry.TABLE_NAME_FTS));
            selectionArgs.add(mSearchQuery);
        }
        Log.d("SELECTION", String.format("%s, %s", selection.toString(), Arrays.toString(selectionArgs.toArray())));
        Log.d("ORDERING", sortOrder);
        return new CursorLoader(mItemListFragment.getContext(),
                ReadLaterContract.ReadLaterEntry.CONTENT_URI, MAIN_LIST_PROJECTION,
                selection.toString(),
                selectionArgs.toArray(new String[selectionArgs.size()]),
                sortOrder);
    }

    /** Устанавливает поисковый запрос и применяет поиск.
     * Вызывает reloadData() самостоятельно, если не выполняется длительная загрузка.
     *
     * @param query поисковый запрос
     */
    void setSearchQuery(String query) {
        mSearchQuery = query;
        if (!MainActivityLongTask.isActive()) {
            reloadData();
        }
    }

    /** Обновляет данные.
     * Оповещает о результатах loaderCallbacks.
     */
    void reloadData() {
        if (!MainActivityLongTask.isActive()) {
            if (mItemListFragment.getActivity().getSupportLoaderManager().getLoader(ITEM_LOADER_ID) != null) {
                mItemListFragment.getActivity().getSupportLoaderManager().restartLoader(ITEM_LOADER_ID,
                        null, mItemListFragment);
            } else {
                mItemListFragment.getActivity().getSupportLoaderManager()
                        .initLoader(ItemListLoaderManager.ITEM_LOADER_ID, null, mItemListFragment);
            }
        }
    }

}
