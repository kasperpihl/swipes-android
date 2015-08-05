package com.swipesapp.android.db.migration;

import android.content.Context;

import com.swipesapp.android.sync.gson.GsonTask;
import com.swipesapp.android.sync.receiver.NotificationsReceiver;
import com.swipesapp.android.sync.service.TasksService;
import com.swipesapp.android.util.PreferenceUtils;
import com.swipesapp.android.values.RepeatOptions;

import java.util.ArrayList;
import java.util.List;

/**
 * Assistant to apply fixes between versions of the app.
 *
 * @author Felipe Bari
 */
public class MigrationAssistant {

    private static TasksService sTasksService;

    public static final String V7_UPGRADE_KEY = "v7_upgrade_performed";

    public static final String V8_UPGRADE_KEY = "v8_upgrade_performed";

    public static final String V21_UPGRADE_KEY = "v21_upgrade_performed";

    /**
     * Applies fixes for each app version.
     *
     * @param context Context instance.
     */
    public static void performUpgrades(Context context) {
        sTasksService = TasksService.getInstance();

        upgradeToV7(context);

        upgradeToV8(context);

        upgradeToV21(context);
    }

    /**
     * Updates repeat option and origin identifier for all tasks.
     *
     * @param context Context instance.
     */
    private static void upgradeToV7(Context context) {
        if (!PreferenceUtils.hasUpgradedToVersion(7, context)) {
            List<GsonTask> updatedTasks = new ArrayList<>();

            // Update all tasks.
            for (GsonTask task : sTasksService.loadAllTasks()) {
                task.setRepeatOption(RepeatOptions.NEVER);
                task.setOriginIdentifier(null);

                updatedTasks.add(task);
            }

            // Save updated tasks.
            sTasksService.saveTasks(updatedTasks, false);

            // Mark as upgraded.
            PreferenceUtils.saveBoolean(V7_UPGRADE_KEY, true, context);
        }
    }

    /**
     * Updates temp ID for all tasks.
     *
     * @param context Context instance.
     */
    private static void upgradeToV8(Context context) {
        if (!PreferenceUtils.hasUpgradedToVersion(8, context)) {
            List<GsonTask> updatedTasks = new ArrayList<>();
            List<GsonTask> tasks = sTasksService.loadAllTasks();

            // Update all tasks.
            for (int i = 0; i < tasks.size(); i++) {
                GsonTask task = tasks.get(i);
                task.setTempId(task.getTempId() + i);

                updatedTasks.add(task);
            }

            // Save updated tasks.
            sTasksService.saveTasks(updatedTasks, false);

            // Mark as upgraded.
            PreferenceUtils.saveBoolean(V8_UPGRADE_KEY, true, context);
        }
    }

    /**
     * Clears invalid notification data.
     *
     * @param context Context instance.
     */
    private static void upgradeToV21(Context context) {
        if (!PreferenceUtils.hasUpgradedToVersion(21, context)) {
            // Clear notification data.
            PreferenceUtils.remove(NotificationsReceiver.KEY_EXPIRED_TASKS, context);
            PreferenceUtils.remove(NotificationsReceiver.KEY_PREVIOUS_COUNT, context);

            // Mark as upgraded.
            PreferenceUtils.saveBoolean(V21_UPGRADE_KEY, true, context);
        }
    }

}
