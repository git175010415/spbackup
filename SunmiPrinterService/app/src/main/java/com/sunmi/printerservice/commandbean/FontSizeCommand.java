package com.sunmi.printerservice.commandbean;

import android.os.RemoteException;

import com.sunmi.printerservice.exception.ExceptionConst;
import com.sunmi.printerservice.exception.PrinterException;
import com.sunmi.printerservice.render.BitmapCreator;
import com.sunmi.printerservice.utils.C;

import woyou.aidlservice.jiuiv5.ICallback;

public class FontSizeCommand extends ITask{
    private float size;

    public FontSizeCommand(BitmapCreator bitmapCreator, float size, ICallback callback) throws PrinterException {
        this.bitmapCreator = bitmapCreator;
        this.createTime = System.currentTimeMillis();
        this.size = size;
        this.callback = callback;
        if (bitmapCreator.getCurrentMemory() + 4 > C.BC_MAXMEMORY) {
            throw new PrinterException(ExceptionConst.ADDTASKFAILED, ExceptionConst.ADDTASKFAILED_MSG);
        } else {
            bitmapCreator.addCurrentMemory(4);
        }
    }


    @Override
    public void run() {
        bitmapCreator.runtime(createTime);
        boolean res = bitmapCreator.setFontSize(size);
        bitmapCreator.delCurrentMemory(4);
        if (callback != null) {
            try {
                callback.onRunResult(res);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }
}
