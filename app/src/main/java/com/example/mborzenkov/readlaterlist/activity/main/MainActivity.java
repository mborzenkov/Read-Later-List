package com.example.mborzenkov.readlaterlist.activity.main;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ProgressBar;

import com.example.mborzenkov.readlaterlist.R;
import com.example.mborzenkov.readlaterlist.fragments.ColorPickerFragment;
import com.example.mborzenkov.readlaterlist.fragments.EditItemFragment;
import com.example.mborzenkov.readlaterlist.adt.ReadLaterItem;
import com.example.mborzenkov.readlaterlist.adt.UserInfo;
import com.example.mborzenkov.readlaterlist.data.ReadLaterContract;
import com.example.mborzenkov.readlaterlist.fragments.ConflictsFragment;
import com.example.mborzenkov.readlaterlist.fragments.OnBackPressedListener;
import com.example.mborzenkov.readlaterlist.fragments.itemlist.ItemListFragment;
import com.example.mborzenkov.readlaterlist.fragments.sync.SyncAsyncTask;
import com.example.mborzenkov.readlaterlist.fragments.sync.SyncFragment;
import com.example.mborzenkov.readlaterlist.utility.ReadLaterDbUtils;

import java.util.ArrayList;
import java.util.List;

/** Главная Activity, представляющая собой список. */
public class MainActivity extends AppCompatActivity implements
        SyncAsyncTask.SyncCallback,
        ConflictsFragment.ConflictsCallback,
        ItemListFragment.ItemListCallbacks,
        EditItemFragment.EditItemCallbacks {

    // [DrawerFragment]
    // TODO: Перезагрузка данных в правильных местах
    // TODO: Обработчик new item click
    // TODO: Вызов toggle sync при событиях в ItemListFragment
    // TODO: Првоерить SwipeRefreshLayout не будет активен, если нет колбеков (и кнопка невидимая)
    // TODO: Переделать бэкап таски и наполнение
    // TODO: Id может быть 0, надо поменять на -1 спец значение

    // [v.0.7.0]
    // TODO: Проверить все на выполнение не на UI Thread (missing frames - причина виртуалки или где-то косяки?)
    // TODO: Проверить алгоритм синхронизации:
    //          Есть явные проблемы, если нет доступа к интернету - не вызовется finishSync и не вызовется reload.
    //          Они должны быть независимыми друг от друга (reload вызывать несколько раз видимо).
    // TODO: Fatal Exception SQLiteQuery при редактировании и обратно и особенно при конфликтах
    //          SELECT _id, label, description, color, created, last_modify, last_view,
    //          image_url, remote_id FROM items WHERE user_id = ? ORDER BY last_modify DESC
    // TODO: Сохранение конфликта не обновляет его на другом устройстве (проверить даты)
    // TODO: Serch работает только на 23

    /////////////////////////
    // Константы

    /** ID контейнера для помещения фрагментов. */
    private static final @IdRes int FRAGMENT_CONTAINER = R.id.fragmentcontainer_mainactivity;

    // Intent
    /** Константа, обозначающая пустой UID. */
    private static final int UID_EMPTY = -1;


    /////////////////////////
    // Поля объекта

    // Хэлперы
    private SyncFragment mSyncFragment;
    private InternetBroadcastReceiver mInternetBroadcastReceiver;
    private IntentFilter mInternetChangedIntentFilter;

    // Элементы layout
    private FrameLayout mFragmentContainer;
    private ProgressBar mLoadingIndicator;

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

        // Инициализация объектов layout
        mFragmentContainer = (FrameLayout) findViewById(FRAGMENT_CONTAINER);
        mLoadingIndicator = (ProgressBar) findViewById(R.id.pb_mainactivity_loading);

        // Инициализациия ItemListFragment
        FragmentManager fragmentManager = getSupportFragmentManager();
        ItemListFragment itemListFragment = ItemListFragment.getInstance(fragmentManager);
        fragmentManager.beginTransaction()
                .add(FRAGMENT_CONTAINER, itemListFragment, ItemListFragment.TAG)
                .commit();

        // TODO: savedInstanceState

        // Инициализация SyncFragment
        mSyncFragment = SyncFragment.getInstance(fragmentManager);

        // Инициализация BroadcastReceiver
        mInternetBroadcastReceiver = new InternetBroadcastReceiver();
        mInternetChangedIntentFilter = new IntentFilter();
        mInternetChangedIntentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);

        // Проверяет, запущена ли длительная операция
        if (MainActivityLongTask.isActive()) {
            // Если запущена, нужно подменить на новую Activity
            MainActivityLongTask.swapActivity(this);
            showLoading();
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
    public void onBackPressed() {

        boolean backHandled = false;

        FragmentManager fragmentManager = getSupportFragmentManager();
        EditItemFragment editItemFragment =
                (EditItemFragment) fragmentManager.findFragmentByTag(EditItemFragment.TAG);
        if ((editItemFragment != null) && editItemFragment.isVisible()) {
            editItemFragment.onBackPressed();
            backHandled = true;
        }

        if (!backHandled) {
            super.onBackPressed();
        }

    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mInternetBroadcastReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        MainActivityLongTask.swapActivity(null);
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
        Log.e("SYNC", item.toString());
        final int remoteId = item.getRemoteId();
        if (remoteId > 0) {
            new BackgroundTask().execute(
                    () -> {
                        final int userId = UserInfo.getCurentUser(MainActivity.this).getUserId();
                        if (mSyncFragment.updateOneItem(item, userId, remoteId)) {
                            ReadLaterDbUtils.updateItemByRemoteId(this, userId, item, remoteId);
                        }
                        if (!MainActivityLongTask.isActive()) {
                            // runOnUiThread(mItemListFragment::reloadData);
                        }
                    },
                    null,
                    null
            );
        }
    }

    @Override
    public void onConflictsMerged() {
        updateLastSyncDate(mLastSync);
        finishSync();
    }


    /////////////////////////
    // Колбеки BasicFragmentCallbacks

    @Override
    public void setNewToolbar(@NonNull Toolbar toolbar, @NonNull String title) {
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(title);
            if (getSupportFragmentManager().getBackStackEntryCount() != 0) {
                actionBar.setDisplayHomeAsUpEnabled(true);
            }
        }
        invalidateOptionsMenu();
    }

    @Override
    public boolean isLongTaskActive() {
        return MainActivityLongTask.isActive();
    }

    /////////////////////////
    // Колбеки ItemListFragment

    @Override
    public void onNewItemClick() {
        /*
                getSupportFragmentManager().beginTransaction().hide(mItemListFragment).commit();
                Intent newItemIntent = new Intent(MainActivity.this, EditItemFragment.class);
                startActivityForResult(newItemIntent, ITEM_ADD_NEW_REQUEST);
         */
    }

    @Override
    public void onItemClick(@NonNull ReadLaterItem item, int localId) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        EditItemFragment editItemFragment = EditItemFragment.getInstance(fragmentManager, item, localId);
        fragmentManager.beginTransaction()
                .replace(FRAGMENT_CONTAINER, editItemFragment, EditItemFragment.TAG)
                .addToBackStack(null).commit();
    }

    @Override
    public void onRefreshToggled() {
        toggleSync();
    }


    /////////////////////////
    // Колбеки EditItemFragment

    @Override
    public void onCreateNewItem(@NonNull ReadLaterItem item) {

    }

    @Override
    public void onSaveItem(@NonNull ReadLaterItem item, @IntRange(from = 1) int localId) {

    }

    @Override
    public void onDeleteItem(@IntRange(from = 1) int localId) {

    }

