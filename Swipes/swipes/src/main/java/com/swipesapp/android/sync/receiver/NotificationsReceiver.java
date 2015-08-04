package com.swipesapp.android.sync.receiver;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;

import com.swipesapp.android.R;
import com.swipesapp.android.analytics.handler.Analytics;
import com.swipesapp.android.analytics.values.Actions;
import com.swipesapp.android.analytics.values.Categories;
import com.swipesapp.android.sync.gson.GsonTask;
import com.swipesapp.android.sync.service.TasksService;
import com.swipesapp.android.ui.activity.SnoozeActivity;
import com.swipesapp.android.ui.activity.TasksActivity;
import com.swipesapp.android.util.PreferenceUtils;
import com.swipesapp.android.values.Constants;
import com.swipesapp.android.values.Intents;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * Receiver for the notifications alarm intent.
 *
 * @author Felipe Bari
 */
public class NotificationsReceiver extends BroadcastReceiver {

    public static final String KEY_EXPIRED_TASKS = "notifications_expired_tasks";
    public static final String KEY_PREVIOUS_COUNT = "notifications_previous_count";

    private static final String TASKS_SEPARATOR = ", ";
    private static final String TASKS_REGEX = "\\s*,\\s*";

    private static NotificationManager sNotificationManager;
    private static TasksService sTasksService;
    private static List<GsonTask> sExpiredTasks;
    private static int sPreviousCount;

    @Override
    public void onReceive(Context context, Intent intent) {
        sTasksService = TasksService.getInstance();

        // Reload expired tasks in case the receiver was killed.
        reloadPreviousData(context);

        // Load tasks with schedule within the current minute.
        for (GsonTask task : sTasksService.loadExpiringTasks()) {
            // Add task to the list of expired.
            if (!sExpiredTasks.contains(task)) {
                sExpiredTasks.add(task);
            }
        }

        if (sExpiredTasks.size() > sPreviousCount) {
            // Refresh tasks and widgets.
            refreshContent(context);

            // Send notification if allowed to.
            sendNotification(context);
        }

        sPreviousCount = sExpiredTasks.size();

        // Persist expired tasks in case the receiver is killed.
        saveCurrentData(context);

        // Schedule next alarm.
        NotificationsHelper.createNotificationsAlarm(context, null);
    }

