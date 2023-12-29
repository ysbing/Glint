package com.ysbing.glint.socket.socketio;


import androidx.annotation.NonNull;

import com.ysbing.glint.http.GlintHttp;
import com.ysbing.glint.http.GlintHttpListener;

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/**
 * SocketIO的URL处理类
 *
 * @author ysbing
 * 创建于 2018/3/25
 */
public class IOWebSocketTransport {
    private static final Pattern PATTERN_HTTP = Pattern.compile("^http");

    public static void getUrl(@NonNull final URI url, @NonNull final GlintSocketIOCallback callback) {
        if ("ws".equals(url.getScheme()) || "wss".equals(url.getScheme())) {
            callback.onSocketUrl(url.toString());
        } else if ("http".equals(url.getScheme()) || "https".equals(url.getScheme())) {
            GlintHttp.get(url + "/socket.io/1/").signature(false).notJson(true).mainThread(false).execute(new GlintHttpListener<String>() {
                @Override
                public void onSuccess(@NonNull String result) throws Throwable {
                    super.onSuccess(result);
                    String[] data = result.split(":");
                    String sessionId = data[0];
                    List<String> protocols = Arrays.asList(data[3].split(","));
                    String socketUrl;
                    if (protocols.contains("websocket")) {
                        socketUrl = PATTERN_HTTP.matcher(url.toString()).replaceFirst("ws") + "/socket.io/1/" + "websocket" + "/" + sessionId;
                    } else {
                        if (!protocols.contains("xhr-polling")) {
                            callback.onError(new RuntimeException("socket url is empty"));
                            return;
                        }
                        socketUrl = url.toString() + "/socket.io/1/" + "xhr-polling" + "/" + sessionId;

                    }
                    callback.onSocketUrl(socketUrl);
                }

                @Override
                public void onFail(@NonNull Throwable error) {
                    super.onFail(error);
                    callback.onError(error);
                }
            });
        } else {
            callback.onError(new RuntimeException("Unknown url"));
        }
    }
}
