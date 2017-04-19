package com.example.mborzenkov.readlaterlist.Activity;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.mborzenkov.readlaterlist.ADT.ReadLaterItem;
import com.example.mborzenkov.readlaterlist.R;

import java.util.List;

public class ItemListAdapter extends ArrayAdapter<ReadLaterItem> {

    // TODO: комментарии

    private final Context mContext;

    public interface ItemListAdapterOnClickHandler {
        void onClick(int itemId);
    }

    public ItemListAdapter(Context context, int resource, List<ReadLaterItem> dataForAdapter) {
        super(context, resource, dataForAdapter);
        mContext = context;
    }

    private class ItemListViewHolder {
        private final int id;
        private final TextView labelTextView;
        private final TextView descriptionTextView;
        private final ImageView colorImageView;

        ItemListViewHolder (View view, int id) {
            labelTextView = (TextView) view.findViewById(R.id.tv_item_label);
            descriptionTextView = (TextView) view.findViewById(R.id.tv_item_description);
            colorImageView = (ImageView) view.findViewById(R.id.iv_item_color);
            this.id = id;
        }
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ItemListViewHolder viewHolder;
        if (convertView == null) {
            convertView = LayoutInflater.from(mContext).inflate(R.layout.content_main_list_item, parent, false);
            viewHolder = new ItemListViewHolder(convertView, position);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ItemListViewHolder) convertView.getTag();
        }

        ReadLaterItem item = getItem(position);

        if (item != null) {
            viewHolder.labelTextView.setText(item.getLabel());
            viewHolder.descriptionTextView.setText(item.getDescription());
            ((GradientDrawable) viewHolder.colorImageView.getBackground()).setColor(item.getColor());
        }
        return convertView;
    }
}
