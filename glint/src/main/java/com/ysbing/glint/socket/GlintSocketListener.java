
package com.ysbing.glint.socket;

import android.support.annotation.NonNull;

/**
 * Socket监听器
 *
 * @author ysbing
 */
public abstract class GlintSocketListener<T> {

    public void onProcess(@NonNull T result) throws Exception {
    }

    public void onError(@NonNull String error) {
    }

}
