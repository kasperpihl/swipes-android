package com.swipesapp.android.sync.receiver;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.crashlytics.android.Crashlytics;
import com.swipesapp.android.app.SwipesApplication;
import com.swipesapp.android.sync.service.SyncService;
import com.swipesapp.android.sync.service.TasksService;
import com.swipesapp.android.values.Intents;

/**
 * Helper for handling the notifications alarm. Automatically starts the alarm when
 * the device boots and provides a convenience call to start it at will.
 *
 * @author Felipe Bari
 */
public class NotificationsHelper extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(Intents.BOOT_COMPLETED)) {
            // Initialize Crashlytics.
            Crashlytics.start(context);

            // Start notifications alarm.
            createNotificationsAlarm(context);

            // Initialize database session.
            SwipesApplication.startDaoSession(context);

            // Initialize services.
            TasksService.newInstance(context);
            SyncService.newInstance(context);

            // Start Analytics tracker.
            SwipesApplication.startTracker(context);
        }
    }

    public static void createNotificationsAlarm(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, NotificationsReceiver.class);
        PendingIntent alarmIntent = PendingIntent.getBroadcast(context, 0, intent, 0);

        // Trigger the alarm after a minute.
        alarmManager.setInexactRepeating(AlarmManager.ELAPSED_REALTIME, 60000, 60000, alarmIntent);
    }

}
