package com.sunmi.printerservice.entity;

/**
 * Author : kaltin
 * Create : 2018/3/26 17:00
 * Describe :
 */

public class PrintData {
    public int printerDis;
    public int cutterTimes;
    public int moneyBoxTimes;
    public int orderCounts;
    public int hotTimes;

    public PrintData(){

    }

    public PrintData(int printerDis, int cutterTimes, int moneyBoxTimes, int orderCounts, int hotTimes) {
        this.printerDis = printerDis;
        this.cutterTimes = cutterTimes;
        this.moneyBoxTimes = moneyBoxTimes;
        this.orderCounts = orderCounts;
        this.hotTimes = hotTimes;
    }
}
