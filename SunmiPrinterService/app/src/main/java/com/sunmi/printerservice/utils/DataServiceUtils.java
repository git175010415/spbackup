package com.sunmi.printerservice.utils;

import android.content.Context;

import com.sunmi.statistics.library.DataBean;
import com.sunmi.statistics.library.MataDataConstant;
import com.sunmi.statistics.library.StatisticsManager;

import java.util.LinkedList;
import java.util.List;

/**
 * Author : kaltin
 * Create : 2018/3/26 15:57
 * Describe :
 */

public class DataServiceUtils {

    public static final String PRINT_DATA = "PRINT_DATA";
    public static final String PRINT_TEXT = "PRINT_TEXT";

    private static final int UPLOAD_INTERVAL_SECOND = 60 * 1000;
    private static final int UPLOAD_INTERVAL_MINUTE = UPLOAD_INTERVAL_SECOND * 30;
    public static final int UPLOAD_INTERVAL_TIME = UPLOAD_INTERVAL_SECOND;

    public static long getNetTime(Context context){
        return StatisticsManager.getInstance().getNetTime(context);
    }

    private static void addData(Context context, List<DataBean> data){
        StatisticsManager.getInstance().addList(context, data);
    }

    public static void addPrintData(Context context, String data){
        DataBean mDataBean = new DataBean();
        mDataBean.type = MataDataConstant.TYPE_INFO;
        mDataBean.packageName = C.PUSH_ACTION;
        mDataBean.businessType = PRINT_DATA;
        mDataBean.priorityType = MataDataConstant.LOW_PRIORITY;
        mDataBean.flowType = MataDataConstant.FLOW_RICH;
        mDataBean.updateType = MataDataConstant.TYPE_SCHEDULE;
        mDataBean.time = getNetTime(context);
        mDataBean.content = data;
        List<DataBean> list = new LinkedList<>();
        list.add(mDataBean);
        addData(context, list);
    }

    public static void addPrintText(Context context, String data, long time){
        DataBean mDataBean = new DataBean();
        mDataBean.type = MataDataConstant.TYPE_INFO;
        mDataBean.packageName = C.PUSH_ACTION;
        mDataBean.businessType = PRINT_TEXT;
        mDataBean.priorityType = MataDataConstant.LOW_PRIORITY;
        mDataBean.flowType = MataDataConstant.FLOW_RICH;
        mDataBean.updateType = MataDataConstant.TYPE_SCHEDULE;
        mDataBean.time = time;
        mDataBean.content = data;
        List<DataBean> list = new LinkedList<>();
        list.add(mDataBean);
        addData(context, list);
    }
}
