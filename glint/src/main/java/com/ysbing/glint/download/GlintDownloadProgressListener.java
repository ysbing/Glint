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
    private static final int WHAT_SUCCESS = 0x02;
    private static final int WHAT_ERROR = 0x03;
    private static final String KEY_BYTES_WRITTEN = "KEY_BYTES_WRITTEN";
    private static final String KEY_CONTENT_LENGTH = "KEY_CONTENT_LENGTH";
    private static final String KEY_SPEED = "KEY_SPEED";
    private static final String KEY_PROGRESS = "KEY_PROGRESS";
    private static final String KEY_FILE_PATH = "KEY_FILE_PATH";
    private static final String KEY_ERROR = "KEY_ERROR";
    private Handler mHandler;
    private final CopyOnWriteArrayList<GlintDownloadListener> mListeners;
    private Response mResponse;
    private long mLastRefreshTime;
    private long mLastBytesWritten;
    private int mLastProgress;

    GlintDownloadProgressListener(CopyOnWriteArrayList<GlintDownloadListener> listeners) {
        this.mListeners = listeners;
    }

    void onProgressChanged(final long bytesWritten, final long contentLength) throws Exception {
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

    /**
     * 进度结束
     */
    void onProgressSuccess(String filePath) {
        ensureHandler();
        Message message = mHandler.obtainMessage();
        message.what = WHAT_SUCCESS;
        Bundle data = new Bundle();
        data.putString(KEY_FILE_PATH, filePath);
        message.setData(data);
        mHandler.sendMessage(message);
    }

    /**
     * 进度结束
     */
    void onProgressError(@NonNull Throwable error, @Nullable Response response) {
        this.mResponse = response;
        ensureHandler();
        Message message = mHandler.obtainMessage();
        message.what = WHAT_ERROR;
        Bundle data = new Bundle();
        data.putSerializable(KEY_ERROR, error);
        message.setData(data);
        mHandler.sendMessage(message);
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
                switch (msg.what) {
                    case WHAT_UPDATE:
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
                        break;
                    case WHAT_SUCCESS:
                        Bundle successData = msg.getData();
                        if (successData == null) {
                            return;
                        }
                        final String filePath = successData.getString(KEY_FILE_PATH);
                        if (filePath == null) {
                            return;
                        }
                        for (GlintDownloadListener listener : mListeners) {
                            try {
                                // 如果是200，则是正确的千帆成功返回
                                // 如果是0，则是正确的非千帆成功返回
                                listener.onSuccess(new File(filePath));
                            } catch (Exception e) {
                                listener.onDownloadFail(e, null);
                            } finally {
                                listener.onFinish();
                            }
                        }
                        break;
                    case WHAT_ERROR:
                        Bundle errorData = msg.getData();
                        if (errorData == null) {
                            return;
                        }
                        Throwable error = (Throwable) errorData.getSerializable(KEY_ERROR);
                        if (error == null) {
                            return;
                        }
                        for (GlintDownloadListener listener : mListeners) {
                            try {
                                listener.onDownloadFail(error, mResponse);
                            } finally {
                                listener.onFinish();
                            }
                        }
                        break;
                    default:
                        break;
                }
            }
        };
    }
}