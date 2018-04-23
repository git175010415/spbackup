package com.sunmi.printerservice.thread;

/**
 * Created by Administrator on 2017/7/24.
 */

public interface DownloadInterface {

    //回调检测打印机是否存在
    void hasPrinter(boolean isExist);

    //开始升级
    void startUpdate();

    //升级结果
    void updateStatus(boolean result);

    //开始打印
    void startPrinter();
}
