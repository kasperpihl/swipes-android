package com.swipesapp.android.sync.service;

import android.content.Context;

import com.google.gson.JsonObject;
import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.Ion;
import com.swipesapp.android.app.SwipesApplication;
import com.swipesapp.android.db.DaoSession;
import com.swipesapp.android.db.dao.ExtDeletedDao;
import com.swipesapp.android.db.dao.ExtTagSyncDao;
import com.swipesapp.android.db.dao.ExtTaskSyncDao;
import com.swipesapp.android.sync.listener.SyncListener;

import java.lang.ref.WeakReference;

/**
 * Service for syncing operations.
 *
 * @author Felipe Bari
 */
public class SyncService {

    private static SyncService sInstance;

    private ExtTaskSyncDao mExtTaskSyncDao;
    private ExtTagSyncDao mExtTagSyncDao;
    private ExtDeletedDao mExtDeletedDao;

    private WeakReference<Context> mContext;
    private SyncListener mListener;
    private TasksService mTasksService;

    /**
     * Internal constructor. Handles loading of extended DAOs for custom DB operations.
     *
     * @param context  Context instance.
     * @param listener Sync listener instance.
     */
    public SyncService(Context context, SyncListener listener) {
        mContext = new WeakReference<Context>(context);
        mListener = listener;
        mTasksService = TasksService.getInstance(context);

        DaoSession daoSession = SwipesApplication.getDaoSession();

        mExtTaskSyncDao = ExtTaskSyncDao.getInstance(daoSession);
        mExtTagSyncDao = ExtTagSyncDao.getInstance(daoSession);
        mExtDeletedDao = ExtDeletedDao.getInstance(daoSession);
    }

    /**
     * Returns an existing instance of the service, or loads a new one if needed.
     * This ensures only one DAO session is active at any given time.
     *
     * @param context Context reference.
     * @return Service instance.
     */
    public static SyncService getInstance(Context context, SyncListener listener) {
        if (sInstance == null) {
            sInstance = new SyncService(context, listener);
        } else {
            sInstance.updateReferences(context, listener);
        }
        return sInstance;
    }

    /**
     * Updates the context reference and listener.
     *
     * @param context  Context reference.
     * @param listener Sync listener instance.
     */
    private void updateReferences(Context context, SyncListener listener) {
        mContext = new WeakReference<Context>(context);
        mListener = listener;
    }

    /**
     * Performs a sync operation.
     */
    public void performSync() {
        // TODO: Implement sync.
    }

    /**
     * Sends local changes to the API.
     */
    private void sendChanges() {
        JsonObject json = new JsonObject();
        json.addProperty("foo", "bar");

        // TODO: Track changes and send to the API.
        Ion.with(mContext.get())
                .load("http://example.com/post")
                .setJsonObjectBody(json)
                .asJsonObject()
                .setCallback(new FutureCallback<JsonObject>() {
                    @Override
                    public void onCompleted(Exception e, JsonObject result) {
                        // Do stuff with the result or error.
                    }
                });
    }

    /**
     * Retrieves remote changes from the API.
     */
    private void retrieveChanges() {
        // TODO: Get API response and save changes.
        Ion.with(mContext.get())
                .load("http://example.com/thing.json")
                .asJsonObject()
                .setCallback(new FutureCallback<JsonObject>() {
                    @Override
                    public void onCompleted(Exception e, JsonObject result) {
                        // Do stuff with the result or error.
                    }
                });
    }

}
