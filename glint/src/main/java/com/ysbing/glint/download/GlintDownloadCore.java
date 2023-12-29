package com.ysbing.glint.download;

import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.ysbing.glint.base.BaseHttpModule;
import com.ysbing.glint.base.Glint;
import com.ysbing.glint.base.GlintResultBean;
import com.ysbing.glint.util.GlintRequestUtil;
import com.ysbing.glint.util.Md5Util;
import com.ysbing.glint.util.UiKit;

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.charset.Charset;
import java.util.Objects;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.BufferedSource;

/**
 * 下载核心类
 *
 * @author ysbing
 */
public class GlintDownloadCore implements Runnable {
    private static final Glint GLINT = Glint.getsInstance();
    private static final OkHttpClient sClient;
    protected GlintDownloadBuilder<BaseHttpModule> mBuilder;
    private okhttp3.Call mOkHttpCall;
    private boolean mPaused = false;
    /**
     * 是否首次拼接
     */
    private boolean isFirst = true;
    private boolean mFinish = false;
    private GlintDownloadProgressListener mProgressListener;
    final GlintDownloadInfo mDownloadInfo = new GlintDownloadInfo();

    static {
        if (GLINT != null) {
            sClient =
                    GLINT.onOkHttpBuildCreate(Glint.GlintType.DOWNLOAD, new OkHttpClient.Builder())
                            .build();
        } else {
            sClient = new OkHttpClient.Builder().build();
        }
    }

    public GlintDownloadBuilder<BaseHttpModule> getBuilder() {
        return mBuilder;
    }

    protected GlintDownloadBuilder<BaseHttpModule> createBuilder() {
        return new GlintDownloadBuilder<>(GLINT);
    }

    protected void moduleUsing(BaseHttpModule module) {
        GlintDownloadBuilder<BaseHttpModule> newBuilder = new GlintDownloadBuilder<>(GLINT, false);
        newBuilder.url = mBuilder.url;
        newBuilder.saveFile = mBuilder.saveFile;
        newBuilder.params = mBuilder.params;
        newBuilder.listeners = mBuilder.listeners;
        newBuilder.customGlintModule = mBuilder.customGlintModule;
        module.configDefaultBuilder(newBuilder);
        newBuilder.addCustomGlintModule(module);
        mBuilder = newBuilder;
    }

    protected void corePause() {
        mPaused = true;
        if (mOkHttpCall != null && mOkHttpCall.isExecuted()) {
            mOkHttpCall.cancel();
        }
        UiKit.runOnMainThreadAsync(new Runnable() {
            @Override
            public void run() {
                for (GlintDownloadListener listener : mBuilder.listeners) {
                    listener.onPause();
                }
            }
        });
    }

    protected void coreCancel() {
        if (mOkHttpCall != null && mOkHttpCall.isExecuted()) {
            mOkHttpCall.cancel();
        }
        UiKit.runOnMainThreadAsync(new Runnable() {
            @Override
            public void run() {
                for (GlintDownloadListener listener : mBuilder.listeners) {
                    listener.onCancel();
                    listener.onFinish();
                }
                //将回调设置为空，就不会回调到上层
                mBuilder.listeners.clear();
                mFinish = true;
            }
        });
    }

    protected boolean isFinish() {
        return mFinish;
    }

