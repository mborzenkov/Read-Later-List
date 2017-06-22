package com.example.mborzenkov.readlaterlist.fragments.itemlist;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.example.mborzenkov.readlaterlist.R;

/** Класс для работы с View в ItemListFragment. */
class ItemListViewHolder {

    /** Максимальная длительность показа индикатора загрузки. */
    private static final int SYNC_ICON_MAX_DURATION = 6000; // 6 сек

    /** Ссылка на Presenter (ItemListFragment. */
    private final @NonNull ItemListFragment mItemListFragment;

    // Объекты layout
    private final @NonNull View mRootView;
    private final @NonNull DrawerLayout mDrawerLayout;
    private final @NonNull SwipeRefreshLayout mSwipeRefreshLayout;
    private final @NonNull RecyclerView mItemsRecyclerView;
    private final @NonNull LinearLayout mEmptyListView;

    /** Создает новый ItemListViewHolder и инфлейтит его.
     *
     * @param fragment фрагмент, Presenter
     * @param inflater инфлейтер для инфлейтинга
     * @param container контейнер, в который нужно все поместить
     */
    ItemListViewHolder(@NonNull ItemListFragment fragment, @NonNull LayoutInflater inflater, ViewGroup container) {

        mItemListFragment = fragment;
        mRootView = inflater.inflate(R.layout.fragment_itemlist, container, false);

        // Инициализируем элементы layout
        mDrawerLayout = (DrawerLayout) mRootView.findViewById(R.id.drawerlayout_itemlist);
        mSwipeRefreshLayout = (SwipeRefreshLayout) mRootView.findViewById(R.id.swiperefreshlayout_itemlist);
        mItemsRecyclerView = (RecyclerView) mRootView.findViewById(R.id.listview_itemlist);
        mEmptyListView = (LinearLayout) mRootView.findViewById(R.id.linearLayout_emptylist);

        Context context = fragment.getContext();

        // Настройка RecyclerView
        mItemsRecyclerView.addItemDecoration(new DividerItemDecoration(context, DividerItemDecoration.VERTICAL));
        mItemsRecyclerView.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false));

        // Инициализация Toolbar
        Toolbar toolbar = (Toolbar) mRootView.findViewById(R.id.toolbar_itemlist);
        toolbar.setTitleTextColor(ContextCompat.getColor(context, R.color.icons));
        mItemListFragment.setToolbar(toolbar);

        // Инициализируем FloatingActionButton
        FloatingActionButton floatingAddButton = (FloatingActionButton) mRootView.findViewById(R.id.fab_item_add);
        floatingAddButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mItemListFragment.onFabAddClick();
            }
        });

        // Слушаем о потягивании refresh
        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                mItemListFragment.onSwipeRefreshToggled();
            }
        });

    }

    /** Вызывается для установки адаптера для RecyclerView. */
    void setAdapterToRecyclerView(@NonNull RecyclerView.Adapter adapter) {
        mItemsRecyclerView.setAdapter(adapter);
    }

    /** Вызывается для установки touch listener для RecyclerView. */
    void setOnTouchListener(@NonNull View.OnTouchListener listener) {
        mItemsRecyclerView.setOnTouchListener(listener);
    }

    /** Вызывается для привязки RecyclerView к TouchHelper. */
    void attachTouchHelperToRecyclerView(@NonNull ItemTouchHelper helper) {
        helper.attachToRecyclerView(mItemsRecyclerView);
    }

    /** Вызывается для открытия Drawer. */
    void openDrawer() {
        mDrawerLayout.openDrawer(Gravity.END);
    }

    /** Вызывается для показа данных.
     *
     * @param isEmpty признак, пустые ли данные
     */
    void showData(boolean isEmpty) {
        if (isEmpty) {
            mItemsRecyclerView.setVisibility(View.INVISIBLE);
            mEmptyListView.setVisibility(View.VISIBLE);
        } else {
            mEmptyListView.setVisibility(View.INVISIBLE);
            mItemsRecyclerView.setVisibility(View.VISIBLE);
        }
    }

    /** Вызывается для показа/скрытия индикатора обновления.
     *
     * @param refreshing признак, показать или скрыть индикатор
     */
    void setRefreshing(boolean refreshing) {
        mSwipeRefreshLayout.setRefreshing(refreshing);
        if (refreshing) {
            mSwipeRefreshLayout.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mSwipeRefreshLayout.setRefreshing(false);
                }
            }, SYNC_ICON_MAX_DURATION);
        }
    }

    /** Возвращает корневой элемент. */
    View getRootView() {
        return mRootView;
    }

}
