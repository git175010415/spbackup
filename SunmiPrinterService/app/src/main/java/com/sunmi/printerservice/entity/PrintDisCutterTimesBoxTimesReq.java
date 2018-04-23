package com.sunmi.printerservice.entity;

import java.math.BigDecimal;

public class PrintDisCutterTimesBoxTimesReq {

	//机具条码
	public String msn;
	//机器类型
	public String machineModel;
	// 打印距离
	public BigDecimal printerDis;// 米
	// 切到次数
	public int cutterTimes;
	// 钱箱打开次数
	public int moneyBoxTimes;

	@Override
	public String toString() {
		return "PrintDisCutterTimesBoxTimesRes [printerDis=" + printerDis + ", cutterTimes=" + cutterTimes
				+ ",moneyBoxTimes=" + moneyBoxTimes + "]";
	}

	public PrintDisCutterTimesBoxTimesReq(String msn,String machineModel,BigDecimal printerDis, int cutterTimes, int moneyBoxTimes) {
		this.msn=msn;
		this.machineModel=machineModel;
		this.printerDis = printerDis;
		this.cutterTimes = cutterTimes;
		this.moneyBoxTimes = moneyBoxTimes;
	}

}