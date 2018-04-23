package com.sunmi.printerservice.kernel;


import android.content.Context;
import android.os.Handler;
import android.os.PowerManager;
import android.os.RemoteException;
import android.support.annotation.IntDef;

import com.longcheer.spijni.SpiJni;
import com.sunmi.printerservice.cell.DataCell;
import com.sunmi.printerservice.commandbean.SeriesTask;
import com.sunmi.printerservice.manager.PrinterState;
import com.sunmi.printerservice.render.RawPrintInterface;
import com.sunmi.printerservice.thread.DownloadRunable;
import com.sunmi.printerservice.thread.DownloadInterface;
import com.sunmi.printerservice.utils.C;
import com.sunmi.printerservice.utils.LogUtils;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Arrays;
import java.util.Comparator;
import java.util.PriorityQueue;

import static com.sunmi.printerservice.manager.PrinterState.FINISH_TRANS;
import static com.sunmi.printerservice.manager.PrinterState.INTERRUPT_TRANS;
import static com.sunmi.printerservice.manager.PrinterState.NO_TRANS;
import static com.sunmi.printerservice.manager.PrinterState.PROCESS_TRANS;
import static com.sunmi.printerservice.manager.PrinterState.P_FACORYNAME;
import static com.sunmi.printerservice.manager.PrinterState.P_NAME;
import static com.sunmi.printerservice.manager.PrinterState.P_NO;
import static com.sunmi.printerservice.manager.PrinterState.P_VERSION;
import static com.sunmi.printerservice.manager.PrinterState.R_PRINTERFACTORNAME;
import static com.sunmi.printerservice.manager.PrinterState.R_PRINTINGLENGTH;
import static com.sunmi.printerservice.manager.PrinterState.SUCCESS_TRANS;
import static com.sunmi.printerservice.manager.PrinterState.WAITING_TRANS;

public class PrinterManager implements DownloadInterface, RawPrintInterface {
    private static final int NOWAKEUP = 0;
    private static final int WAKEUP = 1;
    private static final int FORCEWAKEUP = 2;

    private PrinterState printerState;
    private SpiJni mSpiJni;
    private Thread sendThread;
    private Thread watcherThread;
    private Handler mHandler;
    private PowerManager.WakeLock wakeLock;
    private DeepSleepRunnable sleepRunnable;

