package com.ysbing.glint.http;

import androidx.collection.SparseArrayCompat;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import okhttp3.internal.Util;

public final class GlintHttpDispatcher {

    private static final int mMaxRequests = 32;

    /**
     * Executes calls. Created lazily.
     */
    private ExecutorService mExecutorService;

    /**
     * Ready async calls in the order they'll be run.
     */
    private final Deque<GlintHttpCore<?>> mReadyAsyncCalls = new ArrayDeque<>();

    /**
     * Running asynchronous calls. Includes canceled calls that haven't finished yet.
     */
    private final Deque<GlintHttpCore<?>> mRunningAsyncCalls = new ArrayDeque<>();
    private final SparseArrayCompat<GlintHttpCore<?>> mCallTags = new SparseArrayCompat<>();
    private final SparseArrayCompat<List<Integer>> mTagActivityHashCode = new SparseArrayCompat<>();
    public final List<String> mHostActivityNameList = new ArrayList<>();
    public final List<String> mHostFragmentNameList = new ArrayList<>();
    final List<Integer> mHashCodeList = new ArrayList<>();

    private static final class InstanceHolder {
        static final GlintHttpDispatcher mInstance = new GlintHttpDispatcher();
    }

    public static GlintHttpDispatcher getInstance() {
        return InstanceHolder.mInstance;
    }

    private GlintHttpDispatcher() {
    }

    private synchronized ExecutorService executorService() {
        if (mExecutorService == null) {
            mExecutorService = new ThreadPoolExecutor(0, Integer.MAX_VALUE, 60, TimeUnit.SECONDS,
                    new SynchronousQueue<Runnable>(), Util.threadFactory("Glint Dispatcher", false));
        }
        return mExecutorService;
    }

    public synchronized void executed(GlintHttpCore<?> call) {
        if (mRunningAsyncCalls.size() < mMaxRequests) {
            mRunningAsyncCalls.add(call);
            executorService().execute(call);
        } else {
            mReadyAsyncCalls.add(call);
        }
        mCallTags.put(call.mBuilder.tag, call);

        List<Integer> tags = mTagActivityHashCode.get(call.mBuilder.hostHashCode);
        if (tags == null) {
            tags = new ArrayList<>();
        }
        if (call.mBuilder.tag > 0) {
            tags.add(call.mBuilder.tag);
            mTagActivityHashCode.put(call.mBuilder.hostHashCode, tags);
        }
    }

    public synchronized void cancel(String tag) {
        cancel(tag.hashCode());
    }

    public synchronized void cancel(int tag) {
        GlintHttpCore<?> call = mCallTags.get(tag);
        if (call != null) {
            mCallTags.remove(tag);
            List<Integer> tags = mTagActivityHashCode.get(call.mBuilder.hostHashCode);
            if (tags != null) {
                tags.remove(Integer.valueOf(call.mBuilder.tag));
            }
            mReadyAsyncCalls.remove(call);
            mRunningAsyncCalls.remove(call);
            call.coreCancel();
        }
    }

    public synchronized void cancelAtHashCode(int hashCode) {
        List<Integer> tags = mTagActivityHashCode.get(hashCode);
        if (tags != null) {
            mTagActivityHashCode.remove(hashCode);
            for (Integer tag : tags) {
                if (tag != null) {
                    cancel(tag);
                }
            }
        }
    }

    public synchronized void cancelAll() {
        for (GlintHttpCore<?> call : mReadyAsyncCalls) {
            call.coreCancel();
        }

        for (GlintHttpCore<?> call : mRunningAsyncCalls) {
            call.coreCancel();
        }
        mReadyAsyncCalls.clear();
        mRunningAsyncCalls.clear();
        mCallTags.clear();
        mTagActivityHashCode.clear();
        mHostActivityNameList.clear();
        mHostFragmentNameList.clear();
        mHashCodeList.clear();
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

        for (Iterator<GlintHttpCore<?>> i = mReadyAsyncCalls.iterator(); i.hasNext(); ) {
            GlintHttpCore<?> call = i.next();
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
     * Used by {@code GlintHttpCore#run} to signal completion.
     */
    void finished(GlintHttpCore<?> call) {
        synchronized (this) {
            mCallTags.remove(call.mBuilder.tag);
            List<Integer> tags = mTagActivityHashCode.get(call.mBuilder.hostHashCode);
            if (tags != null) {
                tags.remove(Integer.valueOf(call.mBuilder.tag));
                if (tags.size() == 0) {
                    mTagActivityHashCode.remove(call.mBuilder.hostHashCode);
                }
            }
            finished(mRunningAsyncCalls, call);
        }
    }

    private <T> void finished(Deque<T> calls, T call) {
        calls.remove(call);
        promoteCalls();
    }
}
