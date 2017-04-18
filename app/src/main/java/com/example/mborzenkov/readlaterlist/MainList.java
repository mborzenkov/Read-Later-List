package com.example.mborzenkov.readlaterlist;

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
    // TODO: Запихать все в ресурсы

    private List<ReadLaterItem> mAllData;
    private ListView mItemListView;
    private ItemListAdapter mItemListAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_list);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        mItemListView = (ListView) findViewById(R.id.itemListView);
        mAllData = new ArrayList<>();
        mAllData.add(new ReadLaterItem("Заголовок 1", "Описание 1", Color.RED));
        mAllData.add(new ReadLaterItem("Заголовок 2", "Описание 2", Color.GREEN));
        mAllData.add(new ReadLaterItem("Заголовок 3", "Описание 3", Color.BLUE));
        mItemListAdapter = new ItemListAdapter(this, R.layout.content_main_list_item, mAllData);
        mItemListView.setAdapter(mItemListAdapter);
        mItemListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Log.i("CLICK", "Click on " + position);
                mAllData.add(new ReadLaterItem("Заголовок NEW", "Описание NEW", Color.MAGENTA));
                mItemListAdapter.notifyDataSetChanged();
            }
        });
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
