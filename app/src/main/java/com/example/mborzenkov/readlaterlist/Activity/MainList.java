package com.example.mborzenkov.readlaterlist.Activity;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import com.example.mborzenkov.readlaterlist.ADT.ReadLaterItem;
import com.example.mborzenkov.readlaterlist.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MainList extends AppCompatActivity {

    // TODO: Когда список пуст, показать сообщение о необходимости добавления (лучше layout)
    // TODO: Запихать все в ресурсы, все строки!
    // TODO: Добавить уменьшенное описание

    private static final int ITEM_ADD_NEW_REQUEST = 1;
    private static final int ITEM_EDIT_REQUEST = 2;

    private static List<ReadLaterItem> allData = new ArrayList<>();
    private ListView mItemListView;
    private ItemListAdapter mItemListAdapter;

    static {
        Random randomizer = new Random();
        for (int i = 0; i < 100; i++) {
            float[] colorHSV = new float[3];
            Color.colorToHSV(randomizer.nextInt(), colorHSV);
            allData.add(new ReadLaterItem("Заголовок " + i, "Описание " + i, Color.HSVToColor(colorHSV)));
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_list);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_main_list);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab_item_add);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent newItemIntent = new Intent(MainList.this, EditItem.class);
                startActivityForResult(newItemIntent, ITEM_ADD_NEW_REQUEST);
            }
        });

        mItemListView = (ListView) findViewById(R.id.listview_main_list);
        mItemListAdapter = new ItemListAdapter(this, R.layout.content_main_list_item, allData);
        mItemListView.setAdapter(mItemListAdapter);
        mItemListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent editItemIntent = new Intent(MainList.this, EditItem.class);
                editItemIntent.putExtra(ReadLaterItem.KEY_EXTRA, allData.get(position));
                editItemIntent.putExtra(ReadLaterItem.KEY_UID, position);
                startActivityForResult(editItemIntent, ITEM_EDIT_REQUEST);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK && data != null && data.hasExtra(ReadLaterItem.KEY_EXTRA)) {
            ReadLaterItem resultData = data.getParcelableExtra(ReadLaterItem.KEY_EXTRA);
            switch (requestCode) {
                case ITEM_ADD_NEW_REQUEST:
                    if (resultData != null) {
                        allData.add(resultData);
                        mItemListAdapter.notifyDataSetChanged();
                        Snackbar.make(mItemListView, "Добавлен новый элемент", Snackbar.LENGTH_LONG).show();
                    }
                    break;
                case ITEM_EDIT_REQUEST:
                    if (data.hasExtra(ReadLaterItem.KEY_UID)) {
                        int uid = data.getIntExtra(ReadLaterItem.KEY_UID, -1);
                        if (resultData == null) {
                            allData.remove(uid);
                            Snackbar.make(mItemListView, "Элемент удален", Snackbar.LENGTH_LONG).show();
                        } else {
                            allData.set(uid, resultData);
                            Snackbar.make(mItemListView, "Элемент изменен", Snackbar.LENGTH_LONG).show();
                        }
                        mItemListAdapter.notifyDataSetChanged();
                    }
                    break;
            }
        }
    }
}
