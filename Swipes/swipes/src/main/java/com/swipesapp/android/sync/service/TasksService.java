package com.swipesapp.android.sync.service;

import android.content.Context;
import android.content.Intent;

import com.swipesapp.android.app.SwipesApplication;
import com.swipesapp.android.db.DaoSession;
import com.swipesapp.android.db.Tag;
import com.swipesapp.android.db.Task;
import com.swipesapp.android.db.TaskTag;
import com.swipesapp.android.db.dao.ExtTagDao;
import com.swipesapp.android.db.dao.ExtTaskDao;
import com.swipesapp.android.db.dao.ExtTaskTagDao;
import com.swipesapp.android.sync.gson.GsonTag;
import com.swipesapp.android.sync.gson.GsonTask;
import com.swipesapp.android.values.Actions;
import com.swipesapp.android.values.Sections;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
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

    /**
     * Internal constructor. Handles loading of extended DAOs for custom DB operations.
     *
     * @param context Context reference.
     */
    private TasksService(Context context) {
        mContext = new WeakReference<Context>(context);

        DaoSession daoSession = SwipesApplication.getDaoSession();

        mExtTaskDao = ExtTaskDao.getInstance(daoSession);
        mExtTagDao = ExtTagDao.getInstance(daoSession);
        mExtTaskTagDao = ExtTaskTagDao.getInstance(daoSession);
    }

    /**
     * Returns an existing instance of the service, or loads a new one if needed.
     * This ensures only one DAO session is active at any given time.
     *
     * @param context Context reference.
     * @return Service instance.
     */
    public static TasksService getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new TasksService(context);
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
     * @param sync     True to queue changes and perform sync.
     */
    public void saveTask(GsonTask gsonTask, boolean sync) {
        Long id = gsonTask.getId();

        if (sync) SyncService.getInstance(mContext.get()).saveTaskChangesForSync(gsonTask);

        if (id == null) {
            createTask(gsonTask);
        } else {
            Task task = mExtTaskDao.selectTask(id);
            updateTask(gsonTask, task);
        }

        sendBroadcast(Actions.TASKS_CHANGED);

        if (sync) SyncService.getInstance(mContext.get()).performSync(true);
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
            saveTask(task, true);

            // Delete subtasks.
            deleteSubtasksForTask(task.getTempId());
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
        task.setUpdatedAt(gsonTask.getLocalUpdatedAt());
        task.setDeleted(gsonTask.isDeleted());
        task.setTitle(gsonTask.getTitle());
        task.setNotes(gsonTask.getNotes());
        task.setOrder(gsonTask.getOrder());
        task.setPriority(gsonTask.getPriority());
        task.setCompletionDate(gsonTask.getLocalCompletionDate());
        task.setSchedule(gsonTask.getLocalSchedule());
        task.setLocation(gsonTask.getLocation());
        task.setRepeatDate(gsonTask.getLocalRepeatDate());
        task.setRepeatOption(gsonTask.getRepeatOption());

        synchronized (this) {
            mExtTaskDao.getDao().update(task);
            saveTags(task.getId(), gsonTask.getTags());
        }
    }

    /**
     * Associates existing tags with a task.
     *
     * @param taskId   ID of the task.
     * @param gsonTags List of objects holding tag data.
     */
    private void saveTags(Long taskId, List<GsonTag> gsonTags) {
        if (gsonTags != null) {
            for (GsonTag tag : gsonTags) {
                // Load ID based on temp ID or object ID.
                String tempId = tag.getTempId() != null ? tag.getTempId() : tag.getObjectId();
                Long tagId = loadTag(tempId).getId();

                // Search for association between task and tag.
                TaskTag association = mExtTaskTagDao.selectAssociation(taskId, tagId);

                // If an association already exists, do nothing.
                if (association == null) {
                    // Create association.
                    association = new TaskTag(null, taskId, tagId);
                    mExtTaskTagDao.getDao().insert(association);
                }
            }
        }
    }

    /**
     * Creates a new tag.
     *
     * @param title Tag title.
     */
    public void createTag(String title) {
        if (title != null && !title.isEmpty()) {
            Date currentDate = new Date();
            String tempId = title + currentDate.getTime();

            Tag tag = new Tag(null, null, tempId, currentDate, currentDate, title);

            // Persist new tag.
            mExtTagDao.getDao().insert(tag);

            SyncService.getInstance(mContext.get()).saveTagForSync(loadTag(tempId));

            SyncService.getInstance(mContext.get()).performSync(true);
        }
    }

    /**
     * Saves an existing tag.
     *
     * @param tag Tag to save.
     */
    public void saveTag(GsonTag tag) {
        if (tag.getTitle() != null && !tag.getTitle().isEmpty()) {
            // Create local tag.
            Tag localTag = new Tag(null, tag.getObjectId(), tag.getTempId(), tag.getLocalCreatedAt(), tag.getLocalUpdatedAt(), tag.getTitle());

            // Persist local tag.
            mExtTagDao.getDao().insert(localTag);
        }
    }

    /**
     * Unassigns a tag from a task.
     *
     * @param tagId  ID of the tag to unassign.
     * @param taskId ID of the task to unnasign from.
     */
    public void unassignTag(Long tagId, Long taskId) {
        // Load assignment and delete from database.
        TaskTag assignment = mExtTaskTagDao.selectAssociation(taskId, tagId);
        mExtTaskTagDao.getDao().delete(assignment);
    }

    /**
     * Deletes a task from the database and unassigns it from all tasks.
     *
     * @param tagId ID of the tag to delete.
     */
    public void deleteTag(Long tagId) {
        // Unassign from all tasks.
        List<TaskTag> assignments = mExtTaskTagDao.selectAssociationsForTag(tagId);
        for (TaskTag assignment : assignments) {
            mExtTaskTagDao.getDao().delete(assignment);
        }

        // Delete from database.
        Tag tag = mExtTagDao.selectTag(tagId);
        mExtTagDao.getDao().delete(tag);

        SyncService.getInstance(mContext.get()).saveDeletedTagForSync(tag);

        SyncService.getInstance(mContext.get()).performSync(true);
    }

    /**
     * Loads a single task.
     *
     * @param id ID of the task.
     * @return Selected task.
     */
    public GsonTask loadTask(Long id) {
        Task task = mExtTaskDao.selectTask(id);
        return task != null ? gsonFromTasks(Arrays.asList(task)).get(0) : null;
    }

    /**
     * Loads a single task.
     *
     * @param tempId Temp ID of the task.
     * @return Selected task.
     */
    public GsonTask loadTask(String tempId) {
        Task task = mExtTaskDao.selectTask(tempId);
        return task != null ? gsonFromTasks(Arrays.asList(task)).get(0) : null;
    }

    /**
     * Loads all existing tasks.
     *
     * @return List of tasks.
     */
    public List<GsonTask> loadAllTasks() {
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
     * Searches tasks for the given query and section.
     *
     * @param query   Query to search for.
     * @param section Section to search from.
     * @return List of tasks.
     */
    public List<GsonTask> searchTasks(String query, Sections section) {
        List<GsonTask> tasks = new ArrayList<GsonTask>();
        List<GsonTask> results = new ArrayList<GsonTask>();

        switch (section) {
            case LATER:
                tasks = loadScheduledTasks();
                break;
            case FOCUS:
                tasks = loadFocusedTasks();
                break;
            case DONE:
                tasks = loadCompletedTasks();
                break;
        }

        for (GsonTask task : tasks) {
            String title = task.getTitle() != null ? task.getTitle().toLowerCase() : "";
            String notes = task.getNotes() != null ? task.getNotes().toLowerCase() : "";

            if ((title.contains(query) || notes.contains(query) || tagsContainQuery(query, task) && !task.isDeleted())) {
                results.add(task);
            }
        }

        return results;
    }

    /**
     * Check if tags in a given task contain the given query.
     *
     * @param query Query to search for.
     * @param task  Task to search from.
     * @return True if they contain, false otherwise.
     */
    private boolean tagsContainQuery(String query, GsonTask task) {
        for (GsonTag tag : task.getTags()) {
            String title = tag.getTitle().toLowerCase();

            if (title.contains(query)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Loads a single tag.
     *
     * @param id Database ID of the tag.
     * @return Selected tag.
     */
    public GsonTag loadTag(Long id) {
        Tag tag = mExtTagDao.selectTag(id);
        return tag != null ? gsonFromTags(Arrays.asList(tag)).get(0) : null;
    }

    /**
     * Loads a single tag.
     *
     * @param tempId Temp ID of the tag.
     * @return Selected tag.
     */
    public GsonTag loadTag(String tempId) {
        Tag tag = mExtTagDao.selectTag(tempId);
        return tag != null ? gsonFromTags(Arrays.asList(tag)).get(0) : null;
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
     * Load all tags assigned to at least one task.
     *
     * @return List of tags.
     */
    public List<GsonTag> loadAllAssignedTags() {
        List<GsonTag> tags = loadAllTags();
        List<GsonTag> assigned = new ArrayList<GsonTag>();

        for (GsonTag tag : tags) {
            List<TaskTag> associations = mExtTaskTagDao.selectAssociationsForTag(tag.getId());

            for (TaskTag association : associations) {
                Task task = mExtTaskDao.selectTask(association.getTaskId());

                if (!task.getDeleted() && !assigned.contains(tag)) assigned.add(tag);
            }
        }

        return assigned;
    }

    /**
     * Loads all tasks associated with a tag for the given section.
     *
     * @param tagId   ID of the tag.
     * @param section Section to load tasks from.
     * @return List of tasks.
     */
    public List<GsonTask> loadTasksForTag(Long tagId, Sections section) {
        List<TaskTag> associations = mExtTaskTagDao.selectAssociationsForTag(tagId);
        List<GsonTask> tasks = new ArrayList<GsonTask>();

        for (TaskTag association : associations) {
            GsonTask task = loadTask(association.getTaskId());
            boolean isFromSection = false;

            switch (section) {
                case LATER:
                    isFromSection = task.getLocalSchedule().after(new Date());
                    break;
                case FOCUS:
                    isFromSection = (task.getLocalSchedule() == null || task.getLocalSchedule().before(new Date())) && task.getLocalCompletionDate() == null;
                    break;
                case DONE:
                    isFromSection = task.getLocalCompletionDate() != null;
                    break;
            }

            if (isFromSection && !task.isDeleted()) tasks.add(task);
        }

        return tasks;
    }

    /**
     * Loads all tags associated with a task, for displaying.
     *
     * @param taskId ID of the task.
     * @return List of tags.
     */
    private List<GsonTag> loadTagsForTask(Long taskId) {
        List<TaskTag> associations = mExtTaskTagDao.selectAssociationsForTask(taskId);
        List<GsonTag> tags = new ArrayList<GsonTag>();

        for (TaskTag association : associations) {
            tags.add(loadTag(association.getTagId()));
        }

        return tags;
    }

    /**
     * Loads all subtasks for a given task.
     *
     * @param objectId Parent object ID.
     * @return List of subtasks.
     */
    public List<GsonTask> loadSubtasksForTask(String objectId) {
        return gsonFromTasks(mExtTaskDao.listSubtasksForTask(objectId));
    }

    private void deleteSubtasksForTask(String objectId) {
        List<GsonTask> subtasks = loadSubtasksForTask(objectId);

        // Mark subtasks as deleted and persist changes.
        for (GsonTask subtask : subtasks) {
            subtask.setDeleted(true);
            saveTask(subtask, true);
        }
    }

    /**
     * Clears all user data from the database.
     */
    public void clearAllData() {
        // Delete all task-tag associations.
        List<TaskTag> joins = mExtTaskTagDao.listAllAssociations();
        for (TaskTag join : joins) {
            join.delete();
        }

        // Delete all tasks.
        List<Task> tasks = mExtTaskDao.listAllTasks();
        for (Task task : tasks) {
            task.delete();
        }

        // Delete all tags.
        List<Tag> tags = mExtTagDao.listAllTags();
        for (Tag tag : tags) {
            tag.delete();
        }
    }

    /**
     * Converts a list of Task objects to GsonTask.
     *
     * @param tasks List of tasks.
     * @return Converted list.
     */
    private List<GsonTask> gsonFromTasks(List<Task> tasks) {
        List<GsonTask> gsonTasks = new ArrayList<GsonTask>();

        if (tasks != null) {
            for (Task task : tasks) {
                gsonTasks.add(GsonTask.gsonForLocal(task.getId(), task.getObjectId(), task.getTempId(), task.getParentLocalId(), task.getCreatedAt(), task.getUpdatedAt(), task.getDeleted(), task.getTitle(), task.getNotes(), task.getOrder(), task.getPriority(), task.getCompletionDate(), task.getSchedule(), task.getLocation(), task.getRepeatDate(), task.getRepeatOption(), task.getOrigin(), task.getOriginIdentifier(), loadTagsForTask(task.getId()), task.getId()));
            }
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

        if (tags != null) {
            for (Tag tag : tags) {
                gsonTags.add(GsonTag.gsonForLocal(tag.getId(), tag.getObjectId(), tag.getTempId(), tag.getCreatedAt(), tag.getUpdatedAt(), tag.getTitle()));
            }
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

        if (gsonTasks != null) {
            for (GsonTask gsonTask : gsonTasks) {
                tasks.add(new Task(gsonTask.getId(), gsonTask.getObjectId(), gsonTask.getTempId(), gsonTask.getParentLocalId(), gsonTask.getLocalCreatedAt(), gsonTask.getLocalUpdatedAt(), gsonTask.isDeleted(), gsonTask.getTitle(), gsonTask.getNotes(), gsonTask.getOrder(), gsonTask.getPriority(), gsonTask.getLocalCompletionDate(), gsonTask.getLocalSchedule(), gsonTask.getLocation(), gsonTask.getLocalRepeatDate(), gsonTask.getRepeatOption(), gsonTask.getOrigin(), gsonTask.getOriginIdentifier()));
            }
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

        if (gsonTags != null) {
            for (GsonTag gsonTag : gsonTags) {
                tags.add(new Tag(gsonTag.getId(), gsonTag.getObjectId(), gsonTag.getTempId(), gsonTag.getLocalCreatedAt(), gsonTag.getLocalUpdatedAt(), gsonTag.getTitle()));
            }
        }

        return tags;
    }

}
