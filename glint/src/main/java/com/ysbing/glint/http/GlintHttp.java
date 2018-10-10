package com.ysbing.glint.http;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.ysbing.glint.base.BaseHttpModule;
import com.ysbing.glint.base.GlintResultBean;
import com.ysbing.glint.util.GlintRequestUtil;

import java.lang.reflect.Type;
import java.util.Map;
import java.util.TreeMap;

/**
 * 接口请求的入口
 * 该类不可继承，如果有自定义功能需求，就继承{@link GlintHttpCore}进行扩展
 *
 * @author ysbing
 */
public final class GlintHttp extends GlintHttpCore {

    public static GlintHttp get(@NonNull String url) {
        return get(url, new TreeMap<String, String>());
    }

    public static GlintHttp get(@NonNull String url, @NonNull TreeMap<String, String> params) {
        return new GlintHttp(Method.GET, url, params, null);
    }

    public static GlintHttp post(@NonNull String url) {
        return post(url, new TreeMap<String, String>());
    }

    public static GlintHttp post(@NonNull String url, @NonNull TreeMap<String, String> params) {
        return new GlintHttp(Method.POST, url, params, null);
    }

    public static GlintHttp post(@NonNull String url, @NonNull JsonObject jsonParams) {
        return new GlintHttp(Method.POST, url, new TreeMap<String, String>(), jsonParams).setMimeType("application/json; charset=utf-8");
    }

    public GlintHttp(int method, @NonNull String url) {
        this(method, url, new TreeMap<String, String>(), null);
    }

    public GlintHttp(int method, @NonNull String url, @NonNull TreeMap<String, String> params, @Nullable JsonObject jsonParams) {
        mBuilder = createBuilder();
        mBuilder.method = method;
        mBuilder.url = url;
        mBuilder.params = params;
        mBuilder.jsonParams = jsonParams;
    }

    /**
     * 额外设置的header,出去登录相关的
     */
    public GlintHttp setHeader(@NonNull Map<String, String> header) {
        mBuilder.header = header;
        return this;
    }

    /**
     * 增加cookie到头部信息
     *
     * @param cookie 登录返回的cookie
     */
    public GlintHttp addCookie(@NonNull String cookie) {
        mBuilder.cookie = cookie;
        return this;
    }

    /**
     * 设置是否使用通用签名，默认是
     *
     * @param signature 使用通用签名
     */
    public GlintHttp signature(boolean signature) {
        mBuilder.signature = signature;
        return this;
    }

    /**
     * 设置是否使用标准化序列化，默认否
     */
    public GlintHttp standardDeserialize(boolean standardDeserialize) {
        mBuilder.standardDeserialize = standardDeserialize;
        return this;
    }

    /**
     * 默认重试20次
     *
     * @param retryOnConnectionFailure 失败后重试，默认是true
     */
    public GlintHttp retryOnConnectionFailure(boolean retryOnConnectionFailure) {
        mBuilder.retryOnConnectionFailure = retryOnConnectionFailure;
        return this;
    }

    /**
     * 设置是否使用主线程
     *
     * @param mainThread 是否使用主线程
     */
    public GlintHttp mainThread(boolean mainThread) {
        mBuilder.mainThread = mainThread;
        return this;
    }

    /**
     * 结果不是Json
     */
    public GlintHttp notJson(boolean notJson) {
        mBuilder.notJson = notJson;
        return this;
    }

    /**
     * 设置其他配置
     *
     * @param otherBuilder 其他配置
     */
    public GlintHttp otherBuilder(@NonNull Bundle otherBuilder) {
        mBuilder.otherBuilder = otherBuilder;
        return this;
    }

    /**
     * 设置文件的协议
     *
     * @param mimeType 文件协议
     */
    public GlintHttp setMimeType(String mimeType) {
        mBuilder.mimeType = mimeType;
        return this;
    }

    /**
     * 使用自定义Module，可做高级操作
     *
     * @param module 自定义Module
     */
    public GlintHttp using(@NonNull BaseHttpModule module) {
        super.moduleUsing(module);
        return this;
    }

    /**
     * 重试策略
     * <p>
     * 保存当前请求
     * 错误回调
     * 失败回调
     * <p>
     * 重试次数
     * 重试间隔时间
     * 支持时间迭代
     */
    public GlintHttp retry(@NonNull GlintHttpRetry retry) {
        mBuilder.retry = retry;
        mBuilder.baseRetry = retry;
        return this;
    }

    /**
     * 已有智能生命周期取消网络请求，这个为额外设置的方法
     *
     * @param tag 请求标签，用于取消请求
     */
    public GlintHttp setTag(@NonNull String tag) {
        mBuilder.tag = tag.hashCode();
        return this;
    }

    public GlintHttp setTag(int tag) {
        mBuilder.tag = tag;
        return this;
    }

    /**
     * 自由的生命周期，不受该框架控制
     */
    public GlintHttp freeLife() {
        mBuilder.freeLife = true;
        return this;
    }

    /**
     * 设置缓存
     *
     * @param cacheTime 缓存时间，单位是秒，默认不缓存
     */
    public GlintHttp cache(int cacheTime) {
        mBuilder.cacheTime = cacheTime;
        return this;
    }

    /**
     * 取消请求
     */
    public synchronized void cancel() {
        GlintHttpDispatcher.getInstance().cancel(mBuilder.tag);
    }

    /**
     * 执行网络请求
     */
    public void execute() {
        if (!TextUtils.isEmpty(mBuilder.url)) {
            GlintRequestUtil.addHttpRequestTag(mBuilder);
            if (mBuilder.retry != null && mBuilder.baseRetry != null) {
                mBuilder.retry = null;
                mBuilder.baseRetry.onCreateRequest(this);
            }
            GlintHttpDispatcher.getInstance().executed(this);
        }
    }

    /**
     * 执行网络请求
     * v1.1.4 新增API，改变调用顺序，使用更加合理
     *
     * @param listener 回调
     */
    public <T> void execute(@NonNull GlintHttpListener<T> listener) {
        if (!TextUtils.isEmpty(mBuilder.url)) {
            mBuilder.listener = listener;
            GlintRequestUtil.addHttpRequestTag(mBuilder);
            if (mBuilder.retry != null && mBuilder.baseRetry != null) {
                mBuilder.retry = null;
                mBuilder.baseRetry.onCreateRequest(this);
            }
            GlintHttpDispatcher.getInstance().executed(this);
        }
    }

    /**
     * 执行同步网络请求，该方法需在子线程发起，否则会异常
     *
     * @param classOfT 泛型类
     */
    public <T> GlintResultBean<T> executeSync(@NonNull Class<T> classOfT) throws Exception {
        return executeSync(TypeToken.get(classOfT).getType());
    }

    /**
     * 执行同步网络请求，该方法需在子线程发起，否则会异常
     *
     * @param typeOfT 通用型。您可以通过使用{@link com.google.gson.reflect.TypeToken}这个类来获取
     */
    public <T> GlintResultBean<T> executeSync(@NonNull Type typeOfT) throws Exception {
        if (mBuilder.listener != null) {
            mBuilder.listener = null;
        }
        this.mTypeOfT = typeOfT;
        //noinspection unchecked
        return runSync();
    }

}