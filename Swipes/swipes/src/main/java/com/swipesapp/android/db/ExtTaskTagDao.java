package com.swipesapp.android.db;

import de.greenrobot.dao.internal.DaoConfig;

/**
 * Extended DAO for join of tasks and tags, allowing custom DB operations.
 *
 * @author Felipe Bari
 */
public class ExtTaskTagDao extends TaskTagDao {

    private static ExtTaskTagDao sInstance;

    // Custom constructor to comply with TaskTagDao.
    private ExtTaskTagDao(DaoConfig config) {
        super(config);
    }

    public static ExtTaskTagDao getInstance(DaoSession daoSession) {
        if (sInstance == null) {
            sInstance = (ExtTaskTagDao) daoSession.getTaskTagDao();
        }
        return sInstance;
    }

    public TaskTag selectAssociation(Long taskId, Long tagId) {
        return queryBuilder().where(Properties.TaskId.eq(taskId), Properties.TagId.eq(tagId)).list().get(0);
    }

}
