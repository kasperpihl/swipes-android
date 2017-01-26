package com.swipesapp.android.db.dao;

import com.swipesapp.android.db.DaoSession;
import com.swipesapp.android.db.Tag;
import com.swipesapp.android.db.TagDao;

import java.util.List;

/**
 * Extended DAO for tags, allowing custom DB operations.
 *
 * @author Fernanda Bari
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
        return mDao.queryBuilder().where(TagDao.Properties.Id.eq(id)).unique();
    }

    public Tag selectTag(String tempId) {
        return mDao.queryBuilder().where(mDao.queryBuilder().or(TagDao.Properties.TempId.eq(tempId),
                TagDao.Properties.ObjectId.eq(tempId))).unique();
    }

    public List<Tag> listAllTags() {
        return mDao.queryBuilder().orderAsc(TagDao.Properties.Title).list();
    }

    public long countAllTags() {
        return mDao.queryBuilder().buildCount().count();
    }

}
