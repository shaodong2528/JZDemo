package com.jz.util;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.IBinder;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.Nullable;
import com.jz.util.utils.IniFileUtil;
import com.jz.util.utils.SystemUtils;
import java.io.File;

public class MainService extends Service {

    private static final String TAG = "MainService2";
    private final String toolApk = "jzUtils.apk";
    private final String tool_ini = "jzTool.ini";
    private final String toolPkg = "com.jz.util";


    @Override
    public void onCreate() {
        super.onCreate();
        Log.d("===zxd","checkutil oncreate");
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_MEDIA_MOUNTED);
        intentFilter.addAction(Intent.ACTION_MEDIA_EJECT);
        intentFilter.addAction(Intent.ACTION_MEDIA_REMOVED);
        intentFilter.addDataScheme("file");
        intentFilter.setPriority(IntentFilter.SYSTEM_HIGH_PRIORITY);
        registerReceiver(mMediaReceiver, intentFilter);
    }

    private long preTime;
    /**
     * U盘挂载信息
     */
    private BroadcastReceiver mMediaReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d("===zxd","mMediaReceiver");
            if (null == intent || null == intent.getAction()) {
                return;
            }
            final String action = intent.getAction();
            final Uri uri = intent.getData();
            Log.d("===zxd","checkutil ac="+action+",uri="+uri);
            if (uri != null && uri.getScheme() != null && uri.getScheme().equals("file")) {
                String path = uri.getPath();
                Log.i(TAG, "Log onReceive: path = " + path);
                if (!TextUtils.isEmpty(path)) {
                    if (action.equals(Intent.ACTION_MEDIA_MOUNTED)) {
                        long curTime = System.currentTimeMillis();
                        if (curTime - preTime > 1000) {//防止插U盘后 走两次问题
                            preTime = curTime;
                            Log.i(TAG, "Log onReceive: path = mounted");
                            if(!path.equals(Constants.EXTERNAL_PATH)) {
                                Constants.UDISK1_PATH = path;
                                //20231016 新增安装检测工具
                                File apkini = new File(path + File.separator + tool_ini);
                                if(apkini.isFile() && apkini.exists()){
                                    boolean auto = IniFileUtil.isAutoRun(apkini.getAbsolutePath(),"auto_run_test");  //是否自动打开运行
                                    if(SystemUtils.isInstalled(getApplicationContext(),toolPkg)){
                                        if(auto){
                                            /*Intent toolIntent = new Intent(Intent.ACTION_VIEW);
                                            toolIntent.setClassName(toolPkg,"com.zxd.demo.SystemUtilsActivity");
                                            startActivity(toolIntent);*/
                                            SystemUtils.RunApp(MainService.this,toolPkg);
                                        }
                                    }else{
                                        File toolapk = new File(path + File.separator + toolApk);
                                        if(toolapk.isFile() && toolapk.exists()){
                                            SystemUtils.execShellCmd3(getApplicationContext(), "pm install -r " + toolapk.getAbsolutePath(), new SystemUtils.ExecShellListener() {
                                                @Override
                                                public void onComplete(boolean isok) {
                                                    Log.d("===zxd","isOk="+isok+",autoRun="+auto);
                                                    if(isok && auto){
                                                        /*Intent toolIntent = new Intent(Intent.ACTION_VIEW);
                                                        toolIntent.setClassName(toolPkg,"com.zxd.demo.SystemUtilsActivity");
                                                        startActivity(toolIntent);*/
                                                        SystemUtils.RunApp(MainService.this,toolPkg);
                                                    }
                                                }
                                            });
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    };


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mMediaReceiver);
    }
}
