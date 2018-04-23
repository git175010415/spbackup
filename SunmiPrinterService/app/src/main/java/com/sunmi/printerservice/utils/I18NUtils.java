package com.sunmi.printerservice.utils;

import android.content.Context;

import java.util.Locale;
import java.util.TimeZone;

/**
 * 时区语言工具类
 * @author Xiho
 *
 */
public class I18NUtils {

	/**
	 * 获取当前时区
	 * @return
	 */
	public static String getCurrentTimeZone() {
		TimeZone tz = TimeZone.getDefault();
		String strTz = tz.getDisplayName(false, TimeZone.SHORT);
		return strTz;

	}
	
	
	/**
	 * 获取当前系统语言格式
	 * @param mContext
	 * @return
	 */
	public static String getCurrentLanguage(Context mContext){
	    Locale locale = mContext.getResources().getConfiguration().locale;
        String language=locale.getLanguage();
        String country = locale.getCountry();
        String lc=language+"_"+country;
        return lc;
	}
	

}