    private void sendNotification(Context context) {
        if (PreferenceUtils.areNotificationsEnabled(context)) {
            // Intent for the snooze button.
            Intent snoozeIntent = new Intent(context, ActionsReceiver.class);
            snoozeIntent.setAction(Intents.SNOOZE_TASKS);
            PendingIntent snoozePendingIntent = PendingIntent.getBroadcast(context, 0, snoozeIntent, 0);

            // Intent for the complete button.
            Intent completeIntent = new Intent(context, ActionsReceiver.class);
            completeIntent.setAction(Intents.COMPLETE_TASKS);
            PendingIntent completePendingIntent = PendingIntent.getBroadcast(context, 0, completeIntent, 0);

            // Intent for notification dismiss.
            Intent deleteIntent = new Intent(context, ActionsReceiver.class);
            PendingIntent deletePendingIntent = PendingIntent.getBroadcast(context, 0, deleteIntent, 0);

            // Load strings based on number of tasks.
            Resources res = context.getResources();
            int size = sExpiredTasks.size();
            String title = res.getQuantityString(R.plurals.notification_title, size, size > 1 ? size : sExpiredTasks.get(0).getTitle());
            String snoozeTitle = res.getString(R.string.notification_snooze, loadSnoozeDelay(context));
            String completeTitle = res.getString(R.string.notification_complete);

            // Customize builder.
            NotificationCompat.Builder builder = getDefaultBuilder(context);
            builder.setContentTitle(title);
            builder.addAction(R.drawable.ic_snooze, snoozeTitle, snoozePendingIntent);
            builder.addAction(R.drawable.ic_complete, completeTitle, completePendingIntent);
            builder.setDeleteIntent(deletePendingIntent);

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

    public static NotificationCompat.Builder getDefaultBuilder(Context context) {
        // Create default builder.
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context);
        builder.setSmallIcon(R.drawable.ic_notification);
        builder.setAutoCancel(true);
        builder.setPriority(NotificationCompat.PRIORITY_MAX);

        int defaults = NotificationCompat.DEFAULT_LIGHTS;

        // Turn on vibration if allowed.
        if (PreferenceUtils.isVibrationEnabled(context)) {
            defaults |= NotificationCompat.DEFAULT_VIBRATE;
        }

        if (PreferenceUtils.shouldUseSystemSound(context)) {
            // Set system notification sound.
            defaults |= NotificationCompat.DEFAULT_SOUND;
        } else {
            // Set custom notification sound.
            String filesPath = "android.resource://" + context.getPackageName() + "/";
            builder.setSound(Uri.parse(filesPath + R.raw.notification_default));
        }

        builder.setDefaults(defaults);

        // Intent to open app.
        Intent tasksIntent = new Intent(context, ActionsReceiver.class);
        tasksIntent.setAction(Intents.SHOW_TASKS);
        PendingIntent tasksPendingIntent = PendingIntent.getBroadcast(context, 0, tasksIntent, 0);

        // Set default intent.
        builder.setContentIntent(tasksPendingIntent);

        return builder;
    }

    private static void reloadPreviousData(Context context) {
        if (sExpiredTasks == null) sExpiredTasks = new ArrayList<>();

        String expired = PreferenceUtils.readString(KEY_EXPIRED_TASKS, context);
        sPreviousCount = PreferenceUtils.readInt(KEY_PREVIOUS_COUNT, context);

        // Load tasks from comma-separated task IDs.
        if (expired != null) {
            List<String> taskIds = Arrays.asList(expired.split(TASKS_REGEX));

            for (String taskId : taskIds) {
                GsonTask task = sTasksService.loadTask(taskId);

                if (task != null && !sExpiredTasks.contains(task)) {
                    sExpiredTasks.add(task);
                }
            }
        }
    }

    private static void saveCurrentData(Context context) {
        String taskIds = null;

        // Save tasks as comma-separated task IDs.
        for (GsonTask task : sExpiredTasks) {
            if (taskIds == null) {
                taskIds = task.getTempId();
            } else {
                taskIds += TASKS_SEPARATOR + task.getTempId();
            }
        }

        PreferenceUtils.saveString(KEY_EXPIRED_TASKS, taskIds, context);
        PreferenceUtils.saveInt(KEY_PREVIOUS_COUNT, sPreviousCount, context);
    }

    private static int loadSnoozeDelay(Context context) {
        // Load delay from user preference.
        String prefLaterToday = PreferenceUtils.readString(PreferenceUtils.SNOOZE_LATER_TODAY, context);
        return Integer.valueOf(prefLaterToday);
    }

    private static void refreshContent(Context context) {
        // Broadcast tasks changed.
        sTasksService.sendBroadcast(Intents.TASKS_CHANGED);

        // Refresh app widgets.
        TasksActivity.refreshWidgets(context);
    }

    private static void sendClickEvent(String action) {
        // Send analytics event.
        Analytics.sendEvent(Categories.NOTIFICATIONS, action, null, (long) sExpiredTasks.size());
    }

    /**
     * Receiver for the reminders alarm intent.
     */
    public static class RemindersReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            sTasksService = TasksService.getInstance();
            String action = intent.getAction();

            if (action != null && PreferenceUtils.areNotificationsEnabled(context)) {
                switch (action) {
                    case Intents.DAILY_REMINDER:
                        // Determine if it's time for a daily reminder.
                        if (PreferenceUtils.isDailyReminderEnabled(context)) {
                            // Check number of tasks for today.
                            if (sTasksService.countTasksForDay(new Date()) <= 1) {
                                String title = context.getString(R.string.reminder_daily_title);
                                String message = context.getString(R.string.reminder_daily_message);

                                // Send daily reminder.
                                sendReminder(title, message, 1, context);
                            }
                        }
                        break;

                    case Intents.EVENING_REMINDER:
                        // Determine if it's time for an evening reminder.
                        if (PreferenceUtils.isDailyReminderEnabled(context)) {
                            int tasksForNow = sTasksService.countTasksForNow();
                            int tasksForToday = sTasksService.countTasksForToday();

                            // Determine if there are tasks left.
                            if (tasksForNow > 0 && tasksForNow == tasksForToday) {
                                String title = context.getResources().getQuantityString(R.plurals.reminder_evening_title, tasksForNow);
                                String message = context.getResources().getQuantityString(R.plurals.reminder_evening_message, tasksForNow, tasksForNow);

                                // Send evening reminder.
                                sendReminder(title, message, 2, context);
                            }
                        }
                        break;

                    case Intents.WEEKLY_REMINDER:
                        // Determine if it's time for a weekly reminder.
                        if (PreferenceUtils.isWeeklyReminderEnabled(context)) {
                            Calendar tomorrow = Calendar.getInstance();
                            tomorrow.setTimeInMillis(tomorrow.getTimeInMillis() + 86400000);

                            // Check number of tasks for tomorrow.
                            if (sTasksService.countTasksForDay(tomorrow.getTime()) <= 1) {
                                String title = context.getString(R.string.reminder_weekly_title);
                                String message = context.getString(R.string.reminder_weekly_message);

                                // Send weekly reminder.
                                sendReminder(title, message, 3, context);
                            }
                        }
                        break;
                }
            }
        }

