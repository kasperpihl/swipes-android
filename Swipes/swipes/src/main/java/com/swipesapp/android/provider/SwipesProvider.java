package com.swipesapp.android.provider;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;

import com.swipesapp.android.db.SwipesDbContract;
import com.swipesapp.android.db.SwipesDbHelper;

public class SwipesProvider extends ContentProvider {
    private static final int TODO_LIST = 1;
    private static final int TODO_ID = 2;
    private static final int TAG_LIST = 3;
    private static final int TAG_ID = 4;

    private static final UriMatcher URI_MATCHER;

    static {
        URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);
        URI_MATCHER.addURI(SwipesContract.AUTHORITY, SwipesContract.TODO_PATH, TODO_LIST);
        URI_MATCHER.addURI(SwipesContract.AUTHORITY, SwipesContract.TODO_PATH + "/#", TODO_ID);
        URI_MATCHER.addURI(SwipesContract.AUTHORITY, SwipesContract.TAG_PATH, TAG_LIST);
        URI_MATCHER.addURI(SwipesContract.AUTHORITY, SwipesContract.TAG_PATH + "/#", TAG_ID);
    }

    private SwipesDbHelper mHelper;

    public SwipesProvider() {
    }

    @Override
    public boolean onCreate() {
        mHelper = new SwipesDbHelper(getContext());
        return true;
    }

    @Override
    public String getType(Uri uri) {
        int uriType = URI_MATCHER.match(uri);
        String type;
        switch (uriType) {
            case TODO_LIST:
                type = SwipesContract.ToDo.CONTENT_TYPE;
                break;
            case TODO_ID:
                type = SwipesContract.ToDo.CONTENT_ITEM_TYPE;
                break;
            case TAG_LIST:
                type = SwipesContract.Tag.CONTENT_TYPE;
                break;
            case TAG_ID:
                type = SwipesContract.Tag.CONTENT_ITEM_TYPE;
                break;
            default:
                throw new IllegalArgumentException("Unsupported URI: " + uri);
        }
        return type;
    }

    // TODO: deal with tags
    @Override
    public Uri insert(Uri uri, ContentValues values) {
        if (URI_MATCHER.match(uri) != TODO_LIST) {
            throw new IllegalArgumentException("Unsupported URI for insertion: " + uri);
        }

        Uri insertionResultUri = null;
        SQLiteDatabase db = mHelper.getWritableDatabase();
        long id = db.insert(SwipesDbContract.ToDoEntry.TODO_TABLE_NAME, null, values);
        if (id > 0) {
            insertionResultUri = Uri.withAppendedPath(uri, String.valueOf(id));
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return insertionResultUri;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {

        SQLiteDatabase db = mHelper.getReadableDatabase();
        SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();
        queryBuilder.setTables(SwipesDbContract.ToDoEntry.TODO_TABLE_NAME);
        switch (URI_MATCHER.match(uri)) {
            case TODO_LIST:
                if (TextUtils.isEmpty(sortOrder))
                    sortOrder = SwipesContract.ToDo.SORT_ORDER_DEFAULT;
                break;
            case TODO_ID:
                queryBuilder.appendWhere(SwipesDbContract.ToDoEntry._ID + " = " + uri.getLastPathSegment());
                break;
            default:
                throw new IllegalArgumentException("Unsupported URI: " + uri);
        }

        Cursor cursor = queryBuilder.query(db, projection, selection, selectionArgs, null, null, sortOrder);
        if (cursor != null) {
            cursor.setNotificationUri(getContext().getContentResolver(), SwipesContract.ToDo.CONTENT_URI);
        }
        return cursor;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection,
                      String[] selectionArgs) {
        int updateCount = 0;
        SQLiteDatabase db = mHelper.getWritableDatabase();
        String where;
        switch (URI_MATCHER.match(uri)) {
            case TODO_LIST:
                where = selection;
                break;
            case TODO_ID:
                String id = uri.getLastPathSegment();
                where = SwipesDbContract.ToDoEntry._ID + " = " + id;
                if (!TextUtils.isEmpty(selection)) where += " AND " + selection;
                break;
            default:
                throw new IllegalArgumentException("Unsupported URI: " + uri);
        }
        updateCount = db.update(SwipesDbContract.ToDoEntry.TODO_TABLE_NAME,
                values,
                where,
                selectionArgs);
        if (updateCount > 0) getContext().getContentResolver().notifyChange(uri, null);
        return updateCount;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        int deleteCount = 0;
        SQLiteDatabase db = mHelper.getWritableDatabase();
        String where;
        switch (URI_MATCHER.match(uri)) {
            case TODO_LIST:
                where = selection;
                break;
            case TODO_ID:
                String id = uri.getLastPathSegment();
                where = SwipesDbContract.ToDoEntry._ID + " = " + id;
                if (!TextUtils.isEmpty(selection)) where += " AND " + selection;
                break;
            default:
                throw new IllegalArgumentException("Unsupported URI: " + uri);
        }
        deleteCount = db.delete(SwipesDbContract.ToDoEntry.TODO_TABLE_NAME,
                where,
                selectionArgs);
        if (deleteCount > 0) getContext().getContentResolver().notifyChange(uri, null);
        return deleteCount;
    }


}
