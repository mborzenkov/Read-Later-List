package com.example.mborzenkov.readlaterlist.sync;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Size;

import com.example.mborzenkov.readlaterlist.adt.Conflict;

import java.util.List;

/** Колбек для оповещений о результатах синхронизации. */
public interface SyncCallback {

    /** Ключ для даты последней синхронизации в SharedPreferences.
     *  Под этим ключем хранятся связи String-Long,
     *      где ключ - идентификатор пользоватля, значение - дата и время последней синхронизации как timestamp
     */
    String LAST_SYNC_KEY = "com.example.mborzenkov.mainlist.sync.lastsync";

    /** Проверяет, доступно ли подключение к интернету.
     *
     * @return true - если доступно, иначе false
     */
    boolean isNetworkConnected();

    /** Возвращает дату последней синхронизации из SharedPreferences.
     *
     * @return дата последней синхронизации в формате timestamp или 0, если синхронизаций еще не было
     */
    long getLastSync();

    /** Возвращает контекст приложения.
     *
     * @return контекст приложения
     */
    @NonNull
    Context getApplicationContext();

    /** Вызывается, если синхронизация завершилась с ошибкой. */
    void onSyncFailed();

    /** Вызывается, если синхронизация завершилась успешно, без конфликтов.
     *
     * @param syncStartTime дата начала синхронизации для обновления даты последней синхронизации
     */
    void onSyncSuccess(long syncStartTime);

    /** Выдыватеся, если синхронизация завершилась успешно, но с конфликтами.
     *
     * @param conflicts непустой список конфликтов
     * @param syncStartTime дата начала синхронизации для обновления даты последней синхронизации
     */
    void onSyncWithConflicts(@NonNull @Size(min = 1) List<Conflict> conflicts, long syncStartTime);

}
