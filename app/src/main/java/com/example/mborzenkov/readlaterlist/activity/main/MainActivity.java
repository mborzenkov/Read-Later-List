package com.example.mborzenkov.readlaterlist.activity.main;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.IdRes;
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.Size;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.InputFilter;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ProgressBar;

import com.example.mborzenkov.readlaterlist.BuildConfig;
import com.example.mborzenkov.readlaterlist.R;
import com.example.mborzenkov.readlaterlist.adt.ReadLaterItem;
import com.example.mborzenkov.readlaterlist.adt.UserInfo;
import com.example.mborzenkov.readlaterlist.fragments.ConflictsFragment;
import com.example.mborzenkov.readlaterlist.fragments.EditItemFragment;
import com.example.mborzenkov.readlaterlist.fragments.FilterDrawerFragment;
import com.example.mborzenkov.readlaterlist.fragments.itemlist.ItemListFragment;
import com.example.mborzenkov.readlaterlist.fragments.sync.SyncAsyncTask;
import com.example.mborzenkov.readlaterlist.fragments.sync.SyncFragment;
import com.example.mborzenkov.readlaterlist.utility.ActivityUtils;
import com.example.mborzenkov.readlaterlist.utility.DebugUtils;
import com.example.mborzenkov.readlaterlist.utility.LongTaskNotifications;
import com.example.mborzenkov.readlaterlist.utility.MainListBackupUtils;
import com.example.mborzenkov.readlaterlist.utility.ReadLaterDbUtils;

import java.util.List;

