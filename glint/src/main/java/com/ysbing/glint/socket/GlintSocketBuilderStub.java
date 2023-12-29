package com.ysbing.glint.socket;

import static com.ysbing.glint.util.GlintRequestUtil.sGson;

import android.os.RemoteException;

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
    private Type typeOfT;

    public GlintSocketBuilderStub(GlintSocketBuilder<T> builder) {
        this.builder = builder;
        if (builder.listener != null) {
            typeOfT = GlintRequestUtil.getListenerType(builder.listener.getClass());
        }
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
    public String getResponseCmdId(String response) throws RemoteException {
        if (builder.listener != null && builder.customGlintModule != null) {
            if (!typeOfT.equals(Void.class)) {
                JsonParser parser = new JsonParser();
                JsonElement jsonElement = parser.parse(response);
                return builder.customGlintModule.getCmdId(jsonElement, sGson, typeOfT);
            }
        }
        return "";
    }

    @Override
    public void onResponse(String response) throws RemoteException {
        if (builder.listener != null) {
            T t;
            if (!typeOfT.equals(Void.class)) {
                JsonElement jsonElement = JsonParser.parseString(response);
                if (builder.customGlintModule != null) {
                    try {
                        t = builder.customGlintModule.customDeserialize(jsonElement, sGson, typeOfT);
                        onProcess(t);
                    } catch (Exception e) {
                        onError(e.getMessage());
                    }
                } else {
                    t = GlintRequestUtil.successDeserialize(sGson, jsonElement, typeOfT);
                    onProcess(t);
                }
            }
        }
    }

    private void onProcess(final T t) {
        UiKit.runOnMainThreadAsync(new Runnable() {
            @Override
            public void run() {
                try {
                    builder.listener.onProcess(t);
                } catch (Throwable e) {
                    onError(e.getMessage());
                }
            }
        });
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
