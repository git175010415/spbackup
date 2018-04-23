package com.sunmi.printerservice.commandbean;

import android.graphics.Bitmap;
import android.os.RemoteException;

import com.sunmi.printerservice.exception.ExceptionConst;
import com.sunmi.printerservice.exception.PrinterException;
import com.sunmi.printerservice.render.BitmapCreator;
import com.sunmi.printerservice.utils.C;
import com.sunmi.printerservice.utils.Utils;

import woyou.aidlservice.jiuiv5.BuildConfig;
import woyou.aidlservice.jiuiv5.ICallback;

public class BitMapCommand extends ITask{
    private Bitmap data;
    private int tmp;
    private int type;

    public BitMapCommand(BitmapCreator bitmapCreator, Bitmap pic, ICallback callback) throws PrinterException {
        this.bitmapCreator = bitmapCreator;
        this.createTime = System.currentTimeMillis();
        this.data = pic;
        this.type = 0;
        this.callback = callback;
        if(data == null){
            throw new PrinterException(ExceptionConst.NullPointer, ExceptionConst.NullPointer_MSG);
        }
        tmp = data.getHeight() * data.getWidth() * 4;
        if (bitmapCreator.getCurrentMemory() + tmp > C.BC_MAXMEMORY) {
            throw new PrinterException(ExceptionConst.ADDTASKFAILED, ExceptionConst.ADDTASKFAILED_MSG);
        } else {
            bitmapCreator.addCurrentMemory(tmp);
        }
    }

    public BitMapCommand(BitmapCreator bitmapCreator, Bitmap pic, int type, ICallback callback) throws PrinterException {
        this.bitmapCreator = bitmapCreator;
        this.createTime = System.currentTimeMillis();
        this.data = pic;
        this.type = type;
        this.callback = callback;
        if(data == null){
            throw new PrinterException(ExceptionConst.NullPointer, ExceptionConst.NullPointer_MSG);
        }
        tmp = data.getHeight() * data.getWidth() * 4;
        if (bitmapCreator.getCurrentMemory() + tmp > C.BC_MAXMEMORY) {
            throw new PrinterException(ExceptionConst.ADDTASKFAILED, ExceptionConst.ADDTASKFAILED_MSG);
        } else {
            bitmapCreator.addCurrentMemory(tmp);
        }
    }



    @Override
    public void run() {
        bitmapCreator.runtime(createTime);
        boolean res = false;
        if(data != null){
            if(type == 0){
                if(BuildConfig.FLAVOR == "T1"){
                    res = bitmapCreator.printBitmap(Utils.convertToDithering(data));
                }else{
                    res = bitmapCreator.printBitmap(Utils.convertToBlackWhite(data, 128));
                }
            }else if(type == 1){
                res = bitmapCreator.printBitmap(Utils.convertToBlackWhite(data, 200));
            }else if(type == 2){
                res = bitmapCreator.printBitmap(Utils.convertToDithering(data));
            }
        }
        bitmapCreator.delCurrentMemory(tmp);
        if(callback != null){
            try {
                callback.onRunResult(res);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }
}
