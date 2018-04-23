package com.sunmi.printerservice.kernel;


import android.content.Context;
import android.os.Handler;
import android.os.PowerManager;
import android.os.RemoteException;

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

public class PrinterManager implements DownloadInterface,RawPrintInterface {
    private PrinterState printerState;
    private SpiJni mSpiJni;
    private Thread mDownloadRunable;
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
        mDownloadRunable = new DownloadRunable(service, mSpiJni, this);
        watcherThread = new Thread(new Watcher());
        sendThread = new Thread(new Sender());
        sleepRunnable = new DeepSleepRunnable();
        mDownloadRunable.start();
    }

    //获得打印机正常工作后MCU的信息，如果打印机未工作（无打印机或准备失败）则获取为空
    public String getInitStatus(int info){
        if(printerState.initStatus != 3){
            return "";
        }
        switch (info){
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

    //获得打印机实时信息，返回-1失败
    //若要获取精确地实时反馈状态需要updatePrinterState
    public int getRealStatus(int info){
        if(printerState.initStatus != 3)
            return -1;
        switch (info){
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
        if(printerState.sleep == PrinterState.SleepState.deep_sleeping){
            synchronized (printerState) {
                if (!wakeLock.isHeld()) {
                    wakeLock.acquire();
                }

                while (printerState.sleep == PrinterState.SleepState.deep_sleeping) {
                    mSpiJni.gpio_write(mSpiJni.fd_resume, 1);
                    mSpiJni.gpio_write(mSpiJni.fd_resume, 0);
                    try {
                        printerState.wait(50);
                    }
                    catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
            addData(new DataCell(new byte[]{0x00}, DataCell.QUERY_DATA));
        }
        return getPrinterState();
    }

    //打印机状态反馈 1正常 2准备中 3通信异常 4缺纸 5过热 505:无打印机 507：更新失败
    public int getPrinterState(){
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
        if(!isExist){
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
        if(result){
            mHandler.sendMessage(mHandler.obtainMessage(2));
        }else{
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
        send__(getCommandbyte(null), false);
        send__(getCheckPackage(), false);
        printerState.init(recv);
        watcherThread.start();
        sendThread.start();
    }

    @Override
    public void sendDataCell(DataCell p) {
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

    //深睡眠动作
    private class DeepSleepRunnable implements Runnable {
        @Override
        public void run() {
            synchronized (printerState) {
                if (printerState.sleep == PrinterState.SleepState.waking) {
                    send__(getCommandbyte(new byte[]{0x10, 0x04, 0x01}), true);
                    printerState.sleep = PrinterState.SleepState.waiting_sleep;
                }
            }
        }
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
        //这里获取的打印机状态是已经刷新过得
        mHandler.sendMessage(mHandler.obtainMessage(7, getPrinterState()));
    }

    private class Watcher implements Runnable {

        @Override
        public void run() {
            while (printerState.initStatus == 3) {
                waitStateInterrupt();
                synchronized (printerState) {
                    if (!isSleeping()) {
                        send__(getCheckPackage(), false);
                        checkStatus();
                        if (checkPrinterState()) {
                            sleep();
                        } else {
                            printerState.sleep = PrinterState.SleepState.waking;
                            try {
                                Thread.sleep(200);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }//当唤醒以后延迟200ms通知发送线程，因为唤醒之后MCU需要校准，此时马上发数据会造成数据丢失
                        }
                    } else {
                        printerState.sleep = PrinterState.SleepState.deep_sleeping;
                    }
                    printerState.notify();
                }
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
                    if(printerState.noSleep){
                        printerState.noSleep = false;
                    }

                    if(p == null){
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
                }
                else
                {
                    if (p.series == 1) {
                        synchronized (printerState) {
                            send__(C.INVALIDPACKAGE, true);
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
                    else if (p.series == 2) {
                        if(printerState.current_status == PROCESS_TRANS){
                            if (printerState.printerBufferCount < 9) {
                                try {
                                    Thread.sleep(50);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                                synchronized (printerState) {
                                    if(printerState.current_status < INTERRUPT_TRANS)
                                        send__(C.INVALIDPACKAGE, true);
                                }
                            }//阻塞直到打印机缓存buffer全空
                            else{
                                try {
                                    if(p.callback != null){
                                        p.callback.onPrintResult(0, "Transaction print successful!");
                                    }
                                } catch (RemoteException e) {
                                    // TODO Auto-generated catch block
                                    e.printStackTrace();
                                }//无异常结束
                                printerState.current_status = NO_TRANS;
                                synchronized (dataList) {
                                    dataList.remove(p);
                                    memory -= p.data.length;
                                    synchronized (memoryBlock) {
                                        memoryBlock.notify();
                                    }
                                }
                            }
                        }else if (printerState.current_status == INTERRUPT_TRANS) {
                            // 反馈失败结果
                            printerState.current_status = FINISH_TRANS;
                            addData(new DataCell(new byte[] { 0x10, 0x14, 0x08, 0x01, 0x03, 0x14, 0x01, 0x06, 0x02, 0x08 }, DataCell.COMMAND_DATA));
                        }//if(current_status == 1)  首次获得异常状态，将异常状态上报并改为处理异常状态，同时发送清打印机缓存命令;
                        else{
                            if(p.callback != null){
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

                    if (p.type == DataCell.PRINT_DATA && printerState.printerBufferCount <= 1 && printerState.current_status < INTERRUPT_TRANS) {
                        try {
                            Thread.sleep(50);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        synchronized (printerState) {
                            if(printerState.current_status < INTERRUPT_TRANS){
                                send__(C.INVALIDPACKAGE, true);
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
                            if (p.type == DataCell.TAX_DATA || p.type == DataCell.COMMAND_DATA ) {
                                if (p.type == DataCell.TAX_DATA) {
                                    printerState.tax = p.tax;
                                    addData(new DataCell(C.INVALIDPACKAGE, DataCell.INVALID_DATA));
                                }
                                send[0] = 1;
                                offse = 1;
                            } else if(p.type ==  DataCell.INVALID_DATA) {
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
                            if(p.type != DataCell.PRINT_DATA || printerState.current_status < INTERRUPT_TRANS){
                                send__(send, true);
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

    //打印机复位
    private void reset() {
        if (mSpiJni != null) {
            mSpiJni.gpio_write(mSpiJni.fd_rsest, 1);
        }
    }

    //打印机睡眠逻辑
    private void sleep() {
        if (printerState.sleep == PrinterState.SleepState.waiting_sleep || printerState.sleep == PrinterState.SleepState.waking) {
            send__(getCommandbyte(new byte[] { 0x1F, 0x1B, 0x1F, 0x01, 0x73, 0x02, 0x6C, 0x03, 0x65, 0x04,
                    0x65, 0x05, 0x70}), false);     //发送睡眠包
            send__(C.INVALIDPACKAGE, false);
            if (printerState.sleep_info == 0) {
                printerState.sleep = PrinterState.SleepState.deep_sleeping;
            } else {
                printerState.sleep = PrinterState.SleepState.light_sleeping;
            }

            waitStateInterrupt();
            if (wakeLock.isHeld()) {
                wakeLock.release();
            }
        }else if(printerState.sleep == PrinterState.SleepState.deep_sleeping){
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
                        }
                        if (printerState.sleep == PrinterState.SleepState.deep_exceptional) {
                            printerState.noSleep = true;
                            printerState.sleep = PrinterState.SleepState.waking;
                            printerState.notify();
                        }//如果是深睡眠异常状态，则强制唤醒！
                    }
                }
                dataList.offer(p);
                //Log.v("gh1", "renwuruduilie");
                memory += p.data.length;
                dataList.notify();
            }
        }
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

    private void send__(byte[] d, boolean needWakeUp) {
        if (needWakeUp) {
            wakeUp();
        }
        waitDataInterrupt();
        mSpiJni.spi_transfer(mSpiJni.fd_spi, 256, d, recv);

        printerState.mess = (byte) ((recv[6] & 0x12) != 0x12 || (recv[5] & 0x12) != 0x12 ? 1 : 2);
        if (printerState.mess != 1) {
            printerState.printerBufferCount = recv[1];
            printerState.sleep_info = recv[8];
            printerState.hot = (byte) ((recv[6] & 0x40) > 0 ? 1 : 2);
            printerState.paper = (byte) ((recv[5] & 0x20) > 0 ? 1 : 2);

            if (printerState.paper == 1 || printerState.hot == 1) {
                if (printerState.current_status == PROCESS_TRANS) {
                    printerState.current_status = INTERRUPT_TRANS;
                }
            }

            printerState.printerFactorname = recv[9];
            printerState.printingLength = (((recv[19] & 0xFF) << 24) | ((recv[18] & 0xFF) << 16) | ((recv[17] & 0xFF) << 8) | (recv[16] & 0xFF));
            printerState.extraDataReadCount = (((recv[155] & 0xFF) << 8) | (recv[154] & 0xFF));
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
}
