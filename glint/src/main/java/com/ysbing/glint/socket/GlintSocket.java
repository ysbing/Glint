
package com.ysbing.glint.socket;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.ysbing.glint.socket.socketio.Protocol;

/**
 * Socket请求类，请求的入口
 *
 * @author ysbing
 */
public class GlintSocket {

    /**
     * 连接成功事件
     */
    public static final String EVENT_CONNECT = "EVENT_CONNECT";
    /**
     * 正常断开连接事件
     */
    public static final String EVENT_DISCONNECT = "EVENT_DISCONNECT";
    /**
     * 异常断开事件，如网络中断
     */
    public static final String EVENT_ERROR = "EVENT_ERROR";
    /**
     * 网络错误
     */
    public static final int ERROR_NET = 0x1;
    /**
     * 异常错误
     */
    public static final int ERROR_EXCEPTION = 0x2;

    private final GlintSocketBuilder builder;

    /**
     * 如果有跨进程需求，就做初始化
     * 在单一进程使用的话，不需要初始化
     *
     * @param context 上下文对象
     */
    public static void init(@NonNull Context context) {
        GlintSocketDispatcher.getInstance().init(context.getApplicationContext());
    }

    /**
     * 删除所有，包括Socket的连接
     */
    public static void removeAll() {
        GlintSocketDispatcher.getInstance().removeAll();
    }

    /**
     * 发送一条socket消息
     */
    public static GlintSocket send(@NonNull String url, @NonNull String cmdId, @NonNull String message) {
        return new GlintSocket(url, cmdId, message, -1, GlintSocketBuilder.RequestType.SEND);
    }

    public static GlintSocket sendIO(@NonNull String url, @NonNull String cmdId, @NonNull String message) {
        final int sendId = GlintSocketCore.sSendId.incrementAndGet();
        return new GlintSocket(url, cmdId, "3:::" + Protocol.encode(sendId, cmdId, message), sendId, GlintSocketBuilder.RequestType.SEND);
    }

    /**
     * 设置推送监听
     */
    public static GlintSocket on(@NonNull String url, @NonNull String cmdId) {
        return new GlintSocket(url, cmdId, "", -1, GlintSocketBuilder.RequestType.PUSH_LISTENER);
    }

    /**
     * 移除推送监听
     */
    public static void off(@NonNull String url) {
        off(url, "");
    }

    public static void off(@NonNull String url, @Nullable String cmdId) {
        off(url, cmdId, 0);
    }

    public static void off(@NonNull String url, @Nullable String cmdId, int tag) {
        GlintSocketBuilder builder = new GlintSocketBuilder();
        builder.url = url;
        builder.cmdId = cmdId;
        builder.tag = tag;
        GlintSocketDispatcher.getInstance().removePushListener(builder);
    }

    public void off() {
        GlintSocketDispatcher.getInstance().removePushListener(builder);
    }

    /**
     * @param tag 请求标签，用于取消请求
     */
    public GlintSocket setTag(@NonNull String tag) {
        builder.tag = tag.hashCode();
        return this;
    }

    public GlintSocket setTag(int tag) {
        builder.tag = tag;
        return this;
    }

    public GlintSocket(@NonNull String url, @NonNull String cmdId, @NonNull String params, int sendId, @NonNull GlintSocketBuilder.RequestType requestType) {
        builder = new GlintSocketBuilder<>();
        builder.url = url;
        builder.cmdId = cmdId;
        builder.params = params;
        builder.sendId = sendId;
        builder.requestType = requestType;
    }

    public void execute() {
        execute(null);
    }

    public <T> void execute(@Nullable GlintSocketListener<T> listener) {
        if (!TextUtils.isEmpty(builder.url) && !TextUtils.isEmpty(builder.cmdId)) {
            builder.listener = listener;
            if (builder.tag == 0) {
                builder.tag = System.identityHashCode(builder);
            }
            switch (builder.requestType) {
                case SEND:
                    builder.cmdId = String.valueOf(builder.sendId);
                    GlintSocketDispatcher.getInstance().send(builder);
                    break;
                case PUSH_LISTENER:
                    GlintSocketDispatcher.getInstance().on(builder);
                    break;
                default:
                    break;
            }
        }
    }


}
