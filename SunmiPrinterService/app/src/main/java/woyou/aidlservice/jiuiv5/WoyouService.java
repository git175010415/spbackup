package woyou.aidlservice.jiuiv5;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.text.Html;
import android.text.Spanned;
import android.text.TextUtils;

import com.sunmi.printerservice.kernel.WoyouServiceImpl;
import com.sunmi.printerservice.manager.OneBtnDialogManager;
import com.sunmi.printerservice.utils.Adaptation;
import com.sunmi.printerservice.utils.C;
import com.sunmi.printerservice.utils.LogUtils;
import com.sunmi.printerservice.utils.NetStateUtils;

import static android.net.ConnectivityManager.CONNECTIVITY_ACTION;

public class WoyouService extends Service {
    WoyouServiceImpl service;
    boolean isNeedDelay = true;

    @Override
    public void onCreate() {
        super.onCreate();
        initialization();
    }

    private void initialization() {
        Adaptation.init(this);
        service = new WoyouServiceImpl(getApplicationContext(), mHandler);
        IntentFilter filter = new IntentFilter();
        filter.addAction(C.PUSH_ACTION);
        filter.addAction(CONNECTIVITY_ACTION);
        filter.setPriority(Integer.MAX_VALUE);
        registerReceiver(mReceiver, filter);
        IntentFilter privatefilter = new IntentFilter();
        privatefilter.addAction(C.INNER_ACTION);
        LocalBroadcastManager.getInstance(this).registerReceiver(mReceiver, privatefilter);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return service;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy() {
        mHandler.removeCallbacksAndMessages(null);
        unregisterReceiver(mReceiver);
        if (service != null)
            service.destroy();
        super.onDestroy();
    }

    //动态广播
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle bundle;
            String string;
            String action;
            if (intent != null && (action = intent.getAction()) != null) {
                if(action.equals(CONNECTIVITY_ACTION)){
                    if(NetStateUtils.isWifi(context)){
                        LogUtils.d("wifi connected");
                        service.netChanged(false);
                    }else if(NetStateUtils.is3G(context)){
                        LogUtils.d("3g connected");
                        mHandler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                service.netChanged(true);
                            }
                        }, 2000);
                    }
                } else if(action.equals(C.PUSH_ACTION)){
                    if ((bundle = intent.getExtras()) != null) {
                        if((string = bundle.getString("msg")) != null){
                            LogUtils.d("打印服务收到推送消息:"+string);
                            service.handlePushMessage(string);
                        }
                    }
                } else if(action.equals(C.INNER_ACTION)){
                    if((bundle = intent.getExtras()) != null){
                        if((string = bundle.getString("set")) != null){
                            LogUtils.d("打印服务收到设置消息:"+string);
                            service.handleSettingMessage(string);
                        }
                    }
                }
            }
        }

    };

    private Handler mHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            if (msg.what == 1) {//检查打印机状态来自接口
                doPrinterEvent((Integer) msg.obj, false);
            } else if (msg.what == 2) {//更新成功
                doPrinterEvent(506, true);
            } else if (msg.what == 3) {//更新失败
                doPrinterEvent(507, true);
            } else if (msg.what == 4) {//开始更新
                doPrinterEvent(0, true);
            } else if (msg.what == 5) {
                doPrinterEvent(10, true);
            } else if (msg.what == 6) {
                doPrinterEvent(505, true);
            } else if (msg.what == 7) {//检查打印机状态实时上报
                doPrinterEvent((Integer) msg.obj, true);
            }
            return false;
        }
    });

    /**
     * 打印机状态事件处理
     * @param event     打印机触发事件
     * @param isSend    是否过滤相关事件
     */
    private void doPrinterEvent(int event, boolean isSend) {
        Intent i;
        String code = null;
        switch (event) {
            case 0:
                code = C.FIRMWARE_UPDATING_ACTION;
                break;
            case 2://打印机未工作
                code = C.INIT_ACTION;
                break;
            case 3:
                code = C.ERROR_ACTION;
                break;
            case 4:
                code = C.OUT_OF_PAPER_ACTION;
                break;
            case 5:
                code = C.OVER_HEATING_ACTION;
                break;
            case 6:
                code = C.COVER_OPEN_ACTION;
                break;
            case 7:
                code = C.KNIFE_ERROR_1_ACTION;
                break;
            case 8:
                code = C.KNIFE_ERROR_2_ACTION;
                break;
            case 9:
                code = C.BLACKLABEL_NON_EXISTENT_ACTION;
                break;
            case 10:
                if(isNeedDelay){
                    isNeedDelay = false;
                    mHandler.sendMessageDelayed(mHandler.obtainMessage(5), 1000);
                }else{
                    isNeedDelay = true;
                    code = C.COVER_ERROR_ACTION;
                }
                break;
            case 505:
                code = C.PRINTER_NON_EXISTENT_ACTION;
                break;
            case 506:
                code = C.FIRMWARE_FINISH_ACTION;
                break;
            case 507:
                code = C.FIRMWARE_FAILURE_ACTION;
                break;
            default:
                if (isSend){
                    code = C.NORMAL_ACTION;
                    if(!isNeedDelay){
                        mHandler.removeMessages(5);
                    }
                }
                break;
        }
        if (code != null) {
            LogUtils.d("sendBroadCast:"+code);
            i = new Intent(code);
            i.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
            sendBroadcast(i);
            if(event != 2){
                showDialog(code);
            }
        }
    }

    /**
     * 展示报警dialog
     * @param code 事件
     */
    private void showDialog(String code) {
        if (!isSettingFinish()) return;
        switch (code) {
            case C.NORMAL_ACTION:
                OneBtnDialogManager.getInstance().dissmissDialog();
                break;
            case C.OUT_OF_PAPER_ACTION:
                Spanned smallTxt = Html.fromHtml(getResources().getString(R.string.no_print_paper_small_txt));
                OneBtnDialogManager.getInstance().showNotification(this);
                OneBtnDialogManager.getInstance().show(this, C.OUT_OF_PAPER_ACTION,
                        getResources().getString(R.string.no_print_paper), smallTxt, R.drawable.v5_icon_out_paper,
                        getResources().getString(R.string.ok));
                break;

            case C.COVER_OPEN_ACTION:
                OneBtnDialogManager.getInstance().show(this, C.COVER_OPEN_ACTION,
                        getResources().getString(R.string.close_paper_warehouse), null, R.drawable.t1_close_paper_warehouse,
                        getResources().getString(R.string.ok));
                break;

            case C.OVER_HEATING_ACTION:
                OneBtnDialogManager.getInstance().show(this, C.OVER_HEATING_ACTION,
                        getResources().getString(R.string.wait_cool), null, R.drawable.v5_icon_hot,
                        getResources().getString(R.string.ok));
                break;

            case C.NORMAL_HEATING_ACTION:
                OneBtnDialogManager.getInstance().show(this, C.NORMAL_HEATING_ACTION,
                        getResources().getString(R.string.ok_cool), null, R.drawable.v5_icon_success,
                        getResources().getString(R.string.ok));
                break;

            case C.FIRMWARE_UPDATING_ACTION:
                OneBtnDialogManager.getInstance().show(this, C.FIRMWARE_UPDATING_ACTION,
                        getResources().getString(R.string.start_upgrade), null, R.drawable.update_print_wait,
                        getResources().getString(R.string.ok));
                break;

            case C.FIRMWARE_FINISH_ACTION:
                OneBtnDialogManager.getInstance().show(this, C.FIRMWARE_FINISH_ACTION,
                        getResources().getString(R.string.ok_upgrade), null, R.drawable.update_print_success,
                        getResources().getString(R.string.ok));
                break;

            case C.FIRMWARE_FAILURE_ACTION:
                OneBtnDialogManager.getInstance().show(this, C.FIRMWARE_FINISH_ACTION,
                        getResources().getString(R.string.fail_upgrade), null, R.drawable.update_print_fail,
                        getResources().getString(R.string.ok));
                break;
            case C.KNIFE_ERROR_1_ACTION:
                Spanned cutterErrorTxt = Html.fromHtml(getResources().getString(R.string.cutter_error_small_txt));
                OneBtnDialogManager.getInstance().show(this, C.KNIFE_ERROR_1_ACTION,
                        getResources().getString(R.string.cutter_error_txt), cutterErrorTxt, R.drawable.t1_cutter_error,
                        getResources().getString(R.string.ok));
                break;
            case C.KNIFE_ERROR_2_ACTION:
                OneBtnDialogManager.getInstance().show(this, C.KNIFE_ERROR_2_ACTION,
                        getResources().getString(R.string.cutter_ok_txt), null, R.drawable.t1_cutter_ok,
                        getResources().getString(R.string.ok));
                break;

            case C.COVER_ERROR_ACTION:
                OneBtnDialogManager.getInstance().show(this, C.COVER_ERROR_ACTION,
                        getResources().getString(R.string.not_completely_closed), null, R.drawable.t1_close_paper_warehouse,
                        getResources().getString(R.string.ok));
                break;
            case C.PRINTER_NON_EXISTENT_ACTION:
                OneBtnDialogManager.getInstance().show(this, C.PRINTER_NON_EXISTENT_ACTION,
                        getResources().getString(R.string.no_paper), null, R.drawable.v5_icon_hot,
                        getResources().getString(R.string.ok));
                break;
            case C.BLACKLABEL_NON_EXISTENT_ACTION:
                OneBtnDialogManager.getInstance().show(this, C.BLACKLABEL_NON_EXISTENT_ACTION,
                        getResources().getString(R.string.no_blacklabel), null, R.drawable.v5_icon_hot,
                        getResources().getString(R.string.ok));
                break;
        }
    }

    /**
     * welcome是否结束
     * @return true 结束 false  未结束
     */
    private boolean isSettingFinish() {
        // welcome是否结束
        int welcomeFinished = Settings.Global.getInt(getContentResolver(), "device_provisioned", -1);

        if (welcomeFinished != 0) {
            String currentLauncher = android.provider.Settings.Global.getString(getContentResolver(), "custom_launcher");
            return !(!TextUtils.isEmpty(currentLauncher) && (currentLauncher.equals("com.woyou.channelservice") || currentLauncher.equals("woyou.market")));
        }
        return false;
    }
}
