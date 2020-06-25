package com.ysbing.glint.socket;

import android.os.RemoteException;
import android.support.annotation.NonNull;
import android.support.v4.util.SparseArrayCompat;
import android.text.TextUtils;

import com.ysbing.glint.socket.socketio.GlintSocketIOCore;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Socket的管理和实现类
 */
public class GlintSocketServiceStub extends GlintSocketService.Stub {
    private final Map<String, Map<String, SparseArrayCompat<GlintSocketBuilderWrapper>>> mOnPushListeners = new ConcurrentHashMap<>();
    private final Map<String, GlintSocketCore> mSocketArray = new ConcurrentHashMap<>();
    private final Map<String, GlintSocketIOCore> mSocketIOArray = new ConcurrentHashMap<>();

    @Override
    public void connect(String url) {
        GlintSocketCore socket = mSocketArray.get(url);
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
                mSocketArray.put(url, socket);
            }
        }
    }

    @Override
    public void connectIO(String url) throws RemoteException {
        GlintSocketIOCore socket = mSocketIOArray.get(url);
        if (socket == null || !socket.isConnected()) {
            if (socket != null) {
                socket.connect();
            } else {
                String requestUrl = url;
                if (requestUrl.endsWith("/")) {
                    requestUrl = requestUrl.substring(0, requestUrl.length() - 1);
                }
                socket = new GlintSocketIOCore(URI.create(requestUrl));
                socket.connect();
                mSocketIOArray.put(url, socket);
            }
        }
    }

    @Override
    public void send(GlintSocketBuilderWrapper builderWrapper) throws RemoteException {
        final String url = builderWrapper.getUrl();
        if (TextUtils.isEmpty(url)) {
            return;
        }
        GlintSocketCore socket = mSocketArray.get(url);
        if (socket == null) {
            connect(url);
            socket = mSocketArray.get(url);
        } else if (!socket.isConnected()) {
            socket.connect();
        }
        socket.send(builderWrapper.getParams());
        on(builderWrapper);
    }

    @Override
    public void sendIO(GlintSocketBuilderWrapper builderWrapper) throws RemoteException {
        final String url = builderWrapper.getUrl();
        if (TextUtils.isEmpty(url)) {
            return;
        }
        GlintSocketIOCore socket = mSocketIOArray.get(url);
        if (socket == null) {
            connectIO(url);
            socket = mSocketIOArray.get(url);
        } else if (!socket.isConnected()) {
            socket.connect();
        }
        socket.send(builderWrapper.getParams());
        onIO(builderWrapper);
    }

    @Override
    public void on(final GlintSocketBuilderWrapper builderWrapper) throws RemoteException {
        if (builderWrapper.getCmdId() == null) {
            return;
        }
        GlintSocketCore socket = mSocketArray.get(builderWrapper.getUrl());
        if (socket == null) {
            connectIO(builderWrapper.getUrl());
            socket = mSocketArray.get(builderWrapper.getUrl());
        } else if (!socket.isConnected()) {
            socket.connect();
        }
        Map<String, SparseArrayCompat<GlintSocketBuilderWrapper>> maps = mOnPushListeners.get(builderWrapper.getUrl());
        if (maps == null) {
            maps = new HashMap<>();
            mOnPushListeners.put(builderWrapper.getUrl(), maps);
        }
        SparseArrayCompat<GlintSocketBuilderWrapper> sparseArrayCompat = maps.get(builderWrapper.getCmdId());
        if (sparseArrayCompat == null) {
            sparseArrayCompat = new SparseArrayCompat<>();
            maps.put(builderWrapper.getCmdId(), sparseArrayCompat);
        }
        sparseArrayCompat.append(builderWrapper.getTag(), builderWrapper);
        socket.on(builderWrapper.getCmdId(), new GlintSocketListener<SocketInnerResultBean>() {
            @Override
            public void onProcess(@NonNull SocketInnerResultBean result) throws Throwable {
                super.onProcess(result);
                Map<String, SparseArrayCompat<GlintSocketBuilderWrapper>> maps = mOnPushListeners.get(builderWrapper.getUrl());
                if (result.msgType == 0) {
                    if (maps != null && maps.containsKey(builderWrapper.getCmdId())) {
                        SparseArrayCompat<GlintSocketBuilderWrapper> wrappers = maps.get(builderWrapper.getCmdId());
                        String responseCmdId = null;
                        for (int i = 0; wrappers != null && i < wrappers.size(); i++) {
                            GlintSocketBuilderWrapper socketBuilderWrapper = wrappers.get(wrappers.keyAt(i));
                            try {
                                if (socketBuilderWrapper == null) {
                                    continue;
                                }
                                if (responseCmdId == null) {
                                    responseCmdId = socketBuilderWrapper.getResponseCmdId(result.response);
                                }
                                if (builderWrapper.getCmdId().equals(responseCmdId)) {
                                    socketBuilderWrapper.onResponse(result.response);
                                }
                            } catch (Exception e) {
                                if (builderWrapper.getCmdId().equals(responseCmdId)) {
                                    socketBuilderWrapper.onError(e.getMessage());
                                }
                            }
                        }
                    }
                } else {
                    if (maps != null && maps.containsKey(builderWrapper.getCmdId())
                            && GlintSocket.ALL_EVENT.contains(builderWrapper.getCmdId())) {
                        SparseArrayCompat<GlintSocketBuilderWrapper> wrappers = maps.get(builderWrapper.getCmdId());
                        for (int i = 0; wrappers != null && i < wrappers.size(); i++) {
                            GlintSocketBuilderWrapper socketBuilderWrapper = wrappers.get(wrappers.keyAt(i));
                            try {
                                if (socketBuilderWrapper == null) {
                                    continue;
                                }
                                socketBuilderWrapper.onResponse(result.response);
                            } catch (Exception e) {
                                socketBuilderWrapper.onError(e.getMessage());
                            }
                        }
                    }
                }
            }

            @Override
            public void onError(@NonNull String error) {
                super.onError(error);
                try {
                    Map<String, SparseArrayCompat<GlintSocketBuilderWrapper>> maps = mOnPushListeners.get(builderWrapper.getUrl());
                    if (maps == null) {
                        return;
                    }
                    SparseArrayCompat<GlintSocketBuilderWrapper> wrappers = maps.get(builderWrapper.getCmdId());
                    for (int i = 0; wrappers != null && i < wrappers.size(); i++) {
                        GlintSocketBuilderWrapper socketBuilderWrapper = wrappers.get(wrappers.keyAt(i));
                        try {
                            if (socketBuilderWrapper == null) {
                                continue;
                            }
                            socketBuilderWrapper.onError(error);
                        } catch (Exception e) {
                            try {
                                socketBuilderWrapper.onError(e.getMessage());
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
    public void onIO(final GlintSocketBuilderWrapper builderWrapper) throws RemoteException {
        if (builderWrapper == null) {
            return;
        }
        GlintSocketIOCore socket = mSocketIOArray.get(builderWrapper.getUrl());
        if (socket == null) {
            connectIO(builderWrapper.getUrl());
            socket = mSocketIOArray.get(builderWrapper.getUrl());
        } else if (!socket.isConnected()) {
            socket.connect();
        }
        Map<String, SparseArrayCompat<GlintSocketBuilderWrapper>> maps = mOnPushListeners.get(builderWrapper.getUrl());
        if (maps == null) {
            maps = new HashMap<>();
            mOnPushListeners.put(builderWrapper.getUrl(), maps);
        }
        SparseArrayCompat<GlintSocketBuilderWrapper> sparseArrayCompat = maps.get(builderWrapper.getCmdId());
        if (sparseArrayCompat == null) {
            sparseArrayCompat = new SparseArrayCompat<>();
            maps.put(builderWrapper.getCmdId(), sparseArrayCompat);
        }
        sparseArrayCompat.append(builderWrapper.getTag(), builderWrapper);
        socket.on(builderWrapper.getCmdId(), new GlintSocketListener<String>() {
            @Override
            public void onProcess(@NonNull String result) throws Throwable {
                super.onProcess(result);
                Map<String, SparseArrayCompat<GlintSocketBuilderWrapper>> maps = mOnPushListeners.get(builderWrapper.getUrl());
                if (maps != null && maps.containsKey(builderWrapper.getCmdId())) {
                    SparseArrayCompat<GlintSocketBuilderWrapper> wrappers = maps.get(builderWrapper.getCmdId());
                    for (int i = 0; wrappers != null && i < wrappers.size(); i++) {
                        GlintSocketBuilderWrapper socketBuilderWrapper = wrappers.get(wrappers.keyAt(i));
                        try {
                            if (socketBuilderWrapper == null) {
                                continue;
                            }
                            socketBuilderWrapper.onResponse(result);
                        } catch (Exception e) {
                            socketBuilderWrapper.onError(e.getMessage());
                        }
                    }
                }
            }

            @Override
            public void onError(@NonNull String error) {
                super.onError(error);
                try {
                    Map<String, SparseArrayCompat<GlintSocketBuilderWrapper>> maps = mOnPushListeners.get(builderWrapper.getUrl());
                    SparseArrayCompat<GlintSocketBuilderWrapper> wrappers = maps.get(builderWrapper.getCmdId());
                    for (int i = 0; wrappers != null && i < wrappers.size(); i++) {
                        GlintSocketBuilderWrapper socketBuilderWrapper = wrappers.get(wrappers.keyAt(i));
                        try {
                            if (socketBuilderWrapper == null) {
                                continue;
                            }
                            socketBuilderWrapper.onError(error);
                        } catch (Exception e) {
                            try {
                                socketBuilderWrapper.onError(e.getMessage());
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
        GlintSocketCore socket = mSocketArray.get(url);
        if (socket != null) {
            socket.off(cmdId);
            if (TextUtils.isEmpty(cmdId)) {
                socket.disconnect();
                mSocketArray.remove(url);
            }
        }
        GlintSocketIOCore socketIO = mSocketIOArray.get(url);
        if (socketIO != null) {
            socketIO.off(cmdId);
            if (TextUtils.isEmpty(cmdId)) {
                socketIO.disconnect();
                mSocketIOArray.remove(url);
            }
        }
        Map<String, SparseArrayCompat<GlintSocketBuilderWrapper>> cmdIdMaps = mOnPushListeners.get(url);
        if (cmdIdMaps != null) {
            if (TextUtils.isEmpty(cmdId)) {
                cmdIdMaps.clear();
                mOnPushListeners.remove(url);
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
    public void offAll() {
        for (String key : mSocketArray.keySet()) {
            GlintSocketCore socket = mSocketArray.get(key);
            if (socket != null) {
                socket.disconnect();
            }
        }
        for (String key : mSocketIOArray.keySet()) {
            GlintSocketIOCore socket = mSocketIOArray.get(key);
            if (socket != null) {
                socket.disconnect();
            }
        }
        mSocketArray.clear();
        mSocketIOArray.clear();
    }

}