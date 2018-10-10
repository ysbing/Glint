package com.ysbing.glint.socket;

import android.os.RemoteException;
import android.support.annotation.NonNull;
import android.support.v4.util.SparseArrayCompat;
import android.text.TextUtils;
import android.util.SparseArray;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

/**
 * Socket的管理和实现类
 *
 * @author ysbing
 */
public class GlintSocketServiceStub extends GlintSocketService.Stub {

    private final SparseArrayCompat<Map<String, SparseArrayCompat<GlintSocketBuilderWrapper>>> mOnPushListeners = new SparseArrayCompat<>();
    private final SparseArray<GlintSocketCore> mSocketSparseArray = new SparseArray<>();

    @Override
    public void send(GlintSocketBuilderWrapper builderWrapper) throws RemoteException {
        final String url = builderWrapper.getUrl();
        if (TextUtils.isEmpty(url)) {
            return;
        }
        GlintSocketCore socket = mSocketSparseArray.get(url.hashCode());
        if (socket == null) {
            connect(url);
            socket = mSocketSparseArray.get(url.hashCode());
        } else if (!socket.isConnected()) {
            socket.connect();
        }
        socket.send(builderWrapper.getParams());
        on(builderWrapper);
    }

    @Override
    public void connect(String url) {
        GlintSocketCore socket = mSocketSparseArray.get(url.hashCode());
        if (socket == null || !socket.isConnected()) {
            if (socket != null) {
                socket.connect();
            } else {
                String requestUrl = url;
                if (requestUrl.endsWith("/")) {
                    requestUrl = requestUrl.substring(0, requestUrl.length() - 1);
                }
                socket = new GlintSocketCore(URI.create(requestUrl));
                socket.connect();
                mSocketSparseArray.put(url.hashCode(), socket);
            }
        }
    }

    @Override
    public void on(final GlintSocketBuilderWrapper builderWrapper) throws RemoteException {
        GlintSocketCore socket = mSocketSparseArray.get(builderWrapper.getUrl().hashCode());
        if (socket == null) {
            connect(builderWrapper.getUrl());
            socket = mSocketSparseArray.get(builderWrapper.getUrl().hashCode());
        } else if (!socket.isConnected()) {
            socket.connect();
        }
        Map<String, SparseArrayCompat<GlintSocketBuilderWrapper>> maps = mOnPushListeners.get(builderWrapper.getUrl().hashCode());
        if (maps == null) {
            maps = new HashMap<>();
            mOnPushListeners.put(builderWrapper.getUrl().hashCode(), maps);
        }
        SparseArrayCompat<GlintSocketBuilderWrapper> sparseArrayCompat = maps.get(builderWrapper.getCmdId());
        if (sparseArrayCompat == null) {
            sparseArrayCompat = new SparseArrayCompat<>();
            maps.put(builderWrapper.getCmdId(), sparseArrayCompat);
        }
        sparseArrayCompat.append(builderWrapper.getTag(), builderWrapper);
        socket.on(builderWrapper.getCmdId(), new GlintSocketListener<String>() {
            @Override
            public void onProcess(@NonNull String result) throws Exception {
                super.onProcess(result);
                Map<String, SparseArrayCompat<GlintSocketBuilderWrapper>> maps = mOnPushListeners.get(builderWrapper.getUrl().hashCode());
                SparseArrayCompat<GlintSocketBuilderWrapper> wrappers = maps.get(builderWrapper.getCmdId());
                for (int i = 0; wrappers != null && i < wrappers.size(); i++) {
                    try {
                        wrappers.get(wrappers.keyAt(i)).onResponse(result);
                    } catch (Exception e) {
                        wrappers.get(wrappers.keyAt(i)).onError(e.getMessage());
                    }
                }
            }

            @Override
            public void onError(@NonNull String error) {
                super.onError(error);
                try {
                    Map<String, SparseArrayCompat<GlintSocketBuilderWrapper>> maps = mOnPushListeners.get(builderWrapper.getUrl().hashCode());
                    SparseArrayCompat<GlintSocketBuilderWrapper> wrappers = maps.get(builderWrapper.getCmdId());
                    for (int i = 0; wrappers != null && i < wrappers.size(); i++) {
                        try {
                            wrappers.get(wrappers.keyAt(i)).onError(error);
                        } catch (Exception e) {
                            try {
                                wrappers.get(wrappers.keyAt(i)).onError(e.getMessage());
                            } catch (RemoteException e1) {
                                e1.printStackTrace();
                            }
                        }
                    }
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }

        });
    }

    @Override
    public void off(String url, String cmdId, int tag) {
        GlintSocketCore socket = mSocketSparseArray.get(url.hashCode());
        if (socket != null) {
            socket.off(cmdId);
            if (TextUtils.isEmpty(cmdId)) {
                socket.disconnect();
            }
        }
        Map<String, SparseArrayCompat<GlintSocketBuilderWrapper>> cmdIdMaps = mOnPushListeners.get(url.hashCode());
        if (cmdIdMaps != null) {
            if (TextUtils.isEmpty(cmdId)) {
                cmdIdMaps.clear();
            } else {
                SparseArrayCompat<GlintSocketBuilderWrapper> wrappers = cmdIdMaps.get(cmdId);
                if (wrappers != null) {
                    if (tag == 0) {
                        wrappers.clear();
                    } else {
                        GlintSocketBuilderWrapper wrapper = wrappers.get(tag);
                        if (wrapper != null) {
                            wrappers.remove(tag);
                        }
                    }

                }
            }

        }
    }

    @Override
    public void refreshHost(String url, String lastUrl) {
        if (!TextUtils.equals(url, lastUrl)) {
            mSocketSparseArray.put(url.hashCode(), mSocketSparseArray.get(lastUrl.hashCode()));
            mOnPushListeners.put(url.hashCode(), mOnPushListeners.get(lastUrl.hashCode()));
            mSocketSparseArray.remove(lastUrl.hashCode());
            mOnPushListeners.remove(lastUrl.hashCode());
        }
    }

    @Override
    public void offAll() {
        for (int i = 0; i < mSocketSparseArray.size(); i++) {
            GlintSocketCore socket = mSocketSparseArray.valueAt(i);
            if (socket != null) {
                socket.disconnect();
            }
        }
        mSocketSparseArray.clear();
    }

}