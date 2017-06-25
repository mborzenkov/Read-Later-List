package com.example.mborzenkov.readlaterlist.activity.main;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
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
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
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
import android.widget.Toast;

import com.example.mborzenkov.readlaterlist.BuildConfig;
import com.example.mborzenkov.readlaterlist.MyApplication;
import com.example.mborzenkov.readlaterlist.R;
import com.example.mborzenkov.readlaterlist.adt.Conflict;
import com.example.mborzenkov.readlaterlist.adt.CustomColor;
import com.example.mborzenkov.readlaterlist.adt.MainListFilter;
import com.example.mborzenkov.readlaterlist.adt.ReadLaterItem;
import com.example.mborzenkov.readlaterlist.adt.UserInfo;
import com.example.mborzenkov.readlaterlist.backup.BackupCallback;
import com.example.mborzenkov.readlaterlist.backup.BackupFragment;
import com.example.mborzenkov.readlaterlist.fragments.ColorPickerFragment;
import com.example.mborzenkov.readlaterlist.fragments.ConflictsFragment;
import com.example.mborzenkov.readlaterlist.fragments.edititem.EditItemFragmentActions;
import com.example.mborzenkov.readlaterlist.fragments.edititem.EditItemViewPagerFragment;
import com.example.mborzenkov.readlaterlist.fragments.filterdrawer.FilterDrawerCallbacks;
import com.example.mborzenkov.readlaterlist.fragments.filterdrawer.FilterDrawerFragment;
import com.example.mborzenkov.readlaterlist.fragments.itemlist.ItemListFragment;
import com.example.mborzenkov.readlaterlist.networking.CloudApiComponent;
import com.example.mborzenkov.readlaterlist.networking.ReadLaterCloudApi;
import com.example.mborzenkov.readlaterlist.sync.SyncCallback;
import com.example.mborzenkov.readlaterlist.sync.SyncFragment;
import com.example.mborzenkov.readlaterlist.utility.DebugUtils;
import com.example.mborzenkov.readlaterlist.utility.FavoriteColorsUtils;
import com.example.mborzenkov.readlaterlist.utility.MainListFilterUtils;
import com.example.mborzenkov.readlaterlist.utility.ReadLaterDbUtils;
import com.example.mborzenkov.readlaterlist.utility.UserInfoUtils;

import java.util.List;

