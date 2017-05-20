package com.example.mborzenkov.readlaterlist.fragments.sync;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.IntDef;
import android.support.annotation.IntRange;
import android.support.annotation.MainThread;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;

import com.example.mborzenkov.readlaterlist.adt.ReadLaterItem;
import com.example.mborzenkov.readlaterlist.adt.UserInfo;
import com.example.mborzenkov.readlaterlist.fragments.sync.SyncAsyncTask.SyncCallback;

/** Фрагмент для фоновой синхронизации с Cloud API. */
public class SyncFragment extends Fragment {

    /** Тэг для этого фрагмента. */
    private static final String TAG = "fragment_sync";

    /** Признак запущенной ранее синхронизации. */
    private @NonNull Boolean syncInAction = false;

    /** Колбек для оповещений о ходе синхронизации. */
    private @Nullable SyncCallback mSyncCallback = null;
    /** AsyncTask для синхронизации. */
    private SyncAsyncTask mSyncTask = null;

    /** Возвращает instance фрагмента.
     *  Если FragmentManager уже содержит подобный фрагмент, возвращает его, а не новый.
     *
     * @param manager менеджер фрагментов
     * @return instance SyncFragment
     */
    public static SyncFragment getInstance(@NonNull FragmentManager manager) {
        SyncFragment syncFragment = (SyncFragment) manager.findFragmentByTag(TAG);
        if (syncFragment == null) {
            syncFragment = new SyncFragment();
            manager.beginTransaction().add(syncFragment, TAG).commit();
        }
        return syncFragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        mSyncCallback = (SyncCallback) context;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mSyncCallback = null;
    }

    @Override
    public void onDestroy() {
        stopSync();
        super.onDestroy();
    }

    /** Обновляет одну запись на сервере.
     * Выполняет работу в том же потоке, в котором вызван метод, поэтому должен быть вызван не в основном потоке.
     *
     * @param item запись для обновления. item.getRemoteId() должен быть > 0.
     * @param userId
     * @param remoteId
     *
     * @return true, если обновление прошло успешно, иначе false
     *
     * @throws android.os.NetworkOnMainThreadException если метод запущен на основном потоке
     * @throws IllegalArgumentException если userId <= 0 или remoteId <= 0
     */
    public synchronized boolean updateOneItem(@NonNull ReadLaterItem item,
                                            @IntRange(from = 1) int userId,
                                            @IntRange(from = 1) int remoteId) {

        if ((userId <= 0) || (remoteId <= 0)) {
            throw new IllegalArgumentException(String.format("Error @ updateOneItem: userId = %s, remoteId = %s.",
                    userId,
                    remoteId));
        }
        return SyncAsyncTask.updateItemOnServer(SyncAsyncTask.prepareApi(), userId, remoteId, item);

    }

    /** Принудительно останавливает синхронизацию. */
    public synchronized void stopSync() {
        if (syncInAction) {
            if (mSyncTask != null) {
                mSyncTask.cancel(true);
            }
        }
        syncInAction = false;
    }

    /** Запускает полную синхронизацию.
     * Синхронизация выполняется в AsyncTask, поэтому startFullSync должен быть запущен в UI Thread.
     */
    @MainThread
    public synchronized void startFullSync() {

        // Проверяем, что еще не запущена синхронизация
        if (syncInAction) {
            return;
        }

        // Устанавливаем индикатор запущенной синхронизации
        syncInAction = true;
        mSyncTask = new SyncAsyncTask(mSyncCallback);
        mSyncTask.execute();

    }

}
