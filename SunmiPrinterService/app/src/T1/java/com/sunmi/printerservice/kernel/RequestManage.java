package com.sunmi.printerservice.kernel;

import android.content.Context;

import com.sunmi.printerservice.entity.BlackLabelEntity;
import com.sunmi.printerservice.entity.Result;
import com.sunmi.printerservice.net.NetServer;
import com.sunmi.printerservice.utils.NetStateUtils;
import com.sunmi.printerservice.utils.PreferencesLoader;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RequestManage {
    private Context context;
    private RequestInterface requestInterface;
    private int repeate;

    public RequestManage(Context context, RequestInterface requestInterface) {
        this.context = context.getApplicationContext();
        this.requestInterface = requestInterface;
        init();
    }

    public void init() {
        repeate = 0;
        updateBlackLabel();
    }

    public void reset(boolean once){
        if(once){
            if(repeate > 3){
                return;
            }else{
                repeate++;
            }
        }
        updateBlackLabel();
    }

    /**
     * 获取到网络设置的黑标
     */
    private void updateBlackLabel() {
        if (NetStateUtils.isNetworkConnected(context)) {
            NetServer.getInstance().getBlackLabelInfo(new Callback<Result<BlackLabelEntity>>() {
                @Override
                public void onResponse(Call<Result<BlackLabelEntity>> call, Response<Result<BlackLabelEntity>> response) {
                    PreferencesLoader pl = new PreferencesLoader(context, "settings");
                    int print_mode = 1;
                    int value = -1;
                    try {
                        BlackLabelEntity bl = response.body().getData();
                        if (bl.getPrint_mode() == 1) {
                            pl.saveBoolean("bbm_flag", false);
                        } else {
                            pl.saveBoolean("bbm_flag", true);
                        }
                        print_mode = bl.getPrint_mode();
                        value = (int) ((bl.getCutter_location() + 17.5) / 0.125);
                        pl.saveInt("bbm_value", value);
                        pl.saveInt("web_paper", bl.getPrint_spec());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    set(print_mode, value);
                }

                @Override
                public void onFailure(Call<Result<BlackLabelEntity>> call, Throwable throwable) {
                    set(1, -1);
                }
            });
        }else{
            set(1, -1);
        }
    }

    private void set(int print_mode, int value){
        PreferencesLoader pl = new PreferencesLoader(context, "settings");
        if (0 != (pl.getInt("mark") & 0x1)) {
            requestInterface.setBlackLabel(print_mode, value);
        }
        if(0 != (pl.getInt("mark")&0x2)){
            if(pl.getInt("web_paper") == 2){
                requestInterface.getServiceValue().setPaper(384);
            }else{
                requestInterface.getServiceValue().setPaper(576);
            }
        }else{
            if(pl.getInt("local_paper") == 2){
                requestInterface.getServiceValue().setPaper(384);
            }else{
                requestInterface.getServiceValue().setPaper(576);
            }
        }
        requestInterface.getServiceValue().updateMyId();
    }
}
