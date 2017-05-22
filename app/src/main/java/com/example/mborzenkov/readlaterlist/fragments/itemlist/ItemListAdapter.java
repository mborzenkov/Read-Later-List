package com.example.mborzenkov.readlaterlist.fragments.itemlist;

import android.content.Context;
import android.database.Cursor;
import android.graphics.drawable.GradientDrawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.mborzenkov.readlaterlist.R;
import com.example.mborzenkov.readlaterlist.adt.ReadLaterItem;
import com.example.mborzenkov.readlaterlist.adt.ReadLaterItemDbAdapter;

import java.text.SimpleDateFormat;
import java.util.Locale;

/** Адаптер для ItemList типа {@link android.support.v7.widget.RecyclerView}.
 */
class ItemListAdapter extends RecyclerView.Adapter<ItemListAdapter.ItemListViewHolder> {

    /////////////////////////
    // Константы

    /** Формат выводимых дат. */
    private static final String FORMAT_DATE = "dd.MM.yy HH:mm";


    /////////////////////////
    // Static

    /** Интерфейс для обработчика нажатий. */
    interface ItemListAdapterOnClickHandler {

        /** Вызывается при нажатии на элемент.
         *
         * @param item элемент, на который нажали
         * @param itemLocalId внутренний идентификатор элемента (_id)
         */
        void onClick(@NonNull ReadLaterItem item, int itemLocalId);
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
                mCursor.moveToPosition(getAdapterPosition());
                ReadLaterItemDbAdapter dbAdapter = new ReadLaterItemDbAdapter();
                mClickHandler.onClick(dbAdapter.itemFromCursor(mCursor),
                        mCursor.getInt(ItemListLoaderManager.INDEX_COLUMN_ID));
            }
        }

    }


    /////////////////////////
    // Поля объекта

    /** Контекст. */
    private final @NonNull Context mContext;
    /** Обработчик нажатий. */
    private final @NonNull ItemListAdapterOnClickHandler mClickHandler;
    /** Текущий курсор. */
    private @Nullable Cursor mCursor;


    /////////////////////////
    // Методы и колбеки жизненного цикла

    /** Создает новый объект ItemListAdapter для указанного контекста и с указанным ClickHandler'ом.
     *
     * @param context контекст (activity)
     * @param clickHandler интерфейс для колбеков
     */
    ItemListAdapter(@NonNull Context context, @NonNull ItemListAdapterOnClickHandler clickHandler) {
        mContext = context;
        mClickHandler = clickHandler;
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

}
