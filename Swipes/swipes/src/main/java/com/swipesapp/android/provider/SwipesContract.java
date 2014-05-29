package com.swipesapp.android.provider;

import android.content.ContentResolver;
import android.net.Uri;
import android.provider.BaseColumns;

public class SwipesContract {
    private SwipesContract() {
    }

    public static final String AUTHORITY = "com.swipesapp.provider";

    public static final Uri AUTHORITY_URI = Uri.parse("content://" + AUTHORITY);

    public static final String TODO_PATH = "todo";
    public static final String TAG_PATH = "tag";

    public interface ToDoColumns extends BaseColumns {
        String OBJECT_ID = "objectId";
        String TEMP_ID = "tempId";
        String CREATED_AT = "createdAt";
        String UPDATED_AT = "updatedAt";
        String DELETED = "deleted";
        String TITLE = "title";
        String NOTES = "notes";
        String ORDER = "order";
        String PRIORITY = "priority";
        String COMPLETION_DATE = "completionDate";
        String SCHEDULE = "schedule";
        String LOCATION = "location";
        String REPEAT_DATE = "repeatDate";
        String REPEAT_OPTION = "repeatOption";

    }

    public static final class ToDo implements ToDoColumns {
        public static final Uri CONTENT_URI = AUTHORITY_URI.buildUpon().appendPath(TODO_PATH).build();
        private static final String MIME_TYPE_SUFFIX = "/vnd." + AUTHORITY + ".todo";

        public static final String CONTENT_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE + MIME_TYPE_SUFFIX;
        public static final String CONTENT_ITEM_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE + MIME_TYPE_SUFFIX;

        public static final String[] PROJECTION_ALL = {
                _ID,
                OBJECT_ID,
                TEMP_ID,
                CREATED_AT,
                UPDATED_AT,
                DELETED,
                TITLE,
                NOTES,
                ORDER,
                PRIORITY,
                COMPLETION_DATE,
                SCHEDULE,
                LOCATION,
                REPEAT_DATE,
                REPEAT_OPTION};

        public static final String SORT_ORDER_DEFAULT = ORDER + " ASC";

    }

    public interface TagColumns extends BaseColumns {

    }

    public static final class Tag implements TagColumns {
        public static final Uri CONTENT_URI = AUTHORITY_URI.buildUpon().appendPath(TODO_PATH).build();
        private static final String MIME_TYPE_SUFFIX = "/vnd." + AUTHORITY + ".tag";
        public static final String CONTENT_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE + MIME_TYPE_SUFFIX;
        public static final String CONTENT_ITEM_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE + MIME_TYPE_SUFFIX;
    }


}
