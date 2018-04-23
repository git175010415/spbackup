package com.sunmi.printerservice.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import woyou.aidlservice.jiuiv5.WoyouService;

/**
 * Created by Administrator on 2017/7/19.
 */

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Intent mIntent = new Intent(context, WoyouService.class);
        context.startService(mIntent);
    }
}
