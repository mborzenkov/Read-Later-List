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

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = convertView;
        if (view == null) {
            LayoutInflater layoutInflater = LayoutInflater.from(getContext());
            view = layoutInflater.inflate(R.layout.content_main_list_item, null);
        }

        ReadLaterItem item = getItem(position);

        if (item != null) {
            TextView label = (TextView) view.findViewById(R.id.tv_item_label);
            TextView description = (TextView) view.findViewById(R.id.tv_item_description);
            ImageView color = (ImageView) view.findViewById(R.id.iv_item_color);

            label.setText(item.getLabel());
            description.setText(item.getDescription());
            ((GradientDrawable) color.getBackground()).setColor(item.getColor());
        }
        return view;
    }
}
