package com.swipesapp.android.sync.listener;

/**
 * Listener to handle sync states.
 *
 * @author Felipe Bari
 */
public interface SyncListener {

    /**
     * Called when a sync operation is completed.
     */
    void onComplete();

    /**
     * Called when a sync operation fails.
     */
    void onFailed();

}
