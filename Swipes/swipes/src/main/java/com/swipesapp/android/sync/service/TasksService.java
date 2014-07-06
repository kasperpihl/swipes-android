package com.swipesapp.android.sync.service;

import android.content.Context;
import android.content.Intent;

import com.swipesapp.android.db.DaoMaster;
import com.swipesapp.android.db.DaoSession;
import com.swipesapp.android.db.ExtTagDao;
import com.swipesapp.android.db.ExtTaskDao;
import com.swipesapp.android.db.ExtTaskTagDao;
import com.swipesapp.android.db.Tag;
import com.swipesapp.android.db.Task;
import com.swipesapp.android.db.TaskTag;
import com.swipesapp.android.sync.gson.GsonTag;
import com.swipesapp.android.sync.gson.GsonTask;
import com.swipesapp.android.values.Actions;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Service for task operations.
 *
 * @author Felipe Bari
 */
public class TasksService {

    private static TasksService sInstance;

    private ExtTaskDao mExtTaskDao;
    private ExtTagDao mExtTagDao;
    private ExtTaskTagDao mExtTaskTagDao;

    private WeakReference<Context> mContext;

    private static final String DB_NAME = "swipes-db";

    /**
     * Internal constructor. Handles loading of extended DAOs for custom DB operations.
     *
     * @param context Context instance.
     */
    private TasksService(Context context) {
        mContext = new WeakReference<Context>(context);

        DaoMaster.DevOpenHelper helper = new DaoMaster.DevOpenHelper(mContext.get(), DB_NAME, null);
        DaoMaster daoMaster = new DaoMaster(helper.getWritableDatabase());
        DaoSession daoSession = daoMaster.newSession();

        mExtTaskDao = ExtTaskDao.getInstance(daoSession);
        mExtTagDao = ExtTagDao.getInstance(daoSession);
        mExtTaskTagDao = ExtTaskTagDao.getInstance(daoSession);
    }

