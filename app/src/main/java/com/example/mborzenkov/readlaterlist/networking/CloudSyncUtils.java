package com.example.mborzenkov.readlaterlist.networking;

import android.graphics.Color;
import android.util.Log;

import com.example.mborzenkov.readlaterlist.adt.ReadLaterItem;
import com.example.mborzenkov.readlaterlist.adt.ReadLaterItemJsonAdapter;
import com.example.mborzenkov.readlaterlist.adt.UserInfo;
import com.squareup.moshi.Moshi;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.moshi.MoshiConverterFactory;

public class CloudSyncUtils {

    // TODO: Добавить layout refresh и вызывать по потягиванию синхронизацию
    // TODO: Предусмотреть только один вызов синхронизации, не делать вторую, пока первая не закончилась
    // TODO: Колбек на обновление UI
    // TODO: Отправка данных на сервер асинхронно
    // TODO: Сохранение даты старте синхронизации в переменную, получение LastSync из SharedPreferences
    // TODO: Выполнение синхронизации
    // TODO: При успешном завершении - обновление SharedPreferences
    // TODO: Бродкаст ресивер на подключение к интернету, вызвает полную синхронизацию
    // TODO: Добавить вызов синхронизации каждые N минут (позднее и позднее)

    public static boolean startFullSync() {

        HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
        interceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
        OkHttpClient client = new OkHttpClient.Builder().addInterceptor(interceptor).build();

        Moshi moshi = new Moshi.Builder().add(new ReadLaterItemJsonAdapter()).build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(CloudApiYufimtsev.BASE_URL)
                .client(client)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .build();

        CloudApiYufimtsev cloudApi = retrofit.create(CloudApiYufimtsev.class);

        // Проверить наличие подключения изначально.

        // Получили список всех заметок с сервера, сохранили
        //
        // Цикл по всем заметкам сервера
        //      LastModifiedServer > LastSync
        //          Есть в локальной базе
        //              LastModifiedLocal <= LastSync
        //                  Update Local
        //              Иначе
        //                  - Конфликт, меняем modified на lastSync + 1
        //          Иначе
        //              Insert Local
        //      Иначе
        //          Есть в локальной базе
        //              continue; (случай разбирается дальше)
        //          Иначе
        //              Delete Server
        //
        // Цикл по всем заметкам локальным, кроме уже проверенных
        //      LastModifiedLocal > LastSync
        //          Есть на сервере
        //              LastModifiedServer <= LastSync
        //                  Update Server
        //              Иначе
        //                  - Конфликт, меняем modified на lastSync + 1
        //          Иначе
        //              Insert Server
        //              Update Local itemId
        //      Иначе
        //          Есть на сервере
        //              continue; (изменений нет ни на сервере, ни локально)
        //          Иначе
        //              Delete Local
        //
        // Если выше все успешно, записываем StartSync в SharedPreferences
        //
        // Обработка конфликтов - показать список заметок (если не принять изменения, при следующей синхронизации
        // снова вылезут эти же конфликты)



        return false;
    }

}
