package com.ysbing.glint.http;

import androidx.annotation.NonNull;

/**
 * 重试接口类
 *
 * @author ysbing
 */
public interface GlintHttpRetry {
    /**
     * 创建请求
     *
     * @param request 将该请求保存起来，在重试的时候使用
     */
    void onCreateRequest(@NonNull GlintHttp request);

    /**
     * 请求失败的时候回调，一般是网络超时等原因
     *
     * @param error 错误对象
     * @return 是否继续传递
     */
    boolean retryOnFail(@NonNull Throwable error);
}