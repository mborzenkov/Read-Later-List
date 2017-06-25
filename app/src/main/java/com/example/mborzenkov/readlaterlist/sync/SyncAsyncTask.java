package com.example.mborzenkov.readlaterlist.sync;

import android.content.Context;
import android.database.Cursor;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.example.mborzenkov.readlaterlist.adt.Conflict;
import com.example.mborzenkov.readlaterlist.adt.ReadLaterItem;
import com.example.mborzenkov.readlaterlist.adt.ReadLaterItemDbAdapter;
import com.example.mborzenkov.readlaterlist.data.ReadLaterContract;
import com.example.mborzenkov.readlaterlist.networking.ReadLaterCloudApi;
import com.example.mborzenkov.readlaterlist.utility.ReadLaterDbUtils;
import com.example.mborzenkov.readlaterlist.utility.UserInfoUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;


/** Совмещает в себе AsyncTask для выполнения фоновой синхронизации и Utility методы.
 *  Необходимо избегать вызова операций на основном потоке, если методы вызываются без AsyncTask.
 *  Методы могут вызывать NetworkOnMainThreadException если вызвана операция на основном потоке.
 *  Callback должен быть установлен в onPreExecute и начале doInBackground, иначе выполнение будет отменено.
 *  Если Callback не установлен в onPostExecute, то информация об окончании не будет передана,
 *      соответственно дата последней синхронизации не будет обновлена и синхронизация будет выполнена повторно.
 */
class SyncAsyncTask extends AsyncTask<Void, Void, SyncAsyncTask.SyncResult> {

    // Тэги и тексты ошибок
    private static final String TAG_ERROR_NETWORK   = "Network Error";
    private static final String TAG_SYNC            = "SYNC";

    private static final String ERROR_NETWORK       = "Network not connected";


    /////////////////////////
    // AsyncTask

    /** Интерфейс сервера для обращений. */
    private final ReadLaterCloudApi mCloudApi;
    /** Callback для оповещений о результатах синхронизации. */
    private @Nullable SyncCallback mSyncCallback;
    /** Дата последней синхронизации. */
    private long lastSync = 0;
    /** Дата начала синхронизации. */
    private long syncStartTime = 0;

    /** Создает новый AsyncTask.
     *
     * @param callback интерфейс для оповещения о результатах выполнения и получения необходимой в процессе информации
     * @param cloudAPi API для связи с сервером, не null
     *
     * @throws NullPointerException если cloudApi == null
     */
    SyncAsyncTask(@Nullable SyncCallback callback, @NonNull ReadLaterCloudApi cloudAPi) {
        cloudAPi.getClass(); // NPE
        mSyncCallback = callback;
        mCloudApi = cloudAPi;
    }

    /** Устанавливает колбек для этого таска.
     *
     * @param callback новый колбек, может быть null, если нужно отписаться
     */
    void setCallback(@Nullable SyncCallback callback) {
        mSyncCallback = callback;
    }

    /** Объект для передачи данных из doInBackground в onPostExecute. */
    static class SyncResult {

        /** Признак успешности синхронизации. */
        private final boolean isSuccessful;

        /** Список конфликтов. */
        private final @NonNull List<Conflict> conflicts;

        /** Создает новый объект SyncResult с ошибкой. */
        private SyncResult() {
            this.isSuccessful = false;
            this.conflicts = Collections.emptyList();
        }

        /** Создает новый объект SyncResult с успешным принаком и списком конфликтов.
         *
         * @param conflicts список конфликтов.
         */
        private SyncResult(@NonNull List<Conflict> conflicts) {
            this.isSuccessful = true;
            this.conflicts = conflicts;
        }
    }

    @Override
    protected void onPreExecute() {
        // Проверяем, есть ли SyncCallback и есть ли подлкючение к сети
        if (mSyncCallback != null) {
            lastSync = mSyncCallback.getLastSync();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ssZZZZZ", Locale.US);
            Log.d("SYNC", "Last sync: " + sdf.format(lastSync));
            if (!mSyncCallback.isNetworkConnected()) {
                Log.e(TAG_ERROR_NETWORK, ERROR_NETWORK);
                mSyncCallback.onSyncFailed();
                cancel(true);
            }
        } else {
            cancel(true);
        }
        syncStartTime = System.currentTimeMillis();
    }

