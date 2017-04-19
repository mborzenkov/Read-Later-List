package com.example.mborzenkov.readlaterlist.activity;

import android.content.Context;
import android.database.Cursor;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ResourceCursorAdapter;
import android.widget.TextView;

import com.example.mborzenkov.readlaterlist.R;

/**
 * Адаптер для MainList (очень простой)
 *      Этот адаптер имеет смысл переписать на RecyclerView
 *      Но так как по заданию было запрещено пользоваться RecyclerView, этого сделано не было
 *      // TODO: Переписать адаптер на RecyclerView
 */
public class ItemListAdapter extends ResourceCursorAdapter {

    /** Контекст */
    private final Context mContext;
    /** Обработчик нажатий */
    private final ItemListAdapterOnClickHandler mClickHandler;

    /** Интерфейс для обработчика нажатий */
    public interface ItemListAdapterOnClickHandler {
        void onClick(int position);
    }

    public ItemListAdapter(Context context, int layout, Cursor cursor, int flags, ItemListAdapterOnClickHandler clickHandler) {
        super(context, layout, cursor, flags);
        mContext = context;
        mClickHandler = clickHandler;
    }

    /** Класс, собирающий в себе View (ViewHolder) */
    private class ItemListViewHolder implements View.OnClickListener {
        // View, которые хранятся
        private final TextView labelTextView;
        private final TextView descriptionTextView;
        private final ImageView colorImageView;
        private int position;

        ItemListViewHolder (View view) {
            labelTextView = (TextView) view.findViewById(R.id.tv_item_label);
            descriptionTextView = (TextView) view.findViewById(R.id.tv_item_description);
            colorImageView = (ImageView) view.findViewById(R.id.iv_item_color);
            view.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            Cursor cursor = getCursor();
            cursor.moveToPosition(position);
            mClickHandler.onClick(position);
        }
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        ItemListViewHolder viewHolder = (ItemListViewHolder) view.getTag();
        viewHolder.labelTextView.setText(cursor.getString(MainList.INDEX_COLUMN_LABEL));
        viewHolder.descriptionTextView.setText(cursor.getString(MainList.INDEX_COLUMN_DESCRIPTION));
        ((GradientDrawable) viewHolder.colorImageView.getBackground()).setColor(cursor.getInt(MainList.INDEX_COLUMN_COLOR));
        viewHolder.position = cursor.getPosition();
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.content_main_list_item, parent, false);
        ItemListViewHolder viewHolder = new ItemListViewHolder(view);
        view.setTag(viewHolder);
        return view;
    }

}
