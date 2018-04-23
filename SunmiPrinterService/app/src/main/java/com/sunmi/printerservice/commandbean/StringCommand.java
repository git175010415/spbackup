package com.sunmi.printerservice.commandbean;

import android.os.RemoteException;

import com.sunmi.printerservice.exception.ExceptionConst;
import com.sunmi.printerservice.exception.PrinterException;
import com.sunmi.printerservice.render.BitmapCreator;
import com.sunmi.printerservice.utils.C;

import woyou.aidlservice.jiuiv5.ICallback;

public class StringCommand extends ITask{
    private String data;
    private String typeface;
    private float fontsize;
    private int flag;
    private int tmp;

    public StringCommand(BitmapCreator bitmapCreator, String data, ICallback callback, boolean tr) throws PrinterException {
        this.bitmapCreator = bitmapCreator;
        this.createTime = System.currentTimeMillis();
        this.data = data;
        this.callback = callback;
        try{
            tmp = data.getBytes().length;
        }catch (NullPointerException e){
            throw new PrinterException(ExceptionConst.NullPointer, ExceptionConst.NullPointer_MSG);
        }
        if (bitmapCreator.getCurrentMemory() + tmp > C.BC_MAXMEMORY) {
            throw new PrinterException(ExceptionConst.ADDTASKFAILED, ExceptionConst.ADDTASKFAILED_MSG);
        } else {
            bitmapCreator.addCurrentMemory(tmp);
        }
        if(tr)
            flag = 3;
        else
            flag = 1;
    }

    public StringCommand(BitmapCreator bitmapCreator, String data, String typeface, float fontsize, ICallback callback)
            throws PrinterException {
        this.bitmapCreator = bitmapCreator;
        this.createTime = System.currentTimeMillis();
        this.data = data;
        this.callback = callback;
        try{
            tmp = data.getBytes().length;
        }catch (NullPointerException e){
            throw new PrinterException(ExceptionConst.NullPointer, ExceptionConst.NullPointer_MSG);
        }
        if (bitmapCreator.getCurrentMemory() + tmp> C.BC_MAXMEMORY) {
            throw new PrinterException(ExceptionConst.ADDTASKFAILED, ExceptionConst.ADDTASKFAILED_MSG);
        } else {
            bitmapCreator.addCurrentMemory(tmp);
        }
        this.typeface = typeface;
        this.fontsize = fontsize;
        flag = 2;
    }

    @Override
    public void run() {
        bitmapCreator.runtime(createTime);
        boolean res = false;
        if (data != null) {
            if (flag==2) {
                res = bitmapCreator.printText(data, typeface, fontsize);
            } else if (flag==1){
                res = bitmapCreator.printText(data);
            }else if (flag==3){
                res = bitmapCreator.printText(data);
            }
        }
        bitmapCreator.delCurrentMemory(tmp);
        if (callback != null) {
            try {
                callback.onRunResult(res);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }
}
