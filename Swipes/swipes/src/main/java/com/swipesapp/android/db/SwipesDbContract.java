package com.swipesapp.android.db;

import android.provider.BaseColumns;

public class SwipesDbContract {
    private SwipesDbContract() {
    }

    public static final class ToDoEntry implements BaseColumns {
        public static final String TODO_TABLE_NAME = "todo";
        public static final String TODO_TABLE_COLUMN_OBJECT_ID = "objectId";
        public static final String TODO_TABLE_COLUMN_TEMP_ID = "tempId";
        public static final String TODO_TABLE_COLUMN_CREATED_AT = "createdAt";
        public static final String TODO_TABLE_COLUMN_UPDATED_AT = "updatedAt";
        public static final String TODO_TABLE_COLUMN_DELETED = "deleted";
        public static final String TODO_TABLE_COLUMN_TITLE = "title";
        public static final String TODO_TABLE_COLUMN_NOTES = "notes";
        public static final String TODO_TABLE_COLUMN_ORDER = "order";
        public static final String TODO_TABLE_COLUMN_PRIORITY = "priority";
        public static final String TODO_TABLE_COLUMN_COMPLETION_DATE = "completionDate";
        public static final String TODO_TABLE_COLUMN_SCHEDULE = "schedule";
        public static final String TODO_TABLE_COLUMN_LOCATION = "location";
        public static final String TODO_TABLE_COLUMN_REPEAT_DATE = "repeatDate";
        public static final String TODO_TABLE_COLUMN_REPEAT_OPTION = "repeatOption";
    }

    public static final class TagEntry implements BaseColumns {

    }

    public static final class ToDoTagAssociationEntry implements BaseColumns {

    }
}
