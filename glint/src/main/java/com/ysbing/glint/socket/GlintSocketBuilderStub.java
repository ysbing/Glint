package com.ysbing.glint.socket;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.ysbing.glint.util.GlintRequestUtil;
import com.ysbing.glint.util.UiKit;

import java.lang.reflect.Type;

/**
 * 只做Google的Gson中的JsonObject
 * 该类可做参考，用于扩展更多的实现
 *
 * @author ysbing
 */
public class GlintSocketBuilderStub<T> extends GlintSocketBuilderWrapper.Stub {

    private final GlintSocketBuilder<T> builder;

    public GlintSocketBuilderStub(GlintSocketBuilder<T> builder) {
        this.builder = builder;
    }

    @Override
    public String getUrl() {
        return builder.url;
    }

    @Override
    public String getCmdId() {
        return builder.cmdId;
    }

    @Override
    public String getParams() {
        return builder.params;
    }

    @Override
    public int getSendId() {
        return builder.sendId;
    }

    @Override
    public int getTag() {
        return builder.tag;
    }

    @Override
    public void onResponse(String response) {
        if (builder.listener != null) {
            Type typeOfT = GlintRequestUtil.getListenerType(builder.listener.getClass());
            final T t;
            if (typeOfT.equals(Void.class)) {
                t = null;
            } else {
                JsonParser parser = new JsonParser();
                JsonElement jsonElement = parser.parse(response);
                t = GlintRequestUtil.successDeserialize(new Gson(), jsonElement, typeOfT);
            }
            UiKit.runOnMainThreadAsync(new Runnable() {
                @Override
                public void run() {
                    try {
                        //noinspection ConstantConditions
                        builder.listener.onProcess(t);
                    } catch (Throwable e) {
                        onError(e.getMessage());
                    }
                }
            });
        }
    }

    @Override
    public void onError(final String error) {
        if (builder.listener != null) {
            UiKit.runOnMainThreadAsync(new Runnable() {
                @Override
                public void run() {
                    builder.listener.onError(error);
                }
            });
        }
    }
}
