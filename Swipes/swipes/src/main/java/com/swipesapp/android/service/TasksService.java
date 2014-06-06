package com.swipesapp.android.service;

import android.content.Context;

import com.swipesapp.android.db.DaoMaster;
import com.swipesapp.android.db.DaoSession;
import com.swipesapp.android.db.ExtTagDao;
import com.swipesapp.android.db.ExtTaskDao;
import com.swipesapp.android.db.Tag;
import com.swipesapp.android.db.Task;
import com.swipesapp.android.gson.GsonTag;
import com.swipesapp.android.gson.GsonTask;

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
     * Loads a single task.
     *
     * @param objectId Object ID of the task.
     * @return Selected task.
     */
    public GsonTask loadTask(String objectId) {
        return gsonFromTasks(Arrays.asList(mExtTaskDao.selectTask(objectId))).get(0);
    }

    /**
     * Loads all existing tasks.
     *
     * @return List of tasks.
     */
    public ArrayList<GsonTask> loadAllTasks() {
        return gsonFromTasks(mExtTaskDao.listAllTasks());
    }

    /**
     * Loads scheduled tasks.
     *
     * @return List of tasks.
     */
    public ArrayList<GsonTask> loadScheduledTasks() {
        return gsonFromTasks(mExtTaskDao.listScheduledTasks());
    }

    /**
     * Loads focused tasks.
     *
     * @return List of tasks.
     */
    public ArrayList<GsonTask> loadFocusedTasks() {
        return gsonFromTasks(mExtTaskDao.listFocusedTasks());
    }

    /**
     * Loads completed tasks.
     *
     * @return List of tasks.
     */
    public ArrayList<GsonTask> loadCompletedTasks() {
        return gsonFromTasks(mExtTaskDao.listCompletedTasks());
    }

    /**
     * Load all existing tags.
     *
     * @return List of tags.
     */
    public ArrayList<GsonTag> loadAllTags() {
        return gsonFromTags(mExtTagDao.listAllTags());
    }

    /**
     * Loads all tasks associated with a tag, for filtering.
     *
     * @param tagId ID of the tag.
     * @return List of tasks.
     */
    public ArrayList<GsonTask> loadTasksForTag(Long tagId) {
        return gsonFromTasks(mExtTagDao.listTasksForTag(tagId));
    }

    /**
     * Loads all tags associated with a task, for displaying.
     *
     * @param taskId ID of the task.
     * @return List of tags.
     */
    private ArrayList<GsonTag> loadTagsForTask(Long taskId) {
        return gsonFromTags(mExtTaskDao.listTagsForTask(taskId));
    }

    /**
     * Converts a list of Task objects to GsonTask.
     *
     * @param tasks List of tasks.
     * @return Converted list.
     */
    private ArrayList<GsonTask> gsonFromTasks(List<Task> tasks) {
        ArrayList<GsonTask> gsonTasks = new ArrayList<GsonTask>();

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
    private ArrayList<GsonTag> gsonFromTags(List<Tag> tags) {
        ArrayList<GsonTag> gsonTags = new ArrayList<GsonTag>();

        for (Tag tag : tags) {
            gsonTags.add(new GsonTag(tag.getId(), tag.getObjectId(), tag.getTempId(), tag.getCreatedAt(), tag.getUpdatedAt(), tag.getTitle()));
        }

        return gsonTags;
    }

}
