package com.ysbing.glint.http;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;

import com.ysbing.glint.util.UiStack;

/**
 * 将Fragment的名称收集起来，然后当该Fragment销毁的时候，及时做销毁
 *
 * @author ysbing
 */
class GilntHttpFragmentLifecycleCallbacks extends FragmentManager.FragmentLifecycleCallbacks {
    @Override
    public void onFragmentCreated(FragmentManager fm, Fragment f, Bundle savedInstanceState) {
        super.onFragmentCreated(fm, f, savedInstanceState);
        UiStack.getInstance().pushFragment(f);
        GlintHttpDispatcher.getInstance().mHostFragmentNameList.add(f.getClass().getName());
        GlintHttpDispatcher.getInstance().mHashCodeList.add(f.hashCode());
    }

    @Override
    public void onFragmentDestroyed(FragmentManager fm, Fragment f) {
        super.onFragmentDestroyed(fm, f);
        UiStack.getInstance().popFragment(f);
        GlintHttpDispatcher.getInstance().mHostFragmentNameList.remove(f.getClass().getName());
        GlintHttpDispatcher.getInstance().mHashCodeList.remove(Integer.valueOf(f.hashCode()));
        GlintHttpDispatcher.getInstance().cancelAtHashCode(f.hashCode());
    }
}