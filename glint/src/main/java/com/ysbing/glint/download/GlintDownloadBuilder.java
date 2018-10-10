package com.ysbing.glint.download;

import android.support.annotation.NonNull;

import com.ysbing.glint.base.BaseHttpModule;
import com.ysbing.glint.base.Glint;
import com.ysbing.glint.base.GlintBaseBuilder;

import java.io.File;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 下载的参数配置
 *
 * @author ysbing
 */
public final class GlintDownloadBuilder<E extends BaseHttpModule> extends GlintBaseBuilder<E> implements Cloneable {

    /**
     * 上层的监听回调
     */
    @NonNull
    public CopyOnWriteArrayList<GlintDownloadListener> listeners = new CopyOnWriteArrayList<>();
    /**
     * 要保存的文件
     */
    public File saveFile;
    /**
     * 用于暂停和恢复，
     */
    public long range;
    /**
     * 用于暂停和恢复，
     */
    public long contentLength;
    /**
     * 下载完成校验MD5，默认是true
     */
    public boolean checkMd5 = true;

    public GlintDownloadBuilder(@NonNull Glint glint) {
        this(glint, true);
    }

    public GlintDownloadBuilder(@NonNull Glint glint, boolean init) {
        super();
        this.mimeType = "application/x-www-form-urlencoded; charset=utf-8";
        if (init) {
            glint.configDefaultBuilder(this);
        }
    }

    @Override
    protected GlintDownloadBuilder clone() throws CloneNotSupportedException {
        return (GlintDownloadBuilder) super.clone();
    }
}