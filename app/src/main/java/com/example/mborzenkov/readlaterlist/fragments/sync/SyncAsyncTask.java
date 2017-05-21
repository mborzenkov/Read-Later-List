package com.example.mborzenkov.readlaterlist.fragments.sync;

import android.content.Context;
import android.database.Cursor;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.Size;
import android.util.Log;

import com.example.mborzenkov.readlaterlist.BuildConfig;
import com.example.mborzenkov.readlaterlist.adt.ReadLaterItem;
import com.example.mborzenkov.readlaterlist.adt.ReadLaterItemDbAdapter;
import com.example.mborzenkov.readlaterlist.adt.ReadLaterItemJsonAdapter;
import com.example.mborzenkov.readlaterlist.adt.UserInfo;
import com.example.mborzenkov.readlaterlist.data.ReadLaterContract;
import com.example.mborzenkov.readlaterlist.networking.CloudApiYufimtsev;
import com.example.mborzenkov.readlaterlist.utility.ReadLaterDbUtils;
import com.squareup.moshi.Moshi;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.moshi.MoshiConverterFactory;

/** Совмещает в себе AsyncTask для выполнения фоновой синхронизации и Utility методы.
 *  Необходимо избегать вызова операций на основном потоке, если методы вызываются без AsyncTask.
 *  Методы могут вызывать NetworkOnMainThreadException если вызвана операция на основном потоке
 */
public class SyncAsyncTask extends AsyncTask<Void, Void, SyncAsyncTask.SyncResult> {

    // Тэги и тексты ошибок
    private static final String TAG_ERROR_NETWORK   = "Network Error";
    private static final String TAG_ERROR_CLOUD     = "CloudApi Error";
    private static final String TAG_SYNC            = "SYNC";

    private static final String ERROR_NETWORK       = "Network not connected";
    private static final String ERROR_IO            = "IO error %s: %s, user: %s, remoteId: %s";
    private static final String ERROR_NULLPOINTER   = "Null response error %s: %s, user: %s, remoteId: %s";
    private static final String ERROR_NULL_RESPONSE = "No response error %s, user: %s, remoteId: %s";
    private static final String ERORR_FAIL_RESPONSE = "Fail response %s /w error: %s, user: %s, remoteId: %s";

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
        @NonNull Context getApplicationContext();

        /** Вызывается, если синхронизация завершилась с ошибкой. */
        void onSyncFailed();

        /** Вызывается, если синхронизация завершилась успешно, без конфликтов.
         *
         * @param syncStartTime дата начала синхронизации для обновления даты последней синхронизации
         */
        void onSyncSuccess(long syncStartTime);

