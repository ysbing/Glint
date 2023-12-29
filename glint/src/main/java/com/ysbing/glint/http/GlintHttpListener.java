package com.ysbing.glint.http;

import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;

import com.ysbing.glint.base.GlintResultBean;

/**
 * 请求监听类
 *
 * @author ysbing
 */
public abstract class GlintHttpListener<T> {

    /**
     * 请求之前，开始的回调
     */
    public void onStart() {
    }

    /**
     * @param resultBean 封装结果类
     */
    public void onResponse(@NonNull GlintResultBean<T> resultBean) throws Throwable {
    }

    /**
     * @param result 经过反序列化后的实体类
     */
    public void onSuccess(@NonNull T result) throws Throwable {
    }

    /**
     * 当http状态码200，状态码非200的回调
     *
     * @param status 状态码
     * @param errMsg 对应的错误提示
     */
    public void onError(int status, @NonNull String errMsg) throws Throwable {
    }

    /**
     * 当网络请求出现错误的时候，或者非标准json解析错误时的回调
     *
     * @param error 错误
     */
    @CallSuper
    public void onFail(@NonNull Throwable error) {
        error.printStackTrace();
    }

    /**
     * 当出现onError或者onFail的时候，会同时响应该回调
     * 如果有公共处理的逻辑，可以在此回调进行
     */
    public void onErrorOrFail() {
    }

    /**
     * 取消该请求
     */
    public void onCancel() {
    }

    /**
     * 无论成功与否，都会响应该回调
     */
    public void onFinish() {
    }

}