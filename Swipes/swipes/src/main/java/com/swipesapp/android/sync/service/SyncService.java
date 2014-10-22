package com.swipesapp.android.sync.service;

import android.content.Context;

import com.google.gson.JsonObject;
import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.Ion;
import com.swipesapp.android.sync.listener.SyncListener;

import java.lang.ref.WeakReference;

/**
 * Service for syncing operations.
 *
 * @author Felipe Bari
 */
public class SyncService {

    private WeakReference<Context> mContext;
    private SyncListener mListener;
    private TasksService mTasksService;

    /**
     * Custom constructor.
     *
     * @param context  Context instance.
     * @param listener Sync listener instance.
     */
    public SyncService(Context context, SyncListener listener) {
        mContext = new WeakReference<Context>(context);
        mListener = listener;
        mTasksService = TasksService.getInstance(context);
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
