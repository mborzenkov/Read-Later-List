package com.example.mborzenkov.readlaterlist.networking;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.example.mborzenkov.readlaterlist.BuildConfig;
import com.example.mborzenkov.readlaterlist.adt.ReadLaterItem;
import com.example.mborzenkov.readlaterlist.adt.ReadLaterItemJsonAdapter;
import com.squareup.moshi.Moshi;

import java.io.IOException;
import java.util.List;

import javax.inject.Inject;

import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.moshi.MoshiConverterFactory;

/** Класс для синхронизации объектов ReadLaterItem с облачным API.
 * Методы для обращения к серверу вызывают NetworkOnMainThreadException, если исполняются в основном потоке
 */
public class ReadLaterCloudApi {

    /////////////////////////
    // Константы

    private static final String TAG_ERROR_CLOUD     = "CloudApi Error";

    private static final String ERROR_IO            = "IO error %s: %s, user: %s, remoteId: %s";
    private static final String ERROR_NULL_RESPONSE = "NULL response body error %s, user: %s, remoteId: %s";
    private static final String ERORR_FAIL_RESPONSE = "Fail response %s /w error: %s, user: %s, remoteId: %s";
    private static final String ERROR_NO_STATUS_RESPONSE = "Malformed response (no status) %s, user: %s. remoteId: %s";
    private static final String ERROR_NO_DATA_RESPONSE = "Malformed response (no data) %s, user: %s. remoteId: %s";
    private static final String ERROR_NO_ERROR_RESPONSE = "Malformed fail response (no err) %s, user: %s. remoteId: %s";


    /////////////////////////
    // Объект

    /** URL сервера для подключения. */
    @SuppressWarnings("WeakerAccess")
    @Inject
    HttpUrl mBaseUrl;
    /** Интерфейс для работы с сервером. */
    private final @NonNull CloudApiYufimtsev mCloudApi;

    /** Создает новый объект для синхронизации объектов ReadLaterItem с сервером.
     * Устанавливает логирование запросов, если BuildConfig.DEBUG.
     */
    public ReadLaterCloudApi(CloudApiComponent component) {
        component.inject(this);
        mCloudApi = prepareApi();
    }

