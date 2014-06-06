package com.swipesapp.android.db;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import de.greenrobot.dao.internal.DaoConfig;

/**
 * Extended DAO for tasks, allowing custom DB operations.
 *
 * @author Felipe Bari
 */
public class ExtTaskDao extends TaskDao {

    private static ExtTaskDao sInstance;

    // Custom constructor to comply with TaskDao.
    private ExtTaskDao(DaoConfig config) {
        super(config);
    }

    public static ExtTaskDao getInstance(DaoSession daoSession) {
        if (sInstance == null) {
            sInstance = (ExtTaskDao) daoSession.getTaskDao();
        }
        return sInstance;
    }

    public Task selectTask(String objectId) {
        return queryBuilder().where(Properties.ObjectId.eq(objectId)).unique();
    }

    public List<Task> listAllTasks() {
        return queryBuilder().list();
    }

    public List<Task> listScheduledTasks() {
        return queryBuilder().where(Properties.Schedule.gt(new Date())).orderAsc(Properties.Schedule).list();
    }

    public List<Task> listFocusedTasks() {
        return queryBuilder().where(Properties.Schedule.lt(new Date()), Properties.CompletionDate.isNull()).orderAsc(Properties.Order).list();
    }

    public List<Task> listCompletedTasks() {
        return queryBuilder().where(Properties.CompletionDate.isNotNull()).orderDesc(Properties.CompletionDate).list();
    }

    public List<Tag> listTagsForTask(Long taskId) {
        // TODO: Build a raw query to retrieve all tags for a specific task, using the join table.
        return new ArrayList<Tag>();
    }

}
