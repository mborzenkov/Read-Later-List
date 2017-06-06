package com.example.mborzenkov.readlaterlist.fragments.sync;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.Size;

import com.example.mborzenkov.readlaterlist.adt.Conflict;

import java.util.List;

/** Класс для тестирования колбеков. */
public class SyncCallbackTest implements SyncCallback {

    // Запуск с покрытием:  ./gradlew jacocoTestReport
    // Отчет теста:         ${buildDir}/reports/androidTests/connected/index.html
    // Отчет покрытия:      ${buildDir}/reports/jacoco/jacocoTestReport/html/index.html

    /** Варианты колбеков. */
    public enum TaskResults { FAIL, SUCCESS, CONFLICT }

    /** Последний колбек. */
    public @Nullable TaskResults taskResult = null;
    /** Список конфликтов при onSyncWithConflicts. */
    public @Nullable List<Conflict> conflictList = null;

    /** Объект для синхронизации. */
    private final Object mSyncObject;
    /** Контекст (mock). */
    private final Context mContext;
    /** Дата последней синхронизации. */
    private long mLastSync = 0;
    /** Доступность сети. */
    private boolean mNetworkAvailability = true;

    /** Создает новый объект для получения колбеков.
     *
     * @param syncObject объект для синхронизации, при получении финального колбека, будет вызван syncObject.notify()
     * @param context контекст для возвращения из getApplicationContext
     */
    SyncCallbackTest(@NonNull Object syncObject, @NonNull Context context) {
        mSyncObject = syncObject;
        mContext = context;
    }

    /** Всегда возвращает true. */
    @Override
    public boolean isNetworkConnected() {
        return mNetworkAvailability;
    }

    /** Возвращает дату последней синхронизации в формате timestamp (при первом вызове 0, далее > 0). */
    @Override
    public long getLastSync() {
        return mLastSync;
    }

    /** Возвращает контекст, переданный в конструктор. */
    @NonNull
    @Override
    public Context getApplicationContext() {
        return mContext;
    }

    /** Запоминает вызов в taskResult и вызывает syncObject.notify(). */
    @Override
    public void onSyncFailed() {
        taskResult = TaskResults.FAIL;
        synchronized (mSyncObject) {
            mSyncObject.notify();
        }
    }

    /** Запоминает вызов в taskResult и вызывает syncObject.notify(). */
    @Override
    public void onSyncSuccess(long syncStartTime) {
        taskResult = TaskResults.SUCCESS;
        mLastSync = syncStartTime;
        synchronized (mSyncObject) {
            mSyncObject.notify();
        }
    }

    /** Запоминает вызов в taskResult и вызывает syncObject.notify(). */
    @Override
    public void onSyncWithConflicts(@NonNull @Size(min = 1) List<Conflict> conflicts, long syncStartTime) {
        taskResult = TaskResults.CONFLICT;
        mLastSync = syncStartTime;
        conflictList = conflicts;
        synchronized (mSyncObject) {
            mSyncObject.notify();
        }
    }

    /** Устанавливает доступность сети.
     * Определяет значение, возвращаемое isNetworkConnected().
     *
     * @param availability true - доступно, иначе false
     */
    public void setNetworkAvailability(boolean availability) {
        mNetworkAvailability = availability;
    }

}
