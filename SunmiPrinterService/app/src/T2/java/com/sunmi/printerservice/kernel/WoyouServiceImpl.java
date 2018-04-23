package com.sunmi.printerservice.kernel;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Binder;
import android.os.Handler;
import android.os.RemoteException;
import android.support.v4.util.LruCache;

import com.sunmi.printerservice.cell.DataCell;
import com.sunmi.printerservice.commandbean.BarCodeTask;
import com.sunmi.printerservice.commandbean.BitMapCommand;
import com.sunmi.printerservice.commandbean.CommandTask;
import com.sunmi.printerservice.commandbean.FontSizeCommand;
import com.sunmi.printerservice.commandbean.ITask;
import com.sunmi.printerservice.commandbean.QRCodeTask;
import com.sunmi.printerservice.commandbean.SeriesTask;
import com.sunmi.printerservice.commandbean.StringCommand;
import com.sunmi.printerservice.commandbean.TableTask;
import com.sunmi.printerservice.entity.ServiceValue;
import com.sunmi.printerservice.exception.PrinterException;
import com.sunmi.printerservice.manager.PrinterState;
import com.sunmi.printerservice.render.BitmapCreator;
import com.sunmi.printerservice.utils.C;
import com.sunmi.printerservice.utils.LogUtils;
import com.tencent.stat.StatService;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import woyou.aidlservice.jiuiv5.ICallback;
import woyou.aidlservice.jiuiv5.ITax;
import woyou.aidlservice.jiuiv5.IWoyouService;

public class WoyouServiceImpl extends IWoyouService.Stub implements RequestInterface {

    private long taskId;
    private ExecutorService executorService;
    private LruCache<Integer, BitmapCreator> b;
    private Context service;
    private Handler mHandler;
    private PrinterManager p;
    private ServiceValue mServiceValue;

    public WoyouServiceImpl(Context service, Handler mHandler) {
        taskId = 0;
        this.service = service;
        this.mHandler = mHandler;
        mServiceValue = new ServiceValue(service);
        executorService = Executors.newSingleThreadExecutor();
        b = new LruCache<Integer, BitmapCreator>(C.LRU_MAXMEMORY){
            @Override
            protected int sizeOf(Integer key, BitmapCreator value) {
                return C.BC_MAXMEMORY;
            }
        };
        p = new PrinterManager(service, mHandler);
    }

    //根据uid获得对应bitmapcreator
    //保证每个进程(app)持有一个bitmapcreator
    private BitmapCreator getCreator() {
        BitmapCreator temp;
        int user = Binder.getCallingUid();
        if ((temp = b.get(user)) == null) {
            temp = new BitmapCreator(p, mServiceValue);
            b.put(user, temp);
        }
        return temp;
    }

    //添加打印渲染任务（渲染处理耗时）
    private void addTask(ITask a, BitmapCreator temp) {
        if (temp.getBufferModen()) {
            a.taskId = temp.getTaskId();
            temp.addTaskToBuffer(a);
        } else {
            a.taskId = taskId++;
            executorService.execute(a);
            //由于T1没有睡眠机制，故返回状态为当前保存的状态
            mHandler.sendMessage(mHandler.obtainMessage(1, p.getPrinterState()));
        }
    }

    //调用无效，预留接口
    @Override
    public void updateFirmware() throws RemoteException {
        StatService.trackCustomEvent(service, "printer_interface_1");
    }

    //调用无效，预留接口
    //获取打印机MCU状态：A5 bootloader C3 进入固件程序
    @Override
    public int getFirmwareStatus() throws RemoteException {
        StatService.trackCustomEvent(service, "printer_interface_2");
        return 0;
    }

