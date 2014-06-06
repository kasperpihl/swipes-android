package com.swipesapp.android.db;

import java.util.ArrayList;
import java.util.List;

import de.greenrobot.dao.internal.DaoConfig;

/**
 * Extended DAO for tags, allowing custom DB operations.
 *
 * @author Felipe Bari
 */
public class ExtTagDao extends TagDao {

    private static ExtTagDao sInstance;

    // Custom constructor to comply with TagDao.
    private ExtTagDao(DaoConfig config) {
        super(config);
    }

    public static ExtTagDao getInstance(DaoSession daoSession) {
        if (sInstance == null) {
            sInstance = (ExtTagDao) daoSession.getTagDao();
        }
        return sInstance;
    }

    public List<Tag> listAllTags() {
        return queryBuilder().list();
    }

    public List<Task> listTasksForTag(Long tagId) {
        // TODO: Build a raw query to retrieve all tasks for a specific tag, using the join table.
        return new ArrayList<Task>();
    }

}
