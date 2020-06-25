package com.ysbing.glint.socket.socketio;

import android.support.annotation.NonNull;

/**
 * 用户SocketIO的连接地址
 *
 * @author ysbing
 *         创建于 2018/3/25
 */
public interface GlintSocketIOCallback {
    /**
     * 异步获取socket io地址
     *
     * @param socketUrl 经过拼接的socket地址
     */
    void onSocketUrl(@NonNull String socketUrl);

    /**
     * 错误回调
     *
     * @param throwable 未知错误
     */
    void onError(@NonNull Throwable throwable);
}
