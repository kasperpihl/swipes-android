package com.swipesapp.android.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

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
                // Drop old join table.
                db.execSQL("DROP TABLE TASK_TAG");
                // Create it again with the correct primary key.
                db.execSQL("CREATE TABLE 'TASK_TAG' (" +
                        "'_id' INTEGER PRIMARY KEY ," +
                        "'TASK_ID' INTEGER NOT NULL ," +
                        "'TAG_ID' INTEGER NOT NULL );");
                break;
        }
    }

}
