package com.swipesapp.android.sync.service;

import android.content.Context;
import android.os.Process;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.Ion;
import com.parse.ParseUser;
import com.swipesapp.android.BuildConfig;
import com.swipesapp.android.app.SwipesApplication;
import com.swipesapp.android.db.DaoSession;
import com.swipesapp.android.db.Tag;
import com.swipesapp.android.db.TagSync;
import com.swipesapp.android.db.TaskSync;
import com.swipesapp.android.db.dao.ExtTagSyncDao;
import com.swipesapp.android.db.dao.ExtTaskSyncDao;
import com.swipesapp.android.evernote.EvernoteIntegration;
import com.swipesapp.android.evernote.EvernoteSyncHandler;
import com.swipesapp.android.evernote.OnEvernoteCallback;
import com.swipesapp.android.sync.gson.GsonObjects;
import com.swipesapp.android.sync.gson.GsonSync;
import com.swipesapp.android.sync.gson.GsonTag;
import com.swipesapp.android.sync.gson.GsonTask;
import com.swipesapp.android.util.DateUtils;
import com.swipesapp.android.util.PreferenceUtils;
import com.swipesapp.android.values.Actions;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
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

    private WeakReference<Context> mContext;

    private List<TagSync> mSyncedTags;
    private List<TaskSync> mSyncedTasks;

    private static final String API_URL = "http://api.swipesapp.com/v1/sync";

    private static final String PLATFORM = "android";

    private static final int MAX_OBJECTS = 200;

    private static final String LOG_TAG = SyncService.class.getSimpleName();

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
     * @param changesOnly True to sync only changes.
     */
    public void performSync(final boolean changesOnly) {
        // Create new thread for responsiveness.
        new Thread(new Runnable() {
            @Override
            public void run() {
                // Start thread with low priority.
                Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);

                // Forward call to internal sync method.
                performSync(changesOnly, false);
            }
        }).start();
    }

    /**
     * Performs a sync operation. Use this one for recursive calls.
     *
     * @param changesOnly True to sync only changes.
     * @param isRecursion True when called as recursive function.
     */
    private synchronized void performSync(final boolean changesOnly, boolean isRecursion) {
        // Skip sync when the user isn't logged in.
        if (ParseUser.getCurrentUser() == null) return;

        // Objects holding all local changes.
        List<TagSync> tagsChanged = mExtTagSyncDao.listTagsForSync();
        List<TaskSync> tasksChanged = mExtTaskSyncDao.listTasksForSync();

        // Only sync when called out of recursion or when there still are local changes.
        if (!isRecursion || !tagsChanged.isEmpty() || !tasksChanged.isEmpty()) {
            // Prepare Gson request.
            GsonSync request = prepareRequest(tagsChanged, tasksChanged, changesOnly);

            // Create JSON string.
            Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();
            String json = handleRequestNulls(gson.toJson(request));

            // Send changes to the API.
            Ion.with(mContext.get())
                    .load(API_URL)
                    .setHeader("Content-Type", "application/json")
                    .setStringBody(json)
                    .asString()
                    .setCallback(new FutureCallback<String>() {
                        @Override
                        public void onCompleted(Exception e, String result) {
                            // Skip processing if response is invalid.
                            if (!isResponseValid(result)) return;

                            // Delete synced objects from tracking.
                            deleteTrackedTags(mSyncedTags);
                            deleteTrackedTasks(mSyncedTasks);

                            // Read API response and save changes locally.
                            handleResponse(new Gson().fromJson(result, GsonSync.class));

                            // Call recursion to sync remaining objects.
                            performSync(changesOnly, true);
                        }
                    });
        }

        if (EvernoteIntegration.getInstance().isAuthenticated()) {
            EvernoteSyncHandler.getInstance().synchronizeEvernote(mContext.get(), new OnEvernoteCallback<Void>() {
                @Override
                public void onSuccess(Void data) {
                    Log.i(LOG_TAG, "Evernote synchronized!");
                }

                @Override
                public void onException(Exception ex) {
                    Log.e(LOG_TAG, "Evernote sync error!", ex);
                }
            });
        }
    }

    private GsonSync prepareRequest(List<TagSync> tags, List<TaskSync> tasks, boolean changesOnly) {
        // Amount of objects in the current batch.
        int objectCount = 0;

        // Sync objects included in the current batch.
        mSyncedTags = new ArrayList<TagSync>();
        mSyncedTasks = new ArrayList<TaskSync>();

        // Gson objects to append to the API request.
        List<GsonTag> gsonTags = new ArrayList<GsonTag>();
        List<GsonTask> gsonTasks = new ArrayList<GsonTask>();

        // Prepare tag objects.
        for (TagSync tag : tags) {
            if (objectCount <= MAX_OBJECTS) {
                GsonTag gson = GsonTag.gsonForSync(null, tag.getObjectId(), tag.getTempId(), tag.getCreatedAt(), tag.getUpdatedAt(), tag.getTitle(), tag.getDeleted());
                gsonTags.add(gson);

                mSyncedTags.add(tag);
                objectCount++;
            } else break;
        }

        // Prepare task objects.
        for (TaskSync task : tasks) {
            if (objectCount <= MAX_OBJECTS) {
                GsonTask gson = gsonFromTaskSync(task);
                gsonTasks.add(gson);

                mSyncedTasks.add(task);
                objectCount++;
            } else break;
        }

        // Default request parameters.
        String sessionToken = ParseUser.getCurrentUser().getSessionToken();
        String version = String.valueOf(BuildConfig.VERSION_CODE);
        String lastUpdate = PreferenceUtils.getSyncLastUpdate(mContext.get());

        // Append objects in the current batch.
        GsonObjects objects = new GsonObjects(gsonTags, gsonTasks);
        GsonSync request = new GsonSync(sessionToken, PLATFORM, version, changesOnly, lastUpdate, objects);

        return request;
    }

    private void handleResponse(final GsonSync response) {
        // Process new tags.
        if (response.getTags() != null) {
            for (GsonTag tag : response.getTags()) {
                GsonTag localTag = TasksService.getInstance(mContext.get()).loadTag(tag.getTempId());

                // Check if tag already exists locally.
                if (localTag == null) {
                    // Set dates to local format.
                    tag.setLocalCreatedAt(DateUtils.dateFromSync(tag.getCreatedAt()));
                    tag.setLocalUpdatedAt(DateUtils.dateFromSync(tag.getUpdatedAt()));

                    // Save tag locally.
                    if (!tag.getDeleted()) {
                        TasksService.getInstance(mContext.get()).saveTag(tag);
                    }
                } else {
                    // Delete tag locally.
                    if (tag.getDeleted()) {
                        TasksService.getInstance(mContext.get()).deleteTag(localTag.getId());
                    }
                }
            }
        }

        // Create another thread for processing tasks.
        new Thread(new Runnable() {
            @Override
            public void run() {
                // Start thread with low priority.
                Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);

                // Process new tasks and changes.
                if (response.getTasks() != null) {
                    for (GsonTask task : response.getTasks()) {
                        GsonTask old = TasksService.getInstance(mContext.get()).loadTask(task.getTempId());
                        task.setId(old != null ? old.getId() : null);

                        // Set dates to local format.
                        task.setLocalCreatedAt(DateUtils.dateFromSync(task.getCreatedAt()));
                        task.setLocalUpdatedAt(DateUtils.dateFromSync(task.getUpdatedAt()));
                        task.setLocalCompletionDate(DateUtils.dateFromSync(task.getCompletionDate()));
                        task.setLocalSchedule(DateUtils.dateFromSync(task.getSchedule()));
                        task.setLocalRepeatDate(DateUtils.dateFromSync(task.getRepeatDate()));

                        // Save or update task locally.
                        TasksService.getInstance(mContext.get()).saveTask(task, false);
                    }
                }

                // Refresh local content.
                TasksService.getInstance(mContext.get()).sendBroadcast(Actions.TASKS_CHANGED);
            }
        }).start();

        // Save last update time.
        PreferenceUtils.saveStringPreference(PreferenceUtils.SYNC_LAST_UPDATE, response.getUpdateTime(), mContext.get());
    }

    public void saveTaskChangesForSync(GsonTask task) {
        // Skip saving when the user isn't logged in.
        if (ParseUser.getCurrentUser() == null) return;

        TaskSync taskSync = new TaskSync();

        if (task.getId() == null) {
            // Save entire task for sync.
            taskSync = taskSyncFromGson(task);
            mExtTaskSyncDao.getDao().insert(taskSync);
        } else {
            GsonTask old = TasksService.getInstance(mContext.get()).loadTask(task.getId());

            // Save only changed attributes.
            taskSync.setObjectId(task.getObjectId());
            taskSync.setTempId(task.getTempId());
            taskSync.setUpdatedAt(DateUtils.dateToSync(task.getLocalUpdatedAt()));
            taskSync.setDeleted(hasObjectChanged(old.getDeleted(), task.getDeleted()) ? task.getDeleted() : null);
            taskSync.setTitle(hasObjectChanged(old.getTitle(), task.getTitle()) ? task.getTitle() : null);
            taskSync.setNotes(hasObjectChanged(old.getNotes(), task.getNotes()) ? task.getNotes() : null);
            taskSync.setOrder(hasObjectChanged(old.getOrder(), task.getOrder()) ? task.getOrder() : null);
            taskSync.setPriority(hasObjectChanged(old.getPriority(), task.getPriority()) ? task.getPriority() : null);
            taskSync.setCompletionDate(hasObjectChanged(old.getLocalCompletionDate(), task.getLocalCompletionDate()) ? DateUtils.dateToSync(task.getLocalCompletionDate()) : null);
            taskSync.setSchedule(hasObjectChanged(old.getLocalSchedule(), task.getLocalSchedule()) ? DateUtils.dateToSync(task.getLocalSchedule()) : null);
            taskSync.setLocation(hasObjectChanged(old.getLocation(), task.getLocation()) ? task.getLocation() : null);
            taskSync.setRepeatDate(hasObjectChanged(old.getLocalRepeatDate(), task.getLocalRepeatDate()) ? DateUtils.dateToSync(task.getLocalRepeatDate()) : null);
            taskSync.setRepeatOption(task.getRepeatOption().equals(old.getRepeatOption()) ? null : task.getRepeatOption());
            taskSync.setTags(hasObjectChanged(old.getTags(), task.getTags()) ? tagsToString(task.getTags()) : null);

            mExtTaskSyncDao.getDao().insert(taskSync);
        }
    }

    public void saveTagForSync(GsonTag tag) {
        // Skip saving when the user isn't logged in.
        if (ParseUser.getCurrentUser() == null) return;

        TagSync tagSync = new TagSync(null, tag.getObjectId(), tag.getTempId(), DateUtils.dateToSync(tag.getLocalCreatedAt()), DateUtils.dateToSync(tag.getLocalUpdatedAt()), tag.getTitle(), false);
        mExtTagSyncDao.getDao().insert(tagSync);
    }

    public void saveDeletedTagForSync(Tag tag) {
        // Skip saving when the user isn't logged in.
        if (ParseUser.getCurrentUser() == null) return;

        TagSync tagSync = new TagSync(null, tag.getObjectId(), tag.getTempId(), null, null, null, true);
        mExtTagSyncDao.getDao().insert(tagSync);
    }

    private TaskSync taskSyncFromGson(GsonTask task) {
        return new TaskSync(null, task.getObjectId(), task.getTempId(), task.getParentLocalId(), DateUtils.dateToSync(task.getLocalCreatedAt()), DateUtils.dateToSync(task.getLocalUpdatedAt()),
                task.isDeleted(), task.getTitle(), task.getNotes(), task.getOrder(), task.getPriority(), DateUtils.dateToSync(task.getLocalCompletionDate()), DateUtils.dateToSync(task.getLocalSchedule()),
                task.getLocation(), DateUtils.dateToSync(task.getLocalRepeatDate()), task.getRepeatOption(), task.getOrigin(), task.getOriginIdentifier(), tagsToString(task.getTags()));
    }

    private GsonTask gsonFromTaskSync(TaskSync task) {
        String tempId = task.getTempId() != null ? task.getTempId() : task.getObjectId();
        List<GsonTag> tags = TasksService.getInstance(mContext.get()).loadTask(tempId).getTags();

        return GsonTask.gsonForSync(task.getObjectId(), task.getTempId(), task.getParentLocalId(), task.getCreatedAt(), task.getUpdatedAt(), task.getDeleted(), task.getTitle(), task.getNotes(), task.getOrder(),
                task.getPriority(), task.getCompletionDate(), task.getSchedule(), task.getLocation(), task.getRepeatDate(), task.getRepeatOption(), task.getOrigin(), task.getOriginIdentifier(), tags);
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

    private boolean hasObjectChanged(Object oldObject, Object newObject) {
        boolean hasChanged = false;

        // Check if property has been set to null (e.g. rescheduled a completed task).
        if (oldObject != null && newObject == null) hasChanged = true;

        // Check if objects are different.
        if (newObject != null && !newObject.equals(oldObject)) hasChanged = true;

        return hasChanged;
    }

    private String handleRequestNulls(String json) {
        return json.replace("{\"iso\":\"null\"}", "\"null\"").replace(":\"null\"", ":null");
    }

    private void deleteTrackedTags(List<TagSync> tags) {
        for (TagSync tag : tags) {
            mExtTagSyncDao.getDao().delete(tag);
        }
    }

    private void deleteTrackedTasks(List<TaskSync> tasks) {
        for (TaskSync task : tasks) {
            mExtTaskSyncDao.getDao().delete(task);
        }
    }

    private boolean isResponseValid(String response) {
        // Validates response by attempting to convert it to a Gson object.
        try {
            new Gson().fromJson(response, GsonSync.class);
            return !response.isEmpty();
        } catch (Exception e) {
            Log.w(LOG_TAG, "Invalid response, couldn't convert to Gson. Aborting sync.\n" + e.getMessage());
            return false;
        }
    }

}
