package com.sunmi.printerservice.net;

import com.sunmi.printerservice.entity.BlackLabelEntity;
import com.sunmi.printerservice.entity.GlobalStyle;
import com.sunmi.printerservice.entity.PrintDisCutterTimesBoxTimesRes;
import com.sunmi.printerservice.entity.Result;

import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.POST;

/**
 * Created by Administrator on 2017/8/30.
 */

interface NetServerInterface {

    @FormUrlEncoded
    @POST("/api/hardware/app/setting/1.0/?service=/getblacklabel")
    Call<Result<BlackLabelEntity>> getBlackLabel(@Field("params") String params,
                                                 @Field("isEncrypted") String isEncrypted,
                                                 @Field("timeStamp") String timeStamp,
                                                 @Field("randomNum") String randomNum,
                                                 @Field("sign") String sign);

    @FormUrlEncoded
    @POST("/api/hardware/app/monitor/1.2/?service=/querydata")
    Call<Result<PrintDisCutterTimesBoxTimesRes>> queryData(@Field("params") String params,
                                                           @Field("isEncrypted") String isEncrypted,
                                                           @Field("timeStamp") String timeStamp,
                                                           @Field("randomNum") String randomNum,
                                                           @Field("sign") String sign);

    @SuppressWarnings("rawtypes")
    @FormUrlEncoded
    @POST("/api/hardware/app/monitor/1.2/?service=/uploaddata")
    Call<Result> uploadData(@Field("params") String params,
                            @Field("isEncrypted") String isEncrypted,
                            @Field("timeStamp") String timeStamp,
                            @Field("randomNum") String randomNum,
                            @Field("sign") String sign);

    @FormUrlEncoded
    @POST("/api/hardware/app/setting/1.0/?service=/getwormlist")
    Call<Result<GlobalStyle>> getStyleSettings(@Field("params") String params,
                                               @Field("isEncrypted") String isEncrypted,
                                               @Field("timeStamp") String timeStamp,
                                               @Field("randomNum") String randomNum,
                                               @Field("sign") String sign);
}
