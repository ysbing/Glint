package com.ysbing.glint.util;

import android.app.Activity;

import androidx.fragment.app.Fragment;

import java.lang.ref.SoftReference;
import java.util.Stack;

/**
 * @author ysbing
 * 创建于 2017/11/2
 */
public class UiStack {
    /**
     * Activity堆栈
     */
    private static final Stack<SoftReference<Activity>> activityStack = new Stack<>();
    private static final Stack<SoftReference<Fragment>> fragmentStack = new Stack<>();
    /**
     * 应用管理对象
     */
    private static final UiStack instance = new UiStack();

    private UiStack() {
    }

    /**
     * 单一实例
     *
     * @return 应用管理对象
     */
    public static UiStack getInstance() {
        return instance;
    }

    /**
     * 移除所有Activity
     */
    public void popAllActivity() {
        activityStack.clear();
    }

    /**
     * 移除所有Fragment
     */
    public void popAllFragment() {
        fragmentStack.clear();
    }

    /**
     * 移除指定Activity
     */
    public void popActivity(Activity activity) {
        int removeIndex = -1;
        for (int i = 0; i < activityStack.size(); i++) {
            SoftReference<Activity> activitySoftReference = activityStack.get(i);
            if (activitySoftReference != null && activitySoftReference.get() != null && activitySoftReference.get() == activity) {
                removeIndex = i;
                break;
            }
        }
        if (removeIndex > -1) {
            activityStack.remove(removeIndex);
        }
    }

    /**
     * 移除指定Fragment
     */
    public void popFragment(Fragment fragment) {
        int removeIndex = -1;
        for (int i = 0; i < fragmentStack.size(); i++) {
            SoftReference<Fragment> fragmentSoftReference = fragmentStack.get(i);
            if (fragmentSoftReference != null && fragmentSoftReference.get() != null && fragmentSoftReference.get() == fragment) {
                removeIndex = i;
                break;
            }
        }
        if (removeIndex > -1) {
            fragmentStack.remove(removeIndex);
        }
    }

    /**
     * 添加Activity
     */
    public void pushActivity(Activity activity) {
        SoftReference<Activity> activitySoftReference = new SoftReference<>(activity);
        activityStack.add(activitySoftReference);
    }

    /**
     * 添加Activity
     */
    public void pushFragment(Fragment fragment) {
        SoftReference<Fragment> fragmentSoftReference = new SoftReference<>(fragment);
        fragmentStack.add(fragmentSoftReference);
    }


    /**
     * 根据类名获取Activity对象
     *
     * @param className 类名
     * @return Activity对象
     */
    public Activity getActivity(String className) {
        Class<?> cls = null;
        try {
            cls = Class.forName(className);
        } catch (Exception ignored) {
        }
        if (cls != null) {
            for (int i = activityStack.size() - 1; i >= 0; i--) {
                if (activityStack.get(i).get() != null && activityStack.get(i).get().getClass().equals(cls)) {
                    return activityStack.get(i).get();
                }
            }
        } else {
            return null;
        }
        return null;
    }

    /**
     * 根据类名获取Fragment对象
     *
     * @param className 类名
     * @return Fragment对象
     */
    public Fragment getFragment(String className) {
        Class<?> cls = null;
        try {
            cls = Class.forName(className);
        } catch (Exception ignored) {
        }
        if (cls != null) {
            for (int i = 0; i < fragmentStack.size(); i++) {
                if (fragmentStack.get(i).get() != null && fragmentStack.get(i).get().getClass().equals(cls)) {
                    return fragmentStack.get(i).get();
                }
            }
        } else {
            return null;
        }
        return null;
    }
}
