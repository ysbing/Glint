package com.ysbing.glint.base;

import android.support.annotation.CallSuper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.lang.reflect.Type;
import java.util.Map;
import java.util.TreeMap;

import okhttp3.OkHttpClient;

/**
 * 自定义网络模块
 *
 * @author ysbing
 */
public abstract class BaseHttpModule {
    /**
     * 创建OkHttpClient的额外配置
     *
     * @param clientType 类型，0为http普通请求，1为上传，2为下载
     * @param builder    OkHttpClient配置
     * @return OkHttpClient配置
     */
    public OkHttpClient.Builder onOkHttpBuildCreate(@NonNull Glint.GlintType clientType, @NonNull OkHttpClient.Builder builder) {
        return builder;
    }

    @CallSuper
    public <E extends BaseHttpModule> void configDefaultBuilder(@NonNull GlintBaseBuilder<E> builder) {
    }

    /**
     * @param builder 构造完毕的builder，该builder是克隆出来的，对它修改不影响原builder
     * @param <E>     BaseHttpModule子类
     * @throws Exception 未知异常
     */
    public <E extends BaseHttpModule> void onBuilderCreated(@NonNull GlintBaseBuilder<E> builder) throws Exception {
    }

    public UrlResult getUrl(@NonNull String originalUrl) throws Exception {
        return new UrlResult(originalUrl, true);
    }

    public boolean getParams(@NonNull TreeMap<String, String> originalParams) throws Exception {
        return true;
    }

    public boolean getParams(@NonNull TreeMap<String, String> originalParams, @Nullable JsonObject originalJsonParams) throws Exception {
        return true;
    }

    public boolean getHeaders(@NonNull Map<String, String> originalHeader) throws Exception {
        return true;
    }

    public <T> boolean customDeserialize(@NonNull GlintResultBean<T> result, @NonNull JsonObject jsonObj, @NonNull Gson gson, @NonNull Type typeOfT) throws Exception {
        return true;
    }

    public class UrlResult {
        public final String url;
        public final boolean transitive;

        public UrlResult(@NonNull String url, boolean transitive) {
            this.url = url;
            this.transitive = transitive;
        }
    }
}