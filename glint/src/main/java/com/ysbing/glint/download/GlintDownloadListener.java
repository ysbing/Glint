package com.ysbing.glint.download;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.ysbing.glint.base.BaseHttpModule;
import com.ysbing.glint.base.GlintResultBean;
import com.ysbing.glint.http.GlintHttpListener;

import java.io.File;

import okhttp3.Response;

/**
 * 下载的回调监听
 *
 * @author ysbing
 * 创建于 2018/1/24
 */
public abstract class GlintDownloadListener extends GlintHttpListener<File> {

    /**
     * 该方法不回调，禁止重载
     *
     * @param resultBean 封装结果类
     */
    @Override
    final public void onResponse(@NonNull GlintResultBean<File> resultBean) throws Throwable {
        super.onResponse(resultBean);
    }

    /**
     * 禁止回调
     *
     * @param error 错误
     */
    @Override
    public final void onFail(@NonNull Throwable error) {
        super.onFail(error);
    }

    @Override
    public final void onError(int status, @NonNull String errMsg) throws Throwable {
        super.onError(status, errMsg);
    }

    @Override
    public final void onErrorOrFail() {
        super.onErrorOrFail();
    }

    /**
     * 该方法在非UI线程
     *
     * @param downloadBuilder 配置参数
     * @return 是否终止，默认否
     */
    public boolean onPrepared(@NonNull GlintDownloadBuilder<BaseHttpModule> downloadBuilder) {
        return false;
    }

    /**
     * @param bytesWritten  已读取的长度
     * @param contentLength 总长度
     * @param speed         速度，单位是每秒/字节
     * @param percent       百分比，最小是0，最大是100
     * @throws Exception
     */
    public void onProgress(long bytesWritten, long contentLength, long speed, int percent) throws Exception {
    }

    /**
     * 下载失败的回调
     *
     * @param error    异常对象
     * @param response 网络响应对象，可能为空，使用时要判空
     */
    public void onDownloadFail(@NonNull Throwable error, @Nullable Response response) {
    }

    /**
     * 暂停
     */
    public void onPause() {
    }

    /**
     * 暂停后恢复
     */
    public void onResume() {
    }

}
