package com.ysbing.glint.upload;

import android.text.TextUtils;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.google.gson.internal.Primitives;
import com.google.gson.stream.JsonReader;
import com.ysbing.glint.base.BaseHttpModule;
import com.ysbing.glint.base.Glint;
import com.ysbing.glint.base.GlintResultBean;
import com.ysbing.glint.util.GlintRequestUtil;
import com.ysbing.glint.util.UiKit;

import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.Map;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import okhttp3.internal.Util;


/**
 * 上传核心类
 *
 * @author ysbing
 * 创建于 2018/1/16
 */
public class GlintUploadCore<T> implements Runnable {
    private static final Glint GLINT = Glint.getsInstance();
    private static final Gson gson = new Gson();
    private static OkHttpClient client;
    protected GlintUploadBuilder<T, BaseHttpModule> mBuilder;
    private Type mTypeOfT;
    private okhttp3.Call mOkHttpCall;

    static {
        if (GLINT != null) {
            client = GLINT.onOkHttpBuildCreate(Glint.GlintType.UPLOAD, new OkHttpClient.Builder()).build();
        } else {
            client = new OkHttpClient.Builder().build();
        }
    }

    public GlintUploadBuilder<T, BaseHttpModule> getBuilder() {
        return mBuilder;
    }

    protected GlintUploadBuilder createBuilder() {
        return new GlintUploadBuilder(GLINT);
    }

    protected void moduleUsing(BaseHttpModule module) {
        GlintUploadBuilder<T, BaseHttpModule> newBuilder = new GlintUploadBuilder<>(GLINT, false);
        newBuilder.url = mBuilder.url;
        newBuilder.file = mBuilder.file;
        newBuilder.data = mBuilder.data;
        newBuilder.keyName = mBuilder.keyName;
        newBuilder.params = mBuilder.params;
        newBuilder.listener = mBuilder.listener;
        newBuilder.customGlintModule = mBuilder.customGlintModule;
        module.configDefaultBuilder(newBuilder);
        newBuilder.addCustomGlintModule(module);
        mBuilder = newBuilder;
    }

    protected void coreCancel() {
        if (mOkHttpCall != null && mOkHttpCall.isExecuted()) {
            mOkHttpCall.cancel();
        }
        if (mBuilder.listener != null) {
            mBuilder.listener.onCancel();
            //将回调设置为空，就不会回调到上层
            mBuilder.listener = null;
        }
    }

    @Override
    public void run() {
        okhttp3.Response response = null;
        try {
            prepare();
            response = mOkHttpCall.execute();
            if (mBuilder.listener == null) {
                return;
            }
            // 得到返回数据
            ResponseBody responseBody = response.body();
            if (responseBody == null) {
                return;
            }
            // 将数据装载到ResultBean中
            GlintResultBean<T> result = new GlintResultBean<>();
            T t;

            // 如果不是标准的json数据，直接将整个数据返回
            if (mBuilder.notJson) {
                String responseStr = responseBody.string();
                //noinspection unchecked,ConstantConditions
                t = (T) Primitives.wrap(String.class.getSuperclass()).cast(responseStr);
                result.setRunStatus(Glint.ResultStatus.STATUS_SUCCESS);
                result.setResponseStr(responseStr);
                result.setHeaders(response.headers());
                result.setData(t);
                deliverResponse(result);
                return;
            }
            //开始对数据做解析处理
            JsonReader jsonReader = new JsonReader(new InputStreamReader(responseBody.byteStream(), Util.UTF_8));
            JsonParser parser = new JsonParser();
            JsonElement jsonEl;
            try {
                jsonEl = parser.parse(jsonReader);
            } catch (JsonSyntaxException e) {
                deliverError(e);
                return;
            }
            String responseStr = jsonEl.toString();
            result.setResponseStr(responseStr);
            result.setHeaders(response.headers());

            // 转换成Json对象
            JsonObject jsonObj = jsonEl.getAsJsonObject();
            if (!mBuilder.customGlintModule.isEmpty()) {
                for (BaseHttpModule baseHttpModule : mBuilder.customGlintModule) {
                    boolean transitive = baseHttpModule.customDeserialize(result, jsonObj, gson, mTypeOfT);
                    if (!transitive) {
                        break;
                    }
                }
            } else if (GLINT != null && !mBuilder.standardDeserialize) {
                GLINT.customDeserialize(result, jsonObj, gson, mTypeOfT);
            } else {
                result.setRunStatus(Glint.ResultStatus.STATUS_SUCCESS);
                result.setData(GlintRequestUtil.<T>successDeserialize(gson, jsonObj, mTypeOfT));
            }
            deliverResponse(result);
        } catch (Exception e) {
            deliverError(e);
        } finally {
            if (response != null) {
                response.close();
            }
            GlintUploadDispatcher.getInstance().finished(this);
        }
    }

