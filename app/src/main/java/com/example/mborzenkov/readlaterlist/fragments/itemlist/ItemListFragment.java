package com.example.mborzenkov.readlaterlist.fragments.itemlist;

import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import com.example.mborzenkov.readlaterlist.R;
import com.example.mborzenkov.readlaterlist.adt.ReadLaterItem;
import com.example.mborzenkov.readlaterlist.adt.ReadLaterItemDbAdapter;

public class ItemListFragment extends Fragment implements
        ItemListAdapter.ItemListAdapterOnClickHandler {

    public static ItemListFragment newInstance() {
        ItemListFragment itemListFragment = new ItemListFragment();
        return itemListFragment;
    }


    public interface ItemListCallbacks {
        void onNewItemClick();
        void onItemClick(@NonNull ReadLaterItem item, int localId);
        void onItemListReloaded(boolean isEmpty);
    }

    private @Nullable ItemListCallbacks mCallbacks;

    // Хэлперы
    private @Nullable ItemListAdapter mItemListAdapter;
    private @Nullable ItemListLoaderManager mLoaderManager;

    // Элементы layout
    private ListView mItemListView;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mCallbacks = (ItemListCallbacks) context;
        mItemListAdapter = new ItemListAdapter(context, this);
        mLoaderManager = new ItemListLoaderManager(this);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_itemlist, container, false);

        // Инициализируем FloatingActionButton
        FloatingActionButton fab = (FloatingActionButton) rootView.findViewById(R.id.fab_item_add);
        fab.setOnClickListener(view -> {
            // Отправляем колбек нажатия на создание нового элемента
            if (mCallbacks != null) {
                mCallbacks.onNewItemClick();
            }
        });

        // Инициализируем элементы layout
        mItemListView = (ListView) rootView.findViewById(R.id.listview_itemlist);
        mItemListView.setAdapter(mItemListAdapter);

        return rootView;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mCallbacks = null;
        mItemListAdapter = null;
        mLoaderManager = null;
    }


    @Override
    public void onClick(int position) {
        if ((mCallbacks != null) && (mItemListAdapter != null)) {
            // При нажатии на элемент, подготавливает и отправляет колбек.
            Cursor cursor = mItemListAdapter.getCursor();
            if (cursor != null) {
                cursor.moveToPosition(position);
                int itemLocalId = cursor.getInt(ItemListLoaderManager.INDEX_COLUMN_ID);
                ReadLaterItemDbAdapter dbAdapter = new ReadLaterItemDbAdapter();
                ReadLaterItem item = dbAdapter.itemFromCursor(cursor);
                mCallbacks.onItemClick(item, itemLocalId);
            }
        }
    }

    /** Перезагружает данные. */
    public void reloadData() {
        if (mLoaderManager != null) {
            mLoaderManager.reloadData();
        }
    }

    /** Устанавливает поисковый запрос и применяет поиск.
     * Перезагружает данные самостоятельно, если не выполняется длительная загрузка.
     *
     * @param query поисковый запрос
     */
    public void toggleSearch(String query) {
        if (mLoaderManager != null) {
            mLoaderManager.setSearchQuery(query);
        }
    }

    void onLoaderManagerFinishedLoading(@NonNull Cursor newData) {
        if ((mCallbacks != null) && (mItemListAdapter != null)) {
            mItemListAdapter.changeCursor(newData);
            mCallbacks.onItemListReloaded(newData.getCount() == 0);
        }
    }

}
