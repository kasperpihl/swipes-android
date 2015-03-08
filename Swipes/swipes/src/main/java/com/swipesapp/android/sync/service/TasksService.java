package com.swipesapp.android.sync.service;

import android.content.Context;
import android.content.Intent;

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
import com.swipesapp.android.values.Constants;
import com.swipesapp.android.values.Sections;
import com.swipesapp.android.values.Services;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
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
    private ExtAttachmentDao mExtAttachmentDao;

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
        mExtAttachmentDao = ExtAttachmentDao.getInstance(daoSession);
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
     * @param task Object holding task data.
     * @param sync True to queue changes and perform sync.
     */
    public void saveTask(Task task, boolean sync) {
        Long id = task.getId();
        String parentId = task.getParentLocalId();

        if (sync) SyncService.getInstance().saveTaskChangesForSync(task, null);

        if (id == null) {
            createTask(task);
        } else {
            updateTask(task);
        }

        if (parentId != null) {
            updateParent(task, sync);
        }

        if (sync) SyncService.getInstance().performSync(true, Constants.SYNC_DELAY);
    }

    /**
     * Updates the parent of a given subtask.
     *
     * @param subtask Subtask to update parent.
     * @param sync    True to queue changes for sync.
     */
    private void updateParent(Task subtask, boolean sync) {
        Task parent = loadTask(subtask.getParentLocalId());

        if (parent != null) {
            parent.setUpdatedAt(subtask.getUpdatedAt());

            if (sync) SyncService.getInstance().saveTaskChangesForSync(parent, null);

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
    public void deleteTasks(List<Task> tasks) {
        for (Task task : tasks) {
            // Mark task as deleted and persist change.
            task.setDeleted(true);
            saveTask(task, true);

            // Delete subtasks.
            deleteSubtasksForTask(task.getTempId());

            SyncService.getInstance().performSync(true, Constants.SYNC_DELAY);
        }
    }

    /**
     * Creates a new task.
     *
     * @param task Object holding new task data.
     */
    private void createTask(Task task) {
        synchronized (this) {
            Long taskId = mExtTaskDao.getDao().insert(task);
            saveTags(taskId, task.getTags());
            saveAttachments(taskId, task.getAttachments());
        }
    }

    /**
     * Updates an existing task.
     *
     * @param task Object holding updated data.
     */
    private void updateTask(Task task) {
        // Set new update date.
        task.setUpdatedAt(new Date());

        synchronized (this) {
            mExtTaskDao.getDao().update(task);
            saveTags(task.getId(), task.getTags());
            saveAttachments(task.getId(), task.getAttachments());
        }
    }

    /**
     * Associates existing tags with a task.
     *
     * @param taskId ID of the task.
     * @param tags   List of objects holding tag data.
     */
    private void saveTags(Long taskId, List<Tag> tags) {
        if (tags != null) {
            for (Tag tag : tags) {
                // Load tag based on temp ID or object ID.
                String tempId = tag.getTempId() != null ? tag.getTempId() : tag.getObjectId();
                Tag localTag = loadTag(tempId);

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
    private void saveAttachments(Long taskId, List<Attachment> currentAttachments) {
        // Load existing attachments.
        List<Attachment> previousAttachments = loadTask(taskId).getAttachments();
        // List to keep track of updated objects.
        List<Attachment> updatedAttachments = new ArrayList<>();

        if (previousAttachments != null) {
            for (Attachment previous : previousAttachments) {
                Long id = previous.getId();
                String identifier = previous.getIdentifier();
                boolean hasMatch = false;

                // Match existing with updated.
                if (currentAttachments != null) {
                    for (Attachment current : currentAttachments) {
                        if (id.equals(current.getId()) || identifier.equals(current.getIdentifier())) {
                            // Update attachment.
                            previous.setIdentifier(current.getIdentifier());
                            previous.setService(current.getService());
                            previous.setTitle(current.getTitle());
                            previous.setSync(current.getSync());

                            // Mark as matched.
                            hasMatch = true;

                            // Add to the list of updated.
                            updatedAttachments.add(current);
                        }
                    }
                }

                // Check if match was found and persist changes.
                if (hasMatch) {
                    saveAttachment(previous, taskId);
                } else {
                    // No matches, so the attachment was removed.
                    deleteAttachment(previous.getId());
                }
            }
        }

        // Persist new attachments.
        if (currentAttachments != null) {
            currentAttachments.removeAll(updatedAttachments);

            for (Attachment newAttachment : currentAttachments) {
                saveAttachment(newAttachment, taskId);
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

            SyncService.getInstance().saveTagForSync(loadTag(tempId));
        }
    }

    /**
     * Saves an existing tag.
     *
     * @param tag Tag to save.
     */
    public void saveTag(Tag tag) {
        if (tag.getTitle() != null && !tag.getTitle().isEmpty()) {
            // Persist local tag.
            mExtTagDao.getDao().insert(tag);
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
        if (assignment != null) mExtTaskTagDao.getDao().delete(assignment);

        SyncService.getInstance().saveTaskChangesForSync(loadTask(taskId), null);
    }

    /**
     * Deletes a tag from the database and unassigns it from all tasks.
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

        SyncService.getInstance().saveDeletedTagForSync(tag);
    }

    /**
     * Creates a new attachment, or updates an existing one.
     *
     * @param attachment Object holding attachment data.
     */
    public void saveAttachment(Attachment attachment, long taskId) {
        Long id = attachment.getId();

        if (id == null) {
            createAttachment(attachment, taskId);
        } else {
            updateAttachment(attachment, taskId);
        }
    }

    /**
     * Creates a new attachment.
     *
     * @param attachment Object holding new attachment data.
     * @param taskId     Database ID of the task to associate with.
     */
    private void createAttachment(Attachment attachment, long taskId) {
        synchronized (this) {
            attachment.setTaskId(taskId);
            mExtAttachmentDao.getDao().insert(attachment);
        }
    }

    /**
     * Updates an existing attachment.
     *
     * @param attachment Object holding updated data.
     */
    private void updateAttachment(Attachment attachment, long taskId) {
        synchronized (this) {
            attachment.setTaskId(taskId);
            mExtAttachmentDao.getDao().update(attachment);
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
                Task old = loadTask(attachment.getTaskId());

                // Delete from database.
                mExtAttachmentDao.getDao().delete(attachment);

                // Update task with removed attachment.
                Task updated = loadTask(old.getId());
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
    public Task loadTask(Long id) {
        Task task = mExtTaskDao.selectTask(id);

        if (task != null) {
            task.setTags(loadTagsForTask(id));
            task.setAttachments(loadAttachmentsForTask(id));
        }

        return task;
    }

    /**
     * Loads a single task.
     *
     * @param tempId Temp ID of the task.
     * @return Selected task.
     */
    public Task loadTask(String tempId) {
        Task task = mExtTaskDao.selectTask(tempId);

        if (task != null) {
            task.setTags(loadTagsForTask(task.getId()));
            task.setAttachments(loadAttachmentsForTask(task.getId()));
        }

        return task;
    }

    /**
     * Loads all existing tasks.
     *
     * @return List of tasks.
     */
    public List<Task> loadAllTasks() {
        List<Task> tasks = mExtTaskDao.listAllTasks();

        if (tasks != null) {
            for (Task task : tasks) {
                task.setTags(loadTagsForTask(task.getId()));
                task.setAttachments(loadAttachmentsForTask(task.getId()));
            }
        }

        return tasks != null ? tasks : new ArrayList<Task>();
    }

    /**
     * Loads scheduled tasks.
     *
     * @return List of tasks.
     */
    public List<Task> loadScheduledTasks() {
        List<Task> tasks = mExtTaskDao.listScheduledTasks();

        if (tasks != null) {
            for (Task task : tasks) {
                task.setTags(loadTagsForTask(task.getId()));
                task.setAttachments(loadAttachmentsForTask(task.getId()));
            }
        }

        return tasks != null ? tasks : new ArrayList<Task>();
    }

    /**
     * Loads focused tasks.
     *
     * @return List of tasks.
     */
    public List<Task> loadFocusedTasks() {
        List<Task> tasks = mExtTaskDao.listFocusedTasks();

        if (tasks != null) {
            for (Task task : tasks) {
                task.setTags(loadTagsForTask(task.getId()));
                task.setAttachments(loadAttachmentsForTask(task.getId()));
            }
        }

        return tasks != null ? tasks : new ArrayList<Task>();
    }

    /**
     * Loads completed tasks.
     *
     * @return List of tasks.
     */
    public List<Task> loadCompletedTasks() {
        List<Task> tasks = mExtTaskDao.listCompletedTasks();

        if (tasks != null) {
            for (Task task : tasks) {
                task.setTags(loadTagsForTask(task.getId()));
                task.setAttachments(loadAttachmentsForTask(task.getId()));
            }
        }

        return tasks != null ? tasks : new ArrayList<Task>();
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
     * Searches tasks for the given query and section.
     *
     * @param query   Query to search for.
     * @param section Section to search from.
     * @return List of tasks.
     */
    public List<Task> searchTasks(String query, Sections section) {
        List<Task> tasks = new ArrayList<>();
        List<Task> results = new ArrayList<>();

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

        for (Task task : tasks) {
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
    private boolean tagsContainQuery(String query, Task task) {
        for (Tag tag : task.getTags()) {
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
    public Tag loadTag(Long id) {
        return mExtTagDao.selectTag(id);
    }

    /**
     * Loads a single tag.
     *
     * @param tempId Temp ID of the tag.
     * @return Selected tag.
     */
    public Tag loadTag(String tempId) {
        return mExtTagDao.selectTag(tempId);
    }

    /**
     * Load all existing tags.
     *
     * @return List of tags.
     */
    public List<Tag> loadAllTags() {
        List<Tag> tags = mExtTagDao.listAllTags();
        return tags != null ? tags : new ArrayList<Tag>();
    }

    /**
     * Load all tags assigned to at least one task.
     *
     * @return List of tags.
     */
    public List<Tag> loadAllAssignedTags() {
        List<Tag> tags = loadAllTags();
        List<Tag> assigned = new ArrayList<Tag>();

        for (Tag tag : tags) {
            List<TaskTag> associations = mExtTaskTagDao.selectAssociationsForTag(tag.getId());

            for (TaskTag association : associations) {
                Task task = loadTask(association.getTaskId());

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
    public List<Task> loadTasksForTag(Long tagId, Sections section) {
        List<TaskTag> associations = mExtTaskTagDao.selectAssociationsForTag(tagId);
        List<Task> tasks = new ArrayList<>();

        for (TaskTag association : associations) {
            Task task = loadTask(association.getTaskId());
            boolean isFromSection = false;

            switch (section) {
                case LATER:
                    isFromSection = (task.getSchedule() == null || task.getSchedule().after(new Date())) && task.getCompletionDate() == null;
                    break;
                case FOCUS:
                    isFromSection = (task.getSchedule() == null || task.getSchedule().before(new Date())) && task.getCompletionDate() == null;
                    break;
                case DONE:
                    isFromSection = task.getCompletionDate() != null;
                    break;
            }

            if (isFromSection && !task.isDeleted()) tasks.add(task);
        }

        return tasks;
    }

    /**
     * Loads all tasks with attachment from the Evernote service.
     *
     * @param attachmentSync Attachments are filtered by this value for their "sync" property.
     * @return List of tasks.
     */
    public List<Task> loadTasksWithEvernote(boolean attachmentSync) {
        // Load Evernote attachments.
        List<Attachment> attachments = mExtAttachmentDao.listAttachmentsForService(Services.EVERNOTE);

        // Holds matching tasks.
        List<Task> matches = new ArrayList<>();

        if (attachments != null) {
            for (Attachment attachment : attachments) {
                // Check for sync property.
                if (attachment.getSync() == attachmentSync) {
                    // Add associated task to the list of matches.
                    Task match = loadTask(attachment.getTaskId());
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
                    Task matchingTask = loadTask(attachment.getTaskId());
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
    private List<Tag> loadTagsForTask(Long taskId) {
        List<TaskTag> associations = mExtTaskTagDao.selectAssociationsForTask(taskId);
        List<Tag> tags = new ArrayList<>();

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
    public List<Task> loadSubtasksForTask(String objectId) {
        List<Task> subtasks = mExtTaskDao.listSubtasksForTask(objectId);
        return subtasks != null ? subtasks : new ArrayList<Task>();
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
        List<Task> subtasks = loadSubtasksForTask(objectId);

        // Mark subtasks as deleted and persist changes.
        for (Task subtask : subtasks) {
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
    public List<Attachment> loadAttachmentsForTask(Long taskId) {
        List<Attachment> attachments = mExtAttachmentDao.listAttachmentsForTask(taskId);
        return attachments != null ? attachments : new ArrayList<Attachment>();
    }

    /**
     * Loads a single attachment.
     *
     * @param id Database ID of the attachment.
     * @return Selected attachment.
     */
    public Attachment loadAttachment(Long id) {
        return mExtAttachmentDao.selectAttachment(id);
    }

    /**
     * Loads a single attachment.
     *
     * @param identifier Identifier of the attachment.
     * @return Selected attachment.
     */
    public Attachment loadAttachment(String identifier) {
        return mExtAttachmentDao.selectAttachment(identifier);
    }

    /**
     * Loads all existing attachments.
     *
     * @return List of attachments.
     */
    public List<Attachment> loadAllAttachments() {
        List<Attachment> attachments = mExtAttachmentDao.listAllAttachments();
        return attachments != null ? attachments : new ArrayList<Attachment>();
    }

    /**
     * Loads all attachments for a given service.
     *
     * @param service Service to search for (e.g. Services.EVERNOTE).
     * @return List of attachments.
     */
    public List<Attachment> loadAttachmentsForService(String service) {
        List<Attachment> attachments = mExtAttachmentDao.listAttachmentsForService(service);
        return attachments != null ? attachments : new ArrayList<Attachment>();
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

        // Delete all attachments.
        List<Attachment> attachments = mExtAttachmentDao.listAllAttachments();
        for (Attachment attachment : attachments) {
            mExtAttachmentDao.getDao().delete(attachment);
        }
    }

}
