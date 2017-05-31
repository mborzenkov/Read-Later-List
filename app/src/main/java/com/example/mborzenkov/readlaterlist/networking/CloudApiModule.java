package com.example.mborzenkov.readlaterlist.networking;

import android.support.annotation.Nullable;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import okhttp3.HttpUrl;

@Module
public class CloudApiModule {

    private final HttpUrl mBaseUrl;

    public CloudApiModule(@Nullable HttpUrl baseUrl) {
        if (baseUrl == null) {
            mBaseUrl = CloudApiYufimtsev.BASE_URL;
        } else {
            mBaseUrl = baseUrl;
        }
    }

    @Provides
    @Singleton
    public HttpUrl provideBaseUrl() {
        return mBaseUrl;
    }

}