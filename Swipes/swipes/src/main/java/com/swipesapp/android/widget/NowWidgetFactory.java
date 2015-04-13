package com.swipesapp.android.widget;

import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService.RemoteViewsFactory;

import com.swipesapp.android.R;
import com.swipesapp.android.sync.gson.GsonTask;
import com.swipesapp.android.sync.service.TasksService;
import com.swipesapp.android.util.PreferenceUtils;
import com.swipesapp.android.util.ThemeUtils;
import com.swipesapp.android.values.Constants;
import com.swipesapp.android.values.Intents;

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
        Long taskId = mTasks.get(position).getId();
        String tempId = mTasks.get(position).getTempId();
        String title = mTasks.get(position).getTitle();
        Integer priority = mTasks.get(position).getPriority();
        int subtasks = mTasksService.countUncompletedSubtasksForTask(tempId);

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

            // HACK: Use invisible priority to align right margin.
            if (priority == null || priority == 0) {
                views.setViewVisibility(R.id.now_widget_priority, View.INVISIBLE);
            }
        } else {
            views.setViewVisibility(R.id.now_widget_subtasks, View.GONE);
        }

        // Load current width.
        int width = PreferenceUtils.readInt(NowWidgetProvider.WIDTH_KEY, mContext);

        // Hide views based on width.
        if (width > 0 && width < NowWidgetProvider.SMALL_WIDTH) {
            views.setViewVisibility(R.id.now_widget_subtasks, View.GONE);
        }

        // Set colors according to theme.
        int color = ThemeUtils.getTextColor(mContext);
        views.setInt(R.id.now_widget_task_title, "setTextColor", color);

        int cellBackground = ThemeUtils.isLightTheme(mContext) ?
                R.drawable.widget_cell_selector_light : R.drawable.widget_cell_selector_dark;
        views.setInt(R.id.now_widget_task_title, "setBackgroundResource", cellBackground);

        int checkbox = ThemeUtils.isLightTheme(mContext) ?
                R.drawable.widget_checkbox_light : R.drawable.widget_checkbox_dark;
        views.setInt(R.id.now_widget_complete, "setBackgroundResource", checkbox);

        int subtasksBackground = ThemeUtils.isLightTheme(mContext) ?
                R.drawable.widget_subtasks_selector_light : R.drawable.widget_subtasks_selector_dark;
        views.setInt(R.id.now_widget_subtasks, "setBackgroundResource", subtasksBackground);

        // Setup view click actions.
        setupActions(views, taskId);

        return views;
    }

    private void setupActions(RemoteViews views, Long taskId) {
        // Fill complete task intent.
        Intent completeIntent = new Intent();
        completeIntent.setAction(Intents.WIDGET_COMPLETE_TASK);
        completeIntent.putExtra(Constants.EXTRA_TASK_ID, taskId);

        // Fill open task intent.
        Intent taskIntent = new Intent();
        taskIntent.setAction(Intents.WIDGET_OPEN_TASK);
        taskIntent.putExtra(Constants.EXTRA_TASK_ID, taskId);

        // Fill open subtasks intent.
        Intent subtasksIntent = new Intent();
        subtasksIntent.setAction(Intents.WIDGET_OPEN_SUBTASKS);
        subtasksIntent.putExtra(Constants.EXTRA_TASK_ID, taskId);

        // Attach fill intents.
        views.setOnClickFillInIntent(R.id.now_widget_complete, completeIntent);
        views.setOnClickFillInIntent(R.id.now_widget_task_title, taskIntent);
        views.setOnClickFillInIntent(R.id.now_widget_subtasks, subtasksIntent);
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
