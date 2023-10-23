package com.jz.util.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.NetworkInfo.DetailedState;
import android.net.wifi.ScanResult;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.WifiLock;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;


import com.jz.util.AppApplication;
import com.jz.util.Reflex;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class WifiUtils {

    private String TAG = WifiUtils.class.getSimpleName();
    private static WifiUtils instance;
    private Context context;
    private WifiManager wifiManager;
    private List<WifiConfiguration> wifiConfigList;
    private WifiInfo wifiInfo;
    private WifiLock wifiLock;
    private WifiConfiguration wifiAPConfiguration;

    /**
     *
     */
    private WifiUtils() {
        context = AppApplication.getContext().getApplicationContext();
        wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        wifiInfo = wifiManager.getConnectionInfo();
    }

    /**
     * @return
     */
    public static WifiUtils getInstance() {
        if (instance == null) {
            instance = new WifiUtils();
        }
        return instance;
    }

    /**
     * get wifi stats.
     */
    public int getWifiState() {
        return wifiManager.getWifiState();
    }

    /**
     * @return
     */
    public boolean isWifiEnabled() {
        return wifiManager.isWifiEnabled();
    }

    public boolean isWifiConnected2() {
        boolean isOnline = false;
        int rssi = getConnectedInfo().getRssi();
        Log.i(TAG, "XLlog isWifiConnected2: " + rssi);
        isOnline = rssi > -100 && rssi < 0;
        return isOnline;
    }

    public void switchWifi(boolean _enable) {
        Log.d(TAG, "switchWifi enable:" + _enable);
        if (wifiManager.getWifiState() == WifiManager.WIFI_STATE_ENABLING
                || wifiManager.getWifiState() == WifiManager.WIFI_STATE_DISABLING) {
            return;
        } else if (wifiManager.getWifiState() == WifiManager.WIFI_STATE_ENABLED && !_enable) {
            if (wifiManager.setWifiEnabled(false)) {
            }
        } else if ((wifiManager.getWifiState() == WifiManager.WIFI_STATE_DISABLED && _enable)
                || wifiManager.getWifiState() == WifiManager.WIFI_STATE_UNKNOWN) {
            if (wifiManager.setWifiEnabled(true)) {
                Log.d(TAG, "setWifiEnabled success");
            }
        }
    }

    /**
     * open wifi.
     */
    public void openWifi() {
        if (wifiManager.getWifiState() == WifiManager.WIFI_STATE_ENABLING
                || wifiManager.getWifiState() == WifiManager.WIFI_STATE_DISABLING) {
            return;
        }

        if (wifiManager.getWifiState() == WifiManager.WIFI_STATE_DISABLED
                || wifiManager.getWifiState() == WifiManager.WIFI_STATE_UNKNOWN) {
            wifiManager.setWifiEnabled(true);
        }
    }

    /**
     * close wifi.
     */
    public void closeWifi() {
        if (wifiManager.getWifiState() == WifiManager.WIFI_STATE_ENABLING
                || wifiManager.getWifiState() == WifiManager.WIFI_STATE_DISABLING) {
            return;
        }

        if (wifiManager.getWifiState() == WifiManager.WIFI_STATE_ENABLED) {
            wifiManager.setWifiEnabled(false);
        }
    }

    /**
     *
     */
    public void switchWifi() {
        if (wifiManager.getWifiState() == WifiManager.WIFI_STATE_ENABLING
                || wifiManager.getWifiState() == WifiManager.WIFI_STATE_DISABLING) {
            return;
        }

        if (wifiManager.getWifiState() == WifiManager.WIFI_STATE_ENABLED) {
            wifiManager.setWifiEnabled(false);
        } else if (wifiManager.getWifiState() == WifiManager.WIFI_STATE_DISABLED
                || wifiManager.getWifiState() == WifiManager.WIFI_STATE_UNKNOWN) {
            wifiManager.setWifiEnabled(true);
        }
    }

    /**
     * scan wifi.
     */
    public boolean scanWifi() {
        return wifiManager.startScan();
    }

    /**
     * get scan result.
     */
    public List<ScanResult> getScanResults() {
        return wifiManager.getScanResults();
    }

    /**
     * scan results to string.
     */
    public List<String> scanResultToString(List<ScanResult> list) {
        List<String> strReturnList = new ArrayList<String>();
        for (int i = 0; i < list.size(); i++) {
            ScanResult strScan = list.get(i);
            String str = strScan.toString();
            boolean bool = strReturnList.add(str);
            if (!bool) {
            }
        }
        return strReturnList;
    }

    /**
     * get configuration.
     */
    @SuppressLint("MissingPermission")
    public List<WifiConfiguration> getConfiguration() {
        wifiConfigList = wifiManager.getConfiguredNetworks();
        return wifiConfigList;
    }

    /**
     * network id. according to BSSID,judge whether the specified WIFI has been
     * configured.
     *
     * @param _ssid
     * @return
     */
    @SuppressLint("MissingPermission")
    public int isConfiguration(String _ssid) {
        wifiConfigList = wifiManager.getConfiguredNetworks();
        for (int i = 0; i < wifiConfigList.size(); i++) {
            if (wifiConfigList.get(i).SSID.equals(_ssid)) {
                return wifiConfigList.get(i).networkId;
            }
        }
        return -1;
    }

    /**
     * Adding configuration information for specified WIFI, the SSID does not
     * exist in the original list
     *
     * @param wifiList
     * @param ssid
     * @param pwd
     * @param ispwd
     * @return
     */
    public int addWifiConfig(List<ScanResult> wifiList, String ssid, String pwd, boolean ispwd) {
        int wifiId = -1;
        for (int i = 0; i < wifiList.size(); i++) {
            ScanResult wifi = wifiList.get(i);
            if (wifi.SSID.equals(ssid)) {
                WifiConfiguration wifiCong = new WifiConfiguration();
                wifiCong.SSID = "\"" + wifi.SSID + "\"";
                if (ispwd) {
                    wifiCong.preSharedKey = "\"" + pwd + "\"";
                } else {
                    wifiCong.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
                }
                wifiCong.hiddenSSID = false;
                wifiCong.status = WifiConfiguration.Status.ENABLED;
                wifiId = wifiManager.addNetwork(wifiCong);
                if (wifiId != -1) {
                    return wifiId;
                }
            }
        }
        return wifiId;
    }

    /**
     * wifi设置
     *
     * @param ssid
     * @param pws
     * @param isHasPws
     */
    public static WifiConfiguration getWifiConfig(String ssid, String pws, boolean isHasPws) {
        WifiConfiguration config = new WifiConfiguration();
        config.SSID = "\"" + ssid + "\"";

        if (isHasPws) {
            config.hiddenSSID = true;
            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
            if(pws != null && pws.length() != 0) {
                if (pws.matches("[0-9A-Fa-f]{64}")) {
                    config.preSharedKey = pws;
                } else {
                    config.preSharedKey = '"' + pws + '"';
                }
            }
        } else {
            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
        }
        Object object = Reflex.createObject("android.net.IpConfiguration");
        Reflex.invokeInstanceMethod(config,"setIpConfiguration",new Class[]{object.getClass()},new Object[]{object});
        return config;
    }

    /**
     * 得到配置好的网络连接
     *
     * @param ssid
     * @return
     */
    private WifiConfiguration isExist(String ssid) {
        List<WifiConfiguration> configs = wifiManager.getConfiguredNetworks();
        for (WifiConfiguration config : configs) {
            if (config.SSID.equals("\"" + ssid + "\"")) {
                return config;
            }
        }
        return null;
    }

    /**
     * 有密码连接
     *
     * @param ssid
     * @param pws
     */
    public void connectWifiPws(String ssid, String pws) {
        if(pws.length() < 8){
            Toast.makeText(context, "密码不足8位", Toast.LENGTH_SHORT).show();
            return;
        }
        if (isWifiConnected()) {
            wifiManager.disconnect();
        }
        WifiConfiguration mWifiConfiguration = getWifiConfig(ssid, pws, true);
        wifiManager.addNetwork(mWifiConfiguration);

        Class mCallback = null;
        try {
            mCallback = Class.forName("android.net.wifi.WifiManager$ActionListener");

            /*Object proxy = Proxy.newProxyInstance(WifiManager.class.getClassLoader(),new Class[]{mCallback} , wifiStatueCallBack);
            Method connectMethod = WifiManager.class.getDeclaredMethod("connect", WifiConfiguration.class, mCallback);
            connectMethod.setAccessible(true);
            connectMethod.invoke(wifiManager, mWifiConfiguration, proxy);*/
        } catch (Exception e) {
            e.printStackTrace();
        }
        Reflex.invokeInstanceMethod(wifiManager,"connect",new Class[]{WifiConfiguration.class,mCallback},
                new Object[]{mWifiConfiguration,null});
    }

    private InvocationHandler wifiStatueCallBack = new InvocationHandler() {
        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if(method.getName().equals("onSuccess")){
                Log.d("===zxd","onSuccess");
            }else{
                Log.d("===zxd","onFailed");
            }
            return null;
        }
    };
    /**
     * 无密码连接
     *
     * @param ssid
     */
    public void connectWifiNoPws(String ssid) {
        wifiManager.disableNetwork(wifiManager.getConnectionInfo().getNetworkId());
        int netId = wifiManager.addNetwork(getWifiConfig(ssid, "", false));
        wifiManager.enableNetwork(netId, true);
    }

    /**
     * Connect specified WIFI.
     *
     * @param wifiId
     * @return
     */
    public boolean connectWifi(int wifiId) {
        Toast.makeText(context, "正在连接", Toast.LENGTH_LONG).show();
        Log.d(TAG, "connectWifi wifiId:" + wifiId);
        if (wifiId == wifiManager.getConnectionInfo().getNetworkId()) {
            return false;
        }
        for (int i = 0; i < wifiConfigList.size(); i++) {
            WifiConfiguration wifi = wifiConfigList.get(i);
            Log.d(TAG, "wifiConfiguration:" + wifi.toString());
            if (wifi.networkId == wifiId) {
                // activie the wifiID and connect.
                while (!(wifiManager.enableNetwork(wifiId, true))) {
                }
                wifiManager.saveConfiguration();
                return true;
            }
        }
        return false;
    }

    /**
     * Create wifi lock.
     *
     * @param lockName
     */
    public void createWifiLock(String lockName) {
        wifiLock = wifiManager.createWifiLock(lockName);
    }

    /**
     * Lock wifi.
     */
    public void acquireWifiLock() {
        wifiLock.acquire();
    }

    /**
     * Release wifi lock.
     */
    public void releaseWifiLock() {
        if (wifiLock.isHeld()) {
            wifiLock.release();
        }
    }

    /**
     * Get connection information.
     *
     * @return
     */
    public WifiInfo getConnectedInfo() {
        wifiInfo = wifiManager.getConnectionInfo();
        return wifiInfo;
    }

    public boolean isWifiConnected() {
        int ipAddress = getConnectedInfo() == null ? 0 : getConnectedInfo().getIpAddress();
        if (wifiManager.isWifiEnabled() && ipAddress != 0 && isNetworkAvailable(context)) {
            return true;
        }
        return false;
    }


    /**
     * 是否处于wifi连接的状态
     *
     */
    public boolean isWifiConnected1() {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo wifiNetworkInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        Log.i(TAG, "XLlog isWifiConnected: " + wifiNetworkInfo.isConnected() + "--"+isNetworkAvailable(context));
        if (wifiNetworkInfo.isConnected() && isNetworkAvailable(context)) {
            return true;
        }
        return false;
    }

    //检测网络是否可以使用
    public static boolean isNetworkAvailable(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        if(cm != null){
            NetworkInfo mNetworkInfo = cm.getActiveNetworkInfo();
            if(mNetworkInfo != null) {
                return mNetworkInfo.isAvailable();
            }
        }
        return false;
    }

    //判断当前是否已经连接
    public boolean isGivenWifiConnect(String SSID) {
        return isWifiConnected() && getCurentWifiSSID().equals(SSID);
    }

    //得到当前连接的WiFi  SSID
    public String getCurentWifiSSID() {
        String ssid = "";
        ssid = wifiManager.getConnectionInfo().getSSID();
        if (ssid.substring(0, 1).equals("\"")
                && ssid.substring(ssid.length() - 1).equals("\"")) {
            ssid = ssid.substring(1, ssid.length() - 1);
        }
        return ssid;
    }


    /**
     * Get wifi MAC addr.
     *
     * @return
     */
    public String getMacAddr() {
        return (wifiInfo == null || TextUtils.isEmpty(wifiInfo.getMacAddress())) ? "NULL" : wifiInfo.getMacAddress().toUpperCase().replaceAll(":", "-");
    }

    /**
     * Get connected SSID.
     *
     * @return
     */
    public String getConnectedSSID() {
        return (wifiInfo == null) ? "NULL" : wifiInfo.getSSID();
    }

    /**
     * Get IP addr.
     *
     * @return
     */
    public int getIPAddr() {
        return (wifiInfo == null) ? 0 : wifiInfo.getIpAddress();
    }

    /**
     * Get IP addr changed string.
     *
     * @return
     */
    public String getIPAddr2String() {
        int ip = (wifiInfo == null) ? 0 : wifiInfo.getIpAddress();
        return ipToString(ip);
    }

    /**
     * Get connected network ID.
     *
     * @return
     */
    public int getConnectedID() {
        if(!isWifiConnected()){
            return -1;
        }
        return (wifiInfo == null) ? -1 : wifiInfo.getNetworkId();
    }

    /**
     * Get state changed action, such as "Connecting" , "Get ip addr".
     *
     * @param intent
     * @return
     */
    public DetailedState getDetailedState(Intent intent) {
        DetailedState mdState = ((NetworkInfo) intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO)).getDetailedState();
        return mdState;
    }

    /**
     * Get Supplicant State.
     *
     * @param intent
     * @return
     */
    public SupplicantState getSupplicanState(Intent intent) {
        SupplicantState msState = intent.getParcelableExtra(WifiManager.EXTRA_NEW_STATE);
        return msState;
    }

    /**
     * Disconnect specified wifi.
     *
     * @param netId
     */
    public void disconnectWifi(int netId) {
        Log.d(TAG, "disconnectWifi=" + netId);
        wifiManager.disableNetwork(netId);
        wifiManager.disconnect();
//        wifiManager.removeNetwork(netId);
//        WifiApUtils.forgetNetwork(wifiManager, netId);
    }

    /**
     * Delete wifi
     *
     * @param netId
     */
    public void removeNetwork(int netId) {
        wifiManager.disableNetwork(netId);
        wifiManager.removeNetwork(netId);
        wifiManager.saveConfiguration();
    }

    /**
     * @return
     */
    private String ipToString(int _ip) {
        return (_ip & 0xFF) + "." + ((_ip >> 8) & 0xFF) + "." + ((_ip >> 16) & 0xFF) + "." + (_ip >> 24 & 0xFF);
    }

    /**
     * @param _enabled
     * @return
     */
    public boolean setWifiAPEnabled(boolean _enabled) {
        if (_enabled) {
            wifiManager.setWifiEnabled(false);
        }
//        boolean ret = wifiManager.setWifiApEnabled(wifiAPConfiguration, _enabled);
        wifiManager.setWifiEnabled(true);
        return false;
    }

    /**
     * @return
     */
    public boolean getWifiAPEnabled() {
//        return wifiManager.isWifiApEnabled();
        return false;
    }

    /**
     *
     */
    public void setWifiAPConfigration() {
//        wifiAPConfiguration.SSID = SystemProperties.get(ConstUtils.PERSYS_HOTSPOT_NAME, ConstUtils.hotspot_name_default);
//        wifiAPConfiguration.preSharedKey = SystemProperties.get(ConstUtils.PERSYS_HOTSPOT_PWD, ConstUtils.hotspot_pwd_default);
//        wifiManager.setWifiApConfiguration(wifiAPConfiguration);
    }


}
