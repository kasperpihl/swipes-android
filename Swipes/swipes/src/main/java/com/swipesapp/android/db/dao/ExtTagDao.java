package com.swipesapp.android.db.dao;

import com.swipesapp.android.db.DaoSession;
import com.swipesapp.android.db.Tag;
import com.swipesapp.android.db.TagDao;
import com.swipesapp.android.db.TaskDao;

import java.util.List;

/**
 * Extended DAO for tags, allowing custom DB operations.
 *
 * @author Felipe Bari
 */
public class ExtTagDao {

    private static ExtTagDao sInstance;
    private TagDao mDao;

    private ExtTagDao(DaoSession daoSession) {
        mDao = daoSession.getTagDao();
    }

    public static ExtTagDao getInstance(DaoSession daoSession) {
        if (sInstance == null) {
            sInstance = new ExtTagDao(daoSession);
        }
        return sInstance;
    }

    public TagDao getDao() {
        return mDao;
    }

    public Tag selectTag(Long id) {
        return mDao.queryBuilder().where(TaskDao.Properties.Id.eq(id)).unique();
    }

    public Tag selectTag(String tempId) {
        return mDao.queryBuilder().where(TaskDao.Properties.TempId.eq(tempId)).unique();
    }

    public List<Tag> listAllTags() {
        return mDao.queryBuilder().list();
    }

}
