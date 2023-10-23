package com.jz.util;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothSocket;
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
import android.os.Handler;
import android.os.Message;
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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.jz.util.check.LocationHelper;
import com.jz.util.utils.SystemUtils;
import com.jz.util.utils.WifiUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;

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
    private TextView vWifiName,vWifiConnectStatus;
    private String mWifiName,mWifiPwd,mBlueName;
    private WifiManager wifiManager;
    private final int BLUE_START_SCAN = 0;
    private final int BLUE_STOP_SCAN = 1;
    private final int BLUE_RETRY = 2;
    private String mAddress;
    private Map<String,String> blueList = new HashMap<>();
    private RecyclerView vRecycler;
    private Handler mHandler = new Handler(){
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            switch (msg.what){
                case BLUE_RETRY:
                    mBlueAdapter.enable();
                    if(mBlueAdapter.isEnabled()){
                        vBlueStatue.setText("Blue状态：已开启");
                        vBlueStatue.setTextColor(Color.parseColor("#000000"));
                        mHandler.sendEmptyMessageDelayed(BLUE_START_SCAN,1000);
                    }
                    break;
                case BLUE_START_SCAN:   //开始扫描
                    searchBlueDevices();
                    mHandler.sendEmptyMessageDelayed(BLUE_STOP_SCAN,5000);
                    break;
                case BLUE_STOP_SCAN:    //停止扫描
                    mBlueAdapter.cancelDiscovery();
                    if(blueList.size()==0){
                        Toast.makeText(SystemUtilsActivity.this,"重新扫描",Toast.LENGTH_LONG).show();
                        mBlueAdapter.startDiscovery();
                        mHandler.sendEmptyMessageDelayed(BLUE_STOP_SCAN,5000);
                    }else{
                        ArrayList<ItemBean> lists = new ArrayList<>();
                        for (String key : blueList.keySet()){
                            lists.add(new ItemBean(key,blueList.get(key)));
                        }
                        findViewById(R.id.tv_loading).setVisibility(View.GONE);
                        vRecycler.setAdapter(new BlueAdapter(lists,SystemUtilsActivity.this));
                    }
                    break;
            }
        }
    };

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
        vGpsStatue = findViewById(R.id.tv_gps);
        vSimStatue = findViewById(R.id.tv_sim_sing);
        vSimIcon = findViewById(R.id.sim_icon);
        vWifiName = findViewById(R.id.tv_wifi_name);
        mWifiName =  SystemUtils.getProp("persist.wifi.name", "");
        mWifiPwd =  SystemUtils.getProp("persist.wifi.pwd", "");
        mBlueName =  SystemUtils.getProp("persist.blue.name", "qssi");
        vRecycler = findViewById(R.id.blue_list);
        vRecycler.setLayoutManager(new LinearLayoutManager(this));

        findViewById(R.id.tv_gps).setOnClickListener(this);
        findViewById(R.id.tv_play).setOnClickListener(this);
        findViewById(R.id.tv_stop).setOnClickListener(this);
        findViewById(R.id.tv_close).setOnClickListener(this);

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
        if(wifiManager.isWifiEnabled()){
            vWifiStatue.setText("Wifi状态：已开启");
            vWifiStatue.setTextColor(Color.parseColor("#000000"));
            connectWifiPws(mWifiName,mWifiPwd);  //自动连接wifi
        }else{
            vWifiStatue.setText("Wifi状态：不可用,正在尝试开启");
            vWifiStatue.setTextColor(Color.parseColor("#FF0000"));
            setWifiStatue(true);
        }

        //blue
        setBlueStatue();

        //gps
        LocationHelper.getInstance(this).setListenerGpsCallbacks(this);
        mGpsStatue = LocationHelper.getInstance(this).requestLocation();
        vGpsStatue.setText("GPS状态："+(mGpsStatue ? "正常":"不正常"));

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

    private void connectBlue(String address){
        // 创建一个 UUID 对象
        UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
        // 创建一个 BluetoothDevice 对象
        BluetoothDevice device = mBlueAdapter.getRemoteDevice(address);
        Log.d("===zxd","开始连接，Address"+address+",device="+device.getName());
        // 创建一个 BluetoothSocket 对象
        BluetoothSocket socket = null;
        try {
            socket = device.createRfcommSocketToServiceRecord(uuid);
            socket.connect();
        } catch (Exception e) {
            e.printStackTrace();
            Log.d("===zxd","开始连接，Exception="+e.getMessage());
        }
    }

    private void searchBlueDevices(){
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        registerReceiver(mReceiver, filter);
        //开始扫描
        mBlueAdapter.startDiscovery();
        Log.d("===zxd","开始扫描，状态:"+mBlueAdapter.getState());
    }

    //开始收索 搜索接收函数：
    BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                blueList.put(device.getAddress(),device.getName());
            }else if(BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)){ //连接成功
                Log.d("===zxd","blue连接成功");
            }else if(BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)){

            }
        }
    };
    private PhoneStateListener mPhoneStateListener = new PhoneStateListener() {
        @Override
        public void onSignalStrengthsChanged(@NonNull SignalStrength signalStrength) {
            currentLevel = signalStrength.getLevel();
            if(currentLevel > 4){
                currentLevel = 4;
            }
            String simOperatorName = SystemUtils.getSimOperatorName(SystemUtilsActivity.this);
            if(SystemUtils.hasSimCard(SystemUtilsActivity.this)){
                vSimStatue.setText("已插卡");
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
            vSimStatue.setText("已插卡");
            vSimStatue.setTextColor(Color.parseColor("#000000"));
            SystemUtils.setMobileDataEnabled(true,this);
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
                    vWifiStatue.setText("Wifi状态：已开启");
                    vWifiStatue.setTextColor(Color.parseColor("#000000"));
                    connectWifiPws(mWifiName,mWifiPwd);  //自动连接wifi
                }else{
                    vWifiStatue.setText("Wifi状态：不可用.");
                    vWifiStatue.setTextColor(Color.parseColor("#FF0000"));
                }
            }
        },2000);
    }
    private void setBlueStatue(){
        if(mBlueAdapter.isEnabled()){
            vBlueStatue.setText("Blue状态：已开启");
            vBlueStatue.setTextColor(Color.parseColor("#000000"));
            mHandler.sendEmptyMessageDelayed(BLUE_START_SCAN,1000);
        }else{
            mBlueAdapter.enable();
            vBlueStatue.setText("Blue状态：不可用，正在尝试打开");
            vBlueStatue.setTextColor(Color.parseColor("#FF0000"));
            mHandler.sendEmptyMessageDelayed(BLUE_RETRY,3000);
        }
    }
    @Override
    public void onClick(View v) {
        switch (v.getId()){
            /*case R.id.tv_sim_on:    //打开移动数据
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
                break;*/
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
                System.exit(0);
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
                    if(TextUtils.isEmpty(mWifiName)){
                        mWifiName = wifiManager.getConnectionInfo().getSSID();
                    }
                    vWifiName.setText("已连接:"+mWifiName);
                    vWifiName.setTextColor(Color.parseColor("#000000"));
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
        unregisterReceiver(mReceiver);
    }
}