    @Override
    public void run() {
        okhttp3.Response response = null;
        try {
            prepare();
            response = mOkHttpCall.execute();
            // 比较MD5，如果相同的话，就不用下载了
            String contentMd5 = mBuilder.md5;
            if (TextUtils.isEmpty(contentMd5)) {
                contentMd5 = response.header("ETag");
                if (contentMd5 != null) {
                    contentMd5 = contentMd5.replace("\"", "").toLowerCase();
                    if (contentMd5.length() != 32) {
                        contentMd5 = null;
                    }
                }
            }
            // 得到返回数据
            ResponseBody responseBody = response.body();
            if (responseBody == null) {
                return;
            }
            final String fileName = GlintRequestUtil.getHeaderFileName(response);
            long contentLength = responseBody.contentLength();
            if (mBuilder.contentLength == contentLength) {
                mBuilder.range = 0;
            }
            if (mBuilder.range == 0) {
                mBuilder.contentLength = contentLength;
            }
            if (mBuilder.saveFile.isDirectory()) {
                mBuilder.saveFile = new File(mBuilder.saveFile, fileName);
            }
            boolean isCancel = false;
            for (GlintDownloadListener listener : mBuilder.listeners) {
                if (listener.onPrepared(mBuilder)) {
                    isCancel = true;
                    break;
                }
            }
            if (isCancel) {
                for (GlintDownloadListener listener : mBuilder.listeners) {
                    coreCancel();
                    listener.onFinish();
                }
                return;
            }
            if (contentMd5 != null && TextUtils.equals(Md5Util.getMD5Str(mBuilder.saveFile), contentMd5)) {
                final Response finalResponse = response;
                if (mBuilder.mainThread) {
                    UiKit.runOnMainThreadAsync(new Runnable() {
                        @Override
                        public void run() {
                            for (GlintDownloadListener listener : mBuilder.listeners) {
                                try {
                                    listener.onProgress(mBuilder.saveFile.length(),
                                            mBuilder.saveFile.length(),
                                            mBuilder.saveFile.length(), 100);
                                    listener.onSuccess(mBuilder.saveFile);
                                } catch (Throwable e) {
                                    listener.onDownloadFail(e, finalResponse);
                                } finally {
                                    listener.onFinish();
                                }
                            }
                        }
                    });
                } else {
                    for (GlintDownloadListener listener : mBuilder.listeners) {
                        try {
                            listener.onProgress(mBuilder.saveFile.length(),
                                    mBuilder.saveFile.length(),
                                    mBuilder.saveFile.length(), 100);
                            listener.onSuccess(mBuilder.saveFile);
                        } catch (Throwable e) {
                            listener.onDownloadFail(e, finalResponse);
                        } finally {
                            listener.onFinish();
                        }
                    }
                }
                return;
            }
            int len;
            byte[] buffer = new byte[2048];
            File tempFile = new File(mBuilder.saveFile.getAbsolutePath() + ".temp");
            BufferedSource source = responseBody.source();
            if (!Objects.requireNonNull(mBuilder.saveFile.getParentFile()).exists()) {
                boolean makeDirResult = mBuilder.saveFile.getParentFile().mkdirs();
                if (!makeDirResult) {
                    throw new RuntimeException("Create folder failed, please manually create:" +
                            mBuilder.saveFile.getParentFile());
                }
            } else if (mBuilder.range == 0) {
                //noinspection ResultOfMethodCallIgnored
                mBuilder.saveFile.delete();
                //noinspection ResultOfMethodCallIgnored
                tempFile.delete();
            }
            isFirst = mBuilder.range == 0;
            RandomAccessFile sink = new RandomAccessFile(tempFile, "rwd");
            sink.seek(mBuilder.range);
            while (!mPaused && (len = source.read(buffer)) != -1) {
                sink.write(buffer, 0, len);
                mBuilder.range += len;
                deliverProgress(mBuilder.range, mBuilder.contentLength);
            }
            sink.close();
            source.close();
            if (!mPaused) {
                // 将数据装载到ResultBean中
                GlintResultBean<File> result = new GlintResultBean<>();
                result.setData(mBuilder.saveFile);
                if (mBuilder.checkMd5 && contentMd5 != null &&
                        !TextUtils.equals(Md5Util.getMD5Str(tempFile), contentMd5)) {
                    result.setRunStatus(Glint.ResultStatus.STATUS_ERROR);
                } else {
                    boolean statusResult = true;
                    if (mBuilder.saveFile.exists()) {
                        statusResult = mBuilder.saveFile.delete();
                    }
                    if (statusResult) {
                        result.setRunStatus(tempFile.renameTo(mBuilder.saveFile) ?
                                Glint.ResultStatus.STATUS_SUCCESS :
                                Glint.ResultStatus.STATUS_ERROR);
                    } else {
                        result.setRunStatus(Glint.ResultStatus.STATUS_ERROR);
                    }
                }
                isFirst = true;
                deliverResponse(result, response);
            }
        } catch (Throwable e) {
            if (!mPaused) {
                deliverError(e, response);
            }
        } finally {
            if (response != null) {
                response.close();
            }
            if (!mPaused && isFirst) {
                GlintDownloadDispatcher.getInstance().finished(this);
                mFinish = true;
            }
        }
    }

