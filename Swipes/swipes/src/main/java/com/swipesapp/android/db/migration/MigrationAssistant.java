package com.swipesapp.android.db.migration;

import android.content.Context;

import com.swipesapp.android.sync.gson.GsonTag;
import com.swipesapp.android.sync.gson.GsonTask;
import com.swipesapp.android.sync.service.SyncService;
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

    public static final String V9_UPGRADE_KEY = "v9_upgrade_performed";

    /**
     * Applies fixes for each app version.
     *
     * @param context Context instance.
     */
    public static void performUpgrades(Context context) {
        sTasksService = TasksService.getInstance(context);

        upgradeToV7(context);

        upgradeToV8(context);

        upgradeToV9(context);
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
            PreferenceUtils.saveBooleanPreference(V7_UPGRADE_KEY, true, context);
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

                sTasksService.saveTask(task);
            }

            // Mark as upgraded.
            PreferenceUtils.saveBooleanPreference(V8_UPGRADE_KEY, true, context);
        }
    }

    /**
     * Save all local objects for syncing.
     *
     * @param context Context instance.
     */
    private static void upgradeToV9(Context context) {
        if (!PreferenceUtils.hasUpgradedToVersion(9, context)) {
            // Save all tags for syncing.
            for (GsonTag tag : sTasksService.loadAllTags()) {
                SyncService.getInstance(context).saveTagForSync(tag);
            }

            // Save all tasks for syncing.
            for (GsonTask task : sTasksService.loadAllTasks()) {
                task.setId(null);
                SyncService.getInstance(context).saveTaskChangesForSync(task);
            }

            // Mark as upgraded.
            PreferenceUtils.saveBooleanPreference(V9_UPGRADE_KEY, true, context);
        }
    }

}
