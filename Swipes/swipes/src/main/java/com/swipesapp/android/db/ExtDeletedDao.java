package com.swipesapp.android.db;

import java.util.List;

/**
 * Extended DAO for syncing deleted objects, allowing custom DB operations.
 *
 * @author Felipe Bari
 */
public class ExtDeletedDao {

    private static ExtDeletedDao sInstance;
    private DeletedDao mDao;

    private ExtDeletedDao(DaoSession daoSession) {
        mDao = daoSession.getDeletedDao();
    }

    public static ExtDeletedDao getInstance(DaoSession daoSession) {
        if (sInstance == null) {
            sInstance = new ExtDeletedDao(daoSession);
        }
        return sInstance;
    }

    public DeletedDao getDao() {
        return mDao;
    }

    public List<Deleted> listDeletedObjectsForSync() {
        return mDao.queryBuilder().list();
    }

}
