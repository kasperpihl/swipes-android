package com.swipesapp.android.sync.listener;

/**
 * Listener to handle sync events.
 *
 * @author Fernanda Bari
 */
public interface SyncListener {

    /**
     * Sync has been completed.
     */
    void onSyncDone();

    /**
     * Sync has failed.
     */
    void onSyncFailed();

}
