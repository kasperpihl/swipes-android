package com.swipesapp.android.db;

import android.content.Context;

import com.swipesapp.android.sync.gson.GsonTask;
import com.swipesapp.android.sync.service.TasksService;
import com.swipesapp.android.util.PreferenceUtils;
import com.swipesapp.android.values.RepeatOptions;

/**
 * Assistant to apply fixes between versions of the app.
 *
 * @author Felipe Bari
 */
public class MigrationAssistant {

    private static TasksService sTasksService;

    /**
     * Applies fixes for each app version.
     *
     * @param context Context instance.
     */
    public static void performUpgrades(Context context) {
        sTasksService = TasksService.getInstance(context);

        upgradeToV7(context);
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
                task.setRepeatOption(RepeatOptions.NEVER.getValue());
                task.setOriginIdentifier(null);

                sTasksService.saveTask(task);
            }

            // Mark as upgraded.
            PreferenceUtils.saveBooleanPreference(PreferenceUtils.V7_UPGRADE_KEY, true, context);
        }
    }

}
