package com.example.mborzenkov.readlaterlist.fragments.itemlist;

import android.content.Context;
import android.database.Cursor;
import android.graphics.drawable.GradientDrawable;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ResourceCursorAdapter;
import android.widget.TextView;

import com.example.mborzenkov.readlaterlist.R;

import java.text.SimpleDateFormat;
import java.util.Locale;

/** Адаптер для MainActivity (очень простой).
 *      Этот адаптер имеет смысл переписать на RecyclerView
 *      Но так как по заданию было запрещено пользоваться RecyclerView, этого сделано не было
 *      // TODO: Переписать адаптер на RecyclerView
 */
class ItemListAdapter extends ResourceCursorAdapter {

    /** Формат выводимых дат. */
    private static final String FORMAT_DATE = "dd.MM.yy HH:mm";

    /** Контекст. */
    private final @NonNull Context mContext;
    /** Обработчик нажатий. */
    private final @NonNull ItemListAdapterOnClickHandler mClickHandler;

    /** Интерфейс для обработчика нажатий. */
    interface ItemListAdapterOnClickHandler {
        void onClick(int position);
    }

    /** Создает новый объект ItemListAdapter для указанного контекста и с указанным ClickHandler'ом.
     *
     * @param context контекст (activity)
     * @param clickHandler интерфейс для колбеков
     */
    ItemListAdapter(@NonNull Context context, @NonNull ItemListAdapterOnClickHandler clickHandler) {
        super(context, R.layout.content_mainlist_item, null, 0);
        mContext = context;
        mClickHandler = clickHandler;
    }

    /** Класс, собирающий в себе View (ViewHolder). */
    private class ItemListViewHolder implements View.OnClickListener {
        // View, которые хранятся
        private final TextView labelTextView;
        private final TextView descriptionTextView;
        private final ImageView colorImageView;
        private final TextView dateTextView;
        private int position;

        /** Создает новый экземпляр ItemListViewHolder.
         *
         * @param view родительская view, содержащая внутри все нужные view
         */
        ItemListViewHolder(@NonNull View view) {
            labelTextView = (TextView) view.findViewById(R.id.tv_item_label);
            descriptionTextView = (TextView) view.findViewById(R.id.tv_item_description);
            colorImageView = (ImageView) view.findViewById(R.id.iv_item_color);
            dateTextView = (TextView) view.findViewById(R.id.tv_item_date);
            view.setOnClickListener(this);
        }

        @Override
        public void onClick(@NonNull View view) {
            Cursor cursor = getCursor();
            cursor.moveToPosition(position);
            mClickHandler.onClick(position);
        }
    }

    @Override
    public void bindView(@NonNull View view, @NonNull Context context, @NonNull Cursor cursor) {
        ItemListViewHolder viewHolder = (ItemListViewHolder) view.getTag();
        viewHolder.labelTextView.setText(cursor.getString(ItemListLoaderManager.INDEX_COLUMN_LABEL));
        viewHolder.descriptionTextView.setText(cursor.getString(ItemListLoaderManager.INDEX_COLUMN_DESCRIPTION));
        ((GradientDrawable) viewHolder.colorImageView.getBackground())
                .setColor(cursor.getInt(ItemListLoaderManager.INDEX_COLUMN_COLOR));
        long date = cursor.getLong(ItemListLoaderManager.INDEX_COLUMN_DATE_LAST_MODIFIED);
        SimpleDateFormat sdf = new SimpleDateFormat(FORMAT_DATE, Locale.US);
        viewHolder.dateTextView.setText(sdf.format(date));
        viewHolder.position = cursor.getPosition();
    }

    @Override
    public View newView(@NonNull Context context, @NonNull Cursor cursor, @NonNull ViewGroup parent) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.content_mainlist_item, parent, false);
        ItemListViewHolder viewHolder = new ItemListViewHolder(view);
        view.setTag(viewHolder);
        return view;
    }

}
