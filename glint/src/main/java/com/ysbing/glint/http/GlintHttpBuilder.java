package com.ysbing.glint.http;

import androidx.annotation.NonNull;

import com.ysbing.glint.base.BaseHttpModule;
import com.ysbing.glint.base.Glint;
import com.ysbing.glint.base.GlintBaseBuilder;

/**
 * 网络请求的参数配置
 *
 * @author ysbing
 */
public final class GlintHttpBuilder<T, E extends BaseHttpModule> extends GlintBaseBuilder<E> implements Cloneable {
    /**
     * GET和POST请求方式
     */
    public int method;
    /**
     * 上层的监听回调
     */
    public GlintHttpListener<T> listener;
    /**
     * 结果不是json
     */
    public boolean notJson = false;
    /**
     * 是否重试
     */
    public boolean retryOnConnectionFailure = true;
    /**
     * 自定义重试策略
     */
    public GlintHttpRetry retry;
    public GlintHttpRetry baseRetry;
    /**
     * 该请求所属的宿主，有Activity和Fragment
     * Fragment支持support的Fragment
     * 用于在生命周期时销毁请求
     */
    public int hostHashCode;
    /**
     * 自由的生命周期，不智能销毁请求
     */
    public boolean freeLife = false;
    /**
     * 缓存时间，单位是秒，默认不缓存
     */
    public int cacheTime;

    public GlintHttpBuilder(@NonNull Glint glint) {
        this(glint, true);
    }

    public GlintHttpBuilder(@NonNull Glint glint, boolean init) {
        super();
        this.mimeType = "application/x-www-form-urlencoded; charset=utf-8";
        if (init) {
            glint.configDefaultBuilder(this);
        }
    }

    @Override
    protected GlintHttpBuilder clone() throws CloneNotSupportedException {
        return (GlintHttpBuilder) super.clone();
    }
}