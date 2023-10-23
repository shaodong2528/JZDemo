package com.jz.util;

import static android.content.Context.WIFI_SERVICE;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.util.Log;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;

/**
 * Copyright (C)
 * FileName: WifiApManage
 * Author: JZ
 * Date: 2021/9/15 21:40
 * Description: wifi ap manager
 */
public class WifiApManage {
    private static final String AP_CONNECTED = "0x2";
    private static final String AP_DEVICE_TYPE = "wlan0";
    private static final int WIFI_NONE_TYPE = 0;
    private static final int WIFI_WEP_TYPE = 1;
    private static final int WIFI_WPA_TYPE = 2;
    private static final int WIFI_PSK_TYPE = 3;


    private static WifiApManage mWifiApManage;
    private WifiManager mWifiManager;
    private WifiConfiguration mWifiConfiguration;

    public static WifiApManage newInstance(Context context) {
        if (null == mWifiApManage) {
            mWifiApManage = new WifiApManage(context);
        }
        return mWifiApManage;
    }

    public WifiApManage(Context context) {
        mWifiManager = (WifiManager) context.getSystemService(WIFI_SERVICE);
    }

    /**
     * 创建热点
     *
     * @param ssid   热点名称
     * @param passwd 热点密码
     * @param type   热点类型
     */
    public void startWifiAp(String ssid, String passwd, int type) {
        Method method;

        if (mWifiManager.isWifiEnabled()) {
            mWifiManager.setWifiEnabled(false);
        }

        try {
            WifiConfiguration netConfig = new WifiConfiguration();

            netConfig.SSID = ssid;
            netConfig.preSharedKey = passwd;
            netConfig.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);

            switch (type) {
                case WIFI_NONE_TYPE:
                    netConfig.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
                    netConfig.preSharedKey = null;
                    break;
                case WIFI_WEP_TYPE:
                    netConfig.allowedKeyManagement.set(4);
                    break;
                case WIFI_WPA_TYPE:
                    netConfig.allowedKeyManagement.set(4);
                    break;
                case WIFI_PSK_TYPE:
                    netConfig.allowedKeyManagement.set(4);
                    break;
                default:
                    break;
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Method configMethod = mWifiManager.getClass().getMethod("setWifiApConfiguration", WifiConfiguration.class);
                configMethod.invoke(mWifiManager, netConfig);

                method = mWifiManager.getClass().getMethod("startSoftAp", WifiConfiguration.class);
                method.invoke(mWifiManager, mWifiConfiguration);
            } else {
                method = mWifiManager.getClass().getMethod("setWifiApEnabled", WifiConfiguration.class, boolean.class);
                method.invoke(mWifiManager, netConfig, true);
            }
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
            Log.d("zheng", e.getMessage());
        } catch (InvocationTargetException e) {
            e.printStackTrace();
            Log.d("zheng", e.getMessage());
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            Log.d("zheng", e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            Log.d("zheng", e.getMessage());
        }
    }

