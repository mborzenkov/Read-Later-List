package com.example.mborzenkov.readlaterlist.backup;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.MainThread;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;

/** Фрагмент для фоновой работы с резервными копиями. */
public class BackupFragment extends Fragment {

    /** Тэг для этого фрагмента. */
    private static final String TAG = "fragment_backup";

    /** Признак запущенной ранее операции. */
    private boolean backupInAction = false;

    /** Колбек для оповещений о ходе операции. */
    private @Nullable BackupCallback mBackupCallback = null;
    /** AsyncTask для резервного копирования. */
    private BackupAsyncTask mBackupTask = null;

    /** Возвращает instance фрагмента.
     *  Если FragmentManager уже содержит подобный фрагмент, возвращает его, а не новый.
     *
     * @param manager менеджер фрагментов
     * @return instance BackupFragment
     */
    public static BackupFragment getInstance(@NonNull FragmentManager manager) {
        BackupFragment syncFragment = (BackupFragment) manager.findFragmentByTag(TAG);
        if (syncFragment == null) {
            syncFragment = new BackupFragment();
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
        if (context instanceof BackupCallback) {
            setCallback((BackupCallback) context);
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        setCallback(null);
    }

    @Override
    public void onDestroy() {
        stopBackup();
        super.onDestroy();
    }

    /** Проверяет, запущена ли операция.
     *
     * @return true - если запущена, иначе false
     */
    public synchronized boolean isActive() {
        return backupInAction;
    }

    /** Запускает операцию.
     * Операция выполняется в AsyncTask, поэтому startBackup должен быть запущен в UI Thread.
     * Новая операция не будет запущена, если предыдущая еще не завершилась.
     *
     * @param mode режим, в котором нужно запустить операцию, не null
     *
     * @return признак успешности запуска операции
     */
    @MainThread
    public synchronized boolean startBackup(@NonNull BackupCallback.BackupMode mode) {

        // Проверяем, что еще не запущена синхронизация
        if (backupInAction) {
            return false;
        }

        // Устанавливаем индикатор запущенной синхронизации
        backupInAction = true;
        mBackupTask = new BackupAsyncTask(mode, mBackupCallback);
        mBackupTask.execute();
        return true;

    }

    /** Принудительно останавливает запущенную операцию. */
    public synchronized void stopBackup() {
        if (backupInAction) {
            if (mBackupTask != null) {
                mBackupTask.cancel(true);
            }
        }
        backupInAction = false;
    }

    /** Устанавливает колбек у AsyncTask.
     *
     * @param callback колбек или null
     */
    private synchronized void setCallback(@Nullable BackupCallback callback) {
        mBackupCallback = callback;
        if (mBackupTask != null) {
            mBackupTask.setCallback(mBackupCallback);
        }
    }

}
