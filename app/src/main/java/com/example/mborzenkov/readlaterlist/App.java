package com.example.mborzenkov.readlaterlist;

import android.app.Application;
import android.content.Context;
import android.util.Log;

public class App extends Application {

    private static Context mContext;

    @Override
    public void onCreate() {
        super.onCreate();
        mContext = this;
    }

    public static Context getContext() {
        return mContext;
    }
}
