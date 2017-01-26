package com.swipesapp.android.db.dao;

import com.swipesapp.android.db.DaoSession;
import com.swipesapp.android.db.TaskTag;
import com.swipesapp.android.db.TaskTagDao;

import java.util.List;

/**
 * Extended DAO for join of tasks and tags, allowing custom DB operations.
 *
 * @author Fernanda Bari
 */
public class ExtTaskTagDao {

    private static ExtTaskTagDao sInstance;
    private TaskTagDao mDao;

    private ExtTaskTagDao(DaoSession daoSession) {
        mDao = daoSession.getTaskTagDao();
    }

    public static ExtTaskTagDao getInstance(DaoSession daoSession) {
        if (sInstance == null) {
            sInstance = new ExtTaskTagDao(daoSession);
        }
        return sInstance;
    }

    public TaskTagDao getDao() {
        return mDao;
    }

    public List<TaskTag> listAllAssociations() {
        return mDao.queryBuilder().list();
    }

    public TaskTag selectAssociation(Long taskId, Long tagId) {
        return mDao.queryBuilder().where(TaskTagDao.Properties.TaskId.eq(taskId), TaskTagDao.Properties.TagId.eq(tagId)).unique();
    }

    public List<TaskTag> selectAssociationsForTag(Long tagId) {
        return mDao.queryBuilder().where(TaskTagDao.Properties.TagId.eq(tagId)).list();
    }

    public List<TaskTag> selectAssociationsForTask(Long taskId) {
        return mDao.queryBuilder().where(TaskTagDao.Properties.TaskId.eq(taskId)).list();
    }

}
