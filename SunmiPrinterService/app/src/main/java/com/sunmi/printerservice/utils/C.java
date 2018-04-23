package com.sunmi.printerservice.utils;

/**
 * Created by Administrator on 2017/7/20.
 */

public class C {
    public static final int LRU_MAXMEMORY = 1024 * 1024 * 50;
    public static final int BC_MAXMEMORY = 1024 * 1024 * 10;
    public static final int PM_MAXMEMORY = 1024 * 1024;

    public static final int UPDATE_COUNT = 10;

    public static final byte[] INVALIDPACKAGE = new byte[256];
    public static final byte[] SLEEP_BUFFER = new byte[]{0x1F, 0x1B, 0x1F, 0x01, 0x73, 0x02, 0x6C, 0x03, 0x65, 0x04, 0x65, 0x05, 0x70};
    public static final byte[] WAITING_BUFFER = new byte[]{0x10, 0x04, 0x01};
    public static final byte[] CLEAR_BUFFER = new byte[] { 0x10, 0x14, 0x08, 0x01, 0x03, 0x14, 0x01, 0x06, 0x02, 0x08 };
    public static final byte[] ENTER_BOOTLOADER = new byte[] { 0x1F, 0x1B, 0x1F, 0x33, 0x55, (byte) 0xAA, (byte) 0xCC };
    public static final byte MODEM_SOH = 0x01; // 数据块起始字符 128 字节开始
    public static final byte MODEM_EOT = 0x04; // 文件传输结束
    public static final byte MODEM_ACK = 0x06; // 确认应答
    public static final byte MODEM_NAK = 0x15; // 出现错误
    public static final byte MODEM_CAN = 0x18; // 取消传输
    public static final byte MODEM_CRC = 0x43; // 大写字母C


    //sharepreference
    public static final String VOICE_CACHE_KEY = "sunmi_voice_cache_key_paper_alarm";
    //广播
    // 打印机推送广播
    public final static String PUSH_ACTION = "woyou.aidlservice.jiuiv5";
    // 打印机内部广播
    public final static String INNER_ACTION = "woyou.aidlservice.jiuiv5.INNER";

    // 打印机准备中
    public final static String INIT_ACTION = "woyou.aidlservice.jiuv5.INIT_ACTION";
    // 打印机更新中
    public final static String FIRMWARE_UPDATING_ACTION = "woyou.aidlservice.jiuv5.FIRMWARE_UPDATING_ACITON";
    // 打印机更新中(EN)
    public final static String FIRMWARE_UPDATING_ACTION_EN = "woyou.aidlservice.jiuv5.FIRMWARE_UPDATING_ACITON_EN";
    // 可以打印
    public final static String NORMAL_ACTION = "woyou.aidlservice.jiuv5.NORMAL_ACTION";
    // 打印错误
    public final static String ERROR_ACTION = "woyou.aidlservice.jiuv5.ERROR_ACTION";
    // 缺纸异常
    public final static String OUT_OF_PAPER_ACTION = "woyou.aidlservice.jiuv5.OUT_OF_PAPER_ACTION";
    //缺纸异常（EN）
    public final static String OUT_OF_PAPER_ACTION_EN = "woyou.aidlservice.jiuv5.OUT_OF_PAPER_ACTION_EN";
    // 打印头过热异常
    public final static String OVER_HEATING_ACTION = "woyou.aidlservice.jiuv5.OVER_HEATING_ACITON";
    // 打印头过热异常(EN)
    public final static String OVER_HEATING_ACTION_EN = "woyou.aidlservice.jiuv5.OVER_HEATING_ACITON_EN";
    //打印头温度恢复正常
    public final static String NORMAL_HEATING_ACTION = "woyou.aidlservice.jiuv5.NORMAL_HEATING_ACITON";
    //打印头温度恢复正常(EN)
    public final static String NORMAL_HEATING_ACTION_EN = "woyou.aidlservice.jiuv5.NORMAL_HEATING_ACITON_EN";
    // 开盖子
    public final static String COVER_OPEN_ACTION = "woyou.aidlservice.jiuv5.COVER_OPEN_ACTION";
    // 开盖子（EN)
    public final static String COVER_OPEN_ACTION_EN = "woyou.aidlservice.jiuv5.COVER_OPEN_ACTION_EN";
    //关盖子异常
    public final static String COVER_ERROR_ACTION = "woyou.aidlservice.jiuv5.COVER_ERROR_ACTION";
    //关盖子异常(EN)
    public final static String COVER_ERROR_ACTION_EN = "woyou.aidlservice.jiuv5.COVER_ERROR_ACTION_EN";
    // 切刀异常1
    public final static String KNIFE_ERROR_1_ACTION = "woyou.aidlservice.jiuv5.KNIFE_ERROR_ACTION_1";
    // 切刀异常1
    public final static String KNIFE_ERROR_1_ACTION_EN = "woyou.aidlservice.jiuv5.KNIFE_ERROR_ACTION_1_EN";
    // 切刀异常2(切刀修复)
    public final static String KNIFE_ERROR_2_ACTION = "woyou.aidlservice.jiuv5.KNIFE_ERROR_ACTION_2";
    // 切刀异常2(切刀修复)(EN)
    public final static String KNIFE_ERROR_2_ACTION_EN = "woyou.aidlservice.jiuv5.KNIFE_ERROR_ACTION_2_EN";
    // 打印机固件升级成功
    public final static String FIRMWARE_FINISH_ACTION = "woyou.aidlservice.jiuv5.FIRMWARE_FINISH_ACITON";
    // 打印机固件升级成功(EN)
    public final static String FIRMWARE_FINISH_ACTION_EN = "woyou.aidlservice.jiuv5.FIRMWARE_FINISH_ACITON_EN";
    // 打印机固件升级失败
    public final static String FIRMWARE_FAILURE_ACTION = "woyou.aidlservice.jiuv5.FIRMWARE_FAILURE_ACITON";
    // 打印机固件升级失败(EN)
    public final static String FIRMWARE_FAILURE_ACTION_EN = "woyou.aidlservice.jiuv5.FIRMWARE_FAILURE_ACITON_EN";
    //未发现打印机
    public final static String PRINTER_NON_EXISTENT_ACTION = "woyou.aidlservice.jiuv5.PRINTER_NON_EXISTENT_ACITON";
    //未检测到黑标
    public final static String BLACKLABEL_NON_EXISTENT_ACTION = "woyou.aidlservice.jiuv5.BLACKLABEL_NON_EXISTENT_ACITON";
    //过热（JP）
    public final static String HOT_JP = "sunmi_hot_paper_missing_alarm_hot_HOT_JP";
    //缺纸（JP)
    public final static String NO_PAPER_JP = "sunmi_hot_paper_missing_alarm_hot_NO_PAPER_JP";
    //准备中（JP）
    public final static String UPDATE_JP = "sunmi_hot_paper_missing_alarm_hot_UPDATE_JP";
    //过热（RU）
    public final static String HOT_RU = "sunmi_hot_paper_missing_alarm_hot_HOT_RU";
    //缺纸（RU）
    public final static String NO_PAPER_RU = "sunmi_hot_paper_missing_alarm_hot_NO_PAPER_RU";

}
