package com.swipesapp.android.sync.service;

import android.content.Context;
import android.content.Intent;

import com.swipesapp.android.analytics.handler.Analytics;
import com.swipesapp.android.app.SwipesApplication;
import com.swipesapp.android.db.Attachment;
import com.swipesapp.android.db.DaoSession;
import com.swipesapp.android.db.Tag;
import com.swipesapp.android.db.Task;
import com.swipesapp.android.db.TaskTag;
import com.swipesapp.android.db.dao.ExtAttachmentDao;
import com.swipesapp.android.db.dao.ExtTagDao;
import com.swipesapp.android.db.dao.ExtTaskDao;
import com.swipesapp.android.db.dao.ExtTaskTagDao;
import com.swipesapp.android.sync.gson.GsonAttachment;
import com.swipesapp.android.sync.gson.GsonTag;
import com.swipesapp.android.sync.gson.GsonTask;
import com.swipesapp.android.sync.receiver.NotificationsHelper;
import com.swipesapp.android.util.ListUtils;
import com.swipesapp.android.values.Constants;
import com.swipesapp.android.values.Sections;
import com.swipesapp.android.values.Services;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * Service for task operations.
 *
 * @author Fernanda Bari
 */
public class TasksService {

    private static TasksService sInstance;

    private DaoSession mDaoSession;

    private ExtTaskDao mExtTaskDao;
    private ExtTagDao mExtTagDao;
    private ExtTaskTagDao mExtTaskTagDao;
    private ExtAttachmentDao mExtAttachmentDao;

    private WeakReference<Context> mContext;

    /**
     * Internal constructor. Handles loading of extended DAOs for custom DB operations.
     *
     * @param context Context reference.
     */
    private TasksService(Context context) {
        mContext = new WeakReference<Context>(context);

        mDaoSession = SwipesApplication.getDaoSession();

        mExtTaskDao = ExtTaskDao.getInstance(mDaoSession);
        mExtTagDao = ExtTagDao.getInstance(mDaoSession);
        mExtTaskTagDao = ExtTaskTagDao.getInstance(mDaoSession);
        mExtAttachmentDao = ExtAttachmentDao.getInstance(mDaoSession);
    }

    /**
     * Returns a new instance of the service. Call once during the application's
     * lifecycle to ensure only one DAO session is active at any given time.
     *
     * @param context Application context.
     */
    public static TasksService newInstance(Context context) {
        sInstance = new TasksService(context);
        return sInstance;
    }

    /**
     * Returns an existing instance of the service. Make sure you have called
     * {@link #newInstance(android.content.Context)} at least once before.
     */
    public static TasksService getInstance() {
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

        if (mContext.get() != null) {
            mContext.get().sendBroadcast(intent);
        }
    }

    /**
     * Creates a new task, or updates an existing one.
     *
     * @param gsonTask Object holding task data.
     * @param sync     True to queue changes and perform sync.
     */
    public void saveTask(GsonTask gsonTask, boolean sync) {
        Long id = gsonTask.getId();
        String parentId = gsonTask.getParentLocalId();

        if (sync) {
            NotificationsHelper.handleNextAlarm(mContext.get(), gsonTask);

            SyncService.getInstance().saveTaskChangesForSync(gsonTask, null);
        }

        if (id == null) {
            createTask(gsonTask);
        } else {
            Task task = mExtTaskDao.selectTask(id);
            updateTask(gsonTask, task);
        }

        if (parentId != null) {
            updateParent(gsonTask, sync);
        }

        if (sync) SyncService.getInstance().performSync(true, Constants.SYNC_DELAY);
    }

    /**
     * Creates or updates multiple tasks faster in a transaction. Use this for bulk
     * operations (e.g. sync, migrations, etc.).
     *
     * @param gsonTasks List of objects holding task data.
     * @param sync      True to queue changes and perform sync.
     */
    public void saveTasks(final List<GsonTask> gsonTasks, final boolean sync) {
        mDaoSession.runInTx(new Runnable() {
            @Override
            public void run() {
                for (GsonTask gsonTask : gsonTasks) {
                    // Persist task in transaction.
                    saveTask(gsonTask, sync);
                }
            }
        });
    }

