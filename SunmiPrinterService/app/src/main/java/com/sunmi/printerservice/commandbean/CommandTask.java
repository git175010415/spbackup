package com.sunmi.printerservice.commandbean;

import android.os.RemoteException;

import com.sunmi.printerservice.exception.ExceptionConst;
import com.sunmi.printerservice.exception.PrinterException;
import com.sunmi.printerservice.render.BitmapCreator;
import com.sunmi.printerservice.utils.C;

import woyou.aidlservice.jiuiv5.ICallback;

public class CommandTask extends ITask {
    private byte data[];
    private int tmp;

    public CommandTask(BitmapCreator bitmapCreator, byte data[], ICallback callback) throws PrinterException {
        this.bitmapCreator = bitmapCreator;
        this.createTime = System.currentTimeMillis();
        this.data = data;
        this.callback = callback;
        if(data == null){
            throw new PrinterException(ExceptionConst.NullPointer, ExceptionConst.NullPointer_MSG);
        }else{
            tmp = data.length;
        }
        if (bitmapCreator.getCurrentMemory() + tmp > C.BC_MAXMEMORY) {
            throw new PrinterException(ExceptionConst.ADDTASKFAILED, ExceptionConst.ADDTASKFAILED_MSG);
        } else {
            bitmapCreator.addCurrentMemory(tmp);
        }
    }

    @Override
    public void run() {
        bitmapCreator.runtime(createTime);
        boolean res =  bitmapCreator.sendRAWData(data, callback);
        bitmapCreator.delCurrentMemory(tmp);
        try {
            if (callback != null) {
                callback.onRunResult(res);
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }
}
