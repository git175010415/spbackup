package com.sunmi.printerservice.commandbean;

import android.os.RemoteException;

import com.sunmi.printerservice.exception.ExceptionConst;
import com.sunmi.printerservice.exception.PrinterException;
import com.sunmi.printerservice.render.BitmapCreator;
import com.sunmi.printerservice.utils.C;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

import woyou.aidlservice.jiuiv5.ICallback;

/**
 * Created by Administrator on 2017/8/15.
 */

public class QRCodeTask extends ITask {
    private byte data[];
    private int tmp;


    public QRCodeTask(BitmapCreator bitmapCreator, String text, int modulesize, int errorlevel, ICallback callback) throws PrinterException {
        this.bitmapCreator = bitmapCreator;
        this.createTime = System.currentTimeMillis();
        this.data = getPrintQRCode(text, modulesize, errorlevel);
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
        boolean res = false;
        if (data != null) {
            res = bitmapCreator.sendRAWData(data, callback);
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

    private byte[] getPrintQRCode(String code, int modulesize, int errorlevel) throws PrinterException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try {
            buffer.write(setQRCodeSize(modulesize));
            buffer.write(setQRCodeErrorLevel(errorlevel));
            getQCodeBytes(buffer, code);
            buffer.write(getBytesForPrintQRCode());
        } catch (IOException e) {
            throw new PrinterException(ExceptionConst.CODEFAILED, ExceptionConst.CODEFAILED_MSG);
        } catch (NullPointerException e) {
            throw new PrinterException(ExceptionConst.NullPointer, ExceptionConst.NullPointer_MSG);
        }

        return buffer.toByteArray();
    }


    private byte[] setQRCodeSize(int modulesize) {
        byte[] dtmp = new byte[8];
        dtmp[0] = 0x1D;
        dtmp[1] = 0x28;
        dtmp[2] = 0x6B;
        dtmp[3] = 0x03;
        dtmp[4] = 0x00;
        dtmp[5] = 0x31;
        dtmp[6] = 0x43;
        dtmp[7] = (byte) modulesize;
        return dtmp;
    }

    private byte[] setQRCodeErrorLevel(int errorlevel) {
        byte[] dtmp = new byte[8];
        dtmp[0] = 0x1D;
        dtmp[1] = 0x28;
        dtmp[2] = 0x6B;
        dtmp[3] = 0x03;
        dtmp[4] = 0x00;
        dtmp[5] = 0x31;
        dtmp[6] = 0x45;
        dtmp[7] = (byte) (48 + errorlevel);
        return dtmp;
    }

    private byte[] getBytesForPrintQRCode() {
        byte[] dtmp;
        dtmp = new byte[9];
        dtmp[0] = 0x1D;
        dtmp[1] = 0x28;
        dtmp[2] = 0x6B;
        dtmp[3] = 0x03;
        dtmp[4] = 0x00;
        dtmp[5] = 0x31;
        dtmp[6] = 0x51;
        dtmp[7] = 0x30;
        dtmp[8] = 0x0A;
        return dtmp;
    }

    private void getQCodeBytes(ByteArrayOutputStream buffer, String code) throws NullPointerException, IOException {
        byte[] d = code.getBytes("GB18030");
        int len = d.length + 3;
        if (len > 7092)
            len = 7092;
        buffer.write((byte) 0x1D);
        buffer.write((byte) 0x28);
        buffer.write((byte) 0x6B);
        buffer.write((byte) len);
        buffer.write((byte) (len >> 8));
        buffer.write((byte) 0x31);
        buffer.write((byte) 0x50);
        buffer.write((byte) 0x30);
        for (int i = 0; i < d.length && i < len; i++) {
            buffer.write(d[i]);
        }
    }

}

