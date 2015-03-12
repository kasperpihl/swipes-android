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
import com.swipesapp.android.analytics.Analytics;
import com.swipesapp.android.app.SwipesApplication;
import com.swipesapp.android.db.DaoSession;
import com.swipesapp.android.db.Tag;
import com.swipesapp.android.db.TagSync;
import com.swipesapp.android.db.TaskSync;
import com.swipesapp.android.db.dao.ExtTagSyncDao;
import com.swipesapp.android.db.dao.ExtTaskSyncDao;
import com.swipesapp.android.evernote.EvernoteService;
import com.swipesapp.android.evernote.EvernoteSyncHandler;
import com.swipesapp.android.evernote.OnEvernoteCallback;
import com.swipesapp.android.sync.gson.GsonAttachment;
import com.swipesapp.android.sync.gson.GsonObjects;
import com.swipesapp.android.sync.gson.GsonSync;
import com.swipesapp.android.sync.gson.GsonTag;
import com.swipesapp.android.sync.gson.GsonTask;
import com.swipesapp.android.sync.listener.SyncListener;
import com.swipesapp.android.util.DateUtils;
import com.swipesapp.android.util.PreferenceUtils;
import com.swipesapp.android.values.Actions;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Service for syncing operations.
 *
 * @author Felipe Bari
 */
public class SyncService {

    private static final String API_URL = "http://api.swipesapp.com/v1/sync";
    private static final String PLATFORM = "android";
    private static final int MAX_OBJECTS = 200;

    private static final String LOG_TAG = SyncService.class.getSimpleName();

    private static SyncService sInstance;

    private ExtTaskSyncDao mExtTaskSyncDao;
    private ExtTagSyncDao mExtTagSyncDao;

    private WeakReference<Context> mContext;

    private List<TagSync> mSyncedTags;
    private List<TaskSync> mSyncedTasks;

    private ScheduledFuture mSyncSchedule;
    private ScheduledFuture mEvernoteSyncSchedule;

    private SyncListener mListener;

    private boolean mIsSyncing;
    private boolean mIsSyncingEvernote;

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
     * Returns a new instance of the service. Call once during the application's
     * lifecycle to ensure only one DAO session is active at any given time.
     *
     * @param context Application context.
     */
    public static SyncService newInstance(Context context) {
        sInstance = new SyncService(context);
        return sInstance;
    }

    /**
     * Returns an existing instance of the service. Make sure you have called
     * {@link #newInstance(android.content.Context)} at least once before.
     */
    public static SyncService getInstance() {
        return sInstance;
    }

    /**
     * Sets the listener to call during sync events. It gets cleared after every
     * sync, so keep it updated whenever you need it.
     *
     * @param listener Listener instance.
     */
    public void setListener(SyncListener listener) {
        mListener = listener;
    }

    /**
     * Performs a sync operation.
     *
     * @param changesOnly True to sync only changes.
     * @param delay       The delay in seconds before sync starts.
     */
    public void performSync(final boolean changesOnly, int delay) {
        // Skip sync if it's already running.
        if (!mIsSyncing) {
            // Cancel existing schedule if needed.
            if (mSyncSchedule != null) mSyncSchedule.cancel(false);

            // Schedule new thread for responsiveness.
            mSyncSchedule = new ScheduledThreadPoolExecutor(1).schedule(new Runnable() {
                @Override
                public void run() {
                    // Forward call to internal sync method.
                    performSync(changesOnly, false);
                }
            }, delay, TimeUnit.SECONDS);
        }

        // Skip Evernote sync if it's already running.
        if (!mIsSyncingEvernote) {
            // Cancel existing schedule if needed.
            if (mEvernoteSyncSchedule != null) mEvernoteSyncSchedule.cancel(false);

            // Schedule new thread for responsiveness.
            mEvernoteSyncSchedule = new ScheduledThreadPoolExecutor(1).schedule(new Runnable() {
                @Override
                public void run() {
                    // Forward call to Evernote sync.
                    performEvernoteSync();
                }
            }, delay, TimeUnit.SECONDS);
        }
    }

