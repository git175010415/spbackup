package com.sunmi.printerservice.manager;

import android.content.Context;
import android.media.MediaPlayer;

import com.sunmi.printerservice.utils.C;

import java.util.HashMap;

import woyou.aidlservice.jiuiv5.R;

/**
 * Created by Administrator on 2017/9/7.
 */

public class SoundManager {

    private static SoundManager mInstance;
    private MediaPlayer player;

    private HashMap<String, Integer> soundMap;

    public static SoundManager getInstance() {
        if (mInstance == null) {
            mInstance = new SoundManager();
        }
        return mInstance;
    }


    private SoundManager() {
        soundMap = new HashMap<>();
        soundMap.put(C.OUT_OF_PAPER_ACTION, R.raw.paper_not_ready);
        //soundMap.put(C.COVER_OPEN_ACTION, R.raw.paper_not_ready);
        soundMap.put(C.OVER_HEATING_ACTION, R.raw.over_heat);       // 过热
        soundMap.put(C.NORMAL_HEATING_ACTION, R.raw.ready_cold);    // 冷却OK
        soundMap.put(C.FIRMWARE_UPDATING_ACTION, R.raw.updating);
        soundMap.put(C.FIRMWARE_FINISH_ACTION, R.raw.update_finsh);
        soundMap.put(C.FIRMWARE_FAILURE_ACTION, R.raw.update_finsh);
        soundMap.put(C.COVER_OPEN_ACTION,R.raw.close_warehouse);
        soundMap.put(C.COVER_ERROR_ACTION,R.raw.cover_error);
        soundMap.put(C.KNIFE_ERROR_1_ACTION,R.raw.cutter_error);
        soundMap.put(C.KNIFE_ERROR_2_ACTION,R.raw.cutter_ok);

        soundMap.put(C.OUT_OF_PAPER_ACTION_EN, R.raw.paper_not_ready_en);
        //soundMap.put(C.COVER_OPEN_ACTION_EN, R.raw.paper_not_ready_en);
        soundMap.put(C.OVER_HEATING_ACTION_EN, R.raw.over_heat_en);     // 过热
        soundMap.put(C.NORMAL_HEATING_ACTION_EN, R.raw.ready_cold_en);  // 冷却OK
        soundMap.put(C.FIRMWARE_UPDATING_ACTION_EN, R.raw.updating_en);
        soundMap.put(C.FIRMWARE_FINISH_ACTION_EN, R.raw.update_finsh_en);
        soundMap.put(C.FIRMWARE_FAILURE_ACTION_EN, R.raw.update_finsh_en);
        soundMap.put(C.COVER_OPEN_ACTION_EN,R.raw.close_warehouse_en);
        soundMap.put(C.COVER_ERROR_ACTION_EN,R.raw.cover_error_en);
        soundMap.put(C.KNIFE_ERROR_1_ACTION_EN,R.raw.cutter_error_en);
        soundMap.put(C.KNIFE_ERROR_2_ACTION_EN,R.raw.cutter_ok_en);

        soundMap.put(C.HOT_JP,R.raw.hot_jp);
        soundMap.put(C.NO_PAPER_JP,R.raw.no_paper_jp);
        soundMap.put(C.UPDATE_JP,R.raw.upda_jp);

        soundMap.put(C.HOT_RU,R.raw.over_heat_ru);
        soundMap.put(C.NO_PAPER_RU,R.raw.paper_not_ready_ru);
    }

    //播放声音
    public void playSound(Context context, String sound, MediaPlayer.OnCompletionListener listener) {
        if (player != null && player.isPlaying()) {
            return;
        }
        player = MediaPlayer.create(context, soundMap.get(sound));
        player.setLooping(false); // 不循环播放
        player.start();
        player.setOnCompletionListener(listener);
    }

    /**
     * 停止播放
     */
    public void stopPlayer(){
        if(player != null){
            player.reset();
            player.release();
            player = null;
        }
    }
}
