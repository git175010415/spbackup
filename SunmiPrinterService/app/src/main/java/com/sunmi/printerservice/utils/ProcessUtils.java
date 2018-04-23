package com.sunmi.printerservice.utils;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Build;
import android.widget.Toast;

import java.util.List;

import woyou.aidlservice.jiuiv5.R;

/**
 * 进程相关工具
 *
 * @author longtao.li
 */
public class ProcessUtils {

    /**
     * 启动一个使用指南的安装打印纸activity
     */
    public static boolean startApplication(Context context, String message) {
        Intent intent = new Intent();
        if (Build.MODEL.contains("t1")||Build.MODEL.contains("T1")) {
            intent.setPackage("com.sunmi.instructiont1");
            if (message.equals(C.OUT_OF_PAPER_ACTION)) {
                intent.setAction("activity.InstallPaperAct");
            } else if (message.equals(C.KNIFE_ERROR_1_ACTION)) {
                intent.setAction("activity.PaperJamAct");
            }
        } else {
            intent.setComponent(new ComponentName("com.woyou.instructions_v5", "com.instruction.activity.v1.V1InstallPaperAct"));
            intent.setPackage("com.woyou.instructions_v5");
            intent.putExtra("com.sunmi.paper.alarm.intent", 1);
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        try {
            context.startActivity(intent);
            return true;
        }catch (Exception e){
            Toast.makeText(context, context.getString(R.string.error_use_open), Toast.LENGTH_LONG).show();
            return false;
        }

//        if (isIntentAvailable(context, intent)) {
//            context.startActivity(intent);
//            return true;
//        } else {
//            Toast.makeText(context, context.getString(R.string.error_use_open), Toast.LENGTH_LONG).show();
//            return false;
//        }
    }

    /**
     * 判断Intent是否可用
     */
    public static boolean isIntentAvailable(Context context, Intent intent) {
        final PackageManager packageManager = context.getPackageManager();
        List<ResolveInfo> list = packageManager.queryIntentActivities(intent,
                PackageManager.GET_ACTIVITIES);
        return list.size() > 0;
    }

}
