package com.example.mborzenkov.readlaterlist.activity.main;

import android.app.SearchManager;
import android.content.Intent;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;

import com.example.mborzenkov.readlaterlist.R;
import com.example.mborzenkov.readlaterlist.activity.EditItemActivity;
import com.example.mborzenkov.readlaterlist.adt.ReadLaterItem;
import com.example.mborzenkov.readlaterlist.adt.ReadLaterItemDbAdapter;
import com.example.mborzenkov.readlaterlist.adt.ReadLaterItemParcelable;
import com.example.mborzenkov.readlaterlist.utility.ReadLaterDbUtils;

/** Главная Activity, представляющая собой список. */
public class MainListActivity extends AppCompatActivity implements
        MainListAdapter.ItemListAdapterOnClickHandler,
        SearchView.OnQueryTextListener {

    // Константы
    /** Формат даты для вывода на формах редактирования дат. */
    public static final String FORMAT_DATE = "dd/MM/yy";

    // Intent
    /** Константа, обозначающая пустой UID. */
    private static final int UID_EMPTY = -1;
    /** ID запроса для создания нового элемента. */
    private static final int ITEM_ADD_NEW_REQUEST = 1;
    /** ID запроса для редактирования элемента. */
    private static final int ITEM_EDIT_REQUEST = 2;

    // Хэлперы
    private MainListAdapter mMainListAdapter;
    private MainListDrawerHelper mDrawerHelper;
    private MainListLoaderManager mLoaderManager;

    // Элементы layout
    private ListView mItemListView;
    private ProgressBar mLoadingIndicator;
    private LinearLayout mEmptyList;

    /** ID текущего редактируемого элемента. */
    private int mEditItemId = UID_EMPTY;

    /** Признак долгой загрузки, все действия должны быть заблокированы. */
    private boolean longLoading = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mainlist);

        // Инициализация Toolbar
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_main_list);
        toolbar.setTitleTextColor(ContextCompat.getColor(this, R.color.icons));
        setSupportActionBar(toolbar);
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab_item_add);
        fab.setOnClickListener(view -> {
            // Создание нового элемента
            Intent newItemIntent = new Intent(MainListActivity.this, EditItemActivity.class);
            startActivityForResult(newItemIntent, ITEM_ADD_NEW_REQUEST);
        });

        // Инициализация объектов layout
        mMainListAdapter = new MainListAdapter(this, this);
        mItemListView = (ListView) findViewById(R.id.listview_main_list);
        mItemListView.setAdapter(mMainListAdapter);
        mLoadingIndicator = (ProgressBar) findViewById(R.id.pb_main_loading);
        mEmptyList = (LinearLayout) findViewById(R.id.linearLayout_emptylist);

        // Инициализация Drawer Layout
        mDrawerHelper = new MainListDrawerHelper(this);

        // Начать загрузку данных
        mLoaderManager = new MainListLoaderManager(this);
        getSupportLoaderManager().initLoader(MainListLoaderManager.ITEM_LOADER_ID, null, mLoaderManager);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        getMenuInflater().inflate(R.menu.menu_mainlist, menu);

        // Создание меню поиска
        SearchManager searchManager = (SearchManager) getSystemService(SEARCH_SERVICE);
        SearchView searchView = (SearchView) menu.findItem(R.id.mainlist_action_search).getActionView();
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        searchView.setSubmitButtonEnabled(true);
        searchView.setOnQueryTextListener(this);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.mainlist_settings:
                mDrawerHelper.openDrawer();
                return true;
            default:
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        mLoaderManager.setSearchQuery(query);
        return false;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        mLoaderManager.setSearchQuery(newText);
        return false;
    }

    @Override
    public void onClick(int position) {
        // При нажатии на элемент, открываем EditItemActivity Activity для его редактирования
        Cursor cursor = mMainListAdapter.getCursor();
        if (cursor != null) {
            cursor.moveToPosition(position);
            mEditItemId = cursor.getInt(MainListLoaderManager.INDEX_COLUMN_ID);
            Intent editItemIntent = new Intent(MainListActivity.this, EditItemActivity.class);
            ReadLaterItemDbAdapter dbAdapter = new ReadLaterItemDbAdapter();
            ReadLaterItem data = dbAdapter.itemFromCursor(cursor);
            editItemIntent.putExtra(ReadLaterItemParcelable.KEY_EXTRA, new ReadLaterItemParcelable(data));
            startActivityForResult(editItemIntent, ITEM_EDIT_REQUEST);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        // Каждый раз при окончании редактирования, сбрасываем переменную mEditItemId
        int uid = mEditItemId;
        mEditItemId = UID_EMPTY;

        // Обрабатывает возврат от EditItemActivity
        if (resultCode == RESULT_OK && data != null && data.hasExtra(ReadLaterItemParcelable.KEY_EXTRA)) {
            // Возвращенные данные в формате ReadLaterItem
            ReadLaterItemParcelable parcelableData =
                    (ReadLaterItemParcelable) data.getParcelableExtra(ReadLaterItemParcelable.KEY_EXTRA);
            ReadLaterItem resultData = parcelableData == null ? null : parcelableData.getItem();
            switch (requestCode) {
                case ITEM_ADD_NEW_REQUEST:
                    if (resultData != null) {
                        // Добавляет новый элемент в базу, показывает снэкбар
                        new BackgroundTask().execute(
                                () -> ReadLaterDbUtils.insertItem(MainListActivity.this, resultData),
                                null,
                                null
                        );
                        Snackbar.make(mItemListView, getString(R.string.snackbar_item_added),
                                Snackbar.LENGTH_LONG).show();
                        return;
                    }
                    break;
                case ITEM_EDIT_REQUEST:
                    if (uid != UID_EMPTY) {
                        if (resultData == null) {
                            // Удаляет элемент, показывает снэкбар
                            new BackgroundTask().execute(
                                    () -> ReadLaterDbUtils.deleteItem(this, uid),
                                    null,
                                    null
                            );
                            Snackbar.make(mItemListView,
                                        getString(R.string.snackbar_item_removed), Snackbar.LENGTH_LONG).show();
                        } else {
                            // Изменяет элемент
                            new BackgroundTask().execute(
                                    () -> ReadLaterDbUtils.updateItem(MainListActivity.this, resultData, uid),
                                    null,
                                    null
                            );
                            Snackbar.make(mItemListView,
                                        getString(R.string.snackbar_item_edited), Snackbar.LENGTH_LONG).show();
                        }
                        return;
                    }
                    break;
                default:
                    break;
            }
        }

        // Этот блок вызывается при простом просмотре, тк при успешном случае с ADD_NEW или EDIT, уже был вызван return
        if (uid != UID_EMPTY) {
            new BackgroundTask().execute(
                    () -> ReadLaterDbUtils.updateItemViewDate(MainListActivity.this, uid),
                    null,
                    null
            );
        }

    }

    /** Показывает индикатор загрузки, скрывая все лишнее. */
    private void showLoading() {
        mItemListView.setVisibility(View.INVISIBLE);
        mEmptyList.setVisibility(View.INVISIBLE);
        mLoadingIndicator.setVisibility(View.VISIBLE);
    }

    /** Показывает онбординг, если список пуст или список, если он не пуст. */
    void showDataView() {
        mLoadingIndicator.setVisibility(View.INVISIBLE);
        if (mMainListAdapter.getCursor().getCount() > 0) {
            mEmptyList.setVisibility(View.INVISIBLE);
            mItemListView.setVisibility(View.VISIBLE);
        } else {
            mItemListView.setVisibility(View.INVISIBLE);
            mEmptyList.setVisibility(View.VISIBLE);
        }
    }

    // TODO: 1. reloadData убрать
    // TODO: 2. BlockingActionsLongBackgroundTask заменить на другое

    /** Запускает AsyncTask для выполнения быстрого действия.
     * Действие не будет выполнено, если уже выполняется длительное действие (isInLoadingMode == true).
     */
    private class BackgroundTask extends AsyncTask<Runnable, Void, Void> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            if (!longLoading) {
                showLoading();
            }
        }

        @Override
        protected Void doInBackground(Runnable... backgroundTask) {
            backgroundTask[0].run();
            return null;
        }

        @Override
        protected void onPostExecute(Void taskResult) {
            super.onPostExecute(taskResult);
            if (!longLoading) {
                mLoaderManager.reloadData();
                // Вызывает showDataView по окончанию
            }
        }

    }

    /** Начинает выполнение длительного действия.
     * Действие не будет выполнено, если уже выполняется другое длительное действие (isInLoadingMode == true).
     * Отклоняет другие длительные действия до оконачния выполнения.
     *
     * @param task действие, которое нужно выполнить
     * @return true, если действие запущено и false, если отклонено
     * @see BlockingActionsLongBackgroundTask
     */
    synchronized boolean startLongBackgroundTask(Runnable task) {
        // Может выполняться только одно действие
        if (longLoading) {
            return false;
        }
        longLoading = true;
        new BlockingActionsLongBackgroundTask().execute(task, null, null);
        return true;
    }

    /** Проверяет, выполняется ли сейчас длительное действие.
     *
     * @return true или false, результат
     */
    synchronized boolean isInLoadingMode() {
        return longLoading;
    }

    /** Запускает AsyncTask для выполнения длительного действия.
     * Показывает значок загрузки и устанавливает longLoading = true, что должно блокировать все другие действия.
     * Показывает notification с прогрессом выполнения.
     * По окончанию разблокирует интерфейс и обновляет список.
     */
    private class BlockingActionsLongBackgroundTask extends AsyncTask<Runnable, Integer, Void> {

        @Override
        protected void onPreExecute() {
            showLoading();
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
        }

        @Override
        protected Void doInBackground(Runnable... backgroundTask) {
            backgroundTask[0].run();
            return null;
        }

        @Override
        protected void onPostExecute(Void onFinishTask) {
            longLoading = false;
            mLoaderManager.reloadData();
        }

    }

    /** Перезагружает данные в Activity. */
    void reloadData() {
        mLoaderManager.reloadData();
    }

    /** Подменяет курсон у адаптера на новый.
     *
     * @param newCursor новый курсор или null
     */
    void changeCursorInAdapter(@Nullable Cursor newCursor) {
        mMainListAdapter.changeCursor(newCursor);
    }

}
