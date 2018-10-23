package com.ysbing.glint.util;


import android.support.annotation.NonNull;

import java.io.File;
import java.io.IOException;
import java.security.MessageDigest;

import okhttp3.internal.Util;
import okio.BufferedSource;
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
     * @return 该文件的MD%
     */
    public static String getMD5Str(@NonNull String str) {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("MD5");
            digest.update(str.getBytes(Util.UTF_8));
        } catch (Exception e) {
            return "";
        }
        return bytesToHexString(digest.digest());
    }

    /**
     * 获取单个文件的MD5值
     *
     * @param file 要计算的文件
     * @return 该文件的MD%
     */
    public static String getMD5Str(@NonNull File file) {
        if (!file.isFile()) {
            return "";
        }
        MessageDigest digest;
        BufferedSource source = null;
        byte buffer[] = new byte[1024];
        int len;
        try {
            digest = MessageDigest.getInstance("MD5");
            source = Okio.buffer(Okio.source(file));
            while ((len = source.read(buffer, 0, 1024)) != -1) {
                digest.update(buffer, 0, len);
            }
        } catch (Exception e) {
            return "";
        } finally {
            if (source != null) {
                try {
                    source.close();
                } catch (IOException ignored) {
                }
            }
        }
        return bytesToHexString(digest.digest());
    }

    public static String bytesToHexString(byte[] src) {
        StringBuilder stringBuilder = new StringBuilder();
        if (src == null || src.length <= 0) {
            return "";
        }
        for (byte b : src) {
            int v = b & 0xFF;
            String hv = Integer.toHexString(v);
            if (hv.length() < 2) {
                stringBuilder.append(0);
            }
            stringBuilder.append(hv);
        }
        return stringBuilder.toString();
    }

}