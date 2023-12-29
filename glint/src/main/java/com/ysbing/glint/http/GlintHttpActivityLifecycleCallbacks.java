package com.ysbing.glint.http;


import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.Application;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.fragment.app.FragmentActivity;

import com.ysbing.glint.util.UiKit;
import com.ysbing.glint.util.UiStack;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

/**
 * 将Activity的名称收集起来，然后当该Activity销毁的时候，及时做销毁
 *
 * @author ysbing
 */
@RequiresApi(api = Build.VERSION_CODES.ICE_CREAM_SANDWICH)
public final class GlintHttpActivityLifecycleCallbacks implements Application.ActivityLifecycleCallbacks {
    private final GilntHttpFragmentLifecycleCallbacks mFragmentLifecycleCallbacks = new GilntHttpFragmentLifecycleCallbacks();
    private ActivityManager mActivityManager;
    private Object mActivityThread;
    private Field mActivitiesField;
    private int mActivityCount;

    public GlintHttpActivityLifecycleCallbacks(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            mActivityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        } else {
            try {
                @SuppressLint("PrivateApi") Class<?> activityThreadClass = Class.forName("android.app.ActivityThread");
                mActivityThread = activityThreadClass.getMethod("currentActivityThread").invoke(null);
                mActivitiesField = activityThreadClass.getDeclaredField("mActivities");
                mActivitiesField.setAccessible(true);
            } catch (Exception ignored) {
            }
        }
    }

    @Override
    public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {
        mActivityCount++;
        UiStack.getInstance().pushActivity(activity);
        GlintHttpDispatcher.getInstance().mHostActivityNameList.add(activity.getClass().getName());
        GlintHttpDispatcher.getInstance().mHashCodeList.add(activity.hashCode());
        if (activity instanceof FragmentActivity) {
            ((FragmentActivity) activity).getSupportFragmentManager().registerFragmentLifecycleCallbacks(mFragmentLifecycleCallbacks, true);
        }
    }

    @Override
    public void onActivityStarted(Activity activity) {

    }

    @Override
    public void onActivityResumed(Activity activity) {

    }

    @Override
    public void onActivityPaused(Activity activity) {

    }

    @Override
    public void onActivityStopped(Activity activity) {

    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle outState) {

    }

    @Override
    public void onActivityDestroyed(Activity activity) {
        UiStack.getInstance().popActivity(activity);
        if (activity instanceof FragmentActivity) {
            ((FragmentActivity) activity).getSupportFragmentManager().unregisterFragmentLifecycleCallbacks(mFragmentLifecycleCallbacks);
        }
        GlintHttpDispatcher.getInstance().mHostActivityNameList.remove(activity.getClass().getName());
        GlintHttpDispatcher.getInstance().mHashCodeList.remove(Integer.valueOf(activity.hashCode()));
        GlintHttpDispatcher.getInstance().cancelAtHashCode(activity.hashCode());

        List<ActivityManager.RunningTaskInfo> tasks;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP && (tasks = mActivityManager.getRunningTasks(1)) != null) {
            mActivityCount = tasks.size();
            if (tasks.size() == 1 && tasks.get(0).baseActivity.toString().contains("com.google.android")) {
                mActivityCount--;
            }
        } else {
            try {
                if (mActivitiesField != null && mActivityThread != null) {
                    Map activities = (Map) mActivitiesField.get(mActivityThread);
                    mActivityCount = activities.size();
                }
            } catch (Exception ignored) {
            } finally {
                mActivityCount--;
            }
        }
        if (mActivityCount == 0) {
            GlintHttpDispatcher.getInstance().cancelAll();
            UiStack.getInstance().popAllActivity();
            UiStack.getInstance().popAllFragment();
            UiKit.dispose();
        }
    }
}