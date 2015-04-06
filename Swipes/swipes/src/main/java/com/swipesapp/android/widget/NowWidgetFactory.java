package com.swipesapp.android.widget;

import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService.RemoteViewsFactory;

import com.swipesapp.android.R;
import com.swipesapp.android.sync.gson.GsonTask;
import com.swipesapp.android.sync.service.TasksService;
import com.swipesapp.android.util.ThemeUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter for binding data to views in the Swipes Now widget.
 */
public class NowWidgetFactory implements RemoteViewsFactory {

    private List<GsonTask> mTasks;
    private Context mContext;
    private TasksService mTasksService;

    public NowWidgetFactory(Context context, Intent intent) {
        mTasks = new ArrayList<>();
        mContext = context;
    }

    public void onCreate() {
        // Load service.
        mTasksService = TasksService.getInstance();
        if (mTasksService == null) mTasksService = TasksService.newInstance(mContext);

        // Load initial data.
        mTasks = mTasksService.loadFocusedTasks();
    }

    public void onDataSetChanged() {
        // Reload data.
        mTasks = mTasksService.loadFocusedTasks();
    }

    public void onDestroy() {
        // Do nothing.
    }

    public int getCount() {
        return mTasks.size();
    }

    public RemoteViews getViewAt(int position) {
        // Initialize cell layout.
        RemoteViews views = new RemoteViews(mContext.getPackageName(), R.layout.now_widget_cell);

        // Load task data.
        String taskId = mTasks.get(position).getTempId();
        String title = mTasks.get(position).getTitle();
        Integer priority = mTasks.get(position).getPriority();
        int subtasks = mTasksService.countUncompletedSubtasksForTask(taskId);

        // Setup properties.
        views.setTextViewText(R.id.now_widget_task_title, title);

        // Set priority indicator.
        if (priority != null) {
            int visibility = priority == 1 ? View.VISIBLE : View.GONE;
            views.setViewVisibility(R.id.now_widget_priority, visibility);
        }

        // Show or hide subtasks count.
        if (subtasks > 0) {
            views.setTextViewText(R.id.now_widget_subtasks, String.valueOf(subtasks));
            views.setViewVisibility(R.id.now_widget_subtasks, View.VISIBLE);
        } else {
            views.setViewVisibility(R.id.now_widget_subtasks, View.GONE);
        }

        // Set colors according to theme.
        int color = ThemeUtils.getTextColor(mContext);
        views.setInt(R.id.now_widget_task_title, "setTextColor", color);

        int checkbox = ThemeUtils.isLightTheme(mContext) ?
                R.drawable.checkbox_light : R.drawable.checkbox_dark;
        views.setInt(R.id.now_widget_complete, "setBackgroundResource", checkbox);

        // TODO: Setup buttons.

        return views;
    }

    public RemoteViews getLoadingView() {
        // Initialize an empty view.
        return new RemoteViews(mContext.getPackageName(), R.layout.now_widget_cell_loading);
    }

    public int getViewTypeCount() {
        return 1;
    }

    public long getItemId(int position) {
        GsonTask item = mTasks.get(position);
        return item != null ? item.getItemId() : -1;
    }

    public boolean hasStableIds() {
        return true;
    }

}