    /**
     * Performs a sync operation. Use this one for recursive calls.
     *
     * @param changesOnly True to sync only changes.
     * @param isRecursion True when called as recursive function.
     */
    private synchronized void performSync(final boolean changesOnly, boolean isRecursion) {
        // Skip sync when the user isn't logged in.
        if (ParseUser.getCurrentUser() != null) {

            // Objects holding all local changes.
            List<TagSync> tagsChanged = mExtTagSyncDao.listTagsForSync();
            List<TaskSync> tasksChanged = mExtTaskSyncDao.listTasksForSync();

            // Only sync when called out of recursion or when there still are local changes.
            if (!isRecursion || !tagsChanged.isEmpty() || !tasksChanged.isEmpty()) {
                // Mark sync as in progress.
                mIsSyncing = true;

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
                                if (!isResponseValid(result)) {
                                    sendErrorCallback();
                                    return;
                                }

                                // Delete synced objects from tracking.
                                deleteTrackedTags(mSyncedTags);
                                deleteTrackedTasks(mSyncedTasks);

                                // Read API response and save changes locally.
                                handleResponse(new Gson().fromJson(result, GsonSync.class));

                                // Call recursion to sync remaining objects.
                                performSync(changesOnly, true);

                                // Notifies listeners that sync is done.
                                sendCompletionCallback();
                            }
                        });
            }
        }
    }

    /**
     * Performs an Evernote sync operation.
     */
    private synchronized void performEvernoteSync() {
        // Skip sync when the user isn't authenticated or sync is disabled.
        if (EvernoteService.getInstance().isAuthenticated() && PreferenceUtils.isEvernoteSyncEnabled(mContext.get())) {

            // Mark Evernote sync as in progress.
            mIsSyncingEvernote = true;

            // Start Evernote sync.
            EvernoteSyncHandler.getInstance().synchronizeEvernote(new OnEvernoteCallback<Void>() {
                @Override
                public void onSuccess(Void data) {
                    Log.d(LOG_TAG, "Evernote synced.");

                    // Mark Evernote sync as performed.
                    mIsSyncingEvernote = false;

                    // Refresh local content.
                    TasksService.getInstance().sendBroadcast(Actions.TASKS_CHANGED);
                }

                @Override
                public void onException(Exception ex) {
                    Log.e(LOG_TAG, "Evernote sync error.", ex);

                    // Mark Evernote sync as performed.
                    mIsSyncingEvernote = false;
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
                GsonTag localTag = TasksService.getInstance().loadTag(tag.getTempId());

                // Check if tag already exists locally.
                if (localTag == null) {
                    // Set dates to local format.
                    tag.setLocalCreatedAt(DateUtils.dateFromSync(tag.getCreatedAt()));
                    tag.setLocalUpdatedAt(DateUtils.dateFromSync(tag.getUpdatedAt()));

                    // Save tag locally.
                    if (!tag.getDeleted()) {
                        TasksService.getInstance().saveTag(tag);
                    }
                } else {
                    // Delete tag locally.
                    if (tag.getDeleted()) {
                        TasksService.getInstance().deleteTag(localTag.getId());
                    }
                }
            }

            // Update number of tags dimension.
            Analytics.sendNumberOfTags(mContext.get());
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
                        GsonTask old = TasksService.getInstance().loadTask(task.getTempId());
                        task.setId(old != null ? old.getId() : null);

                        // Set dates to local format.
                        task.setLocalCreatedAt(DateUtils.dateFromSync(task.getCreatedAt()));
                        task.setLocalUpdatedAt(DateUtils.dateFromSync(task.getUpdatedAt()));
                        task.setLocalCompletionDate(DateUtils.dateFromSync(task.getCompletionDate()));
                        task.setLocalSchedule(DateUtils.dateFromSync(task.getSchedule()));
                        task.setLocalRepeatDate(DateUtils.dateFromSync(task.getRepeatDate()));

                        // HACK: Fix bug causing other platforms to delete the attachments.
                        if (old != null && (task.getAttachments() == null || task.getAttachments().isEmpty())) {
                            task.setAttachments(old.getAttachments());
                        }

                        // Save or update task locally.
                        TasksService.getInstance().saveTask(task, false);
                    }

                    // Save last update time.
                    if (response.getUpdateTime() != null) {
                        PreferenceUtils.saveStringPreference(PreferenceUtils.SYNC_LAST_UPDATE, response.getUpdateTime(), mContext.get());
                    }

                    // Refresh local content.
                    TasksService.getInstance().sendBroadcast(Actions.TASKS_CHANGED);

                    // Update recurring tasks dimension.
                    Analytics.sendRecurringTasks(mContext.get());
                }
            }
        }).start();
    }

    public void saveTaskChangesForSync(GsonTask current, GsonTask old) {
        // Skip saving when the user isn't logged in.
        if (ParseUser.getCurrentUser() == null) return;

        if (current.getId() == null) {
            // Save entire task for sync.
            TaskSync taskSync = taskSyncFromGson(current);
            mExtTaskSyncDao.getDao().insert(taskSync);
        } else {
            // Load old task if not provided.
            if (old == null) {
                old = TasksService.getInstance().loadTask(current.getId());
            }

            // Load previous (not yet synced) changes.
            TaskSync taskSync = mExtTaskSyncDao.selectTaskForSync(current.getTempId());
            if (taskSync == null) taskSync = new TaskSync();

            // Save only changed attributes.
            taskSync.setObjectId(current.getObjectId());
            taskSync.setTempId(current.getTempId());
            taskSync.setUpdatedAt(DateUtils.dateToSync(current.getLocalUpdatedAt()));
            taskSync.setDeleted(hasObjectChanged(old.getDeleted(), current.getDeleted()) ? current.getDeleted() : taskSync.getDeleted());
            taskSync.setTitle(hasObjectChanged(old.getTitle(), current.getTitle()) ? current.getTitle() : taskSync.getTitle());
            taskSync.setNotes(hasObjectChanged(old.getNotes(), current.getNotes()) ? current.getNotes() : taskSync.getNotes());
            taskSync.setOrder(hasObjectChanged(old.getOrder(), current.getOrder()) ? current.getOrder() : taskSync.getOrder());
            taskSync.setPriority(hasObjectChanged(old.getPriority(), current.getPriority()) ? current.getPriority() : taskSync.getPriority());
            taskSync.setCompletionDate(hasObjectChanged(old.getLocalCompletionDate(), current.getLocalCompletionDate()) ? DateUtils.dateToSync(current.getLocalCompletionDate()) : taskSync.getCompletionDate());
            taskSync.setSchedule(hasObjectChanged(old.getLocalSchedule(), current.getLocalSchedule()) ? DateUtils.dateToSync(current.getLocalSchedule()) : taskSync.getSchedule());
            taskSync.setLocation(hasObjectChanged(old.getLocation(), current.getLocation()) ? current.getLocation() : taskSync.getLocation());
            taskSync.setRepeatDate(hasObjectChanged(old.getLocalRepeatDate(), current.getLocalRepeatDate()) ? DateUtils.dateToSync(current.getLocalRepeatDate()) : taskSync.getRepeatDate());
            taskSync.setRepeatOption(hasObjectChanged(old.getRepeatOption(), current.getRepeatOption()) ? current.getRepeatOption() : taskSync.getRepeatOption());
            taskSync.setTags(hasObjectChanged(old.getTags(), current.getTags()) ? tagsToString(current.getTags()) : taskSync.getTags());
            taskSync.setAttachments(hasObjectChanged(old.getAttachments(), current.getAttachments()) ? attachmentsToString(current.getAttachments()) : taskSync.getAttachments());
            taskSync.setOrigin(hasObjectChanged(old.getOrigin(), current.getOrigin()) ? current.getOrigin() : taskSync.getOrigin());
            taskSync.setOriginIdentifier(hasObjectChanged(old.getOriginIdentifier(), current.getOriginIdentifier()) ? current.getOriginIdentifier() : taskSync.getOriginIdentifier());

            // Save or update tracked changes.
            if (taskSync.getId() == null) {
                mExtTaskSyncDao.getDao().insert(taskSync);
            } else {
                mExtTaskSyncDao.getDao().update(taskSync);
            }
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
        return new TaskSync(null, task.getObjectId(), task.getTempId(), task.getParentLocalId(), DateUtils.dateToSync(task.getLocalCreatedAt()),
                DateUtils.dateToSync(task.getLocalUpdatedAt()), task.isDeleted(), task.getTitle(), task.getNotes(), task.getOrder(), task.getPriority(),
                DateUtils.dateToSync(task.getLocalCompletionDate()), DateUtils.dateToSync(task.getLocalSchedule()), task.getLocation(), DateUtils.dateToSync(task.getLocalRepeatDate()),
                task.getRepeatOption(), task.getOrigin(), task.getOriginIdentifier(), tagsToString(task.getTags()), attachmentsToString(task.getAttachments()));
    }

    private GsonTask gsonFromTaskSync(TaskSync task) {
        String tempId = task.getTempId() != null ? task.getTempId() : task.getObjectId();
        GsonTask gsonTask = TasksService.getInstance().loadTask(tempId);

        return gsonTask != null ? GsonTask.gsonForSync(task.getObjectId(), task.getTempId(), task.getParentLocalId(), task.getCreatedAt(), task.getUpdatedAt(), task.getDeleted(), task.getTitle(),
                task.getNotes(), task.getOrder(), task.getPriority(), task.getCompletionDate(), task.getSchedule(), task.getLocation(), task.getRepeatDate(), task.getRepeatOption(),
                task.getOrigin(), task.getOriginIdentifier(), gsonTask.getTags(), gsonTask.getAttachments()) : null;
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

    private String attachmentsToString(List<GsonAttachment> attachments) {
        String stringAttachments = "";

        // Convert attachments to comma-separated string.
        if (attachments != null) {
            for (GsonAttachment attachment : attachments) {
                stringAttachments += attachment.getIdentifier() + ",";
            }
        }

        return stringAttachments;
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
            Log.e(LOG_TAG, "Invalid response, couldn't convert to Gson. Aborting sync.\n" +
                    e.getMessage() + "\n" + response);
            return false;
        }
    }

    private void sendCompletionCallback() {
        // Mark sync as performed.
        mIsSyncing = false;
        Log.d(LOG_TAG, "Sync done.");

        // Send callback.
        if (mListener != null) {
            mListener.onSyncDone();
            mListener = null;
        }
    }

    private void sendErrorCallback() {
        // Mark sync as performed.
        mIsSyncing = false;
        Log.d(LOG_TAG, "Sync error.");

        // Send callback.
        if (mListener != null) {
            mListener.onSyncFailed();
            mListener = null;
        }
    }

}
