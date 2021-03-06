package com.example.mborzenkov.readlaterlist.fragments.itemlist;

import android.app.SearchManager;
import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.example.mborzenkov.readlaterlist.R;
import com.example.mborzenkov.readlaterlist.adt.MainListFilter;
import com.example.mborzenkov.readlaterlist.adt.ReadLaterItem;
import com.example.mborzenkov.readlaterlist.adt.ReadLaterItemDbAdapter;
import com.example.mborzenkov.readlaterlist.fragments.BasicFragmentCallbacks;
import com.example.mborzenkov.readlaterlist.fragments.edititem.EditItemFragmentActions;
import com.example.mborzenkov.readlaterlist.fragments.filterdrawer.FilterDrawerFragment;
import com.example.mborzenkov.readlaterlist.utility.MainListFilterUtils;

/** Фрагмент со списком ReadLaterItem.
 * Activity, использующая фрагмент, должна реализовывать интерфейс ItemListCallbacks.
 */
public class ItemListFragment extends Fragment implements
        SearchView.OnQueryTextListener,
        LoaderManager.LoaderCallbacks<Cursor> {


    /////////////////////////
    // Константы

    /** TAG фрагмента для фрагмент менеджера. */
    public static final String TAG = "fragment_itemlist";

    /** ID контейнера для Drawer. */
    private static final @IdRes int CONTAINER_FRAGMENT_FILTER = R.id.filterfragmentcontainer_itemlist;


    /////////////////////////
    // Static

    /** Возвращает уже созданный ранее объект ItemListFragment или создает новый, если такого нет.
     * Для создания объектов следует всегда использовать этот метод.
     * Не помещает объект в FragmentManager.
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
    public interface ItemListCallbacks extends
            BasicFragmentCallbacks,
            ItemListAdapter.ItemListAdapterEventHandler {

        /** Вызывается при нажатии на (+). */
        void onNewItemClick();

        /** Вызывается при потягивании SwipeRefreshLayout или нажатии на кнопку Refresh. */
        void onRefreshToggled();

    }


    /////////////////////////
    // Поля объекта

    /** Объект для колбеков о событиях во фрагменте. */
    private @Nullable ItemListCallbacks mCallbacks = null;

    // Хэлперы
    private ItemListViewHolder mViewHolder;
    private ItemListAdapter mItemListAdapter;
    private @Nullable ItemListLoaderManager mLoaderManager = null;
    private @Nullable ItemTouchHelperCallback mTouchHelperCallback = null;
    private FilterDrawerFragment mDrawerFragment;


    /////////////////////////
    // Колбеки Fragment

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof ItemListCallbacks) {
            mCallbacks = (ItemListCallbacks) context;
            mItemListAdapter = new ItemListAdapter(context, mCallbacks);
        }
        mLoaderManager = new ItemListLoaderManager(this);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        mViewHolder = new ItemListViewHolder(this, inflater, container);

        // Подключаем адаптер к RecyclerView
        mViewHolder.setAdapterToRecyclerView(mItemListAdapter);

        // Настройка тачев
        if (mItemListAdapter != null) {
            mTouchHelperCallback = new ItemTouchHelperCallback(mItemListAdapter);
            ItemTouchHelper itemTouchHelper = new ItemTouchHelper(mTouchHelperCallback);
            mViewHolder.attachTouchHelperToRecyclerView(itemTouchHelper);
            mViewHolder.setOnTouchListener(mTouchHelperCallback);
            mTouchHelperCallback.setDragEnabled(MainListFilterUtils.getCurrentFilter().getSortType()
                    == MainListFilter.SortType.MANUAL);
        }

        // Инициализация FilterFragment
        FragmentManager fragmentManager = getChildFragmentManager();
        mDrawerFragment = FilterDrawerFragment.getInstance(fragmentManager);
        fragmentManager.beginTransaction()
                .replace(CONTAINER_FRAGMENT_FILTER, mDrawerFragment, FilterDrawerFragment.TAG)
                .commit();

        setHasOptionsMenu(true);

        return mViewHolder.getRootView();

    }

    @Override
    public void onResume() {
        super.onResume();
        if (mLoaderManager != null) {
            mLoaderManager.restartLoader();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        setHasOptionsMenu(false);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mCallbacks = null;
        mItemListAdapter = null;
        mLoaderManager = null;
    }


    /////////////////////////
    // Колбеки Menu

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, MenuInflater inflater) {

        inflater.inflate(R.menu.menu_mainlist, menu);

        // Создание меню поиска
        SearchManager searchManager = (SearchManager) getContext().getSystemService(Context.SEARCH_SERVICE);
        SearchView searchView = (SearchView) menu.findItem(R.id.mainlist_action_search).getActionView();
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getActivity().getComponentName()));
        searchView.setSubmitButtonEnabled(true);
        searchView.setOnQueryTextListener(this);

        if (mCallbacks == null) {
            menu.findItem(R.id.mainlist_action_refresh).setVisible(false);
        }

    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.mainlist_settings:
                mViewHolder.openDrawer();
                return true;
            case R.id.mainlist_action_refresh:
                if (mCallbacks != null) {
                    mCallbacks.onRefreshToggled();
                }
                return true;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }


    /////////////////////////
    // Колбеки ввода текста (search)

    @Override
    public boolean onQueryTextSubmit(@NonNull String query) {
        if (mLoaderManager != null) {
            mLoaderManager.setSearchQuery(query);
        }
        return false;
    }

    @Override
    public boolean onQueryTextChange(@NonNull String newText) {
        if (mLoaderManager != null) {
            mLoaderManager.setSearchQuery(newText);
        }
        return false;
    }


    /////////////////////////
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
        if (mItemListAdapter != null) {
            mItemListAdapter.swapCursor(data);
            boolean listIsEmpty = ((data == null) || (data.getCount() == 0));
            mViewHolder.showData(listIsEmpty);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        // При сбросе загрузчика данных, сбрасываем данные
        if (mItemListAdapter != null) {
            mItemListAdapter.swapCursor(null);
        }
    }


    /////////////////////////
    // Колбеки ViewHolder

    /** Вызывается, когда готов новый toolbar и его нужно установить. */
    void setToolbar(Toolbar toolbar) {
        if (mCallbacks != null) {
            mCallbacks.setNewToolbar(toolbar, getString(R.string.app_name));
        }
    }

    /** Вызывается при нажатии на FloatingActionButton ADD. */
    void onFabAddClick() {
        if (mCallbacks != null) {
            mCallbacks.onNewItemClick();
        }
    }

    /** Вызывается при потягивании swipe refresh layout. */
    void onSwipeRefreshToggled() {
        if (mCallbacks != null) {
            mCallbacks.onRefreshToggled();
        }
    }


    /////////////////////////
    // Все остальное

    /** Устанавливает индикатор загрузки.
     *
     * @param refreshing true - индикатор появляется, false - убирается
     */
    public void setRefreshing(boolean refreshing) {
        mViewHolder.setRefreshing(refreshing);
    }

    /** Метод для оповещения об изменениях данных.
     * Перезагружает лоадер менеджер.
     */
    public void onDataChanged() {
        // Обновить доступность драг-н-дроп
        if (mTouchHelperCallback != null) {
            mTouchHelperCallback.setDragEnabled(MainListFilterUtils.getCurrentFilter().getSortType()
                    == MainListFilter.SortType.MANUAL);
        }
        if (mLoaderManager != null) {
            mLoaderManager.restartLoader();
        }
    }

    /** Возвращает объект на позиции position в наборе данных адаптера.
     *
     * @see com.example.mborzenkov.readlaterlist.fragments.edititem.EditItemViewPagerFragment.EditItemViewPagerCallbacks
     */
    public @Nullable ReadLaterItem getItemAt(int position) {
        if (mItemListAdapter != null) {
            Cursor cursor = mItemListAdapter.getCurrentCursor();
            if (cursor != null && !cursor.isClosed() && (cursor.getCount() != 0)) {
                int prevPosition = cursor.getPosition();
                cursor.moveToPosition(position);
                ReadLaterItem item = (new ReadLaterItemDbAdapter()).itemFromCursor(cursor);
                cursor.moveToPosition(prevPosition);
                return item;
            }
        }
        return null;
    }

    /** Возвращает внутренний идентификатор объекта на позиции position в наборе данных адаптера.
     *
     * @see com.example.mborzenkov.readlaterlist.fragments.edititem.EditItemViewPagerFragment.EditItemViewPagerCallbacks
     */
    public int getItemLocalIdAt(int position) {
        if (mItemListAdapter != null) {
            Cursor cursor = mItemListAdapter.getCurrentCursor();
            if (cursor != null && !cursor.isClosed() && (cursor.getCount() != 0)) {
                int prevPosition = cursor.getPosition();
                cursor.moveToPosition(position);
                int localId = cursor.getInt(ItemListLoaderManager.INDEX_COLUMN_ID);
                cursor.moveToPosition(prevPosition);
                return localId;
            }
        }
        return EditItemFragmentActions.UID_EMPTY;
    }

    /** Устанавливает нового пользователя в интерфейсе. */
    public void onUserChanged(@NonNull String newUser) {
        mDrawerFragment.setCurrentUser(newUser);
    }

    /** Обновляет Drawer в соответствии с новым фильтром. */
    public void onFilterChanged(MainListFilter newFilter) {
        mDrawerFragment.onFilterChanged(newFilter);
    }

    /** Устанавливает выбор текущего фильтра. */
    public void setSavedFilterSelection(int position, boolean reload) {
        mDrawerFragment.setSavedFilterSelection(position, reload);
    }

}
