package com.sunmi.printerservice.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class PreferencesLoader {
	private SharedPreferences mSharedPreferences;
	private Context mContext;

    /**
     *  string 为空时用默认
     *  author by kaltin
     *  created at 2016/11/21 0021 17:57
     */
    public PreferencesLoader(Context context, String file) {
        mContext = context;
        if(file == null || file.equals(""))
            mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);
        else
            mSharedPreferences = mContext.getSharedPreferences(file, Context.MODE_PRIVATE);
    }

    public void saveBoolean(int keyResId, Boolean value) {
        String key = mContext.getString(keyResId);
        saveBoolean(key, value);
    }


    public void saveBoolean(String key, Boolean value) {
        SharedPreferences.Editor editor = mSharedPreferences.edit();
        editor.putBoolean(key, value);
        editor.apply();
    }


    public Boolean getBoolean(String key) {
        return mSharedPreferences.getBoolean(key, false);
    }


    public Boolean getBoolean(String key, boolean def) {
        return mSharedPreferences.getBoolean(key, def);
    }


    public Boolean getBoolean(int keyResId, boolean def) {
        String key = mContext.getString(keyResId);
        return mSharedPreferences.getBoolean(key, def);
    }


    public int getInt(String key) {
        return mSharedPreferences.getInt(key, -1);
    }


    public void saveInt(String key, int value) {
        SharedPreferences.Editor editor = mSharedPreferences.edit();
        editor.putInt(key, value);
        editor.apply();
    }

    public String getString(String key){return mSharedPreferences.getString(key, "");}

    public void saveString(String key, String value){
        SharedPreferences.Editor editor = mSharedPreferences.edit();
        editor.putString(key, value);
        editor.apply();
    }
}
