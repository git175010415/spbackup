package com.sunmi.printerservice.manager;

import com.sunmi.printerservice.utils.Utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import woyou.aidlservice.jiuiv5.ILcdCallback;
import woyou.aidlservice.jiuiv5.ITax;

public class PrinterState implements Cloneable{

    //打印机MCU的信息
    public static final int P_NO = 1;
    public static final int P_VERSION = 2;
    public static final int P_NAME = 3;
    public static final int P_FACORYNAME = 4;
    //打印机实时信息，需要更新打印机状态才能获取到最新状态
    public static final int R_PRINTERFACTORNAME = 101;
    public static final int R_PRINTINGLENGTH = 102;
    public static final int R_CUTTIMES = 103;
    public static final int R_BOXTIMES = 104;
    public static final int R_BLACKMODE = 105;
    public static final int R_BLACKVALUE = 106;
    public static final int R_ORDERS = 107;
    public static final int R_HOTS = 108;

    //事务打印状态
    public static final int NO_TRANS = -1;          //非事务状态
    public static final int PROCESS_TRANS = 0;      //事务发送中
    public static final int WAITING_TRANS = 1;      //事务发送完毕，MCU处理中
    public static final int INTERRUPT_TRANS = 2;    //事务异常中断
    public static final int SUCCESS_TRANS = 3;      //MCU处理结束
    public static final int FINISH_TRANS =4;        //事务异常结束

    public int initStatus;              //打印机是否准备结束 0未准备好 1打印机不存在 2打印机更新失败 3打印机可以工作

    //初始化时记录的打印机状态
    public String NO_;                  //打印机序列号
    public String version;              //打印机固件版本号
    public String name;                 //打印机型号
    public String fatoryName;           //打印机制造商

    //运行时记录的打印机相关状态
    public int printerFactorname;       //打印头型号
    public int printingLength;          //总打印距离
    public int cuttingCount;            //总切刀次数
    public int boxCount;                //总开箱次数(初始化不在打印机管理类中）
    public byte bbm_flag;               //黑标模式：0 false 1 true
    public short bbm_value;             //黑标自动走纸距离 0-1133
    public int orderCount;              //总订单数统计
    public int hotCount;                //总过热统计

    //不需要加锁控制
    public volatile int current_status; //事务模式过程中的状态
    //需要加锁控制
    public SleepState sleep;            //本地记录的睡眠状态
    public volatile boolean noSleep;    //强制唤醒标志
    public byte mess;                   //消息验证
    public byte paper;                  //缺纸
    public byte hot;                    //过热
    public byte cover;                  //开盖
    public byte kinfe;                  //切刀
    public byte black;                  //黑标
    public byte printerBufferCount;     //MCU剩余可用buffer
    public byte sleep_info;             //MCU反馈的睡眠状态
    public byte run_info;               //MCU反馈是否正在打印
    public int extraDataReadCount;      //税控可读字节数
    public int extraDataReadOffset;     //税控读取字节数偏移量
    public byte[] extraData;            //税控读取数据buffer

    //更新判断
    public byte mess_;
    public byte paper_;
    public byte hot_;
    public byte cover_;
    public byte kinfe_;
    public byte black_;

    //缓存税控反馈接口
    public ITax tax;
    //缓存LCD反馈接口
    public ILcdCallback lcd;

    @Override
    protected Object clone() {
        try {
            return  super.clone();
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
        }
        return this;
    }

    public enum SleepState {
        waking, deep_sleeping, light_sleeping, waiting_sleep, deep_exceptional;
    }

    public void init(byte[] recv) {
        getPrinterSerialNo(recv);
        getPrinterVersion(recv);
        getPrinterName(recv);
        getProductor(recv);
        sleep = SleepState.waking;
        current_status = NO_TRANS;
        printingLength = 0;
        cuttingCount = 0;
        printerFactorname = -1;

        mess_ = mess = 0;
        paper_ = paper = 0;
        hot_ = hot = 0;
        cover_ = cover = 0;
        kinfe_ = kinfe = 0;
        black_ = black = 0;
        initStatus = 3;
    }

    public void reset(){
        sleep = SleepState.waking;
        current_status = NO_TRANS;

        mess_ = mess = 0;
        paper_ = paper = 0;
        hot_ = hot = 0;
        cover_ = cover = 0;
        kinfe_ = kinfe = 0;
        black_ = black = 0;
        initStatus = 3;

        extraData = null;
        tax = null;
        extraDataReadOffset = 0;
    }

    //获取打印机的序列号
    private void getPrinterSerialNo(byte[] recv)  {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        for (int i = 112; i <= 127; i++) {
            if (recv[i] != 0) {
                buffer.write(recv[i]);
            } else {
                break;
            }
        }
        NO_ = Utils.getHexStringFromBytes(buffer.toByteArray());
        try {
            buffer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //获得打印版本号
    private void getPrinterVersion(byte[] recv) {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        for (int i = 96; i <= 111; i++) {
            if (recv[i] != 0) {
                buffer.write(recv[i]);
            } else {
                break;
            }
        }
        version = buffer.toString();
        try {
            buffer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //获得打印机型号
    private void getPrinterName(byte[] recv) {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        for (int i = 80; i <= 95; i++) {
            if (recv[i] != 0) {
                buffer.write(recv[i]);
            } else {
                break;
            }
        }
        name = buffer.toString();
        try {
            buffer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //制造商
    private void getProductor(byte[] recv) {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        for (int i = 64; i <= 79; i++) {
            if (recv[i] != 0) {
                buffer.write(recv[i]);
            } else {
                break;
            }
        }
        fatoryName = buffer.toString();
        try {
            buffer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
