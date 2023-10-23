package com.jz.util.utils;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.Instrumentation;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.provider.Settings;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.WindowManager;

import androidx.annotation.RequiresApi;
import androidx.annotation.VisibleForTesting;


import com.jz.util.AppApplication;
import com.jz.util.Reflex;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

/**
 * @ClassName SystemUtils
 * @Author xuexb
 * @Date 2022/5/6 10:37
 */
public class SystemUtils {

    private static final String TAG = "SystemUtils";

    public interface ExecShellListener {
        void onComplete(boolean isok);
    }

    public static void execShellCmd3(Context context, String str_cmd1, ExecShellListener callback) {
        boolean isok = false;
        String[] str_cmd2 = {"/bin/sh","-c",str_cmd1};
        Process process = null;
        BufferedReader successResult = null;
        BufferedReader errorResult = null;
        StringBuilder successMsg = new StringBuilder();
        StringBuilder errorMsg = new StringBuilder();
        try {
            process = Runtime.getRuntime().exec(str_cmd2);
            successResult = new BufferedReader(new InputStreamReader(process.getInputStream()));
            errorResult = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            String s;
            String s1;
            while ((s = successResult.readLine()) != null) {
                successMsg.append(s);
            }
            while ((s1 = errorResult.readLine()) != null) {
                errorMsg.append(s1);
            }
            if(TextUtils.isEmpty(successMsg.toString())){
                isok = false;
            }else{
                isok = true;
            }
        } catch (Exception e) {
            isok = false;
        } finally {
            closeAll(successResult,errorResult);
            if (process != null) {
                process.destroy();
            }
        }
        sync();
        if(callback != null)
            callback.onComplete(isok);
    }

    public static boolean isInstalled(Context mContext,String packageName) {
        try {
            mContext.getPackageManager().getApplicationInfo(packageName, 0);
            return true;
        } catch (PackageManager.NameNotFoundException var3) {
            try {
                mContext.getPackageManager().getApplicationInfo(packageName, 0);
                return true;
            } catch (PackageManager.NameNotFoundException var2) {
                return false;
            }
        }
    }

    //移动开关是否打开
    @SuppressLint({"NewApi", "MissingPermission"})
    public static boolean isDataEnabled(Context mContext) {
        if(mContext == null){
            mContext = AppApplication.getContext();
        }
        TelephonyManager  mTelephonyManager = (TelephonyManager)mContext.getSystemService(Context.TELEPHONY_SERVICE);
        if(mTelephonyManager != null){
            return mTelephonyManager.isDataEnabled();
        }else{
            return false;
        }
        //return getTelephonyManager(mContext).isDataEnabled();
    }

    public static int getScreenWidth() {
        Context context = AppApplication.getContext();
        DisplayMetrics dm = new DisplayMetrics();
        WindowManager manager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        manager.getDefaultDisplay().getMetrics(dm);
        //后续所有用于分辨率的屏幕宽高，不用上面原来的方式，都改用下面的都区域显示的属性
        int width = dm.widthPixels;
        String display_width = getProp("pandora.viewarea_width", "0");
        try {
            width = Integer.parseInt(display_width);
        }catch (Exception e){
        }
        return width;
    }

    public static int getScreenHeight() {
        Context context = AppApplication.getContext();
        DisplayMetrics dm = new DisplayMetrics();
        WindowManager manager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        manager.getDefaultDisplay().getMetrics(dm);
        //后续所有用于分辨率的屏幕宽高，不用上面原来的方式，都改用下面的读区域显示的属性
        int height = dm.heightPixels;
        String display_height = getProp("pandora.viewarea_height", "0");
        try {
            height = Integer.parseInt(display_height);
        }catch (Exception e){
        }
        return height;
    }