    private void prepare() throws Exception {
        if (mBuilder.listener != null) {
            UiKit.runOnMainThreadAsync(new Runnable() {
                @Override
                public void run() {
                    mBuilder.listener.onStart();
                }
            });
            mTypeOfT = GlintRequestUtil.getListenerType(mBuilder.listener.getClass());
        }
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
        RequestBody file;
        if (mBuilder.file != null) {
            file = RequestBody.create(contentType, mBuilder.file);
        } else {
            file = RequestBody.create(contentType, mBuilder.data);
        }
        MultipartBody.Builder mb = new MultipartBody.Builder();
        mb.setType(MultipartBody.FORM);
        mb.addFormDataPart("file", mBuilder.keyName, file);
        for (Map.Entry<String, String> entry : mBuilder.params.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null) {
                continue;
            }
            mb.addFormDataPart(entry.getKey(), entry.getValue());
        }
        RequestBody body = mb.build();
        body = new GlintUploadCountingRequestBody(body, mBuilder.listener);
        okhttp3.Request.Builder okHttpRequestBuilder = new okhttp3.Request.Builder()
                .post(body)
                .url(newUrl);
        //添加头部到请求里
        okHttpRequestBuilder.headers(mBuilder.headers.build());
        if (!TextUtils.isEmpty(mBuilder.cookie)) {
            okHttpRequestBuilder.addHeader("Cookie", mBuilder.cookie);
        }
        mOkHttpCall = client.newCall(okHttpRequestBuilder.build());
    }

    private void deliverResponse(GlintResultBean<T> response) {
        if (mBuilder.listener == null) {
            return;
        }
        if (mBuilder.mainThread) {
            UiKit.runOnMainThreadAsync(new ResultRunnable(response));
        } else {
            new ResultRunnable(response).run();
        }
    }

    private void deliverError(Exception error) {
        if (mBuilder.listener == null) {
            return;
        }
        if (mBuilder.mainThread) {
            UiKit.runOnMainThreadAsync(new ErrorRunnable(error));
        } else {
            new ErrorRunnable(error).run();
        }
    }

    private class ResultRunnable implements Runnable {
        private final GlintResultBean<T> response;

        ResultRunnable(GlintResultBean<T> response) {
            this.response = response;
        }

        @Override
        public synchronized void run() {
            if (mBuilder.listener == null) {
                return;
            }
            try {
                // 如果是200，则是正确的成功返回
                // 如果是0，则是正确的非成功返回
                mBuilder.listener.onResponse(response);
                if (response.getRunStatus() == Glint.ResultStatus.STATUS_SUCCESS) {
                    mBuilder.listener.onSuccess(response.getData());
                } else {
                    mBuilder.listener.onError(response.getStatus(), response.getMessage());
                    mBuilder.listener.onErrorOrFail();
                }
            } catch (Throwable e) {
                if (mBuilder.listener != null) {
                    mBuilder.listener.onFail(e);
                    mBuilder.listener.onErrorOrFail();
                }
            } finally {
                if (mBuilder.listener != null) {
                    mBuilder.listener.onFinish();
                }
            }
        }
    }

    private class ErrorRunnable implements Runnable {
        private final Exception error;

        ErrorRunnable(Exception error) {
            this.error = error;
        }

        @Override
        public synchronized void run() {
            if (mBuilder.listener == null) {
                return;
            }
            try {
                mBuilder.listener.onFail(error);
            } finally {
                if (mBuilder.listener != null) {
                    mBuilder.listener.onErrorOrFail();
                    mBuilder.listener.onFinish();
                }
            }
        }
    }
}
