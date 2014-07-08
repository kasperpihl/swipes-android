package com.swipesapp.android.util;

import android.os.Looper;

import com.swipesapp.android.BuildConfig;

/**
 * Utilitary methods to deal with threading.
 *
 * @author Felipe Bari
 */
public class ThreadUtils {

    /**
     * Checks if the method is running on the main thread. If not, throws an exception.
     */
    public static void checkOnMainThread() {
        if (BuildConfig.DEBUG) {
            if (Thread.currentThread() != Looper.getMainLooper().getThread()) {
                throw new IllegalStateException("This method should be called from the main thread");
            }
        }
    }

}
