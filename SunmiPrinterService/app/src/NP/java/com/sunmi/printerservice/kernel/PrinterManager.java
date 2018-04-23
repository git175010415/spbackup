package com.sunmi.printerservice.kernel;


import android.content.Context;
import android.os.Handler;

import com.longcheer.spijni.SpiJni;
import com.sunmi.printerservice.cell.DataCell;
import com.sunmi.printerservice.manager.PrinterState;
import com.sunmi.printerservice.render.RawPrintInterface;
import com.sunmi.printerservice.thread.DownloadInterface;

import static com.sunmi.printerservice.manager.PrinterState.R_BOXTIMES;


/*
 *
 */
public class PrinterManager implements DownloadInterface, RawPrintInterface {
    private PrinterState printerState;
    private SpiJni mSpiJni;

    public static byte[] INVALIDPACKAGE = new byte[256];


    public PrinterManager(Context service, Handler handler) {
        printerState = new PrinterState();
        mSpiJni = new SpiJni();
    }

    //获得打印机正常工作后MCU的信息，如果打印机未工作（无打印机或准备失败）则获取为空
    public String getInitStatus(int info) {
        return "";
    }

    //获取打印机实时反馈记录信息，如果未工作将返回-1
    //实时反馈需要updatePrinterState
    public int getRealStatus(int info) {
        if(info == R_BOXTIMES){
            return printerState.boxCount;
        }else{
            return  -1;
        }
    }

    //打印机状态反馈 1正常 2准备中 3通信异常 4缺纸 5过热 6开盖 7切刀异常 8切刀修复 9黑标 10未完全关盖 505:无打印机
    public int getPrinterState() {
        return 2;
    }

    @Override
    public void hasPrinter(boolean isExist) {

    }

    @Override
    public void startUpdate() {

    }

    @Override
    public void updateStatus(boolean result) {

    }

    @Override
    public void startPrinter() {

    }

    @Override
    public void sendDataCell(DataCell p) {

    }

    @Override
    public synchronized void openBox() {
        mSpiJni.gpio_write(mSpiJni.fd_box, 1);
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        printerState.boxCount++;
    }

    public void close() {
        printerState.initStatus = 0;
        mSpiJni.close();
    }
}
