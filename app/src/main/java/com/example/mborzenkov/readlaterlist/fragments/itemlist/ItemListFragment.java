package com.example.mborzenkov.readlaterlist.fragments.itemlist;

import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ListView;

import com.example.mborzenkov.readlaterlist.R;
import com.example.mborzenkov.readlaterlist.adt.ReadLaterItem;
import com.example.mborzenkov.readlaterlist.adt.ReadLaterItemDbAdapter;

/** Фрагмент со списком ReadLaterItem.
 * Activity, использующая фрагмент, должна реализовывать интерфейс ItemListCallbacks.
 */
public class ItemListFragment extends Fragment implements
        ItemListAdapter.ItemListAdapterOnClickHandler,
        LoaderManager.LoaderCallbacks<Cursor> {

    /** TAG фрагмента для фрагмент менеджера. */
    public static final String TAG = "fragment_itemlist";

    /** Возвращает уже созданный ранее объект ItemListFragment или создает новый, если такого нет.
     * Для создания объектов следует всегда использовать этот метод.
     * Не помещает объектво FragmentManager.
     * При помещении объекта в FragmentManager, следует использовать тэг TAG.
     *
     * @param fragmentManager менеджер для поиска фрагментов по тэгу
     * @return новый объект ItemListFragment
     */
    public static ItemListFragment getInstance(FragmentManager fragmentManager) {

        ItemListFragment fragment = (ItemListFragment) fragmentManager.findFragmentByTag(TAG);

        if (fragment == null) {
            fragment = new ItemListFragment();
        }

        return fragment;

    }

    /** Интерфейс для оповещений о событиях во фрагменте. */
    public interface ItemListCallbacks {

        /** Вызывается при клике на элемент списка.
         *
         * @param item элемент списка в формате ReadLaterItem
         * @param localId _id этого элемента
         */
        void onItemClick(@NonNull ReadLaterItem item, int localId);

        /** Вызывается, когда список список обновился.
         * А именно, когда LoaderManager вернул onLoadFinished.
         *
         * @param isEmpty признак, пустой ли сейчас список
         */
        void onItemListReloaded(boolean isEmpty);
    }

    /** Объект для колбеков о событиях во фрагменте. */
    private @Nullable ItemListCallbacks mCallbacks = null;

    // Хэлперы
    private @Nullable ItemListAdapter mItemListAdapter = null;
    private @Nullable ItemListLoaderManager mLoaderManager = null;

    // Объекты layout
    private ListView mItemsListView;
    private LinearLayout mEmptyListView;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof ItemListCallbacks) {
            mCallbacks = (ItemListCallbacks) context;
        }
        mItemListAdapter = new ItemListAdapter(context, this);
        mLoaderManager = new ItemListLoaderManager(this);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_itemlist, container, false);

        // Инициализируем элементы layout
        mItemsListView = (ListView) rootView.findViewById(R.id.listview_itemlist);
        mItemsListView.setAdapter(mItemListAdapter);
        mEmptyListView = (LinearLayout) rootView.findViewById(R.id.linearLayout_emptylist);

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
            if (cursor != null && !cursor.isClosed()) {
                cursor.moveToPosition(position);
                int itemLocalId = cursor.getInt(ItemListLoaderManager.INDEX_COLUMN_ID);
                ReadLaterItemDbAdapter dbAdapter = new ReadLaterItemDbAdapter();
                ReadLaterItem item = dbAdapter.itemFromCursor(cursor);
                mCallbacks.onItemClick(item, itemLocalId);
            }
        }
    }

    /** Перезагружает данные в списке. */
    public void reloadData() {
        if (mLoaderManager != null) {
            mLoaderManager.reloadData();
        }
    }

    /** Устанавливает поисковый запрос и применяет поиск.
     * Перезагружает данные самостоятельно.
     *
     * @param query поисковый запрос
     */
    public void toggleSearch(String query) {
        if (mLoaderManager != null) {
            mLoaderManager.setSearchQuery(query);
        }
    }


    // Колбеки из ItemListLoaderManager

    @Override
    public Loader<Cursor> onCreateLoader(int loaderId, final Bundle args) {
        if (mLoaderManager != null) {
            switch (loaderId) {
                case ItemListLoaderManager.ITEM_LOADER_ID:
                    // Создаем новый CursorLoader, нужно все имеющееся в базе данных
                    return mLoaderManager.getNewCursorLoader();
                default:
                    throw new IllegalArgumentException("Loader Not Implemented: " + loaderId);
            }
        } else {
            return null;
        }
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, @Nullable Cursor data) {
        // По завершению загрузки, подменяем Cursor в адаптере и показываем данные
        Handler handler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                if (msg.what == 1) {
                    if ((mCallbacks != null) && (mItemListAdapter != null)) {
                        mItemListAdapter.changeCursor(data);
                        boolean listIsEmpty = data == null || data.getCount() == 0;
                        if (listIsEmpty) {
                            mItemsListView.setVisibility(View.INVISIBLE);
                            mEmptyListView.setVisibility(View.VISIBLE);
                        } else {
                            mEmptyListView.setVisibility(View.INVISIBLE);
                            mItemsListView.setVisibility(View.VISIBLE);
                        }
                        mCallbacks.onItemListReloaded(listIsEmpty);
                    }
                }
            }
        };
        handler.sendEmptyMessage(1);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        // При сбросе загрузчика данных, сбрасываем данные
        if (mItemListAdapter != null) {
            mItemListAdapter.changeCursor(null);
        }
    }

    /** Проверяет, загружены ли данные в список.
     * Получает Cursor у адаптера и проверяет, что он не null.
     * Если данные загружены, но список пуст, то результат будет true.
     *
     * @return true, если данные загружены, иначе false
     */
    public boolean dataIsLoaded() {
        return (mItemListAdapter != null) && (mItemListAdapter.getCursor() != null);
    }

}
