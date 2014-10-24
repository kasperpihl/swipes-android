package com.swipesapp.android.sync.service;

import android.content.Context;

import com.google.gson.JsonObject;
import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.Ion;
import com.swipesapp.android.app.SwipesApplication;
import com.swipesapp.android.db.DaoSession;
import com.swipesapp.android.db.Deleted;
import com.swipesapp.android.db.Tag;
import com.swipesapp.android.db.TagSync;
import com.swipesapp.android.db.TaskSync;
import com.swipesapp.android.db.dao.ExtDeletedDao;
import com.swipesapp.android.db.dao.ExtTagSyncDao;
import com.swipesapp.android.db.dao.ExtTaskSyncDao;
import com.swipesapp.android.sync.gson.GsonTag;
import com.swipesapp.android.sync.gson.GsonTask;
import com.swipesapp.android.sync.listener.SyncListener;

import java.lang.ref.WeakReference;
import java.util.List;

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

    private static final String API_URL = "http://api.swipesapp.com/sync";

    /**
     * Internal constructor. Handles loading of extended DAOs for custom DB operations.
     *
     * @param context Context instance.
     */
    public SyncService(Context context) {
        mContext = new WeakReference<Context>(context);

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
     */
    public static SyncService getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new SyncService(context);
        } else {
            sInstance.updateContext(context);
        }
        return sInstance;
    }

    /**
     * Updates the context reference.
     *
     * @param context Context reference.
     */
    private void updateContext(Context context) {
        mContext = new WeakReference<Context>(context);
    }

    /**
     * Performs a sync operation.
     *
     * @param listener Sync listener instance.
     */
    public void performSync(SyncListener listener) {
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
                        // TODO: Get API response and save changes.
                    }
                });
    }

    public void saveTaskChangesForSync(GsonTask task) {
        TaskSync taskSync = new TaskSync();

        if (task.getId() == null) {
            // Save entire task for sync.
            taskSync = taskSyncFromGson(task);
            mExtTaskSyncDao.getDao().insert(taskSync);
        } else if (task.isDeleted()) {
            // Save task to deleted objects.
            Deleted deleted = new Deleted(null, "ToDo", task.getTempId(), true);
            mExtDeletedDao.getDao().insert(deleted);
        } else {
            GsonTask old = TasksService.getInstance(mContext.get()).loadTask(task.getId());

            // Save only changed attributes.
            taskSync.setObjectId(task.getObjectId());
            taskSync.setUpdatedAt(task.getUpdatedAt());
            taskSync.setTitle(task.getTitle().equals(old.getTitle()) ? null : task.getTitle());
            taskSync.setNotes(task.getNotes().equals(old.getNotes()) ? null : task.getNotes());
            taskSync.setOrder(task.getOrder().equals(old.getOrder()) ? null : task.getOrder());
            taskSync.setPriority(task.getPriority().equals(old.getPriority()) ? null : task.getPriority());
            taskSync.setCompletionDate(task.getCompletionDate().equals(old.getCompletionDate()) ? null : task.getCompletionDate());
            taskSync.setSchedule(task.getSchedule().equals(old.getSchedule()) ? null : task.getSchedule());
            taskSync.setLocation(task.getLocation().equals(old.getLocation()) ? null : task.getLocation());
            taskSync.setRepeatDate(task.getRepeatDate().equals(old.getRepeatDate()) ? null : task.getRepeatDate());
            taskSync.setRepeatOption(task.getRepeatOption().equals(old.getRepeatOption()) ? null : task.getRepeatOption());
            taskSync.setTags(task.getTags().equals(old.getTags()) ? null : tagsToString(task.getTags()));

            mExtTaskSyncDao.getDao().insert(taskSync);
        }
    }

    public void saveTagForSync(Tag tag) {
        TagSync tagSync = new TagSync(tag.getId(), tag.getObjectId(), tag.getTempId(), tag.getCreatedAt(), tag.getUpdatedAt(), tag.getTitle());
        mExtTagSyncDao.getDao().insert(tagSync);
    }

    public void saveDeletedTagForSync(Tag tag) {
        Deleted deleted = new Deleted(null, "Tag", tag.getTempId(), true);
        mExtDeletedDao.getDao().insert(deleted);
    }

    private TaskSync taskSyncFromGson(GsonTask task) {
        return new TaskSync(task.getId(), task.getObjectId(), task.getTempId(), task.getParentLocalId(), task.getCreatedAt(), task.getUpdatedAt(), task.isDeleted(), task.getTitle(), task.getNotes(), task.getOrder(), task.getPriority(), task.getCompletionDate(), task.getSchedule(), task.getLocation(), task.getRepeatDate(), task.getRepeatOption(), task.getOrigin(), task.getOriginIdentifier(), tagsToString(task.getTags()));
    }

    private String tagsToString(List<GsonTag> tags) {
        String stringTags = "";

        // Convert tags to comma-separated string.
        if (tags != null) {
            for (GsonTag tag : tags) {
                stringTags += tag.getTempId() + ",";
            }
        }

        return stringTags;
    }

}
