
package com.ysbing.glint.socket;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.util.LruCache;
import android.text.TextUtils;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.ysbing.glint.base.Glint;
import com.ysbing.glint.socket.socketio.IOMessage;
import com.ysbing.glint.socket.socketio.IOWebSocketTransport;
import com.ysbing.glint.util.UiKit;

import java.net.URI;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

/**
 * WebSocket核心类
 *
 * @author ysbing
 * 创建于 2018/3/25
 */
public class GlintSocketCore {

    public static final AtomicInteger sSendId = new AtomicInteger();
    private static final Glint GLINT = Glint.getsInstance();
    private static OkHttpClient sClient;

    private WebSocket mWebSocket;
    private final URI mUrl;
    private final Deque<URI> mRunningAsyncCalls = new ArrayDeque<>();
    private final Deque<String> mWaitMessages = new ArrayDeque<>();
    private final Map<String, GlintSocketListener<String>> mAsyncListeners = new ConcurrentHashMap<>();
    private final LruCache<String, Boolean> mWaitPushMessages = new LruCache<>(10);
    private boolean mConnected = false;
    private boolean mAuthorized = false;

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

    public void connect() {
        if (mRunningAsyncCalls.contains(mUrl)) {
            return;
        }
        mRunningAsyncCalls.push(mUrl);
        IOWebSocketTransport.getUrl(mUrl, new GlintSocketIOCallback() {
            @Override
            public void onSocketIoUrl(@NonNull String socketUrl) {
                socketConnect(socketUrl);
            }

            @Override
            public void onError(@NonNull Throwable throwable) {
                mRunningAsyncCalls.remove(mUrl);
                Set<String> keySet = mAsyncListeners.keySet();
                for (String s : keySet) {
                    if (GlintSocket.EVENT_CONNECT.equals(s) || GlintSocket.EVENT_DISCONNECT.equals(s) || GlintSocket.EVENT_ERROR.equals(s)) {
                        continue;
                    }
                    mAsyncListeners.get(s).onError(throwable.toString());
                }
                if (mAsyncListeners.containsKey(GlintSocket.EVENT_ERROR)) {
                    try {
                        mAsyncListeners.get(GlintSocket.EVENT_ERROR).onProcess("");
                    } catch (Exception ignored) {
                    }
                }

            }
        });
    }

    private void socketConnect(@NonNull String socketUrl) {
        mRunningAsyncCalls.remove(mUrl);
        if (mWebSocket != null) {
            mWebSocket.cancel();
            mWebSocket = null;
        }
        Request request = new Request.Builder().url(socketUrl).build();
        sClient.newWebSocket(request, socketListener);
    }

    private final WebSocketListener socketListener = new WebSocketListener() {
        @Override
        public void onOpen(WebSocket webSocket, Response response) {
            super.onOpen(webSocket, response);
            mWebSocket = webSocket;
            mConnected = true;
            for (String waitMessage : mWaitMessages) {
                webSocket.send(waitMessage);
            }
            mWaitMessages.clear();
            if (mAsyncListeners.containsKey(GlintSocket.EVENT_CONNECT)) {
                try {
                    mAsyncListeners.get(GlintSocket.EVENT_CONNECT).onProcess("");
                } catch (Exception ignored) {
                }
            }
        }

        @Override
        public void onMessage(WebSocket webSocket, String text) {
            super.onMessage(webSocket, text);
            IOMessage message = new IOMessage(text);
            switch (message.getType()) {
                case IOMessage.TYPE_DISCONNECT:
                    disconnect();
                    break;
                case IOMessage.TYPE_CONNECT:
                    break;
                case IOMessage.TYPE_HEARTBEAT:
                    send("2::");
                    break;
                case IOMessage.TYPE_MESSAGE:
                    messagePush(message.getData());
                    if (mAuthorized) {
                        for (String cacheMessage : mWaitPushMessages.snapshot().keySet()) {
                            messagePush(cacheMessage);
                        }
                        mWaitPushMessages.evictAll();
                    }
                    break;
                case IOMessage.TYPE_JSON_MESSAGE:
                case IOMessage.TYPE_EVENT:
                case IOMessage.TYPE_ACK:
                    break;
                case IOMessage.TYPE_ERROR:
                    Set<String> keySet2 = mAsyncListeners.keySet();
                    for (String s : keySet2) {
                        if (GlintSocket.EVENT_CONNECT.equals(s) || GlintSocket.EVENT_DISCONNECT.equals(s) || GlintSocket.EVENT_ERROR.equals(s)) {
                            continue;
                        }
                        mAsyncListeners.get(s).onError(message.getData());
                    }
                case IOMessage.TYPE_NOOP:
                    break;
                default:
                    break;
            }
        }

        @Override
        public void onClosing(WebSocket webSocket, int code, String reason) {
            super.onClosing(webSocket, code, reason);
            mConnected = false;
        }

        @Override
        public void onClosed(WebSocket webSocket, int code, String reason) {
            super.onClosed(webSocket, code, reason);
            mConnected = false;
            if (mAsyncListeners.containsKey(GlintSocket.EVENT_DISCONNECT)) {
                try {
                    mAsyncListeners.get(GlintSocket.EVENT_DISCONNECT).onProcess("");
                } catch (Exception ignored) {
                }
            }
        }

        @Override
        public void onFailure(WebSocket webSocket, Throwable t, Response response) {
            super.onFailure(webSocket, t, response);
            if (mAsyncListeners.containsKey(GlintSocket.EVENT_ERROR) && mWebSocket != null) {
                try {
                    mAsyncListeners.get(GlintSocket.EVENT_ERROR).onProcess("");
                } catch (Exception ignored) {
                }
            }
            disconnect();
        }
    };

    private void messagePush(String messageStr) {
        JsonParser jsonParser = new JsonParser();
        JsonObject jsonObject = jsonParser.parse(messageStr).getAsJsonObject();
        String cmdId;
        boolean hasId = false;
        if (jsonObject.get("id") != null) {
            cmdId = jsonObject.get("id").getAsString();
            hasId = true;
        } else if (jsonObject.get("route") != null) {
            cmdId = jsonObject.get("route").getAsString();
            if (!mAuthorized) {
                mWaitPushMessages.put(messageStr, true);
            }
        } else {
            return;
        }
        Set<String> keySet = mAsyncListeners.keySet();
        if (keySet.contains(cmdId)) {
            String body = jsonObject.get("body").toString();
            try {
                mAsyncListeners.get(cmdId).onProcess(body);
            } catch (Exception e) {
                mAsyncListeners.get(cmdId).onError(e.getMessage());
            }
        }
        if (hasId) {
            UiKit.runOnMainThreadSync(new Runnable() {
                @Override
                public void run() {
                    mAuthorized = true;
                }
            });
        }
    }

    public synchronized void disconnect() {
        mConnected = false;
        mAuthorized = false;
        mAsyncListeners.clear();
        if (mWebSocket != null) {
            mWebSocket.cancel();
            mWebSocket = null;
        }
    }

    public boolean isConnected() {
        return mConnected;
    }

    public synchronized void send(@NonNull String message) {
        if (mWebSocket != null && isConnected()) {
            mWebSocket.send(message);
        } else {
            mWaitMessages.add(message);
        }
    }

    public void on(@NonNull String cmdId, @NonNull GlintSocketListener<String> listener) {
        mAsyncListeners.put(cmdId, listener);
    }

    public synchronized void off(@Nullable String cmdId) {
        if (TextUtils.isEmpty(cmdId)) {
            mAsyncListeners.clear();
        } else {
            mAsyncListeners.remove(cmdId);
        }
    }
}
