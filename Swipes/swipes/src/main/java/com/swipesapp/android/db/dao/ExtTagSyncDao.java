package com.swipesapp.android.db.dao;

import com.swipesapp.android.db.DaoSession;
import com.swipesapp.android.db.TagSync;
import com.swipesapp.android.db.TagSyncDao;

import java.util.List;

/**
 * Extended DAO for tag syncing, allowing custom DB operations.
 *
 * @author Felipe Bari
 */
public class ExtTagSyncDao {

    private static ExtTagSyncDao sInstance;
    private TagSyncDao mDao;

    private ExtTagSyncDao(DaoSession daoSession) {
        mDao = daoSession.getTagSyncDao();
    }

    public static ExtTagSyncDao getInstance(DaoSession daoSession) {
        if (sInstance == null) {
            sInstance = new ExtTagSyncDao(daoSession);
        }
        return sInstance;
    }

    public TagSyncDao getDao() {
        return mDao;
    }

    public List<TagSync> listTagsForSync() {
        return mDao.queryBuilder().list();
    }

}
