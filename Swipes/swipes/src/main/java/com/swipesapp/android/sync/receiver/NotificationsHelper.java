package com.swipesapp.android.sync.receiver;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import com.crashlytics.android.Crashlytics;
import com.swipesapp.android.app.SwipesApplication;
import com.swipesapp.android.handler.LanguageHandler;
import com.swipesapp.android.sync.gson.GsonTask;
import com.swipesapp.android.sync.service.SyncService;
import com.swipesapp.android.sync.service.TasksService;
import com.swipesapp.android.ui.view.TimePreference;
import com.swipesapp.android.util.DateUtils;
import com.swipesapp.android.util.PreferenceUtils;
import com.swipesapp.android.values.Intents;

import java.util.Calendar;
import java.util.Date;

import io.fabric.sdk.android.Fabric;

/**
 * Helper for handling the notification alarms. Automatically starts the alarms when
 * the device boots and provides convenience calls to start them at will.
 *
 * @author Felipe Bari
 */
public class NotificationsHelper extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(Intents.BOOT_COMPLETED)) {
            // Initialize Crashlytics.
            Fabric.with(context, new Crashlytics());

            // Apply user selected language.
            LanguageHandler.applyLanguage(context);

            // Initialize database session.
            SwipesApplication.startDaoSession(context);

            // Initialize services.
            TasksService.newInstance(context);
            SyncService.newInstance(context);

            // Start notification alarms.
            createNotificationsAlarm(context, null);
            createRemindersAlarm(context);

