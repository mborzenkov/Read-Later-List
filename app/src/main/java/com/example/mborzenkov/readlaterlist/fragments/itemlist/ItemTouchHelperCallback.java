package com.example.mborzenkov.readlaterlist.fragments.itemlist;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.Size;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.MotionEvent;
import android.view.View;

/** Класс колбеков для ItemTouchHelper. */
class ItemTouchHelperCallback extends ItemTouchHelper.Callback implements View.OnTouchListener {

    /////////////////////////
    // Обработчики перемещений

    /** Интерфейс для адаптера. */
    interface ItemTouchHelperAdapter {

        /** Вызывается для оповещения о перемещении элемента.
         * Вызывается после эвента MotionEvent.ACTION_UP, следующего за onMove.
         * Адаптер должен переместить элемент с fromPosition на toPosition.
         *
         * @param fromPosition предыдущая позиция элемента в адаптере
         * @param toPosition новая позиция элемента в адаптере
         */
        void onItemMove(int fromPosition, int toPosition);

    }


    /////////////////////////
    // Поля объекта

    /** Ссылка на адаптер для оповещений. */
    private final @NonNull ItemTouchHelperAdapter mAdapter;
    /** Признак доступности drag event. */
    private boolean mDragEnabled = false;

    /** Переменная для запоминания перемещений в onMove.
     *  mItemMoveValues[0] - предыдущая позиция элемента в адаптере
     *  mItemMoveValues[1] - новая позиция элемента в адаптере
     */
    private @Nullable @Size(value = 2) int[] mItemMoveValues;

    ItemTouchHelperCallback(@NonNull ItemTouchHelperAdapter adapter) {
        mAdapter = adapter;
    }

    /** Устанавливает доступность drag event. */
    void setDragEnabled(boolean enabled) {
        mDragEnabled = enabled;
    }

    /////////////////////////
    // Колбеки ItemTouchHelper.Callback

    @Override
    public int getMovementFlags(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
        int dragFlags = ItemTouchHelper.UP | ItemTouchHelper.DOWN;
        int swipeFlags = ItemTouchHelper.START | ItemTouchHelper.END;
        return makeMovementFlags(dragFlags, swipeFlags);
    }

    @Override
    public boolean isLongPressDragEnabled() {
        return mDragEnabled;
    }

    @Override
    public boolean isItemViewSwipeEnabled() {
        return false;
    }

    @Override
    public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder from, RecyclerView.ViewHolder to) {
        // Во время onMove запоминаем перемещения
        final int fromPosition = from.getAdapterPosition();
        final int toPosition = to.getAdapterPosition();
        if ((mItemMoveValues == null) || (mItemMoveValues[1] != toPosition) || (mItemMoveValues[0] != fromPosition)) {
            mItemMoveValues = new int[] {fromPosition, toPosition};
        }
        return true;
    }

    @Override
    public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) { }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if ((event.getAction() & MotionEvent.ACTION_MASK) == MotionEvent.ACTION_UP) {
            // В UP, если были перемещения, оповещаем адаптер и сбрасываем запомненное значение
            if (mItemMoveValues != null) {
                mAdapter.onItemMove(mItemMoveValues[0], mItemMoveValues[1]);
                mItemMoveValues = null;
            }
        }
        return false;
    }
}
