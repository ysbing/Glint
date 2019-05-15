package com.ysbing.glint.util;


import android.support.annotation.NonNull;

import java.io.File;
import java.io.IOException;

import okio.BufferedSource;
import okio.ByteString;
import okio.Okio;

/**
 * MD5获取工具
 *
 * @author ysbing
 */
public class Md5Util {

    /**
     * 获取字符串的MD5
     *
     * @param str 要计算的字符串
     * @return 该文件的MD5
     */
    public static String getMD5Str(@NonNull String str) {
        return ByteString.encodeUtf8(str).md5().hex();
    }

    /**
     * 获取字节数组的MD5
     *
     * @param bytes 要计算的字节数组
     * @return 该文件的MD5
     */
    public static String getMD5Str(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return "";
        }
        return ByteString.of(bytes).md5().hex();
    }

    /**
     * 获取单个文件的MD5值
     *
     * @param file 要计算的文件
     * @return 该文件的MD5
     */
    public static String getMD5Str(@NonNull File file) {
        if (!file.isFile()) {
            return "";
        }
        BufferedSource source = null;
        try {
            source = Okio.buffer(Okio.source(file));
            return source.readByteString().md5().hex();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (source != null) {
                    source.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return "";
    }
}