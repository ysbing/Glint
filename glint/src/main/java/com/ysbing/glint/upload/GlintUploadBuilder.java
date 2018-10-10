package com.ysbing.glint.upload;

import android.support.annotation.NonNull;

import com.ysbing.glint.base.BaseHttpModule;
import com.ysbing.glint.base.Glint;
import com.ysbing.glint.base.GlintBaseBuilder;

import java.io.File;

/**
 * 上传的参数配置
 *
 * @author ysbing
 * 创建于 2018/1/16
 */
public final class GlintUploadBuilder<T, E extends BaseHttpModule> extends GlintBaseBuilder<E> implements Cloneable {

    /**
     * 上层的监听回调
     */
    public GlintUploadListener<T> listener;
    /**
     * 要上传的文件，与{@link #data}同时不为空时优先使用data
     */
    public File file;
    /**
     * 要上传的数据，与{@link #file}同时不为空时优先使用data
     */
    public byte[] data;
    /**
     * 要上传的文件名称，默认使用file.getName，
     * 如使用byte[]而不指定keyName，将抛出异常
     */
    public String keyName;
    /**
     * 结果不是json
     */
    public boolean notJson = false;

    public GlintUploadBuilder(@NonNull Glint glint) {
        this(glint, true);
    }

    public GlintUploadBuilder(@NonNull Glint glint, boolean init) {
        super();
        this.mimeType = "multipart/form-data;charset=utf-8";
        if (init) {
            glint.configDefaultBuilder(this);
        }
    }

    @Override
    protected GlintUploadBuilder clone() throws CloneNotSupportedException {
        return (GlintUploadBuilder) super.clone();
    }
}