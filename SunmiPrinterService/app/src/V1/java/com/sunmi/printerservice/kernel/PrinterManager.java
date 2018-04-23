package com.sunmi.printerservice.kernel;


import android.content.Context;
import android.os.Handler;
import android.os.PowerManager;

import com.longcheer.spijni.SpiJni;
import com.sunmi.printerservice.cell.DataCell;
import com.sunmi.printerservice.manager.PrinterState;
import com.sunmi.printerservice.render.RawPrintInterface;
import com.sunmi.printerservice.thread.DownloadRunable;
import com.sunmi.printerservice.thread.DownloadInterface;
import com.sunmi.printerservice.utils.C;

import java.util.Arrays;
import java.util.Comparator;
import java.util.PriorityQueue;

import static com.sunmi.printerservice.manager.PrinterState.P_FACORYNAME;
import static com.sunmi.printerservice.manager.PrinterState.P_NAME;
import static com.sunmi.printerservice.manager.PrinterState.P_NO;
import static com.sunmi.printerservice.manager.PrinterState.P_VERSION;
import static com.sunmi.printerservice.manager.PrinterState.R_PRINTINGLENGTH;

public class PrinterManager implements DownloadInterface, RawPrintInterface {
    private PrinterState printerState;
    private SpiJni mSpiJni;
    private Thread mDownloadRunable;
    private Thread sendThread;
    private Thread watcherThread;
    private PowerManager.WakeLock wakeLock;
    private SleepRunnable sleepRunnable;
    private Handler handler;
    private boolean isSleep;

    public static byte[] INVALIDPACKAGE = new byte[256];

    private byte[] send = new byte[256];
    private byte[] recv = new byte[256];
    private int memory = 0;
    private boolean interrupt = false;
    private Object memoryBlock = new Object();
    private Object lock = new Object();
    private PriorityQueue<DataCell> dataList = new PriorityQueue<>(100, new Comparator<DataCell>() {

        @Override
        public int compare(DataCell lhs, DataCell rhs) {
            if (lhs.type - rhs.type < 0) {
                return 1;
            } else if (lhs.type - rhs.type > 0) {
                return -1;
            } else {
                return (int) (lhs.time - rhs.time);
            }
        }
    });


