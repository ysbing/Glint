
package com.ysbing.glint.socket;

/**
 * 重要类，使用时定义的任务
 *
 * @author ysbing
 */
public final class GlintSocketBuilder<T> {
    /**
     * 请求的地址
     */
    public String url;
    /**
     * 消息命令
     */
    public String cmdId;
    /**
     * 请求参数
     */
    public String params;
    /**
     * 发送消息的id，用于回调识别
     */
    public int sendId;
    /**
     * 上层的监听回调
     */
    public GlintSocketListener<T> listener;
    /**
     * 请求类型，有发送消息和监听推送两种
     */
    public RequestType requestType;
    /**
     * 请求标签，用于取消请求
     */
    public int tag;

    public enum RequestType {
        SEND,
        PUSH_LISTENER
    }
}
