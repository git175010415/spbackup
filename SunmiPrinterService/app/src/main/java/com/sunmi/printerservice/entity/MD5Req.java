package com.sunmi.printerservice.entity;

import android.text.TextUtils;

import com.google.gson.Gson;
import com.sunmi.printerservice.utils.DateUtils;
import com.sunmi.printerservice.utils.DesUtils;
import com.sunmi.printerservice.utils.I18NUtils;
import com.sunmi.printerservice.utils.MD5Utils;

import woyou.aidlservice.jiuiv5.BuildConfig;

/**
 * 请求加密实体
 * 
 * @author longtao.li
 * 
 */
public class MD5Req<T> {

	public MD5Req(T params) {
		this.params = params;
		md5();
	}

	public MD5Req(T params, boolean isEncrypted) {
		this.params = params;
		this.isEncrypted = isEncrypted ? "1" : "0";;
		md5();
	}

	public T params; // 请求携带的数据

	public String jsonParams=""; // params的json字符串

	public String isEncrypted = BuildConfig.IS_ENCRYPTED ? "1" : "0"; // 是否加密

	public String timeStamp=""; // 时间戳

	public String randomNum = ""; // 随机数(6位)

	public String sign=""; // md5加密后的字符串
	public String timeZone;//时区
	public String language;//语言
	// 计算方法：
	// MD5(Parameter + IsEncrypted + TimeStamp + RandomNum + MD5(Key))
	private void md5() {
		if( TextUtils.isEmpty(timeStamp) ){
			timeStamp = DateUtils._getGMTime()+"";
		}

		while(randomNum.length()<6)
			randomNum+=(int)(Math.random()*10);
//		
		if(params != null){
			jsonParams = new Gson().toJson(params);
			jsonParams = jsonParams.replace(" ", "");
			
			if(isEncrypted.equals("1")){
				try {
					jsonParams = DesUtils.encode(jsonParams);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}


		sign = jsonParams + isEncrypted + timeStamp +randomNum + MD5Utils.md5(BuildConfig.DELIVER_KEY);
		sign = MD5Utils.md5(sign);
		timeZone = I18NUtils.getCurrentTimeZone();
		language = null;
	}
	

}
