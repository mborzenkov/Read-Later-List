package com.example.mborzenkov.readlaterlist.activity.main;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
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
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.InputFilter;
import android.text.InputType;
import android.transition.TransitionInflater;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.example.mborzenkov.readlaterlist.BuildConfig;
import com.example.mborzenkov.readlaterlist.R;
import com.example.mborzenkov.readlaterlist.adt.Conflict;
import com.example.mborzenkov.readlaterlist.adt.ReadLaterItem;
import com.example.mborzenkov.readlaterlist.fragments.ColorPickerFragment;
import com.example.mborzenkov.readlaterlist.fragments.ConflictsFragment;
import com.example.mborzenkov.readlaterlist.fragments.filterdrawer.FilterDrawerCallbacks;
import com.example.mborzenkov.readlaterlist.fragments.filterdrawer.FilterDrawerFragment;
import com.example.mborzenkov.readlaterlist.fragments.edititem.EditItemFragmentActions;
import com.example.mborzenkov.readlaterlist.fragments.edititem.EditItemViewPagerFragment;
import com.example.mborzenkov.readlaterlist.fragments.itemlist.ItemListFragment;
import com.example.mborzenkov.readlaterlist.fragments.sync.SyncAsyncTask;
import com.example.mborzenkov.readlaterlist.fragments.sync.SyncFragment;
import com.example.mborzenkov.readlaterlist.utility.ActivityUtils;
import com.example.mborzenkov.readlaterlist.utility.DebugUtils;
import com.example.mborzenkov.readlaterlist.utility.LongTaskNotifications;
import com.example.mborzenkov.readlaterlist.utility.MainListBackupUtils;
import com.example.mborzenkov.readlaterlist.utility.ReadLaterDbUtils;
import com.example.mborzenkov.readlaterlist.utility.UserInfoUtils;

import java.util.List;

