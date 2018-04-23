package com.sunmi.printerservice.kernel;


import android.content.Context;
import android.os.Handler;
import android.os.RemoteException;

import com.longcheer.spijni.SpiJni;
import com.sunmi.printerservice.cell.DataCell;
import com.sunmi.printerservice.commandbean.SeriesTask;
import com.sunmi.printerservice.manager.PrinterState;
import com.sunmi.printerservice.render.RawPrintInterface;
import com.sunmi.printerservice.thread.DownloadRunable;
import com.sunmi.printerservice.thread.DownloadInterface;
import com.sunmi.printerservice.utils.C;

import java.util.Arrays;
import java.util.Comparator;
import java.util.PriorityQueue;

import static com.sunmi.printerservice.manager.PrinterState.INTERRUPT_TRANS;
import static com.sunmi.printerservice.manager.PrinterState.NO_TRANS;
import static com.sunmi.printerservice.manager.PrinterState.PROCESS_TRANS;
import static com.sunmi.printerservice.manager.PrinterState.P_FACORYNAME;
import static com.sunmi.printerservice.manager.PrinterState.P_NAME;
import static com.sunmi.printerservice.manager.PrinterState.P_NO;
import static com.sunmi.printerservice.manager.PrinterState.P_VERSION;
import static com.sunmi.printerservice.manager.PrinterState.R_BLACKMODE;
import static com.sunmi.printerservice.manager.PrinterState.R_BLACKVALUE;
import static com.sunmi.printerservice.manager.PrinterState.R_BOXTIMES;
import static com.sunmi.printerservice.manager.PrinterState.R_CUTTIMES;
import static com.sunmi.printerservice.manager.PrinterState.R_PRINTINGLENGTH;

public class PrinterManager implements DownloadInterface, RawPrintInterface {
    private PrinterState printerState;
    private SpiJni mSpiJni;
    private Thread mDownloadRunable;
    private Thread sendThread;
    private Thread watcherThread;
    private Handler mHandler;
    private RequestInterface requestInterface;

    public static byte[] INVALIDPACKAGE = new byte[256];

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


