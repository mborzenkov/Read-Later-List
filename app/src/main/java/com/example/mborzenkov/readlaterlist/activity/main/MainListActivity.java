package com.example.mborzenkov.readlaterlist.activity.main;

import android.app.SearchManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
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
import com.example.mborzenkov.readlaterlist.adt.UserInfo;
import com.example.mborzenkov.readlaterlist.networking.CloudSyncTask;
import com.example.mborzenkov.readlaterlist.utility.ReadLaterDbUtils;

import java.util.List;

/** Главная Activity, представляющая собой список. */
public class MainListActivity extends AppCompatActivity implements
        MainListAdapter.ItemListAdapterOnClickHandler,
        SearchView.OnQueryTextListener,
        CloudSyncTask.SyncCallback,
        MainListConflictFragment.ConflictsCallback {

    /** Константа запроса разрешения на чтение файлов. */
    public static final int PERMISSION_READ_EXTERNAL_STORAGE = 101;
    /** Константа запроса разрешения на запись файлов. */
    public static final int PERMISSION_WRITE_EXTERNAL_STORAGE = 102;

    // Константы
    /** Длительность показа значка синхронизации в мс. */
    private static final int SYNC_ICON_DURATION = 1000;

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
    private MainListSyncFragment mSyncFragment;
    private InternetBroadcastReceiver mInternetBroadcastReceiver;
    private IntentFilter mInternetChangedIntentFilter;

    // Элементы layout
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private ListView mItemListView;
    private ProgressBar mLoadingIndicator;
    private LinearLayout mEmptyList;

    /** ID текущего редактируемого элемента. */
    private int mEditItemId = UID_EMPTY;
    /** Дата последней синхронизации. */
    private long mLastSync;

    /** Класс {@link android.content.BroadcastReceiver} для получения бродкаста об изменении сети.
     *  Запускает синхронизацию при подключении к интернету.
     *
     */
    private class InternetBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (isNetworkConnected()) {
                toggleSync();
            }
        }
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
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
        mSwipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swiperefresh_mainlist);
        mSwipeRefreshLayout.setOnRefreshListener(this::toggleSync);
        mMainListAdapter = new MainListAdapter(this, this);
        mItemListView = (ListView) findViewById(R.id.listview_main_list);
        mItemListView.setAdapter(mMainListAdapter);
        mLoadingIndicator = (ProgressBar) findViewById(R.id.pb_main_loading);
        mEmptyList = (LinearLayout) findViewById(R.id.linearLayout_emptylist);

        // Инициализация SyncFragment
        mSyncFragment = MainListSyncFragment.getInstance(getSupportFragmentManager());

        // Инициализация BroadcastReceiver
        mInternetBroadcastReceiver = new InternetBroadcastReceiver();
        mInternetChangedIntentFilter = new IntentFilter();
        mInternetChangedIntentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);

        // Инициализация Drawer Layout
        mDrawerHelper = new MainListDrawerHelper(this);

        // Начать загрузку данных
        mLoaderManager = new MainListLoaderManager(this);
        if (!MainListLongTask.isActive()) {
            mLoaderManager.reloadData();
        } else {
            MainListLongTask.swapActivity(this);
            showLoading();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        toggleSync();
        registerReceiver(mInternetBroadcastReceiver, mInternetChangedIntentFilter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mInternetBroadcastReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        MainListLongTask.swapActivity(null);
    }

    @Override
    public boolean onCreateOptionsMenu(@NonNull Menu menu) {

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
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.mainlist_settings:
                mDrawerHelper.openDrawer();
                return true;
            case R.id.mainlist_action_refresh:
                toggleSync();
                return true;
            default:
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onQueryTextSubmit(@NonNull String query) {
        mLoaderManager.setSearchQuery(query);
        return false;
    }

    @Override
    public boolean onQueryTextChange(@NonNull String newText) {
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
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {

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

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_WRITE_EXTERNAL_STORAGE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    mDrawerHelper.startBackupSaving();
                }
                break;
            case PERMISSION_READ_EXTERNAL_STORAGE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    mDrawerHelper.startBackupRestoring();
                }
                break;
            default:
                break;
        }
    }

    /** Вызывает начало синхронизации. */
    void toggleSync() {
        mSwipeRefreshLayout.setRefreshing(true);
        mSyncFragment.startFullSync();
        mSwipeRefreshLayout.postDelayed(() -> mSwipeRefreshLayout.setRefreshing(false), SYNC_ICON_DURATION);
    }

    @Override
    public long getLastSync() {
        // Читаем дату последней синхронизации
        SharedPreferences sharedPreferences = getSharedPreferences(LAST_SYNC_KEY, Context.MODE_PRIVATE);
        return sharedPreferences.getLong(String.valueOf(UserInfo.getCurentUser(this).getUserId()), 0);
    }

    @Override
    public boolean isNetworkConnected() {
        NetworkInfo networkInfo = ((ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE))
                .getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isConnected();
    }

    @Override
    public void onSyncWithConflicts(@Nullable List<ReadLaterItem[]> conflicts, long syncStartTime) {
        mLastSync = syncStartTime;
        if (conflicts != null && !conflicts.isEmpty()) {
            MainListConflictFragment conflictFragment =
                    MainListConflictFragment.getInstance(conflicts);
            conflictFragment.show(getSupportFragmentManager(), "dsds");
        } else {
            updateLastSyncDate(mLastSync);
            finishSync();
        }
    }

    @Override
    public void saveConflict(@NonNull ReadLaterItem item) {
        HandlerThread handlerThread = new HandlerThread("BackupHandlerThread");
        handlerThread.start();
        Looper looper = handlerThread.getLooper();
        Handler handler = new Handler(looper);
        handler.post(() -> {
            int remoteId = item.getRemoteId();
            int userId = UserInfo.getCurentUser(this).getUserId();
            if (remoteId > 0) {
                if (CloudSyncTask.updateItemOnServer(CloudSyncTask.prepareApi(), userId, remoteId, item)) {
                    ReadLaterDbUtils.updateItemByRemoteId(this, userId, item, remoteId);
                }
            }
        });
    }

    @Override
    public void onConflictsMerged() {
        updateLastSyncDate(mLastSync);
        finishSync();
    }

    @Override
    public void onSyncFailed() {
        finishSync();
    }

    @Override
    public void onSyncSuccess(long syncStartTime) {
        mLastSync = syncStartTime;
        updateLastSyncDate(mLastSync);
        finishSync();
    }

    /** Обновляет дату последней синхронизации в SharedPreferences.
     *
     * @param lastSyncDate дата, которую нужно сохранить
     */
    private void updateLastSyncDate(long lastSyncDate) {
        SharedPreferences.Editor sharedPreferencesEditor =
                getSharedPreferences(LAST_SYNC_KEY, Context.MODE_PRIVATE).edit();
        sharedPreferencesEditor.putLong(String.valueOf(UserInfo.getCurentUser(this).getUserId()), lastSyncDate);
        sharedPreferencesEditor.apply();
        mLastSync = lastSyncDate;
    }

    /** Завершает синхронизацию принудительно. */
    private void finishSync() {
        mSyncFragment.stopSync();
        mLoaderManager.reloadData();
    }

    /** Показывает индикатор загрузки, скрывая все лишнее. */
    void showLoading() {
        mItemListView.setVisibility(View.INVISIBLE);
        mEmptyList.setVisibility(View.INVISIBLE);
        mLoadingIndicator.setVisibility(View.VISIBLE);
        mSwipeRefreshLayout.setEnabled(false);
    }

    /** Показывает онбординг, если список пуст или список, если он не пуст. */
    void showDataView() {
        mLoadingIndicator.setVisibility(View.INVISIBLE);
        if (mMainListAdapter.getCursor().getCount() > 0) {
            mEmptyList.setVisibility(View.INVISIBLE);
            mItemListView.setVisibility(View.VISIBLE);
            mSwipeRefreshLayout.setEnabled(true);
        } else {
            mItemListView.setVisibility(View.INVISIBLE);
            mEmptyList.setVisibility(View.VISIBLE);
            mSwipeRefreshLayout.setEnabled(false);
        }
    }
    
    /** Запускает AsyncTask для выполнения быстрого действия.
     * Действие не будет выполнено, если уже выполняется длительное действие (isInLoadingMode == true).
     */
    private class BackgroundTask extends AsyncTask<Runnable, Void, Void> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            if (!MainListLongTask.isActive()) {
                showLoading();
            }
        }

        @Override
        protected Void doInBackground(@NonNull Runnable... backgroundTask) {
            backgroundTask[0].run();
            return null;
        }

        @Override
        protected void onPostExecute(Void taskResult) {
            super.onPostExecute(taskResult);
            if (!MainListLongTask.isActive()) {
                mLoaderManager.reloadData();
                // Вызывает showDataView по окончанию
            }
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
