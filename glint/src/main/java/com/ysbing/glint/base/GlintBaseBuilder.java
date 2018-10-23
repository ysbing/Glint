package com.ysbing.glint.base;

import android.os.Bundle;
import android.support.annotation.NonNull;

import com.google.gson.JsonObject;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * 请求基础参数配置
 *
 * @author ysbing
 * 创建于 2018/1/16
 */
public class GlintBaseBuilder<E extends BaseHttpModule> {
    /**
     * 请求的地址
     */
    public String url;
    /**
     * 请求参数
     */
    public TreeMap<String, String> params;
    /**
     * 使用JSON协议的请求参数
     */
    public JsonObject jsonParams;
    /**
     * 是否使用标准json解析
     */
    public boolean standardDeserialize = true;
    /**
     * 使用签名，默认否
     */
    public boolean signature = false;
    /**
     * 是否跳转回UI线程
     */
    public boolean mainThread = true;
    /**
     * 头部信息
     */
    public Map<String, String> header = new HashMap<>();
    /**
     * 额外添加的cookie
     */
    public String cookie;
    /**
     * 上传协议类型
     */
    public String mimeType;
    /**
     * 额外的配置数据
     */
    public Bundle otherBuilder = new Bundle();
    /**
     * 自定义解析Module
     */
    public Set<E> customGlintModule = new LinkedHashSet<>();
    /**
     * 请求标签，用于取消请求
     */
    public int tag;

    public void addCustomGlintModule(@NonNull E customGlintModule) {
        this.customGlintModule.add(customGlintModule);
    }

}
