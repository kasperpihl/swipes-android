package com.swipesapp.android.db.migration;

import android.content.Context;

import com.swipesapp.android.sync.gson.GsonTask;
import com.swipesapp.android.sync.service.TasksService;
import com.swipesapp.android.util.PreferenceUtils;
import com.swipesapp.android.values.RepeatOptions;

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

    /**
     * Applies fixes for each app version.
     *
     * @param context Context instance.
     */
    public static void performUpgrades(Context context) {
        sTasksService = TasksService.getInstance();

        upgradeToV7(context);

        upgradeToV8(context);
    }

    /**
     * Updates repeat option and origin identifier for all tasks.
     *
     * @param context Context instance.
     */
    private static void upgradeToV7(Context context) {
        if (!PreferenceUtils.hasUpgradedToVersion(7, context)) {
            // Update all tasks.
            for (GsonTask task : sTasksService.loadAllTasks()) {
                task.setRepeatOption(RepeatOptions.NEVER);
                task.setOriginIdentifier(null);

                sTasksService.saveTask(task, false);
            }

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
            List<GsonTask> tasks = sTasksService.loadAllTasks();

            // Update all tasks.
            for (int i = 0; i < tasks.size(); i++) {
                GsonTask task = tasks.get(i);
                task.setTempId(task.getTempId() + i);

                sTasksService.saveTask(task, false);
            }

            // Mark as upgraded.
            PreferenceUtils.saveBoolean(V8_UPGRADE_KEY, true, context);
        }
    }

}
