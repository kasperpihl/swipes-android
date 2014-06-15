package com.swipesapp.android.db;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Extended DAO for tasks, allowing custom DB operations.
 *
 * @author Felipe Bari
 */
public class ExtTaskDao {

    private static ExtTaskDao sInstance;
    private TaskDao mDao;

    private ExtTaskDao(DaoSession daoSession) {
        mDao = daoSession.getTaskDao();
    }

    public static ExtTaskDao getInstance(DaoSession daoSession) {
        if (sInstance == null) {
            sInstance = new ExtTaskDao(daoSession);
        }
        return sInstance;
    }

    public TaskDao getDao() {
        return mDao;
    }

    public Task selectTask(String objectId) {
        return mDao.queryBuilder().where(TaskDao.Properties.ObjectId.eq(objectId)).unique();
    }

    public List<Task> listAllTasks() {
        return mDao.queryBuilder().list();
    }

    public List<Task> listScheduledTasks() {
        return mDao.queryBuilder().where(TaskDao.Properties.Schedule.gt(new Date())).orderAsc(TaskDao.Properties.Schedule).list();
    }

    public List<Task> listFocusedTasks() {
        return mDao.queryBuilder().where(mDao.queryBuilder().or(TaskDao.Properties.Schedule.lt(new Date()), TaskDao.Properties.Schedule.isNull()),
                TaskDao.Properties.CompletionDate.isNull()).orderAsc(TaskDao.Properties.Order).list();
    }

    public List<Task> listCompletedTasks() {
        return mDao.queryBuilder().where(TaskDao.Properties.CompletionDate.isNotNull()).orderDesc(TaskDao.Properties.CompletionDate).list();
    }

    public List<Tag> listTagsForTask(Long taskId) {
        // TODO: Build a raw query to retrieve all tags for a specific task, using the join table.
        return new ArrayList<Tag>();
    }

}
