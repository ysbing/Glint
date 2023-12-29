package com.ysbing.glint.upload;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import okhttp3.internal.Util;

/**
 * 下载的线程调度
 *
 * @author ysbing
 * 创建于 2018/1/16
 */

public final class GlintUploadDispatcher {

    private static final int maxRequests = 5;

    /**
     * Executes calls. Created lazily.
     */
    private ExecutorService executorService;

    /**
     * Ready async calls in the order they'll be run.
     */
    private final Deque<GlintUploadCore<?>> readyAsyncCalls = new ArrayDeque<>();

    /**
     * Running asynchronous calls. Includes canceled calls that haven't finished yet.
     */
    private final Deque<GlintUploadCore<?>> runningAsyncCalls = new ArrayDeque<>();

    private static final class InstanceHolder {
        static final GlintUploadDispatcher mInstance = new GlintUploadDispatcher();
    }

    public static GlintUploadDispatcher getInstance() {
        return InstanceHolder.mInstance;
    }

    private GlintUploadDispatcher() {
    }

    private synchronized ExecutorService executorService() {
        if (executorService == null) {
            executorService = new ThreadPoolExecutor(0, Integer.MAX_VALUE, 60, TimeUnit.SECONDS,
                    new SynchronousQueue<Runnable>(), Util.threadFactory("GlintUpload Dispatcher", false));
        }
        return executorService;
    }

    public synchronized void executed(GlintUploadCore<?> call) {
        if (runningAsyncCalls.size() < maxRequests) {
            runningAsyncCalls.add(call);
            executorService().execute(call);
        } else {
            readyAsyncCalls.add(call);
        }
    }

    public synchronized void cancel(GlintUploadCore<?> call) {
        if (call != null) {
            readyAsyncCalls.remove(call);
            runningAsyncCalls.remove(call);
            call.coreCancel();
        }
    }

    public synchronized void cancelAll() {
        for (GlintUploadCore<?> call : readyAsyncCalls) {
            call.coreCancel();
        }

        for (GlintUploadCore<?> call : runningAsyncCalls) {
            call.coreCancel();
        }
        readyAsyncCalls.clear();
        runningAsyncCalls.clear();
    }

    private void promoteCalls() {
        // Already running max capacity.
        if (runningAsyncCalls.size() >= maxRequests) {
            return;
        }
        // No ready calls to promote.
        if (readyAsyncCalls.isEmpty()) {
            return;
        }

        for (Iterator<GlintUploadCore<?>> i = readyAsyncCalls.iterator(); i.hasNext(); ) {
            GlintUploadCore<?> call = i.next();
            i.remove();
            runningAsyncCalls.add(call);
            executorService().execute(call);
            // Reached max capacity.
            if (runningAsyncCalls.size() >= maxRequests) {
                return;
            }
        }
    }

    /**
     * Used by {@code GlintUploadCore#run} to signal completion.
     */
    void finished(GlintUploadCore<?> call) {
        synchronized (this) {
            finished(runningAsyncCalls, call);
        }
    }

    private <T> void finished(Deque<T> calls, T call) {
        calls.remove(call);
        promoteCalls();
    }
}