/** Главная Activity, представляющая собой список. */
public class MainActivity extends AppCompatActivity implements
        SyncAsyncTask.SyncCallback,
        ConflictsFragment.ConflictsCallback,
        ItemListFragment.ItemListCallbacks,
        EditItemFragment.EditItemCallbacks,
        FilterDrawerFragment.DrawerCallbacks {

    // [v.0.7.0]
    // TODO: Проверить все на выполнение не на UI Thread (missing frames - причина виртуалки или где-то косяки?)
    // TODO: Проверить алгоритм синхронизации:
    //          Есть явные проблемы, если нет доступа к интернету - не вызовется finishSync и не вызовется reload.
    //          Они должны быть независимыми друг от друга (reload вызывать несколько раз видимо).
    // TODO: Fatal Exception SQLiteQuery при редактировании и обратно и особенно при конфликтах
    //          SELECT _id, label, description, color, created, last_modify, last_view,
    //          image_url, remote_id FROM items WHERE user_id = ? ORDER BY last_modify DESC
    // TODO: Serch работает только на 23
    // TODO: Исключить обращение к synchronized методам из MainThread

    /////////////////////////
    // Константы

    /** ID контейнера для помещения фрагментов. */
    private static final @IdRes int FRAGMENT_CONTAINER = R.id.fragmentcontainer_mainactivity;

    /** Тэг для HandlerThread. */
    private static final String HANDLERTHREAD_TAG = "mainactivity_handlerthread";


    /////////////////////////
    // Поля объекта

    // Хэлперы
    private SyncFragment mSyncFragment;
    private InternetBroadcastReceiver mInternetBroadcastReceiver;
    private IntentFilter mInternetChangedIntentFilter;
    private HandlerThread mHandlerThread;
    private Handler mHandlerThreadHandler;

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

        // Инициализация SyncFragment
        mSyncFragment = SyncFragment.getInstance(fragmentManager);

        // Инициализация BroadcastReceiver
        mInternetBroadcastReceiver = new InternetBroadcastReceiver();
        mInternetChangedIntentFilter = new IntentFilter();
        mInternetChangedIntentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);

        // Инициализация полезного HandlerThread
        mHandlerThread = new HandlerThread(HANDLERTHREAD_TAG);
        mHandlerThread.start();
        mHandlerThreadHandler = new Handler(mHandlerThread.getLooper());


        // Проверяет, запущена ли длительная операция
        if (MainActivityLongTask.isActive()) {
            // Если запущена, нужно подменить на новую Activity
            MainActivityLongTask.swapActivity(this);
            showLoading();
        }

        // Если это запуск с 0, добавляем itemlistFragment и синхронизируем activity
        if (savedInstanceState == null) {
            fragmentManager.beginTransaction()
                    .add(FRAGMENT_CONTAINER, itemListFragment, ItemListFragment.TAG)
                    .commit();
        }

    }

    @Override
    protected void onResume() {
        super.onResume();
        // Вызывает синхронизацию данных с сервером, которая по окончанию вызовет обновление списка.
        if (!MainActivityLongTask.isActive()) {
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
    protected void onStop() {
        super.onStop();
        // Синхронизируемся на всякий случай, но не при смене ориентации (там синхронизируемся в onResume)
        if (!isChangingConfigurations()) {
            if (!MainActivityLongTask.isActive()) {
                toggleSync();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        MainActivityLongTask.swapActivity(null);
        mHandlerThread.quit();
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
    public void onSyncWithConflicts(@NonNull @Size(min = 1) List<ReadLaterItem[]> conflicts, long syncStartTime) {
        mLastSync = syncStartTime;
        ConflictsFragment conflictFragment =
                ConflictsFragment.getInstance(conflicts);
        conflictFragment.show(getSupportFragmentManager(), "fragment_conflicts");
    }

    @Override
    public void onSyncFailed() {
        finishSync();
    }

    @Override
    public void onSyncSuccess(long syncStartTime) {
        mLastSync = syncStartTime;
        mHandlerThreadHandler.post(() -> updateLastSyncDate(mLastSync));
        finishSync();
    }


    /////////////////////////
    // Колбеки ConflictsFragment

    @Override
    public void saveConflict(@NonNull ReadLaterItem item) {
        final int remoteId = item.getRemoteId();
        if (remoteId > 0) {
            mHandlerThreadHandler.post(() -> {
                final int userId = UserInfo.getCurentUser(MainActivity.this).getUserId();
                if (mSyncFragment.updateOneItem(item, userId, remoteId)) {
                    ReadLaterDbUtils.updateItem(this, item, userId, remoteId);
                }
            });
        }
    }

    @Override
    public void onConflictsMerged() {
        onSyncSuccess(mLastSync);
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

    /////////////////////////
    // Колбеки ItemListFragment

    @Override
    public void onNewItemClick() {
        FragmentManager fragmentManager = getSupportFragmentManager();
        EditItemFragment editItemFragment = EditItemFragment.getInstance(
                fragmentManager, null, EditItemFragment.UID_EMPTY);
        fragmentManager.beginTransaction()
                .replace(FRAGMENT_CONTAINER, editItemFragment, EditItemFragment.TAG)
                .addToBackStack(null).commit();
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
    // Колбеки FilterDrawerLayout

    @Override
    public void onActionToggled(FilterDrawerFragment.DrawerActions action) {

        // Если выполняется какая-то работа, кнопки не работают, показывается предупреждение.
        if (MainActivityLongTask.isActive()) {
            ActivityUtils.showAlertDialog(this,
                    getString(R.string.mainlist_longloading_title),
                    getString(R.string.mainlist_longloading_text),
                    null,
                    null);
            return;
        }

        switch (action) {
            case BACKUP_SAVE:
                // Действие "Сохранить бэкап" открывает окно подтверждения и по положительному ответу
                // вызывает функцию для сохранения
                ActivityUtils.showAlertDialog(
                        this,
                        getString(R.string.mainlist_drawer_backup_save_question_title),
                        getString(R.string.mainlist_drawer_backup_save_question_text),
                    () -> {
                        mSyncFragment.stopSync();
                        MainActivityLongTask.startLongBackgroundTask(
                            () -> MainListBackupUtils.saveEverythingAsJsonFile(this),
                                this);
                        showLoading();
                    },
                        null);
                break;
            case BACKUP_RESTORE:
                // Действие "Восстановить из бэкапа" открывает окно подтверждения и по положительному ответу
                // вызывает функцию для восстановления
                ActivityUtils.showAlertDialog(
                        this,
                        getString(R.string.mainlist_drawer_backup_restore_question_title),
                        getString(R.string.mainlist_drawer_backup_restore_question_text),
                    () -> {
                        mSyncFragment.stopSync();
                        MainActivityLongTask.startLongBackgroundTask(
                            () -> MainListBackupUtils.restoreEverythingFromJsonFile(this),
                                this);
                        showLoading();
                    },
                        null);
                break;
            case FILL_PLACEHOLDERS:
                // Действие "Заполнить данными" открывает окно подтверждения и по положительному ответу
                // вызывает функцию для заполнения
                if (BuildConfig.DEBUG) {
                    EditText inputNumber = new EditText(this);
                    inputNumber.setInputType(InputType.TYPE_CLASS_NUMBER);
                    inputNumber.setFilters(new InputFilter[] {new InputFilter.LengthFilter(1)}); // 0-9
                    ActivityUtils.showInputTextDialog(
                            this,
                            inputNumber,
                            getString(R.string.mainlist_menu_add_placeholders_question_title),
                            getString(R.string.mainlist_menu_add_placeholders_question_text),
                        (input) -> {
                            try {
                                // Смотрим введенное значение
                                int count = Integer.parseInt(input);
                                // Запускаем бэкграунд таск
                                mSyncFragment.stopSync();
                                MainActivityLongTask.startLongBackgroundTask(
                                    () -> DebugUtils.addPlaceholdersToDatabase(this, count),
                                        this);
                                showLoading();
                            } catch (NumberFormatException e) {
                                Log.e("CAST ERROR", "Ошибка преобразования ввода пользователя в число");
                            }
                        },
                            null);
                }
                break;
            case DELETE_ALL:
                // Действие "Удалить все" открывает окно подтверждения и по положительному ответу
                // вызывает функцию для очистки
                if (BuildConfig.DEBUG) {
                    ActivityUtils.showAlertDialog(
                            this,
                            getString(R.string.mainlist_menu_delete_all_question_title),
                            getString(R.string.mainlist_menu_delete_all_question_text),
                        () -> {
                            mSyncFragment.stopSync();
                            MainActivityLongTask.startLongBackgroundTask(
                                () -> {
                                    ReadLaterDbUtils.deleteAll(this);
                                    LongTaskNotifications.cancelNotification();
                                },
                                    this);
                            showLoading();
                        },
                            null);
                }
                break;
            default:
                break;
        }

    }

    @Override
    public void onUserChanged() {
        toggleSync();
    }


    /////////////////////////
    // Колбеки EditItemFragment

    @Override
    public void onCreateNewItem(@NonNull ReadLaterItem item) {
        mHandlerThreadHandler.post(() -> ReadLaterDbUtils.insertItem(MainActivity.this, item));
        popFragmentFromBackstack();
        Snackbar.make(mFragmentContainer, getString(R.string.snackbar_item_added), Snackbar.LENGTH_LONG).show();
    }

    @Override
    public void onSaveItem(@NonNull ReadLaterItem item, @IntRange(from = 0) int localId) {
        mHandlerThreadHandler.post(() -> ReadLaterDbUtils.updateItem(MainActivity.this, item, localId));
        popFragmentFromBackstack();
        Snackbar.make(mFragmentContainer, getString(R.string.snackbar_item_edited), Snackbar.LENGTH_LONG).show();
    }

    @Override
    public void onDeleteItem(@IntRange(from = 0) int localId) {
        mHandlerThreadHandler.post(() -> ReadLaterDbUtils.deleteItem(this, localId));
        popFragmentFromBackstack();
        Snackbar.make(mFragmentContainer, getString(R.string.snackbar_item_removed), Snackbar.LENGTH_LONG).show();
    }

    @Override
    public void onExitWithoutModifying(@Nullable ReadLaterItem item,
                                       @IntRange(from = EditItemFragment.UID_EMPTY) int localId) {

        if (item != null) {
            // Этот блок вызывается при простом просмотре без изменений
            mHandlerThreadHandler.post(() -> ReadLaterDbUtils.updateItemViewDate(MainActivity.this, localId));
        }
        popFragmentFromBackstack();

    }


    /////////////////////////
    // Методы синхронизации

    /** Вызывает начало синхронизации.
     * Синхронизация будет запущена, если не выполняется LongTask.
     * По окончанию синхронизации при любом исходе вызывается finishSync.
     */
    private void toggleSync() {
        if (!MainActivityLongTask.isActive()) {
            ItemListFragment itemListFragment =
                    (ItemListFragment) getSupportFragmentManager().findFragmentByTag(ItemListFragment.TAG);
            if ((itemListFragment != null) && itemListFragment.isVisible()) {
                itemListFragment.setRefreshing(true);
            }
            mSyncFragment.startFullSync();
        }
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

    /** Завершает синхронизацию. */
    private void finishSync() {
        mSyncFragment.stopSync();
        ItemListFragment itemListFragment =
                (ItemListFragment) getSupportFragmentManager().findFragmentByTag(ItemListFragment.TAG);
        if ((itemListFragment != null) && itemListFragment.isVisible()) {
            itemListFragment.setRefreshing(false);
            // Оповещаем itemListFragment об изменениях, если не запущен лонг таск
            if (!MainActivityLongTask.isActive()) {
                itemListFragment.onDataChanged();
            }
        }
    }


    /////////////////////////
    // Колбеки MainActivityLongTask

    /** Колбек для MainActivityLongTask об окончании работ. */
    void onLongTaskFinished() {
        ItemListFragment itemListFragment =
                (ItemListFragment) getSupportFragmentManager().findFragmentByTag(ItemListFragment.TAG);
        if (itemListFragment != null) {
            // Оповещаем itemListFragment об изменениях
            itemListFragment.onDataChanged();
        }
        showData();
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

    /** Показывает данные. */
    private void showData() {
        FragmentManager fragmentManager = getSupportFragmentManager();
        Fragment currentFragment = fragmentManager.findFragmentById(FRAGMENT_CONTAINER);
        getSupportFragmentManager().beginTransaction().show(currentFragment).commit();
        mLoadingIndicator.setVisibility(View.INVISIBLE);
    }

    /** Убирает последний фрагмент из бэкстака и скрывает клавиатуру. */
    private void popFragmentFromBackstack() {
        View focus = getCurrentFocus();
        if (focus != null) {
            InputMethodManager mgr = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            mgr.hideSoftInputFromWindow(focus.getWindowToken(), 0);
        }
        getSupportFragmentManager().popBackStack();
    }

}
