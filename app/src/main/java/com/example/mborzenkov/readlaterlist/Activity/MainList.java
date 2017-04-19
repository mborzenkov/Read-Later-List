package com.example.mborzenkov.readlaterlist.Activity;

import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;

import com.example.mborzenkov.readlaterlist.ADT.ReadLaterItem;
import com.example.mborzenkov.readlaterlist.R;
import com.example.mborzenkov.readlaterlist.data.ReadLaterContentProvider;
import com.example.mborzenkov.readlaterlist.data.ReadLaterContract;
import com.example.mborzenkov.readlaterlist.data.ReadLaterDbHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MainList extends AppCompatActivity implements
        LoaderManager.LoaderCallbacks<Cursor>,
        ItemListAdapter.ItemListAdapterOnClickHandler {

    protected static final int ITEM_ADD_NEW_REQUEST = 1;
    protected static final int ITEM_EDIT_REQUEST = 2;

    protected static final String[] MAIN_LIST_PROJECTION = {
            ReadLaterContract.ReadLaterEntry._ID,
            ReadLaterContract.ReadLaterEntry.COLUMN_LABEL,
            ReadLaterContract.ReadLaterEntry.COLUMN_DESCRIPTION,
            ReadLaterContract.ReadLaterEntry.COLUMN_COLOR
    };
    protected static final int INDEX_COLUMN_ID = 0;
    protected static final int INDEX_COLUMN_LABEL = 1;
    protected static final int INDEX_COLUMN_DESCRIPTION = 2;
    protected static final int INDEX_COLUMN_COLOR = 3;

    private static final int ITEM_LOADER_ID = 13;

    private ItemListAdapter mItemListAdapter;
    private ListView mItemListView;
    private ProgressBar mLoadingIndicator;
    private LinearLayout mEmptyList;
    private Cursor mDataCursor;
    private int mPosition = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_list);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_main_list);
        toolbar.setTitleTextColor(Color.WHITE);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab_item_add);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent newItemIntent = new Intent(MainList.this, EditItem.class);
                startActivityForResult(newItemIntent, ITEM_ADD_NEW_REQUEST);
            }
        });

        mItemListAdapter = new ItemListAdapter(this, R.layout.content_main_list_item, null, 0, this);
        mItemListView = (ListView) findViewById(R.id.listview_main_list);
        mItemListView.setAdapter(mItemListAdapter);
        mLoadingIndicator = (ProgressBar) findViewById(R.id.pb_main_loading);
        mEmptyList = (LinearLayout) findViewById(R.id.linearLayout_emptylist);

        showLoading();

        getSupportLoaderManager().initLoader(ITEM_LOADER_ID, null, this);
    }

    @Override
    public void onClick(int position) {
        mPosition = position;
        Intent editItemIntent = new Intent(MainList.this, EditItem.class);
        mDataCursor.moveToPosition(position);
        ReadLaterItem data = new ReadLaterItem(mDataCursor.getString(INDEX_COLUMN_LABEL), mDataCursor.getString(INDEX_COLUMN_DESCRIPTION), mDataCursor.getInt(INDEX_COLUMN_COLOR));
        editItemIntent.putExtra(ReadLaterItem.KEY_EXTRA, data);
        editItemIntent.putExtra(ReadLaterItem.KEY_UID, mDataCursor.getInt(INDEX_COLUMN_ID));
        startActivityForResult(editItemIntent, ITEM_EDIT_REQUEST);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int loaderId, final Bundle args) {
        switch (loaderId) {

            case ITEM_LOADER_ID:
                Uri itemsQueryUri = ReadLaterContract.ReadLaterEntry.CONTENT_URI;
                String sortOrder = ReadLaterContract.ReadLaterEntry._ID + " ASC";
                return new CursorLoader(this, itemsQueryUri, MAIN_LIST_PROJECTION, null, null, sortOrder);
            default:
                throw new RuntimeException("Loader Not Implemented: " + loaderId);
        }
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        mDataCursor = data;
        mItemListAdapter.changeCursor(mDataCursor);
        mItemListView.smoothScrollToPosition(mPosition);
        showDataView();
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mDataCursor = null;
        mItemListAdapter.changeCursor(mDataCursor);
    }

    private void showDataView() {
        Log.i("INFO", "Cursor count: " + mDataCursor.getCount());
        mLoadingIndicator.setVisibility(View.INVISIBLE);
        if (mDataCursor.getCount() > 0) {
            mEmptyList.setVisibility(View.INVISIBLE);
            mItemListView.setVisibility(View.VISIBLE);
        } else {
            mItemListView.setVisibility(View.INVISIBLE);
            mEmptyList.setVisibility(View.VISIBLE);
        }
    }

    private void showLoading() {
        mItemListView.setVisibility(View.INVISIBLE);
        mEmptyList.setVisibility(View.INVISIBLE);
        mLoadingIndicator.setVisibility(View.VISIBLE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK && data != null && data.hasExtra(ReadLaterItem.KEY_EXTRA)) {
            ReadLaterItem resultData = data.getParcelableExtra(ReadLaterItem.KEY_EXTRA);
            switch (requestCode) {
                case ITEM_ADD_NEW_REQUEST:
                    if (resultData != null) {
                        ContentValues contentValues = new ContentValues();
                        contentValues.put(ReadLaterContract.ReadLaterEntry.COLUMN_LABEL, resultData.getLabel());
                        contentValues.put(ReadLaterContract.ReadLaterEntry.COLUMN_DESCRIPTION, resultData.getDescription());
                        contentValues.put(ReadLaterContract.ReadLaterEntry.COLUMN_COLOR, resultData.getColor());
                        Uri uri = getContentResolver().insert(ReadLaterContract.ReadLaterEntry.CONTENT_URI, contentValues);
                        if (uri != null) {
                            Snackbar.make(mItemListView, "Добавлен новый элемент", Snackbar.LENGTH_LONG).show();
                            getSupportLoaderManager().restartLoader(ITEM_LOADER_ID, null, this);
                        }
                    }
                    break;
                case ITEM_EDIT_REQUEST:
                    if (data.hasExtra(ReadLaterItem.KEY_UID)) {
                        int uid = data.getIntExtra(ReadLaterItem.KEY_UID, -1);
                        if (resultData == null) {
                            int deleted = getContentResolver().delete(ReadLaterContract.ReadLaterEntry.buildUriForOneItem(uid), null, null);
                            if (deleted > 0) {
                                Snackbar.make(mItemListView, "Элемент удален", Snackbar.LENGTH_LONG).show();
                                showDataView();
                            }
                        } else {
                            ContentValues contentValues = new ContentValues();
                            contentValues.put(ReadLaterContract.ReadLaterEntry.COLUMN_LABEL, resultData.getLabel());
                            contentValues.put(ReadLaterContract.ReadLaterEntry.COLUMN_DESCRIPTION, resultData.getDescription());
                            contentValues.put(ReadLaterContract.ReadLaterEntry.COLUMN_COLOR, resultData.getColor());
                            int updated = getContentResolver().update(ReadLaterContract.ReadLaterEntry.buildUriForOneItem(uid), contentValues, null, null);
                            if (updated > 0) {
                                Snackbar.make(mItemListView, "Элемент изменен", Snackbar.LENGTH_LONG).show();
                            }
                        }
                        mItemListAdapter.notifyDataSetChanged();
                    }
                    break;
            }
        }
    }
}
