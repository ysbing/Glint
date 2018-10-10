package com.ysbing.glint.upload;

import com.ysbing.glint.http.GlintHttpListener;

/**
 * 上传监听回调
 *
 * @author ysbing
 *         创建于 2018/1/16
 */
public abstract class GlintUploadListener<T> extends GlintHttpListener<T> {
    /**
     * @param bytesWritten  已读取的长度
     * @param contentLength 总长度
     * @param speed         速度，单位是每秒/字节
     * @param percent       百分比，最小是0，最大是100
     * @throws Exception
     */
    public void onProgress(long bytesWritten, long contentLength, long speed, int percent) throws Exception {
    }
}
