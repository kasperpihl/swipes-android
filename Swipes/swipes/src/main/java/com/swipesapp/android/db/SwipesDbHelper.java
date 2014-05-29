package com.swipesapp.android.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class SwipesDbHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "swipes.db";
    private static final int DB_VERSION = 1;

    private static final String TEXT_TYPE = " TEXT";
    private static final String DATE_TYPE = " TEXT"; // there's no DATE in SQLite
    private static final String INTEGER_TYPE = " INTEGER";
    private static final String BOOLEAN_TYPE = " INTEGER"; // there's no BOOLEAN in SQLite

    private static final String COMMA_SEP = ",";

    private static final String SQL_CREATE_TODO_TABLE = "CREATE TABLE "
            + SwipesDbContract.ToDoEntry.TODO_TABLE_NAME + " ("
            + SwipesDbContract.ToDoEntry._ID + INTEGER_TYPE + " PRIMARY KEY" + COMMA_SEP
            + SwipesDbContract.ToDoEntry.TODO_TABLE_COLUMN_OBJECT_ID + TEXT_TYPE + COMMA_SEP
            + SwipesDbContract.ToDoEntry.TODO_TABLE_COLUMN_TEMP_ID + TEXT_TYPE + COMMA_SEP
            + SwipesDbContract.ToDoEntry.TODO_TABLE_COLUMN_CREATED_AT + DATE_TYPE + COMMA_SEP
            + SwipesDbContract.ToDoEntry.TODO_TABLE_COLUMN_UPDATED_AT + DATE_TYPE + COMMA_SEP
            + SwipesDbContract.ToDoEntry.TODO_TABLE_COLUMN_DELETED + BOOLEAN_TYPE + COMMA_SEP
            + SwipesDbContract.ToDoEntry.TODO_TABLE_COLUMN_TITLE + TEXT_TYPE + COMMA_SEP
            + SwipesDbContract.ToDoEntry.TODO_TABLE_COLUMN_NOTES + TEXT_TYPE + COMMA_SEP
            + SwipesDbContract.ToDoEntry.TODO_TABLE_COLUMN_ORDER + INTEGER_TYPE + COMMA_SEP
            + SwipesDbContract.ToDoEntry.TODO_TABLE_COLUMN_PRIORITY + INTEGER_TYPE + COMMA_SEP
            + SwipesDbContract.ToDoEntry.TODO_TABLE_COLUMN_COMPLETION_DATE + DATE_TYPE + COMMA_SEP
            + SwipesDbContract.ToDoEntry.TODO_TABLE_COLUMN_SCHEDULE + DATE_TYPE + COMMA_SEP
            + SwipesDbContract.ToDoEntry.TODO_TABLE_COLUMN_LOCATION + TEXT_TYPE + COMMA_SEP
            + SwipesDbContract.ToDoEntry.TODO_TABLE_COLUMN_REPEAT_DATE + DATE_TYPE + COMMA_SEP
            + SwipesDbContract.ToDoEntry.TODO_TABLE_COLUMN_REPEAT_OPTION + TEXT_TYPE
            + ");";

    private static final String SQL_DROP_TODO_TABLE = "DROP TABLE IF EXISTS " + SwipesDbContract.ToDoEntry.TODO_TABLE_NAME;

    public SwipesDbHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_TODO_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // for now, just drop the table and recreate
        db.execSQL(SQL_DROP_TODO_TABLE);
        onCreate(db);
    }

}