    //获取打印服务的版本
    @Override
    public String getServiceVersion() throws RemoteException {
        StatService.trackCustomEvent(service, "printer_interface_3");
        try {
            PackageManager packageManager = service.getPackageManager();
            return packageManager.getPackageInfo(service.getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            return "";
        }
    }

    //获取MCU序列号
    @Override
    public String getPrinterSerialNo() throws RemoteException {
        StatService.trackCustomEvent(service, "printer_interface_6");
        return p.getInitStatus(PrinterState.P_NO);
    }

    //获取MCU固件版本号
    @Override
    public String getPrinterVersion() throws RemoteException {
        StatService.trackCustomEvent(service, "printer_interface_7");
        return p.getInitStatus(PrinterState.P_VERSION);
    }

    //获取打印机型号（同机具机型）
    @Override
    public String getPrinterModal() throws RemoteException {
        StatService.trackCustomEvent(service, "printer_interface_8");
        return p.getInitStatus(PrinterState.P_NAME);
    }

    //获取打印机上电以来的打印距离
    @Override
    public int getPrintedLength() throws RemoteException {
        StatService.trackCustomEvent(service, "printer_interface_9");
        if (p.getRealStatus(PrinterState.R_PRINTINGLENGTH) == -1) {
            return -1;
        }else{
            mServiceValue.addDistance(p.getRealStatus(PrinterState.R_PRINTINGLENGTH));
            return mServiceValue.getDistance();
        }
    }

    //获取打印机切刀上电以来次数
    @Override
    public int getCutPaperTimes() throws RemoteException {
        StatService.trackCustomEvent(service, "printer_interface_34");
        if (p.getRealStatus(PrinterState.R_CUTTIMES) == -1) {
            return -1;
        }else{
            mServiceValue.addCuts(p.getRealStatus(PrinterState.R_CUTTIMES));
            return mServiceValue.getCuts();
        }
    }

    //初始化渲染，不影响初始化之后的队列，但会清除初始化前的数据
    @Override
    public void printerInit(ICallback callback) throws RemoteException {
        StatService.trackCustomEvent(service, "printer_interface_4");
        BitmapCreator temp = getCreator();
        try {
            addTask(new CommandTask(temp, new byte[]{0x1B, 0x40}, callback), temp);
        } catch (PrinterException e) {
            if (callback != null){
                callback.onRunResult(false);
                callback.onRaiseException(e.code, e.msg);
            }
        }
    }

    //输出打印自检页
    @Override
    public void printerSelfChecking(ICallback callback) throws RemoteException {
        StatService.trackCustomEvent(service, "printer_interface_5");
        BitmapCreator temp = getCreator();
        try {
            addTask(new CommandTask(temp, new byte[]{0x1F, 0x1B, 0x1F, 0x53}, callback), temp);
        } catch (PrinterException e) {
            if (callback != null){
                callback.onRunResult(false);
                callback.onRaiseException(e.code, e.msg);
            }
        }
    }

    //输出跳行
    @Override
    public void lineWrap(int n, ICallback callback) throws RemoteException {
        StatService.trackCustomEvent(service, "printer_interface_30");
        BitmapCreator temp = getCreator();
        StringBuilder text = new StringBuilder("");
        for (int i = 0; i < n; i++) {
            text.append("\n");
        }
        try {
            addTask(new StringCommand(temp, text.toString(), callback, false), temp);
        } catch (PrinterException e) {
            if (callback != null){
                callback.onRunResult(false);
                callback.onRaiseException(e.code, e.msg);
            }
        }
    }

    //epson指令集
    @Override
    public void sendRAWData(byte[] data, ICallback callback) throws RemoteException {
        StatService.trackCustomEvent(service, "printer_interface_11");
        BitmapCreator temp = getCreator();
        try {
            addTask(new CommandTask(temp, data, callback), temp);
        } catch (PrinterException e) {
            if (callback != null){
                callback.onRunResult(false);
                callback.onRaiseException(e.code, e.msg);
            }
        }
    }

    //设置对齐模式
    @Override
    public void setAlignment(int alignment, ICallback callback) throws RemoteException {
        StatService.trackCustomEvent(service, "printer_interface_12");
        BitmapCreator temp = getCreator();
        try {
            addTask(new CommandTask(temp, new byte[]{0x1B, 0x61, (byte) alignment}, callback), temp);
        } catch (PrinterException e) {
            if (callback != null){
                callback.onRunResult(false);
                callback.onRaiseException(e.code, e.msg);
            }
        }
    }

    //打印机切刀(会走纸）
    public void cutPaper(ICallback callback) throws RemoteException {
        StatService.trackCustomEvent(service, "printer_interface_33");
        BitmapCreator temp = getCreator();
        try {
            addTask(new CommandTask(temp, new byte[]{0x1D, 0x56, 0x42, 0x00}, callback), temp);
        } catch (PrinterException e) {
            if (callback != null){
                callback.onRunResult(false);
                callback.onRaiseException(e.code, e.msg);
            }
        }
    }

    //打开钱箱
    public void openDrawer(ICallback callback) throws RemoteException {
        StatService.trackCustomEvent(service, "printer_interface_35");
        p.openBox();
    }

    //打开钱箱次数
    public int getOpenDrawerTimes() throws RemoteException {
        StatService.trackCustomEvent(service, "printer_interface_36");
        if (p.getRealStatus(PrinterState.R_BOXTIMES) == -1) {
            return -1;
        }else{
            mServiceValue.addOpens(p.getRealStatus(PrinterState.R_BOXTIMES));
            return mServiceValue.getOpens();
        }
    }

    //调用无效，预留接口
    @Override
    public void setFontName(String typeface, ICallback callback) throws RemoteException {
        StatService.trackCustomEvent(service, "printer_interface_13");
    }

    //设置字体的大小
    @Override
    public void setFontSize(float fontsize, ICallback callback) throws RemoteException {
        StatService.trackCustomEvent(service, "printer_interface_14");
        BitmapCreator temp = getCreator();
        try {
            addTask(new FontSizeCommand(temp, fontsize, callback), temp);
        } catch (PrinterException e) {
            if (callback != null){
                callback.onRunResult(false);
                callback.onRaiseException(e.code, e.msg);
            }
        }
    }

    //将数据保存到缓冲区,缓冲区满或换行符后将输出
    @Override
    public void printText(String text, ICallback callback) throws RemoteException {
        StatService.trackCustomEvent(service, "printer_interface_15");
        BitmapCreator temp = getCreator();
        try {
            addTask(new StringCommand(temp, text, callback, false), temp);
        } catch (PrinterException e) {
            if (callback != null){
                callback.onRunResult(false);
                callback.onRaiseException(e.code, e.msg);
            }
        }
    }

    //目前此方法同printText
    @Override
    public void printOriginalText(String text, ICallback callback) throws RemoteException {
        StatService.trackCustomEvent(service, "printer_interface_16");
        BitmapCreator temp = getCreator();
        try {
            addTask(new StringCommand(temp, text, callback, true), temp);
        } catch (PrinterException e) {
            if (callback != null){
                callback.onRunResult(false);
                callback.onRaiseException(e.code, e.msg);
            }
        }
    }

    //同printText，可设置输出字体大小（仅本次有效），暂未实现typeface设置
    @Override
    public void printTextWithFont(String text, String typeface, float fontsize, ICallback callback) throws RemoteException {
        StatService.trackCustomEvent(service, "printer_interface_17");
        BitmapCreator temp = getCreator();
        try {
            addTask(new StringCommand(temp, text, typeface, fontsize, callback), temp);
        } catch (PrinterException e) {
            if (callback != null){
                callback.onRunResult(false);
                callback.onRaiseException(e.code, e.msg);
            }
        }
    }

    //旧的输出表格方法（对外文如阿拉伯语不支持）
    @Override
    public void printColumnsText(String[] colsTextArr, int[] colsWidthArr, int[] colsAlign, ICallback callback) throws RemoteException {
        StatService.trackCustomEvent(service, "printer_interface_18");
        BitmapCreator temp = getCreator();
        try {
            addTask(new TableTask(temp, colsTextArr, colsWidthArr, colsAlign, 1, callback), temp);
        } catch (PrinterException e) {
            if (callback != null){
                callback.onRunResult(false);
                callback.onRaiseException(e.code, e.msg);
            }
        }
    }

    //新的输出表格方法，全语言支持
    @Override
    public void printColumnsString(String[] colsTextArr, int[] colsWidthArr, int[] colsAlign, ICallback callback) throws RemoteException {
        StatService.trackCustomEvent(service, "printer_interface_19");
        BitmapCreator temp = getCreator();
        try {
            addTask(new TableTask(temp, colsTextArr, colsWidthArr, colsAlign, 2, callback), temp);
        } catch (PrinterException e) {
            if (callback != null){
                callback.onRunResult(false);
                callback.onRaiseException(e.code, e.msg);
            }
        }
    }

    //将图片保存到缓冲区，换行符或缓冲区慢将输出
    @Override
    public void printBitmap(Bitmap bitmap, ICallback callback) throws RemoteException {
        StatService.trackCustomEvent(service, "printer_interface_20");
        BitmapCreator temp = getCreator();
        try {
            addTask(new BitMapCommand(temp, bitmap, callback), temp);
        } catch (PrinterException e) {
            if (callback != null){
                callback.onRunResult(false);
                callback.onRaiseException(e.code, e.msg);
            }
        }
    }

    //打印一维条码
    @Override
    public void printBarCode(String data, int symbology, int height, int width, int textposition, ICallback callback) throws RemoteException {
        StatService.trackCustomEvent(service, "printer_interface_21");
        BitmapCreator temp = getCreator();
        try {
            addTask(new BarCodeTask(temp, data, symbology, height, width, textposition, callback), temp);
        } catch (PrinterException e) {
            if (callback != null){
                callback.onRunResult(false);
                callback.onRaiseException(e.code, e.msg);
            }
        }
    }

    //打印二维码
    @Override
    public void printQRCode(String data, int modulesize, int errorlevel, ICallback callback) throws RemoteException {
        StatService.trackCustomEvent(service, "printer_interface_22");
        BitmapCreator temp = getCreator();
        try {
            addTask(new QRCodeTask(temp, data, modulesize, errorlevel, callback), temp);
        } catch (PrinterException e) {
            if (callback != null){
                callback.onRunResult(false);
                callback.onRaiseException(e.code, e.msg);
            }
        }
    }

    //进入事务打印模式
    @Override
    public void enterPrinterBuffer(boolean clean) throws RemoteException {
        StatService.trackCustomEvent(service, "printer_interface_24");
        BitmapCreator temp = getCreator();
        temp.setBufferModen(true);
        if (clean || temp.isNullTaskList()) {
            temp.cancleTaskList();
            temp.setTaskId(taskId++);
            addTask(new SeriesTask(temp, SeriesTask.HEAD), temp);
        }
    }

    //提交事务打印
    @Override
    public void commitPrinterBuffer() throws RemoteException {
        StatService.trackCustomEvent(service, "printer_interface_25");
        BitmapCreator temp = getCreator();
        //缓冲打印提交前增加尾task
        if (!temp.getBufferModen()) {
            return;
        }//非事务模式调用无效

        if (temp.isNullTaskList()) {
            return;
        }//已提交过调用无效

        addTask(new SeriesTask(temp, SeriesTask.TAIL), temp);
        temp.excuteTaskList(executorService);
        temp.cancleTaskList();
        temp.setTaskId(taskId++);
        addTask(new SeriesTask(temp, SeriesTask.HEAD), temp);
    }


    //退出事务打印
    @Override
    public void exitPrinterBuffer(boolean commit) throws RemoteException {
        StatService.trackCustomEvent(service, "printer_interface_27");
        BitmapCreator temp = getCreator();
        if (!temp.getBufferModen()) {
            return;
        }//只有buffermode才能使用提交buffer直接跳过

        if (commit && !temp.isNullTaskList()) {
            addTask(new SeriesTask(temp, SeriesTask.TAIL), temp);
            temp.excuteTaskList(executorService);
        }
        temp.setBufferModen(false);
    }

    //发送税控数据，直接通过打印管理类
    @Override
    public void tax(byte[] data, ITax callback) throws RemoteException {
        StatService.trackCustomEvent(service, "printer_interface_29");
        p.sendDataCell(new DataCell(data, DataCell.TAX_DATA, callback));
    }

    @Override
    public int getPrinterMode() throws RemoteException {
        StatService.trackCustomEvent(service, "printer_interface_37");
        updatePrinterState();
        return p.getRealStatus(PrinterState.R_BLACKMODE);
    }

    @Override
    public int getPrinterBBMDistance() throws RemoteException {
        StatService.trackCustomEvent(service, "printer_interface_38");
        updatePrinterState();
        return p.getRealStatus(PrinterState.R_BLACKVALUE);
    }

    @Override
    public int updatePrinterState() throws RemoteException {
        StatService.trackCustomEvent(service, "printer_interface_32");
        return p.updatePrinterState();
    }

    @Override
    public void commitPrinterBufferWithCallback(ICallback callback) throws RemoteException {
        StatService.trackCustomEvent(service, "printer_interface_26");
        BitmapCreator temp = getCreator();
        //缓冲打印提交前增加尾task
        if (!temp.getBufferModen()) {
            return;
        }//只有buffermode才能使用提交buffer直接跳过

        if (temp.isNullTaskList()) {
            return;
        }//已提交过调用无效

        addTask(new SeriesTask(temp, SeriesTask.TAIL, callback), temp);
        temp.excuteTaskList(executorService);
        temp.cancleTaskList();
        temp.setTaskId(taskId++);
        addTask(new SeriesTask(temp, SeriesTask.HEAD), temp);
    }

    @Override
    public void exitPrinterBufferWithCallback(boolean commit, ICallback callback) throws RemoteException {
        StatService.trackCustomEvent(service, "printer_interface_28");
        BitmapCreator temp = getCreator();
        if (!temp.getBufferModen()) {
            return;
        }//只有buffermode才能使用提交buffer直接跳过

        if (commit && !temp.isNullTaskList()) {
            addTask(new SeriesTask(temp, SeriesTask.TAIL, callback), temp);
            temp.excuteTaskList(executorService);
        }
        temp.setBufferModen(false);
    }

    @Override
    public void printBitmapCustom(Bitmap bitmap, int type, ICallback callback) throws RemoteException {
        BitmapCreator temp = getCreator();
        try {
            addTask(new BitMapCommand(temp, bitmap, type, callback), temp);
        } catch (PrinterException e) {
            if (callback != null){
                callback.onRunResult(false);
                callback.onRaiseException(e.code, e.msg);
            }
        }
    }

    /*********************
     * 内部回调接口
     * ********************/
    //更新本地打印机三参数
    @Override
    public void updataLocalDCB() {

    }

    //获取运行时参数
    @Override
    public ServiceValue getServiceValue() {
        return mServiceValue;
    }

    /*********************
     * 内部调用接口
     * ********************/
    //网络变化操作
    public void netChanged(boolean onlyone){
        LogUtils.e("nothing to do" + onlyone);
    }

    /**
     * 处理设置消息
     * @param msg 来自setting的数据
     */
    public void handleSettingMessage(String msg){
        LogUtils.e("it shouldn't receive setting: "+msg);
    }

    /**
     * 处理推送消息
     * @param msg 消息内容
     *            mark说明： 初值（clear值）-1 清除配置
     *            黑标设置 低0位：0 设置 1未设置
     *            纸张设置 低1位：0 设置 1未设置
     *            全局设置 低2位：0 设置 1未设置
     *            扩展
     */
    public void handlePushMessage(String msg) {
        LogUtils.e("it shouldn't receive push: "+msg);
    }

    //销毁
    public void destroy() {
        executorService.shutdownNow();
        p.close();
    }
}
