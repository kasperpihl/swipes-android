package com.swipesapp.android.db;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;

import de.greenrobot.dao.AbstractDao;
import de.greenrobot.dao.Property;
import de.greenrobot.dao.internal.DaoConfig;

import com.swipesapp.android.db.Deleted;

// THIS CODE IS GENERATED BY greenDAO, DO NOT EDIT.
/** 
 * DAO for table DELETED.
*/
public class DeletedDao extends AbstractDao<Deleted, Long> {

    public static final String TABLENAME = "DELETED";

    /**
     * Properties of entity Deleted.<br/>
     * Can be used for QueryBuilder and for referencing column names.
    */
    public static class Properties {
        public final static Property Id = new Property(0, Long.class, "id", true, "_id");
        public final static Property ClassName = new Property(1, String.class, "className", false, "CLASS_NAME");
        public final static Property ObjectId = new Property(2, String.class, "objectId", false, "OBJECT_ID");
        public final static Property Deleted = new Property(3, Boolean.class, "deleted", false, "DELETED");
    };


    public DeletedDao(DaoConfig config) {
        super(config);
    }
    
    public DeletedDao(DaoConfig config, DaoSession daoSession) {
        super(config, daoSession);
    }

    /** Creates the underlying database table. */
    public static void createTable(SQLiteDatabase db, boolean ifNotExists) {
        String constraint = ifNotExists? "IF NOT EXISTS ": "";
        db.execSQL("CREATE TABLE " + constraint + "'DELETED' (" + //
                "'_id' INTEGER PRIMARY KEY ," + // 0: id
                "'CLASS_NAME' TEXT," + // 1: className
                "'OBJECT_ID' TEXT," + // 2: objectId
                "'DELETED' INTEGER);"); // 3: deleted
    }

    /** Drops the underlying database table. */
    public static void dropTable(SQLiteDatabase db, boolean ifExists) {
        String sql = "DROP TABLE " + (ifExists ? "IF EXISTS " : "") + "'DELETED'";
        db.execSQL(sql);
    }

    /** @inheritdoc */
    @Override
    protected void bindValues(SQLiteStatement stmt, Deleted entity) {
        stmt.clearBindings();
 
        Long id = entity.getId();
        if (id != null) {
            stmt.bindLong(1, id);
        }
 
        String className = entity.getClassName();
        if (className != null) {
            stmt.bindString(2, className);
        }
 
        String objectId = entity.getObjectId();
        if (objectId != null) {
            stmt.bindString(3, objectId);
        }
 
        Boolean deleted = entity.getDeleted();
        if (deleted != null) {
            stmt.bindLong(4, deleted ? 1l: 0l);
        }
    }

    /** @inheritdoc */
    @Override
    public Long readKey(Cursor cursor, int offset) {
        return cursor.isNull(offset + 0) ? null : cursor.getLong(offset + 0);
    }    

    /** @inheritdoc */
    @Override
    public Deleted readEntity(Cursor cursor, int offset) {
        Deleted entity = new Deleted( //
            cursor.isNull(offset + 0) ? null : cursor.getLong(offset + 0), // id
            cursor.isNull(offset + 1) ? null : cursor.getString(offset + 1), // className
            cursor.isNull(offset + 2) ? null : cursor.getString(offset + 2), // objectId
            cursor.isNull(offset + 3) ? null : cursor.getShort(offset + 3) != 0 // deleted
        );
        return entity;
    }
     
    /** @inheritdoc */
    @Override
    public void readEntity(Cursor cursor, Deleted entity, int offset) {
        entity.setId(cursor.isNull(offset + 0) ? null : cursor.getLong(offset + 0));
        entity.setClassName(cursor.isNull(offset + 1) ? null : cursor.getString(offset + 1));
        entity.setObjectId(cursor.isNull(offset + 2) ? null : cursor.getString(offset + 2));
        entity.setDeleted(cursor.isNull(offset + 3) ? null : cursor.getShort(offset + 3) != 0);
     }
    
    /** @inheritdoc */
    @Override
    protected Long updateKeyAfterInsert(Deleted entity, long rowId) {
        entity.setId(rowId);
        return rowId;
    }
    
    /** @inheritdoc */
    @Override
    public Long getKey(Deleted entity) {
        if(entity != null) {
            return entity.getId();
        } else {
            return null;
        }
    }

    /** @inheritdoc */
    @Override    
    protected boolean isEntityUpdateable() {
        return true;
    }
    
}