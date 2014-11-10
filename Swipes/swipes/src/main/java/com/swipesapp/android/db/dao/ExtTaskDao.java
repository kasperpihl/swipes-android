package com.swipesapp.android.db.dao;

import com.swipesapp.android.db.DaoSession;
import com.swipesapp.android.db.Task;
import com.swipesapp.android.db.TaskDao;

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

    public Task selectTask(Long id) {
        return mDao.queryBuilder().where(TaskDao.Properties.Id.eq(id)).unique();
    }

    public Task selectTask(String tempId) {
        return mDao.queryBuilder().where(TaskDao.Properties.TempId.eq(tempId)).unique();
    }

    public List<Task> listAllTasks() {
        return mDao.queryBuilder().list();
    }

    public List<Task> listScheduledTasks() {
        return mDao.queryBuilder().where(TaskDao.Properties.Schedule.gt(new Date()), TaskDao.Properties.Deleted.eq(false), TaskDao.Properties.ParentLocalId.isNull()).orderAsc(TaskDao.Properties.Schedule).list();
    }

    public List<Task> listFocusedTasks() {
        return mDao.queryBuilder().where(mDao.queryBuilder().or(TaskDao.Properties.Schedule.lt(new Date()), TaskDao.Properties.Schedule.isNull()),
                TaskDao.Properties.CompletionDate.isNull(), TaskDao.Properties.Deleted.eq(false), TaskDao.Properties.ParentLocalId.isNull()).orderAsc(TaskDao.Properties.Order).orderDesc(TaskDao.Properties.CreatedAt).list();
    }

    public List<Task> listCompletedTasks() {
        return mDao.queryBuilder().where(TaskDao.Properties.CompletionDate.isNotNull(), TaskDao.Properties.Deleted.eq(false), TaskDao.Properties.ParentLocalId.isNull()).orderDesc(TaskDao.Properties.CompletionDate).list();
    }

    public List<Task> listSubtasksForTask(String objectId) {
        return mDao.queryBuilder().where(TaskDao.Properties.ParentLocalId.eq(objectId), TaskDao.Properties.Deleted.eq(false)).orderAsc(TaskDao.Properties.Order).orderAsc(TaskDao.Properties.CreatedAt).list();
    }

}