    @Override
    protected @Nullable SyncResult doInBackground(Void... params) {

        // Синхронизация по следующей схеме:
        // Server \ Local       есть, изменен       есть, без изм.      нет
        //
        // есть, изменен        - Конфликт -        Update Local        Insert Local
        //
        // есть, без изм.       Update Server       X (пропуск)         Delete Server
        //
        // нет                  Insert Server       Delete Local        X (таких нет)

        if (isCancelled() || mSyncCallback == null) {
            return null;
        }

        // Запоминаем дату начала синхронизации, контекст приложения (для бд), объект для доступа к API и тек. польз.
        final Context appContext = mSyncCallback.getApplicationContext();
        final int userId = UserInfoUtils.getCurentUser(appContext).getUserId();

        // Список всех идентификаторов заметок на сервере
        List<Integer> allServerIds = new ArrayList<>();
        List<Conflict> conflicts = new ArrayList<>();
        {
            // Получили список всех заметок с сервера, сохранили
            List<ReadLaterItem> itemsOnServer = mCloudApi.getAllItemsOnServer(userId);
            if (itemsOnServer == null) {
                return new SyncResult();
            }
            // Обрабатываем все, что есть на сервере
            for (ReadLaterItem itemServer : itemsOnServer) {
                // Проверяем на null, получаем remoteId, у заметок с сервера он должен быть всегда
                if (itemServer != null) {
                    final int remoteId = itemServer.getRemoteId();
                    if (remoteId != 0) {

                        allServerIds.add(remoteId);
                        // Делим заметки на измененные и не измененные
                        if (itemServer.getDateModified() > lastSync) {
                            // Делим локальные заметки на измененные, без изменений и нет
                            ReadLaterItem itemLocal = ReadLaterDbUtils.getItemByRemoteId(appContext, userId, remoteId);
                            if (itemLocal == null) {
                                // Server: есть, изменен; Local: нет
                                Log.d(TAG_SYNC, "Inserting Local: " + remoteId);
                                ReadLaterDbUtils.insertItem(appContext, itemServer);
                            } else if (itemLocal.getDateModified() <= lastSync) {
                                // Server: есть, изменен; Local: есть, без изм.
                                Log.d(TAG_SYNC, "Updating Local: " + remoteId + ", item: " + itemServer);
                                ReadLaterDbUtils.updateItem(appContext, itemServer, userId, remoteId);
                            } else {
                                // Server: есть, изменен; Local: есть, изменен
                                if (!itemLocal.equalsByContent(itemServer)) {
                                    // Разбор конфликтов пользователем происходит только если не равны содержательно
                                    conflicts.add(new Conflict(itemServer, itemLocal));
                                } else {
                                    // Если они равны содержательно, но все таки изменены, то запишем в оба места
                                    //      меньшую дату создания и большие даты изменения и просмотра
                                    ReadLaterItem.Builder itemBuilder = new ReadLaterItem.Builder(itemLocal);
                                    itemBuilder.dateCreated(
                                            Math.min(itemLocal.getDateCreated(), itemServer.getDateCreated()));
                                    itemBuilder.dateModified(Math.max(
                                            itemLocal.getDateModified(), itemServer.getDateModified()));
                                    itemBuilder.dateViewed(Math.max(
                                            itemLocal.getDateViewed(), itemServer.getDateViewed()));
                                    ReadLaterItem savingItem = itemBuilder.build();
                                    mCloudApi.updateItemOnServer(userId, remoteId, savingItem);
                                    ReadLaterDbUtils.updateItem(appContext, savingItem, userId, remoteId);
                                    Log.d(TAG_SYNC, "Auto merge: " + remoteId + ", item: " + itemServer);
                                }
                            }
                        } else {
                            // Делим локальные заметки на измененные, без изменений и нет
                            ReadLaterItem itemLocal = ReadLaterDbUtils.getItemByRemoteId(appContext, userId, remoteId);
                            if (itemLocal == null) {
                                // Server: есть, без изм.; Local: нет
                                Log.d(TAG_SYNC, "Deleting Server: " + remoteId);
                                if (!mCloudApi.deleteItemOnServer(userId, remoteId)) {
                                    return new SyncResult();
                                }
                            } else if (itemLocal.getDateModified() <= lastSync)  {
                                // Server: есть, без изм.; Local: есть, без изм.
                                //noinspection UnnecessaryContinue
                                continue;
                            } else {
                                // Server: есть, без изм.; Local: есть, изменен
                                Log.d(TAG_SYNC, "Updating Server: " + itemLocal);
                                ReadLaterItem.Builder itemBuilder = new ReadLaterItem.Builder(itemLocal);
                                itemBuilder.dateModified(System.currentTimeMillis());
                                if (!mCloudApi.updateItemOnServer(userId, remoteId, itemBuilder.build())) {
                                    return new SyncResult();
                                }
                            }
                        }

                    }
                }
            }
        }

        {
            // Получили список всех заметок локальных
            Cursor itemsLocalCursor = ReadLaterDbUtils.queryAllItems(appContext, userId);
            if (itemsLocalCursor == null) {
                return new SyncResult();
            }
            ReadLaterItemDbAdapter itemDbAdapter = new ReadLaterItemDbAdapter();
            for (int i = 0, totalItems = itemsLocalCursor.getCount(); i < totalItems; i++) {
                // Обрабатываем все, что есть локально, но нет на сервере
                itemsLocalCursor.moveToPosition(i);
                ReadLaterItem itemLocal = itemDbAdapter.itemFromCursor(itemsLocalCursor);
                // Проверяем на null, получаем remoteId
                if (itemLocal != null) {
                    final Integer remoteId = itemLocal.getRemoteId();

                    // Отсекаем то, что уже проверяли
                    if (allServerIds.contains(remoteId)) {
                        continue;
                    }

                    final int uid = itemsLocalCursor.getInt(
                            itemsLocalCursor.getColumnIndex(ReadLaterContract.ReadLaterEntry._ID));

                    // Делим заметки на измененные и не измененные
                    if (itemLocal.getDateModified() > lastSync) {
                        // Server: нет; Local: есть, изменен
                        ReadLaterItem.Builder itemBuilder = new ReadLaterItem.Builder(itemLocal);
                        itemBuilder.dateModified(System.currentTimeMillis());
                        ReadLaterItem savingItem = itemBuilder.build();
                        Log.d(TAG_SYNC, "Inserting Server: " + savingItem);
                        Integer newRemoteId = mCloudApi.insertItemOnServer(userId, savingItem);
                        if (newRemoteId == null) {
                            return new SyncResult();
                        }
                        Log.d(TAG_SYNC, "Updating local remoteId: " + newRemoteId);
                        ReadLaterDbUtils.updateItemRemoteId(appContext, uid, newRemoteId);
                    } else {
                        // Server: нет; Local: есть, без изм.
                        if (remoteId != 0) {
                            // Тут всегда есть remoteId (потому что раз без изм., то синхронизация уже была),
                            //      но лучше лишний раз проверить
                            ReadLaterDbUtils.deleteItem(appContext, uid);
                            Log.d(TAG_SYNC, "Deleting Local: " + remoteId);
                        }
                    }
                }
            }
            itemsLocalCursor.close();
        }

        return new SyncResult(conflicts);
    }

    @Override
    protected void onPostExecute(@Nullable SyncResult syncResult) {
        if (mSyncCallback != null) {
            if (syncResult == null || !syncResult.isSuccessful) {
                mSyncCallback.onSyncFailed();
            } else {
                if (!syncResult.conflicts.isEmpty()) {
                    mSyncCallback.onSyncWithConflicts(syncResult.conflicts, syncStartTime);
                } else {
                    mSyncCallback.onSyncSuccess(syncStartTime);
                }
            }
        }
    }

}
