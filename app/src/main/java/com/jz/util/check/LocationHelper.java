package com.jz.util.check;

import android.content.Context;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.lang.ref.WeakReference;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class LocationHelper {

    private static final String TAG = LocationHelper.class.getSimpleName();
    public static final String ACTION_LOCATION_CHANGED_INFO = "xl.action.LOCATION_CHANGED_INFO";
    public static final String EXTRA_SPEED = "EXTRA_SPEED";
    private static final int MSG_ID_LOCATION_CHANGED_INFO = 1;
    private static LocationHelper instance;
    private Context mContext;
    private LocationManager mLocationManager;
    private MyHandler mHandler = new MyHandler(this);

    private LocationHelper(Context context) {
        mContext = context;
    }

    public static LocationHelper getInstance(Context context) {
        if (instance == null) {
            instance = new LocationHelper(context);
        }

        return instance;
    }

    private static class MyHandler extends Handler {
        private WeakReference<LocationHelper> mHelper;

        private MyHandler(LocationHelper helper) {
            this.mHelper = new WeakReference<>(helper);
        }

        @Override
        public void handleMessage(Message msg) {
            LocationHelper helper = mHelper.get();
            if (helper != null) {
                switch (msg.what) {
                    case MSG_ID_LOCATION_CHANGED_INFO: {
                        break;
                    }
                }
            }
        }
    }

    private final LocationListener mLocationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            synchronized (mGpsCallbacks) {
                mTmpListeners.clear();
                mTmpListeners.addAll(mGpsCallbacks);
            }
            for (int i = mTmpListeners.size() - 1; i >= 0; i--) {
                mTmpListeners.get(i).onLocationChanged(location);
            }
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle bundle) {
        }

        @Override
        public void onProviderEnabled(String provider) {
        }

        @Override
        public void onProviderDisabled(String provider) {
        }

    };

    private final GpsStatus.Listener mGpsStatusListener = new GpsStatus.Listener() {
        @Override
        public void onGpsStatusChanged(int event) {
            synchronized (mGpsCallbacks) {
                mTmpListeners.clear();
                mTmpListeners.addAll(mGpsCallbacks);
            }
            for (int i = mTmpListeners.size() - 1; i >= 0; i--) {
                mTmpListeners.get(i).onGpsStatusChanged(event);
            }
        }
    };

    public LocationManager getLocationManager() {
        if (mLocationManager == null) {
            mLocationManager = (LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE);
        }
        return mLocationManager;
    }

    public boolean requestLocation() {
        LocationManager locationManager = getLocationManager();
        if (locationManager == null) {
            return false;
        }

        locationManager.addGpsStatusListener(mGpsStatusListener);
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0, mLocationListener);

        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            return false;
        }
        return true;
    }

    public void test(){
        synchronized (mGpsCallbacks) {
            mTmpListeners.clear();
            mTmpListeners.addAll(mGpsCallbacks);
        }
        for (int i = mTmpListeners.size() - 1; i >= 0; i--) {
            mTmpListeners.get(i).onGpsStatusChanged(2);
            mTmpListeners.get(i).onLocationChanged(null);
        }
    }

    public void removeLocation() {
        if (mLocationManager != null) {
            mLocationManager.removeGpsStatusListener(mGpsStatusListener);
            mLocationManager.removeUpdates(mLocationListener);
        }
        mLocationManager = null;
        mHandler.removeCallbacksAndMessages(null);
    }

    private String scale(Double doubleValue) {
        Log.i(TAG, "scale - doubleValue: " + doubleValue);
        DecimalFormat format = new DecimalFormat("0.##");
        String scaled = format.format(doubleValue);
        return scaled;
    }

    private final List<GpsCallbacks> mGpsCallbacks = new ArrayList<>();
    private final List<GpsCallbacks> mTmpListeners = new ArrayList<>();

    public interface GpsCallbacks {
        default void onGpsStatusChanged(int event){};
        default void onLocationChanged(Location location){};
        default void onTianqiChanged(String city, String temp, String temp_text, int icon){};
    }

    public void setListenerGpsCallbacks(GpsCallbacks callbacks) {
        if(!mGpsCallbacks.contains(callbacks)) {
            mGpsCallbacks.add(callbacks);
        }
    }

    public void removeGpsCallbacks(GpsCallbacks callbacks) {
        if(mGpsCallbacks.contains(callbacks)) {
            mGpsCallbacks.remove(callbacks);
        }
    }

}
