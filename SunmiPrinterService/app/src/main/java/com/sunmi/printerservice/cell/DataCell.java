package com.sunmi.printerservice.cell;

import com.sunmi.printerservice.commandbean.SeriesTask;

import woyou.aidlservice.jiuiv5.ICallback;
import woyou.aidlservice.jiuiv5.ILcdCallback;
import woyou.aidlservice.jiuiv5.ITax;

public class DataCell {
    public static final int PRINT_DATA = 0;
    public static final int LCD_DATA = 1;
    public static final int TAX_DATA = 2;
    public static final int COMMAND_DATA = 3;
    public static final int QUERY_DATA = 4;
    public static final int INVALID_DATA = 5;
    private static long TIME = 10;
    private static short ID = 0;

    public byte[] data;
    public int type;
    public int series; // 0 非连续 1 连续头 2 连续尾
    public int offset;
    public long time;
    public byte[] id;
    public ITax tax;
    public ICallback callback;
    public ILcdCallback lcd;


    public DataCell(byte[] data, int type) {
        this.data = data;
        this.type = type;
        this.series = SeriesTask.COMMON;
        this.offset = 0;
        this.time = TIME++;
        if (type == PRINT_DATA) {
            ID++;
            if (ID > 4095) {
                ID = 0;
            }
            id = new byte[2];
            id[1] = (byte) (0x40 | (byte) (ID & (short) 0x003F));
            id[0] = (byte) (0x80 | (byte) ((ID & (short) 0x0FC0) >> 6));
        }
        this.tax = null;
        this.callback = null;
        this.lcd = null;
    }

    public DataCell(byte[] data, int type, ITax tax) {
        this.data = data;
        this.type = type;
        this.series = SeriesTask.COMMON;
        this.offset = 0;
        this.time = TIME++;
        if (type == PRINT_DATA) {
            ID++;
            if (ID > 4095) {
                ID = 0;
            }
            id = new byte[2];
            id[1] = (byte) (0x40 | (byte) (ID & (short) 0x003F));
            id[0] = (byte) (0x80 | (byte) ((ID & (short) 0x0FC0) >> 6));
        }
        this.tax = tax;
        this.callback = null;
        this.lcd = null;
    }

    public DataCell(byte[] data, ILcdCallback lcd) {
        this.data = data;
        this.type = LCD_DATA;
        this.series = SeriesTask.COMMON;
        this.offset = 0;
        this.time = TIME++;
        if (type == PRINT_DATA) {
            ID++;
            if (ID > 4095) {
                ID = 0;
            }
            id = new byte[2];
            id[1] = (byte) (0x40 | (byte) (ID & (short) 0x003F));
            id[0] = (byte) (0x80 | (byte) ((ID & (short) 0x0FC0) >> 6));
        }
        this.tax = null;
        this.callback = null;
        this.lcd = lcd;
    }

    public DataCell(int series, ICallback callback) {
        this.data = new byte[] { 0x0 };
        this.type = PRINT_DATA;
        this.series = series;
        this.offset = 0;
        this.time = TIME++;
        this.id = null;
        this.tax = null;
        this.callback = callback;
        this.lcd = null;
    }
}
