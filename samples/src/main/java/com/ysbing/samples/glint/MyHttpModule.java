package com.ysbing.samples.glint;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.ysbing.glint.base.BaseHttpModule;
import com.ysbing.glint.base.Glint;
import com.ysbing.glint.base.GlintBaseBuilder;
import com.ysbing.glint.base.GlintResultBean;
import com.ysbing.glint.util.GlintRequestUtil;

import java.lang.reflect.Type;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;

public class MyHttpModule extends BaseHttpModule {

    public static MyHttpModule get() {
        return new MyHttpModule();
    }

    @Override
    public OkHttpClient.Builder onOkHttpBuildCreate(@NonNull Glint.GlintType clientType, @NonNull OkHttpClient.Builder builder) {
        return builder.readTimeout(3000L, TimeUnit.MILLISECONDS)
                .writeTimeout(5000L, TimeUnit.MILLISECONDS);
    }

    @Override
    public <E extends BaseHttpModule> void configDefaultBuilder(@NonNull GlintBaseBuilder<E> builder) {
        super.configDefaultBuilder(builder);
    }

    @Override
    public boolean getHeaders(@NonNull Map<String, String> originalHeader) throws Exception {
        return super.getHeaders(originalHeader);
    }

    @Override
    public boolean getParams(@NonNull TreeMap<String, String> originalParams) throws Exception {
        return super.getParams(originalParams);
    }

    @Override
    public boolean getParams(@NonNull TreeMap<String, String> originalParams, @Nullable JsonObject originalJsonParams) throws Exception {
        return super.getParams(originalParams, originalJsonParams);
    }

    @Override
    public UrlResult getUrl(@NonNull String originalUrl) throws Exception {
        return super.getUrl(originalUrl);
    }

    @Override
    public <T> boolean customDeserialize(@NonNull GlintResultBean<T> result, @NonNull JsonObject jsonObj, @NonNull Gson gson, @NonNull Type typeOfT) throws Exception {
        JsonElement statusElement = jsonObj.get("status");
        JsonElement messageElement = jsonObj.get("message");
        JsonElement dataElement = jsonObj.get("data");
        // 如果为空，可能是标准的json，不用判断状态码，直接解析
        if (statusElement == null) {
            result.setRunStatus(Glint.ResultStatus.STATUS_NORMAL);
            result.setData(GlintRequestUtil.<T>standardDeserialize(gson, jsonObj, typeOfT));
        } else {
            // status节点，这里判断出是否请求成功
            int status = statusElement.getAsInt();
            if (messageElement != null) {
                // message节点
                String message = messageElement.getAsString();
                result.setMessage(message);
            }
            result.setStatus(status);
            if (status == 200) {
                result.setRunStatus(Glint.ResultStatus.STATUS_SUCCESS);
                if (dataElement != null) {
                    result.setData(GlintRequestUtil.<T>successDeserialize(gson, dataElement, typeOfT));
                }
            } else {
                result.setRunStatus(Glint.ResultStatus.STATUS_ERROR);
            }
        }
        return false;
    }
}
