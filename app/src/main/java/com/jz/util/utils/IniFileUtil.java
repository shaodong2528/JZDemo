package com.jz.util.utils;

import android.text.TextUtils;
import android.util.Log;


import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.URLDecoder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Copyright (C)
 * FileName: IniFileUtil
 * Author: zhengfei
 * Date: 2022/4/11 21:49
 * Description:IniFileUtil
 */
public class IniFileUtil {

    public static String readCfgValue(String file, String section, String variable, String defaultValue)
            throws IOException {
        String strLine, value = "";
        BufferedReader bufferedReader = new BufferedReader(new FileReader(URLDecoder.decode(file, "UTF-8")));
        boolean isInSection = false;
        try {
            while ((strLine = bufferedReader.readLine()) != null) {
                strLine = strLine.trim();
                strLine = strLine.split("[;]")[0];
                Pattern p;
                Matcher m;
                p = Pattern.compile("\\[\\w+]");
                m = p.matcher((strLine));
                if (m.matches()) {
                    p = Pattern.compile("\\[" + section + "\\]");
                    m = p.matcher(strLine);
                    if (m.matches()) {
                        isInSection = true;
                    } else {
                        isInSection = false;
                    }
                }
                if (isInSection == true) {
                    strLine = strLine.trim();
                    String[] strArray = strLine.split("=");
                    if (strArray.length == 1) {
                        value = strArray[0].trim();
                        if (value.equalsIgnoreCase(variable)) {
                            value = "";
                            return value;
                        }
                    } else if (strArray.length == 2) {
                        value = strArray[0].trim();
                        if (value.equalsIgnoreCase(variable)) {
                            value = strArray[1].trim();
                            return value;
                        }
                    } else if (strArray.length > 2) {
                        value = strArray[0].trim();
                        if (value.equalsIgnoreCase(variable)) {
                            value = strLine.substring(strLine.indexOf("=") + 1).trim();
                            return value;
                        }
                    }
                }
            }
        } finally {
            bufferedReader.close();
        }
        return defaultValue;
    }

    public static boolean isAutoRun(String file,String autoStart) {
        String strLine, value = "",name="";
        boolean auto = false;
        BufferedReader bufferedReader = null;
        try {
            bufferedReader = new BufferedReader(new FileReader(URLDecoder.decode(file, "UTF-8")));
            while ((strLine = bufferedReader.readLine()) != null) {
                strLine = strLine.trim();
                String[] strArray = strLine.split("=");
                if (strArray.length == 2) {
                    name = strArray[0].trim();
                    value = strArray[1].trim();
                    Log.d("===zxd","name="+name+",value="+value);
                    if(!TextUtils.isEmpty(name) && !TextUtils.isEmpty(value)){
                        if(name.equalsIgnoreCase(autoStart)){
                            auto = Boolean.valueOf(value);
                        }else if(name.equalsIgnoreCase("wifi_test")){
                            SystemUtils.setProp("persist.wifi.enable",value);
                        }else if(name.equalsIgnoreCase("bt_test")){
                            SystemUtils.setProp("persist.blue.enable",value);
                        }else if(name.equalsIgnoreCase("audio_play_test")){
                            SystemUtils.setProp("persist.music.start",value);
                        }else if(name.equalsIgnoreCase("wifi_name")){
                            SystemUtils.setProp("persist.wifi.name",value);
                        }else if(name.equalsIgnoreCase("wifi_pwd")){
                            SystemUtils.setProp("persist.wifi.pwd",value);
                        }
                    }
                }
            }
        } catch (Exception e){
            e.printStackTrace();
        }finally {
            try {
                bufferedReader.close();
            }catch (Exception e){
                e.printStackTrace();
            }
            return auto;
        }
    }

}