    public void writeWifiCfg(String ssid, String passwd, int type) {
        Method method;
        try {
            WifiConfiguration netConfig = new WifiConfiguration();

            netConfig.SSID = ssid;
            netConfig.preSharedKey = passwd;
            netConfig.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);

            switch (type) {
                case WIFI_NONE_TYPE:
                    netConfig.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
                    netConfig.preSharedKey = null;
                    break;
                case WIFI_WEP_TYPE:
                    netConfig.allowedKeyManagement.set(4);
                    break;
                case WIFI_WPA_TYPE:
                    netConfig.allowedKeyManagement.set(4);
                    break;
                case WIFI_PSK_TYPE:
                    netConfig.allowedKeyManagement.set(4);
                    break;
                default:
                    break;
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Method configMethod = mWifiManager.getClass().getMethod("setWifiApConfiguration", WifiConfiguration.class);
                configMethod.invoke(mWifiManager, netConfig);
            } else {
                method = mWifiManager.getClass().getMethod("setWifiApEnabled", WifiConfiguration.class, boolean.class);
                method.invoke(mWifiManager, netConfig, true);
            }
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 检查是否开启Wifi热点
     *
     * @return
     */
    public boolean isWifiApEnabled() {
        try {
            Method method = mWifiManager.getClass().getMethod("isWifiApEnabled");
            method.setAccessible(true);
            return (boolean) method.invoke(mWifiManager);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static boolean isWifiApOpen(Context context) {
        try {
            WifiManager manager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
            Method method = manager.getClass().getDeclaredMethod("getWifiApState");
            int state = (int) method.invoke(manager);
            Field field = manager.getClass().getDeclaredField("WIFI_AP_STATE_ENABLED");
            int value = (int) field.get(manager);
            if (state == value) {
                return true;
            } else {
                return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * 关闭热点
     */
    @SuppressLint({"ObsoleteSdkInt", "SoonBlockedPrivateApi"})
    public void closeWifiAp() {
        Method method;
        if (isWifiApEnabled()) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    method = mWifiManager.getClass().getDeclaredMethod("stopSoftAp");
                    method.invoke(mWifiManager);
                } else {
                    method = mWifiManager.getClass().getMethod("getWifiApConfiguration");
                    method.setAccessible(true);
                    WifiConfiguration config = (WifiConfiguration) method.invoke(mWifiManager);
                    Method method2 = mWifiManager.getClass().getMethod("setWifiApEnabled", WifiConfiguration.class, boolean.class);
                    method2.invoke(mWifiManager, config, false);
                }
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 开启热点
     */
    public void openWifiAp() {
        Method method;
        if (mWifiManager.isWifiEnabled()) {
            mWifiManager.setWifiEnabled(false);
        }

        if (!isWifiApEnabled()) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    mWifiConfiguration = getWifiApInfo();
                    Method configMethod = mWifiManager.getClass().getMethod("setWifiApConfiguration", WifiConfiguration.class);
                    configMethod.invoke(mWifiManager, mWifiConfiguration);
                    method = mWifiManager.getClass().getMethod("startSoftAp", WifiConfiguration.class);
                    method.invoke(mWifiManager, mWifiConfiguration);
                } else {
                    method = mWifiManager.getClass().getMethod("getWifiApConfiguration");
                    method.setAccessible(true);
                    WifiConfiguration config = (WifiConfiguration) method.invoke(mWifiManager);
                    Method method2 = mWifiManager.getClass().getMethod("setWifiApEnabled", WifiConfiguration.class, boolean.class);
                    method2.invoke(mWifiManager, config, true);
                }
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
    }

    public WifiConfiguration getWifiApInfo() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Method method = mWifiManager.getClass().getMethod("getWifiApConfiguration");
                method.setAccessible(true);
                mWifiConfiguration = (WifiConfiguration) method.invoke(mWifiManager);
            } else {
                Method method = mWifiManager.getClass().getMethod("getWifiApConfiguration");
                method.setAccessible(true);
                mWifiConfiguration = (WifiConfiguration) method.invoke(mWifiManager);
            }
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return mWifiConfiguration;
    }


    /**
     * 开热点手机获得其他连接手机IP的方法
     *
     * @return 其他手机IP 数组列表
     */
    public ArrayList<String> getConnectedIP() {
        ArrayList<String> connectedIp = new ArrayList<String>();
        BufferedReader br = null;
        FileReader fileReader = null;
        boolean flags = true;
        try {
            String line;
            fileReader = new FileReader("/proc/net/arp");
            br = new BufferedReader(fileReader);

            mWifiManager.getDhcpInfo().toString();

            while ((line = br.readLine()) != null) {
                if (!flags) {
                    final String[] splitted = line.split(" + ");
                    if (splitted != null && splitted.length >= 6) {
                        if (splitted[2].equals(AP_CONNECTED) && splitted[5].equals(AP_DEVICE_TYPE)) {
                            connectedIp.add(splitted[0] + " " + splitted[3]);
                        }
                    }
                }
                flags = false;
            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (null != fileReader) {
                    fileReader.close();
                }

                if (null != br) {
                    br.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return connectedIp;
    }

    public String getClientDeviceName() {
        BufferedReader br = null;
        FileReader fileReader = null;

        try {
            fileReader = new FileReader("/data/misc/dhcp/dnsmasq.leases");
            br = new BufferedReader(fileReader);
            String line = "";
            while ((line = br.readLine()) != null) {
                Log.d("WifiApManage", "enter device name : " + line);
                if (line.indexOf("") != 1) {
                    String[] fields = line.split(" ");
                    //校验数据是不是破损
                    if (fields.length > 4) {
                        //返回第4个栏位
                        return fields[3];
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (null != fileReader) {
                    fileReader.close();
                }

                if (null != br) {
                    br.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return null;
    }

    private String int2ip(int ipInt) {
        StringBuilder sb = new StringBuilder();
        sb.append((ipInt >> 24) & 0xFF);
        return sb.toString();
    }

    /**
     * Get connection information.
     *
     * @return
     */
    public WifiInfo getConnectedInfo() {
        WifiInfo wifiInfo = mWifiManager.getConnectionInfo();
        return wifiInfo;
    }

    public boolean isWifiConnected() {
        int ipAddress = getConnectedInfo() == null ? 0 : getConnectedInfo().getIpAddress();
        if (mWifiManager.isWifiEnabled() && ipAddress != 0) {
            return true;
        }
        return false;
    }

    public String getLocalIp() {
        String ip = "000";
        if(isWifiConnected()) {
            int ipAddress = getConnectedInfo().getIpAddress();
            ip = int2ip(ipAddress);
        }
        return ip;
    }

    public static String getWifiApName(Context context){
        String ssid = "";
        try {
            WifiManager manager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
            Method method = manager.getClass().getDeclaredMethod("getWifiApConfiguration");
            WifiConfiguration configuration = (WifiConfiguration) method.invoke(manager);
            ssid = configuration.SSID;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ssid;
    }

    public static String getValidPassword(Context context){
        try {
            WifiManager mWifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
            Method method = mWifiManager.getClass().getMethod("getWifiApConfiguration");
            WifiConfiguration configuration = (WifiConfiguration)method.invoke(mWifiManager);
            return configuration.preSharedKey;
        } catch (Exception e) {
            return null;
        }
    }

}