    private byte[] send = new byte[256];
    private byte[] recv = new byte[256];
    private int memory = 0;
    private Object memoryBlock = new Object();
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
        mHandler = handler;
        printerState = new PrinterState();
        mSpiJni = new SpiJni();
        PowerManager pm = (PowerManager) service.getSystemService(Context.POWER_SERVICE);
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK | PowerManager.ON_AFTER_RELEASE, getClass().getCanonicalName());
        if (!wakeLock.isHeld()) {
            wakeLock.acquire();
        }
        Thread mDownloadRunable = new DownloadRunable(service, mSpiJni, this);
        watcherThread = new Thread(new Watcher());
        sendThread = new Thread(new Sender());
        sleepRunnable = new DeepSleepRunnable();
        mDownloadRunable.start();
    }

    //获得打印机正常工作后MCU的信息，如果打印机未工作（无打印机或准备失败）则获取为空
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

    //获得打印机实时信息，如果打印机未工作则返回-1
    //若要获取精确地实时反馈状态需要updatePrinterState
    public int getRealStatus(int info) {
        if (printerState.initStatus != 3)
            return -1;
        switch (info) {
            case R_PRINTINGLENGTH:
                return printerState.printingLength;
            case R_PRINTERFACTORNAME:
                return printerState.printerFactorname;
            default:
                return -1;
        }
    }

    //获得打印机状态信息会刷新打印机状态
    public synchronized int updatePrinterState() {
        if (printerState.sleep == PrinterState.SleepState.deep_sleeping) {
            synchronized (printerState) {
                if (!wakeLock.isHeld()) {
                    wakeLock.acquire();
                }
                while (printerState.sleep == PrinterState.SleepState.deep_sleeping) {
                    mSpiJni.gpio_write(mSpiJni.fd_resume, 1);
                    mSpiJni.gpio_write(mSpiJni.fd_resume, 0);
                    try {
                        printerState.wait(50);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
            addData(new DataCell(new byte[]{0x00}, DataCell.QUERY_DATA));
        }
        return getPrinterState();
    }

    //打印机状态反馈 1正常 2停止工作 3通信异常 4缺纸 5过热 505:无打印机 507:更新失败
    public int getPrinterState() {
        if (printerState.initStatus == 0) {
            return 2;
        } else if (printerState.initStatus == 1) {
            return 505;
        } else if (printerState.initStatus == 2) {
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
            mHandler.sendMessage(mHandler.obtainMessage(6));
        }
    }

    @Override
    public void startUpdate() {
        mHandler.sendMessage(mHandler.obtainMessage(4));
    }

    @Override
    public void updateStatus(boolean result) {
        if (result) {
            mHandler.sendMessage(mHandler.obtainMessage(2));
        } else {
            printerState.initStatus = 2;
            mHandler.sendMessage(mHandler.obtainMessage(3));
        }
    }

    @Override
    public void startPrinter() {
        for (int i = 0; i < 5; i++) {
            waitStateInterrupt();
            waitDataInterrupt();
        }
        send__(getCommandbyte(null), NOWAKEUP);
        send__(getCheckPackage(), NOWAKEUP);
        printerState.init(recv);
        watcherThread.start();
        sendThread.start();
    }

    @Override
    public void sendDataCell(DataCell p) {
        if (printerState.initStatus == 1 || printerState.initStatus == 2) {
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

    //打印机复位
    private void reset() {
        if (mSpiJni != null) {
            mSpiJni.gpio_write(mSpiJni.fd_power, 0);

            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            mSpiJni.gpio_write(mSpiJni.fd_power, 1);
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            for(int i = 0; i < 2; i++){
                waitStateInterrupt();
                waitDataInterrupt();
            }
            waitDataInterrupt();
            mSpiJni.spi_transfer(mSpiJni.fd_spi, 256, getCommandbyte(null), recv);
            waitDataInterrupt();
            mSpiJni.spi_transfer(mSpiJni.fd_spi, 256, getCheckPackage(), recv);
            printerState.reset();
        }
    }

    //深睡眠动作
    private class DeepSleepRunnable implements Runnable {
        @Override
        public void run() {
            synchronized (printerState) {
                if (printerState.sleep == PrinterState.SleepState.waking) {
                    send__(getCommandbyte(C.WAITING_BUFFER), NOWAKEUP);
                    printerState.sleep = PrinterState.SleepState.waiting_sleep;
                }
            }
        }
    }

    //获得中断后检查状态更新，若有更新则报告
    private void checkStatus() {
        if (printerState.paper_ != printerState.paper || printerState.paper_ == 0) {
            printerState.paper_ = printerState.paper;
        } else if (printerState.hot_ != printerState.hot || printerState.hot_ == 0) {
            printerState.hot_ = printerState.hot;
        } else if (printerState.mess_ != printerState.mess || printerState.mess_ == 0) {
            printerState.mess_ = printerState.mess;
        } else {
            return;
        }
        mHandler.sendMessage(mHandler.obtainMessage(7, getPrinterState()));
    }

    private class Watcher implements Runnable {

        @Override
        public void run() {
            while (printerState.initStatus == 3) {
                waitStateInterrupt();
                synchronized (printerState) {
                    if (!isSleeping()) {
                        send__(getCheckPackage(), NOWAKEUP);
                        checkStatus();
                        if (checkPrinterState()) {
                            sleep();
                        } else {
                            printerState.sleep = PrinterState.SleepState.waking;
                            try {
                                Thread.sleep(200);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }//当唤醒以后延迟500ms通知发送线程，因为唤醒之后MCU需要校准，此时马上发数据会造成数据丢失
                        }
                    } else {
                        printerState.sleep = PrinterState.SleepState.deep_sleeping;
                    }
                    printerState.notify();
                }
                LogUtils.d("current status:" + printerState.sleep);
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
                if (p == null || (p.type == DataCell.PRINT_DATA && printerState.noSleep)) {
                    //在准备睡眠前要清除掉不睡标记，否则无法进入深睡眠
                    //如果是普通数据此时被标记了noSleep那么要去掉标记
                    if (printerState.noSleep) {
                        printerState.noSleep = false;
                    }
                    if (p == null) {
                        //当没有数据时，目前将直接睡眠改为延迟睡眠防止短时间内频繁唤醒睡眠
                        //对于事务打印过程中的异常，由于clearbuffer后已经触发中断转为深睡眠，故不用再睡眠
                        mHandler.postDelayed(sleepRunnable, 5000);
                        synchronized (dataList) {
                            try {
                                if (dataList.isEmpty()) {
                                    dataList.wait();
                                }
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                } else {
                    if (p.series == SeriesTask.HEAD) {
                        synchronized (printerState) {
                            send__(C.INVALIDPACKAGE, WAKEUP);
                        }
                        printerState.current_status = PROCESS_TRANS;
                        synchronized (dataList) {
                            dataList.remove(p);
                            memory -= p.data.length;
                            synchronized (memoryBlock) {
                                memoryBlock.notify();
                            }
                        }
                        continue;
                    }//if(p.series == 1)  处理头task，初始化缓冲状态
                    else if (p.series == SeriesTask.TAIL) {
                        /* ===========================================================================================
                      * 数据包当执行到事务尾包时：
                      * 1、状态还在事务发送中将转为MCU处理中
                      * 2、状态在MCU处理中时将延迟50ms发送一次无效包来更新状态
                      * 3、状态变成MCU处理结束时表明打印真正结束，反馈消息并转为非事务模式
                      * 4、状态变成异常状态时会发送清楚MCU缓存命令，命令包优先于数据包:事务尾，此时异常已进入睡眠将会强制唤醒MCU;
                      * 5、清除缓存命令发送后，数据包再次执行事务尾，此时状态为结束状态，会反馈给用户；
                      *  ==========================================================================================*/
                        if (printerState.current_status == PROCESS_TRANS) {
                            printerState.current_status = WAITING_TRANS;
                        } else if (printerState.current_status == WAITING_TRANS) {
                            try {
                                Thread.sleep(50);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            synchronized (printerState) {
                                if (printerState.current_status < INTERRUPT_TRANS) {
                                    send__(C.INVALIDPACKAGE, WAKEUP);
                                }
                            }
                        } else if (printerState.current_status == SUCCESS_TRANS) {
                            printerState.current_status = NO_TRANS;
                            try {
                                if (p.callback != null) {
                                    p.callback.onPrintResult(0, "Transaction print successful!");
                                }
                            } catch (RemoteException e) {
                                // TODO Auto-generated catch block
                                e.printStackTrace();
                            }//无异常结束
                            synchronized (dataList) {
                                dataList.remove(p);
                                memory -= p.data.length;
                                synchronized (memoryBlock) {
                                    memoryBlock.notify();
                                }
                            }
                        } else if (printerState.current_status == INTERRUPT_TRANS) {
                            printerState.current_status = FINISH_TRANS;
                            synchronized (printerState){
                                send__(getCommandbyte(C.CLEAR_BUFFER), FORCEWAKEUP);
                            }
                        } else {
                            if (p.callback != null) {
                                try {
                                    p.callback.onPrintResult(1, "Transaction print failed!");
                                } catch (RemoteException e) {
                                    // TODO Auto-generated catch block
                                    e.printStackTrace();
                                }
                            }
                            printerState.current_status = NO_TRANS;
                            synchronized (dataList) {
                                dataList.remove(p);
                                memory -= p.data.length;
                                synchronized (memoryBlock) {
                                    memoryBlock.notify();
                                }
                            }
                        }
                        continue;
                    }//else if(p.series == 2)  处理尾task
                    else if (printerState.current_status == INTERRUPT_TRANS) {
                            printerState.current_status = FINISH_TRANS;
                        synchronized (printerState){
                            send__(getCommandbyte(C.CLEAR_BUFFER), FORCEWAKEUP);
                        }
                    }//处理异常

                    if (p.type == DataCell.PRINT_DATA && printerState.printerBufferCount <= 1 && printerState.current_status < INTERRUPT_TRANS) {
                        try {
                            Thread.sleep(50);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        synchronized (printerState) {
                            if (printerState.current_status < INTERRUPT_TRANS) {
                                send__(C.INVALIDPACKAGE, WAKEUP);
                            }
                        }
                        continue;
                    }//处理打印数据时需要等待打印机缓冲区空间 current_status为-1时无条件等但为0时可能会改变，故不需要等以防卡数据

                    if ((p.type == DataCell.QUERY_DATA) || (p.type == DataCell.PRINT_DATA && printerState.current_status > PROCESS_TRANS)) {
                        synchronized (dataList) {
                            dataList.remove(p);
                            memory -= p.data.length;
                            synchronized (memoryBlock) {
                                memoryBlock.notify();
                            }
                        }
                    }//异常状态时，头尾task间还没发送的数据全部丢到  补充查询包也丢掉，不用查询
                    else {
                        if (p.offset < p.data.length) {
                            int offse;
                            if (p.type == DataCell.TAX_DATA) {
                                if (printerState.run_info == 0x34) {
                                    try {
                                        Thread.sleep(10);
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                    addData(new DataCell(C.INVALIDPACKAGE, DataCell.INVALID_DATA));
                                    continue;
                                } else {
                                    printerState.tax = p.tax;
                                    addData(new DataCell(C.INVALIDPACKAGE, DataCell.INVALID_DATA));
                                    send[0] = 1;
                                    offse = 1;
                                }
                            } else if (p.type == DataCell.COMMAND_DATA) {
                                send[0] = 1;
                                offse = 1;
                            } else if (p.type == DataCell.INVALID_DATA) {
                                offse = 0;
                            } else {
                                send[0] = 3;
                                if (p.offset == 0) {
                                    send[1] = p.id[0];
                                    send[2] = p.id[1];
                                    offse = 3;
                                } else {
                                    offse = 1;
                                }
                            }
                            if (p.data.length - p.offset > 256 - offse) {
                                System.arraycopy(p.data, p.offset, send, offse, 256 - offse);
                                p.offset += (256 - offse);
                            } else {
                                System.arraycopy(p.data, p.offset, send, offse, p.data.length - p.offset);
                                for (int i = offse; i < 256 - (p.data.length - p.offset); i++) {
                                    send[i + p.data.length - p.offset] = 0;
                                }
                                p.offset += p.data.length - p.offset;
                            }
                        }//if (p.offset < p.data.length)  对可发送数据的处理
                        synchronized (printerState) {
                            if(p.type != DataCell.PRINT_DATA){
                                send__(send, FORCEWAKEUP);
                            }//对于非数据包的数据
                            else if (printerState.current_status < INTERRUPT_TRANS) {
                                send__(send, WAKEUP);
                            }//真正发送时可能会遇到异常状态，这个时候要跳过发送数据
                        }

                        if (p.offset == p.data.length) {
                            synchronized (dataList) {
                                dataList.remove(p);
                                memory -= p.data.length;
                                synchronized (memoryBlock) {
                                    memoryBlock.notify();
                                }
                            }
                        }
                    }
                }//datalist有数据
            }
        }
    }

    //打印机睡眠逻辑
    private void sleep() {
        if (printerState.sleep == PrinterState.SleepState.waiting_sleep || printerState.sleep == PrinterState.SleepState.waking) {
            send__(getCommandbyte(C.SLEEP_BUFFER), NOWAKEUP);
            send__(C.INVALIDPACKAGE, NOWAKEUP);
            if (printerState.sleep_info == 0) {
                printerState.sleep = PrinterState.SleepState.deep_sleeping;
            } else {
                printerState.sleep = PrinterState.SleepState.light_sleeping;
            }

            waitStateInterrupt();
            if (wakeLock.isHeld()) {
                wakeLock.release();
            }
        } else if (printerState.sleep == PrinterState.SleepState.deep_sleeping) {
            printerState.sleep = PrinterState.SleepState.deep_exceptional;
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

    private boolean checkPrinterState() {
        if (printerState.mess != 1) {
            if (printerState.noSleep) {
                return false;
            }
            if (printerState.paper == 1) {
                return true;
            }
            if (printerState.hot == 1) {
                return true;
            }
            if (printerState.sleep == PrinterState.SleepState.waiting_sleep && printerState.printerBufferCount == 9) {
                return true;
            }
        } else {
            return true;
        }
        return false;
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

    //获得检查包
    private byte[] getCheckPackage() {
        byte[] dataPackage = new byte[256];
        dataPackage[0] = 16;
        Arrays.fill(dataPackage, 1, dataPackage.length, (byte) 0);
        return dataPackage;
    }


    //状态中断
    private void waitStateInterrupt() {
        mSpiJni.readblock2();
    }

    //数据中断
    private void waitDataInterrupt() {
        mSpiJni.readblock1();
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

            //有新数据来将移除即将到来的深睡眠
            mHandler.removeCallbacks(sleepRunnable);
            synchronized (dataList) {
                if (p.type == DataCell.COMMAND_DATA || p.type == DataCell.TAX_DATA) {
                    synchronized (printerState) {
                        forceWakeUp();
                    }
                }
                dataList.offer(p);
                memory += p.data.length;
                dataList.notify();
            }
        }
    }

    private void forceWakeUp() {
        if (printerState.sleep != PrinterState.SleepState.waking && printerState.sleep != PrinterState.SleepState.deep_exceptional) {
            if (!wakeLock.isHeld()) {
                wakeLock.acquire();
            }
            printerState.noSleep = true;
            while (printerState.sleep != PrinterState.SleepState.waking) {
                mSpiJni.gpio_write(mSpiJni.fd_resume, 1);
                mSpiJni.gpio_write(mSpiJni.fd_resume, 0);
                try {
                    printerState.wait(50);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        } else if (printerState.sleep == PrinterState.SleepState.deep_exceptional) {
            printerState.noSleep = true;
            printerState.sleep = PrinterState.SleepState.waking;
            printerState.notify();
        }//如果是深睡眠异常状态，则强制唤醒！
    }

    private void wakeUp() {
        if (!wakeLock.isHeld()) {
            wakeLock.acquire();
        }
        while (printerState.sleep != PrinterState.SleepState.waking) {
            if (printerState.sleep == PrinterState.SleepState.deep_sleeping) {
                mSpiJni.gpio_write(mSpiJni.fd_resume, 1);
                mSpiJni.gpio_write(mSpiJni.fd_resume, 0);
                try {
                    printerState.wait(50);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } else if (printerState.sleep == PrinterState.SleepState.deep_exceptional || printerState.sleep == PrinterState.SleepState.waiting_sleep || printerState.sleep == PrinterState.SleepState.light_sleeping) {
                try {
                    printerState.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void send__(byte[] d, @WakeType int needWakeUp) {
        if (needWakeUp == WAKEUP) {
            wakeUp();
        }else if(needWakeUp == FORCEWAKEUP){
            forceWakeUp();
        }
        waitDataInterrupt();
        mSpiJni.spi_transfer(mSpiJni.fd_spi, 256, d, recv);

        printerState.mess = (byte) ((recv[6] & 0x12) != 0x12 || (recv[5] & 0x12) != 0x12 ? 1 : 2);
        if (printerState.mess != 1) {
            printerState.printerBufferCount = recv[1];
            printerState.sleep_info = recv[8];
            printerState.run_info = recv[14];
            printerState.hot = (byte) ((recv[6] & 0x40) > 0 ? 1 : 2);
            printerState.paper = (byte) ((recv[5] & 0x20) > 0 ? 1 : 2);

            /****************************************************************
             * 在事务模式中如果出现异常将会进入事务中断状态
             * 在事务模式中如果未出现异常但数据在系统端发送完后需要：
             * 将判断[1]MCU数据缓存是否清空 9：清空了 <9：未清空
             * 将判断[14]MCU是否正在打印 0x12:停止打印 0x34:正在打印 0x00:默认
             * 如果MCU缓存请空且停止打印，将会进入事务完成状态
             * **************************************************************/
            if (printerState.paper == 1 || printerState.hot == 1) {
                if (printerState.current_status == PROCESS_TRANS || printerState.current_status == WAITING_TRANS) {
                    printerState.current_status = INTERRUPT_TRANS;
                }
            } else if (printerState.current_status == WAITING_TRANS) {
                if (recv[1] == 9 && recv[14] == 0x12) {
                    printerState.current_status = SUCCESS_TRANS;
                }
            }

            printerState.printingLength = (((recv[19] & 0xFF) << 24) | ((recv[18] & 0xFF) << 16) | ((recv[17] & 0xFF) << 8) | (recv[16] & 0xFF));
            printerState.extraDataReadCount = (((recv[155] & 0xFF) << 8) | (recv[154] & 0xFF));
            if ((recv[6] & 0x20) > 0) {
                reset();
                return;
            }
            if (printerState.extraDataReadCount != 0) {
                if (printerState.extraData == null) {
                    printerState.extraData = new byte[printerState.extraDataReadCount];
                    for (int i = 0; i < printerState.extraDataReadCount / 100; i++) {
                        addData(new DataCell(C.INVALIDPACKAGE, DataCell.INVALID_DATA));
                    }
                }
                int l = Math.min(100, printerState.extraDataReadCount);
                System.arraycopy(recv, 156, printerState.extraData, printerState.extraDataReadOffset, l);
                printerState.extraDataReadOffset += l;
                if (printerState.extraData.length == printerState.extraDataReadOffset) {
                    if (printerState.tax != null) {
                        try {
                            printerState.tax.onDataResult(printerState.extraData);
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }
                    }
                    printerState.extraData = null;
                    printerState.tax = null;
                    printerState.extraDataReadOffset = 0;
                }
            }
        }
    }

    @IntDef({NOWAKEUP, WAKEUP, FORCEWAKEUP})
    @Retention(RetentionPolicy.SOURCE)
    private  @interface WakeType {

    }
}
