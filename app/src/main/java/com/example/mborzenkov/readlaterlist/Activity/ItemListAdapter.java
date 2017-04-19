package com.example.mborzenkov.readlaterlist.Activity;

import android.content.Context;
import android.database.Cursor;
import android.graphics.drawable.GradientDrawable;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.ResourceCursorAdapter;
import android.widget.TextView;

import com.example.mborzenkov.readlaterlist.ADT.ReadLaterItem;
import com.example.mborzenkov.readlaterlist.R;

import java.util.List;

public class ItemListAdapter extends ResourceCursorAdapter {

    private final Context mContext;
    private final ItemListAdapterOnClickHandler mClickHandler;

    public interface ItemListAdapterOnClickHandler {
        void onClick(int position);
    }

    public ItemListAdapter(Context context, int layout, Cursor cursor, int flags, ItemListAdapterOnClickHandler clickHandler) {
        super(context, layout, cursor, flags);
        mContext = context;
        mClickHandler = clickHandler;
    }

    private class ItemListViewHolder implements View.OnClickListener {
        private final TextView labelTextView;
        private final TextView descriptionTextView;
        private final ImageView colorImageView;
        protected int position;

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
