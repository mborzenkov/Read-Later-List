package com.example.mborzenkov.readlaterlist;

import android.graphics.Color;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.List;

public class MainList extends AppCompatActivity {

    // TODO: Когда список пуст, показать сообщение о необходимости добавления (лучше layout)

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

        ListView itemListView = (ListView) findViewById(R.id.itemListView);
        List<ReadLaterItem> data = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            data.add(new ReadLaterItem("Заголовок 1", "Описание 1", Color.RED));
            data.add(new ReadLaterItem("Заголовок 2", "Описание 2", Color.GREEN));
            data.add(new ReadLaterItem("Заголовок 3", "Описание 3", Color.BLUE));
        }
        ItemListAdapter itemListAdapter = new ItemListAdapter(this, R.layout.content_main_list_item, data);
        itemListView.setAdapter(itemListAdapter);
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