    /**
     * Updates the parent of a given subtask.
     *
     * @param subtask Subtask to update parent.
     * @param sync    True to queue changes for sync.
     */
    private void updateParent(GsonTask subtask, boolean sync) {
        GsonTask gsonParent = loadTask(subtask.getParentLocalId());

        if (gsonParent != null) {
            gsonParent.setLocalUpdatedAt(subtask.getLocalUpdatedAt());

            if (sync) SyncService.getInstance().saveTaskChangesForSync(gsonParent, null);

            Task parent = tasksFromGson(Arrays.asList(gsonParent)).get(0);

            synchronized (this) {
                mExtTaskDao.getDao().update(parent);
            }
        }
    }

    /**
     * Deletes tasks.
     *
     * @param tasks List containing tasks to delete.
     */
    public void deleteTasks(final List<GsonTask> tasks) {
        mDaoSession.runInTx(new Runnable() {
            @Override
            public void run() {
                for (GsonTask task : tasks) {
                    // Mark task as deleted and persist change.
                    task.setDeleted(true);
                    saveTask(task, true);

                    // Delete subtasks.
                    deleteSubtasksForTask(task.getTempId());

                    SyncService.getInstance().performSync(true, Constants.SYNC_DELAY);
                }
            }
        });
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
            saveAttachments(taskId, gsonTask.getAttachments());
        }
    }

    /**
     * Updates an existing task.
     *
     * @param gsonTask Object holding updated data.
     * @param task     Task to update.
     */
    private void updateTask(GsonTask gsonTask, Task task) {
        // Set new update date.
        gsonTask.setLocalUpdatedAt(new Date());

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
        task.setOrigin(gsonTask.getOrigin());
        task.setOriginIdentifier(gsonTask.getOriginIdentifier());

        synchronized (this) {
            mExtTaskDao.getDao().update(task);
            saveTags(task.getId(), gsonTask.getTags());
            saveAttachments(task.getId(), gsonTask.getAttachments());
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
            // Filter tags no longer assigned.
            GsonTask oldTask = loadTask(taskId);
            if (oldTask != null) {
                List<GsonTag> oldTags = oldTask.getTags();
                oldTags.removeAll(gsonTags);

                // Remove tags not assigned.
                for (GsonTag tag : oldTags) {
                    unassignTag(tag.getId(), oldTask.getId());
                }
            }

            // Process new or existing tags.
            for (GsonTag tag : gsonTags) {
                // Load tag based on temp ID or object ID.
                String tempId = tag.getTempId() != null ? tag.getTempId() : tag.getObjectId();
                GsonTag localTag = loadTag(tempId);

                // Make sure tag already exists locally.
                if (localTag != null) {
                    // Search for association between task and tag.
                    TaskTag association = mExtTaskTagDao.selectAssociation(taskId, localTag.getId());

                    // If an association already exists, do nothing.
                    if (association == null) {
                        // Create association.
                        association = new TaskTag(null, taskId, localTag.getId());
                        mExtTaskTagDao.getDao().insert(association);
                    }
                }
            }
        }
    }

    /**
     * Saves or updates attachments to a task.
     *
     * @param taskId             Task to associate with.
     * @param currentAttachments List of attachments.
     */
    private void saveAttachments(Long taskId, List<GsonAttachment> currentAttachments) {
        // Load existing attachments.
        List<GsonAttachment> previousAttachments = loadTask(taskId).getAttachments();
        // List to keep track of updated objects.
        List<GsonAttachment> updatedAttachments = new ArrayList<>();

        if (previousAttachments != null) {
            for (GsonAttachment previous : previousAttachments) {
                Long id = previous.getId();
                String identifier = previous.getIdentifier();
                boolean hasMatch = false;
                boolean hasChanged = false;

                // Match existing with updated.
                if (currentAttachments != null) {
                    for (GsonAttachment current : currentAttachments) {
                        if (id.equals(current.getId()) || identifier.equals(current.getIdentifier())) {
                            if (current.hasChanged(previous)) {
                                // Update attributes.
                                previous.setIdentifier(current.getIdentifier());
                                previous.setService(current.getService());
                                previous.setTitle(current.getTitle());
                                previous.setSync(current.getSync());

                                // Mark as changed.
                                hasChanged = true;
                            }

                            // Mark as matched.
                            hasMatch = true;

                            // Add to the list of updated.
                            updatedAttachments.add(current);
                        }
                    }
                }

                // Check if match was found.
                if (hasMatch) {
                    // Persist changes if needed.
                    if (hasChanged) saveAttachment(previous, taskId);
                } else {
                    // No matches, so the attachment was removed.
                    deleteAttachment(previous.getId());
                }
            }
        }

        // Persist new attachments.
        if (currentAttachments != null) {
            List<GsonAttachment> newAttachments = new ArrayList<>(currentAttachments);
            newAttachments.removeAll(updatedAttachments);

            for (GsonAttachment newAttachment : newAttachments) {
                saveAttachment(newAttachment, taskId);
            }
        }
    }

    /**
     * Creates a new tag.
     *
     * @param title Tag title.
     */
    public long createTag(String title) {
        long id = 0;

        if (title != null && !title.isEmpty()) {
            Date currentDate = new Date();
            String tempId = title + currentDate.getTime();

            Tag tag = new Tag(null, null, tempId, currentDate, currentDate, title);

            // Persist new tag.
            id = mExtTagDao.getDao().insert(tag);

            SyncService.getInstance().saveTagForSync(loadTag(tempId));

            // Update number of tags dimension.
            Analytics.sendNumberOfTags(mContext.get());
        }

        return id;
    }

    /**
     * Updates an existing tag.
     *
     * @param tag  Tag to update.
     * @param sync True to queue changes for sync.
     */
    public void editTag(GsonTag tag, boolean sync) {
        if (tag.getTempId() != null && tag.getTitle() != null && !tag.getTitle().isEmpty()) {
            Tag localTag = mExtTagDao.selectTag(tag.getTempId());

            // Update attributes.
            localTag.setObjectId(tag.getObjectId());
            localTag.setTempId(tag.getTempId());
            localTag.setCreatedAt(tag.getLocalCreatedAt());
            localTag.setUpdatedAt(tag.getLocalUpdatedAt());
            localTag.setTitle(tag.getTitle());

            // Persist updated tag.
            mExtTagDao.getDao().update(localTag);

            // Save changes for sync.
            if (sync) SyncService.getInstance().saveTagForSync(tag);
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
     * Saves multiple existing tags. Use this for bulk operations (e.g. sync).
     *
     * @param tags List of tags to save.
     */
    public void saveTags(final List<GsonTag> tags) {
        mDaoSession.runInTx(new Runnable() {
            @Override
            public void run() {
                for (GsonTag tag : tags) {
                    // Persist tag in transaction.
                    saveTag(tag);
                }
            }
        });
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
        if (assignment != null) mExtTaskTagDao.getDao().delete(assignment);

        SyncService.getInstance().saveTaskChangesForSync(loadTask(taskId), null);
    }

    /**
     * Unassigns a tag from multiple tasks.
     *
     * @param tagId ID of the tag to unassign.
     * @param tasks List of tasks to unassign from.
     */
    public void unassignTag(final Long tagId, final List<GsonTask> tasks) {
        mDaoSession.runInTx(new Runnable() {
            @Override
            public void run() {
                // Unassign from tasks in transaction.
                for (GsonTask task : tasks) {
                    unassignTag(tagId, task.getId());
                }
            }
        });
    }

    /**
     * Deletes a tag from the database and unassigns it from all tasks.
     *
     * @param tagId ID of the tag to delete.
     */
    public void deleteTag(Long tagId) {
        // Unassign from all tasks.
        List<TaskTag> assignments = mExtTaskTagDao.selectAssociationsForTag(tagId);
        if (assignments != null) {
            for (TaskTag assignment : assignments) {
                mExtTaskTagDao.getDao().delete(assignment);
            }
        }

        // Delete from database.
        Tag tag = mExtTagDao.selectTag(tagId);
        mExtTagDao.getDao().delete(tag);

        SyncService.getInstance().saveDeletedTagForSync(tag);

        // Update number of tags dimension.
        Analytics.sendNumberOfTags(mContext.get());
    }

    /**
     * Creates a new attachment, or updates an existing one.
     *
     * @param gsonAttachment Object holding attachment data.
     */
    public void saveAttachment(GsonAttachment gsonAttachment, long taskId) {
        Long id = gsonAttachment.getId();

        if (id == null) {
            createAttachment(gsonAttachment, taskId);
        } else {
            Attachment attachment = mExtAttachmentDao.selectAttachment(id);
            updateAttachment(gsonAttachment, attachment, taskId);
        }
    }

    /**
     * Creates a new attachment.
     *
     * @param gsonAttachment Object holding new attachment data.
     * @param taskId         Database ID of the task to associate with.
     */
    private void createAttachment(GsonAttachment gsonAttachment, long taskId) {
        Attachment attachment = attachmentsFromGson(Arrays.asList(gsonAttachment), taskId).get(0);

        synchronized (this) {
            mExtAttachmentDao.getDao().insert(attachment);
        }
    }

    /**
     * Updates an existing attachment.
     *
     * @param gsonAttachment Object holding updated data.
     * @param attachment     Attachment to update.
     */
    private void updateAttachment(GsonAttachment gsonAttachment, Attachment attachment, long taskId) {
        if (attachment != null) {
            // Update only mutable attributes.
            attachment.setIdentifier(gsonAttachment.getIdentifier());
            attachment.setService(gsonAttachment.getService());
            attachment.setTitle(gsonAttachment.getTitle());
            attachment.setSync(gsonAttachment.getSync());
            attachment.setTaskId(taskId);

            synchronized (this) {
                mExtAttachmentDao.getDao().update(attachment);
            }
        }
    }

    /**
     * Deletes an attachment from the database.
     *
     * @param id ID of the attachment to delete.
     */
    public void deleteAttachment(long id) {
        // Delete from database.
        Attachment attachment = mExtAttachmentDao.selectAttachment(id);
        if (attachment != null) mExtAttachmentDao.getDao().delete(attachment);
    }

    /**
     * Deletes all attachments for a given service.
     *
     * @param service Service to search for (e.g. Services.EVERNOTE).
     */
    public void deleteAttachmentsForService(String service) {
        // Load attachments.
        List<Attachment> attachments = mExtAttachmentDao.listAttachmentsForService(service);

        if (attachments != null) {
            for (Attachment attachment : attachments) {
                // Load task with attachment.
                GsonTask old = loadTask(attachment.getTaskId());

                // Delete from database.
                mExtAttachmentDao.getDao().delete(attachment);

                // Update task with removed attachment.
                GsonTask updated = loadTask(old.getId());
                SyncService.getInstance().saveTaskChangesForSync(updated, old);
            }
        }
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
     * Loads the most recent scheduled task.
     *
     * @return Selected task.
     */
    public GsonTask loadFirstScheduledTask() {
        List<GsonTask> scheduledTasks = loadScheduledTasks();
        return !scheduledTasks.isEmpty() ? scheduledTasks.get(0) : null;
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
     * Loads tasks with a schedule within the current minute.
     *
     * @return List of tasks.
     */
    public List<GsonTask> loadExpiringTasks() {
        return gsonFromTasks(mExtTaskDao.listExpiringTasks());
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
     * Counts all tasks.
     *
     * @return Total number of tasks. Deleted tasks are not considered.
     */
    public int countAllTasks() {
        return (int) mExtTaskDao.countAllTasks();
    }

    /**
     * Counts all uncompleted tasks for today.
     *
     * @return Number of uncompleted tasks for today.
     */
    public int countTasksForToday() {
        return (int) mExtTaskDao.countTasksForToday();
    }

    /**
     * Counts all tasks completed today.
     *
     * @return Number of tasks completed today.
     */
    public int countTasksCompletedToday() {
        return (int) mExtTaskDao.countCompletedTasksToday();
    }

    /**
     * Counts all tasks for now.
     *
     * @return Number of tasks for now.
     */
    public int countTasksForNow() {
        return (int) mExtTaskDao.countTasksForNow();
    }

    /**
     * Counts all tasks for a given day.
     *
     * @return Number of tasks for given day.
     */
    public int countTasksForDay(Date day) {
        return (int) mExtTaskDao.countTasksForDay(day);
    }

    /**
     * Counts all recurring tasks.
     *
     * @return Number of recurring tasks. Deleted tasks are not considered.
     */
    public int countRecurringTasks() {
        return (int) mExtTaskDao.countRecurringTasks();
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
     * Counts all tags.
     *
     * @return Total number of tags.
     */
    public int countAllTags() {
        return (int) mExtTagDao.countAllTags();
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
                    isFromSection = (task.getLocalSchedule() == null || task.getLocalSchedule().after(new Date())) && task.getLocalCompletionDate() == null;
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

        switch (section) {
            case LATER:
                ListUtils.sortScheduledTasks(tasks);
                break;
            case FOCUS:
                ListUtils.sortFocusedTasks(tasks);
                break;
            case DONE:
                ListUtils.sortCompletedTasks(tasks);
                break;
        }

        return tasks;
    }

    /**
     * Loads all tasks with attachment from the Evernote service.
     *
     * @param attachmentSync Attachments are filtered by this value for their "sync" property.
     * @return List of tasks.
     */
    public List<GsonTask> loadTasksWithEvernote(boolean attachmentSync) {
        // Load Evernote attachments.
        List<Attachment> attachments = mExtAttachmentDao.listAttachmentsForService(Services.EVERNOTE);

        // Holds matching tasks.
        List<GsonTask> matches = new ArrayList<GsonTask>();

        if (attachments != null) {
            for (Attachment attachment : attachments) {
                // Check for sync property.
                if (attachment.getSync() == attachmentSync) {
                    // Add associated task to the list of matches.
                    GsonTask match = loadTask(attachment.getTaskId());
                    if (!match.getDeleted())
                        matches.add(match);
                }
            }
        }

        return matches;
    }

    /**
     * Loads all attachment identifiers for the Evernote service.
     *
     * @param attachmentSync Attachments are filtered by this value for their "sync" property.
     * @return List of identifiers for all evernote attachments.
     */
    public List<String> loadIdentifiersWithEvernote(boolean attachmentSync) {
        // Load Evernote attachments.
        List<Attachment> attachments = mExtAttachmentDao.listAttachmentsForService(Services.EVERNOTE);

        // Holds matching tasks.
        List<String> matches = new ArrayList<String>();

        if (attachments != null) {
            for (Attachment attachment : attachments) {
                // Check for sync property.
                if (attachment.getSync() == attachmentSync) {
                    // Check if the task for attachment is not deleted.
                    GsonTask matchingTask = loadTask(attachment.getTaskId());
                    if (!matchingTask.getDeleted())
                        matches.add(attachment.getIdentifier());
                }
            }
        }

        return matches;
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

    /**
     * Counts uncompleted subtasks for a given task.
     *
     * @param objectId Parent object ID.
     * @return Number of uncompleted subtasks.
     */
    public int countUncompletedSubtasksForTask(String objectId) {
        return (int) mExtTaskDao.countUncompletedSubtasksForTask(objectId);
    }

    /**
     * Deletes all subtasks for a given task.
     *
     * @param objectId Parent object ID.
     */
    private void deleteSubtasksForTask(String objectId) {
        List<GsonTask> subtasks = loadSubtasksForTask(objectId);

        // Mark subtasks as deleted and persist changes.
        for (GsonTask subtask : subtasks) {
            subtask.setDeleted(true);
            saveTask(subtask, true);
        }
    }

    /**
     * Loads all attachments for a given task.
     *
     * @param taskId ID of the task.
     * @return List of attachments.
     */
    public List<GsonAttachment> loadAttachmentsForTask(Long taskId) {
        return gsonFromAttachments(mExtAttachmentDao.listAttachmentsForTask(taskId));
    }

    /**
     * Loads a single attachment.
     *
     * @param id Database ID of the attachment.
     * @return Selected attachment.
     */
    public GsonAttachment loadAttachment(Long id) {
        Attachment attachment = mExtAttachmentDao.selectAttachment(id);
        return attachment != null ? gsonFromAttachments(Arrays.asList(attachment)).get(0) : null;
    }

    /**
     * Loads a single attachment.
     *
     * @param identifier Identifier of the attachment.
     * @return Selected attachment.
     */
    public GsonAttachment loadAttachment(String identifier) {
        Attachment attachment = mExtAttachmentDao.selectAttachment(identifier);
        return attachment != null ? gsonFromAttachments(Arrays.asList(attachment)).get(0) : null;
    }

    /**
     * Loads all existing attachments.
     *
     * @return List of attachments.
     */
    public List<GsonAttachment> loadAllAttachments() {
        return gsonFromAttachments(mExtAttachmentDao.listAllAttachments());
    }

    /**
     * Loads all attachments for a given service.
     *
     * @param service Service to search for (e.g. Services.EVERNOTE).
     * @return List of attachments.
     */
    public List<GsonAttachment> loadAttachmentsForService(String service) {
        return gsonFromAttachments(mExtAttachmentDao.listAttachmentsForService(service));
    }

    /**
     * Clears all user data from the database.
     */
    public void clearAllData() {
        mDaoSession.runInTx(new Runnable() {
            @Override
            public void run() {
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

                // Delete all attachments.
                List<Attachment> attachments = mExtAttachmentDao.listAllAttachments();
                for (Attachment attachment : attachments) {
                    mExtAttachmentDao.getDao().delete(attachment);
                }
            }
        });
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
                GsonTask gsonTask = GsonTask.gsonForLocal(task.getId(), task.getObjectId(), task.getTempId(), task.getParentLocalId(),
                        task.getCreatedAt(), task.getUpdatedAt(), task.getDeleted(), task.getTitle(), task.getNotes(), task.getOrder(),
                        task.getPriority(), task.getCompletionDate(), task.getSchedule(), task.getLocation(), task.getRepeatDate(),
                        task.getRepeatOption(), task.getOrigin(), task.getOriginIdentifier(), loadTagsForTask(task.getId()),
                        loadAttachmentsForTask(task.getId()), task.getId());

                gsonTask.setSubtasksCount(countUncompletedSubtasksForTask(task.getTempId()));
                gsonTasks.add(gsonTask);
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
                gsonTags.add(GsonTag.gsonForLocal(tag.getId(), tag.getObjectId(), tag.getTempId(), tag.getCreatedAt(),
                        tag.getUpdatedAt(), tag.getTitle()));
            }
        }

        return gsonTags;
    }

    /**
     * Converts a list of Attachment objects to GsonAttachment.
     *
     * @param attachments List of attachments.
     * @return Converted list.
     */
    private List<GsonAttachment> gsonFromAttachments(List<Attachment> attachments) {
        List<GsonAttachment> gsonAttachments = new ArrayList<GsonAttachment>();

        if (attachments != null) {
            for (Attachment attachment : attachments) {
                gsonAttachments.add(new GsonAttachment(attachment.getId(), attachment.getIdentifier(), attachment.getService(),
                        attachment.getTitle(), attachment.getSync()));
            }
        }

        return gsonAttachments;
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
                tasks.add(new Task(gsonTask.getId(), gsonTask.getObjectId(), gsonTask.getTempId(), gsonTask.getParentLocalId(),
                        gsonTask.getLocalCreatedAt(), gsonTask.getLocalUpdatedAt(), gsonTask.isDeleted(), gsonTask.getTitle(),
                        gsonTask.getNotes(), gsonTask.getOrder(), gsonTask.getPriority(), gsonTask.getLocalCompletionDate(),
                        gsonTask.getLocalSchedule(), gsonTask.getLocation(), gsonTask.getLocalRepeatDate(), gsonTask.getRepeatOption(),
                        gsonTask.getOrigin(), gsonTask.getOriginIdentifier()));
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
                tags.add(new Tag(gsonTag.getId(), gsonTag.getObjectId(), gsonTag.getTempId(), gsonTag.getLocalCreatedAt(),
                        gsonTag.getLocalUpdatedAt(), gsonTag.getTitle()));
            }
        }

        return tags;
    }

    /**
     * Converts a list of GsonAttachment to Attachment objects.
     *
     * @param gsonAttachments List of GsonAttachment.
     * @return Converted list.
     */
    private List<Attachment> attachmentsFromGson(List<GsonAttachment> gsonAttachments, long taskId) {
        List<Attachment> attachments = new ArrayList<Attachment>();

        if (gsonAttachments != null) {
            for (GsonAttachment gsonAttachment : gsonAttachments) {
                attachments.add(new Attachment(gsonAttachment.getId(), gsonAttachment.getIdentifier(), gsonAttachment.getService(),
                        gsonAttachment.getTitle(), gsonAttachment.getSync(), taskId));
            }
        }

        return attachments;
    }

}
