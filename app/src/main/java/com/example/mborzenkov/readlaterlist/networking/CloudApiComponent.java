package com.example.mborzenkov.readlaterlist.networking;

import dagger.Component;
import javax.inject.Singleton;

@Singleton
@Component(modules = { CloudApiModule.class })
public interface CloudApiComponent {
    void inject(ReadLaterCloudApi cloudApi);
}