    public static int getResourcesId(String name, String type) {
        float ratio = getScreenWidth()/1.0f/getScreenHeight()/1.0f;
        String layout_prefix = "";
        if(ratio > 1.94 && ratio < 2.3){
            layout_prefix = "land_1024_480_";
        }else if(ratio > 2.3){
            layout_prefix = "land_1280_480_";
        }else{
            layout_prefix = "land_";
        }
        return getResourcesId(layout_prefix, name, type);
    }

    public static void sendKeyCode(final int keyCode) {
        new Thread() {
            public void run() {
                try {
                    Instrumentation inst = new Instrumentation();
                    inst.sendKeyDownUpSync(keyCode);
                } catch (Exception e) {
                }
            }
        }.start();
    }

    public static int getResourcesId(String resources_id_prefix, String name, String type) {
        Resources resources = AppApplication.getContext().getResources();
        String packageName = AppApplication.getContext().getPackageName();
        int layout = resources.getIdentifier(resources_id_prefix + name, type, packageName);

        if (layout == 0 && !TextUtils.isEmpty(resources_id_prefix)) {
            layout = resources.getIdentifier(name, type, packageName);
        }

        if (layout == 0 && !TextUtils.isEmpty(resources_id_prefix)) {
            layout = resources.getIdentifier(name, type+"-nodpi", packageName);
        }

        if (layout == 0 && !TextUtils.isEmpty(resources_id_prefix)) {
            layout = resources.getIdentifier(name, type+"-anydpi", packageName);
        }

        if (layout == 0 && !TextUtils.isEmpty(resources_id_prefix)) {
            layout = resources.getIdentifier("land_" + name, type, packageName);
        }

        return layout;
    }

    public static void RunApp(Context context, String packageNam,String className) {
        Intent intent = null;
        try {
            intent = new Intent(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_LAUNCHER);
            ComponentName cn = new ComponentName(packageNam,className);
            intent.setComponent(cn);
            context.startActivity(intent);
        } catch (Exception e) {
            Log.i(TAG, "RunApp with packageName error: " + e.toString());
        }
    }

    public static void RunApp(Context context, String packageName) {
        try {
            if (packageName != null) {
                Log.i(TAG, "RunApp with packageName: " + packageName);

                PackageManager pm = context.getPackageManager();
                Intent intent = pm.getLaunchIntentForPackage(packageName);
                if (intent != null) {
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(intent);
                }
            }
        } catch (Exception e) {
            Log.i(TAG, "RunApp with packageName error: " + e.toString());
        }
    }

    //获取系统属性
    public static String getProp(String key) {
        String value = (String) Reflex.invokeStaticMethod("android.os.SystemProperties", "get",
                new Class[]{String.class, String.class}, new String[]{key, ""});
        return value;
    }

    //获取系统属性
    public static String getProp(String key, String def) {
        String value = (String) Reflex.invokeStaticMethod("android.os.SystemProperties", "get",
                new Class[]{String.class, String.class}, new String[]{key, def});
        return value;
    }

    //获取系统属性
    public static int getIntProp(String key) {
        String value = (String) Reflex.invokeStaticMethod("android.os.SystemProperties", "get",
                new Class[]{String.class, String.class}, new String[]{key, "0"});
        return Integer.parseInt(value);
    }

    //设置系统属性
    public static void setProp(String key ,String value) {
        Reflex.invokeStaticMethod("android.os.SystemProperties","set",
                new Class[]{String.class,String.class},new String[]{key,value});
    }
    
