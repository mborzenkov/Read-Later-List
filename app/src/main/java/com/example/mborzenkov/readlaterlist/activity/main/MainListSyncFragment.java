package com.example.mborzenkov.readlaterlist.activity.main;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;

import com.example.mborzenkov.readlaterlist.networking.CloudSyncTask;
import com.example.mborzenkov.readlaterlist.networking.CloudSyncTask.SyncCallback;

/** Фрагмент для фоновой синхронизации с Cloud API. */
public class MainListSyncFragment extends Fragment {

    /** Тэг для этого фрагмента. */
    private static final String TAG = "fragment_mainlist_sync";

    /** Признак запущенной ранее синхронизации. */
    private @NonNull Boolean syncInAction = false;

    /** Колбек для оповещений о ходе синхронизации. */
    private @Nullable SyncCallback mSyncCallback = null;
    /** AsyncTask для синхронизации. */
    private CloudSyncTask mSyncTask = null;

    /** Возвращает instance фрагмента.
     *  Если FragmentManager уже содержит подобный фрагмент, возвращает его, а не новый.
     *
     * @param manager менеджер фрагментов
     * @return instance MainListSyncFragment
     */
    public static MainListSyncFragment getInstance(@NonNull FragmentManager manager) {
        MainListSyncFragment syncFragment = (MainListSyncFragment) manager.findFragmentByTag(TAG);
        if (syncFragment == null) {
            syncFragment = new MainListSyncFragment();
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

    /** Принудительно останавливает синхронизацию. */
    synchronized void stopSync() {
        if (syncInAction) {
            if (mSyncTask != null) {
                mSyncTask.cancel(true);
            }
        }
        syncInAction = false;
    }

    /** Запускает полную синхронизацию. */
    synchronized void startFullSync() {

        // Проверяем, что еще не запущена синхронизация
        if (syncInAction) {
            return;
        }

        // Устанавливаем индикатор запущенной синхронизации
        syncInAction = true;
        mSyncTask = new CloudSyncTask(mSyncCallback);
        mSyncTask.execute();

    }

}
