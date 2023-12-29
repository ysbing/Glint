package com.ysbing.glint.socket;


import static com.ysbing.glint.util.GlintRequestUtil.sGson;

import androidx.annotation.NonNull;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.ysbing.glint.util.GlintRequestUtil;

import java.lang.reflect.Type;

/**
 * 自定义Socket网络模块
 *
 * @author ysbing
 */
public abstract class SocketHttpModule {

    public String getCmdId(@NonNull JsonElement jsonEl, @NonNull Gson gson, @NonNull Type typeOfT) {
        return null;
    }

    public <T> T customDeserialize(@NonNull JsonElement jsonEl, @NonNull Gson gson, @NonNull Type typeOfT) throws Exception {
        return GlintRequestUtil.successDeserialize(sGson, jsonEl, typeOfT);
    }
}