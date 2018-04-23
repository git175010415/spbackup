package com.sunmi.printerservice.entity;

import android.content.Context;
import android.graphics.Typeface;

import com.sunmi.printerservice.utils.LogUtils;

import woyou.aidlservice.jiuiv5.BuildConfig;

public class ServiceValue {
    private int lastDistance;               //最近一次MCU记录的切刀距离
    private int lastCut;                    //最近一次MCU记录的切刀次数
    private int lastOpen;                   //最近一次MCU记录的开箱次数
    private int lastOrder;                  //最近一次MCU记录的订单总数
    private int lastHot;                    //最近一次MCU记录的过热次数

    private int myId;                       //用于更新全球化设置
    private boolean isTextTimes;            //是否倍宽
    private boolean reversePrintingMode;    //是否反白
    private boolean isFakeBoldText;         //是否加粗
    private boolean isUnderlineText;        //是否下划
    private boolean isSetLineHeight;        //是否设置行高
    private int TextTimesHeight;            //倍高值
    private int TextTimesWidth;            //倍宽值
    private int lineHeight;                 //行高
    private int paper;                      //纸张宽度
    private Typeface mTypeface;             //正在使用的字体
    private Typeface typefaceGh;            //gh字体 老字体
    private Typeface typefaceKaltin;        //kaltin字体 新字体

    private long initTime;
    private long uploadTime;
    private StringBuilder uploadData;

    public ServiceValue(Context context) {
        myId = 0;
        lastDistance = 0;
        lastOpen = 0;
        lastCut = 0;
        lastOrder = 0;
        lastHot = 0;
        paper = BuildConfig.CANVASWIDTH;
        if (BuildConfig.DEFAULTFONT) {
            typefaceGh = Typeface.createFromAsset(context.getAssets(), "gh.ttf");
            mTypeface = typefaceGh;
        } else {
            typefaceKaltin = Typeface.createFromAsset(context.getAssets(), "kaltin.ttf");
            mTypeface = typefaceKaltin;
        }

        initTime = -1;
        uploadTime = -1;
        uploadData = new StringBuilder();
    }

    public boolean compareId(int id) {
        return myId == id;
    }

    public int getMyId() {
        return myId;
    }

    //使能生效
    public void updateMyId() {
        myId++;
    }

    public int updateDistance(int distance) {
        int delta = distance - lastDistance;
        lastDistance = distance;
        return delta > 0 ? delta : 0;
    }

    public int updateCut(int cut) {
        int delta = cut - lastCut;
        lastCut = cut;
        return delta > 0 ? delta : 0;
    }

    public int updateOpen(int open) {
        int delta = open - lastOpen;
        lastOpen = open;
        return delta > 0 ? delta : 0;
    }

    public int updateOrder(int order){
        int delta = order - lastOrder;
        lastOrder = order;
        return delta > 0 ? delta : 0;
    }

    public int updateHot(int hot){
        int delta = hot - lastHot;
        lastHot = hot;
        return delta > 0? delta:0;
    }

    public boolean isTextTimes() {
        return isTextTimes;
    }

    public void setTextTimes(boolean textTimes) {
        isTextTimes = textTimes;
    }

    public boolean isReversePrintingMode() {
        return reversePrintingMode;
    }

    public void setReversePrintingMode(boolean reversePrintingMode) {
        this.reversePrintingMode = reversePrintingMode;
    }

    public boolean isFakeBoldText() {
        return isFakeBoldText;
    }

    public void setFakeBoldText(boolean fakeBoldText) {
        isFakeBoldText = fakeBoldText;
    }

    public boolean isUnderlineText() {
        return isUnderlineText;
    }

    public void setUnderlineText(boolean underlineText) {
        isUnderlineText = underlineText;
    }

    public int getTextTimesHeight() {
        return TextTimesHeight;
    }

    public void setTextTimesHeight(int textTimesHeight) {
        TextTimesHeight = textTimesHeight;
    }

    public int getTextTimesWidth() {
        return TextTimesWidth;
    }

    public void setTextTimesWidth(int textTimesWidth) {
        TextTimesWidth = textTimesWidth;
    }

    public boolean isSetLineHeight() {
        return isSetLineHeight;
    }

    public void setSetLineHeight(boolean setLineHeight) {
        isSetLineHeight = setLineHeight;
    }

    public int getLineHeight() {
        return lineHeight;
    }

    public void setLineHeight(int lineHeight) {
        this.lineHeight = lineHeight;
    }

    public int getPaper() {
        return paper;
    }

    public void setPaper(int paper) {
        this.paper = paper;
    }

    public Typeface getTypeface() {
        return mTypeface;
    }

    public void setTypeface(Context context, boolean isDefault) {
        if (isDefault) {
            if (typefaceGh == null) {
                typefaceGh = Typeface.createFromAsset(context.getAssets(), "gh.ttf");
            }
            mTypeface = typefaceGh;
        } else {
            if (typefaceKaltin == null) {
                typefaceKaltin = Typeface.createFromAsset(context.getAssets(), "kaltin.ttf");
            }
            mTypeface = typefaceKaltin;
        }
    }

    public long getInitTime() {
        return initTime;
    }

    public void setInitTime(long initTime) {
        this.initTime = initTime;
    }

    public long getUploadTime() {
        return uploadTime;
    }

    public void setUploadTime(long uploadTime) {
        this.uploadTime = uploadTime;
    }

    public String getUploadData() {
        return uploadData.toString();
    }

    public void appendUploadData(String content) {
        uploadData.append(content);
    }

    public void clearUploadData(){
        uploadData.delete(0, uploadData.length());
    }
}
