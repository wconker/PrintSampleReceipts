package com.example.administrator.print;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Created by Administrator on 2016/3/4.
 */
public class SharedPreferencesUtil {
    private static SharedPreferences sharedPreferences = App.getContext().getSharedPreferences("print", Context.MODE_PRIVATE);
    private static SharedPreferences.Editor editor = sharedPreferences.edit();

    public static float getFloatValue(String key) {
        return sharedPreferences.getFloat(key, 0.0f);
    }

    public static String getStringValue(String key) {
        return sharedPreferences.getString(key, "");
    }

    public static boolean getBooleanValue(String key) {
        return sharedPreferences.getBoolean(key, false);
    }

    public static float getIntValue(String key) {
        return sharedPreferences.getInt(key, 0);
    }

    public static void saveValue(String key, String value) {
        editor.putString(key, value).commit();
    }

    public static void saveValue(String key, float value) {
        editor.putFloat(key, value).commit();
    }

    public static void saveValue(String key, boolean value) {
        editor.putBoolean(key, value).commit();
    }

    public static void saveValue(String key, int value) {
        editor.putInt(key, value).commit();
    }


    /**
     * 存放mac地址
     *
     * @return
     */
    public static String getMac() {
        return sharedPreferences.getString("macAddress", "");
    }

    public static void setMac(String macAddress) {
        editor.putString("macAddress", macAddress).commit();
    }


    /**
     * 清除
     */
    public static void clearData() {
    }

}
