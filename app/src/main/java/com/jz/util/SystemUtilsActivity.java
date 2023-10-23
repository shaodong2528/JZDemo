package com.jz.util;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.media.MediaPlayer;
import android.net.ConnectivityManager;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.telephony.PhoneStateListener;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.jz.util.check.LocationHelper;
import com.jz.util.utils.SystemUtils;
import com.jz.util.utils.WifiUtils;

import java.util.Iterator;

public class SystemUtilsActivity extends AppCompatActivity implements View.OnClickListener, LocationHelper.GpsCallbacks {

    private TextView vMode;
    private TextView vWifiStatue, vBlueStatue,vGpsNum,vGpsStatue,vSimStatue;
    private WifiManager mwifiManager;
    private BluetoothAdapter mBlueAdapter;
    private boolean mGpsStatue;
    private TelephonyManager mTelephonyManager;
    private int currentLevel;
    private ImageView vSimIcon;
    private MediaPlayer mMediaPlayer;
    private TextView vWifiName,vWifiPwd,vWifiConnet,vWifiConnectStatus;
    private String mWifiName,mWifiPwd;
    private WifiManager wifiManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        setContentView(R.layout.activity_system_utils);

        vMode = findViewById(R.id.tv_mode);
        vMode.setText("常用检测测试工具");
        vWifiStatue = findViewById(R.id.tv_wifi_statue);
        vBlueStatue = findViewById(R.id.tv_blue_statue);
        vGpsNum = findViewById(R.id.tv_gps_info);
        vGpsStatue = findViewById(R.id.tv_gps_statue);
        vSimStatue = findViewById(R.id.tv_sim_sing);
        vSimIcon = findViewById(R.id.sim_icon);
        vWifiName = findViewById(R.id.tv_wifi_name);
        vWifiPwd = findViewById(R.id.tv_wifi_pwd);
        vWifiConnet = findViewById(R.id.tv_connect_wifi);
        vWifiConnectStatus = findViewById(R.id.tv_wifi_isconnect);
        mWifiName =  SystemUtils.getProp("persist.wifi.name", "");
        mWifiPwd =  SystemUtils.getProp("persist.wifi.pwd", "");
        if(!TextUtils.isEmpty(mWifiName)){
            vWifiName.setText("Wifi名称:"+mWifiName);
            vWifiPwd.setText("Wifi密码:"+mWifiPwd);
        }else{
            vWifiName.setVisibility(View.INVISIBLE);
            vWifiPwd.setVisibility(View.INVISIBLE);
            vWifiConnet.setVisibility(View.INVISIBLE);
            vWifiConnectStatus.setVisibility(View.INVISIBLE);
        }
        findViewById(R.id.tv_wifi_on).setOnClickListener(this);
        findViewById(R.id.tv_wifi_off).setOnClickListener(this);
        findViewById(R.id.tv_sim_on).setOnClickListener(this);
        findViewById(R.id.tv_sim_off).setOnClickListener(this);
        findViewById(R.id.tv_gps).setOnClickListener(this);
        findViewById(R.id.tv_blue_on).setOnClickListener(this);
        findViewById(R.id.tv_blue_off).setOnClickListener(this);
        findViewById(R.id.tv_play).setOnClickListener(this);
        findViewById(R.id.tv_stop).setOnClickListener(this);
        findViewById(R.id.tv_close).setOnClickListener(this);
        vWifiConnet.setOnClickListener(this);

        mwifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        mBlueAdapter = BluetoothAdapter.getDefaultAdapter();
        initData();
    }

    private void initData() {
        //注册网络监听
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        intentFilter.addAction(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION);
        registerReceiver(netReceiver, intentFilter);

        //wifi
        wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        String wifiEnable = SystemUtils.getProp("persist.wifi.enable", "true");
        boolean enable = Boolean.valueOf(wifiEnable);
        setWifiStatue(enable);

        //blue
        String blueEnable = SystemUtils.getProp("persist.blue.enable", "true");
        setBlueStatue(Boolean.valueOf(blueEnable));

        //gps
        LocationHelper.getInstance(this).setListenerGpsCallbacks(this);
        mGpsStatue = LocationHelper.getInstance(this).requestLocation();
        vGpsStatue.setText("状态："+(mGpsStatue ? "正常":"不正常"));

        //SIM卡
        loadSimCard();
        mTelephonyManager = (TelephonyManager)getSystemService(Context.TELEPHONY_SERVICE);
        mTelephonyManager.listen(mPhoneStateListener,
                PhoneStateListener.LISTEN_SERVICE_STATE
                        | PhoneStateListener.LISTEN_SIGNAL_STRENGTHS
                        | PhoneStateListener.LISTEN_CALL_STATE
                        | PhoneStateListener.LISTEN_DATA_CONNECTION_STATE
                        | PhoneStateListener.LISTEN_DATA_ACTIVITY
                /*| PhoneStateListener.LISTEN_CARRIER_NETWORK_CHANGE*/);

        //音乐
        mMediaPlayer = MediaPlayer.create(this, R.raw.test);
        boolean isstart = Boolean.valueOf(SystemUtils.getProp("persist.music.start", "true"));
        if(isstart){
            mMediaPlayer.start();
        }

    }

    private PhoneStateListener mPhoneStateListener = new PhoneStateListener() {
        @Override
        public void onSignalStrengthsChanged(@NonNull SignalStrength signalStrength) {
            currentLevel = signalStrength.getLevel();
            if(currentLevel > 4){
                currentLevel = 4;
            }
            String simOperatorName = SystemUtils.getSimOperatorName(SystemUtilsActivity.this);
            if(SystemUtils.hasSimCard(SystemUtilsActivity.this)){
                if(TextUtils.isEmpty(simOperatorName)){
                    vSimStatue.setText("未知运营商");
                }else{
                    vSimStatue.setText(simOperatorName);
                }
                vSimStatue.setTextColor(Color.parseColor("#000000"));
                vSimIcon.setVisibility(View.VISIBLE);
            }else{
                vSimStatue.setText("未检测到SIM卡");
                vSimStatue.setTextColor(Color.parseColor("#ff0000"));
                vSimIcon.setVisibility(View.GONE);
            }
            if(SystemUtils.isDataEnabled(SystemUtilsActivity.this)){
                vSimIcon.setImageDrawable(getDrawable(R.drawable.sim_lte));
                vSimIcon.getDrawable().setLevel(currentLevel);
            }else{
                vSimIcon.setImageDrawable(getDrawable(R.drawable.sim_signal));
                vSimIcon.getDrawable().setLevel(currentLevel);
            }
        }
    };

    private void loadSimCard(){
        if(SystemUtils.hasSimCard(this)){
            String simOperatorName = SystemUtils.getSimOperatorName(this);
            if(TextUtils.isEmpty(simOperatorName)){
                vSimStatue.setText("未知运营商");
            }else{
                vSimStatue.setText(simOperatorName);
            }
            vSimStatue.setTextColor(Color.parseColor("#000000"));
        }else{
            vSimStatue.setText("未检测到SIM卡");
            vSimStatue.setTextColor(Color.parseColor("#FF0000"));
        }
    }

    private void setWifiStatue(boolean enable){
        mwifiManager.setWifiEnabled(enable);
        vWifiStatue.postDelayed(new Runnable() {
            @Override
            public void run() {
                if(mwifiManager.isWifiEnabled()){
                    vWifiStatue.setText("状态:已开启");
                    vWifiStatue.setTextColor(Color.parseColor("#000000"));
                }else{
                    vWifiStatue.setText("状态:已关闭");
                    vWifiStatue.setTextColor(Color.parseColor("#FF0000"));
                }
                SystemUtils.setProp("persist.wifi.enable", enable+"");
            }
        },1000);
    }
    private void setBlueStatue(boolean enable){
        if(enable){
            mBlueAdapter.enable();
        }else{
            mBlueAdapter.disable();
        }
        vWifiStatue.postDelayed(new Runnable() {
            @Override
            public void run() {
                if(mBlueAdapter.isEnabled()){
                    vBlueStatue.setText("状态:已开启");
                    vBlueStatue.setTextColor(Color.parseColor("#000000"));
                }else{
                    vBlueStatue.setText("状态:已关闭");
                    vBlueStatue.setTextColor(Color.parseColor("#FF0000"));
                }
                SystemUtils.setProp("persist.blue.enable", enable+"");
            }
        },1000);
    }
    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.tv_wifi_on:  //打开wifi
                setWifiStatue(true);
                break;
            case R.id.tv_wifi_off:  //关闭wifi
                setWifiStatue(false);
                break;
            case R.id.tv_blue_on:    //打开蓝牙
                setBlueStatue(true);
                break;
            case R.id.tv_blue_off:   //关闭蓝牙
                setBlueStatue(false);
                break;
            case R.id.tv_sim_on:    //打开移动数据
                if(SystemUtils.hasSimCard(this)){
                    //开启移动数据
                    SystemUtils.setMobileDataEnabled(true, this);
                    vSimIcon.setImageDrawable(getDrawable(R.drawable.sim_lte));
                    vSimIcon.getDrawable().setLevel(currentLevel);
                }
                break;
            case R.id.tv_sim_off:   //关闭移动数据
                if(SystemUtils.hasSimCard(this)){
                    //关闭移动数据
                    SystemUtils.setMobileDataEnabled(false, this);
                    vSimIcon.setImageDrawable(getDrawable(R.drawable.sim_signal));
                    vSimIcon.getDrawable().setLevel(currentLevel);
                }
                break;
            case R.id.tv_gps:   //打开GPS
            case R.id.tv_gps_info:   //打开GPS
                SystemUtils.RunApp(this,"com.chartcross.gpstest");
                break;
            case R.id.tv_stop:   //暂停音乐
                mMediaPlayer.pause();
                break;
            case R.id.tv_play:   //播放音乐
                mMediaPlayer.start();
                break;
            case R.id.tv_close:
                finish();
                //System.exit(0);
                break;
            case R.id.tv_connect_wifi:  //连接wifi
                connectWifiPws(mWifiName,mWifiPwd);
                break;
        }
    }


    @Override
    public void onGpsStatusChanged(int event) {
        GpsStatus status = LocationHelper.getInstance(this).getLocationManager().getGpsStatus(null); // 取当前状态
        if (event == GpsStatus.GPS_EVENT_SATELLITE_STATUS) {
            int maxSatellites = status.getMaxSatellites();
            Iterator<GpsSatellite> it = status.getSatellites().iterator();
            int mSatellites = 0;
            Log.i("===utiLs", "XLlog onGpsStatusChanged: " + it.hasNext() + " --" + maxSatellites);
            while (it.hasNext() && mSatellites <= maxSatellites) {
                GpsSatellite satellite = it.next();
                float snr = satellite.getSnr();
                int prn = satellite.getPrn();
                Log.i("===SystemUtilsActivity", "XLlog onGpsStatusChanged: " + snr + " --" + prn);
                if (snr > 0 /*&& snr <= 50 && prn > 0 && prn <= 32*/) {
                    if (satellite.usedInFix()) {
                        mSatellites++;
                    }
                    Log.d("===","snr信号="+snr+",prn="+prn);
                }
            }
            vGpsNum.setText("GPS数量:"+mSatellites);
        }
    }

    public boolean connectWifi(int wifiId) {
        if (wifiId == wifiManager.getConnectionInfo().getNetworkId()) {
            return false;
        }
        /*for (int i = 0; i < wifiConfigList.size(); i++) {
            WifiConfiguration wifi = wifiConfigList.get(i);
            Log.d("===zxd", "wifiConfiguration:" + wifi.toString());
            if (wifi.networkId == wifiId) {
                // activie the wifiID and connect.
                while (!(wifiManager.enableNetwork(wifiId, true))) {}
                wifiManager.saveConfiguration();
                return true;
            }
        }*/
        return false;
    }

    public void connectWifiPws(String ssid, String pws) {
        if(pws.length() < 8){
            Toast.makeText(this, "密码不足8位", Toast.LENGTH_SHORT).show();
            return;
        }
        if (wifiManager.isWifiEnabled()  && WifiUtils.isNetworkAvailable(this)) {
            wifiManager.disconnect();
        }
        WifiConfiguration mWifiConfiguration = WifiUtils.getWifiConfig(ssid, pws, true);
        wifiManager.addNetwork(mWifiConfiguration);
        Class mCallback = null;
        try {
            mCallback = Class.forName("android.net.wifi.WifiManager$ActionListener");
        } catch (Exception e) {
            e.printStackTrace();
        }
        Reflex.invokeInstanceMethod(wifiManager,"connect",new Class[]{WifiConfiguration.class,mCallback}, new Object[]{mWifiConfiguration,null});
    }

    BroadcastReceiver netReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            try {
                String action = intent.getAction();
                if(action.equals(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION)){
                    int error = intent.getIntExtra(WifiManager.EXTRA_SUPPLICANT_ERROR, -1);
                    if (error == WifiManager.ERROR_AUTHENTICATING) {
                        Toast.makeText(context, "连接失败...", Toast.LENGTH_LONG).show();
                        vWifiConnectStatus.setText("连接失败");
                        vWifiConnectStatus.setTextColor(Color.parseColor("#FF0000"));
                    }
                }else if(action.equals(ConnectivityManager.CONNECTIVITY_ACTION) && WifiUtils.isNetworkAvailable(context)){
                    Toast.makeText(SystemUtilsActivity.this,"连接成功",Toast.LENGTH_LONG).show();
                    vWifiConnectStatus.setText("已连接");
                    vWifiConnectStatus.setTextColor(Color.parseColor("#000000"));
                }else{
                    vWifiConnectStatus.setText("未连接");
                    vWifiConnectStatus.setTextColor(Color.parseColor("#FF0000"));
                }
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    };
    
    @Override
    protected void onPause() {
        super.onPause();
        //System.exit(0);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        LocationHelper.getInstance(this).removeLocation();
    }
}