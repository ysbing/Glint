package com.ysbing.glint.util;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Context;

import java.lang.ref.WeakReference;
import java.lang.reflect.Method;

/**
 * 获取Context对象的助手
 *
 * @author ysbing
 */
public class ContextHelper {

    private static WeakReference<Application> mApplicationWeak;

    public static Context getAppContext() {
        return getApplication().getApplicationContext();
    }

    public static Application getApplication() {
        if (mApplicationWeak == null || mApplicationWeak.get() == null) {
            synchronized (ContextHelper.class) {
                if (mApplicationWeak == null || mApplicationWeak.get() == null) {
                    try {
                        @SuppressLint("PrivateApi") Class<?> clazzActivityThread = Class.forName("android.app.ActivityThread");
                        Method currentActivityThread = clazzActivityThread.getDeclaredMethod("currentActivityThread");
                        Method getApplicationMethod = clazzActivityThread.getMethod("getApplication");
                        Object activityThread = currentActivityThread.invoke(null);
                        Application application = (Application) getApplicationMethod.invoke(activityThread);
                        mApplicationWeak = new WeakReference<>(application);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        return mApplicationWeak.get();
    }
}
