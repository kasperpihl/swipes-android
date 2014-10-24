package com.swipesapp.android.db;

import java.util.List;

/**
 * Extended DAO for task syncing, allowing custom DB operations.
 *
 * @author Felipe Bari
 */
public class ExtTaskSyncDao {

    private static ExtTaskSyncDao sInstance;
    private TaskSyncDao mDao;

    private ExtTaskSyncDao(DaoSession daoSession) {
        mDao = daoSession.getTaskSyncDao();
    }

    public static ExtTaskSyncDao getInstance(DaoSession daoSession) {
        if (sInstance == null) {
            sInstance = new ExtTaskSyncDao(daoSession);
        }
        return sInstance;
    }

    public TaskSyncDao getDao() {
        return mDao;
    }

    public List<TaskSync> listTasksForSync() {
        return mDao.queryBuilder().list();
    }

}
