package com.example.mborzenkov.readlaterlist.networking;

import android.support.annotation.Nullable;

import dagger.Module;
import dagger.Provides;
import javax.inject.Singleton;
import okhttp3.HttpUrl;

@SuppressWarnings("unused") // Dagger
@Module
public class CloudApiModule {

    private final HttpUrl mBaseUrl;

    /** Модуль Dagger 2 для инъекций HttpUrl в CloudApi. */
    public CloudApiModule(@Nullable HttpUrl baseUrl) {
        if (baseUrl == null) {
            mBaseUrl = CloudApiYufimtsev.BASE_URL;
        } else {
            mBaseUrl = baseUrl;
        }
    }

    /** Предоставляет установленный HttpUrl. */
    @Provides
    @Singleton
    public HttpUrl provideBaseUrl() {
        return mBaseUrl;
    }

}