    /**
     * Returns an existing instance of the service, or loads a new one if needed.
     * This ensures only one DAO session is active at any given time.
     *
     * @param context
     * @return Service instance.
     */
    public static TasksService getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new TasksService(context);
        }
        return sInstance;
    }

    /**
     * Sends a broadcast.
     *
     * @param action Action to broadcast.
     */
    public void sendBroadcast(String action) {
        Intent intent = new Intent();
        intent.setAction(action);
        mContext.get().sendBroadcast(intent);
    }

    /**
     * Creates a new task, or updates an existing one.
     *
     * @param gsonTask Object holding task data.
     */
    public void saveTask(GsonTask gsonTask) {
        Task task = mExtTaskDao.selectTask(gsonTask.getTempId());

        if (task == null) {
            createTask(gsonTask);
        } else {
            updateTask(gsonTask, task);
        }

        sendBroadcast(Actions.TASKS_CHANGED);
    }

    /**
     * Deletes tasks.
     *
     * @param tasks List containing tasks to delete.
     */
    public void deleteTasks(List<GsonTask> tasks) {
        for (GsonTask task : tasks) {
            // Mark task as deleted and persist change.
            task.setDeleted(true);
            saveTask(task);
        }
    }

    /**
     * Creates a new task.
     *
     * @param gsonTask Object holding new task data.
     */
    private void createTask(GsonTask gsonTask) {
        Task task = tasksFromGson(Arrays.asList(gsonTask)).get(0);

        synchronized (this) {
            Long taskId = mExtTaskDao.getDao().insert(task);
            saveTags(taskId, gsonTask.getTags());
        }
    }

    /**
     * Updates an existing task.
     *
     * @param gsonTask Object holding updated data.
     * @param task     Task to update.
     */
    private void updateTask(GsonTask gsonTask, Task task) {
        // Update only mutable attributes.
        task.setTempId(gsonTask.getTempId());
        task.setUpdatedAt(gsonTask.getUpdatedAt());
        task.setDeleted(gsonTask.getDeleted());
        task.setTitle(gsonTask.getTitle());
        task.setNotes(gsonTask.getNotes());
        task.setOrder(gsonTask.getOrder());
        task.setPriority(gsonTask.getPriority());
        task.setCompletionDate(gsonTask.getCompletionDate());
        task.setSchedule(gsonTask.getSchedule());
        task.setLocation(gsonTask.getLocation());
        task.setRepeatDate(gsonTask.getRepeatDate());
        task.setRepeatOption(gsonTask.getRepeatOption());

        synchronized (this) {
            mExtTaskDao.getDao().update(task);
            saveTags(task.getId(), gsonTask.getTags());
        }
    }

    /**
     * Creates new tags, or associates existing ones with a task.
     *
     * @param taskId   ID of the task.
     * @param gsonTags List of objects holding tag data.
     */
    private void saveTags(Long taskId, List<GsonTag> gsonTags) {
        List<Tag> tags = tagsFromGson(gsonTags);

        for (Tag tag : tags) {
            Long tagId = tag.getId();

            // Search for association between task and tag.
            TaskTag association = mExtTaskTagDao.selectAssociation(taskId, tagId);

            // If an association already exists, do nothing.
            if (association == null) {
                // If tag exists, create only association.
                if (tagId == null) {
                    // Create new tag.
                    tagId = mExtTagDao.getDao().insert(tag);
                }

                // Create association.
                association = new TaskTag(taskId, tagId);
                mExtTaskTagDao.getDao().insert(association);
            }
        }
    }

    /**
     * Loads a single task.
     *
     * @param tempId Temp ID of the task.
     * @return Selected task.
     */
    public GsonTask loadTask(String tempId) {
        return gsonFromTasks(Arrays.asList(mExtTaskDao.selectTask(tempId))).get(0);
    }

    /**
     * Loads all existing tasks.
     *
     * @return List of tasks.
     */
    private List<GsonTask> loadAllTasks() {
        return gsonFromTasks(mExtTaskDao.listAllTasks());
    }

    /**
     * Loads scheduled tasks.
     *
     * @return List of tasks.
     */
    public List<GsonTask> loadScheduledTasks() {
        return gsonFromTasks(mExtTaskDao.listScheduledTasks());
    }

    /**
     * Loads focused tasks.
     *
     * @return List of tasks.
     */
    public List<GsonTask> loadFocusedTasks() {
        return gsonFromTasks(mExtTaskDao.listFocusedTasks());
    }

    /**
     * Loads completed tasks.
     *
     * @return List of tasks.
     */
    public List<GsonTask> loadCompletedTasks() {
        return gsonFromTasks(mExtTaskDao.listCompletedTasks());
    }

    /**
     * Load all existing tags.
     *
     * @return List of tags.
     */
    public List<GsonTag> loadAllTags() {
        return gsonFromTags(mExtTagDao.listAllTags());
    }

    /**
     * Loads all tasks associated with a tag, for filtering.
     *
     * @param tagId ID of the tag.
     * @return List of tasks.
     */
    public List<GsonTask> loadTasksForTag(Long tagId) {
        return gsonFromTasks(mExtTagDao.listTasksForTag(tagId));
    }

    /**
     * Loads all tags associated with a task, for displaying.
     *
     * @param taskId ID of the task.
     * @return List of tags.
     */
    private List<GsonTag> loadTagsForTask(Long taskId) {
        return gsonFromTags(mExtTaskDao.listTagsForTask(taskId));
    }

    /**
     * Converts a list of Task objects to GsonTask.
     *
     * @param tasks List of tasks.
     * @return Converted list.
     */
    private List<GsonTask> gsonFromTasks(List<Task> tasks) {
        List<GsonTask> gsonTasks = new ArrayList<GsonTask>();

        for (Task task : tasks) {
            gsonTasks.add(new GsonTask(task.getObjectId(), task.getTempId(), task.getParentId(), task.getCreatedAt(), task.getUpdatedAt(), task.getDeleted(), task.getTitle(), task.getNotes(), task.getOrder(), task.getPriority(), task.getCompletionDate(), task.getSchedule(), task.getLocation(), task.getRepeatDate(), task.getRepeatOption(), loadTagsForTask(task.getId())));
        }

        return gsonTasks;
    }

    /**
     * Converts a list of Tag objects to GsonTag.
     *
     * @param tags List of tags.
     * @return Converted list.
     */
    private List<GsonTag> gsonFromTags(List<Tag> tags) {
        List<GsonTag> gsonTags = new ArrayList<GsonTag>();

        for (Tag tag : tags) {
            gsonTags.add(new GsonTag(tag.getId(), tag.getObjectId(), tag.getTempId(), tag.getCreatedAt(), tag.getUpdatedAt(), tag.getTitle()));
        }

        return gsonTags;
    }

    /**
     * Converts a list of GsonTask to Task objects.
     *
     * @param gsonTasks List of GsonTask.
     * @return Converted list.
     */
    private List<Task> tasksFromGson(List<GsonTask> gsonTasks) {
        List<Task> tasks = new ArrayList<Task>();

        for (GsonTask gsonTask : gsonTasks) {
            tasks.add(new Task(null, gsonTask.getObjectId(), gsonTask.getTempId(), gsonTask.getParentId(), gsonTask.getCreatedAt(), gsonTask.getUpdatedAt(), gsonTask.getDeleted(), gsonTask.getTitle(), gsonTask.getNotes(), gsonTask.getOrder(), gsonTask.getPriority(), gsonTask.getCompletionDate(), gsonTask.getSchedule(), gsonTask.getLocation(), gsonTask.getRepeatDate(), gsonTask.getRepeatOption()));
        }

        return tasks;
    }

    /**
     * Converts a list of GsonTag to Tag objects.
     *
     * @param gsonTags List of GsonTag.
     * @return Converted list.
     */
    private List<Tag> tagsFromGson(List<GsonTag> gsonTags) {
        List<Tag> tags = new ArrayList<Tag>();

        for (GsonTag gsonTag : gsonTags) {
            tags.add(new Tag(gsonTag.getTagId(), gsonTag.getObjectId(), gsonTag.getTempId(), gsonTag.getCreatedAt(), gsonTag.getUpdatedAt(), gsonTag.getTitle()));
        }

        return tags;
    }

}
