package com.sunmi.printerservice.commandbean;

import com.sunmi.printerservice.render.BitmapCreator;

import woyou.aidlservice.jiuiv5.ICallback;

/**
 * Created by Administrator on 2017/8/10.
 */

public abstract class ITask implements Runnable {
    public long taskId;
    protected ICallback callback;
    protected BitmapCreator bitmapCreator;
    protected long createTime;
}
