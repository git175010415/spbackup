package com.sunmi.printerservice.manager;

import android.app.Dialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.text.TextUtils;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.RemoteViews;
import android.widget.TextView;

import com.sunmi.printerservice.utils.Adaptation;
import com.sunmi.printerservice.utils.C;
import com.sunmi.printerservice.utils.LogUtils;
import com.sunmi.printerservice.utils.PreferencesLoader;
import com.sunmi.printerservice.utils.ProcessUtils;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import woyou.aidlservice.jiuiv5.R;



public class OneBtnDialogManager {
    private Map<String, Dialog> mMap;
    private static OneBtnDialogManager mInstance;
    private NotificationManager manager;

    private OneBtnDialogManager() {
        mMap = new HashMap<>();
    }

    public static OneBtnDialogManager getInstance() {
        if (mInstance == null) {
            mInstance = new OneBtnDialogManager();
        }
        return mInstance;
    }

    public void show(final Context context, final String message, CharSequence titleTxt, CharSequence smallTxt, int iconImg,
                     CharSequence okTxt) {
        Dialog dialog = mMap.get(message);
        if ((dialog != null && dialog.isShowing())) {
            return;
        } else {
            Collection<Dialog> dialogs = mMap.values();
            for (Dialog dialog1 : dialogs) {
                dialog1.dismiss();
            }
        }
        mMap.clear();
        final Dialog mDialog = new Dialog(context, R.style.defaultDialogStyle);
        if (Adaptation.proportion == Adaptation.SCREEN_4_3 || Adaptation.proportion == Adaptation.SCREEN_16_9) {
            mDialog.setContentView(R.layout.t1_dialog_one_button);
        } else {
            mDialog.setContentView(R.layout.v1_dialog_one_button);
        }

        mDialog.setCancelable(false);
        ImageView imgIcon = (ImageView) mDialog.findViewById(R.id.imageView_icon);
        if (iconImg == -1) {
            imgIcon.setVisibility(View.GONE);
        } else {
            imgIcon.setBackgroundResource(iconImg);
        }
        RelativeLayout relVoice = (RelativeLayout) mDialog.findViewById(R.id.rel_voice);
        final ImageView imgVoice = (ImageView) mDialog.findViewById(R.id.img_voice);
        setVoice(context, imgVoice);
        relVoice.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new PreferencesLoader(context, "obj").saveBoolean(C.VOICE_CACHE_KEY, !isOpenVoice(context));
                setVoice(context, imgVoice);
            }
        });
        TextView txtMsg = (TextView) mDialog.findViewById(R.id.textView_msg);
        txtMsg.setText(titleTxt);
        TextView txtOther = (TextView) mDialog.findViewById(R.id.textView_other);
        TextView txtBtn = (TextView) mDialog.findViewById(R.id.textView_btn);
        txtBtn.setText(okTxt);
        LinearLayout btn = (LinearLayout) mDialog.findViewById(R.id.linear_bottom);
        if (!TextUtils.isEmpty(smallTxt)) {
            txtOther.setVisibility(View.VISIBLE);
            txtOther.setText(smallTxt);
            txtOther.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (ProcessUtils.startApplication(context, message)) {
                        mMap.remove(message);
                        mDialog.cancel();
                        if (manager != null && message.equals(C.OUT_OF_PAPER_ACTION)) {
                            manager.cancel(1);
                        }
                    }
                }
            });
        } else {
            txtOther.setVisibility(View.GONE);
    }
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mMap.remove(message);
                mDialog.dismiss();
                if (manager != null && message.equals(C.OUT_OF_PAPER_ACTION)) {
                    manager.cancel(1);
                }
            }
        });
        if (message.equals(C.COVER_OPEN_ACTION) || message.equals(C.OUT_OF_PAPER_ACTION)) {
            if (!isOpenVoice(context)) {
                relVoice.setVisibility(View.VISIBLE);
                playVoice(context, message);
            }
        } else if(!message.equals(C.FIRMWARE_FAILURE_ACTION)){
            relVoice.setVisibility(View.GONE);
            playVoice(context, message);
        }
        try {
            Window window = mDialog.getWindow();
            if(window != null)
                window.setType(WindowManager.LayoutParams.TYPE_TOAST);
            mMap.put(message, mDialog);
            mDialog.show();
        } catch (Exception e) {
            LogUtils.e(e.getLocalizedMessage());
        }
    }

    public void dissmissDialog() {
        Collection<Dialog> dialogs = mMap.values();
        for (Dialog dialog1 : dialogs) {
            if ((dialog1 != null)) {
                dialog1.dismiss();
                SoundManager.getInstance().stopPlayer();
            }
        }
        if (manager != null) {
            manager.cancel(1);
        }
    }

    public void showNotification(Context context) {
        manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        Notification notification = new Notification();
        notification.icon = R.drawable.v5_icon_notification;
        notification.tickerText = context.getResources().getString(R.string.no_print_paper_showNotification);
        notification.when = System.currentTimeMillis();
        notification.flags = Notification.FLAG_AUTO_CANCEL;  // 不能够自动清除
        RemoteViews views;
        if (Adaptation.proportion == Adaptation.SCREEN_4_3 || Adaptation.proportion == Adaptation.SCREEN_16_9) {
            views = new RemoteViews(context.getPackageName(), R.layout.t1_notification_item);
        } else {
            views = new RemoteViews(context.getPackageName(), R.layout.v1_notification_item);
        }
        notification.contentView = views;
        // 通知消息与Intent关联
        Intent intent = new Intent();
        notification.contentIntent = PendingIntent.getActivity(context, 1, intent, Intent.FILL_IN_ACTION);
        manager.notify(1, notification);
    }

    private void setVoice(Context context, ImageView imageView) {
        if (isOpenVoice(context)) {
            imageView.setBackgroundResource(R.drawable.voice_off);
        } else {
            imageView.setBackgroundResource(R.drawable.voice_on);
        }
    }

    private boolean isOpenVoice(Context context) {
        PreferencesLoader p = new PreferencesLoader(context, "obj");
        return p.getBoolean(C.VOICE_CACHE_KEY);
    }

    private void playVoice(Context context, String message) {
        String sound;
        String language = context.getResources().getConfiguration().locale.getCountry().trim();
        if(TextUtils.equals("JP",language)){
            switch (message){
                case C.OVER_HEATING_ACTION:
                    sound = C.HOT_JP;
                    break;
                case C.OUT_OF_PAPER_ACTION:
                    sound = C.NO_PAPER_JP;
                    break;
                case C.FIRMWARE_UPDATING_ACTION:
                    sound = C.UPDATE_JP;
                    break;
                default:
                    sound = message + "_EN";
                    break;
            }
        }else if(TextUtils.equals("RU",language)){
            switch (message){
                case C.OVER_HEATING_ACTION:
                    sound = C.HOT_RU;
                    break;
                case C.OUT_OF_PAPER_ACTION:
                    sound = C.NO_PAPER_RU;
                    break;
                default:
                    sound = message + "_EN";
                    break;
            }
        }else if(!language.equals("CN") && !language.equals("TW")){
            sound = message + "_EN";
        }else{
            sound = message;
        }
        try {
            SoundManager.getInstance().playSound(context, sound, new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    SoundManager.getInstance().stopPlayer();
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
