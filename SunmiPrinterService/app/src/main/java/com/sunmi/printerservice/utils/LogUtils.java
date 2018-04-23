package com.sunmi.printerservice.utils;

import android.util.Log;

import woyou.aidlservice.jiuiv5.BuildConfig;


public class LogUtils {
    private static final String TAG = "kaltin_printerservice";
    private LogUtils(){}

    private static boolean isDebuggable() {
        return BuildConfig.DEBUG;
    }

    public static void v(String msg){
        if(!isDebuggable()){
            return;
        }

        Log.v(TAG, createLog(msg));
    }

    public static void d(String msg){
        if(!isDebuggable()){
            return;
        }

        Log.d(TAG, createLog(msg));
    }

    public static void i(String msg){
        if(!isDebuggable()){
            return;
        }

        Log.i(TAG, createLog(msg));
    }

    public static void w(String msg){
        if(!isDebuggable()){
            return;
        }

        Log.w(TAG, createLog(msg));
    }

    public static void e(String msg){
        if(!isDebuggable()){
            return;
        }

        Log.e(TAG, createLog(msg));
    }

    private static String createLog(String msg){
        StackTraceElement targetStackTraceElement = null;
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        boolean isTrace = false;
        for(StackTraceElement stackTraceElement:stackTrace){
            boolean isLogMethod = stackTraceElement.getClassName().equals(LogUtils.class.getName());
            if(isTrace && !isLogMethod){
                targetStackTraceElement = stackTraceElement;
            }
            isTrace = isLogMethod;
        }

        //使用StringBuffer因为是线程安全的
        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append("[(").append(targetStackTraceElement.getFileName()).append(":").append(targetStackTraceElement.getLineNumber()).append(")#").append(targetStackTraceElement.getMethodName()).append("]");
        stringBuffer.append(msg);
        return  stringBuffer.toString();
    }
}
