package com.swipesapp.android.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

/**
 * Utilitary class for shared preferences operations.
 *
 * @author Felipe Bari
 */
public class PreferenceUtils {

    public static final String THEME_KEY = "settings_theme";

    public static final String FIRST_RUN = "app_first_run";

    public static final String NOTIFICATIONS_KEY = "settings_enable_notifications";

    public static final String V7_UPGRADE_KEY = "v7_upgrade_performed";

    /**
     * Saves a boolean preference.
     *
     * @param preference Preference to save.
     * @param value      Value to apply.
     * @param context    Context instance.
     */
    public static void saveBooleanPreference(String preference, boolean value, Context context) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        settings.edit().putBoolean(preference, value).apply();
    }

    /**
     * Reads theme setting.
     *
     * @param context Context instance.
     * @return Current theme.
     */
    public static String readThemeSetting(Context context) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        return settings.getString(THEME_KEY, null);
    }

    /**
     * Determines if it is the app's first run.
     *
     * @param context Context instance.
     * @return True it is the app's first run.
     */
    public static boolean isFirstRun(Context context) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        boolean isFirstRun = settings.getString(FIRST_RUN, "").isEmpty();

        if (isFirstRun) {
            settings.edit().putString(FIRST_RUN, "NO").apply();
        }

        return isFirstRun;
    }

    /**
     * Check if notifications are enabled.
     *
     * @param context Context instance.
     * @return True if they are enabled in the app.
     */
    public static boolean areNotificationsEnabled(Context context) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        return settings.getBoolean(NOTIFICATIONS_KEY, true);
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
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);

        switch (version) {
            case 7:
                hasUpgraded = settings.getBoolean(V7_UPGRADE_KEY, false);
                break;
        }

        return hasUpgraded;
    }

}
