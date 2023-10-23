package com.jz.util;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.util.Log;


public class AppApplication extends Application {

    public static final String TAG = Application.class.getSimpleName();
    private static AppApplication instance = null;
    public static Context context;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate");
        context = getApplicationContext();
        instance = this;
        startService(new Intent(this, MainService.class));
    }
    public static Context getContext() {
        return context;
    }
    public static AppApplication getInstance() {
        return instance;
    }

    /**
     * @param _context
     * @return
     */
    private String getLocalVersionName(Context _context) {
        String localVersion = "";
        try {
            PackageInfo packageInfo = _context.getApplicationContext().getPackageManager().getPackageInfo(_context.getPackageName(), 0);
            localVersion = packageInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return localVersion;
    }

}
