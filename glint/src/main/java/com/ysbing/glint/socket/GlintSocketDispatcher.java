
package com.ysbing.glint.socket;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Socket请求类，请求的入口
 *
 * @author ysbing
 */
public class GlintSocketDispatcher implements ServiceConnection {

    private static GlintSocketDispatcher mInstance = new GlintSocketDispatcher();

    private final LinkedBlockingQueue<GlintSocketBuilderWrapper> mQueue = new LinkedBlockingQueue<>();
    private final List<GlintSocketBuilder> mSocketTaskBuilders = new ArrayList<>();
    private final GlintSocketServiceStub mSocketServiceStub;

    private GlintSocketService mService;
    private boolean mInit;

    private GlintSocketDispatcher() {
        Worker worker = new Worker();
        worker.start();
        mSocketServiceStub = new GlintSocketServiceStub();
    }

    public static GlintSocketDispatcher getInstance() {
        if (mInstance == null) {
            synchronized (GlintSocketDispatcher.class) {
                if (mInstance == null) {
                    mInstance = new GlintSocketDispatcher();
                }
            }
        }
        return mInstance;
    }

    /**
     * 如果有跨进程需求，就做初始化
     * 在单一进程使用的话，不需要初始化
     *
     * @param context 上下文对象
     */
    void init(Context context) {
        mInit = true;
        if (mService == null) {
            Intent i = new Intent(context, GlintSocketServiceNative.class);
            context.bindService(i, mInstance, Service.BIND_AUTO_CREATE);
        }
    }

    /**
     * 发送一条socket消息
     */
    <T> void send(@NonNull GlintSocketBuilder<T> builder) {
        mQueue.offer(new GlintSocketBuilderStub<>(builder));
    }

    /**
     * 设置一个推送监听
     */
    public <T> void on(@NonNull GlintSocketBuilder<T> builder) {
        if (mService == null && mInit) {
            mSocketTaskBuilders.add(builder);
            return;
        }
        try {
            if (mService != null) {
                mService.connect(builder.url);
            } else {
                mSocketServiceStub.connect(builder.url);
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        try {
            if (mService != null) {
                mService.on(new GlintSocketBuilderStub<>(builder));
            } else {
                mSocketServiceStub.on(new GlintSocketBuilderStub<>(builder));
            }
        } catch (Exception e) {
            if (builder.listener != null) {
                builder.listener.onError(e.getMessage());
            }
        }
    }

    /**
     * 移除发送队列
     */
    void removePushListener(GlintSocketBuilder builder) {
        try {
            if (mService != null) {
                mService.off(builder.url, builder.cmdId, builder.tag);
            } else {
                mSocketServiceStub.off(builder.url, builder.cmdId, builder.tag);
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    /**
     * 删除所有，包括Socket的连接
     */
    void removeAll() {
        try {
            if (mService != null) {
                mService.offAll();
            } else {
                mSocketServiceStub.offAll();
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }


    @Override
    public void onServiceConnected(ComponentName name, IBinder iBinder) {
        try {
            mService = GlintSocketService.Stub.asInterface(iBinder);
            for (GlintSocketBuilder taskBuilder : mSocketTaskBuilders) {
                on(taskBuilder);
            }
        } catch (Exception e) {
            mService = null;
        }
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        mService = null;
    }

    private void continueProcessTaskWrappers() {
        try {
            GlintSocketBuilderWrapper taskWrapper = mQueue.take();
            if (mService != null) {
                if (taskWrapper == null) {
                    return;
                }
                mService.send(taskWrapper);
            } else {
                if (taskWrapper == null) {
                    return;
                }
                mSocketServiceStub.send(taskWrapper);
            }
        } catch (Exception ignored) {
        }
    }

    private class Worker extends Thread {

        @Override
        public void run() {
            //这样写可以防止代码注入
            //noinspection InfiniteLoopStatement
            for (; ; ) {
                continueProcessTaskWrappers();
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    //
                }
            }
        }
    }

}
