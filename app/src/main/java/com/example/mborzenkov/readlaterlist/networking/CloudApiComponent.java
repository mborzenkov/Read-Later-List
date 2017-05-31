package com.example.mborzenkov.readlaterlist.networking;

import javax.inject.Singleton;

import dagger.Component;

@Singleton
@Component(modules = { CloudApiModule.class })
public interface CloudApiComponent {
    void inject(ReadLaterCloudApi cloudApi);
}