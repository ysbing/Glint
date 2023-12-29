package com.ysbing.glint.base;

import android.app.Application;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.ysbing.glint.http.GlintHttpActivityLifecycleCallbacks;
import com.ysbing.glint.util.ContextHelper;

import java.lang.reflect.Type;
import java.util.Set;
import java.util.TreeMap;

import okhttp3.Headers;
import okhttp3.OkHttpClient;

/**
 * 获取Application做初始化工作的地方
 *
 * @author ysbing
 */
public final class Glint extends BaseHttpModule {
    private static volatile Glint sInstance;
    private BaseHttpModule mClazzBaseHttpModule;

    public static Glint getsInstance() {
        if (sInstance == null) {
            synchronized (Glint.class) {
                if (sInstance == null) {
                    Application application = ContextHelper.getApplication();
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
                        application.registerActivityLifecycleCallbacks(
                                new GlintHttpActivityLifecycleCallbacks(
                                        application.getApplicationContext()));
                    }
                    sInstance = new Glint(application.getApplicationContext());
                }
            }
        }
        return sInstance;
    }

    private Glint(Context context) {
        if (context == null) {
            throw new RuntimeException("Context is null");
        }
        if (!(context instanceof Application)) {
            throw new RuntimeException("Context is not Application");
        }
        initGlintHttpModule(context);
    }

    private void initGlintHttpModule(@NonNull Context context) {
        try {
            ApplicationInfo appInfo = context.getPackageManager()
                    .getApplicationInfo(context.getPackageName(),
                            PackageManager.GET_META_DATA);
            Set<String> keySet = appInfo.metaData.keySet();
            for (String key : keySet) {
                Object value = appInfo.metaData.get(key);
                if (!(value instanceof String)) {
                    continue;
                }
                String valueStr = (String) value;
                if (TextUtils.equals(valueStr, "GlintHttpModule")) {
                    Class<?> clazz = Class.forName(key);
                    mClazzBaseHttpModule = (BaseHttpModule) clazz.newInstance();
                    break;
                }
            }
        } catch (Exception ignored) {
        }
    }

    @Override
    public OkHttpClient.Builder onOkHttpBuildCreate(@NonNull GlintType clientType,
                                                    @NonNull OkHttpClient.Builder builder) {
        if (mClazzBaseHttpModule != null) {
            return mClazzBaseHttpModule.onOkHttpBuildCreate(clientType, builder);
        } else {
            return builder;
        }
    }

    @Override
    public <E extends BaseHttpModule> void configDefaultBuilder(
            @NonNull GlintBaseBuilder<E> builder) {
        super.configDefaultBuilder(builder);
        if (mClazzBaseHttpModule != null) {
            mClazzBaseHttpModule.configDefaultBuilder(builder);
        }
    }

    @Override
    public <E extends BaseHttpModule> void onBuilderCreated(@NonNull GlintBaseBuilder<E> builder)
            throws Exception {
        super.onBuilderCreated(builder);
        if (mClazzBaseHttpModule != null) {
            mClazzBaseHttpModule.onBuilderCreated(builder);
        }
    }

    @Override
    public UrlResult getUrl(@NonNull String originalUrl) throws Exception {
        if (mClazzBaseHttpModule != null) {
            return mClazzBaseHttpModule.getUrl(originalUrl);
        } else {
            return new UrlResult(originalUrl, true);
        }
    }

    @Override
    public boolean getParams(@NonNull TreeMap<String, String> originalParams) throws Exception {
        return mClazzBaseHttpModule == null || mClazzBaseHttpModule.getParams(originalParams);
    }

    @Override
    public boolean getHeaders(@NonNull Headers.Builder originalHeader) throws Exception {
        return mClazzBaseHttpModule == null || mClazzBaseHttpModule.getHeaders(originalHeader);
    }

    @Override
    public <T> boolean customDeserialize(@NonNull GlintResultBean<T> result,
                                         @NonNull JsonElement jsonEl, @NonNull Gson gson,
                                         @NonNull Type typeOfT) throws Exception {
        if (mClazzBaseHttpModule != null) {
            return mClazzBaseHttpModule.customDeserialize(result, jsonEl, gson, typeOfT);
        }
        return super.customDeserialize(result, jsonEl, gson, typeOfT);
    }

    /**
     * 使用框架的类型，目前有三种
     */
    public enum GlintType {
        /**
         * 普通接口请求
         */
        HTTP,
        /**
         * 文件上传
         */
        UPLOAD,
        /**
         * 文件下载
         */
        DOWNLOAD,
        /**
         * 长连接
         */
        SOCKET,
        /**
         * 柚子IO长连接
         */
        SOCKET_IO
    }

    public enum ResultStatus {
        /**
         * 解析成功的状态
         */
        STATUS_SUCCESS,
        /**
         * 解析错误或者网络错误的状态
         */
        STATUS_ERROR
    }
}