package com.sunmi.printerservice.entity;

import java.math.BigDecimal;

/**
 * Created by Administrator on 2017/5/18.
 */

public class PrintDisCutterTimesBoxTimesRes {

    // 打印距离
    public BigDecimal printerDis;// 米
    // 切到次数
    public int cutterTimes=-1;
    // 钱箱打开次数
    public int moneyBoxTimes=-1;

    public int getPrinterDis() {
        return (int) (printerDis.floatValue()*1000);
    }

    public void setPrinterDis(BigDecimal printerDis) {
        this.printerDis = printerDis;
    }

    public int getCutterTimes() {
        return cutterTimes;
    }

    public void setCutterTimes(int cutterTimes) {
        this.cutterTimes = cutterTimes;
    }

    public int getMoneyBoxTimes() {
        return moneyBoxTimes;
    }

    public void setMoneyBoxTimes(int moneyBoxTimes) {
        this.moneyBoxTimes = moneyBoxTimes;
    }

    @Override
    public String toString() {
        return "PrintDisCutterTimesBoxTimesRes [printerDis=" + printerDis + ", cutterTimes=" + cutterTimes
                + ",moneyBoxTimes=" + moneyBoxTimes + "]";
    }
    
	
    public PrintDisCutterTimesBoxTimesRes(){
        printerDis=new BigDecimal(-1);
    }


    public PrintDisCutterTimesBoxTimesRes(BigDecimal printerDis, int cutterTimes, int moneyBoxTimes) {
        this.printerDis = printerDis;
        this.cutterTimes = cutterTimes;
        this.moneyBoxTimes = moneyBoxTimes;
    }

}