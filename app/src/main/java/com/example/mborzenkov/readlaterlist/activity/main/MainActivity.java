package com.example.mborzenkov.readlaterlist.activity.main;

import android.app.SearchManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;

import com.example.mborzenkov.readlaterlist.R;
import com.example.mborzenkov.readlaterlist.activity.EditItemActivity;
import com.example.mborzenkov.readlaterlist.adt.ReadLaterItem;
import com.example.mborzenkov.readlaterlist.adt.ReadLaterItemParcelable;
import com.example.mborzenkov.readlaterlist.adt.UserInfo;
import com.example.mborzenkov.readlaterlist.fragments.ConflictsFragment;
import com.example.mborzenkov.readlaterlist.fragments.FilterDrawerFragment;
import com.example.mborzenkov.readlaterlist.fragments.sync.SyncFragment;
import com.example.mborzenkov.readlaterlist.fragments.itemlist.ItemListFragment;
import com.example.mborzenkov.readlaterlist.fragments.sync.SyncAsyncTask;
import com.example.mborzenkov.readlaterlist.utility.DebugUtils;
import com.example.mborzenkov.readlaterlist.utility.LongTaskNotifications;
import com.example.mborzenkov.readlaterlist.utility.MainListBackupUtils;
import com.example.mborzenkov.readlaterlist.utility.ReadLaterDbUtils;

import java.util.List;

