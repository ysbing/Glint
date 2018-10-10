package com.ysbing.glint.download;

import android.support.annotation.NonNull;
import android.support.v4.util.SparseArrayCompat;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import okhttp3.internal.Util;

/**
 * 下载的线程调度
 *
 * @author ysbing
 */
public final class GlintDownloadDispatcher {
    private static GlintDownloadDispatcher mInstance;

    private static final int mMaxRequests = 5;

    /**
     * Executes calls. Created lazily.
     */
    private ExecutorService mExecutorService;

    /**
     * Ready async calls in the order they'll be run.
     */
    private final Deque<GlintDownloadCore> mReadyAsyncCalls = new ArrayDeque<>();

    /**
     * Running asynchronous calls. Includes canceled calls that haven't finished yet.
     */
    private final List<GlintDownloadCore> mRunningAsyncCalls = new CopyOnWriteArrayList<>();
    private final SparseArrayCompat<GlintDownloadCore> mCallTags = new SparseArrayCompat<>();

    public static GlintDownloadDispatcher getInstance() {
        if (mInstance == null) {
            synchronized (GlintDownloadDispatcher.class) {
                if (mInstance == null) {
                    mInstance = new GlintDownloadDispatcher();
                }
            }
        }
        return mInstance;
    }

    private GlintDownloadDispatcher() {
    }

    private synchronized ExecutorService executorService() {
        if (mExecutorService == null) {
            mExecutorService = new ThreadPoolExecutor(0, Integer.MAX_VALUE, 60, TimeUnit.SECONDS,
                    new SynchronousQueue<Runnable>(), Util.threadFactory("GlintDownload Dispatcher", false));
        }
        return mExecutorService;
    }

    public synchronized void executed(GlintDownloadCore call) {
        if (mRunningAsyncCalls.size() < mMaxRequests) {
            mRunningAsyncCalls.add(call);
            executorService().execute(call);
        } else {
            mReadyAsyncCalls.add(call);
        }
        mCallTags.put(call.mBuilder.tag, call);
    }

    public synchronized void pause(int tag) {
        GlintDownloadCore call = mCallTags.get(tag);
        if (call != null) {
            call.corePause();
        }
    }

    public synchronized void resume(int tag) {
        GlintDownloadCore call = mCallTags.get(tag);
        if (call != null) {
            executorService().execute(call);
        }
    }

    public synchronized void cancel(@NonNull String tag) {
        cancel(tag.hashCode());
    }

    public synchronized void cancel(int tag) {
        GlintDownloadCore call = mCallTags.get(tag);
        if (call != null) {
            mCallTags.remove(tag);
            mReadyAsyncCalls.remove(call);
            mRunningAsyncCalls.remove(call);
            call.coreCancel();
        }
    }

    public GlintDownloadInfo getDownloaderInfo(@NonNull String tag) {
        return getDownloaderInfo(tag.hashCode());
    }

    public GlintDownloadInfo getDownloaderInfo(int tag) {
        GlintDownloadCore call = mCallTags.get(tag);
        if (call != null) {
            return call.mDownloadInfo;
        }
        return null;
    }

    public List<GlintDownloadCore> getDownloaderList() {
        return mRunningAsyncCalls;
    }

    public synchronized void cancelAll() {
        for (GlintDownloadCore call : mReadyAsyncCalls) {
            call.coreCancel();
        }

        for (GlintDownloadCore call : mRunningAsyncCalls) {
            call.coreCancel();
        }
        mCallTags.clear();
        mReadyAsyncCalls.clear();
        mRunningAsyncCalls.clear();
    }

    private void promoteCalls() {
        // Already running max capacity.
        if (mRunningAsyncCalls.size() >= mMaxRequests) {
            return;
        }
        // No ready calls to promote.
        if (mReadyAsyncCalls.isEmpty()) {
            return;
        }

        for (Iterator<GlintDownloadCore> i = mReadyAsyncCalls.iterator(); i.hasNext(); ) {
            GlintDownloadCore call = i.next();
            i.remove();
            mRunningAsyncCalls.add(call);
            executorService().execute(call);
            // Reached max capacity.
            if (mRunningAsyncCalls.size() >= mMaxRequests) {
                return;
            }
        }
    }

    /**
     * Used by {@code GlintDownloadCore#run} to signal completion.
     */
    void finished(GlintDownloadCore call) {
        synchronized (this) {
            mCallTags.remove(call.mBuilder.tag);
            finished(mRunningAsyncCalls, call);
        }
    }

    private <T> void finished(List<T> calls, T call) {
        calls.remove(call);
        promoteCalls();
    }
}
