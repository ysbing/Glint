package com.ysbing.glint.download;

import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.ysbing.glint.base.BaseHttpModule;
import com.ysbing.glint.util.GlintRequestUtil;

import java.io.File;
import java.util.TreeMap;

import okhttp3.Headers;

/**
 * 下载请求的入口
 * 该类不可继承，如果有自定义功能需求，就继承{@link GlintDownloadCore}进行扩展
 *
 * @author ysbing
 */
public final class GlintDownload extends GlintDownloadCore {

    public static GlintDownload download(@NonNull String url, @NonNull String savePath) {
        return download(url, new File(savePath));
    }

    public static GlintDownload download(@NonNull String url, @NonNull File saveFile) {
        return download(url, saveFile, new TreeMap<String, String>());
    }

    public static GlintDownload download(@NonNull String url, @NonNull String savePath, @NonNull TreeMap<String, String> params) {
        return download(url, new File(savePath), params);
    }

    public static GlintDownload download(@NonNull String url, @NonNull File saveFile, @NonNull TreeMap<String, String> params) {
        return new GlintDownload(url, saveFile, params);
    }

    public GlintDownload(@NonNull String url, @NonNull File saveFile, @NonNull TreeMap<String, String> params) {
        mBuilder = createBuilder();
        mBuilder.url = url;
        mBuilder.saveFile = saveFile;
        mBuilder.params = params;
    }

    /**
     * 额外设置的header,出去登录相关的
     */
    public GlintDownload setHeader(@NonNull Headers.Builder headers) {
        mBuilder.headers = headers;
        return this;
    }

    /**
     * 增加cookie到头部信息
     *
     * @param cookie 登录返回的cookie
     */
    public GlintDownload addCookie(@NonNull String cookie) {
        mBuilder.cookie = cookie;
        return this;
    }

    /**
     * 设置是否使用通用签名，默认是
     *
     * @param signature 使用通用签名
     */
    public GlintDownload signature(boolean signature) {
        mBuilder.signature = signature;
        return this;
    }

    /**
     * 设置文件的协议
     *
     * @param mimeType 文件协议
     */
    public GlintDownload setMimeType(@NonNull String mimeType) {
        mBuilder.mimeType = mimeType;
        return this;
    }

    /**
     * 使用自定义Module，可做高级操作
     * 该方法将会重置builder，使用时需在第一个使用
     *
     * @param module 自定义Module
     */
    public GlintDownload using(@NonNull BaseHttpModule module) {
        super.moduleUsing(module);
        return this;
    }

    /**
     * @param tag 请求标签，用于取消请求
     */
    public GlintDownload setTag(@NonNull String tag) {
        mBuilder.tag = tag.hashCode();
        return this;
    }

    public GlintDownload setTag(int tag) {
        mBuilder.tag = tag;
        return this;
    }

    /**
     * @param urgent 是否紧急，用于插队
     * @return
     */
    public GlintDownload urgent(boolean urgent) {
        mBuilder.urgent = urgent;
        return this;
    }

    /**
     * @param checkMd5 是否检查文件完整性
     * @return
     */
    public GlintDownload checkMd5(boolean checkMd5) {
        mBuilder.checkMd5 = checkMd5;
        return this;
    }

    /**
     * @param md5 传入要检查文件完整性的哈希码
     * @return
     */
    public GlintDownload md5(String md5) {
        mBuilder.md5 = md5;
        return this;
    }


    /**
     * 设置是否使用主线程
     *
     * @param mainThread 是否使用主线程
     */
    public GlintDownload mainThread(boolean mainThread) {
        mBuilder.mainThread = mainThread;
        return this;
    }

    /**
     * 暂停
     */
    public void pause() {
        GlintDownloadDispatcher.getInstance().pause(mBuilder.tag);
    }

    /**
     * 继续
     */
    public void resume() {
        GlintDownloadDispatcher.getInstance().resume(mBuilder.tag);
    }

    /**
     * 用于获取下载的下载状态
     *
     * @return 是否下载结束
     */
    public boolean isFinish() {
        return super.isFinish();
    }


    public synchronized void cancel() {
        GlintDownloadDispatcher.getInstance().cancel(mBuilder.tag);
    }

    /**
     * 执行网络请求
     */
    public void execute() {
        if (!TextUtils.isEmpty(mBuilder.url)) {
            GlintRequestUtil.addDownloadRequestTag(mBuilder);
            GlintDownloadDispatcher.getInstance().executed(this, mBuilder.urgent);
        }
    }

    /**
     * 执行网络请求
     *
     * @param listener 回调
     */
    @SuppressWarnings("unchecked")
    public void execute(@NonNull GlintDownloadListener listener) {
        if (!TextUtils.isEmpty(mBuilder.url)) {
            boolean newRequest = true;
            GlintRequestUtil.addDownloadRequestTag(mBuilder);
            for (GlintDownloadCore downloadCore : GlintDownloadDispatcher.getInstance().getDownloaderList()) {
                if (downloadCore.mBuilder.tag == mBuilder.tag) {
                    downloadCore.mBuilder.listeners.add(listener);
                    newRequest = false;
                    break;
                }
            }
            if (newRequest) {
                mBuilder.listeners.add(listener);
                GlintDownloadDispatcher.getInstance().executed(this);
            }
        }
    }
}