    private void prepare() throws Throwable {
        final boolean isResume = mBuilder.range > 0;
        UiKit.runOnMainThreadAsync(new Runnable() {
            @Override
            public void run() {
                for (GlintDownloadListener listener : mBuilder.listeners) {
                    listener.onStart();
                    if (isResume) {
                        listener.onResume();
                    }
                }
            }
        });
        String newUrl;
        if (!mBuilder.customGlintModule.isEmpty()) {
            //传递所有配置到自定义Module
            for (BaseHttpModule baseHttpModule : mBuilder.customGlintModule) {
                baseHttpModule.onBuilderCreated(mBuilder.clone());
            }
            //传递头部到自定义Module
            for (BaseHttpModule baseHttpModule : mBuilder.customGlintModule) {
                boolean transitive = baseHttpModule.getHeaders(mBuilder.headers);
                if (!transitive) {
                    break;
                }
            }
            //传递URL到自定义Module
            newUrl = mBuilder.url;
            for (BaseHttpModule baseHttpModule : mBuilder.customGlintModule) {
                BaseHttpModule.UrlResult urlResult = baseHttpModule.getUrl(newUrl);
                newUrl = urlResult.url;
                if (!urlResult.transitive) {
                    break;
                }
            }
            //传递参数到自定义Module
            for (BaseHttpModule baseHttpModule : mBuilder.customGlintModule) {
                boolean transitive = baseHttpModule.getParams(mBuilder.params);
                if (!transitive) {
                    break;
                }
            }
        } else if (GLINT != null) {
            GLINT.onBuilderCreated(mBuilder.clone());
            GLINT.getParams(mBuilder.params);
            GLINT.getHeaders(mBuilder.headers);
            BaseHttpModule.UrlResult urlResult = GLINT.getUrl(mBuilder.url);
            newUrl = urlResult.url;
        } else {
            newUrl = mBuilder.url;
        }
        MediaType contentType = MediaType.parse(mBuilder.mimeType);
        String paramsEncoding;
        if (contentType == null) {
            paramsEncoding = GlintRequestUtil.UTF_8.name();
        } else {
            Charset charset = contentType.charset(GlintRequestUtil.UTF_8);
            if (charset == null) {
                paramsEncoding = GlintRequestUtil.UTF_8.name();
            } else {
                paramsEncoding = charset.name();
            }
        }
        String params = GlintRequestUtil.encodeParameters(mBuilder.params, paramsEncoding);
        String requestUrl;
        if (newUrl.contains("?")) {
            if (!newUrl.endsWith("&") && !newUrl.endsWith("?")) {
                requestUrl = newUrl + "&" + params;
            } else {
                requestUrl = newUrl + params;
            }
        } else {
            requestUrl = newUrl + "?" + params;
        }
        okhttp3.Request.Builder okHttpRequestBuilder = new okhttp3.Request.Builder()
                .url(mBuilder.url = requestUrl);
        //添加头部到请求里
        okHttpRequestBuilder.headers(mBuilder.headers.build());
        okHttpRequestBuilder.header("RANGE", "bytes=" + mBuilder.range + "-");
        if (!TextUtils.isEmpty(mBuilder.cookie)) {
            okHttpRequestBuilder.addHeader("Cookie", mBuilder.cookie);
        }
        mOkHttpCall = sClient.newCall(okHttpRequestBuilder.build());
        mProgressListener = new GlintDownloadProgressListener(mBuilder.listeners);
        mPaused = false;
    }

    private void deliverProgress(long bytesWritten, long contentLength) {
        if (mBuilder.listeners.isEmpty() || mProgressListener == null) {
            return;
        }
        mDownloadInfo.bytesWritten = bytesWritten;
        mDownloadInfo.contentLength = contentLength;
        mDownloadInfo.progress = contentLength > 0 ? (int) (bytesWritten * 100 / contentLength) : 0;
        mProgressListener
                .onProgressChanged(mDownloadInfo.bytesWritten, mDownloadInfo.contentLength);
    }

    private void deliverResponse(GlintResultBean<File> response, Response httpResponse) {
        if (mBuilder.listeners.isEmpty()) {
            return;
        }
        if (mBuilder.mainThread) {
            UiKit.runOnMainThreadAsync(new ResultRunnable(response, httpResponse));
        } else {
            new ResultRunnable(response, httpResponse).run();
        }
    }

    private void deliverError(@NonNull Throwable error, @Nullable Response response) {
        if (mBuilder.listeners.isEmpty()) {
            return;
        }
        if (mBuilder.mainThread) {
            UiKit.runOnMainThreadAsync(new ErrorRunnable(error, response));
        } else {
            new ErrorRunnable(error, response).run();
        }
    }

    private class ResultRunnable implements Runnable {
        private final GlintResultBean<File> response;
        private final Response httpResponse;

        ResultRunnable(GlintResultBean<File> response, Response httpResponse) {
            this.response = response;
            this.httpResponse = httpResponse;
        }

        @Override
        public synchronized void run() {
            if (mProgressListener != null) {
                String filePath = response.getData().getAbsolutePath();
                for (GlintDownloadListener listener : mBuilder.listeners) {
                    try {
                        listener.onSuccess(new File(filePath));
                    } catch (Throwable e) {
                        listener.onDownloadFail(e, httpResponse);
                    }
                }
                if (!mPaused && isFirst) {
                    for (GlintDownloadListener listener : mBuilder.listeners) {
                        listener.onFinish();
                    }
                }
            }
        }
    }

    private class ErrorRunnable implements Runnable {
        private final Throwable error;
        private final Response httpResponse;

        ErrorRunnable(Throwable error, Response httpResponse) {
            this.error = error;
            this.httpResponse = httpResponse;
        }

        @Override
        public synchronized void run() {
            for (GlintDownloadListener listener : mBuilder.listeners) {
                listener.onDownloadFail(error, httpResponse);
            }
            if (!mPaused && isFirst) {
                for (GlintDownloadListener listener : mBuilder.listeners) {
                    listener.onFinish();
                }
            }
        }
    }
}
