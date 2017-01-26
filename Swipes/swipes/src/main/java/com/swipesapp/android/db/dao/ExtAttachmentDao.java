package com.swipesapp.android.db.dao;

import com.swipesapp.android.db.Attachment;
import com.swipesapp.android.db.AttachmentDao;
import com.swipesapp.android.db.DaoSession;

import java.util.List;

/**
 * Extended DAO for attachments, allowing custom DB operations.
 *
 * @author Fernanda Bari
 */
public class ExtAttachmentDao {

    private static ExtAttachmentDao sInstance;
    private AttachmentDao mDao;

    private ExtAttachmentDao(DaoSession daoSession) {
        mDao = daoSession.getAttachmentDao();
    }

    public static ExtAttachmentDao getInstance(DaoSession daoSession) {
        if (sInstance == null) {
            sInstance = new ExtAttachmentDao(daoSession);
        }
        return sInstance;
    }

    public AttachmentDao getDao() {
        return mDao;
    }

    public Attachment selectAttachment(Long id) {
        return mDao.queryBuilder().where(AttachmentDao.Properties.Id.eq(id)).unique();
    }

    public Attachment selectAttachment(String identifier) {
        return mDao.queryBuilder().where(AttachmentDao.Properties.Identifier.eq(identifier)).unique();
    }

    public List<Attachment> listAllAttachments() {
        return mDao.queryBuilder().list();
    }

    public List<Attachment> listAttachmentsForService(String service) {
        return mDao.queryBuilder().where(AttachmentDao.Properties.Service.eq(service)).list();
    }

    public List<Attachment> listAttachmentsForTask(Long taskId) {
        return mDao.queryBuilder().where(AttachmentDao.Properties.TaskId.eq(taskId)).list();
    }

}
