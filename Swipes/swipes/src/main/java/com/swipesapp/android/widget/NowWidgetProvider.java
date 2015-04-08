package com.swipesapp.android.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.Toast;

import com.swipesapp.android.R;
import com.swipesapp.android.handler.RepeatHandler;
import com.swipesapp.android.sync.gson.GsonTask;
import com.swipesapp.android.sync.service.TasksService;
import com.swipesapp.android.ui.activity.EditTaskActivity;
import com.swipesapp.android.ui.activity.TasksActivity;
import com.swipesapp.android.util.DateUtils;
import com.swipesapp.android.util.ThemeUtils;
import com.swipesapp.android.values.Constants;
import com.swipesapp.android.values.Intents;
import com.swipesapp.android.values.Sections;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * Provider for the Swipes Now widget.
 *
 * @author Felipe Bari
 */
public class NowWidgetProvider extends AppWidgetProvider {

    private static TasksService sTasksService;
    private static long sLastToastTime;

    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // Load service.
        sTasksService = TasksService.getInstance();
        if (sTasksService == null) sTasksService = TasksService.newInstance(context);

        // Perform loop for each widget belonging to this provider.
        for (int appWidgetId : appWidgetIds) {
            // Initialize widget layout.
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.now_widget);

            // Setup tasks list.
            setupTasksList(appWidgetId, views, context);

            // Setup toolbar at the bottom.
            setupToolbar(views, context);

            // Update empty view messages.
            updateEmptyView(views, context);

