package com.ysbing.glint.download;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.io.File;
import java.util.concurrent.CopyOnWriteArrayList;

import okhttp3.Response;

/**
 * 下载的监听分发
 *
 * @author ysbing
 */
final class GlintDownloadProgressListener {
    private static final int WHAT_UPDATE = 0x01;
    private static final String KEY_BYTES_WRITTEN = "KEY_BYTES_WRITTEN";
    private static final String KEY_CONTENT_LENGTH = "KEY_CONTENT_LENGTH";
    private static final String KEY_SPEED = "KEY_SPEED";
    private static final String KEY_PROGRESS = "KEY_PROGRESS";
    private Handler mHandler;
    private final CopyOnWriteArrayList<GlintDownloadListener> mListeners;
    private long mLastRefreshTime;
    private long mLastBytesWritten;
    private int mLastProgress;

    GlintDownloadProgressListener(CopyOnWriteArrayList<GlintDownloadListener> listeners) {
        this.mListeners = listeners;
    }

    void onProgressChanged(final long bytesWritten, final long contentLength) {
        int progress = contentLength > 0 ? (int) (bytesWritten * 100 / contentLength) : 0;
        long time = System.currentTimeMillis();
        if ((progress - mLastProgress > 0 || time - mLastRefreshTime > 200)) {
            long lastSpeed = (time - mLastRefreshTime) > 0 ? (bytesWritten - mLastBytesWritten) * 1000 / (time - mLastRefreshTime) : 0L;
            mLastRefreshTime = time;
            mLastProgress = progress;
            mLastBytesWritten = bytesWritten;
            ensureHandler();
            Message message = mHandler.obtainMessage();
            message.what = WHAT_UPDATE;
            Bundle data = new Bundle();
            data.putLong(KEY_BYTES_WRITTEN, bytesWritten);
            data.putLong(KEY_CONTENT_LENGTH, contentLength);
            data.putLong(KEY_SPEED, lastSpeed);
            data.putInt(KEY_PROGRESS, progress);
            message.setData(data);
            mHandler.sendMessage(message);
        }
    }

    private void ensureHandler() {
        if (mHandler != null) {
            return;
        }
        mHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                if (msg == null) {
                    return;
                }
                if (msg.what == WHAT_UPDATE) {
                    Bundle updateData = msg.getData();
                    if (updateData == null) {
                        return;
                    }
                    final long bytesWritten = updateData.getLong(KEY_BYTES_WRITTEN);
                    final long contentLength = updateData.getLong(KEY_CONTENT_LENGTH);
                    final long speed = updateData.getLong(KEY_SPEED);
                    final int progress = updateData.getInt(KEY_PROGRESS);
                    for (GlintDownloadListener listener : mListeners) {
                        try {
                            listener.onProgress(bytesWritten, contentLength, speed, progress);
                        } catch (Exception ignored) {
                        }
                    }
                }
            }
        };
    }
}