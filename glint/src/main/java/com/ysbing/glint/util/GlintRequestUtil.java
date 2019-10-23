package com.ysbing.glint.util;

import android.app.Activity;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.text.TextUtils;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.internal.Primitives;
import com.ysbing.glint.download.GlintDownloadBuilder;
import com.ysbing.glint.http.GlintHttpBuilder;
import com.ysbing.glint.http.GlintHttpDispatcher;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import okhttp3.Response;

/**
 * 网络请求的数据转换工具类
 * 里面方法中的参数Type{@link Type}的获取方法:
 * Type mTypeOfT = new TypeToken<GlintResultBean<UserBean>>() {
 * }.getType();
 *
 * @author ysbing
 */
public class GlintRequestUtil {

    public static final Charset UTF_8 = Charset.forName("UTF-8");

    /**
     * 成功响应体反序列化
     *
     * @param context 序列号工具
     * @param jsonStr json字符串
     * @param type    转换的类型
     * @param <T>     转换的对象类型
     * @return 转换后的对象
     */
    public static <T> T successDeserialize(@NonNull Gson context, @NonNull String jsonStr,
                                           @NonNull Type type) {
        JsonParser parser = new JsonParser();
        JsonElement jsonEl = parser.parse(jsonStr);
        return successDeserialize(context, jsonEl, type);
    }

    /**
     * 成功响应体反序列化
     *
     * @param context     序列号工具
     * @param jsonElement jsonElement
     * @param type        转换的类型
     * @param <T>         转换的对象类型
     * @return 转换后的对象
     */
    @SuppressWarnings({"unchecked", "ConstantConditions"})
    public static <T> T successDeserialize(@NonNull Gson context,
                                           @NonNull JsonElement jsonElement,
                                           @NonNull Type type) {
        T t;
        // 需要根据不同的基本类型做不同的数据处理
        if (type.equals(String.class)) {
            if (jsonElement.isJsonArray()) {
                JsonArray tempObj = (JsonArray) jsonElement;
                if (tempObj.size() == 1 && tempObj.get(0).isJsonPrimitive()) {
                    //数量为1的时候，用getAs，否则用toString
                    t = (T) Primitives.wrap(String.class.getSuperclass())
                            .cast(jsonElement.getAsString());
                } else {
                    t = (T) Primitives.wrap(String.class.getSuperclass())
                            .cast(jsonElement.toString());
                }
            } else if (jsonElement.isJsonPrimitive()) {
                t = (T) Primitives.wrap(String.class.getSuperclass())
                        .cast(jsonElement.getAsString());
            } else {
                t = (T) Primitives.wrap(String.class.getSuperclass()).cast(jsonElement.toString());
            }
        } else if (type.equals(Boolean.class)) {
            t = (T) Primitives.wrap(Boolean.class.getSuperclass()).cast(jsonElement.getAsBoolean());
        } else if (type.equals(Integer.class)) {
            t = (T) Primitives.wrap(Integer.class.getSuperclass()).cast(jsonElement.getAsInt());
        } else if (type.equals(Long.class)) {
            t = (T) Primitives.wrap(Long.class.getSuperclass()).cast(jsonElement.getAsLong());
        } else if (type.equals(Float.class)) {
            t = (T) Primitives.wrap(Float.class.getSuperclass()).cast(jsonElement.getAsFloat());
        } else if (type.equals(Double.class)) {
            t = (T) Primitives.wrap(Double.class.getSuperclass()).cast(jsonElement.getAsDouble());
        } else if (type.equals(JsonObject.class)) {
            t = (T) Primitives.wrap(JsonObject.class.getSuperclass())
                    .cast(jsonElement.getAsJsonObject());
        } else if (type.equals(JsonArray.class)) {
            t = (T) Primitives.wrap(JsonArray.class.getSuperclass())
                    .cast(jsonElement.getAsJsonArray());
        } else {
            t = context.fromJson(jsonElement, type);
        }
        return t;
    }

    /**
     * 错误时反序列化
     *
     * @param jsonElement jsonElement
     * @return 拿到关键节点，转换成String类型并返回
     */
    public static String errorDeserialize(@NonNull JsonElement jsonElement) {
        // 非200的处理方式一样，只是非泛型，不需要做太多转换处理
        String msg;
        if (jsonElement.isJsonArray()) {
            JsonArray tempObj = (JsonArray) jsonElement;
            if (tempObj.size() == 1 && tempObj.get(0).isJsonPrimitive()) {
                msg = jsonElement.getAsString();
            } else {
                msg = jsonElement.toString();
            }
        } else if (jsonElement.isJsonPrimitive()) {
            msg = jsonElement.getAsString();
        } else {
            msg = jsonElement.toString();
        }
        return msg;
    }

