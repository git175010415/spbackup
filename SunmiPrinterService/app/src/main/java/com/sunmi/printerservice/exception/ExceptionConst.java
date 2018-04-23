package com.sunmi.printerservice.exception;

/**
 * Created by Administrator on 2017/8/14.
 */

public class ExceptionConst {
    public static final int UNSUPPORT = -1;
    public static final int UNSUPPORTENCODING = -2;
    public static final int ADDTASKFAILED = -3;
    public static final int CODEFAILED = -4;
    public static final int IllegalParameter  = -5;
    public static final int NullPointer = -6;

    public static final String UNSUPPORT_MSG = "command is not support,index #";
    public static final String UNSUPPORTUNSUPPORTENCODING_MSG = "# encoding is not support";
    public static final String ADDTASKFAILED_MSG = "oops,add task failed (the buffer of the task queue is 10M),please try later";
    public static final String CODEFAILED_MSG = "create command failed";
    public static final String IllegalParameter_MSG = "Illegal parameter";
    public static final String NullPointer_MSG = "param found null pointer";
}
