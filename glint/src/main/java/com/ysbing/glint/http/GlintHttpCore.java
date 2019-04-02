package com.ysbing.glint.http;

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
import com.ysbing.glint.util.ContextHelper;
import com.ysbing.glint.util.GlintRequestUtil;
import com.ysbing.glint.util.UiKit;

import java.io.File;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.charset.Charset;

import okhttp3.Cache;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import okhttp3.internal.Util;
import okio.ByteString;

/**
 * 根据网络请求封装的解析类
 * 在状态200的时候Message是数据
 * 非200的时候是String类型的错误消息
 *
 * @author ysbing
 */
public class GlintHttpCore<T> implements Runnable {
    private static final Glint GLINT = Glint.getsInstance();
    private static OkHttpClient sClient;
    private static final Cache sCache = new Cache(new File(ContextHelper.getAppContext().getCacheDir(), "glint_http"), 1024 * 1024 * 10);
    private static final Gson sGson = new Gson();

    /**
     * 用户带有List之类的类型
     */
    protected GlintHttpBuilder<T, BaseHttpModule> mBuilder;
    protected Type mTypeOfT;
    private okhttp3.Call mOkHttpCall;

    static {
        if (GLINT != null) {
            sClient = GLINT.onOkHttpBuildCreate(Glint.GlintType.HTTP, new OkHttpClient.Builder()).build();
        } else {
            sClient = new OkHttpClient.Builder().build();
        }
    }

    public GlintHttpBuilder<T, BaseHttpModule> getBuilder() {
        return mBuilder;
    }

    public Type getTypeOfT() {
        return mTypeOfT;
    }

    protected GlintHttpBuilder createBuilder() {
        return new GlintHttpBuilder(GLINT);
    }

    protected void moduleUsing(BaseHttpModule module) {
        GlintHttpBuilder<T, BaseHttpModule> newBuilder = new GlintHttpBuilder<>(GLINT, false);
        try {
            newBuilder = mBuilder.clone();
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
        }
        module.configDefaultBuilder(newBuilder);
        newBuilder.addCustomGlintModule(module);
        mBuilder = newBuilder;
    }

