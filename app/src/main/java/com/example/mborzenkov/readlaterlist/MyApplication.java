package com.example.mborzenkov.readlaterlist;

import android.app.Application;
import android.support.annotation.NonNull;

import com.example.mborzenkov.readlaterlist.networking.CloudApiComponent;
import com.example.mborzenkov.readlaterlist.networking.CloudApiModule;

public class MyApplication extends Application {

    private CloudApiComponent mCloudApiComponent;

    @Override
    public void onCreate() {
        super.onCreate();
        if (mCloudApiComponent == null) {
            mCloudApiComponent = DaggerCloudApiComponent.builder().cloudApiModule(new CloudApiModule(null)).build();
        }
    }

    public void setCloudApiComponent(@NonNull CloudApiComponent cloudApiComponent) {
        mCloudApiComponent = cloudApiComponent;
    }

    public CloudApiComponent getCloudApiComponent() {
        return mCloudApiComponent;
    }

}