//    @Override
//    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
//
//        // Каждый раз при окончании редактирования, сбрасываем переменную mEditItemId
//        int uid = mEditItemId;
//        mEditItemId = UID_EMPTY;
//
//        // Обрабатывает возврат от EditItemFragment
//        if (resultCode == RESULT_OK && data != null && data.hasExtra(ReadLaterItemParcelable.KEY_EXTRA)) {
//            // Возвращенные данные в формате ReadLaterItem
//            ReadLaterItemParcelable parcelableData = data.getParcelableExtra(ReadLaterItemParcelable.KEY_EXTRA);
//            ReadLaterItem resultData = parcelableData == null ? null : parcelableData.getItem();
//            switch (requestCode) {
//                case ITEM_ADD_NEW_REQUEST:
//                    if (resultData != null) {
//                        // Добавляет новый элемент в базу, показывает снэкбар
//                        new BackgroundTask().execute(
//                                () -> ReadLaterDbUtils.insertItem(MainActivity.this, resultData),
//                                null,
//                                null
//                        );
//                        Snackbar.make(mFragmentContainer, getString(R.string.snackbar_item_added),
//                                Snackbar.LENGTH_LONG).show();
//                        return;
//                    }
//                    break;
//                case ITEM_EDIT_REQUEST:
//                    if (uid != UID_EMPTY) {
//                        if (resultData == null) {
//                            // Удаляет элемент, показывает снэкбар
//                            new BackgroundTask().execute(
//                                    () -> ReadLaterDbUtils.deleteItem(this, uid),
//                                    null,
//                                    null
//                            );
//                            Snackbar.make(mFragmentContainer,
//                                    getString(R.string.snackbar_item_removed), Snackbar.LENGTH_LONG).show();
//                        } else {
//                            // Изменяет элемент
//                            new BackgroundTask().execute(
//                                    () -> ReadLaterDbUtils.updateItem(MainActivity.this, resultData, uid),
//                                    null,
//                                    null
//                            );
//                            Snackbar.make(mFragmentContainer,
//                                    getString(R.string.snackbar_item_edited), Snackbar.LENGTH_LONG).show();
//                        }
//                        return;
//                    }
//                    break;
//                default:
//                    break;
//            }
//        }
//
//        // Этот блок вызывается при простом просмотре, тк при успешном случае с ADD_NEW или EDIT, уже был вызван return
//        if (uid != UID_EMPTY) {
//            new BackgroundTask().execute(
//                    () -> ReadLaterDbUtils.updateItemViewDate(MainActivity.this, uid),
//                    null,
//                    null
//            );
//        }
//
//        // В случае, если ничего не изменилось, список сразу не перезагружается.
//
//    }

    /////////////////////////
    // Методы синхронизации

    /** Вызывает начало синхронизации. */
    private void toggleSync() {
//        mItemListFragment.setRefreshing(true);
//        mSyncFragment.startFullSync();
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
        getContentResolver().notifyChange(ReadLaterContract.ReadLaterEntry.CONTENT_URI, null);
//        mItemListFragment.setRefreshing(false);
    }


    /////////////////////////
    // Колбеки MainActivityLongTask

    /** Колбек для MainActivityLongTask об окончании работ. */
    void onLongTaskFinished() {
        // mItemListFragment.reloadData();
    }


    /////////////////////////
    // Все остальное

    /** Показывает индикатор загрузки, скрывая все лишнее. */
    private void showLoading() {
        FragmentManager fragmentManager = getSupportFragmentManager();
        Fragment currentFragment = fragmentManager.findFragmentById(FRAGMENT_CONTAINER);
        getSupportFragmentManager().beginTransaction().hide(currentFragment).commit();
        mLoadingIndicator.setVisibility(View.VISIBLE);
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
                // mItemListFragment.reloadData();
            }
        }

    }

}
