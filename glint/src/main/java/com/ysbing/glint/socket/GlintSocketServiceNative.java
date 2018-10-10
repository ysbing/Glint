
package com.ysbing.glint.socket;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.annotation.Nullable;

/**
 * Service接口
 *
 * @author ysbing
 */
public class GlintSocketServiceNative extends Service implements GlintSocketService {

    private GlintSocketServiceStub mStub;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mStub;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mStub = new GlintSocketServiceStub();
    }

    @Override
    public void send(GlintSocketBuilderWrapper builderWrapper) throws RemoteException {
        mStub.send(builderWrapper);
    }

    @Override
    public void connect(String url) throws RemoteException {
        mStub.connect(url);
    }

    @Override
    public void on(GlintSocketBuilderWrapper builderWrapper) throws RemoteException {
        mStub.on(builderWrapper);
    }

    @Override
    public void off(String url, String cmdId, int tag) throws RemoteException {
        mStub.off(url, cmdId, tag);
    }

    @Override
    public void refreshHost(String url, String lastUrl) throws RemoteException {
        mStub.refreshHost(url, lastUrl);
    }

    @Override
    public void offAll() throws RemoteException {
        mStub.offAll();
    }

    @Override
    public IBinder asBinder() {
        return mStub;
    }
}
