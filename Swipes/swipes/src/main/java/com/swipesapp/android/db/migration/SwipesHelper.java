package com.swipesapp.android.db.migration;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.swipesapp.android.db.DaoMaster;

/**
 * Extended database helper to deal with migrations.
 *
 * @author Felipe Bari
 */
public class SwipesHelper extends DaoMaster.OpenHelper {

    public SwipesHelper(Context context, String name, SQLiteDatabase.CursorFactory factory) {
        super(context, name, factory);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.i("greenDAO", "Upgrading schema from version " + oldVersion + " to " + newVersion);

        switch (oldVersion) {
            case 1000:
                migrateToVersion(1001, db);
                migrateToVersion(1002, db);
                migrateToVersion(1003, db);
                break;
            case 1001:
                migrateToVersion(1002, db);
                migrateToVersion(1003, db);
                break;
            case 1002:
                migrateToVersion(1003, db);
                break;
        }
    }

    private void migrateToVersion(int version, SQLiteDatabase db) {
        switch (version) {
            case 1001:
                // Drop old join table.
                db.execSQL("DROP TABLE 'TASK_TAG'");

                // Create it again with the correct primary key.
                db.execSQL("CREATE TABLE 'TASK_TAG' (" +
                        "'_id' INTEGER PRIMARY KEY ," +
                        "'TASK_ID' INTEGER NOT NULL ," +
                        "'TAG_ID' INTEGER NOT NULL );");
                break;
            case 1002:
                // Create task sync table.
                db.execSQL("CREATE TABLE 'TASK_SYNC' (" +
                        "'_id' INTEGER PRIMARY KEY ," +
                        "'OBJECT_ID' TEXT," +
                        "'TEMP_ID' TEXT," +
                        "'PARENT_LOCAL_ID' TEXT," +
                        "'CREATED_AT' TEXT," +
                        "'UPDATED_AT' TEXT," +
                        "'DELETED' INTEGER," +
                        "'TITLE' TEXT," +
                        "'NOTES' TEXT," +
                        "'ORDER' INTEGER," +
                        "'PRIORITY' INTEGER," +
                        "'COMPLETION_DATE' TEXT," +
                        "'SCHEDULE' TEXT," +
                        "'LOCATION' TEXT," +
                        "'REPEAT_DATE' TEXT," +
                        "'REPEAT_OPTION' TEXT," +
                        "'ORIGIN' TEXT," +
                        "'ORIGIN_IDENTIFIER' TEXT," +
                        "'TAGS' TEXT);");

                // Create tag sync table.
                db.execSQL("CREATE TABLE 'TAG_SYNC' (" +
                        "'_id' INTEGER PRIMARY KEY ," +
                        "'OBJECT_ID' TEXT," +
                        "'TEMP_ID' TEXT," +
                        "'CREATED_AT' TEXT," +
                        "'UPDATED_AT' TEXT," +
                        "'TITLE' TEXT);");

                // Create deleted objects table.
                db.execSQL("CREATE TABLE 'DELETED' (" +
                        "'_id' INTEGER PRIMARY KEY ," +
                        "'CLASS_NAME' TEXT," +
                        "'OBJECT_ID' TEXT," +
                        "'DELETED' INTEGER);");
                break;
            case 1003:
                // Change tag sync table.
                db.execSQL("ALTER TABLE 'TAG_SYNC' ADD COLUMN 'DELETED' INTEGER");

                // Drop deleted objects table.
                db.execSQL("DROP TABLE 'DELETED'");
                break;
        }
    }

}
