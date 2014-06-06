package com.swipesapp.android.db;

/**
 * Extended DAO for join of tasks and tags, allowing custom DB operations.
 *
 * @author Felipe Bari
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

    public TaskTag selectAssociation(Long taskId, Long tagId) {
        return mDao.queryBuilder().where(TaskTagDao.Properties.TaskId.eq(taskId), TaskTagDao.Properties.TagId.eq(tagId)).list().get(0);
    }

}
