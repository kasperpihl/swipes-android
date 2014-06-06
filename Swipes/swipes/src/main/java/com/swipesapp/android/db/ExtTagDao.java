package com.swipesapp.android.db;

import java.util.ArrayList;
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

    public List<Tag> listAllTags() {
        return mDao.queryBuilder().list();
    }

    public List<Task> listTasksForTag(Long tagId) {
        // TODO: Build a raw query to retrieve all tasks for a specific tag, using the join table.
        return new ArrayList<Task>();
    }

}
