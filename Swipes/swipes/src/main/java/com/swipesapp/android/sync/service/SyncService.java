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
import com.swipesapp.android.db.Attachment;
import com.swipesapp.android.db.DaoSession;
import com.swipesapp.android.db.Tag;
import com.swipesapp.android.db.TagSync;
import com.swipesapp.android.db.Task;
import com.swipesapp.android.db.TaskSync;
import com.swipesapp.android.db.dao.ExtTagSyncDao;
import com.swipesapp.android.db.dao.ExtTaskSyncDao;
import com.swipesapp.android.evernote.EvernoteService;
import com.swipesapp.android.evernote.EvernoteSyncHandler;
import com.swipesapp.android.evernote.OnEvernoteCallback;
import com.swipesapp.android.sync.gson.GsonDate;
import com.swipesapp.android.sync.gson.GsonObjects;
import com.swipesapp.android.sync.gson.GsonSync;
import com.swipesapp.android.sync.gson.SyncExclusionStrategy;
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
                String json = handleRequestNulls(serializeRequest(request));

                logDebugMessage("Request: " + json);

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
                                    sendErrorCallback(result);
                                    return;
                                }

                                logDebugMessage("Response: " + result);

                                // Delete synced objects from tracking.
                                deleteTrackedTags(mSyncedTags);
                                deleteTrackedTasks(mSyncedTasks);

                                // Read API response and save changes locally.
                                handleResponse(result);

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
                    logDebugMessage("Evernote synced.");

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

    private GsonSync prepareRequest(List<TagSync> syncTags, List<TaskSync> syncTasks, boolean changesOnly) {
        // Amount of objects in the current batch.
        int objectCount = 0;

        // Sync objects included in the current batch.
        mSyncedTags = new ArrayList<>();
        mSyncedTasks = new ArrayList<>();

        // Gson objects to append to the API request.
        List<Tag> requestTags = new ArrayList<>();
        List<Task> requestTasks = new ArrayList<>();

        // Prepare tag objects.
        for (TagSync tagSync : syncTags) {
            if (objectCount <= MAX_OBJECTS) {
                Tag tag = new Tag(null, tagSync.getObjectId(), tagSync.getTempId(), null, null, tagSync.getTitle());
                tag.setDeleted(tagSync.getDeleted());
                requestTags.add(tag);

                mSyncedTags.add(tagSync);
                objectCount++;
            } else break;
        }

        // Prepare task objects.
        for (TaskSync task : syncTasks) {
            if (objectCount <= MAX_OBJECTS) {
                Task gson = taskFromTaskSync(task);
                requestTasks.add(gson);

                mSyncedTasks.add(task);
                objectCount++;
            } else break;
        }

        // Default request parameters.
        String sessionToken = ParseUser.getCurrentUser().getSessionToken();
        String version = String.valueOf(BuildConfig.VERSION_CODE);
        String lastUpdate = PreferenceUtils.getSyncLastUpdate(mContext.get());

        // Append objects in the current batch.
        GsonObjects objects = new GsonObjects(requestTags, requestTasks);
        GsonSync request = new GsonSync(sessionToken, PLATFORM, version, changesOnly, lastUpdate, objects);

        return request;
    }

    private void handleResponse(String result) {
        // Deserialize response.
        final GsonSync response = deserializeResponse(result);

        // Process new tags.
        if (response.getTags() != null) {
            for (Tag tag : response.getTags()) {
                Tag localTag = TasksService.getInstance().loadTag(tag.getTempId());

                // Check if tag already exists locally.
                if (localTag == null) {
                    // Set dates to local format.
                    tag.setCreatedAt(DateUtils.dateFromSync(tag.getSyncCreatedAt()));
                    tag.setUpdatedAt(DateUtils.dateFromSync(tag.getSyncUpdatedAt()));

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
        }

        // Create another thread for processing tasks.
        new Thread(new Runnable() {
            @Override
            public void run() {
                // Start thread with low priority.
                Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);

                // Process new tasks and changes.
                if (response.getTasks() != null) {
                    for (Task task : response.getTasks()) {
                        Task old = TasksService.getInstance().loadTask(task.getTempId());
                        task.setId(old != null ? old.getId() : null);

                        // Convert attachments to local format.
                        task.setAttachments(attachmentsFromSync(task));

                        // Set dates to local format.
                        task.setCreatedAt(DateUtils.dateFromSync(task.getSyncCreatedAt()));
                        task.setUpdatedAt(DateUtils.dateFromSync(task.getSyncUpdatedAt()));
                        task.setCompletionDate(DateUtils.dateFromSync(task.getSyncCompletionDate()));
                        task.setSchedule(DateUtils.dateFromSync(task.getSyncSchedule()));
                        task.setRepeatDate(DateUtils.dateFromSync(task.getSyncRepeatDate()));

                        // Save or update task locally.
                        TasksService.getInstance().saveTask(task, false);
                    }

                    // Save last update time.
                    if (response.getUpdateTime() != null) {
                        PreferenceUtils.saveStringPreference(PreferenceUtils.SYNC_LAST_UPDATE, response.getUpdateTime(), mContext.get());
                    }

                    // Refresh local content.
                    TasksService.getInstance().sendBroadcast(Actions.TASKS_CHANGED);
                }
            }
        }).start();
    }

    private String serializeRequest(GsonSync request) {
        // Exclusion strategy to remove objects annotated as @LocalOnly.
        SyncExclusionStrategy localOnly = new SyncExclusionStrategy();

        // Serialize request.
        Gson gson = new GsonBuilder().setExclusionStrategies(localOnly).create();
        return gson.toJson(request);
    }

    private GsonSync deserializeResponse(String result) {
        // Exclusion strategy to remove objects annotated as @LocalOnly.
        SyncExclusionStrategy localOnly = new SyncExclusionStrategy();

        // Deserialize response.
        Gson gson = new GsonBuilder().setExclusionStrategies(localOnly).create();
        return gson.fromJson(result, GsonSync.class);
    }

    public void saveTaskChangesForSync(Task current, Task old) {
        // Skip saving when the user isn't logged in.
        if (ParseUser.getCurrentUser() == null) return;

        if (current.getId() == null) {
            // Save entire task for sync.
            TaskSync taskSync = taskSyncFromTask(current);
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
            taskSync.setUpdatedAt(DateUtils.dateToSync(current.getUpdatedAt()));
            taskSync.setDeleted(hasObjectChanged(old.getDeleted(), current.getDeleted()) ? current.getDeleted() : taskSync.getDeleted());
            taskSync.setTitle(hasObjectChanged(old.getTitle(), current.getTitle()) ? current.getTitle() : taskSync.getTitle());
            taskSync.setNotes(hasObjectChanged(old.getNotes(), current.getNotes()) ? current.getNotes() : taskSync.getNotes());
            taskSync.setOrder(hasObjectChanged(old.getOrder(), current.getOrder()) ? current.getOrder() : taskSync.getOrder());
            taskSync.setPriority(hasObjectChanged(old.getPriority(), current.getPriority()) ? current.getPriority() : taskSync.getPriority());
            taskSync.setCompletionDate(hasObjectChanged(old.getCompletionDate(), current.getCompletionDate()) ? DateUtils.dateToSync(current.getCompletionDate()) : taskSync.getCompletionDate());
            taskSync.setSchedule(hasObjectChanged(old.getSchedule(), current.getSchedule()) ? DateUtils.dateToSync(current.getSchedule()) : taskSync.getSchedule());
            taskSync.setLocation(hasObjectChanged(old.getLocation(), current.getLocation()) ? current.getLocation() : taskSync.getLocation());
            taskSync.setRepeatDate(hasObjectChanged(old.getRepeatDate(), current.getRepeatDate()) ? DateUtils.dateToSync(current.getRepeatDate()) : taskSync.getRepeatDate());
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

    public void saveTagForSync(Tag tag) {
        // Skip saving when the user isn't logged in.
        if (ParseUser.getCurrentUser() == null) return;

        TagSync tagSync = new TagSync(null, tag.getObjectId(), tag.getTempId(), DateUtils.dateToSync(tag.getCreatedAt()), DateUtils.dateToSync(tag.getUpdatedAt()), tag.getTitle(), false);
        mExtTagSyncDao.getDao().insert(tagSync);
    }

    public void saveDeletedTagForSync(Tag tag) {
        // Skip saving when the user isn't logged in.
        if (ParseUser.getCurrentUser() == null) return;

        TagSync tagSync = new TagSync(null, tag.getObjectId(), tag.getTempId(), null, null, null, true);
        mExtTagSyncDao.getDao().insert(tagSync);
    }

    private TaskSync taskSyncFromTask(Task task) {
        // Track changes from task object.
        return new TaskSync(null, task.getObjectId(), task.getTempId(), task.getParentLocalId(), DateUtils.dateToSync(task.getCreatedAt()),
                DateUtils.dateToSync(task.getUpdatedAt()), task.isDeleted(), task.getTitle(), task.getNotes(), task.getOrder(), task.getPriority(),
                DateUtils.dateToSync(task.getCompletionDate()), DateUtils.dateToSync(task.getSchedule()), task.getLocation(), DateUtils.dateToSync(task.getRepeatDate()),
                task.getRepeatOption(), task.getOrigin(), task.getOriginIdentifier(), tagsToString(task.getTags()), attachmentsToString(task.getAttachments()));
    }

    private Task taskFromTaskSync(TaskSync taskSync) {
        String tempId = taskSync.getTempId() != null ? taskSync.getTempId() : taskSync.getObjectId();
        Task current = TasksService.getInstance().loadTask(tempId);

        // Create task object from tracked changes.
        Task task = new Task(null, taskSync.getObjectId(), taskSync.getTempId(), taskSync.getParentLocalId(), null, null, taskSync.getDeleted(),
                taskSync.getTitle(), taskSync.getNotes(), taskSync.getOrder(), taskSync.getPriority(), null, null, taskSync.getLocation(), null,
                taskSync.getRepeatOption(), taskSync.getOrigin(), taskSync.getOriginIdentifier());

        if (current != null) {
            // Load tags if needed.
            if (taskSync.getTags() != null) {
                task.setTags(current.getTags());
            }

            // Load attachments if needed.
            if (taskSync.getAttachments() != null) {
                List<Attachment> currentAttachments = current.getAttachments();
                task.setAttachments(attachmentsToSync(currentAttachments));
            }

            // Set dates with sync format.
            task.setSyncCreatedAt(taskSync.getCreatedAt());
            task.setSyncUpdatedAt(taskSync.getUpdatedAt());
            task.setSyncCompletionDate(GsonDate.dateForSync(taskSync.getCompletionDate()));
            task.setSyncSchedule(GsonDate.dateForSync(taskSync.getSchedule()));
            task.setSyncRepeatDate(GsonDate.dateForSync(taskSync.getRepeatDate()));
        }

        return current != null ? task : null;
    }

    private List<Attachment> attachmentsToSync(List<Attachment> attachments) {
        // Convert sync property to integer.
        if (attachments != null) {
            for (Attachment attachment : attachments) {
                attachment.setSyncInt(attachment.getSync());
            }
        }

        return attachments;
    }

    private List<Attachment> attachmentsFromSync(Task task) {
        List<Attachment> attachments = null;

        try {
            // Try to load attachments.
            attachments = task.getAttachments();

            // Convert sync property to boolean.
            if (attachments != null) {
                for (Attachment attachment : attachments) {
                    attachment.setSync(attachment.getSyncInt());
                }
            }
        } catch (Exception e) {
            // Attachments are empty. Move on.
        }

        return attachments;
    }

    private String tagsToString(List<Tag> tags) {
        String stringTags = null;

        // Convert tags to comma-separated string.
        if (tags != null) {
            for (Tag tag : tags) {
                if (stringTags == null) {
                    stringTags = tag.getTempId();
                } else {
                    stringTags += "," + tag.getTempId();
                }
            }
        }

        return stringTags;
    }

    private String attachmentsToString(List<Attachment> attachments) {
        String stringAttachments = null;

        // Convert attachments to comma-separated string.
        if (attachments != null) {
            for (Attachment attachment : attachments) {
                if (stringAttachments == null) {
                    stringAttachments = attachment.getIdentifier();
                } else {
                    stringAttachments += "," + attachment.getIdentifier();
                }
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
            deserializeResponse(response);
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
        logDebugMessage("Sync done.");

        // Send callback.
        if (mListener != null) {
            mListener.onSyncDone();
            mListener = null;
        }
    }

    private void sendErrorCallback(String response) {
        // Mark sync as performed.
        mIsSyncing = false;
        Log.e(LOG_TAG, "Sync error. Response: " + response);

        // Send callback.
        if (mListener != null) {
            mListener.onSyncFailed();
            mListener = null;
        }
    }

    private void logDebugMessage(String message) {
        if (BuildConfig.DEBUG) {
            Log.d(LOG_TAG, message);
        }
    }

}