            // Tell AppWidgetManager to update current widget.
            appWidgetManager.updateAppWidget(appWidgetId, views);
        }
    }

    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Long taskId = intent.getLongExtra(Constants.EXTRA_TASK_ID, 0);

        // Filter widget actions.
        if (Intents.WIDGET_COMPLETE_TASK.equals(action)) {
            sTasksService = TasksService.getInstance();

            // Complete task.
            GsonTask task = sTasksService.loadTask(taskId);
            task.setLocalCompletionDate(new Date());
            sTasksService.saveTask(task, true);

            // Handle repeat.
            RepeatHandler repeatHandler = new RepeatHandler();
            repeatHandler.handleRepeatedTask(task);

            // Show success message.
            long now = Calendar.getInstance().getTimeInMillis();
            if (now - sLastToastTime > 2000) {
                Toast.makeText(context, context.getString(R.string.now_widget_complete_message), Toast.LENGTH_SHORT).show();
                sLastToastTime = now;
            }

            // Refresh widget and tasks.
            TasksActivity.refreshWidgets(context);
            TasksActivity.setPendingRefresh();
        } else if (Intents.WIDGET_OPEN_TASK.equals(action) || Intents.WIDGET_OPEN_SUBTASKS.equals(action)) {
            // Open task intent.
            Intent openIntent = new Intent(context, EditTaskActivity.class);
            openIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
            openIntent.putExtra(Constants.EXTRA_TASK_ID, taskId);
            openIntent.putExtra(Constants.EXTRA_SECTION_NUMBER, Sections.FOCUS.getSectionNumber());
            openIntent.putExtra(Constants.EXTRA_SHOW_ACTION_STEPS, Intents.WIDGET_OPEN_SUBTASKS.equals(action));
            openIntent.putExtra(Constants.EXTRA_FROM_WIDGET, true);

            // Show task details.
            context.startActivity(openIntent);
        }

        super.onReceive(context, intent);
    }

    private void setupTasksList(int appWidgetId, RemoteViews views, Context context) {
        // Intent to start the widget service.
        Intent intent = new Intent(context, NowWidgetService.class);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        intent.setData(Uri.parse(intent.toUri(Intent.URI_INTENT_SCHEME)));

        // Set the remote adapter to populate data.
        views.setRemoteAdapter(R.id.now_widget_list, intent);

        // Set the empty view.
        views.setEmptyView(R.id.now_widget_list, R.id.now_widget_empty);

        // Widget action intent.
        Intent actionIntent = new Intent(context, NowWidgetProvider.class);
        PendingIntent actionPendingIntent = PendingIntent.getBroadcast(context, 0, actionIntent, 0);

        // Attach intent template.
        views.setPendingIntentTemplate(R.id.now_widget_list, actionPendingIntent);

        // Set backgrounds according to theme.
        int background = ThemeUtils.isLightTheme(context) ?
                R.drawable.widget_tasks_background_light : R.drawable.widget_tasks_background_dark;

        views.setInt(R.id.now_widget_list, "setBackgroundResource", background);
        views.setInt(R.id.now_widget_empty, "setBackgroundResource", background);

        // Set text colors.
        int color = ThemeUtils.getSecondaryTextColor(context);
        views.setInt(R.id.now_widget_all_done, "setTextColor", color);
        views.setInt(R.id.now_widget_next_task, "setTextColor", color);
    }

    private void setupToolbar(RemoteViews views, Context context) {
        // Show tasks intent.
        Intent tasksIntent = new Intent(context, TasksActivity.class);
        tasksIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent tasksPendingIntent = PendingIntent.getActivity(context, 0, tasksIntent, 0);

        // Add task intent.
        Intent addIntent = new Intent(context, TasksActivity.class);
        addIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
        addIntent.setAction(Intents.ADD_TASK);
        PendingIntent addPendingIntent = PendingIntent.getActivity(context, 0, addIntent, 0);

        // Attach click listeners to buttons.
        views.setOnClickPendingIntent(R.id.now_widget_show_tasks, tasksPendingIntent);
        views.setOnClickPendingIntent(R.id.now_widget_add_task, addPendingIntent);
        views.setOnClickPendingIntent(R.id.now_widget_count_area, tasksPendingIntent);

        // Retrieve tasks count.
        int completedToday = sTasksService.countTasksCompletedToday();
        int tasksToday = sTasksService.countTasksForToday() + completedToday;

        // Display tasks count.
        if (tasksToday > 0) {
            views.setTextViewText(R.id.now_widget_tasks_completed, String.valueOf(completedToday));
            views.setTextViewText(R.id.now_widget_tasks_today, String.valueOf(tasksToday));

            views.setViewVisibility(R.id.now_widget_tasks_progress, View.VISIBLE);
            views.setViewVisibility(R.id.now_widget_empty_count, View.GONE);
        } else {
            views.setViewVisibility(R.id.now_widget_tasks_progress, View.GONE);
            views.setViewVisibility(R.id.now_widget_empty_count, View.VISIBLE);
        }

        // Show progress area.
        views.setViewVisibility(R.id.now_widget_count_area, View.VISIBLE);
    }

    private void updateEmptyView(RemoteViews views, Context context) {
        // Load next scheduled task.
        List<GsonTask> tasks = sTasksService.loadScheduledTasks();
        GsonTask nextTask = !tasks.isEmpty() ? tasks.get(0) : null;

        if (nextTask != null && nextTask.getLocalSchedule() != null) {
            Date nextSchedule = nextTask.getLocalSchedule();

            // Set text according to the next scheduled task.
            if (DateUtils.isToday(nextSchedule)) {
                views.setTextViewText(R.id.now_widget_all_done, context.getString(R.string.all_done_now));
                String nextDate = DateUtils.getTimeAsString(context, nextSchedule);
                views.setTextViewText(R.id.now_widget_next_task, context.getString(R.string.all_done_now_next, nextDate));
            } else {
                views.setTextViewText(R.id.now_widget_all_done, context.getString(R.string.all_done_today));
                String nextDate = DateUtils.formatToRecent(nextSchedule, context, false);
                views.setTextViewText(R.id.now_widget_next_task, context.getString(R.string.all_done_today_next, nextDate));
            }
        } else {
            // Show default message.
            views.setTextViewText(R.id.now_widget_all_done, context.getString(R.string.all_done_today));
            views.setTextViewText(R.id.now_widget_next_task, context.getString(R.string.all_done_next_empty));
        }
    }

}
