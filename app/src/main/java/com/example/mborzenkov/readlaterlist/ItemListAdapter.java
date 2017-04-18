package com.example.mborzenkov.readlaterlist;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class ItemListAdapter extends ArrayAdapter<ReadLaterItem> {

    // TODO: комментарии

    public ItemListAdapter(Context context, int textViewResourceId) {
        super(context, textViewResourceId);
    }

    public ItemListAdapter(Context context, int resource, List<ReadLaterItem> dataForAdapter) {
        super(context, resource, dataForAdapter);
    }

    private static class ViewHolder {
        private TextView labelTextView;
        private TextView descriptionTextView;
        private ImageView colorImageView;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder viewHolder;
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.content_main_list_item, parent, false);
            viewHolder = new ViewHolder();
            viewHolder.labelTextView = (TextView) convertView.findViewById(R.id.tv_item_label);
            viewHolder.descriptionTextView = (TextView) convertView.findViewById(R.id.tv_item_description);
            viewHolder.colorImageView = (ImageView) convertView.findViewById(R.id.iv_item_color);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
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
