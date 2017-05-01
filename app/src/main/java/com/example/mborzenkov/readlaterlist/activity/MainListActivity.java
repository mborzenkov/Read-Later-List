package com.example.mborzenkov.readlaterlist.activity;

import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;

import com.example.mborzenkov.readlaterlist.adt.ReadLaterItem;
import com.example.mborzenkov.readlaterlist.adt.ReadLaterItemParcelable;
import com.example.mborzenkov.readlaterlist.BuildConfig;
import com.example.mborzenkov.readlaterlist.R;
import com.example.mborzenkov.readlaterlist.data.ReadLaterContract;
import com.example.mborzenkov.readlaterlist.utility.ReadLaterDbUtils;

/**
 * Главная Activity, представляющая собой список
 */
public class MainListActivity extends AppCompatActivity implements
        LoaderManager.LoaderCallbacks<Cursor>,
        ItemListAdapter.ItemListAdapterOnClickHandler {

    /** ID запроса для создания нового элемента */
    protected static final int ITEM_ADD_NEW_REQUEST = 1;
    /** ID запроса для редактирования элемента */
    protected static final int ITEM_EDIT_REQUEST = 2;

    /** Используемые в MainListActivity колонки базы данных */
    protected static final String[] MAIN_LIST_PROJECTION = {
            ReadLaterContract.ReadLaterEntry._ID,
            ReadLaterContract.ReadLaterEntry.COLUMN_LABEL,
            ReadLaterContract.ReadLaterEntry.COLUMN_DESCRIPTION,
            ReadLaterContract.ReadLaterEntry.COLUMN_COLOR
    };

    // Индексы для колонок из MAIN_LIST_PROJECTION, для упрощения
    protected static final int INDEX_COLUMN_ID = 0;
    protected static final int INDEX_COLUMN_LABEL = 1;
    protected static final int INDEX_COLUMN_DESCRIPTION = 2;
    protected static final int INDEX_COLUMN_COLOR = 3;

    /** ID Используемого LoadManager'а */
    private static final int ITEM_LOADER_ID = 13;

    // Элементы layout
    private ItemListAdapter mItemListAdapter;
    private ListView mItemListView;
    private ProgressBar mLoadingIndicator;
    private LinearLayout mEmptyList;

    /** Cursor с данными */
    private Cursor mDataCursor;
    /** Текущая позиция mDataCursor */
    private int mPosition = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_list);

        // Инициализация Toolbar
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_main_list);
        toolbar.setTitleTextColor(ContextCompat.getColor(this, R.color.icons));
        setSupportActionBar(toolbar);
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab_item_add);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Создание нового элемента
                Intent newItemIntent = new Intent(MainListActivity.this, EditItemActivity.class);
                startActivityForResult(newItemIntent, ITEM_ADD_NEW_REQUEST);
            }
        });

        // Инициализация объектов layout
        mItemListAdapter = new ItemListAdapter(this, R.layout.content_main_list_item, null, 0, this);
        mItemListView = (ListView) findViewById(R.id.listview_main_list);
        mItemListView.setAdapter(mItemListAdapter);
        mLoadingIndicator = (ProgressBar) findViewById(R.id.pb_main_loading);
        mEmptyList = (LinearLayout) findViewById(R.id.linearLayout_emptylist);

        // Показать иконку загрузки
        showLoading();

        // Начать загрузку данных
        getSupportLoaderManager().initLoader(ITEM_LOADER_ID, null, this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Специальные возможности создаются только в DEBUG
        if (BuildConfig.DEBUG) {
            getMenuInflater().inflate(R.menu.menu_main_list, menu);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.mainlist_action_add_placeholders:
                // Действие "Заполнить данными" открывает окно подтверждения и по положительному ответу вызывает функцию для заполнения
                new AlertDialog.Builder(this)
                        .setTitle(getString(R.string.mainlist_menu_add_placeholders_question_title))
                        .setMessage(getString(R.string.mainlist_menu_add_placeholders_question_text))
                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                ReadLaterDbUtils.addPlaceholdersToDatabase(MainListActivity.this);
                                getSupportLoaderManager().restartLoader(ITEM_LOADER_ID, null, MainListActivity.this);
                            }
                        })
                        .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                // do nothing
                            }
                        })
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .show();
                return true;
            case R.id.mainlist_action_delete_all:
                // Действие "Удалить все" открывает окно подтверждения и по положительному ответу вызывает функцию для очистки
                new AlertDialog.Builder(this)
                        .setTitle(getString(R.string.mainlist_menu_delete_all_question_title))
                        .setMessage(getString(R.string.mainlist_menu_delete_all_question_text))
                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                ReadLaterDbUtils.deleteItemsFromDatabase(MainListActivity.this, mDataCursor, INDEX_COLUMN_ID);
                                getSupportLoaderManager().restartLoader(ITEM_LOADER_ID, null, MainListActivity.this);
                            }
                        })
                        .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                // do nothing
                            }
                        })
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .show();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int loaderId, final Bundle args) {
        switch (loaderId) {
            case ITEM_LOADER_ID:
                // Создаем новый CursorLoader, нужно все имеющееся в базе данных
                Uri itemsQueryUri = ReadLaterContract.ReadLaterEntry.CONTENT_URI;
                String sortOrder = ReadLaterContract.ReadLaterEntry._ID + " ASC";
                return new CursorLoader(this, itemsQueryUri, MAIN_LIST_PROJECTION, null, null, sortOrder);
            default:
                throw new RuntimeException("Loader Not Implemented: " + loaderId);
        }
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        // По завершению загрузки, подменяем Cursor в адаптере и показываем данные
        mDataCursor = data;
        mItemListAdapter.changeCursor(mDataCursor);
        mItemListView.smoothScrollToPosition(mPosition);
        showDataView();
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        // При сбросе загрузчика данных, сбрасываем данные
        mDataCursor = null;
        mItemListAdapter.changeCursor(mDataCursor);
    }

    @Override
    public void onClick(int position) {
        // При нажатии на элемент, открываем EditItemActivity Activity для его редактирования
        mPosition = position;
        Intent editItemIntent = new Intent(MainListActivity.this, EditItemActivity.class);
        mDataCursor.moveToPosition(position);
        ReadLaterItem data = new ReadLaterItem(mDataCursor.getString(INDEX_COLUMN_LABEL), mDataCursor.getString(INDEX_COLUMN_DESCRIPTION), mDataCursor.getInt(INDEX_COLUMN_COLOR));
        editItemIntent.putExtra(ReadLaterItemParcelable.KEY_EXTRA, new ReadLaterItemParcelable(data));
        editItemIntent.putExtra(ReadLaterItemParcelable.KEY_UID, mDataCursor.getInt(INDEX_COLUMN_ID));
        startActivityForResult(editItemIntent, ITEM_EDIT_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Обрабатывает возврат от EditItemActivity
        if (resultCode == RESULT_OK && data != null && data.hasExtra(ReadLaterItemParcelable.KEY_EXTRA)) {
            // Возвращенные данные в формате ReadLaterItem
            ReadLaterItem resultData = ((ReadLaterItemParcelable) data.getParcelableExtra(ReadLaterItemParcelable.KEY_EXTRA)).getItem();
            switch (requestCode) {
                case ITEM_ADD_NEW_REQUEST:
                    if (resultData != null) {
                        // Добавляет новый элемент в базу, показывает снэкбар
                        if (ReadLaterDbUtils.insertItem(MainListActivity.this, resultData)) {
                            Snackbar.make(mItemListView, getString(R.string.snackbar_item_added), Snackbar.LENGTH_LONG).show();
                            getSupportLoaderManager().restartLoader(ITEM_LOADER_ID, null, this);
                        }
                    }
                    break;
                case ITEM_EDIT_REQUEST:
                    if (data.hasExtra(ReadLaterItemParcelable.KEY_UID)) {
                        int uid = data.getIntExtra(ReadLaterItemParcelable.KEY_UID, -1);
                        if (resultData == null) {
                            // Удаляет элемент, показывает снэкбар
                            int deleted = getContentResolver().delete(ReadLaterContract.ReadLaterEntry.buildUriForOneItem(uid), null, null);
                            if (deleted > 0) {
                                Snackbar.make(mItemListView, getString(R.string.snackbar_item_removed), Snackbar.LENGTH_LONG).show();
                                showDataView();
                            }
                        } else {
                            // Изменяет элемент
                            if (ReadLaterDbUtils.updateItem(MainListActivity.this, resultData, uid)) {
                                Snackbar.make(mItemListView, getString(R.string.snackbar_item_edited), Snackbar.LENGTH_LONG).show();
                            }
                        }
                        getSupportLoaderManager().restartLoader(ITEM_LOADER_ID, null, this);
                    }
                    break;
            }
        }
    }

    /**
     * Показывает онбординг, если список пуст или список, если он не пуст
     */
    private void showDataView() {
        mLoadingIndicator.setVisibility(View.INVISIBLE);
        if (mDataCursor.getCount() > 0) {
            mEmptyList.setVisibility(View.INVISIBLE);
            mItemListView.setVisibility(View.VISIBLE);
        } else {
            mItemListView.setVisibility(View.INVISIBLE);
            mEmptyList.setVisibility(View.VISIBLE);
        }
    }

    /** Показывает индикатор загрузки, скрывая все лишнее */
    private void showLoading() {
        mItemListView.setVisibility(View.INVISIBLE);
        mEmptyList.setVisibility(View.INVISIBLE);
        mLoadingIndicator.setVisibility(View.VISIBLE);
    }







}
