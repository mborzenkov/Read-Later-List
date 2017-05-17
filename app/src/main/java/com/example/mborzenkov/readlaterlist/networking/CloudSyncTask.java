package com.example.mborzenkov.readlaterlist.networking;

import android.content.Context;
import android.database.Cursor;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.example.mborzenkov.readlaterlist.BuildConfig;
import com.example.mborzenkov.readlaterlist.adt.ReadLaterItem;
import com.example.mborzenkov.readlaterlist.adt.ReadLaterItemDbAdapter;
import com.example.mborzenkov.readlaterlist.adt.ReadLaterItemJsonAdapter;
import com.example.mborzenkov.readlaterlist.adt.UserInfo;
import com.example.mborzenkov.readlaterlist.data.ReadLaterContract;
import com.example.mborzenkov.readlaterlist.utility.ReadLaterDbUtils;
import com.squareup.moshi.Moshi;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.moshi.MoshiConverterFactory;

public class CloudSyncTask extends AsyncTask<Void, Void, CloudSyncTask.SyncResult> {

    // Автосинхронизация
    // TODO: Бродкаст ресивер на подключение к интернету, вызвает полную синхронизацию
    // TODO: Добавить вызов синхронизации каждые N минут (позднее и позднее)

    // Завершение
    // TODO: Документация CloudSyncTask
    // TODO: Документация MainListSyncFragment
    // TODO: Коммент про context - SyncCallback в SyncFragment
    // TODO: Документация ReadLaterDbUtils
    // TODO: Документация UserInfo
    // TODO: Инспекторы

    private static final String TAG_ERROR_NETWORK   = "Network Error";
    private static final String TAG_ERROR_CLOUD     = "CloudApi Error";
    private static final String TAG_SYNC            = "SYNC";

    private static final String ERROR_NETWORK       = "Network not connected";
    private static final String ERROR_IO            = "IO error %s: %s, user: %s, remoteId: %s";
    private static final String ERROR_NULLPOINTER   = "Null response error %s: %s, user: %s, remoteId: %s";
    private static final String ERROR_NULL_RESPONSE = "No response error %s, user: %s, remoteId: %s";
    private static final String ERORR_FAIL_RESPONSE = "Fail response %s /w error: %s, user: %s, remoteId: %s";

    public interface SyncCallback {

        String SYNC_KEY = "com.example.mborzenkov.mainlist.sync";
        /** Ключ для даты последней синхронизации в SharedPreferences. */
        String LAST_SYNC_KEY = "lastsync";


        boolean isNetworkConnected();
        long getLastSync();
        Context getApplicationContext();
        void onSyncFailed();
        void onSyncSuccess(long syncStartTime);
        void onSyncWithConflicts(List<ReadLaterItem[]> conflicts, long syncStartTime);
    }

    private @Nullable SyncCallback mSyncCallback;
    private long lastSync = 0;
    private long syncStartTime = 0;

    public CloudSyncTask(@Nullable SyncCallback callback) {
        mSyncCallback   = callback;
    }

    class SyncResult {

        private final boolean isSuccessful;
        private final @Nullable List<ReadLaterItem[]> conflicts;

        private SyncResult() {
            this.isSuccessful = false;
            this.conflicts = null;
        }

        private SyncResult(List<ReadLaterItem[]> conflicts) {
            this.isSuccessful = true;
            this.conflicts = conflicts;
        }
    }

    @Override
    protected void onPreExecute() {
        if (mSyncCallback != null) {
            lastSync = mSyncCallback.getLastSync();
            if (!mSyncCallback.isNetworkConnected()) {
                Log.e(TAG_ERROR_NETWORK, ERROR_NETWORK);
                mSyncCallback.onSyncFailed();
                cancel(true);
            }
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
                                Log.d(TAG_SYNC, "Updating Local: " + remoteId);
                                ReadLaterDbUtils.updateItemByRemoteId(appContext, userId, itemServer, remoteId);
                            } else {
                                // Server: есть, изменен; Local: есть, изменен
                                if (!itemLocal.equals(itemServer)) {
                                    Log.d(TAG_SYNC, "Conflict: " + remoteId);
                                    conflicts.add(new ReadLaterItem[]{itemServer, itemLocal});
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
                                continue;
                            } else {
                                // Server: есть, без изм.; Local: есть, изменен
                                Log.d(TAG_SYNC, "Updating Server: " + remoteId);
                                if (!updateItemOnServer(cloudApi, userId, remoteId, itemLocal)) {
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
                        Log.d(TAG_SYNC, "Inserting Server: " + remoteId);
                        Integer newRemoteId = insertItemOnServer(cloudApi, userId, itemLocal);
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
        }

        return new SyncResult(conflicts);
    }

    @Override
    protected void onPostExecute(@Nullable SyncResult syncResult) {
        if (mSyncCallback != null) {
            if (syncResult == null || !syncResult.isSuccessful) {
                mSyncCallback.onSyncFailed();
            } else {
                if (syncResult.conflicts != null) {
                    mSyncCallback.onSyncWithConflicts(syncResult.conflicts, syncStartTime);
                } else {
                    mSyncCallback.onSyncSuccess(syncStartTime);
                }
            }
        }
    }

    private static CloudApiYufimtsev prepareApi() {
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

    private @Nullable List<ReadLaterItem> getAllItemsOnServer(@NonNull CloudApiYufimtsev cloudApi, int userId) {
        final String methodName = "getAll";
        final String remoteId = "all";
        CloudApiYufimtsev.AllItemsResponse response = null;
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

    private @Nullable Integer insertItemOnServer(@NonNull CloudApiYufimtsev cloudApi,
                                                 int userId,
                                                 @NonNull ReadLaterItem item) {
        final String methodName = "insert";
        final String remoteId = "no";
        CloudApiYufimtsev.NewItemResponse response = null;
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

    private boolean updateItemOnServer(@NonNull CloudApiYufimtsev cloudApi,
                                       int userId,
                                       int remoteId,
                                       @NonNull ReadLaterItem item) {
        final String methodName = "update";
        CloudApiYufimtsev.DefaultResponse response = null;
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

    private boolean deleteItemOnServer(@NonNull CloudApiYufimtsev cloudApi, int userId, int remoteId) {
        final String methodName = "delete";
        CloudApiYufimtsev.DefaultResponse response = null;
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

}