        /** Выдыватеся, если синхронизация завершилась успешно, но с конфликтами.
         *
         * @param conflicts список конфликтов, каждый элемент состоит из 2 объектов ReadLaterItem
         * @param syncStartTime дата начала синхронизации для обновления даты последней синхронизации
         */
        void onSyncWithConflicts(@NonNull @Size(min = 1) List<ReadLaterItem[]> conflicts, long syncStartTime);

    }


    /////////////////////////
    // Util

    /** Подготавливает API для синхронизации.
     *
     * @return API для синхронизации
     */
    static CloudApiYufimtsev prepareApi() {
        // Подготавливаем Retrofit к получению данных и Moshi к обработке данных
        Moshi moshi = new Moshi.Builder().add(new ReadLaterItemJsonAdapter()).build();
        Retrofit.Builder retrofitBuilder = new Retrofit.Builder()
                .baseUrl(CloudApiYufimtsev.BASE_URL)
                .addConverterFactory(MoshiConverterFactory.create(moshi));

        // Устанавливаем логирование запросов, если дебаг
        if (BuildConfig.DEBUG) {
            HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
            interceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
            OkHttpClient client = new OkHttpClient.Builder().addInterceptor(interceptor).build();
            retrofitBuilder.client(client);
        }

        // Создаем объект для доступа к API
        return retrofitBuilder.build().create(CloudApiYufimtsev.class);
    }

    /** Обновляет заметку на сервере.
     *  Делает записи в Log.e в случае ошибок.
     *
     * @param userId идентификатор пользователя, UserInfo.getCurrentUser().getId()
     * @param remoteId внешний идентификатор заметки
     * @param item заметка в формате ReadLaterItem
     * @return true - если обновление прошло успешно, false иначе
     */
    static boolean updateItemOnServer(@NonNull CloudApiYufimtsev cloudApi,
                                      int userId,
                                      int remoteId,
                                      @NonNull ReadLaterItem item) {
        final String methodName = "update";
        CloudApiYufimtsev.DefaultResponse response;
        try {
            response = cloudApi.updateItem(userId, remoteId, item).execute().body();
        } catch (IOException e) {
            Log.e(TAG_ERROR_CLOUD, String.format(ERROR_IO, methodName, e.toString(), userId, remoteId));
            return false;
        } catch (NullPointerException e) {
            Log.e(TAG_ERROR_CLOUD, String.format(ERROR_NULLPOINTER, methodName, e.toString(), userId, remoteId));
            return false;
        }
        if (response == null) {
            Log.e(TAG_ERROR_CLOUD, String.format(ERROR_NULL_RESPONSE, methodName, userId, remoteId));
            return false;
        } else if (!response.status.equals(CloudApiYufimtsev.STATUS_SUCCESS)) {
            Log.e(TAG_ERROR_CLOUD, String.format(ERORR_FAIL_RESPONSE, methodName, response.error, userId, remoteId));
            return false;
        }
        return true;
    }

    /** Получает все записи с сервера.
     *  Делает записи в Log.e в случае ошибок.
     *
     * @param cloudApi API, полученный из prepareApi
     * @param userId идентификатор пользователя, UserInfo.getCurrentUser().getId()
     * @return все записи на сервере в формате списка из элементов ReadLaterItem, null в случае ошибок
     */
    private static @Nullable List<ReadLaterItem> getAllItemsOnServer(@NonNull CloudApiYufimtsev cloudApi, int userId) {
        final String methodName = "getAll";
        final String remoteId = "all";
        CloudApiYufimtsev.AllItemsResponse response;
        try {
            response = cloudApi.getAllItems(userId).execute().body();
        } catch (IOException e) {
            Log.e(TAG_ERROR_CLOUD, String.format(ERROR_IO, methodName, e.toString(), userId, remoteId));
            return null;
        } catch (NullPointerException e) {
            Log.e(TAG_ERROR_CLOUD, String.format(ERROR_NULLPOINTER, methodName, e.toString(), userId, remoteId));
            return null;
        }
        if (response == null) {
            Log.e(TAG_ERROR_CLOUD, String.format(ERROR_NULL_RESPONSE, methodName, userId, remoteId));
            return null;
        } else if (!response.status.equals(CloudApiYufimtsev.STATUS_SUCCESS)) {
            Log.e(TAG_ERROR_CLOUD, String.format(ERORR_FAIL_RESPONSE, methodName, response.error, userId, remoteId));
            return null;
        }
        return response.data;
    }

    /** Добавляет заметку на сервер.
     *  Делает записи в Log.e в случае ошибок.
     *
     * @param cloudApi API, полученный из prepareApi
     * @param userId идентификатор пользователя, UserInfo.getCurrentUser().getId()
     * @param item заметка в формате ReadLaterItem
     * @return внешний идентификатор заметки на сервере, null в случае ошибок
     */
    private static @Nullable Integer insertItemOnServer(@NonNull CloudApiYufimtsev cloudApi,
                                                        int userId,
                                                        @NonNull ReadLaterItem item) {
        final String methodName = "insert";
        final String remoteId = "no";
        CloudApiYufimtsev.NewItemResponse response;
        try {
            response = cloudApi.createItem(userId, item).execute().body();
        } catch (IOException e) {
            Log.e(TAG_ERROR_CLOUD, String.format(ERROR_IO, methodName, e.toString(), userId, remoteId));
            return null;
        } catch (NullPointerException e) {
            Log.e(TAG_ERROR_CLOUD, String.format(ERROR_NULLPOINTER, methodName, e.toString(), userId, remoteId));
            return null;
        }
        if (response == null) {
            Log.e(TAG_ERROR_CLOUD, String.format(ERROR_NULL_RESPONSE, methodName, userId, remoteId));
            return null;
        } else if (!response.status.equals(CloudApiYufimtsev.STATUS_SUCCESS)) {
            Log.e(TAG_ERROR_CLOUD, String.format(ERORR_FAIL_RESPONSE, methodName, response.error, userId, remoteId));
            return null;
        }
        return response.data;
    }

    /** Удаляет заметку с сервера.
     *  Делает записи в Log.e в случае ошибок.
     * @param cloudApi API, полученный из prepareApi
     * @param userId идентификатор пользователя, UserInfo.getCurrentUser().getId()
     * @param remoteId внешний идентификатор заметки
     * @return true - если обновление прошло успешно, false иначе
     */
    private static boolean deleteItemOnServer(@NonNull CloudApiYufimtsev cloudApi, int userId, int remoteId) {
        final String methodName = "delete";
        CloudApiYufimtsev.DefaultResponse response;
        try {
            response = cloudApi.deleteItem(userId, remoteId).execute().body();
        } catch (IOException e) {
            Log.e(TAG_ERROR_CLOUD, String.format(ERROR_IO, methodName, e.toString(), userId, remoteId));
            return false;
        } catch (NullPointerException e) {
            Log.e(TAG_ERROR_CLOUD, String.format(ERROR_NULLPOINTER, methodName, e.toString(), userId, remoteId));
            return false;
        }
        if (response == null) {
            Log.e(TAG_ERROR_CLOUD, String.format(ERROR_NULL_RESPONSE, methodName, userId, remoteId));
            return false;
        } else if (!response.status.equals(CloudApiYufimtsev.STATUS_SUCCESS)) {
            Log.e(TAG_ERROR_CLOUD, String.format(ERORR_FAIL_RESPONSE, methodName, response.error, userId, remoteId));
            return false;
        }
        return true;
    }


    /////////////////////////
    // AsyncTask

    /** Callback для оповещений о результатах синхронизации. */
    private @Nullable SyncCallback mSyncCallback;
    /** Дата последней синхронизации. */
    private long lastSync = 0;
    /** Дата начала синхронизации. */
    private long syncStartTime = 0;

    SyncAsyncTask(@Nullable SyncCallback callback) {
        mSyncCallback = callback;
    }

    /** Устанавливает колбек для этого таска.
     *
     * @param callback новый колбек, может быть null, если нужно отписаться
     */
    void setCallback(@Nullable SyncCallback callback) {
        mSyncCallback = callback;
    }

    /** Объект для передачи данных из doInBackground в onPostExecute. */
    class SyncResult {

        /** Признак успешности синхронизации. */
        private final boolean isSuccessful;

        /** Список конфликтов, каждый элемент состоит из 2 объектов ReadLaterItem. */
        private final @NonNull List<ReadLaterItem[]> conflicts;

        /** Создает новый объект SyncResult с ошибкой. */
        private SyncResult() {
            this.isSuccessful = false;
            this.conflicts = Collections.emptyList();
        }

        /** Создает новый объект SyncResult с успешным принаком и списком конфликтов.
         *
         * @param conflicts список конфликтов, каждый элемент состоит из 2 объектов ReadLaterItem.
         */
        private SyncResult(@NonNull List<ReadLaterItem[]> conflicts) {
            this.isSuccessful = true;
            this.conflicts = conflicts;
        }
    }

    @Override
    protected void onPreExecute() {
        // Проверяем, есть ли SyncCallback и есть ли подлкючение к сети
        if (mSyncCallback != null) {
            lastSync = mSyncCallback.getLastSync();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss.SSSZ", Locale.US);
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
        final Context appContext            = mSyncCallback.getApplicationContext();
        final CloudApiYufimtsev cloudApi    = prepareApi();
        final int userId = UserInfo.getCurentUser(appContext).getUserId();

        // Список всех идентификаторов заметок на сервере
        List<Integer> allServerIds = new ArrayList<>();
        List<ReadLaterItem[]> conflicts = new ArrayList<>();
        {
            // Получили список всех заметок с сервера, сохранили
            List<ReadLaterItem> itemsOnServer = getAllItemsOnServer(cloudApi, userId);
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
                                Log.d(TAG_SYNC, "Updating Local: " + remoteId + ", item: " + itemServer.toString());
                                ReadLaterDbUtils.updateItem(appContext, itemServer, userId, remoteId);
                            } else {
                                // Server: есть, изменен; Local: есть, изменен
                                if (!itemLocal.equalsByContent(itemServer)) {
                                    // Разбор конфликтов пользователем происходит только если не равны содержательно
                                    conflicts.add(new ReadLaterItem[]{itemServer, itemLocal});
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
                                    updateItemOnServer(cloudApi, userId, remoteId, savingItem);
                                    ReadLaterDbUtils.updateItem(appContext, savingItem, userId, remoteId);
                                    Log.d(TAG_SYNC, "Auto merge: " + remoteId + ", item: " + itemServer.toString());
                                }
                            }
                        } else {
                            // Делим локальные заметки на измененные, без изменений и нет
                            ReadLaterItem itemLocal = ReadLaterDbUtils.getItemByRemoteId(appContext, userId, remoteId);
                            if (itemLocal == null) {
                                // Server: есть, без изм.; Local: нет
                                Log.d(TAG_SYNC, "Deleting Server: " + remoteId);
                                if (!deleteItemOnServer(cloudApi, userId, remoteId)) {
                                    return new SyncResult();
                                }
                            } else if (itemLocal.getDateModified() <= lastSync)  {
                                // Server: есть, без изм.; Local: есть, без изм.
                                //noinspection UnnecessaryContinue
                                continue;
                            } else {
                                // Server: есть, без изм.; Local: есть, изменен
                                Log.d(TAG_SYNC, "Updating Server: " + itemLocal.toString());
                                ReadLaterItem.Builder itemBuilder = new ReadLaterItem.Builder(itemLocal);
                                itemBuilder.dateModified(System.currentTimeMillis());
                                if (!updateItemOnServer(cloudApi, userId, remoteId, itemBuilder.build())) {
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
                        Log.d(TAG_SYNC, "Inserting Server: " + savingItem.toString());
                        Integer newRemoteId = insertItemOnServer(cloudApi, userId, savingItem);
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
