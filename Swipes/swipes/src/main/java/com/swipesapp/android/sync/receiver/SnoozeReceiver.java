package com.swipesapp.android.sync.receiver;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.support.v4.app.NotificationCompat;

import com.swipesapp.android.R;
import com.swipesapp.android.sync.gson.GsonTask;
import com.swipesapp.android.sync.service.TasksService;
import com.swipesapp.android.ui.activity.BlankActivityDark;
import com.swipesapp.android.ui.activity.BlankActivityLight;
import com.swipesapp.android.ui.activity.SnoozeActivity;
import com.swipesapp.android.util.PreferenceUtils;
import com.swipesapp.android.util.ThemeUtils;
import com.swipesapp.android.values.Actions;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * Receiver for the snooze alarm intent.
 *
 * @author Felipe Bari
 */
public class SnoozeReceiver extends BroadcastReceiver {

    private static NotificationManager sNotificationManager;
    private static TasksService sTasksService;
    private static List<GsonTask> sExpiredTasks;

    @Override
    public void onReceive(Context context, Intent intent) {
        sTasksService = TasksService.getInstance(context);

        List<GsonTask> snoozedTasks = new ArrayList<GsonTask>();
        sExpiredTasks = new ArrayList<GsonTask>();

        // Tasks might have changed seconds ago, so consider both snoozed and focused.
        snoozedTasks.addAll(sTasksService.loadScheduledTasks());
        snoozedTasks.addAll(sTasksService.loadFocusedTasks());

        Calendar calendar = Calendar.getInstance();
        long now = calendar.getTimeInMillis();

        // Look for tasks with snooze date within the last minute.
        for (GsonTask task : snoozedTasks) {
            if (task.getSchedule() != null) {
                calendar.setTime(task.getSchedule());
                long schedule = calendar.getTimeInMillis();
                long delta = now - schedule;

                // Add task to the list of expired.
                if (delta >= 0 && delta < 60000) sExpiredTasks.add(task);
            }
        }

        if (!sExpiredTasks.isEmpty()) {
            // Broadcast tasks changed.
            sTasksService.sendBroadcast(Actions.TASKS_CHANGED);

            // Send notification if allowed to.
            sendNotification(context);
        }
    }

    private void sendNotification(Context context) {
        if (PreferenceUtils.areNotificationsEnabled(context)) {
            NotificationCompat.Builder builder = new NotificationCompat.Builder(context);
            builder.setSmallIcon(R.drawable.ic_notification);
            builder.setDefaults(Notification.DEFAULT_ALL);
            builder.setAutoCancel(true);

            // Intent to open app.
            Class intentTarget = ThemeUtils.isLightTheme(context) ? BlankActivityLight.class : BlankActivityDark.class;
            Intent tasksIntent = new Intent(context, intentTarget);
            PendingIntent tasksPendingIntent = PendingIntent.getActivity(context, 0, tasksIntent, 0);
            builder.setContentIntent(tasksPendingIntent);

            // Intent for the snooze button.
            Intent snoozeIntent = new Intent(context, ActionsReceiver.class);
            snoozeIntent.setAction(Actions.SNOOZE_TASKS);
            PendingIntent snoozePendingIntent = PendingIntent.getBroadcast(context, 0, snoozeIntent, 0);

            // Intent for the complete button.
            Intent completeIntent = new Intent(context, ActionsReceiver.class);
            completeIntent.setAction(Actions.COMPLETE_TASKS);
            PendingIntent completePendingIntent = PendingIntent.getBroadcast(context, 0, completeIntent, 0);

            // Load strings based on number of tasks.
            Resources res = context.getResources();
            int size = sExpiredTasks.size();
            String title = res.getQuantityString(R.plurals.notification_title, size, size > 1 ? size : sExpiredTasks.get(0).getTitle());
            String snoozeTitle = res.getQuantityString(R.plurals.notification_snooze, size, 3);
            String completeTitle = res.getQuantityString(R.plurals.notification_complete, size);

            builder.setContentTitle(title);
            builder.addAction(R.drawable.ic_snooze, snoozeTitle, snoozePendingIntent);
            builder.addAction(R.drawable.ic_complete, completeTitle, completePendingIntent);

            // Display task titles for multiple tasks.
            if (size > 1) {
                NotificationCompat.InboxStyle content = new NotificationCompat.InboxStyle();
                for (GsonTask task : sExpiredTasks) {
                    content.addLine(task.getTitle());
                }
                builder.setStyle(content);
            }

            // Send notification.
            sNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            sNotificationManager.notify(0, builder.build());
        }
    }

    public static class ActionsReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Actions.SNOOZE_TASKS)) {
                // Set snooze time.
                Calendar snooze = Calendar.getInstance();
                int laterToday = snooze.get(Calendar.HOUR_OF_DAY) + 3;
                snooze.set(Calendar.HOUR_OF_DAY, laterToday);

                SnoozeActivity.applyNextDayTreatment(snooze);

                // Snooze tasks from notification.
                for (GsonTask task : sExpiredTasks) {
                    task.setSchedule(snooze.getTime());
                    sTasksService.saveTask(task);
                }
            } else if (intent.getAction().equals(Actions.COMPLETE_TASKS)) {
                // Complete tasks from notification.
                for (GsonTask task : sExpiredTasks) {
                    task.setCompletionDate(new Date());
                    sTasksService.saveTask(task);
                }
            }

            // Cancel notification.
            sNotificationManager.cancel(0);
        }
    }

}