    public PrinterManager(Context service, Handler handler) {
        this.handler = handler;
        printerState = new PrinterState();
        mSpiJni = new SpiJni();
        PowerManager pm = (PowerManager) service.getSystemService(Context.POWER_SERVICE);
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK | PowerManager.ON_AFTER_RELEASE, getClass().getCanonicalName());
        if (!wakeLock.isHeld()) {
            wakeLock.acquire();
        }
        mDownloadRunable = new DownloadRunable(service, mSpiJni, this);
        watcherThread = new Thread(new Watcher());
        sendThread = new Thread(new Sender());
        sleepRunnable = new SleepRunnable();
        mDownloadRunable.start();
    }

    //获得打印机正常工作后MCU的信息，如果打印机未工作（无打印机或准备失败）则获取为空
    //取消同步获取机制，防止调用阻塞
    public String getInitStatus(int info) {
        if (printerState.initStatus != 3) {
            return "";
        }
        switch (info) {
            case P_NO:
                return printerState.NO_;
            case P_FACORYNAME:
                return printerState.fatoryName;
            case P_NAME:
                return printerState.name;
            case P_VERSION:
                return printerState.version;
            default:
                return "";
        }
    }

    //获得实时的打印机设置状态(V1机器通讯逻辑未改无法获取实时）
    public int getRealStatus(int info) {
        if (printerState.initStatus != 3)
            return -1;
        switch (info) {
            case R_PRINTINGLENGTH:
                return printerState.printingLength;
            default:
                return -1;
        }
    }

    //V1未实现更新打印状态
    public int updatePrinterState(){
        return -1;
    }

    //打印机状态反馈 1正常 2准备中 3通信异常 4缺纸 5过热 505:无打印机 507:更新失败
    public int getPrinterState() {
        if (printerState.initStatus == 0) {
            return 2;
        }else if(printerState.initStatus == 1){
            return 505;
        }else if(printerState.initStatus == 2){
            return 507;
        }

        if (printerState.mess == 1) {
            return 3;
        }
        if (printerState.hot == 1) {
            return 5;
        }
        if (printerState.paper == 1) {
            return 4;
        }
        return 1;
    }

    @Override
    public void hasPrinter(boolean isExist) {
        if (!isExist) {
            printerState.initStatus = 1;
            handler.sendMessage(handler.obtainMessage(6));
        }
    }

    @Override
    public void startUpdate() {
        handler.sendMessage(handler.obtainMessage(4));
    }

    @Override
    public void updateStatus(boolean result) {
        if(result){
            handler.sendMessage(handler.obtainMessage(2));
        }else{
            printerState.initStatus = 2;
            handler.sendMessage(handler.obtainMessage(3));
        }
    }

    @Override
    public void startPrinter() {
        for (int i = 0; i < 5; i++) {
            mSpiJni.readblock1();
            mSpiJni.readblock2();
        }
        send__(getCommandbyte(null));
        send__(INVALIDPACKAGE);
        printerState.init(recv);
        watcherThread.start();
        sendThread.start();
    }

    //需要加锁保证税控数据可以并发
    @Override
    public synchronized void sendDataCell(DataCell p) {
        if(printerState.initStatus == 1 || printerState.initStatus == 2){
            return;
        }
        addData(p);
    }

    @Override
    public void openBox() {

    }

    public void close() {
        printerState.initStatus = 0;
        mSpiJni.close();
    }

    private void checkStatus(){
        if (printerState.paper_ != printerState.paper || printerState.paper_ == 0) {
            printerState.paper_ = printerState.paper;
        } else if (printerState.hot_ != printerState.hot || printerState.hot_ == 0) {
            printerState.hot_ = printerState.hot;
        } else if (printerState.mess_ != printerState.mess || printerState.mess_ == 0) {
            printerState.mess_ = printerState.mess;
        } else{
            return;
        }

        handler.sendMessage(handler.obtainMessage(7, getPrinterState()));
    }

    private class SleepRunnable implements Runnable {
        @Override
        public void run() {
            sleep();
        }
    }

    private class Watcher implements Runnable {

        @Override
        public void run() {
            while (printerState.initStatus == 3) {
                if (!isSleeping()) {
                    synchronized (printerState) {
                        interrupt = true;
                    }
                    send__(getCommandbyte(null));
                    send__(INVALIDPACKAGE);
                    checkStatus();
                    byte flag = (byte) 0xff;
                    if (printerState.mess != 1) {
                        if (printerState.paper == 1) // 缺纸
                        {
                            flag &= 0xFE;
                        }
                        if (printerState.hot == 1) // 过热
                        {
                            flag &= 0xFD;
                        }
                    } else {
                        flag = 0;
                    }
                    if (flag == (byte) 0xff) {
                        synchronized (printerState) {
                            interrupt = false;
                            printerState.notify();
                        }
                    }
                    isSleep = false;
                    handler.postDelayed(sleepRunnable, 5000);
                }
                mSpiJni.readblock2();
            }
        }
    }

    private class Sender implements Runnable {

        @Override
        public void run() {
            while (printerState.initStatus == 3) {
                DataCell p;
                synchronized (dataList) {
                    p = dataList.peek();
                }
                if (p == null) {
                    synchronized (lock) {
                        try {
                            handler.postDelayed(sleepRunnable, 5000);
                            lock.wait();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                } else {
                    resume();
                    int index = 0;
                    while (index < p.data.length && p.type == DataCell.PRINT_DATA) {
                        send[0] = 3;
                        if (p.data.length - index > 255) {
                            System.arraycopy(p.data, index, send, 1, 255);
                            index += 255;
                        } else {
                            System.arraycopy(p.data, index, send, 1, p.data.length - index);
                            for (int i = 1; i < 256 - (p.data.length - index); i++) {
                                send[i + p.data.length - index] = 0;
                            }
                            index += p.data.length - index;
                        }
                        while (printerState.printerBufferCount <= 1) {
                            try {
                                Thread.sleep(50);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            synchronized (printerState) {
                                if (interrupt) {
                                    try {
                                        printerState.wait();
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                }
                                send__(INVALIDPACKAGE);
                            }
                        }
                        synchronized (printerState) {
                            if (interrupt) {
                                try {
                                    printerState.wait();
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }
                            send__(send);
                            handler.removeCallbacks(sleepRunnable);
                        }
                    }

                    synchronized (dataList) {
                        dataList.remove(p);
                        memory -= p.data.length;
                        synchronized (memoryBlock) {
                            memoryBlock.notify();
                        }
                    }
                }//datalist有数据
            }
        }

    }

    //打印机睡眠逻辑
    private void sleep() {
        if (!isSleep) {
            mSpiJni.gpio_write(mSpiJni.fd_sleep, 1);
            mSpiJni.gpio_write(mSpiJni.fd_sleep, 0);
            mSpiJni.readblock1();
            isSleep = true;
            if (wakeLock.isHeld()) {
                wakeLock.release();
            }
        }
    }

    private void resume() {
        if (isSleep) {
            do {
                if (!isSleep) {
                    return;
                }
                if (isSleeping()) {
                    break;
                }
                mSpiJni.gpio_write(mSpiJni.fd_sleep, 1);
                mSpiJni.gpio_write(mSpiJni.fd_sleep, 0);
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } while (true);
            if (!wakeLock.isHeld()) {
                wakeLock.acquire();
            }
            mSpiJni.gpio_write(mSpiJni.fd_resume, 1);
            mSpiJni.gpio_write(mSpiJni.fd_resume, 0);
            do {
                if (!isSleep) {
                    return;
                }
                if (!isSleeping()) {
                    break;
                }
                mSpiJni.gpio_write(mSpiJni.fd_resume, 1);
                mSpiJni.gpio_write(mSpiJni.fd_resume, 0);
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } while (true);
            isSleep = false;
        }
    }


    //打印机睡眠状态
    private boolean isSleeping() {
        int i = mSpiJni.readbusy1();
        if (i == 0) {
            return false;
        } else {
            return true;
        }
    }

    //获得命令包
    private byte[] getCommandbyte(byte[] b) {
        byte[] dataPackage = new byte[256];
        dataPackage[0] = 1;
        Arrays.fill(dataPackage, 1, dataPackage.length, (byte) 0);
        if (b != null)
            System.arraycopy(b, 0, dataPackage, 1, b.length);
        return dataPackage;
    }

    private void addData(DataCell p) {
        if (p.data != null) {
            while (p.type == DataCell.PRINT_DATA && p.data.length < C.PM_MAXMEMORY && p.data.length + memory >= C.PM_MAXMEMORY) {
                try {
                    synchronized (memoryBlock) {
                        memoryBlock.wait();
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            synchronized (dataList) {
                dataList.offer(p);
                memory += p.data.length;
            }

            synchronized (lock) {
                lock.notify();
            }
        }
    }

    private void send__(byte[] d) {
        mSpiJni.readblock1();
        mSpiJni.spi_transfer(mSpiJni.fd_spi, 256, d, recv);
        printerState.mess = (byte) ((recv[6] & 0x12) != 0x12 || (recv[5] & 0x12) != 0x12 ? 1 : 2);
        if (printerState.mess != 1) {
            printerState.printerBufferCount = recv[1];
            printerState.hot = (byte) ((recv[6] & 0x40) > 0 ? 1 : 2);
            printerState.paper = (byte) ((recv[5] & 0x20) > 0 ? 1 : 2);
            printerState.printingLength = (((recv[19] & 0xFF) << 24) | ((recv[18] & 0xFF) << 16) | ((recv[17] & 0xFF) << 8) | (recv[16] & 0xFF));
        }
    }
}
