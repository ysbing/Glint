
package com.ysbing.glint.socket;

import androidx.annotation.NonNull;

/**
 * Socket监听器
 *
 * @author ysbing
 */
public abstract class GlintSocketListener<T> {

    public void onProcess(@NonNull T result) throws Throwable {
    }

    public void onError(@NonNull String error) {
    }

}