    public PrinterManager(Context service, Handler handler, RequestInterface requestInterface) {
        this.mHandler = handler;
        this.requestInterface = requestInterface;
        printerState = new PrinterState();
        mSpiJni = new SpiJni();
        mDownloadRunable = new DownloadRunable(service, mSpiJni, this);
        watcherThread = new Thread(new Watcher());
        sendThread = new Thread(new Sender());
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

    //获取打印机实时反馈记录信息，如果未工作将返回-1
    //实时反馈需要updatePrinterState
    public int getRealStatus(int info) {
        if (printerState.initStatus != 3)
            return -1;
        switch (info) {
            case R_PRINTINGLENGTH:
                return printerState.printingLength;
            case R_CUTTIMES:
                return printerState.cuttingCount;
            case R_BOXTIMES:
                return printerState.boxCount;
            case R_BLACKMODE:
                return printerState.bbm_flag;
            case R_BLACKVALUE:
                return printerState.bbm_value;
            default:
                return -1;
        }
    }

    //更新打印机的状态（将刷新为最新状态）
    public int updatePrinterState() {
        synchronized (printerState) {
            send__(INVALIDPACKAGE);
        }
        return getPrinterState();
    }

    //打印机状态反馈 1正常 2准备中 3通信异常 4缺纸 5过热 6开盖 7切刀异常 8切刀修复 9黑标 10未完全关盖 505:无打印机
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

        if (printerState.kinfe != 2 && printerState.kinfe != 0) {
            if (printerState.kinfe == 3) {
                return 7;
            } else {
                return 8;
            }
        }

        if (printerState.hot == 1) {
            return 5;
        }

        if (printerState.cover == 1) {
            return 6;
        } else if (printerState.cover == 3) {
            return 10;
        }
        if (printerState.paper == 1) {
            return 4;
        }

        if (printerState.black == 1) {
            return 9;
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
            mSpiJni.readblock1();
            mSpiJni.readblock2();
        }
        send__(getCommandbyte(null));
        send__(INVALIDPACKAGE);
        printerState.init(recv);
        //T1mini要主动设置一下纸张类型：
        if(printerState.name.contains("58")){
            requestInterface.getServiceValue().setPaper(384);
            requestInterface.getServiceValue().updateMyId();
        }

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
        mSpiJni.gpio_write(mSpiJni.fd_box, 1);
        try {
            Thread.sleep(150);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        mSpiJni.gpio_write(mSpiJni.fd_box, 0);
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

    private void checkStatus() {
        if (printerState.mess_ != printerState.mess || printerState.mess_ == 0) {
            printerState.mess_ = printerState.mess;
        } else if (printerState.kinfe_ != printerState.kinfe || printerState.kinfe_ == 0) {
            printerState.kinfe_ = printerState.kinfe;
        }
        if (printerState.cover_ != printerState.cover || printerState.cover_ == 0) {
            printerState.cover_ = printerState.cover;
            if (printerState.cover == 2) {
                printerState.paper_ = printerState.paper;
            }
        } else if (printerState.paper_ != printerState.paper || printerState.paper_ == 0) {
            printerState.paper_ = printerState.paper;
            if (printerState.cover == 1 && printerState.paper == 2) {
                printerState.cover = printerState.cover_ = 3;
            }
        } else if (printerState.hot_ != printerState.hot || printerState.hot_ == 0) {
            printerState.hot_ = printerState.hot;
        }

        mHandler.sendMessage(mHandler.obtainMessage(7, getPrinterState()));
    }

    private class Watcher implements Runnable {

        @Override
        public void run() {
            while (printerState.initStatus == 3) {
                synchronized (printerState) {
                    send__(getCommandbyte(null));
                    send__(INVALIDPACKAGE);
                    checkStatus();
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
                    synchronized (dataList) {
                        try {
                            if (dataList.isEmpty()) {
                                dataList.wait();
                            }
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                } else {
                    if (p.type == DataCell.PRINT_DATA && ((printerState.printerBufferCount <= 1 && printerState.current_status < INTERRUPT_TRANS) || (getPrinterState() != 1 && printerState.current_status < PROCESS_TRANS))) {
                        try {
                            Thread.sleep(50);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        synchronized (printerState) {
                            send__(INVALIDPACKAGE);
                        }
                    } else {
                        if (p.series == SeriesTask.HEAD) {
                            printerState.current_status = PROCESS_TRANS;//进入事务模式
                            //kick out!
                        }//处理事务头包
                        else if (p.series == SeriesTask.TAIL) {
                            if (printerState.current_status == PROCESS_TRANS) {
                                if (printerState.printerBufferCount < 9) {
                                    synchronized (printerState) {
                                        send__(INVALIDPACKAGE);
                                    }
                                    continue;
                                }//阻塞直到打印机缓存buffer全空
                                else {
                                    try {
                                        if (p.callback != null) {
                                            p.callback.onPrintResult(0, "Transaction print successful!");
                                        }
                                    } catch (RemoteException e) {
                                        e.printStackTrace();
                                    }//无异常结束
                                    printerState.current_status = NO_TRANS;
                                    //kick out!
                                }
                            }//处理事务尾包发送成功
                            else if (printerState.current_status == INTERRUPT_TRANS) {
                                synchronized (printerState) {
                                    send__(getCommandbyte(new byte[]{0x10, 0x14, 0x08, 0x01, 0x03, 0x14, 0x01, 0x06, 0x02, 0x08}));
                                }
                                try {
                                    if (p.callback != null) {
                                        p.callback.onPrintResult(1, "Transaction print failed!");
                                    }
                                } catch (RemoteException e) {
                                    e.printStackTrace();
                                }//无异常结束
                                printerState.current_status = NO_TRANS;
                                //kick out!
                            }//处理事务尾包发送失败，与P1区别在于只需判断一次
                        }//处理事务尾包
                        else {
                            if (p.type == DataCell.PRINT_DATA && printerState.current_status == INTERRUPT_TRANS) {
                                //kick out!!
                            } else {
                                if (p.offset < p.data.length) {
                                    int offse;
                                    if (p.type == DataCell.TAX_DATA || p.type == DataCell.COMMAND_DATA || p.type == DataCell.LCD_DATA) {
                                        if (p.type == DataCell.TAX_DATA) {
                                            printerState.tax = p.tax;
                                            addData(new DataCell(INVALIDPACKAGE, DataCell.INVALID_DATA));
                                        }else if(p.type == DataCell.LCD_DATA){
                                            printerState.lcd = p.lcd;
                                            addData(new DataCell(INVALIDPACKAGE, DataCell.INVALID_DATA));
                                        }
                                        send[0] = 1;
                                        offse = 1;
                                    } else if (p.type == DataCell.INVALID_DATA) {
                                        offse = 0;
                                    } else {
                                        send[0] = 3;
                                        offse = 1;
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
                                }
                                synchronized (printerState) {
                                    send__(send);
                                }
                                if (p.offset == p.data.length) {
                                    //kick out!
                                } else {
                                    continue;
                                }
                            }
                        }//处理非事务头尾包
                        synchronized (dataList) {
                            dataList.remove(p);
                            memory -= p.data.length;
                            synchronized (memoryBlock) {
                                memoryBlock.notify();
                            }
                        }
                    }
                }
            }
        }
    }

    //打印机复位
    private void reset() {
        if (mSpiJni != null) {
            mSpiJni.gpio_write(mSpiJni.fd_rsest, 1);
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
                dataList.notify();
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
            printerState.cover = (byte) ((recv[5] & 0x04) > 0 ? 1 : 2);
            printerState.kinfe = (byte) ((recv[6] & 0x0C) != 0 ? ((recv[6] & 0x0C) == 0x08 ? 3 : 4) : 2);
            printerState.black = (byte) ((recv[6] & 0x80) != 0 ? 1 : 2);

            if (printerState.paper == 1 || printerState.hot == 1 || printerState.cover == 1 || printerState.kinfe == 3) {
                if (printerState.current_status == PROCESS_TRANS) {
                    printerState.current_status = INTERRUPT_TRANS;
                }
            }

            printerState.printingLength = (((recv[19] & 0xFF) << 24) | ((recv[18] & 0xFF) << 16)
                    | ((recv[17] & 0xFF) << 8) | (recv[16] & 0xFF));
            printerState.cuttingCount = (((recv[23] & 0xFF) << 24) | ((recv[22] & 0xFF) << 16)
                    | ((recv[21] & 0xFF) << 8) | (recv[20] & 0xFF));

            try {
                if(printerState.lcd != null){
                    if(recv[135] == 0x10 && recv[136] == 0x03){
                        printerState.lcd.onRunResult(true);
                        printerState.lcd = null;
                    }else if(recv[135] == 0x10 && (recv[136] == 0x23 || recv[136] == 0x26)){
                        printerState.lcd.onRunResult(false);
                        printerState.lcd = null;
                    }
                }
            } catch (RemoteException e) {
                e.printStackTrace();}

            printerState.extraDataReadCount = (((recv[155] & 0xFF) << 8) | (recv[154] & 0xFF));
            if (printerState.extraDataReadCount != 0) {
                if (printerState.extraData == null) {
                    printerState.extraData = new byte[printerState.extraDataReadCount];
                    for (int i = 0; i < printerState.extraDataReadCount / 100; i++) {
                        addData(new DataCell(INVALIDPACKAGE, DataCell.INVALID_DATA));
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
