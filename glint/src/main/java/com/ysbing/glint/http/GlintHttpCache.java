package com.ysbing.glint.http;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Response;

/**
 * 缓存拦截器
 */
final class GlintHttpCache implements Interceptor {
    private final int mCacheTime;

    GlintHttpCache(int cacheTime) {
        this.mCacheTime = cacheTime;
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        return chain.proceed(chain.request()).newBuilder()
                .removeHeader("Pragma")
                .header("Cache-Control", "public, max-age=" + mCacheTime)
                .build();
    }
}