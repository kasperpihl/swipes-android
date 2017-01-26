package com.swipesapp.android.db.dao;

import android.util.Log;

import com.swipesapp.android.db.DaoSession;
import com.swipesapp.android.db.TaskSync;
import com.swipesapp.android.db.TaskSyncDao;

import java.util.List;

/**
 * Extended DAO for task syncing, allowing custom DB operations.
 *
 * @author Fernanda Bari
 */
public class ExtTaskSyncDao {

    private static ExtTaskSyncDao sInstance;
    private TaskSyncDao mDao;

    private ExtTaskSyncDao(DaoSession daoSession) {
        mDao = daoSession.getTaskSyncDao();
    }

    private static final String LOG_TAG = ExtTaskDao.class.getSimpleName();

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

    public TaskSync selectTaskForSync(String tempId) {
        List<TaskSync> tasks = mDao.queryBuilder().where(mDao.queryBuilder().or(TaskSyncDao.Properties.TempId.eq(tempId),
                TaskSyncDao.Properties.ObjectId.eq(tempId))).list();

        if (tasks != null && tasks.size() > 1) {
            // TODO: Log analytics event so we know when duplicates are being created.
            Log.w(LOG_TAG, "Duplicate found with tempId" + tempId);
        }

        return tasks != null && !tasks.isEmpty() ? tasks.get(0) : null;
    }

}
