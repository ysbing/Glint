package com.ysbing.glint.upload;

import android.os.Bundle;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.ysbing.glint.base.BaseHttpModule;

import java.io.File;
import java.util.TreeMap;

import okhttp3.Headers;

/**
 * 上传请求的入口
 * 该类不可继承，如果有自定义功能需求，就继承{@link GlintUploadCore}进行扩展
 *
 * @author ysbing
 * 创建于 2018/1/16
 */
public final class GlintUpload extends GlintUploadCore {

    public static GlintUpload upload(@NonNull String url, @NonNull File file) {
        return upload(url, file, new TreeMap<String, String>());
    }

    public static GlintUpload upload(@NonNull String url, @NonNull File file, @NonNull String keyName) {
        return upload(url, file, keyName, new TreeMap<String, String>());
    }

    public static GlintUpload upload(@NonNull String url, @NonNull File file, @NonNull TreeMap<String, String> params) {
        return upload(url, file, file.getName(), params);
    }

    public static GlintUpload upload(@NonNull String url, @NonNull File file, @NonNull String keyName, @NonNull TreeMap<String, String> params) {
        return new GlintUpload(url, file, null, keyName, params);
    }

    public static GlintUpload upload(@NonNull String url, @NonNull byte[] data, @NonNull String keyName) {
        return upload(url, data, keyName, new TreeMap<String, String>());
    }

    public static GlintUpload upload(@NonNull String url, @NonNull byte[] data, @NonNull String keyName, @NonNull TreeMap<String, String> params) {
        return new GlintUpload(url, null, data, keyName, params);
    }


    public GlintUpload(@NonNull String url, @Nullable File file, @Nullable byte[] data, @Nullable String keyName, @NonNull TreeMap<String, String> params) {
        mBuilder = createBuilder();
        mBuilder.url = url;
        mBuilder.file = file;
        mBuilder.data = data;
        mBuilder.keyName = keyName;
        mBuilder.params = params;
        if (mBuilder.file != null && TextUtils.isEmpty(mBuilder.keyName)) {
            mBuilder.keyName = mBuilder.file.getName();
        } else if (this.mBuilder == null && TextUtils.isEmpty(mBuilder.keyName)) {
            throw new RuntimeException(mBuilder.url + ":keyName is null");
        }
    }

    /**
     * 额外设置的header,出去登录相关的
     */
    public GlintUpload setHeader(@NonNull Headers.Builder headers) {
        mBuilder.headers = headers;
        return this;
    }

    /**
     * 增加cookie到头部信息
     *
     * @param cookie 登录返回的cookie
     */
    public GlintUpload addCookie(@NonNull String cookie) {
        mBuilder.cookie = cookie;
        return this;
    }

    /**
     * 设置是否使用通用签名，默认是
     *
     * @param signature 使用通用签名
     */
    public GlintUpload signature(boolean signature) {
        mBuilder.signature = signature;
        return this;
    }

    /**
     * 设置是否使用标准化序列化，默认否
     */
    public GlintUpload standardDeserialize(boolean standardDeserialize) {
        mBuilder.standardDeserialize = standardDeserialize;
        return this;
    }

    /**
     * 设置是否使用主线程
     */
    public GlintUpload mainThread() {
        mBuilder.mainThread = true;
        return this;
    }

    /**
     * 结果不是Json
     */
    public GlintUpload notJson(boolean notJson) {
        mBuilder.notJson = notJson;
        return this;
    }

    /**
     * 设置文件的协议
     *
     * @param mimeType 文件协议
     */
    public GlintUpload setMimeType(String mimeType) {
        mBuilder.mimeType = mimeType;
        return this;
    }

    /**
     * 使用自定义Module，可做高级操作
     * 该方法将会重置builder，使用时需在第一个使用
     *
     * @param module 自定义Module
     */
    public GlintUpload using(@NonNull BaseHttpModule module) {
        super.moduleUsing(module);
        return this;
    }


    /**
     * 设置其他配置
     *
     * @param otherBuilder 其他配置
     */
    public GlintUpload otherBuilder(@NonNull Bundle otherBuilder) {
        mBuilder.otherBuilder = otherBuilder;
        return this;
    }

    /**
     * 上传中途取消
     */
    public synchronized void cancel() {
        GlintUploadDispatcher.getInstance().cancel(this);
    }

    /**
     * 执行网络请求
     */
    public void execute() {
        if (!TextUtils.isEmpty(mBuilder.url)) {
            GlintUploadDispatcher.getInstance().executed(this);
        }
    }

    public <T> void execute(@NonNull GlintUploadListener<T> listener) {
        if (!TextUtils.isEmpty(mBuilder.url)) {
            mBuilder.listener = listener;
            GlintUploadDispatcher.getInstance().executed(this);
        }
    }
}
