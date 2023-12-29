
package com.ysbing.glint.socket;

import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.ysbing.glint.base.Glint;
import com.ysbing.glint.socket.socketio.GlintSocketIOCallback;
import com.ysbing.glint.socket.socketio.IOWebSocketTransport;

import java.net.URI;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

/**
 * WebSocket核心类
 *
 * @author ysbing
 * 创建于 2020/6/25
 */
public class GlintSocketCore {

    private static final Glint GLINT = Glint.getsInstance();
    private static final OkHttpClient sClient;

    private WebSocket mWebSocket;
    private final URI mUrl;
    private final Deque<String> mWaitMessages = new ArrayDeque<>();
    private final Map<String, GlintSocketListener<SocketInnerResultBean>> mAsyncListeners = new ConcurrentHashMap<>();
    private boolean mConnecting = false;
    private boolean mConnected = false;

    static {
        if (GLINT != null) {
            sClient = GLINT.onOkHttpBuildCreate(Glint.GlintType.SOCKET, new OkHttpClient.Builder()).build();
        } else {
            sClient = new OkHttpClient.Builder().build();
        }
    }

    GlintSocketCore(@NonNull URI url) {
        this.mUrl = url;
    }

    synchronized void connect() {
        if (mConnecting || mConnected) {
            return;
        }
        mConnecting = true;
        IOWebSocketTransport.getUrl(mUrl, new GlintSocketIOCallback() {
            @Override
            public void onSocketUrl(@NonNull String socketUrl) {
                socketConnect(socketUrl);
            }

            @Override
            public void onError(@NonNull Throwable throwable) {
                mConnecting = false;
                Set<String> keySet = mAsyncListeners.keySet();
                for (String s : keySet) {
                    if (GlintSocket.ALL_EVENT.contains(s)) {
                        continue;
                    }
                    GlintSocketListener<SocketInnerResultBean> listener = mAsyncListeners.get(s);
                    if (listener != null) {
                        listener.onError(throwable.toString());
                    }
                }
                if (mAsyncListeners.containsKey(GlintSocket.EVENT_ERROR)) {
                    GlintSocketListener<SocketInnerResultBean> listener = mAsyncListeners.get(GlintSocket.EVENT_ERROR);
                    if (listener != null) {
                        try {
                            SocketInnerResultBean bean = new SocketInnerResultBean();
                            bean.response = String.valueOf(GlintSocket.ERROR_NET);
                            bean.msgType = 1;
                            listener.onProcess(bean);
                        } catch (Throwable ignored) {
                        }
                    }
                }
            }
        });
    }

    private void socketConnect(@NonNull String socketUrl) {
        if (mWebSocket != null) {
            mWebSocket.cancel();
            mWebSocket = null;
        }
        Request request = new Request.Builder().url(socketUrl).build();
        sClient.newWebSocket(request, getSocketListener());
    }

    private WebSocketListener getSocketListener() {
        return new WebSocketListener() {
            @Override
            public void onOpen(@NonNull WebSocket webSocket, @NonNull Response response) {
                super.onOpen(webSocket, response);
                mWebSocket = webSocket;
                mConnecting = false;
                mConnected = true;
                for (String waitMessage : mWaitMessages) {
                    webSocket.send(waitMessage);
                }
                mWaitMessages.clear();
                if (mAsyncListeners.containsKey(GlintSocket.EVENT_CONNECT)) {
                    GlintSocketListener<SocketInnerResultBean> listener = mAsyncListeners.get(GlintSocket.EVENT_CONNECT);
                    if (listener != null) {
                        try {
                            SocketInnerResultBean bean = new SocketInnerResultBean();
                            bean.response = "";
                            bean.msgType = 1;
                            listener.onProcess(bean);
                        } catch (Throwable ignored) {
                        }
                    }
                }
            }

            @Override
            public void onMessage(@NonNull WebSocket webSocket, @NonNull String text) {
                super.onMessage(webSocket, text);
                messagePush(text);
            }

            @Override
            public void onClosing(@NonNull WebSocket webSocket, int code, @NonNull String reason) {
                super.onClosing(webSocket, code, reason);
                mConnected = false;
            }

            @Override
            public void onClosed(@NonNull WebSocket webSocket, int code, @NonNull String reason) {
                super.onClosed(webSocket, code, reason);
                mConnected = false;
                if (mAsyncListeners.containsKey(GlintSocket.EVENT_DISCONNECT)) {
                    GlintSocketListener<SocketInnerResultBean> listener = mAsyncListeners.get(GlintSocket.EVENT_DISCONNECT);
                    if (listener != null) {
                        try {
                            SocketInnerResultBean bean = new SocketInnerResultBean();
                            bean.response = "";
                            bean.msgType = 1;
                            listener.onProcess(bean);
                        } catch (Throwable ignored) {
                        }
                    }
                }
            }

            @Override
            public void onFailure(@NonNull WebSocket webSocket, @NonNull Throwable t, @Nullable Response response) {
                super.onFailure(webSocket, t, response);
                if (mAsyncListeners.containsKey(GlintSocket.EVENT_ERROR)) {
                    GlintSocketListener<SocketInnerResultBean> listener = mAsyncListeners.get(GlintSocket.EVENT_ERROR);
                    if (listener != null) {
                        try {
                            SocketInnerResultBean bean = new SocketInnerResultBean();
                            bean.response = String.valueOf(GlintSocket.ERROR_EXCEPTION);
                            bean.msgType = 1;
                            listener.onProcess(bean);
                        } catch (Throwable ignored) {
                        }
                    }
                }
                disconnect();
            }
        };
    }

    private void messagePush(String messageStr) {
        Set<String> keySet = mAsyncListeners.keySet();
        for (String s : keySet) {
            GlintSocketListener<SocketInnerResultBean> listener = mAsyncListeners.get(s);
            if (listener != null) {
                try {
                    SocketInnerResultBean bean = new SocketInnerResultBean();
                    bean.response = messageStr;
                    bean.msgType = 0;
                    listener.onProcess(bean);
                } catch (Throwable e) {
                    listener.onError(Objects.requireNonNull(e.getMessage()));
                }
            }
        }
    }

    synchronized void disconnect() {
        mConnected = false;
        mConnecting = false;
        mAsyncListeners.clear();
        if (mWebSocket != null) {
            mWebSocket.cancel();
            mWebSocket = null;
        }
    }

    boolean isConnected() {
        return mConnected;
    }

    void on(@NonNull String cmdId, @NonNull GlintSocketListener<SocketInnerResultBean> listener) {
        mAsyncListeners.put(cmdId, listener);
    }

    synchronized void send(@NonNull String message) {
        if (mWebSocket != null && isConnected()) {
            mWebSocket.send(message);
        } else {
            mWaitMessages.add(message);
        }
    }

    public synchronized void off(@Nullable String cmdId) {
        if (TextUtils.isEmpty(cmdId)) {
            disconnect();
        } else {
            for (String key : mAsyncListeners.keySet()) {
                if (key.contains(GlintSocket.class.getSimpleName()) && key.contains(cmdId)) {
                    mAsyncListeners.remove(key);
                }
            }
            mAsyncListeners.remove(cmdId);
        }
    }
}
