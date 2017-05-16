package com.example.mborzenkov.readlaterlist.networking;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.AsyncTask;
import android.support.annotation.Nullable;
import android.util.Log;

import com.example.mborzenkov.readlaterlist.BuildConfig;
import com.example.mborzenkov.readlaterlist.adt.ReadLaterItem;
import com.example.mborzenkov.readlaterlist.adt.ReadLaterItemJsonAdapter;
import com.example.mborzenkov.readlaterlist.adt.UserInfo;
import com.example.mborzenkov.readlaterlist.utility.ReadLaterDbUtils;
import com.squareup.moshi.Moshi;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.moshi.MoshiConverterFactory;

public class CloudSyncTask extends AsyncTask<Void, Void, CloudSyncTask.SyncResult> {

    // Автосинхронизация
    // TODO: Добавить синхронизацию при открытии приложения
    // TODO: Бродкаст ресивер на подключение к интернету, вызвает полную синхронизацию
    // TODO: Добавить вызов синхронизации каждые N минут (позднее и позднее)
    // TODO: Документация этого класса

    // Выполнение синхронизации
    // TODO: Выполнить все запланированные операции Insert, Update, Delete
    // TODO: Собрать и передать conflict

    // Разбор конфликтов
    // TODO: Принять и разобрать конфликты в MainActivity
    // TODO: Создать layout для разбора конфликтов
    // TODO: Открыть фрагмент для выбора изменений и сохранять их по ходу дела

    // Пользователи
    // TODO: layout регистрации
    // TODO: Вместо названия приложения сделать имя пользоваетеля, выпадающий список с пунктом "Выйти"
    // TODO: Выйти сбрасывает текущего выбранного и открывает вновь layout регистрации
    // TODO: Сохранение и восстановление последнего выбранного Shared Preferences
    // TODO: cancel sync при смене пользователя

    // Завершение
    // TODO: Документация CloudSyncTask
    // TODO: Документация MainListSyncFragment
    // TODO: Коммент про context - SyncCallback в SyncFragment
    // TODO: Документация UserInfo

    public interface SyncCallback {
        /** Ключ для даты последней синхронизации в SharedPreferences. */
        String LAST_SYNC_KEY = "com.example.mborzenkov.mainlist.sync.lastsync";

        boolean isNetworkConnected();
        long getLastSync();
        Context getApplicationContext();
        void handleConflicts(List<ReadLaterItem[]> conflicts);
        void onSyncFinished();
        void onSyncFinished(long syncStartTime);
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
                mSyncCallback.onSyncFinished();
                cancel(true);
            }
        }
        syncStartTime = System.currentTimeMillis();
    }

    @Override
    protected SyncResult doInBackground(Void... params) {

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
        final int userId = UserInfo.getCurentUser().getUserId();

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
                    final Integer remoteId = itemServer.getRemoteId();
                    if (remoteId != null) {

                        allServerIds.add(remoteId);
                        // Делим заметки на измененные и не измененные
                        if (itemServer.getDateModified() > lastSync) {
                            // Делим локальные заметки на измененные, без изменений и нет
                            ReadLaterItem itemLocal = ReadLaterDbUtils.getItemByRemoteId(appContext, userId, remoteId);
                            if (itemLocal == null) {
                                // Server: есть, изменен; Local: нет
                                // TODO: Insert Local
                                // ReadLaterDbUtils.insertItem(appContext, serverItem);
                                Log.d("SYNC", "Inserting Local: " + remoteId);
                            } else if (itemLocal.getDateModified() <= lastSync) {
                                // Server: есть, изменен; Local: есть, без изм.
                                // TODO: Update Local
                                Log.d("SYNC", "Updating Local: " + remoteId);
                            } else {
                                // Server: есть, изменен; Local: есть, изменен
                                if (!itemServer.equals(itemLocal)) {
                                    conflicts.add(new ReadLaterItem[]{itemServer, itemLocal});
                                } else {
                                    // TODO: Update Local
                                }
                                Log.d("SYNC", "Conflict: " + remoteId);
                            }
                        } else {
                            // Делим локальные заметки на измененные, без изменений и нет
                            ReadLaterItem itemLocal = ReadLaterDbUtils.getItemByRemoteId(appContext, userId, remoteId);
                            if (itemLocal == null) {
                                // Server: есть, без изм.; Local: нет
                                // TODO: Delete Server
                                Log.d("SYNC", "Deleting Server: " + remoteId);
                            } else if (itemLocal.getDateModified() <= lastSync)  {
                                // Server: есть, без изм.; Local: есть, без изм.
                                continue;
                            } else {
                                // Server: есть, без изм.; Local: есть, изменен
                                // TODO: Update Server
                                Log.d("SYNC", "Updating Server: " + remoteId);
                            }
                        }

                    }
                }
            }
        }

        {
            // Получили список всех заметок локальных
            List<ReadLaterItem> itemsLocal = ReadLaterDbUtils.getAllItems(appContext, userId);
            if (itemsLocal == null) {
                return new SyncResult();
            }
            // Обрабатываем все, что есть локально, но нет на сервере
            for (ReadLaterItem localItem : itemsLocal) {
                // Проверяем на null, получаем remoteId
                if (localItem != null) {
                    final Integer remoteId = localItem.getRemoteId();

                    // Отсекаем то, что уже проверяли
                    if (allServerIds.contains(localItem.getRemoteId())) {
                        continue;
                    }

                    // Делим заметки на измененные и не измененные
                    if (localItem.getDateModified() > lastSync) {
                        // Server: нет; Local: есть, изменен
                        // TODO: Insert Server
                        Log.d("SYNC", "Inserting Server: " + remoteId);
                    } else {
                        // Server: нет; Local: есть, без изм.
                        // TODO: Delete Local
                        Log.d("SYNC", "Deleting Local: " + remoteId);
                        // Тут почти наверняка есть remoteId, удаляем по нему
                    }
                }
            }
        }

        if (conflicts == null) {
            return new SyncResult();
        }
        // TODO: SyncResult(conflicts)
        return new SyncResult();
    }

    @Override
    protected void onPostExecute(SyncResult syncResult) {
        if (mSyncCallback != null) {
            if (syncResult == null || !syncResult.isSuccessful) {
                mSyncCallback.onSyncFinished();
            } else {
                if (syncResult.conflicts != null) {
                    mSyncCallback.handleConflicts(syncResult.conflicts);
                }
                mSyncCallback.onSyncFinished(syncStartTime);
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

    private List<ReadLaterItem> getAllItemsOnServer(CloudApiYufimtsev cloudApi, int userId) {
        List<ReadLaterItem> result = null;
        try {
            result = cloudApi.getAllItems(userId).execute().body().data;
        } catch (IOException e) {
            // TODO: Log.e
            return null;
        } catch (NullPointerException e) {
            // TODO: Log.e
            return null;
        }
        return result;
    }

    // TODO: Обработка конфликтов изменений
    // Если при синхронизации становится известно, что произошёл конфликт изменений (например, на сервере и локально
    // у одной и той же заметки поменяли описание), нужно предложить пользователю выбрать, какой из вариантов сохранить.
    // TODO: layout сравнения и выбор

}
