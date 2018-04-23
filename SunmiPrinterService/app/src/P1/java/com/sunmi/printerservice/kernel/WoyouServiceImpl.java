package com.sunmi.printerservice.kernel;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
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
import com.sunmi.printerservice.exception.ExceptionConst;
import com.sunmi.printerservice.exception.PrinterException;
import com.sunmi.printerservice.manager.PrinterState;
import com.sunmi.printerservice.render.BitmapCreator;
import com.sunmi.printerservice.utils.C;
import com.sunmi.printerservice.utils.LogUtils;
import com.sunmi.printerservice.utils.PreferencesLoader;
import com.sunmi.trans.TransBean;
import com.tencent.stat.StatService;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import woyou.aidlservice.jiuiv5.ICallback;
import woyou.aidlservice.jiuiv5.ITax;
import woyou.aidlservice.jiuiv5.IWoyouService;

public class WoyouServiceImpl extends IWoyouService.Stub implements RequestInterface{

    private long taskId;
    private ExecutorService executorService;
    private LruCache<Integer, BitmapCreator> b;
    private Context service;
    private Handler mHandler;
    private PrinterManager p;
    private RequestManage mRequestImp;
    private ServiceValue mServiceValue;

    public WoyouServiceImpl(Context service, Handler mHandler){
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
        mRequestImp = new RequestManage(service, this);
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
            //这里上报的打印机状态需要刷新
            mHandler.sendMessage(mHandler.obtainMessage(1, p.updatePrinterState()));
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
    public void getPrintedLength(ICallback callback) throws RemoteException {
        StatService.trackCustomEvent(service, "printer_interface_9");
        if (callback != null) {
            if(p.getRealStatus(PrinterState.R_PRINTINGLENGTH) == -1){
                callback.onReturnString("");
            }else{
                callback.onReturnString(p.getRealStatus(PrinterState.R_PRINTINGLENGTH) +"");
            }
        }
    }

    //获取打印机上电以来的打印头驱动类型
    @Override
    public void getPrinterFactory(ICallback callback) throws RemoteException {
        StatService.trackCustomEvent(service, "printer_interface_10");
        if (callback == null)
            return;
        if (p.getRealStatus(PrinterState.R_PRINTERFACTORNAME) == 0) {
            callback.onReturnString("精工打印头");
        } else if (p.getRealStatus(PrinterState.R_PRINTERFACTORNAME) == 1) {
            callback.onReturnString("普瑞特打印头");
        } else {
            callback.onReturnString("");
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

    //transbean 老接口需要保留
    public void commitPrint(TransBean[] transbean, ICallback callback) throws RemoteException {
        StatService.trackCustomEvent(service, "printer_interface_23");
        BitmapCreator temp = getCreator();
        if (transbean == null || transbean.length == 0) {
            if (callback != null) {
                callback.onRaiseException(ExceptionConst.IllegalParameter, ExceptionConst.IllegalParameter_MSG);
            }
            return;
        }
        for (TransBean bean : transbean) {
            switch (bean.getType()) {
                case 0: // 指令数据
                    try {
                        addTask(new CommandTask(temp, bean.getData(), callback), temp);
                    } catch (PrinterException e) {
                        if (callback != null)
                            callback.onRaiseException(ExceptionConst.ADDTASKFAILED, ExceptionConst.ADDTASKFAILED_MSG);
                    }
                    break;
                case 1: // 打印字符串
                    try {
                        addTask(new StringCommand(temp, bean.getText(), callback, false), temp);
                    } catch (PrinterException e) {
                        if (callback != null)
                            callback.onRaiseException(ExceptionConst.ADDTASKFAILED, ExceptionConst.ADDTASKFAILED_MSG);
                    }
                    break;
                case 2: // 打印原始宽度字符串
                    try {
                        addTask(new StringCommand(temp, bean.getText(), callback, true), temp);
                    } catch (PrinterException e) {
                        if (callback != null)
                            callback.onRaiseException(ExceptionConst.ADDTASKFAILED, ExceptionConst.ADDTASKFAILED_MSG);
                    }
                    break;
                case 3: // 设置字体大小
                    try {
                        float fontsize = Float.parseFloat("" + bean.getText());
                        addTask(new FontSizeCommand(temp, fontsize, callback), temp);
                    } catch (PrinterException e) {
                        if (callback != null)
                            callback.onRaiseException(ExceptionConst.ADDTASKFAILED, ExceptionConst.ADDTASKFAILED_MSG);
                    }
                    break;
                case 4: // 设置字体, text传递是字体路径

                    break;
                case 5: // 设置字体, text传递是字体名称

                    break;
                case 6: // 打印bitmap图片
                    byte[] data = bean.getData();
                    Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
                    try {
                        addTask(new BitMapCommand(temp, bitmap, callback), temp);
                    } catch (PrinterException e) {
                        if (callback != null)
                            callback.onRaiseException(ExceptionConst.ADDTASKFAILED, ExceptionConst.ADDTASKFAILED_MSG);
                    }
                    break;
                default:
                    try {
                        addTask(new CommandTask(temp, bean.getData(), callback), temp);
                    } catch (PrinterException e) {
                        if (callback != null)
                            callback.onRaiseException(ExceptionConst.ADDTASKFAILED, ExceptionConst.ADDTASKFAILED_MSG);
                    }
                    break;
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

    //提交事务打印带回调
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

    //退出事务打印带回调
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

    //发送税控数据，直接通过打印管理类
    @Override
    public void tax(byte[] data, ITax callback) throws RemoteException {
        StatService.trackCustomEvent(service, "printer_interface_29");
        p.sendDataCell(new DataCell(data, DataCell.TAX_DATA, callback));
    }

    //已废弃 无效接口
    @Deprecated
    public void clearBuffer() throws RemoteException {
        StatService.trackCustomEvent(service, "printer_interface_31");
    }

    //接口获取打印机的状态（会刷新获取实时状态）
    @Override
    public int updatePrinterState() throws RemoteException {
        StatService.trackCustomEvent(service, "printer_interface_32");
        return p.updatePrinterState();
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
     * 接口回调
     *********************/
    @Override
    public void setSettingStyle(String style) {
        JSONObject jsonObject;
        try{
            jsonObject = new JSONObject(style);
        }catch (JSONException e){
            e.printStackTrace();
            return;
        }
        try{
            mServiceValue.setTypeface(service, jsonObject.getBoolean("fontDefault"));
            mServiceValue.updateMyId();
        }catch (JSONException e){
            e.printStackTrace();
        }
        try {
            jsonObject = new JSONObject(style);
            if(jsonObject.getInt("font_width") != 0 && jsonObject.getInt("font_height") != 0){
                mServiceValue.setTextTimesWidth(2);
                mServiceValue.setTextTimesHeight(2);
                mServiceValue.setTextTimes(true);
            }else if(jsonObject.getInt("font_height") != 0){
                mServiceValue.setTextTimesWidth(1);
                mServiceValue.setTextTimesHeight(2);
                mServiceValue.setTextTimes(true);
            }else if(jsonObject.getInt("font_width") != 0){
                mServiceValue.setTextTimesWidth(2);
                mServiceValue.setTextTimesHeight(1);
                mServiceValue.setTextTimes(true);
            }else{
                mServiceValue.setTextTimesWidth(1);
                mServiceValue.setTextTimesHeight(1);
                mServiceValue.setTextTimes(false);
            }
            mServiceValue.setReversePrintingMode(jsonObject.getInt("inverse_white") != 0);
            mServiceValue.setFakeBoldText(jsonObject.getInt("font_weight")!= 0);
            mServiceValue.setUnderlineText(jsonObject.getInt("underline") != 0);
            mServiceValue.setSetLineHeight(jsonObject.getInt("row_height") != 0);
            int row_space = (int) jsonObject.getDouble("row_space");
            if(row_space >= 0 && row_space < 256){
                mServiceValue.setLineHeight(row_space);
            }else{
                mServiceValue.setLineHeight(32);
            }
            mServiceValue.updateMyId();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void updataLocalDCB() {
        //无DCB上传
    }

    @Override
    public ServiceValue getServiceValue() {
        return mServiceValue;
    }


    /*********************
     * 内部调用接口
     * ********************/
    //销毁
    public void destroy() {
        executorService.shutdownNow();
        p.close();
    }

    /**
     * 处理网络变化
     */
    public void netChanged(boolean onlyone){
        mRequestImp.reset(onlyone);
    }

    /**
     * 处理推送
     * @param msg 消息处理
     */
    public void handlePushMessage(String msg) {
        if(msg == null){
            return;
        }
        try{
            PreferencesLoader pl = new PreferencesLoader(service, "settings");
            pl.saveString("web_style", msg);
            if(0 != (pl.getInt("mark")&0x04)){
                setSettingStyle(msg);
            }
        }catch (Exception e){
            LogUtils.e(e.getMessage());
        }
    }

    /**
     * 配置消息处理，直接启用
     * @param msg 配置来的消息
     */
    public void handleSettingMessage(String msg){
        if(msg != null){
            setSettingStyle(msg);
        }
    }
}
