package com.tarasantoshchuk.permissionsmanager.sample;

import android.app.Application;

import com.tarasantoshchuk.permissionsmanager.PermissionsManager;


public class MyApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        PermissionsManager.init(this);
    }
}