    @RequiresApi(api = Build.VERSION_CODES.M)
    public static void setZoneTime(String key){
        Class obj_class = null;
        try {
            obj_class = Class.forName("android.app.AlarmManager");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        Object timeZoneDetector = AppApplication.getInstance().getApplicationContext().getSystemService(obj_class);
        Reflex.invokeInstanceMethod(timeZoneDetector,"setTimeZone",String.class,key);
    }
    /**
     * 根据经度获取时区；例如121：+8;-121：-8;返回值为字符串（返回正数时候带+符号）
     *
     * @return
     */


    public static void restartApp(Context context) {
        Intent intent = context.getPackageManager().getLaunchIntentForPackage(context.getPackageName());
        PendingIntent restartIntent = PendingIntent.getActivity(context, 0, intent, 0);
        AlarmManager mgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        mgr.set(AlarmManager.RTC, System.currentTimeMillis(), restartIntent);
        System.exit(0);
    }

    public static int getMaxVolume(AudioManager mAudio, int stream) {
        return mAudio.getStreamMaxVolume(stream);
    }

    public static int getVolume(AudioManager mAudio,int stream) {
        return mAudio.getStreamVolume(stream);//getLastAudibleStreamVolume
    }

    public static void setAudioVolume(AudioManager mAudio, int stream, int level, int flag) {
        mAudio.setStreamVolume(stream, level, flag);
        setProp("persist.audiovolume",level+"");
    }

    public static int getMinVolume(AudioManager mAudio, int stream) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            return mAudio.getStreamMinVolume(stream);//getStreamMinVolumeInt
        }
        return 0;
    }

    //热点开关
    public static void setHotspotEnabled(boolean enabled,ConnectivityManager mConnectivityManager) {
        if (enabled) {
            Reflex.invokeInstanceMethod(mConnectivityManager,"startTethering",
                    new Class[]{int.class,boolean.class,Object.class},
                    new Object[]{0,false,null});
        } else {
            Reflex.invokeInstanceMethod(mConnectivityManager,"startTethering",
                    new Class[]{int.class},
                    new Object[]{0});
        }
    }

    //移动开关
    @RequiresApi(api = Build.VERSION_CODES.O)
    public static void setMobileDataEnabled(boolean enabled,Context mContext) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            getTelephonyManager(mContext).setDataEnabled(enabled);
        }
    }

    //判断热点是否打开
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

    //获取热点名称
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

    //获取热点密码
    public static String getValidPassword(Context context){
        try {
            WifiManager mWifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
            Method method = mWifiManager.getClass().getMethod("getWifiApConfiguration");
            WifiConfiguration configuration = (WifiConfiguration)method.invoke(mWifiManager);
            return configuration.preSharedKey;
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
            return null;
        }

    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    @VisibleForTesting
    private static TelephonyManager getTelephonyManager(Context mContext) {
        int subscriptionId = SubscriptionManager.INVALID_SUBSCRIPTION_ID;
        if (!SubscriptionManager.isValidSubscriptionId(subscriptionId)) {
            subscriptionId = SubscriptionManager.getDefaultDataSubscriptionId();
        }
        if (!SubscriptionManager.isValidSubscriptionId(subscriptionId)) {
            SubscriptionManager subscriptionManager = SubscriptionManager.from(mContext);
            int[] activeSubIds = (int[]) Reflex.invokeInstanceMethod(subscriptionManager,
                    "getActiveSubscriptionIdList");
            if (!isEmpty(activeSubIds)) {
                subscriptionId = activeSubIds[0];
            }
        }
        TelephonyManager mTelephonyManager = (TelephonyManager) Reflex.invokeStaticMethod(TelephonyManager.class,
                "from",new Class[]{Context.class},new Object[]{mContext});
        return mTelephonyManager.createForSubscriptionId(subscriptionId);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @SuppressLint("MissingPermission")
    public static boolean isMobileDataEnable(Context mContext) {
        TelephonyManager telephonyManager = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            telephonyManager = getTelephonyManager(mContext);
        }
        if(telephonyManager != null) {
            return telephonyManager.isDataEnabled();
        }
        return false;
    }

    public static String getSimOperatorName(Context mContext) {
        String name = "";
        if (mContext != null) {
            TelephonyManager telephonyManager;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                telephonyManager = getTelephonyManager(mContext);
            }else{
                telephonyManager = (TelephonyManager)mContext.getSystemService(Context.TELEPHONY_SERVICE);
            }
            if(telephonyManager != null) {
                name = telephonyManager.getSimOperatorName();
                if(TextUtils.isEmpty(name)){
                    name = telephonyManager.getNetworkOperatorName();
                }
                String simOperator = telephonyManager.getSimOperator();
                if(TextUtils.isEmpty(simOperator)){
                    simOperator = "";
                }
                //CTCC:中国电信、CUCC:中国联通、CMCC:中国移动 （运营商ISP）
                if(simOperator.equals("46003") || simOperator.equals("46005") ||simOperator.equals("46011")){
                    name = "CTCC";
                }else if(simOperator.equals("46001") || simOperator.equals("46006") ||simOperator.equals("46009")){
                    name = "CUCC";
                }
            }
//            if (TextUtils.isEmpty(name)) {
//                name = getProp("gsm.sim.operator.alpha", "");
//                name = name.replace("[", "").replace("]", "").replace(",", "");
//            }
            if (TextUtils.isEmpty(name)) {
                name = "";
            }
        }
        return name;
    }


    public static String getSimSerialNumber(Context mContext) {
        TelephonyManager telephonyManager = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            telephonyManager = getTelephonyManager(mContext);
        }
        if(telephonyManager != null) {
            return telephonyManager.getSubscriberId();
        }
        return "";
    }

    public static boolean hasSimCard(Context context) {
        TelephonyManager telMgr = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        int simState = telMgr.getSimState();
        boolean result = true;
        switch (simState) {
            case TelephonyManager.SIM_STATE_ABSENT:
            case TelephonyManager.SIM_STATE_UNKNOWN:
                result = false;
                break;
        }
        return result;
    }

    public static boolean isEmpty(@androidx.annotation.Nullable int[] array) {
        return array == null || array.length == 0;
    }


    //获取版本号
    public static String getSystemVersion1() {
        String version = "";
        Method systemProperties_get = null;
        try {
            int time = getIntProp("ro.build.date.utc");
            Log.i(TAG, "XLlog getSystemVersion: " + time);
            long utc = time*1000L;
            SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd.HHmmss");
            format.setTimeZone(TimeZone.getTimeZone("GMT+08")); //北京时区GMT+8
            version = format.format(new Date(utc));
            Log.i(TAG, "XLlog getSystemVersion: " + version);
        } catch (Exception e) {
            e.printStackTrace();
            return "19700101.000000";
        }
        return version;
    }

    //获取版本号
    public static String getSystemVersion() {
        String version = getProp("ro.pandora.version.incremental");
        if(TextUtils.isEmpty(version) || version.length() > 20){
            version = getSystemVersion1();
        }
        return version;
    }

    //获取app的版本号
    public static String getVersionCode(Context context,String packageName) {
        String code = "";
        List<PackageInfo> packages = context.getPackageManager().getInstalledPackages(0);
        for(int i = 0;i < packages.size();i++) {
            PackageInfo packageInfo = packages.get(i);
            if(packageInfo.packageName.equals(packageName)){
                code = packageInfo.versionName;
                break;
            }
        }
        /*String prop = SystemUtils.getSystemVersion1();//Launcher版本号暂时显示为编译时间，确保ota后launcher版本号生效，后续优化
        if(!TextUtils.isEmpty(prop)){
            code = prop;
        }*/
        return code;
    }

    //读取文件内容
    public static String readData(String path) {
        createFile(path);
        StringBuilder res = new StringBuilder();
        BufferedReader bufferedReader = null;
        try {
            bufferedReader = new BufferedReader(new FileReader(path));
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                line.trim();
                if (line.isEmpty()) {
                    continue;
                }
                res.append(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (bufferedReader != null) {
                closeAll(bufferedReader);
            }
        }
        return res.toString();
    }

    //往文件写入内容
    public static void writeData(String path, String value) {
        createFile(path);
        BufferedWriter bufferedWriter = null;
        try {
            bufferedWriter = new BufferedWriter(new FileWriter(path));
            bufferedWriter.write(value);
            bufferedWriter.flush();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (bufferedWriter != null) {
                closeAll(bufferedWriter);
            }
        }
        sync();
    }

    public static void writeData(String path, String value, boolean append) {
        createFile(path);
        BufferedWriter bufferedWriter = null;
        try {
            bufferedWriter = new BufferedWriter(new FileWriter(path, append));
            bufferedWriter.write(value);
            bufferedWriter.flush();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (bufferedWriter != null) {
                closeAll(bufferedWriter);
            }
        }
        sync();
    }

    public static File createFile(String path){
        File file = new File(path);
        File fileParent = file.getParentFile();
        if(!fileParent.exists()){
            fileParent.mkdirs();
        }
        if(!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        sync();
        return file;
    }

    private static void sync(){
        try {
            Runtime.getRuntime().exec("sync");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //关闭流
    public static void closeAll(Closeable...ables){
        for(Closeable c : ables){
            if(c != null){
                try {
                    c.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    //Apollo_L6R_ota_20211024.041824.zip  -> 20211024.041824
    public static String zipToStringB(String version){
        String flag = "_ota_";
        int ota_ = version.indexOf(flag)+flag.length();
        return version.substring(ota_);
    }

    //Apollo_L6R_ota_20211024.041824.zip  -> Apollo_L6R
    public static String zipToStringF(String version){
        String flag = "_ota_";
        int ota_ = version.indexOf(flag);
        return version.substring(0,ota_);
    }

    //这个版本号是否可以升级  高于当前版本可以升级
    public static boolean canUpgradeForVersion(String version) {
        String timeStr = zipToStringB(version);
        Log.i(TAG, "Log canUpgradeForVersion:  = " + timeStr);
        String[] split1 = timeStr.split("\\.");
        String[] split2 = getSystemVersion().split("\\.");
        if(split1.length < 2 || split2.length < 2){
            return false;
        }
        int upgrade_Data = getInt(split1[0]);
        int upgrade_Time = getInt(split1[1]);
        int cur_Data = getInt(split2[0]);
        int cur_Time = getInt(split2[1]);
        if(upgrade_Data > cur_Data){
            return true;
        }else if(upgrade_Data < cur_Data){
            return false;
        }else{
            if(upgrade_Time > cur_Time){
                return true;
            }
        }
        return false;
    }

    public static int getInt(String str){
        int i = 0;
        try {
            i = Integer.parseInt(str);
        }catch (Exception e){
            i = 0;
        }
        return i;
    }

    public static void setDpi(int dpi) {
        setProp("persist.pandora.display_density",""+dpi);
        execShellCmd("wm density "+ dpi);
    }

    //执行adb shell操作
    public static void execShellCmd(String str_cmd) {
        Process process = null;
        try {
            process = Runtime.getRuntime().exec(str_cmd);
            process.waitFor();
            int i = process.exitValue();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static List<String> getAllApps() {
        List<String> mlist = new ArrayList<String>();
        String[] str_cmd2 = {"/bin/sh","-c","pm list  package -u"};
        Process process = null;
        BufferedReader successResult = null;
        BufferedReader errorResult = null;
        StringBuilder successMsg = new StringBuilder();
        StringBuilder errorMsg = new StringBuilder();
        try {
            process = Runtime.getRuntime().exec(str_cmd2);
            successResult = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String s;
            while ((s = successResult.readLine()) != null) {
                mlist.add(s.toString().trim().replace("package:",""));
            }
        } catch (Exception e) {
        } finally {
            closeAll(successResult);
            if (process != null) {
                process.destroy();
            }
        }
        sync();
        return mlist;
    }

    public static String getLauncherMode(Context mContext){
        String mode = Settings.Secure.getString(mContext.getContentResolver(), "launcher_mode");
        if(TextUtils.isEmpty(mode)){
            mode = "0";
        }
        return mode;
    }
}
