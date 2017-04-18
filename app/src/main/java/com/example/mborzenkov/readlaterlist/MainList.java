package com.example.mborzenkov.readlaterlist;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class MainList extends AppCompatActivity {

    // TODO: Когда список пуст, показать сообщение о необходимости добавления (лучше layout)
    // TODO: Запихать все в ресурсы, все строки!

    private static final int ITEM_ADD_NEW_REQUEST = 1;
    private static final int ITEM_EDIT_REQUEST = 2;

    private List<ReadLaterItem> mAllData;
    private ListView mItemListView;
    private ItemListAdapter mItemListAdapter;

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
                Intent editItemIntent = new Intent(MainList.this, EditItem.class);
                startActivityForResult(editItemIntent, ITEM_ADD_NEW_REQUEST);
            }
        });

        mItemListView = (ListView) findViewById(R.id.listview_main_list);
        mAllData = new ArrayList<>();
        mAllData.add(new ReadLaterItem("Заголовок 1", "Описание 1", Color.RED));
        mAllData.add(new ReadLaterItem("Заголовок 2", "Описание 2", Color.GREEN));
        mAllData.add(new ReadLaterItem("Заголовок 3", "Описание 3", Color.BLUE));
        mItemListAdapter = new ItemListAdapter(this, R.layout.content_main_list_item, mAllData);
        mItemListView.setAdapter(mItemListAdapter);
        mItemListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent editItemIntent = new Intent(MainList.this, EditItem.class);
                editItemIntent.putExtra(Intent.EXTRA_INDEX, position);
                startActivityForResult(editItemIntent, ITEM_EDIT_REQUEST);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case ITEM_ADD_NEW_REQUEST:
                    // TODO: Спарсить полученные данные
//                mAllData.add(new ReadLaterItem("Заголовок NEW", "Описание NEW", Color.MAGENTA));
//                mItemListAdapter.notifyDataSetChanged();
                    Snackbar.make(mItemListView, "Добавлен новый элемент", Snackbar.LENGTH_LONG).show();
                    break;
                case ITEM_EDIT_REQUEST:
                    // TODO: Спарсить полученные данные & remove
                    break;
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main_list, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