        private static void sendReminder(String title, String content, int id, Context context) {
            // Customize builder.
            NotificationCompat.Builder builder = getDefaultBuilder(context);
            builder.setContentTitle(title);
            builder.setStyle(new NotificationCompat.BigTextStyle().bigText(content));

            // Send notification.
            sNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            sNotificationManager.notify(id, builder.build());
        }

    }

    /**
     * Receiver for notification actions.
     */
    public static class ActionsReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            sTasksService = TasksService.getInstance();
            String action = intent.getAction();

            // Reload expired tasks in case the receiver was killed.
            reloadPreviousData(context);

            if (action != null) {
                switch (action) {
                    case Intents.SNOOZE_TASKS:
                        // Set snooze time.
                        Calendar snooze = SnoozeActivity.getBaseCalendar();
                        int laterToday = snooze.get(Calendar.HOUR_OF_DAY) + loadSnoozeDelay(context);
                        snooze.set(Calendar.HOUR_OF_DAY, laterToday);

                        SnoozeActivity.roundMinutes(snooze);

                        SnoozeActivity.applyNextDayTreatment(snooze);

                        // Snooze tasks from notification.
                        for (GsonTask task : sExpiredTasks) {
                            task.setLocalSchedule(snooze.getTime());

                            sTasksService.saveTask(task, true);
                        }

                        refreshContent(context);

                        sendClickEvent(Actions.NOTIFICATION_SNOOZED);
                        break;

                    case Intents.COMPLETE_TASKS:
                        // Complete tasks from notification.
                        for (GsonTask task : sExpiredTasks) {
                            Date currentDate = new Date();
                            task.setLocalCompletionDate(currentDate);
                            task.setLocalSchedule(currentDate);

                            sTasksService.saveTask(task, true);
                        }

                        refreshContent(context);

                        sendClickEvent(Actions.NOTIFICATION_COMPLETED);
                        break;

                    case Intents.SHOW_TASKS:
                        // Open main activity.
                        Intent tasksIntent = new Intent(context, TasksActivity.class);
                        tasksIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        tasksIntent.putExtra(Constants.EXTRA_FROM_NOTIFICATIONS, true);
                        context.startActivity(tasksIntent);

                        sendClickEvent(Actions.NOTIFICATION_SHOW_TASKS);
                        break;
                }
            }

            // Clear expired tasks.
            sExpiredTasks.clear();
            sPreviousCount = 0;

            // Persist expired tasks in case the receiver is killed.
            saveCurrentData(context);

            // Cancel notification.
            sNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            sNotificationManager.cancel(0);
        }

    }

}
