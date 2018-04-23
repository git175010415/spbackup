package com.sunmi.printerservice.commandbean;

import android.os.RemoteException;

import com.sunmi.printerservice.exception.ExceptionConst;
import com.sunmi.printerservice.exception.PrinterException;
import com.sunmi.printerservice.render.BitmapCreator;
import com.sunmi.printerservice.utils.C;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import woyou.aidlservice.jiuiv5.ICallback;

public class BarCodeTask extends ITask {
    private byte data[];
    private int tmp;

    public BarCodeTask(BitmapCreator bitmapCreator, String text, int symbology, int height, int width, int textposition,
                       ICallback callback) throws PrinterException {
        this.bitmapCreator = bitmapCreator;
        this.createTime = System.currentTimeMillis();
        this.data = getPrintBarCode(text, symbology, height, width, textposition);
        this.callback = callback;
        tmp = data.length;
        if (bitmapCreator.getCurrentMemory() + tmp > C.BC_MAXMEMORY) {
            throw new PrinterException(ExceptionConst.ADDTASKFAILED, ExceptionConst.ADDTASKFAILED_MSG);
        } else {
            bitmapCreator.addCurrentMemory(tmp);
        }
    }


    @Override
    public void run() {
        bitmapCreator.runtime(createTime);
        boolean res =false;
        if (data != null) {
            res =  bitmapCreator.sendRAWData(data, callback);
        }
        bitmapCreator.delCurrentMemory(tmp);
        try {
            if (callback != null) {
                callback.onRunResult(res);
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private  byte[] getPrintBarCode(String data, int symbology, int height, int width, int textposition)throws PrinterException {
        if(data == null || data.equals("")){
            throw new PrinterException(ExceptionConst.NullPointer, ExceptionConst.NullPointer_MSG);
        }

        if (symbology < 0 || symbology > 8) {
            return new byte[] { 0x0A };
        }
        if (width < 2 || width > 6) {
            width = 2;
        }
        if (textposition < 0 || textposition > 3) {
            textposition = 0;
        }
        if (height < 1 || height > 255) {
            height = 162;
        }
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try {
            buffer.write(new byte[] { 0x1D, 0x66, 0x01, 0x1D, 0x48, (byte) textposition, 0x1D, 0x77, (byte) width, 0x1D, 0x68, (byte) height, 0x0A });
            byte[] barcode = data.getBytes();
            if (symbology == 8) {//aidl接口默认code128b
                buffer.write(new byte[] { 0x1D, 0x6B, 0x49, (byte) (barcode.length + 2), 0x7B, 0x42 });
            } else {
                buffer.write(new byte[] { 0x1D, 0x6B, (byte) (symbology + 0x41), (byte) barcode.length });
            }
            buffer.write(barcode);
        } catch (IOException e) {
            throw new PrinterException(ExceptionConst.CODEFAILED, ExceptionConst.CODEFAILED_MSG);
        }
        return buffer.toByteArray();
    }
}
