package com.ysbing.glint.download;

/**
 * 下载信息结构体
 *
 * @author ysbing
 */
public class GlintDownloadInfo {
    /**
     * 写入的字节数
     */
    public long bytesWritten;
    /**
     * 文件总长度
     */
    public long contentLength;
    /**
     * 进度条，最小是0，最大是100
     */
    public int progress;
}