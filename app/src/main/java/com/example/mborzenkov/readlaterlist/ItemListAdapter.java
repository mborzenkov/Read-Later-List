package com.example.mborzenkov.readlaterlist;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import java.util.ArrayList;
import java.util.List;

public class ItemListAdapter extends ArrayAdapter<ReadLaterItem> {

    public ItemListAdapter(Context context, int textViewResourceId) {
        super(context, textViewResourceId);
    }

    public ItemListAdapter(Context context, int resource, List<ReadLaterItem> dataForAdapter) {
        super(context, resource, dataForAdapter);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        return null;
    }
}