    protected synchronized void coreCancel() {
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
            JsonElement jsonEl;
            // 如果不是标准的json数据，直接将整个数据返回
            if (mBuilder.notJson) {
                String responseStr = responseBody.string(); //这里是强转泛型的方法
                //noinspection unchecked,ConstantConditions
                t = (T) Primitives.wrap(String.class.getSuperclass()).cast(responseStr);
                result.setRunStatus(Glint.ResultStatus.STATUS_NORMAL);
                result.setData(t);
                deliverResponse(result);
                return;
            } else {
                JsonReader jsonReader = new JsonReader(new InputStreamReader(responseBody.byteStream(), Util.UTF_8));
                //开始对数据做解析处理
                JsonParser parser = new JsonParser();
                try {
                    jsonEl = parser.parse(jsonReader);
                } catch (JsonSyntaxException e) {
                    deliverError(e);
                    return;
                }
                String responseStr = jsonEl.toString();
                result.setResponseStr(responseStr);
                result.setHeaders(response.headers());
            }

            // 转换成Json对象
            JsonObject jsonObj = jsonEl.getAsJsonObject();
            if (!mBuilder.customGlintModule.isEmpty()) {
                for (BaseHttpModule baseHttpModule : mBuilder.customGlintModule) {
                    boolean transitive = baseHttpModule.customDeserialize(result, jsonObj, sGson, mTypeOfT);
                    if (!transitive) {
                        break;
                    }
                }
            } else if (GLINT != null && !mBuilder.standardDeserialize) {
                GLINT.customDeserialize(result, jsonObj, sGson, mTypeOfT);
            } else {
                result.setRunStatus(Glint.ResultStatus.STATUS_NORMAL);
                result.setData(GlintRequestUtil.<T>standardDeserialize(sGson, jsonObj, mTypeOfT));
            }
            deliverResponse(result);
        } catch (Exception e) {
            deliverError(e);
        } finally {
            if (response != null) {
                response.close();
            }
            GlintHttpDispatcher.getInstance().finished(this);
        }
    }

    protected GlintResultBean<T> runSync() throws Exception {
        prepare();
        // 将数据装载到ResultBean中
        GlintResultBean<T> result = new GlintResultBean<>();
        okhttp3.Response response = mOkHttpCall.execute();
        // 得到返回数据
        ResponseBody responseBody = response.body();
        if (responseBody == null) {
            return result;
        }
        JsonElement jsonEl;
        if (mBuilder.notJson) {
            String responseStr = responseBody.string();
            result.setResponseStr(responseStr);
            result.setHeaders(response.headers());
            //这里是强转泛型的方法
            //noinspection unchecked,ConstantConditions
            T t = (T) Primitives.wrap(String.class.getSuperclass()).cast(responseStr);
            result.setRunStatus(Glint.ResultStatus.STATUS_NORMAL);
            result.setData(t);
            return result;
        } else {
            JsonReader jsonReader = new JsonReader(new InputStreamReader(responseBody.byteStream(), Util.UTF_8));
            //开始对数据做解析处理
            JsonParser parser = new JsonParser();
            jsonEl = parser.parse(jsonReader);
            String responseStr = jsonEl.toString();
            result.setResponseStr(responseStr);
            result.setHeaders(response.headers());
        }
        // 转换成Json对象
        JsonObject jsonObj = jsonEl.getAsJsonObject();
        if (!mBuilder.customGlintModule.isEmpty()) {
            for (BaseHttpModule baseHttpModule : mBuilder.customGlintModule) {
                boolean transitive = baseHttpModule.customDeserialize(result, jsonObj, sGson, mTypeOfT);
                if (!transitive) {
                    break;
                }
            }
        } else if (GLINT != null && !mBuilder.standardDeserialize) {
            GLINT.customDeserialize(result, jsonObj, sGson, mTypeOfT);
        } else {
            result.setRunStatus(Glint.ResultStatus.STATUS_NORMAL);
            result.setData(GlintRequestUtil.<T>standardDeserialize(sGson, jsonObj, mTypeOfT));
        }
        return result;
    }

    private void prepare() throws Exception {
        if (mBuilder.listener != null) {
            UiKit.runOnMainThreadAsync(new Runnable() {
                @Override
                public void run() {
                    if (mBuilder.listener != null) {
                        mBuilder.listener.onStart();
                    }
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
                boolean transitive = baseHttpModule.getHeaders(mBuilder.header);
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
            GLINT.getHeaders(mBuilder.header);
            BaseHttpModule.UrlResult urlResult = GLINT.getUrl(mBuilder.url);
            newUrl = urlResult.url;
        } else {
            newUrl = mBuilder.url;
        }
        MediaType contentType = MediaType.parse(mBuilder.mimeType);
        String paramsEncoding;
        if (contentType == null) {
            paramsEncoding = Util.UTF_8.name();
        } else {
            Charset charset = contentType.charset(Util.UTF_8);
            if (charset == null) {
                paramsEncoding = Util.UTF_8.name();
            } else {
                paramsEncoding = charset.name();
            }
        }
        String params = GlintRequestUtil.encodeParameters(mBuilder.params, paramsEncoding);
        okhttp3.Request.Builder okHttpRequestBuilder;
        if (mBuilder.method == Method.GET) {
            if (!TextUtils.isEmpty(params)) {
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
                okHttpRequestBuilder = new okhttp3.Request.Builder()
                        .get()
                        .url(requestUrl);
            } else {
                okHttpRequestBuilder = new okhttp3.Request.Builder()
                        .get()
                        .url(newUrl);
            }
        } else if (mBuilder.jsonParams != null) {
            RequestBody requestBodyPost = RequestBody.create(contentType, ByteString.encodeUtf8(mBuilder.jsonParams.toString()));
            okHttpRequestBuilder = new okhttp3.Request.Builder()
                    .post(requestBodyPost)
                    .url(newUrl);
        } else {
            RequestBody requestBodyPost = RequestBody.create(contentType, params);
            okHttpRequestBuilder = new okhttp3.Request.Builder()
                    .post(requestBodyPost)
                    .url(newUrl);
        }
        // 添加头部到请求里
        for (String name : mBuilder.header.keySet()) {
            okHttpRequestBuilder.addHeader(name, mBuilder.header.get(name));
        }
        if (!TextUtils.isEmpty(mBuilder.cookie)) {
            okHttpRequestBuilder.addHeader("Cookie", mBuilder.cookie);
        }
        OkHttpClient httpClient;
        OkHttpClient.Builder clientBuilder = sClient.newBuilder();
        if (!mBuilder.retryOnConnectionFailure) {
            clientBuilder.retryOnConnectionFailure(false);
        }
        if (mBuilder.cacheTime > 0) {
            clientBuilder.addNetworkInterceptor(new GlintHttpCache(mBuilder.cacheTime));
        }
        // 缓存10M
        clientBuilder.cache(sCache);
        httpClient = clientBuilder.build();
        mOkHttpCall = httpClient.newCall(okHttpRequestBuilder.build());
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
        if (mBuilder.baseRetry != null) {
            boolean transitive = mBuilder.baseRetry.retryOnFail(error);
            if (!transitive) {
                return;
            }
        }
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
                if (response.getRunStatus() == Glint.ResultStatus.STATUS_SUCCESS || response.getRunStatus() == Glint.ResultStatus.STATUS_NORMAL) {
                    mBuilder.listener.onSuccess(response.getData());
                } else {
                    mBuilder.listener.onError(response.getStatus(), response.getMessage());
                    mBuilder.listener.onErrorOrFail();
                }
            } catch (Exception e) {
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

    /**
     * Supported request methods.
     */
    public interface Method {
        int GET = 0;
        int POST = 1;
    }

}