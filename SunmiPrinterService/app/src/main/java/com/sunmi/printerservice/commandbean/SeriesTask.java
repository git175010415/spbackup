package com.sunmi.printerservice.commandbean;

import com.sunmi.printerservice.render.BitmapCreator;

import woyou.aidlservice.jiuiv5.ICallback;

/**
 * Created by Administrator on 2017/8/15.
 */

public class SeriesTask extends ITask{
    public static final int COMMON = 0;
    public static final int HEAD = 1;
    public static final int TAIL = 2;
    private int series;

    public SeriesTask(BitmapCreator bitmapCreator, int series) {
        this.bitmapCreator = bitmapCreator;
        this.createTime = System.currentTimeMillis();
        this.series = series;
    }

    public SeriesTask(BitmapCreator bitmapCreator, int series, ICallback iCallback) {
        this.bitmapCreator = bitmapCreator;
        this.createTime = System.currentTimeMillis();
        this.series = series;
        this.callback = iCallback;
    }

    @Override
    public void run() {
        bitmapCreator.runtime(createTime);
        bitmapCreator.sendSeriesData(series, callback);
    }
}