/** Главная Activity, представляющая собой список. */
public class MainActivity extends AppCompatActivity implements
        SearchView.OnQueryTextListener,
        SyncAsyncTask.SyncCallback,
        ConflictsFragment.ConflictsCallback,
        ItemListFragment.ItemListCallbacks,
        FilterDrawerFragment.DrawerCallbacks {

    // Константы
    /** ID контейнера для помещения фрагментов. */
    private static final @IdRes int FRAGMENT_CONTAINER = R.id.fragmentcontainer_mainactivity;

    // Intent
    /** Константа, обозначающая пустой UID. */
    private static final int UID_EMPTY = -1;
    /** ID запроса для создания нового элемента. */
    private static final int ITEM_ADD_NEW_REQUEST = 1;
    /** ID запроса для редактирования элемента. */
    private static final int ITEM_EDIT_REQUEST = 2;

    // Хэлперы
    private ItemListFragment mItemListFragment;
    private FilterDrawerFragment mFilterDrawerFragment;
    private SyncFragment mSyncFragment;
    private InternetBroadcastReceiver mInternetBroadcastReceiver;
    private IntentFilter mInternetChangedIntentFilter;

    // Элементы layout
    private DrawerLayout mDrawerLayout;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private ProgressBar mLoadingIndicator;
    private FloatingActionButton mFloatingAddButton;

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
            if (isNetworkConnected() && !MainActivityLongTask.isActive()) {
                toggleSync();
            }
        }
    }


    /////////////////////////
    // Колбеки Activity

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Инициализация Toolbar
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_mainactivity);
        toolbar.setTitleTextColor(ContextCompat.getColor(this, R.color.icons));
        setSupportActionBar(toolbar);

        // Инициализируем FloatingActionButton
        mFloatingAddButton = (FloatingActionButton) findViewById(R.id.fab_item_add);
        mFloatingAddButton.setOnClickListener(view -> {
            getSupportFragmentManager().beginTransaction().hide(mItemListFragment).commit();
            Intent newItemIntent = new Intent(MainActivity.this, EditItemActivity.class);
            startActivityForResult(newItemIntent, ITEM_ADD_NEW_REQUEST);
        });

        // Инициализация объектов layout
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawerlayout_mainlist);
        mSwipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.fragmentcontainer_mainactivity);
        mSwipeRefreshLayout.setOnRefreshListener(this::toggleSync);
        mLoadingIndicator = (ProgressBar) findViewById(R.id.pb_mainactivity_loading);

        // Инициализациия ItemListFragment
        FragmentManager fragmentManager = getSupportFragmentManager();
        mItemListFragment = ItemListFragment.getInstance(fragmentManager);

        if (savedInstanceState == null) {
            fragmentManager.beginTransaction()
                    .add(FRAGMENT_CONTAINER, mItemListFragment, ItemListFragment.TAG).commit();
        }

        // Инициализация SyncFragment
        mSyncFragment = SyncFragment.getInstance(fragmentManager);

        // Инициализация BroadcastReceiver
        mInternetBroadcastReceiver = new InternetBroadcastReceiver();
        mInternetChangedIntentFilter = new IntentFilter();
        mInternetChangedIntentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);

        // Инициализация Drawer Layout и обработчика открытия и закрытия Drawer
        mFilterDrawerFragment = (FilterDrawerFragment) fragmentManager.findFragmentByTag(FilterDrawerFragment.TAG);
        //
        ActionBarDrawerToggle drawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout,
                R.string.mainlist_drawer_title, R.string.mainlist_drawer_title) {

            @Override
            public void onDrawerClosed(View view) {
                // При закрытии - устанавливаем фильтр
                super.onDrawerClosed(view);
                if (!MainActivityLongTask.isActive()) {
                    mItemListFragment.reloadData();
                }
            }

            @Override
            public void onDrawerOpened(View drawerView) {
                //  При открытии - обновляем Drawer на основании фильтра
                super.onDrawerOpened(drawerView);
                mFilterDrawerFragment.reloadDataFromCurrentFilter();
            }
        };
        mDrawerLayout.addDrawerListener(drawerToggle);

        // Проверяет, запущена ли длительная операция
        if (MainActivityLongTask.isActive()) {
            // Если запущена, нужно подменить на новую Activity
            MainActivityLongTask.swapActivity(this);
            showLoading();
        } else if (!mItemListFragment.dataIsLoaded()) {
            // Загружает данные локальные в список, если еще не были загружены
            mItemListFragment.reloadData();
        }

    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!MainActivityLongTask.isActive()) {
            // Вызывает синхронизацию данных с сервером, которая по окончанию вызовет обновление списка.
            toggleSync();
        }
        registerReceiver(mInternetBroadcastReceiver, mInternetChangedIntentFilter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mInternetBroadcastReceiver);
    }

    @Override
    protected void onDestroy() {
        MainActivityLongTask.swapActivity(null);
        super.onDestroy();
    }


    /////////////////////////
    // Колбеки Menu

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
                mDrawerLayout.openDrawer(Gravity.END);
                return true;
            case R.id.mainlist_action_refresh:
                toggleSync();
                return true;
            default:
                break;
        }

        return super.onOptionsItemSelected(item);
    }


    /////////////////////////
    // Колбеки ввода текста (search)

    @Override
    public boolean onQueryTextSubmit(@NonNull String query) {
        if (mItemListFragment.isVisible() && !MainActivityLongTask.isActive()) {
            mItemListFragment.toggleSearch(query);
        }
        return false;
    }

    @Override
    public boolean onQueryTextChange(@NonNull String newText) {
        if (mItemListFragment.isVisible() && !MainActivityLongTask.isActive()) {
            mItemListFragment.toggleSearch(newText);
        }
        return false;
    }


    /////////////////////////
    // Колбеки Intent

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
                                () -> {
                                    ReadLaterDbUtils.insertItem(MainActivity.this, resultData);
                                    if (!MainActivityLongTask.isActive()) {
                                        runOnUiThread(mItemListFragment::reloadData);
                                    }
                                },
                                null,
                                null
                        );
                        Snackbar.make(mSwipeRefreshLayout, getString(R.string.snackbar_item_added),
                                Snackbar.LENGTH_LONG).show();
                        return;
                    }
                    break;
                case ITEM_EDIT_REQUEST:
                    if (uid != UID_EMPTY) {
                        if (resultData == null) {
                            // Удаляет элемент, показывает снэкбар
                            new BackgroundTask().execute(
                                    () -> {
                                        ReadLaterDbUtils.deleteItem(this, uid);
                                        if (!MainActivityLongTask.isActive()) {
                                            runOnUiThread(mItemListFragment::reloadData);
                                        }
                                    },
                                    null,
                                    null
                            );
                            Snackbar.make(mSwipeRefreshLayout,
                                        getString(R.string.snackbar_item_removed), Snackbar.LENGTH_LONG).show();
                        } else {
                            // Изменяет элемент
                            new BackgroundTask().execute(
                                    () -> {
                                        ReadLaterDbUtils.updateItem(MainActivity.this, resultData, uid);
                                        if (!MainActivityLongTask.isActive()) {
                                            runOnUiThread(mItemListFragment::reloadData);
                                        }
                                    },
                                    null,
                                    null
                            );
                            Snackbar.make(mSwipeRefreshLayout,
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
                    () -> ReadLaterDbUtils.updateItemViewDate(MainActivity.this, uid),
                    null,
                    null
            );
        }

        // В случае, если ничего не изменилось, список сразу не перезагружается.

    }


    /////////////////////////
    // Колбеки SyncAsyncTask

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
            ConflictsFragment conflictFragment =
                    ConflictsFragment.getInstance(conflicts);
            conflictFragment.show(getSupportFragmentManager(), "fragment_conflicts");
        } else {
            updateLastSyncDate(mLastSync);
            finishSync();
        }
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


    /////////////////////////
    // Колбеки ConflictsFragment

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
                if (SyncAsyncTask.updateItemOnServer(SyncAsyncTask.prepareApi(), userId, remoteId, item)) {
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


    /////////////////////////
    // Колбеки ItemListFragment

    @Override
    public void onItemClick(@NonNull ReadLaterItem item, int localId) {
        mEditItemId = localId;
        getSupportFragmentManager().beginTransaction().hide(mItemListFragment).commit();
        Intent editItemIntent = new Intent(MainActivity.this, EditItemActivity.class);
        editItemIntent.putExtra(ReadLaterItemParcelable.KEY_EXTRA, new ReadLaterItemParcelable(item));
        startActivityForResult(editItemIntent, ITEM_EDIT_REQUEST);
    }

    @Override
    public void onItemListReloaded(boolean isEmpty) {
        // Показывает онбординг, если список пуст или список, если он не пуст.
        mLoadingIndicator.setVisibility(View.INVISIBLE);
        mFloatingAddButton.setVisibility(View.VISIBLE);
        mSwipeRefreshLayout.setEnabled(true);
        getSupportFragmentManager().beginTransaction().show(mItemListFragment).commit();
    }


    /////////////////////////
    // Колбеки FilterDrawerFragment

    @Override
    public void closeDrawer() {
        mDrawerLayout.closeDrawer(Gravity.END);
    }

    @Override
    public void onUserChanged() {
        toggleSync();
    }

    @Override
    public void onFillPlaceholdersChosen(int count) {
        // Запускаем таск, показываем нотификейшены
        if (MainActivityLongTask.startLongBackgroundTask(
                () -> DebugUtils.addPlaceholdersToDatabase(this, count),
                this)) {
            showLoading();
        }
    }

    @Override
    public void onDeleteAllChosen() {
        // Запускаем таск, показываем нотификейшены
        if (MainActivityLongTask.startLongBackgroundTask(
                () -> {
                    ReadLaterDbUtils.deleteAll(this);
                    LongTaskNotifications.cancelNotification();
                },
                this)) {
            showLoading();
        }
    }

    @Override
    public void onBackupSaveChosen() {
        handleBackupTask(true);
    }

    @Override
    public void onBackupRestoreChosen() {
        handleBackupTask(false);
    }

    @Override
    public boolean isLongTaskActive() {
        return MainActivityLongTask.isActive();
    }


    /////////////////////////
    // Методы синхронизации

    /** Вызывает начало синхронизации. */
    private void toggleSync() {
        mSwipeRefreshLayout.setRefreshing(true);
        mSyncFragment.startFullSync();
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
        if (!MainActivityLongTask.isActive()) {
            mItemListFragment.reloadData();
        }
        mSwipeRefreshLayout.setRefreshing(false);
    }


    /////////////////////////
    // Методы сохранения и восстановления из бэкапа
    /** Выполняет сохранение или восстановление бэкапов в фоновом потоке.
     *
     * @param savingMode true - режим сохранения данных, false - режим восстановления
     */
    private void handleBackupTask(boolean savingMode) {

        // Пробуем заблокировать интерфейс
        if (!MainActivityLongTask.startAnotherLongTask()) {
            return; // не удалось, что то уже происходит
        }

        // Запускаем поток
        HandlerThread handlerThread = new HandlerThread("BackupHandlerThread");
        handlerThread.start();
        Looper looper = handlerThread.getLooper();
        Handler handler = new Handler(looper);

        // Выполняем работу
        if (savingMode) {
            handler.post(() -> {
                MainListBackupUtils.saveEverythingAsJsonFile(this);
                MainActivityLongTask.stopAnotherLongTask();
            });
        } else {
            showLoading();
            handler.post(() -> {
                MainListBackupUtils.restoreEverythingFromJsonFile(this);
                if (MainActivityLongTask.stopAnotherLongTask()) {
                    runOnUiThread(mItemListFragment::reloadData);
                }
            });
        }

    }


    /////////////////////////
    // Все остальное

    /** Колбек для MainActivityLongTask об окончании работ. */
    void onLongTaskFinished() {
        mItemListFragment.reloadData();
    }

    /** Показывает индикатор загрузки, скрывая все лишнее. */
    private void showLoading() {
        getSupportFragmentManager().beginTransaction().hide(mItemListFragment).commit();
        mLoadingIndicator.setVisibility(View.VISIBLE);
        mSwipeRefreshLayout.setEnabled(false);
        mFloatingAddButton.setVisibility(View.INVISIBLE);
    }

    /** Запускает AsyncTask для выполнения быстрого действия.
     * Действие не будет выполнено, если уже выполняется длительное действие (isInLoadingMode == true).
     */
    private class BackgroundTask extends AsyncTask<Runnable, Void, Void> {

        @Override
        protected Void doInBackground(@NonNull Runnable... backgroundTask) {
            backgroundTask[0].run();
            return null;
        }

        @Override
        protected void onPostExecute(Void taskResult) {
            super.onPostExecute(taskResult);
            if (!MainActivityLongTask.isActive()) {
                mItemListFragment.reloadData();
            }
        }

    }

}