/** Главная Activity, представляющая собой список. */
public class MainActivity extends AppCompatActivity implements
        SyncCallback,
        BackupCallback,
        ConflictsFragment.ConflictsCallback,
        ItemListFragment.ItemListCallbacks,
        EditItemFragmentActions.EditItemCallbacks,
        EditItemViewPagerFragment.EditItemViewPagerCallbacks,
        FilterDrawerCallbacks,
        ColorPickerFragment.ColorPickerCallbacks {

    // TODO: MVP

    /////////////////////////
    // Константы

    /** ID контейнера для помещения фрагментов. */
    private static final @IdRes int FRAGMENT_CONTAINER = R.id.fragmentcontainer_mainactivity;

    /** Тэг для HandlerThread. */
    private static final String HANDLERTHREAD_TAG = "mainactivity_handlerthread";

    /** Префикс для SharedElement. */
    public static final String SHARED_ELEMENT_COLOR_TRANSITION_NAME = "readlateritem_sharedelement_color";

    /** Константа запроса разрешения на чтение файлов. */
    private static final int PERMISSION_READ_EXTERNAL_STORAGE = 101;
    /** Константа запроса разрешения на запись файлов. */
    private static final int PERMISSION_WRITE_EXTERNAL_STORAGE = 102;


    /////////////////////////
    // Поля объекта

    // Хэлперы
    private SyncFragment mSyncFragment;
    private BackupFragment mBackupFragment;
    private InternetBroadcastReceiver mInternetBroadcastReceiver;
    private IntentFilter mInternetChangedIntentFilter;
    private HandlerThread mHandlerThread;
    private Handler mHandlerThreadHandler;
    private ItemListFragment mItemListFragment;

    // Элементы layout
    private FrameLayout mFragmentContainer;

    /** Дата последней синхронизации. */
    private long mLastSync;
    /** Последнее запущенное уведомление. */
    private @Nullable ReadLaterNotification mLastNotification;

    /** Класс {@link android.content.BroadcastReceiver} для получения бродкаста об изменении сети.
     *  Запускает синхронизацию при подключении к интернету.
     *
     */
    private class InternetBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (isNetworkConnected() && !mBackupFragment.isActive()) {
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

        // Инициализациия ItemListFragment
        FragmentManager fragmentManager = getSupportFragmentManager();
        mItemListFragment = ItemListFragment.getInstance(fragmentManager);

        // Инициализация SyncFragment
        mSyncFragment = SyncFragment.getInstance(fragmentManager);

        // Инициализация BackupFragment
        mBackupFragment = BackupFragment.getInstance(fragmentManager);

        // Инициализация BroadcastReceiver
        mInternetBroadcastReceiver = new InternetBroadcastReceiver();
        mInternetChangedIntentFilter = new IntentFilter();
        mInternetChangedIntentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);

        // Инициализация полезного HandlerThread
        mHandlerThread = new HandlerThread(HANDLERTHREAD_TAG);
        mHandlerThread.start();
        mHandlerThreadHandler = new Handler(mHandlerThread.getLooper());

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
        toggleSync();
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
            toggleSync();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mHandlerThread.quit();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_WRITE_EXTERNAL_STORAGE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (mBackupFragment.startBackup(BackupCallback.BackupMode.SAVE)) {
                        int notificationId = ReadLaterNotification.nextId();
                        mLastNotification = new ReadLaterNotification(this,
                                getString(R.string.notification_backup_save_title), notificationId);
                        mLastNotification.showWithProgress(0, true);
                    }
                }
                break;
            case PERMISSION_READ_EXTERNAL_STORAGE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (mBackupFragment.startBackup(BackupCallback.BackupMode.RESTORE)) {
                        int notificationId = ReadLaterNotification.nextId();
                        mLastNotification = new ReadLaterNotification(this,
                                getString(R.string.notification_backup_restore_title), notificationId);
                        mLastNotification.showWithProgress(0, true);
                    }
                }
                break;
            default:
                break;
        }
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
    // Колбеки SyncAsyncTask

    @Override
    public void onBackupProgressUpdate(@NonNull BackupMode mode,
                                       @IntRange(from = PROGRESS_MIN, to = PROGRESS_MAX) int progress) {

        if (mLastNotification != null) {
            mLastNotification.showWithProgress(progress, false);
        }

    }

    @Override
    public void onBackupFailed(@NonNull BackupMode mode) {
        mBackupFragment.stopBackup();
        if (mLastNotification != null) {
            mLastNotification.cancel();
        }
    }

    @Override
    public void onBackupSuccess(@NonNull BackupMode mode) {
        mBackupFragment.stopBackup();
        if (mLastNotification != null) {
            mLastNotification.cancel();
        }
        switch (mode) {
            case SAVE:
                Toast.makeText(this, R.string.toast_backup_save_finished, Toast.LENGTH_SHORT).show();
                break;
            case RESTORE:
                Toast.makeText(this, R.string.toast_backup_restore_finished, Toast.LENGTH_SHORT).show();
                mItemListFragment.onDataChanged(); // перезагружаем данные сначала и по окончанию синхронизации тоже
                toggleSync();
                break;
            default:
                break;
        }
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
                    CloudApiComponent component = ((MyApplication) getApplication()).getCloudApiComponent();
                    if (new ReadLaterCloudApi(component).updateItemOnServer(userId, remoteId, item)) {
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

    @Override
    public void onChangeItemOrder(final int localId, final int newPosition) {
        mHandlerThreadHandler.post(new Runnable() {
            @Override
            public void run() {
                ReadLaterDbUtils.changeItemOrder(MainActivity.this, localId, newPosition);
                mItemListFragment.onDataChanged();
            }
        });
    }


    /////////////////////////
    // Колбеки FilterDrawerLayout

    @Override
    public void onActionToggled(FilterDrawerFragment.DrawerActions action) {

        // Если выполняется резервное копирование, кнопки не работают, показывается предупреждение.
        if (mBackupFragment.isActive()) {
            DialogUtils.showAlertDialog(this,
                    getString(R.string.mainlist_backupisactive_title),
                    getString(R.string.mainlist_backupisactive_text),
                    null,
                    null);
            return;
        }

        switch (action) {
            case BACKUP_SAVE:
                // Действие "Сохранить бэкап" открывает окно подтверждения и по положительному ответу
                // вызывает функцию для сохранения
                DialogUtils.showAlertDialog(
                        this,
                        getString(R.string.mainlist_drawer_backup_save_question_title),
                        getString(R.string.mainlist_drawer_backup_save_question_text),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                mSyncFragment.stopSync();
                                if (ContextCompat.checkSelfPermission(MainActivity.this,
                                        Manifest.permission.WRITE_EXTERNAL_STORAGE)
                                        != PackageManager.PERMISSION_GRANTED) {
                                    ActivityCompat.requestPermissions(MainActivity.this,
                                            new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                                            PERMISSION_WRITE_EXTERNAL_STORAGE);
                                } else if (mBackupFragment.startBackup(BackupCallback.BackupMode.SAVE)) {
                                    int notificationId = ReadLaterNotification.nextId();
                                    mLastNotification =
                                            new ReadLaterNotification(MainActivity.this,
                                                    getString(R.string.notification_backup_save_title), notificationId);
                                    mLastNotification.showWithProgress(0, true);
                                }
                            }
                        },
                        null);
                break;
            case BACKUP_RESTORE:
                // Действие "Восстановить из бэкапа" открывает окно подтверждения и по положительному ответу
                // вызывает функцию для восстановления
                DialogUtils.showAlertDialog(
                        this,
                        getString(R.string.mainlist_drawer_backup_restore_question_title),
                        getString(R.string.mainlist_drawer_backup_restore_question_text),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                mSyncFragment.stopSync();
                                if (ContextCompat.checkSelfPermission(MainActivity.this,
                                        Manifest.permission.WRITE_EXTERNAL_STORAGE)
                                        != PackageManager.PERMISSION_GRANTED) {
                                    ActivityCompat.requestPermissions(MainActivity.this,
                                            new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                                            PERMISSION_READ_EXTERNAL_STORAGE);
                                } else if (mBackupFragment.startBackup(BackupCallback.BackupMode.RESTORE)) {
                                    int notificationId = ReadLaterNotification.nextId();
                                    mLastNotification =
                                            new ReadLaterNotification(MainActivity.this,
                                                    getString(R.string.notification_backup_restore_title),
                                                    notificationId);
                                    mLastNotification.showWithProgress(0, true);
                                }
                            }
                        },
                        null);
                break;
            case FILL_PLACEHOLDERS:
                // Действие "Заполнить данными" открывает окно подтверждения и по положительному ответу
                // вызывает функцию для заполнения
                if (BuildConfig.DEBUG) {
                    final EditText inputNumber = new EditText(this);
                    inputNumber.setInputType(InputType.TYPE_CLASS_NUMBER);
                    inputNumber.setFilters(new InputFilter[] {new InputFilter.LengthFilter(1)}); // 0-9
                    DialogUtils.showInputTextDialog(
                            this,
                            inputNumber,
                            getString(R.string.mainlist_menu_add_placeholders_question_title),
                            getString(R.string.mainlist_menu_add_placeholders_question_text),
                            new DialogUtils.OnClickWithTextInput() {
                                @Override
                                public void onClick(@NonNull String input) {
                                    try {
                                        // Смотрим введенное значение
                                        final int count = Integer.parseInt(input);
                                        // Запускаем бэкграунд таск
                                        mSyncFragment.stopSync();
                                        mHandlerThreadHandler.post(new Runnable() {
                                            @Override
                                            public void run() {
                                                DebugUtils.addPlaceholdersToDatabase(MainActivity.this, count);
                                                mItemListFragment.onDataChanged();
                                            }
                                        });
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
                    DialogUtils.showAlertDialog(
                            this,
                            getString(R.string.mainlist_menu_delete_all_question_title),
                            getString(R.string.mainlist_menu_delete_all_question_text),
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    mSyncFragment.stopSync();
                                    mHandlerThreadHandler.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            ReadLaterDbUtils.deleteAll(MainActivity.this);
                                            mItemListFragment.onDataChanged();
                                        }
                                    });
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
    public void onSavedFilterClick(int position) {

        int indexSavedAdd = MainListFilterUtils.getIndexSavedAdd();
        int indexSavedDelete = MainListFilterUtils.getIndexSavedDelete();
        if (position == indexSavedAdd) {
            // Вариант 1: Клик на кнопку "+ Добавить"
            // Показываем окно ввода текста, сохраняем при успешном вводе
            final int currentIndex = MainListFilterUtils.getIndexSavedCurrent();
            final EditText editText = new EditText(this);
            DialogUtils.showInputTextDialog(
                    this,
                    editText,
                    getString(R.string.mainlist_drawer_filters_save_question_title),
                    null,
                    new DialogUtils.OnClickWithTextInput() {
                        @Override
                        public void onClick(@NonNull String input) {
                            if (!input.isEmpty()
                                    && !input.equals(getString(R.string.mainlist_drawer_filters_default))) {
                                MainListFilterUtils.saveFilter(MainActivity.this, input);
                                mItemListFragment.onFilterChanged(MainListFilterUtils.getCurrentFilter());
                                mItemListFragment.onDataChanged();
                                mItemListFragment.setSavedFilterSelection(
                                        MainListFilterUtils.getIndexSavedCurrent(), true);
                            } else {
                                mItemListFragment.setSavedFilterSelection(currentIndex, false);
                            }
                        }
                    },
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            mItemListFragment.setSavedFilterSelection(currentIndex, false);
                        }
                    });

        } else if (position == indexSavedDelete) {
            // Вариант 2: Клик на кнопку "- Удалить"
            // Показываем окно подтверждения, удаляем при положительном ответе
            final int currentIndex = MainListFilterUtils.getIndexSavedCurrent();
            if (currentIndex == MainListFilterUtils.INDEX_SAVED_DEFAULT) {
                mItemListFragment.setSavedFilterSelection(currentIndex, false);
                return;
            }
            DialogUtils.showAlertDialog(
                    this,
                    getString(R.string.mainlist_drawer_filters_remove_question_title),
                    getString(R.string.mainlist_drawer_filters_remove_question_text),
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            MainListFilterUtils.removeCurrentFilter(MainActivity.this);
                            mItemListFragment.onFilterChanged(MainListFilterUtils.getCurrentFilter());
                            mItemListFragment.onDataChanged();
                            mItemListFragment.setSavedFilterSelection(MainListFilterUtils.getIndexSavedCurrent(), true);
                        }
                    },
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            mItemListFragment.setSavedFilterSelection(currentIndex, false);
                        }
                    });
        } else {
            Log.d("FILTER", "CREATION w position: " + position);
            // Остальные варианты - выбираем
            MainListFilterUtils.clickOnSavedFilter(position);
            mItemListFragment.onFilterChanged(MainListFilterUtils.getCurrentFilter());
            mItemListFragment.onDataChanged();
        }
    }

    @Override
    public void onDateFilterClick(int position) {
        MainListFilter filter = MainListFilterUtils.getCurrentFilter();
        filter.setSelection(MainListFilterUtils.getDateFilterSelection(position));
        mItemListFragment.onFilterChanged(filter);
        mItemListFragment.onDataChanged();
    }

    @Override
    public void onDateFromSet(long date) {
        MainListFilter filter = MainListFilterUtils.getCurrentFilter();
        filter.setDateFrom(date);
        mItemListFragment.onFilterChanged(filter);
        mItemListFragment.onDataChanged();
    }

    @Override
    public void onDateToSet(long date) {
        MainListFilter filter = MainListFilterUtils.getCurrentFilter();
        filter.setDateTo(date);
        mItemListFragment.onFilterChanged(filter);
        mItemListFragment.onDataChanged();
    }

    @Override
    public void onFavoriteColorClick(int color) {
        MainListFilter filter = MainListFilterUtils.getCurrentFilter();
        if (filter.getColorFilter().contains(color)) {
            filter.removeColorFilter(color);
        } else {
            filter.addColorFilter(color);
        }
        mItemListFragment.onFilterChanged(filter);
        mItemListFragment.onDataChanged();
    }

    @Override
    public void onChangeUserClick() {
        // Нажатие на "сменить пользователя"
        final int userId = UserInfoUtils.getCurentUser(this).getUserId();
        EditText inputNumber = new EditText(this);
        inputNumber.setInputType(InputType.TYPE_CLASS_NUMBER);
        inputNumber.setFilters(new InputFilter[] {new InputFilter.LengthFilter(UserInfo.USER_ID_MAX_LENGTH)});
        inputNumber.setText(String.valueOf(userId));
        DialogUtils.showInputTextDialog(
                this,
                inputNumber,
                getString(R.string.mainlist_user_change_question_title),
                getString(R.string.mainlist_user_change_question_text),
                new DialogUtils.OnClickWithTextInput() {
                    @Override
                    public void onClick(@NonNull String input) {
                        try {
                            // Смотрим введенное значение
                            int number = Integer.parseInt(input);
                            if (number != userId) {
                                UserInfoUtils.changeCurrentUser(MainActivity.this, number);
                                mItemListFragment.onUserChanged(String.valueOf(number));
                                toggleSync();
                            }
                        } catch (ClassCastException e) {
                            Log.e("CAST ERROR", "Ошибка преобразования ввода пользователя в число");
                        }
                    }
                },
                null);
    }

    @Override
    public void onSortButtonClick(MainListFilter.SortType type) {
        MainListFilter filter = MainListFilterUtils.getCurrentFilter();
        if (filter.getSortType() == type) {
            filter.nextSortOrder();
        } else {
            filter.setSortType(type);
        }
        mItemListFragment.onFilterChanged(filter);
        mItemListFragment.onDataChanged();
    }

    @NonNull
    @Override
    public String getCurrentUser() {
        return String.valueOf(UserInfoUtils.getCurentUser(this).getUserId());
    }

    @NonNull
    @Override
    public MainListFilter getCurrentFilter() {
        return MainListFilterUtils.getCurrentFilter();
    }

    @Override
    public List<String> getSavedFiltersList() {
        return MainListFilterUtils.getSavedFiltersList(this);
    }

    @Override
    public List<String> getDateFiltersList() {
        return MainListFilterUtils.getDateFiltersList(this);
    }



    /////////////////////////
    // Колбеки EditItemFragment

    @Override
    public void onRequestColorPicker(int color, ImageView sharedElement) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        ColorPickerFragment colorPickerFragment =
                ColorPickerFragment.getInstance(fragmentManager, new CustomColor(color));
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

        if ((item != null) && (localId >= 0)) {
            // Этот блок вызывается при простом просмотре без изменений
            @IntRange(from = 0) final int localIdPositive = localId;
            mHandlerThreadHandler.post(new Runnable() {
                @Override
                public void run() {
                    ReadLaterDbUtils.updateItemViewDate(MainActivity.this, localIdPositive);
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
    public @NonNull int[] getFavoriteColors() {
        return FavoriteColorsUtils.getFavoriteColorsFromSharedPreferences(this, null);
    }

    @Override
    public void saveFavoriteColor(@NonNull CustomColor newColor, @IntRange(from = 0) int position) {
        FavoriteColorsUtils.saveFavoriteColor(this, null, newColor.getColorRgb(), position);
    }

    @Override
    public void onColorPicked(@NonNull CustomColor newColor) {
        popFragmentFromBackstack();
        FragmentManager fragmentManager = getSupportFragmentManager();
        EditItemViewPagerFragment editItem = (EditItemViewPagerFragment)
                fragmentManager.findFragmentByTag(EditItemViewPagerFragment.TAG);
        if (editItem != null) {
            editItem.setColor(newColor.getColorRgb());
        }
    }

    @Override
    public void onEndPickingColor() {
        popFragmentFromBackstack();
    }


    /////////////////////////
    // Методы синхронизации

    /** Вызывает начало синхронизации.
     * Синхронизация будет запущена, если не выполняется резервное копирование.
     * По окончанию синхронизации при любом исходе вызывается finishSync.
     * Если выполняется резервное копирование, то сразу будет вызван finishSync.
     * Для справки: при успешном завершении BackupTask, вызывается toggleSync и синхронизация проходит в штатном режиме.
     */
    private void toggleSync() {
        if (!mBackupFragment.isActive()) {
            if (mItemListFragment.isVisible()) {
                mItemListFragment.setRefreshing(true);
            }
            mSyncFragment.startFullSync();
        } else {
            finishSync();
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
            // Оповещаем itemListFragment об изменениях
            mItemListFragment.onDataChanged();
        }
    }


    /////////////////////////
    // Все остальное

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
