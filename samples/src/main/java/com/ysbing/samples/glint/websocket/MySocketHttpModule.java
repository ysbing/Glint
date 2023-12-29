package com.ysbing.samples.glint.websocket;


import androidx.annotation.NonNull;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.ysbing.glint.socket.SocketHttpModule;

import java.lang.reflect.Type;

/**
 * 自定义网络模块
 *
 * @author ysbing
 */
public class MySocketHttpModule extends SocketHttpModule {
    public static final String SOCKET_CMD_SEND = "MY_CMD";

    public static MySocketHttpModule get() {
        return new MySocketHttpModule();
    }

    @Override
    public String getCmdId(@NonNull JsonElement jsonEl, @NonNull Gson gson, @NonNull Type typeOfT) {
        return SOCKET_CMD_SEND;
    }

    @Override
    public <T> T customDeserialize(@NonNull JsonElement jsonEl, @NonNull Gson gson, @NonNull Type typeOfT) throws Exception {
        return super.customDeserialize(jsonEl, gson, typeOfT);
    }
}