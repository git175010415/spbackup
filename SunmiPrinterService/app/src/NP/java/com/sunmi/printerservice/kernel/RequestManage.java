package com.sunmi.printerservice.kernel;

import android.content.Context;

import com.sunmi.printerservice.entity.PrintDisCutterTimesBoxTimesRes;
import com.sunmi.printerservice.entity.Result;
import com.sunmi.printerservice.net.NetServer;
import com.sunmi.printerservice.utils.LogUtils;
import com.sunmi.printerservice.utils.NetStateUtils;
import com.sunmi.printerservice.utils.PreferencesLoader;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RequestManage {
    private Context context;
    private RequestInterface requestInterface;
    private TimedUploadRunable timedUpload;
    private int repeate;

    public RequestManage(Context context, RequestInterface requestInterface) {
        this.context = context.getApplicationContext();
        this.requestInterface = requestInterface;
        init();
    }

    /**
     * 更新打印距离、切刀次数和钱箱打开次数统计
     */
    private void updateDCB(){
        if (NetStateUtils.isNetworkConnected(context)) {
            NetServer.getInstance().queryData(new Callback<Result<PrintDisCutterTimesBoxTimesRes>>() {
                @Override
                public void onResponse(Call<Result<PrintDisCutterTimesBoxTimesRes>> call, Response<Result<PrintDisCutterTimesBoxTimesRes>> response) {
                    try {
                        PrintDisCutterTimesBoxTimesRes res = response.body().getData();
                        if (res.getPrinterDis() > requestInterface.getServiceValue().getDistance()) {
                            requestInterface.getServiceValue().setDistance(res.getPrinterDis());
                        }
                        if (res.getCutterTimes() > requestInterface.getServiceValue().getCuts()) {
                            requestInterface.getServiceValue().setCuts(res.getCutterTimes());
                        }
                        if (res.getMoneyBoxTimes() > requestInterface.getServiceValue().getOpens()) {
                            requestInterface.getServiceValue().setOpens(res.getMoneyBoxTimes());
                        }
                    } catch (Exception e) {
                        LogUtils.d(e.getMessage());
                    }
                }

                @Override
                public void onFailure(Call<Result<PrintDisCutterTimesBoxTimesRes>> call, Throwable throwable) {
                    LogUtils.d(throwable.getLocalizedMessage());
                }
            });
        }
    }

    /**
     * 初始化打印距离、切刀次数和钱箱打开次数统计
     */
    private void initDCB() {
        int distance, cuttings, opens;
        PreferencesLoader pl = new PreferencesLoader(context, "settings");
        distance = pl.getInt("printer_length");
        cuttings = pl.getInt("printer_cut");
        opens = pl.getInt("printer_open");
        if (distance < 0 || cuttings < 0 || opens < 0) {
            if (NetStateUtils.isNetworkConnected(context)) {
                NetServer.getInstance().queryData(new Callback<Result<PrintDisCutterTimesBoxTimesRes>>() {
                    @Override
                    public void onResponse(Call<Result<PrintDisCutterTimesBoxTimesRes>> call, Response<Result<PrintDisCutterTimesBoxTimesRes>> response) {
                        try {
                            PrintDisCutterTimesBoxTimesRes res = response.body().getData();
                            if(res.getPrinterDis() > 0){
                                requestInterface.getServiceValue().setDistance(res.getPrinterDis());
                            }
                            if(res.getCutterTimes() > 0){
                                requestInterface.getServiceValue().setCuts(res.getCutterTimes());
                            }
                            if(res.getMoneyBoxTimes() > 0){
                                requestInterface.getServiceValue().setOpens(res.getMoneyBoxTimes());
                            }
                        } catch (Exception e) {
                            LogUtils.d(e.getMessage());
                        }
                    }

                    @Override
                    public void onFailure(Call<Result<PrintDisCutterTimesBoxTimesRes>> call, Throwable throwable) {
                        LogUtils.d(throwable.getLocalizedMessage());
                    }
                });
            }
        } else {
            requestInterface.getServiceValue().setDistance(distance);
            requestInterface.getServiceValue().setCuts(cuttings);
            requestInterface.getServiceValue().setOpens(opens);
        }
    }

    public void init() {
        repeate = 0;
        initDCB();
        timedUpload = new TimedUploadRunable(requestInterface);
        timedUpload.start();
    }

    public void reset(boolean once){
        if(once){
            if(repeate > 3){
                return;
            }else{
                repeate++;
            }
        }
        updateDCB();
    }

    void kill() {
        timedUpload.kill();
    }
}
