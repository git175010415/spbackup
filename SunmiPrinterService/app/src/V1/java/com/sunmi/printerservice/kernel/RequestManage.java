package com.sunmi.printerservice.kernel;

import android.content.Context;

import com.google.gson.Gson;
import com.sunmi.printerservice.entity.GlobalStyle;
import com.sunmi.printerservice.entity.Result;
import com.sunmi.printerservice.net.NetServer;
import com.sunmi.printerservice.utils.LogUtils;
import com.sunmi.printerservice.utils.NetStateUtils;
import com.sunmi.printerservice.utils.PreferencesLoader;

import org.json.JSONException;
import org.json.JSONObject;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


public class RequestManage {
    private Context context;
    private RequestInterface requestInterface;
    private int repeate;

    RequestManage(Context context, RequestInterface requestInterface) {
        this.context = context;
        this.requestInterface = requestInterface;
        init();
    }

    public void init() {
        repeate = 0;
        updateGlobal();
        updateFont();
    }

    public void reset(boolean once){
        if(once){
            if(repeate > 3){
                return;
            }else{
                repeate++;
            }
        }
        updateGlobal();
    }

    private void updateGlobal() {
        if(!NetStateUtils.isNetworkConnected(context)){
            setGlobalStyle(null);
            return;
        }
        NetServer.getInstance().getStyleSettings(new Callback<Result<GlobalStyle>>() {
            @Override
            public void onResponse(Call<Result<GlobalStyle>> call, Response<Result<GlobalStyle>> response) {
                LogUtils.d(response.body().toString());
                GlobalStyle globalStyle;
                try{
                    globalStyle = response.body().getData();
                }catch (Exception e){
                    e.printStackTrace();
                    globalStyle = null;
                }
                setGlobalStyle(globalStyle);
            }

            @Override
            public void onFailure(Call<Result<GlobalStyle>> call, Throwable throwable) {
                setGlobalStyle(null);
            }
        });
    }

    private void updateFont() {
        PreferencesLoader pl = new PreferencesLoader(context, "settings");
        if(!pl.getBoolean("fontEnable")){
            return;
        }
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("fontDefault", pl.getBoolean("fontDefault"));
            requestInterface.setSettingStyle(jsonObject.toString());
        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /*
    *  获取当前的全局化设置
    *  关于mark设置状态：
    *  初始值即全部清除状态 -1即FFFFFFFF
    *  低 0 位 黑标设置 0已设置 1未设置
    *  低 1 位 纸张设置 0已设置 1未设置
    *  低 2 位 全局设置 0已设置 1未设置
     */
    private void setGlobalStyle(GlobalStyle style){
        PreferencesLoader pl = new PreferencesLoader(context, "settings");
        Gson gson = new Gson();
        String string;
        if(style != null){
            string = gson.toJson(style);
            pl.saveString("web_style", string);
        }

        if(0 == (pl.getInt("mark")&0x04)){
            string = pl.getString("local_style");
        }else{
            string = pl.getString("web_style");
        }

        if(string != null){
            requestInterface.setSettingStyle(string);
        }
    }
}
