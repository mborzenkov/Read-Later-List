package com.example.mborzenkov.readlaterlist.fragments.itemlist;

import android.content.Context;
import android.database.Cursor;
import android.graphics.drawable.GradientDrawable;
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.mborzenkov.readlaterlist.R;
import com.example.mborzenkov.readlaterlist.activity.main.MainActivity;
import com.example.mborzenkov.readlaterlist.adt.ReadLaterItem;
import com.example.mborzenkov.readlaterlist.adt.ReadLaterItemDbAdapter;
import com.example.mborzenkov.readlaterlist.utility.ItemTouchHelperCallback;
import com.example.mborzenkov.readlaterlist.utility.ReadLaterDbUtils;

import java.text.SimpleDateFormat;
import java.util.Locale;

/** Адаптер для ItemList типа {@link android.support.v7.widget.RecyclerView}.
 */
class ItemListAdapter extends RecyclerView.Adapter<ItemListAdapter.ItemListViewHolder> implements
        ItemTouchHelperCallback.ItemTouchHelperAdapter {

    /////////////////////////
    // Константы

    /** Формат выводимых дат. */
    private static final String FORMAT_DATE = "dd.MM.yy HH:mm";


    /////////////////////////
    // Static

    /** Интерфейс для обработчика нажатий. */
    interface ItemListAdapterEventHandler {

        /** Вызывается при клике на элемент списка.
         *
         * @param position позиция элемента, на который нажали
         * @param totalItems общее число элементов в массиве данных
         * @param item элемент списка в формате ReadLaterItem
         * @param localId _id этого элемента, > 0
         * @param sharedElement shared element для использования при открытии фрагмента редактирования,
         *                      не null, у него обязательно установлен transition name
         */
        void onItemClick(@IntRange(from = 0) int position,
                         @IntRange(from = 1) int totalItems,
                         @NonNull ReadLaterItem item,
                         @IntRange(from = 0) int localId,
                         @NonNull ImageView sharedElement);

        /** Оповещает о том, что данные в адаптере изменились. */
        void onDataChanged();

    }


    /////////////////////////
    // ViewHolder

    /** Класс, собирающий в себе View (ViewHolder). */
    class ItemListViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        // View, которые хранятся
        private final TextView labelTextView;
        private final TextView descriptionTextView;
        private final ImageView colorImageView;
        private final TextView dateTextView;

        /** Создает новый экземпляр ItemListViewHolder.
         *
         * @param view родительская view, содержащая внутри все нужные view
         */
        ItemListViewHolder(@NonNull View view) {
            super(view);
            labelTextView = (TextView) view.findViewById(R.id.tv_item_label);
            descriptionTextView = (TextView) view.findViewById(R.id.tv_item_description);
            colorImageView = (ImageView) view.findViewById(R.id.iv_item_color);
            dateTextView = (TextView) view.findViewById(R.id.tv_item_date);
            view.setOnClickListener(this);
        }

        @Override
        public void onClick(@NonNull View view) {
            if (mCursor != null) {
                int position = getAdapterPosition();
                mCursor.moveToPosition(position);
                ReadLaterItemDbAdapter dbAdapter = new ReadLaterItemDbAdapter();
                ReadLaterItem itemFromCursor = dbAdapter.itemFromCursor(mCursor);
                if (itemFromCursor != null) {
                    ViewCompat.setTransitionName(colorImageView,
                            MainActivity.SHARED_ELEMENT_COLOR_TRANSITION_NAME);
                    mEventHandler.onItemClick(
                            position,
                            mCursor.getCount(),
                            itemFromCursor,
                            mCursor.getInt(ItemListLoaderManager.INDEX_COLUMN_ID),
                            colorImageView);
                }
            }
        }

    }


    /////////////////////////
    // Поля объекта

    /** Контекст. */
    private final @NonNull Context mContext;
    /** Обработчик событий в адаптере. */
    private final @NonNull ItemListAdapterEventHandler mEventHandler;
    /** Текущий курсор. */
    private @Nullable Cursor mCursor;


    /////////////////////////
    // Методы и колбеки жизненного цикла

    /** Создает новый объект ItemListAdapter для указанного контекста и с указанным ClickHandler'ом.
     *
     * @param context контекст (activity)
     * @param clickHandler интерфейс для колбеков
     */
    ItemListAdapter(@NonNull Context context, @NonNull ItemListAdapterEventHandler clickHandler) {
        mContext = context;
        mEventHandler = clickHandler;
    }

    @Override
    public ItemListViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        View view = LayoutInflater.from(mContext).inflate(R.layout.content_itemlist_item, parent, false);
        view.setFocusable(true);
        return new ItemListViewHolder(view);

    }

    @Override
    public void onBindViewHolder(ItemListViewHolder holder, int position) {
        if (mCursor != null) {
            mCursor.moveToPosition(position);

            holder.labelTextView.setText(mCursor.getString(ItemListLoaderManager.INDEX_COLUMN_LABEL));
            holder.descriptionTextView.setText(mCursor.getString(ItemListLoaderManager.INDEX_COLUMN_DESCRIPTION));
            ((GradientDrawable) holder.colorImageView.getBackground())
                    .setColor(mCursor.getInt(ItemListLoaderManager.INDEX_COLUMN_COLOR));
            long date = mCursor.getLong(ItemListLoaderManager.INDEX_COLUMN_DATE_LAST_MODIFIED);
            SimpleDateFormat sdf = new SimpleDateFormat(FORMAT_DATE, Locale.US);
            holder.dateTextView.setText(sdf.format(date));
        }
    }

    @Override
    public int getItemCount() {
        if (mCursor != null) {
            return mCursor.getCount();
        }
        return 0;
    }

    @Override
    public int getItemViewType(int position) {
        return 0;
    }

    /** Подменяет курсор в адаптере на новый.
     *
     * @param newCursor новый курсор или null, если данных нет
     */
    void swapCursor(@Nullable Cursor newCursor) {
        mCursor = newCursor;
        notifyDataSetChanged();
    }

    /** Возвращает текущий курсор в адаптере.
     *
     * @return текущий курсор
     */
    @Nullable Cursor getCurrentCursor() {
        return mCursor;
    }


    /////////////////////////
    // Обработчики перемещений

    @Override
    public void onItemMove(int fromPosition, int toPosition) {
        // Выполняет перемещение
        if (mCursor != null) {
            mCursor.moveToPosition(fromPosition);
            final int localId = mCursor.getInt(ItemListLoaderManager.INDEX_COLUMN_ID);
            mCursor.moveToPosition(toPosition);
            final int newPosition = mCursor.getInt(ItemListLoaderManager.INDEX_COLUMN_ORDER);
            ReadLaterDbUtils.changeItemOrder(mContext, localId, newPosition);
            mEventHandler.onDataChanged();
        }
    }


}
