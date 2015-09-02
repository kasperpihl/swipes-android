package com.swipesapp.android.db.migration;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

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

    private static final String MIGRATION_FILE = "com.swipesapp.android.migration_prefs";

    public static final String V7_UPGRADE_KEY = "v7_upgrade_performed";
    public static final String V8_UPGRADE_KEY = "v8_upgrade_performed";
    public static final String V21_UPGRADE_KEY = "v21_upgrade_performed";
    public static final String V31_UPGRADE_KEY = "v31_upgrade_performed";

    /**
     * Applies fixes for each app version.
     *
     * @param context Context instance.
     */
    public static void performUpgrades(Context context) {
        sTasksService = TasksService.getInstance();

        updatePreferencesFile(context);

        upgradeToV7(context);
        upgradeToV8(context);
        upgradeToV21(context);
    }

    /**
     * Determines if the app has been upgraded to a given version.
     *
     * @param version Version to check for upgrade.
     * @param context Context instance.
     * @return True if it has been upgraded.
     */
    public static boolean hasUpgradedToVersion(int version, Context context) {
        boolean hasUpgraded = false;
        SharedPreferences settings = context.getSharedPreferences(MIGRATION_FILE, Context.MODE_PRIVATE);

        switch (version) {
            case 7:
                hasUpgraded = settings.getBoolean(V7_UPGRADE_KEY, false);
                break;
            case 8:
                hasUpgraded = settings.getBoolean(V8_UPGRADE_KEY, false);
                break;
            case 21:
                hasUpgraded = settings.getBoolean(V21_UPGRADE_KEY, false);
                break;
            case 31:
                hasUpgraded = settings.getBoolean(V31_UPGRADE_KEY, false);
                break;
        }

        return hasUpgraded;
    }

    /**
     * Saves a boolean preference to the migration file.
     *
     * @param preference Preference to save.
     * @param value      Value to apply.
     * @param context    Context instance.
     */
    public static void saveBooleanPreference(String preference, boolean value, Context context) {
        SharedPreferences settings = context.getSharedPreferences(MIGRATION_FILE, Context.MODE_PRIVATE);
        settings.edit().putBoolean(preference, value).apply();
    }

    /**
     * Moves migration info to a separate preferences file.
     *
     * @param context Context instance.
     */
    private static void updatePreferencesFile(Context context) {
        if (!hasUpgradedToVersion(31, context)) {
            SharedPreferences oldPrefs = PreferenceManager.getDefaultSharedPreferences(context);

            // Read values from old file.
            boolean hasUpgradedV7 = oldPrefs.getBoolean(V7_UPGRADE_KEY, false);
            boolean hasUpgradedV8 = oldPrefs.getBoolean(V8_UPGRADE_KEY, false);
            boolean hasUpgradedV21 = oldPrefs.getBoolean(V21_UPGRADE_KEY, false);

            // Save values to new file.
            saveBooleanPreference(V7_UPGRADE_KEY, hasUpgradedV7, context);
            saveBooleanPreference(V8_UPGRADE_KEY, hasUpgradedV8, context);
            saveBooleanPreference(V21_UPGRADE_KEY, hasUpgradedV21, context);

            // Mark as upgraded.
            saveBooleanPreference(V31_UPGRADE_KEY, true, context);
        }
    }

    /**
     * Updates repeat option and origin identifier for all tasks.
     *
     * @param context Context instance.
     */
    private static void upgradeToV7(Context context) {
        if (!hasUpgradedToVersion(7, context)) {
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
            saveBooleanPreference(V7_UPGRADE_KEY, true, context);
        }
    }

    /**
     * Updates temp ID for all tasks.
     *
     * @param context Context instance.
     */
    private static void upgradeToV8(Context context) {
        if (!hasUpgradedToVersion(8, context)) {
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
            saveBooleanPreference(V8_UPGRADE_KEY, true, context);
        }
    }

    /**
     * Clears invalid notification data.
     *
     * @param context Context instance.
     */
    private static void upgradeToV21(Context context) {
        if (!hasUpgradedToVersion(21, context)) {
            // Clear notification data.
            PreferenceUtils.remove(NotificationsReceiver.KEY_EXPIRED_TASKS, context);
            PreferenceUtils.remove(NotificationsReceiver.KEY_PREVIOUS_COUNT, context);

            // Mark as upgraded.
            saveBooleanPreference(V21_UPGRADE_KEY, true, context);
        }
    }

}
