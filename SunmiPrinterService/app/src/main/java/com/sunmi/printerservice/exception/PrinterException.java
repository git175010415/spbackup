package com.sunmi.printerservice.exception;

/**
 * Created by Administrator on 2017/8/14.
 */

public class PrinterException extends Exception {
    public PrinterException(int unsupport, String replace) {
        this.code=unsupport;
        this.msg=replace;
    }
    private static final long serialVersionUID = -1429391985402902259L;
    public int code;
    public String msg;
}
