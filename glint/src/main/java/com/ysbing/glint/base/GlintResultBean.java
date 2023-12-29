package com.ysbing.glint.base;

import com.ysbing.glint.http.GlintHttpCore;

import okhttp3.Headers;

/**
 * 接口请求统一结果实体类
 *
 * @author ysbing
 */
public final class GlintResultBean<T> {
    /**
     * 原始数据
     */
    private String responseStr;
    /**
     * 运行时状态码，有三种
     * 详情看{@link GlintHttpCore}
     */
    private Glint.ResultStatus runStatus;
    /**
     * 接口返回的状态
     */
    private int status;
    /**
     * 解析后数据
     */
    private T data;
    /**
     * 非成功状态的错误码
     */
    private String message;
    /**
     * 头部信息
     */
    private Headers headers;

    public GlintResultBean() {
    }

    public void setResponseStr(String responseStr) {
        this.responseStr = responseStr;
    }

    public String getResponseStr() {
        return responseStr;
    }

    public void setRunStatus(Glint.ResultStatus runStatus) {
        this.runStatus = runStatus;
    }

    public Glint.ResultStatus getRunStatus() {
        return runStatus;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public void setHeaders(Headers headers) {
        this.headers = headers;
    }

    public Headers getHeaders() {
        return headers;
    }
}