    /**
     * Converts <code>params</code> into an application/x-www-form-urlencoded encoded string.
     */
    public static String encodeParameters(@NonNull Map<String, String> params,
                                          @NonNull String paramsEncoding) {
        StringBuilder encodedParams = new StringBuilder();
        try {
            for (Map.Entry<String, String> entry : params.entrySet()) {
                if (entry.getKey() == null || entry.getValue() == null) {
                    continue;
                }
                encodedParams.append(URLEncoder.encode(entry.getKey(), paramsEncoding));
                encodedParams.append('=');
                encodedParams.append(URLEncoder.encode(entry.getValue(), paramsEncoding));
                encodedParams.append('&');
            }
            return encodedParams.toString();
        } catch (UnsupportedEncodingException uee) {
            throw new RuntimeException("Encoding not supported: " + paramsEncoding, uee);
        }
    }

    /**
     * 添加接口请求的标签
     *
     * @param builder 构造参数
     */
    public static void addHttpRequestTag(@NonNull GlintHttpBuilder builder) {
        List<String> hostActivityNameList = GlintHttpDispatcher.getInstance().mHostActivityNameList;
        List<String> hostFragmentNameList = GlintHttpDispatcher.getInstance().mHostFragmentNameList;
        if (builder.listener != null && !builder.freeLife) {
            StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();
            //前面4个是没用的
            if (stackTraceElements.length > 15) {
                stackTraceElements = Arrays.copyOfRange(stackTraceElements, 4, 15);
            } else {
                stackTraceElements =
                        Arrays.copyOfRange(stackTraceElements, 4, stackTraceElements.length);
            }
            for (StackTraceElement stackTraceElement : stackTraceElements) {
                if (hostActivityNameList.contains(stackTraceElement.getClassName())) {
                    Activity activity =
                            UiStack.getInstance().getActivity(stackTraceElement.getClassName());
                    if (activity != null) {
                        builder.hostHashCode = activity.hashCode();
                    }
                    if (builder.tag == 0) {
                        builder.tag = System.identityHashCode(builder);
                    }
                    break;
                } else if (hostFragmentNameList.contains(stackTraceElement.getClassName())) {
                    Fragment fragment =
                            UiStack.getInstance().getFragment(stackTraceElement.getClassName());
                    if (fragment != null) {
                        builder.hostHashCode = fragment.hashCode();
                    }
                    if (builder.tag == 0) {
                        builder.tag = System.identityHashCode(builder);
                    }
                }
            }
        }
    }

    /**
     * 添加下载请求的标签
     *
     * @param builder 构造参数
     */
    public static void addDownloadRequestTag(@NonNull GlintDownloadBuilder builder) {
        if (builder.tag == 0) {
            builder.tag = builder.url.hashCode();
        }
    }

    /**
     * 解析文件头
     * Content-Disposition:attachment;filename=FileName.txt
     * Content-Disposition: attachment; filename*="UTF-8''%E6%9B%BF%E6%8D%A2%E5%AE%9E%E9%AA%8C%E6%8A%A5%E5%91%8A.pdf"
     */
    public static String getHeaderFileName(@NonNull Response response) {
        String fileName = "";
        String dispositionHeader = response.header("Content-Disposition");
        if (dispositionHeader != null && !TextUtils.isEmpty(dispositionHeader)) {
            dispositionHeader = dispositionHeader.replace("attachment;filename=", "")
                    .replace("filename*=utf-8", "");
            String[] strings = dispositionHeader.split("; ");
            if (strings.length > 1) {
                dispositionHeader = strings[1].replace("filename=", "");
                dispositionHeader = dispositionHeader.replace("\"", "");
                fileName = dispositionHeader;
            }
        }
        if (TextUtils.isEmpty(fileName)) {
            Uri uri = Uri.parse(response.request().url().toString());
            fileName = uri.getLastPathSegment();
        }
        return fileName;
    }

    public static Type getListenerType(Class<?> clazz) {
        Class<?> tClass = clazz;
        Type superClass;
        do {
            superClass = tClass.getGenericSuperclass();
            tClass = tClass.getSuperclass();
        }
        while (tClass != null && superClass != null &&
                !ParameterizedType.class.isAssignableFrom(superClass.getClass()));
        if (superClass instanceof ParameterizedType) {
            return ((ParameterizedType) superClass).getActualTypeArguments()[0];
        } else {
            return clazz.getGenericSuperclass();
        }
    }
}