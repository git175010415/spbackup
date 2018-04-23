package com.sunmi.printerservice.net;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import woyou.aidlservice.jiuiv5.BuildConfig;

/**
 * 网络接口服务(Service)的包装类
 */
public class RetrofitWrapper {

    private static RetrofitWrapper instance;
    private RetrofitWrapper() {
    }

    public static RetrofitWrapper getInstance(){
        if( instance == null ){
            synchronized (RetrofitWrapper.class){
                if(instance == null)
                    instance = new RetrofitWrapper();
            }
        }
        return instance;
    }

    public <T> T getNetService(Class<T> clazz) {
        String url = BuildConfig.API_HOST;
        return getNetService(clazz, url);
    }


    public <T> T getNetService(Class<T> clazz, String url) {
        OkHttpClient client = new OkHttpClient.Builder().
                connectTimeout(5, TimeUnit.SECONDS).
                readTimeout(5, TimeUnit.SECONDS).
                writeTimeout(5, TimeUnit.SECONDS).
                retryOnConnectionFailure(false).
                build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(url)
                .addConverterFactory(GsonConverterFactory.create())
                .client(client)
                .build();
        return retrofit.create(clazz);
    }
}