    /** Подготавливает API для синхронизации.
     * Устанавливает логирование запросов, если BuildConfig.DEBUG.
     *
     * @return API для синхронизации
     */
    private CloudApiYufimtsev prepareApi() {
        // Подготавливаем Retrofit к получению данных и Moshi к обработке данных
        Moshi moshi = new Moshi.Builder().add(new ReadLaterItemJsonAdapter()).build();
        Retrofit.Builder retrofitBuilder = new Retrofit.Builder()
                .baseUrl(mBaseUrl)
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

    /** Получает все записи с сервера.
     *  Делает записи в Log.e в случае ошибок (при возврате null).
     *
     * @param userId идентификатор пользователя, UserInfoUtils.getCurrentUser().getId()
     *
     * @return все записи на сервере в формате списка из элементов ReadLaterItem, null в случае ошибок
     *
     * @throws android.os.NetworkOnMainThreadException если исполняется в основном потоке
     */
    public @Nullable List<ReadLaterItem> getAllItemsOnServer(int userId) {
        final String method = "getAll";
        final String remoteId = "all";
        CloudApiYufimtsev.AllItemsResponse response;
        try {
            response = mCloudApi.getAllItems(userId).execute().body();
        } catch (IOException e) {
            Log.e(TAG_ERROR_CLOUD, String.format(ERROR_IO, method, e.toString(), userId, remoteId));
            return null;
        }
        if (response == null) {
            Log.e(TAG_ERROR_CLOUD, String.format(ERROR_NULL_RESPONSE, method, userId, remoteId));
            return null;
        } else if (response.status == null) {
            Log.e(TAG_ERROR_CLOUD, String.format(ERROR_NO_STATUS_RESPONSE, method, userId, remoteId));
            return null;
        } else if (response.status.equals(CloudApiYufimtsev.STATUS_SUCCESS)) {
            if (response.data == null) {
                Log.e(TAG_ERROR_CLOUD, String.format(ERROR_NO_DATA_RESPONSE, method, userId, remoteId));
                return null;
            }
        } else {
            if (response.error == null) {
                Log.e(TAG_ERROR_CLOUD, String.format(ERROR_NO_ERROR_RESPONSE, method, userId, remoteId));
                return null;
            } else {
                Log.e(TAG_ERROR_CLOUD, String.format(ERORR_FAIL_RESPONSE, method, response.error, userId, remoteId));
                return null;
            }
        }
        return response.data;
    }

    /** Добавляет заметку на сервер.
     *  Делает записи в Log.e в случае ошибок (при возврате null).
     *
     * @param userId идентификатор пользователя, UserInfoUtils.getCurrentUser().getId()
     * @param item заметка в формате ReadLaterItem
     *
     * @return внешний идентификатор заметки на сервере, null в случае ошибок
     *
     * @throws android.os.NetworkOnMainThreadException если исполняется в основном потоке
     */
    public @Nullable Integer insertItemOnServer(int userId, @NonNull ReadLaterItem item) {
        final String method = "insert";
        final String remoteId = "no";
        CloudApiYufimtsev.NewItemResponse response;
        try {
            response = mCloudApi.createItem(userId, item).execute().body();
        } catch (IOException e) {
            Log.e(TAG_ERROR_CLOUD, String.format(ERROR_IO, method, e.toString(), userId, remoteId));
            return null;
        }
        if (response == null) {
            Log.e(TAG_ERROR_CLOUD, String.format(ERROR_NULL_RESPONSE, method, userId, remoteId));
            return null;
        } else if (response.status == null) {
            Log.e(TAG_ERROR_CLOUD, String.format(ERROR_NO_STATUS_RESPONSE, method, userId, remoteId));
            return null;
        } else if (response.status.equals(CloudApiYufimtsev.STATUS_SUCCESS)) {
            if (response.data == null) {
                Log.e(TAG_ERROR_CLOUD, String.format(ERROR_NO_DATA_RESPONSE, method, userId, remoteId));
                return null;
            }
        } else {
            if (response.error == null) {
                Log.e(TAG_ERROR_CLOUD, String.format(ERROR_NO_ERROR_RESPONSE, method, userId, remoteId));
                return null;
            } else {
                Log.e(TAG_ERROR_CLOUD, String.format(ERORR_FAIL_RESPONSE, method, response.error, userId, remoteId));
                return null;
            }
        }
        return response.data;
    }

    /** Обновляет заметку на сервере.
     *  Делает записи в Log.e в случае ошибок (при возврате false).
     *
     * @param userId идентификатор пользователя, UserInfoUtils.getCurrentUser().getId()
     * @param remoteId внешний идентификатор заметки
     * @param item заметка в формате ReadLaterItem
     *
     * @return true - если обновление прошло успешно, false иначе
     *
     * @throws android.os.NetworkOnMainThreadException если исполняется в основном потоке
     */
    public boolean updateItemOnServer(int userId, int remoteId, @NonNull ReadLaterItem item) {
        final String method = "update";
        CloudApiYufimtsev.DefaultResponse response;
        try {
            response = mCloudApi.updateItem(userId, remoteId, item).execute().body();
        } catch (IOException e) {
            Log.e(TAG_ERROR_CLOUD, String.format(ERROR_IO, method, e.toString(), userId, remoteId));
            return false;
        }
        if (response == null) {
            Log.e(TAG_ERROR_CLOUD, String.format(ERROR_NULL_RESPONSE, method, userId, remoteId));
            return false;
        } else if (response.status == null) {
            Log.e(TAG_ERROR_CLOUD, String.format(ERROR_NO_STATUS_RESPONSE, method, userId, remoteId));
            return false;
        } else if (response.status.equals(CloudApiYufimtsev.STATUS_ERROR)) {
            if (response.error == null) {
                Log.e(TAG_ERROR_CLOUD, String.format(ERROR_NO_ERROR_RESPONSE, method, userId, remoteId));
                return false;
            } else {
                Log.e(TAG_ERROR_CLOUD, String.format(ERORR_FAIL_RESPONSE, method, response.error, userId, remoteId));
                return false;
            }
        }
        return true;
    }

    /** Удаляет заметку с сервера.
     *  Делает записи в Log.e в случае ошибок  (при возврате false).
     *
     * @param userId идентификатор пользователя, UserInfoUtils.getCurrentUser().getId()
     * @param remoteId внешний идентификатор заметки
     *
     * @return true - если обновление прошло успешно, false иначе
     *
     * @throws android.os.NetworkOnMainThreadException если исполняется в основном потоке
     */
    public boolean deleteItemOnServer(int userId, int remoteId) {
        final String method = "delete";
        CloudApiYufimtsev.DefaultResponse response;
        try {
            response = mCloudApi.deleteItem(userId, remoteId).execute().body();
        } catch (IOException e) {
            Log.e(TAG_ERROR_CLOUD, String.format(ERROR_IO, method, e.toString(), userId, remoteId));
            return false;
        }
        if (response == null) {
            Log.e(TAG_ERROR_CLOUD, String.format(ERROR_NULL_RESPONSE, method, userId, remoteId));
            return false;
        } else if (response.status == null) {
            Log.e(TAG_ERROR_CLOUD, String.format(ERROR_NO_STATUS_RESPONSE, method, userId, remoteId));
            return false;
        } else if (response.status.equals(CloudApiYufimtsev.STATUS_ERROR)) {
            if (response.error == null) {
                Log.e(TAG_ERROR_CLOUD, String.format(ERROR_NO_ERROR_RESPONSE, method, userId, remoteId));
                return false;
            } else {
                Log.e(TAG_ERROR_CLOUD, String.format(ERORR_FAIL_RESPONSE, method, response.error, userId, remoteId));
                return false;
            }
        }
        return true;
    }

}