/** Главная Activity, представляющая собой список. */
public class MainActivity extends AppCompatActivity implements
        SyncAsyncTask.SyncCallback,
        ConflictsFragment.ConflictsCallback,
        ItemListFragment.ItemListCallbacks,
        EditItemFragmentActions.EditItemCallbacks,
        EditItemViewPagerFragment.EditItemViewPagerCallbacks,
        FilterDrawerCallbacks,
        ColorPickerFragment.ColorPickerCallbacks {


    /////////////////////////
    // Константы

    /** ID контейнера для помещения фрагментов. */
    private static final @IdRes int FRAGMENT_CONTAINER = R.id.fragmentcontainer_mainactivity;

    /** Тэг для HandlerThread. */
    private static final String HANDLERTHREAD_TAG = "mainactivity_handlerthread";

    /** Префикс для SharedElement. */
    public static final String SHARED_ELEMENT_COLOR_TRANSITION_NAME = "readlateritem_sharedelement_color";


    /////////////////////////
    // Поля объекта

    // Хэлперы
    private SyncFragment mSyncFragment;
    private InternetBroadcastReceiver mInternetBroadcastReceiver;
    private IntentFilter mInternetChangedIntentFilter;
    private HandlerThread mHandlerThread;
    private Handler mHandlerThreadHandler;
    private ItemListFragment mItemListFragment;

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
        mItemListFragment = ItemListFragment.getInstance(fragmentManager);

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
                    .add(FRAGMENT_CONTAINER, mItemListFragment, ItemListFragment.TAG)
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
        EditItemViewPagerFragment editItem =
                (EditItemViewPagerFragment) fragmentManager.findFragmentByTag(EditItemViewPagerFragment.TAG);
        if ((editItem != null) && (editItem.isVisible())) {
            editItem.onBackPressed();
            backHandled = true;
        } else {
            ColorPickerFragment colorPickerFragment =
                    (ColorPickerFragment) fragmentManager.findFragmentByTag(ColorPickerFragment.TAG);
            if ((colorPickerFragment != null) && (colorPickerFragment.isVisible())) {
                colorPickerFragment.onBackPressed();
                backHandled = true;
            }
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
        return sharedPreferences.getLong(String.valueOf(UserInfoUtils.getCurentUser(this).getUserId()), 0);
    }

    @Override
    public boolean isNetworkConnected() {
        NetworkInfo networkInfo = ((ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE))
                .getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isConnected();
    }

    @Override
    public void onSyncWithConflicts(@NonNull @Size(min = 1) List<Conflict> conflicts, long syncStartTime) {
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
        mHandlerThreadHandler.post(new Runnable() {
            @Override
            public void run() {
                updateLastSyncDate(mLastSync);
            }
        });
        finishSync();
    }


    /////////////////////////
    // Колбеки ConflictsFragment

    @Override
    public void saveConflict(@NonNull final ReadLaterItem item) {
        final int remoteId = item.getRemoteId();
        if (remoteId > 0) {
            mHandlerThreadHandler.post(new Runnable() {
                @Override
                public void run() {
                    final int userId = UserInfoUtils.getCurentUser(MainActivity.this).getUserId();
                    if (mSyncFragment.updateOneItem(item, userId, remoteId)) {
                        ReadLaterDbUtils.updateItem(MainActivity.this, item, userId, remoteId);
                    }
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
    }

    /////////////////////////
    // Колбеки ItemListFragment

    @Override
    public void onNewItemClick() {

        // Открываем EditItemViewPagerFragment, без shared element, потому что новый
        FragmentManager fragmentManager = getSupportFragmentManager();
        EditItemViewPagerFragment editItem  = EditItemViewPagerFragment.getInstance(
                fragmentManager, null, EditItemViewPagerFragment.UID_EMPTY, 0, 1);
        openEditItemFragment(fragmentManager, editItem, null);
    }

    @Override
    public void onItemClick(@IntRange(from = 0) int position,
                            @IntRange(from = 1) int totalItems,
                            @NonNull ReadLaterItem item,
                            @IntRange(from = 0) int localId,
                            @NonNull ImageView sharedElement) {

        // Открываем EditItemViewPagerFragment, в нем есть shared element
        FragmentManager fragmentManager = getSupportFragmentManager();
        EditItemViewPagerFragment editItem =
                EditItemViewPagerFragment.getInstance(fragmentManager, item, localId, position, totalItems);

        openEditItemFragment(fragmentManager, editItem, sharedElement);

    }

    @Override
    public void onDataChanged() {
        mItemListFragment.onDataChanged();
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
                        new Runnable() {
                            @Override
                            public void run() {
                                mSyncFragment.stopSync();
                                MainActivityLongTask.startLongBackgroundTask(new Runnable() {
                                    @Override
                                    public void run() {
                                        MainListBackupUtils.saveEverythingAsJsonFile(MainActivity.this);
                                    }
                                }, MainActivity.this);
                                showLoading();
                            }
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
                        new Runnable() {
                            @Override
                            public void run() {
                                mSyncFragment.stopSync();
                                MainActivityLongTask.startLongBackgroundTask(new Runnable() {
                                    @Override
                                    public void run() {
                                        MainListBackupUtils.restoreEverythingFromJsonFile(MainActivity.this);
                                    }
                                }, MainActivity.this);
                                showLoading();
                            }
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
                            new ActivityUtils.Consumer<String>() {
                                @Override
                                public void accept(String param) {
                                    try {
                                        // Смотрим введенное значение
                                        final int count = Integer.parseInt(param);
                                        // Запускаем бэкграунд таск
                                        mSyncFragment.stopSync();
                                        MainActivityLongTask.startLongBackgroundTask(new Runnable() {
                                            @Override
                                            public void run() {
                                                MainListBackupUtils.restoreEverythingFromJsonFile(MainActivity.this);
                                                showLoading();
                                            }
                                        }, MainActivity.this);
                                        MainActivityLongTask.startLongBackgroundTask(new Runnable() {
                                            @Override
                                            public void run() {
                                                DebugUtils.addPlaceholdersToDatabase(MainActivity.this, count);
                                            }
                                        }, MainActivity.this);
                                        showLoading();
                                    } catch (NumberFormatException e) {
                                        Log.e("CAST ERROR", "Ошибка преобразования ввода пользователя в число");
                                    }
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
                            new Runnable() {
                                @Override
                                public void run() {
                                    mSyncFragment.stopSync();
                                    MainActivityLongTask.startLongBackgroundTask(new Runnable() {
                                        @Override
                                        public void run() {
                                            ReadLaterDbUtils.deleteAll(MainActivity.this);
                                            LongTaskNotifications.cancelNotification();
                                        }
                                    }, MainActivity.this);
                                    showLoading();
                                }
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

    @Override
    public void onFilterChanged() {
        mItemListFragment.onDataChanged();
    }

    /////////////////////////
    // Колбеки EditItemFragment

    @Override
    public void onRequestColorPicker(int color, ImageView sharedElement) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        ColorPickerFragment colorPickerFragment = ColorPickerFragment.getInstance(fragmentManager, color);
        FragmentTransaction transaction = fragmentManager.beginTransaction();

        // Shared element
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            colorPickerFragment.setSharedElementEnterTransition(
                    TransitionInflater.from(this).inflateTransition(android.R.transition.move));
            colorPickerFragment.setEnterTransition(
                    TransitionInflater.from(this).inflateTransition(android.R.transition.fade));
            transaction.addSharedElement(sharedElement, SHARED_ELEMENT_COLOR_TRANSITION_NAME);
        }

        transaction.replace(FRAGMENT_CONTAINER, colorPickerFragment, ColorPickerFragment.TAG)
                .addToBackStack(null).commit();
    }

    @Override
    public void onCreateNewItem(@NonNull final ReadLaterItem item) {
        mHandlerThreadHandler.post(new Runnable() {
            @Override
            public void run() {
                ReadLaterDbUtils.insertItem(MainActivity.this, item);
            }
        });
        popFragmentFromBackstack();
        Snackbar.make(mFragmentContainer, getString(R.string.snackbar_item_added), Snackbar.LENGTH_LONG).show();
    }

    @Override
    public void onSaveItem(@NonNull final ReadLaterItem item, @IntRange(from = 0) final int localId) {
        mHandlerThreadHandler.post(new Runnable() {
            @Override
            public void run() {
                ReadLaterDbUtils.updateItem(MainActivity.this, item, localId);
            }
        });
        popFragmentFromBackstack();
        Snackbar.make(mFragmentContainer, getString(R.string.snackbar_item_edited), Snackbar.LENGTH_LONG).show();
    }

    @Override
    public void onDeleteItem(@IntRange(from = 0) final int localId) {
        mHandlerThreadHandler.post(new Runnable() {
            @Override
            public void run() {
                ReadLaterDbUtils.deleteItem(MainActivity.this, localId);
            }
        });
        popFragmentFromBackstack();
        Snackbar.make(mFragmentContainer, getString(R.string.snackbar_item_removed), Snackbar.LENGTH_LONG).show();
    }

    @Override
    public void onExitWithoutModifying(@Nullable ReadLaterItem item,
                                       @IntRange(from = EditItemViewPagerFragment.UID_EMPTY) final int localId) {

        if (item != null) {
            // Этот блок вызывается при простом просмотре без изменений
            mHandlerThreadHandler.post(new Runnable() {
                @Override
                public void run() {
                    ReadLaterDbUtils.updateItemViewDate(MainActivity.this, localId);
                }
            });
        }
        popFragmentFromBackstack();

    }


    /////////////////////////
    // Колбеки EditItemViewPagerFragment

    @Override
    public @Nullable ReadLaterItem getItemAt(@IntRange(from = 0) int position) {
        return mItemListFragment.getItemAt(position);
    }

    @Override
    public int getItemLocalIdAt(@IntRange(from = 0) int position) {
        return mItemListFragment.getItemLocalIdAt(position);
    }


    /////////////////////////
    // Колбеки ColorPickerCallbacks

    @Override
    public void onColorPicked(int newColor) {
        popFragmentFromBackstack();
        FragmentManager fragmentManager = getSupportFragmentManager();
        EditItemViewPagerFragment editItem = (EditItemViewPagerFragment)
                fragmentManager.findFragmentByTag(EditItemViewPagerFragment.TAG);
        if (editItem != null) {
            editItem.setColor(newColor);
        }
    }

    @Override
    public void onEndPickingColor() {
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
            if (mItemListFragment.isVisible()) {
                mItemListFragment.setRefreshing(true);
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
        sharedPreferencesEditor.putLong(String.valueOf(UserInfoUtils.getCurentUser(this).getUserId()), lastSyncDate);
        sharedPreferencesEditor.apply();
        mLastSync = lastSyncDate;
    }

    /** Завершает синхронизацию. */
    private void finishSync() {
        mSyncFragment.stopSync();
        if (mItemListFragment.isVisible()) {
            mItemListFragment.setRefreshing(false);
            // Оповещаем itemListFragment об изменениях, если не запущен лонг таск
            if (!MainActivityLongTask.isActive()) {
                mItemListFragment.onDataChanged();
            }
        }
    }


    /////////////////////////
    // Колбеки MainActivityLongTask

    /** Колбек для MainActivityLongTask об окончании работ. */
    void onLongTaskFinished() {
        // Оповещаем itemListFragment об изменениях
        mItemListFragment.onDataChanged();
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
        getSupportFragmentManager().popBackStackImmediate();
    }

    private void openEditItemFragment(@NonNull FragmentManager fragmentManager,
                                      @NonNull EditItemViewPagerFragment fragment,
                                      @Nullable ImageView sharedElement) {

        FragmentTransaction transaction = fragmentManager.beginTransaction();

        // Shared element
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            fragment.setSharedElementEnterTransition(
                    TransitionInflater.from(this).inflateTransition(android.R.transition.move));
            fragment.setEnterTransition(
                    TransitionInflater.from(this).inflateTransition(android.R.transition.fade));
            if (sharedElement != null) {
                transaction.addSharedElement(sharedElement, SHARED_ELEMENT_COLOR_TRANSITION_NAME);
            }
        }

        transaction.replace(FRAGMENT_CONTAINER, fragment, EditItemViewPagerFragment.TAG)
                .addToBackStack(null).commit();
    }

}
