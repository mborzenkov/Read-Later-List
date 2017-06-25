package com.example.mborzenkov.readlaterlist.fragments.itemlist;

import android.support.annotation.NonNull;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.util.Log;

import com.example.mborzenkov.readlaterlist.adt.MainListFilter;
import com.example.mborzenkov.readlaterlist.data.ReadLaterContract.ReadLaterEntry;
import com.example.mborzenkov.readlaterlist.utility.MainListFilterUtils;
import com.example.mborzenkov.readlaterlist.utility.UserInfoUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** {@link LoaderManager} для {@link ItemListFragment}.
 */
class ItemListLoaderManager  {

    /** ID Используемого LoadManager'а. */
    static final int ITEM_LOADER_ID = 13;

    /** Используемые колонки базы данных. */
    private static final String[] MAIN_LIST_PROJECTION = {
        ReadLaterEntry._ID,
        ReadLaterEntry.COLUMN_LABEL,
        ReadLaterEntry.COLUMN_DESCRIPTION,
        ReadLaterEntry.COLUMN_COLOR,
        ReadLaterEntry.COLUMN_DATE_CREATED,
        ReadLaterEntry.COLUMN_DATE_LAST_MODIFIED,
        ReadLaterEntry.COLUMN_DATE_LAST_VIEW,
        ReadLaterEntry.COLUMN_IMAGE_URL,
        ReadLaterEntry.COLUMN_REMOTE_ID,
        ReadLaterEntry.COLUMN_ORDER
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
    static final int INDEX_COLUMN_ORDER = 9;

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

        final int userId = UserInfoUtils.getCurentUser(mItemListFragment.getContext()).getUserId();

        MainListFilter filter = MainListFilterUtils.getCurrentFilter();
        StringBuilder selection = new StringBuilder();
        List<String> selectionArgs = new ArrayList<>();
        String sortOrder = "";
        if (filter != null) {
            sortOrder = filter.getSqlSortOrder();
            String filterSelection = filter.getSqlSelection(mItemListFragment.getContext());
            if (!filterSelection.isEmpty()) {
                selection.append(filterSelection);
                selectionArgs.addAll(Arrays.asList(filter.getSqlSelectionArgs(mItemListFragment.getContext())));
            }
        }
        if (!mSearchQuery.isEmpty()) {
            if (!selection.toString().trim().isEmpty()) {
                selection.append(" AND ");
            }
            selection.append(String.format("_id IN (SELECT docid FROM %s WHERE %s MATCH ?)",
                    ReadLaterEntry.TABLE_NAME_FTS, ReadLaterEntry.TABLE_NAME_FTS));
            selectionArgs.add(mSearchQuery);
        }
        Log.d("SELECTION", String.format("%s, %s", selection.toString(), Arrays.toString(selectionArgs.toArray())));
        Log.d("ORDERING", sortOrder);
        return new CursorLoader(mItemListFragment.getContext(),
                ReadLaterEntry.buildUriForUserItems(userId), MAIN_LIST_PROJECTION,
                selection.toString(),
                selectionArgs.toArray(new String[selectionArgs.size()]),
                sortOrder);
    }

    /** Устанавливает поисковый запрос и применяет поиск.
     * Вызывает reloadData() самостоятельно.
     *
     * @param query поисковый запрос
     */
    void setSearchQuery(String query) {
        mSearchQuery = query;
        restartLoader();
    }

    /** Обновляет данные.
     */
    void restartLoader() {
        if (mItemListFragment.getActivity().getSupportLoaderManager().getLoader(ITEM_LOADER_ID) != null) {
            mItemListFragment.getActivity().getSupportLoaderManager().restartLoader(ITEM_LOADER_ID,
                    null, mItemListFragment);
        } else {
            mItemListFragment.getActivity().getSupportLoaderManager()
                    .initLoader(ItemListLoaderManager.ITEM_LOADER_ID, null, mItemListFragment);
        }
    }

}