            // Start Analytics tracker.
            SwipesApplication.startTracker(context);
        }
    }

    public static void createNotificationsAlarm(Context context, Long time) {
        if (time == null) {
            // Load first scheduled task.
            GsonTask firstScheduled = TasksService.getInstance().loadFirstScheduledTask();

            if (firstScheduled != null && firstScheduled.getLocalSchedule() != null) {
                Calendar alarmTime = getBaseCalendar();
                alarmTime.setTime(firstScheduled.getLocalSchedule());

                // Set alarm time based on first scheduled.
                time = alarmTime.getTimeInMillis();
            }
        }

        if (time != null) {
            // Create notifications intent.
            Intent intent = new Intent(context, NotificationsReceiver.class);
            PendingIntent alarmIntent = PendingIntent.getBroadcast(context, 0, intent, 0);

            // Cancel existing alarms.
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            alarmManager.cancel(alarmIntent);

            // Trigger the alarm on the given time.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, time, alarmIntent);
            } else {
                alarmManager.set(AlarmManager.RTC_WAKEUP, time, alarmIntent);
            }
        }
    }

    public static void handleNextAlarm(Context context, GsonTask latestTask) {
        Date latestSchedule = latestTask.getLocalSchedule();

        // Check if latest task was snoozed.
        if (!latestTask.getDeleted() && latestSchedule != null && latestSchedule.after(new Date())) {
            GsonTask firstScheduled = TasksService.getInstance().loadFirstScheduledTask();

            // Check if it was snoozed for a date more recent than the first scheduled.
            if (firstScheduled == null || firstScheduled.getLocalSchedule() == null ||
                    latestSchedule.before(firstScheduled.getLocalSchedule())) {

                Calendar alarmTime = getBaseCalendar();
                alarmTime.setTime(latestSchedule);

                // Create an alarm for the latest task.
                createNotificationsAlarm(context, alarmTime.getTimeInMillis());
            }
        }
    }

    public static void createRemindersAlarm(Context context) {
        // Schedule daily alarm.
        createDailyReminderAlarm(context);

        // Schedule evening alarm.
        createEveningReminderAlarm(context);

        // Schedule weekly alarm.
        createWeeklyReminderAlarm(context);
    }

    public static void createDailyReminderAlarm(Context context) {
        // Load day start preference.
        String prefWeekendDayStart = PreferenceUtils.readString(PreferenceUtils.SNOOZE_WEEKEND_DAY_START, context);
        int weekendDayStartHour = TimePreference.getHour(prefWeekendDayStart);
        int weekendDayStartMinute = TimePreference.getMinute(prefWeekendDayStart);

        // Create reminders intent.
        PendingIntent alarmIntent = getRemindersIntent(context, Intents.DAILY_REMINDER);

        // Trigger the alarm.
        triggerDailyAlarm(context, weekendDayStartHour, weekendDayStartMinute, alarmIntent);
    }

    public static void createEveningReminderAlarm(Context context) {
        // Load evening start preference.
        String prefEveningStart = PreferenceUtils.readString(PreferenceUtils.SNOOZE_EVENING_START, context);
        int eveningStartHour = TimePreference.getHour(prefEveningStart);
        int eveningStartMinute = TimePreference.getMinute(prefEveningStart);

        // Create reminders intent.
        PendingIntent alarmIntent = getRemindersIntent(context, Intents.EVENING_REMINDER);

        // Trigger the alarm.
        triggerDailyAlarm(context, eveningStartHour, eveningStartMinute, alarmIntent);
    }

    private static void triggerDailyAlarm(Context context, int hour, int minute, PendingIntent alarmIntent) {
        // Set alarm time based on preference.
        Calendar alarmTime = getBaseCalendar();
        alarmTime.set(Calendar.HOUR_OF_DAY, hour);
        alarmTime.set(Calendar.MINUTE, minute);

        // Check if alarm should be in the next day.
        if (alarmTime.before(Calendar.getInstance())) {
            alarmTime.setTimeInMillis(alarmTime.getTimeInMillis() + 86400000);
        }

        // Cancel existing alarms.
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(alarmIntent);

        // Trigger the alarm daily on the given time.
        alarmManager.setInexactRepeating(AlarmManager.RTC_WAKEUP, alarmTime.getTimeInMillis(), 86400000, alarmIntent);
    }

    public static void createWeeklyReminderAlarm(Context context) {
        // Load week start preference.
        String prefWeekStart = PreferenceUtils.readString(PreferenceUtils.SNOOZE_WEEK_START, context);
        int weekStartDay = DateUtils.weekdayFromPrefValue(prefWeekStart);
        int dayBeforeWeekStart = weekStartDay - 1 == 0 ? Calendar.SATURDAY : weekStartDay - 1;

        // Load evening start preference.
        String prefEveningStart = PreferenceUtils.readString(PreferenceUtils.SNOOZE_EVENING_START, context);
        int eveningStartHour = TimePreference.getHour(prefEveningStart);
        int eveningStartMinute = TimePreference.getMinute(prefEveningStart);

        // Set alarm time based on preference.
        Calendar alarmTime = getBaseCalendar();
        alarmTime.set(Calendar.HOUR_OF_DAY, eveningStartHour);
        alarmTime.set(Calendar.MINUTE, eveningStartMinute);
        alarmTime.set(Calendar.DAY_OF_WEEK, dayBeforeWeekStart);

        // Check if alarm should be in the next week.
        if (alarmTime.before(Calendar.getInstance())) {
            alarmTime.setTimeInMillis(alarmTime.getTimeInMillis() + 604800000);
        }

        // Create reminders intent.
        PendingIntent alarmIntent = getRemindersIntent(context, Intents.WEEKLY_REMINDER);

        // Cancel existing alarms.
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(alarmIntent);

        // Trigger the alarm weekly on the given time.
        alarmManager.setInexactRepeating(AlarmManager.RTC_WAKEUP, alarmTime.getTimeInMillis(), 604800000, alarmIntent);
    }

    private static Calendar getBaseCalendar() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        return calendar;
    }

    private static PendingIntent getRemindersIntent(Context context, String action) {
        Intent intent = new Intent(context, NotificationsReceiver.RemindersReceiver.class);
        intent.setAction(action);
        PendingIntent alarmIntent = PendingIntent.getBroadcast(context, 0, intent, 0);

        return alarmIntent;
    }

}
