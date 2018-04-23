package com.sunmi.printerservice.net;

import android.os.Build;
import android.os.SystemProperties;

import com.sunmi.printerservice.entity.BlackLabelEntity;
import com.sunmi.printerservice.entity.GlobalStyle;
import com.sunmi.printerservice.entity.MD5Req;
import com.sunmi.printerservice.entity.MachineInfo;
import com.sunmi.printerservice.entity.PrintDisCutterTimesBoxTimesReq;
import com.sunmi.printerservice.entity.PrintDisCutterTimesBoxTimesRes;
import com.sunmi.printerservice.entity.Result;

import java.math.BigDecimal;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Created by Administrator on 2017/8/30.
 */

public class NetServer {
    private NetServerInterface netServerInterface;

    private NetServer(){
        RetrofitWrapper mRetrofitWrapper = RetrofitWrapper.getInstance();
        netServerInterface = mRetrofitWrapper.getNetService(NetServerInterface.class);
    }

    private static class NetServerContainer{
        private static NetServer instance = new NetServer();
    }

    public static NetServer getInstance() {
        return NetServerContainer.instance;
    }

    public void getBlackLabelInfo(Callback<Result<BlackLabelEntity>> callback) {
        MachineInfo machineInfo = new MachineInfo(Build.MODEL, SystemProperties.get("ro.serialno"));
        MD5Req<MachineInfo> req = new MD5Req<>(machineInfo);
        Call<Result<BlackLabelEntity>> call = netServerInterface.getBlackLabel(req.jsonParams, req.isEncrypted, req.timeStamp, req.randomNum, req.sign);
        call.enqueue(callback);
    }

    public void queryData(Callback<Result<PrintDisCutterTimesBoxTimesRes>> callback) {
        MachineInfo machineInfo = new MachineInfo(Build.MODEL, SystemProperties.get("ro.serialno"));
        MD5Req<MachineInfo> req = new MD5Req<>(machineInfo);
        Call<Result<PrintDisCutterTimesBoxTimesRes>> call = netServerInterface.queryData(req.jsonParams, req.isEncrypted, req.timeStamp, req.randomNum, req.sign);
        call.enqueue(callback);
    }

    public PrintDisCutterTimesBoxTimesRes queryData() throws Exception {
        MachineInfo machineInfo = new MachineInfo(Build.MODEL, SystemProperties.get("ro.serialno"));
        MD5Req<MachineInfo> req = new MD5Req<>(machineInfo);
        Call<Result<PrintDisCutterTimesBoxTimesRes>> call = netServerInterface.queryData(req.jsonParams, req.isEncrypted, req.timeStamp, req.randomNum, req.sign);
        Response<Result<PrintDisCutterTimesBoxTimesRes>> response = call.execute();
        return response.body().getData();
    }

    @SuppressWarnings("rawtypes")
    public Result uploadData(int distance, int cut, int open) throws Exception {
        PrintDisCutterTimesBoxTimesReq mPrintDisCutterTimesBoxTimesReq = new PrintDisCutterTimesBoxTimesReq(SystemProperties.get("ro.serialno"), Build.MODEL, new BigDecimal((distance/1000.0)), cut, open);
        MD5Req<PrintDisCutterTimesBoxTimesReq> md5Req = new MD5Req<>(mPrintDisCutterTimesBoxTimesReq);
        Call<Result> call = netServerInterface.uploadData(md5Req.jsonParams, md5Req.isEncrypted, md5Req.timeStamp, md5Req.randomNum,md5Req.sign);
        Response<Result> response = call.execute();
        return response.body();
    }

    public void getStyleSettings(Callback<Result<GlobalStyle>> callback) {
        MachineInfo machineInfo = new MachineInfo(Build.MODEL, SystemProperties.get("ro.serialno"));
        MD5Req<MachineInfo> req = new MD5Req<>(machineInfo);
        Call<Result<GlobalStyle>> call = netServerInterface.getStyleSettings(req.jsonParams, req.isEncrypted, req.timeStamp, req.randomNum, req.sign);
        call.enqueue(callback);
    }
}
