package com.swipesapp.android.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.swipesapp.android.db.migration.MigrationAssistant;

/**
 * Utilitary class for shared preferences operations.
 *
 * @author Felipe Bari
 */
public class PreferenceUtils {

    public static final String THEME_KEY = "settings_theme";

    public static final String FIRST_RUN = "app_first_run";

    public static final String WELCOME_SCREEN = "shown_welcome_screen";

    public static final String NOTIFICATIONS_KEY = "settings_enable_notifications";


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
     * Saves a string preference.
     *
     * @param preference Preference to save.
     * @param value      Value to apply.
     * @param context    Context instance.
     */
    public static void saveStringPreference(String preference, String value, Context context) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        settings.edit().putString(preference, value).apply();
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
     * @return True if it's the app's first run.
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
     * Determines if the welcome screen has been shown.
     *
     * @param context Context instance.
     * @return True if it has been shown.
     */
    public static boolean hasShownWelcomeScreen(Context context) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        return !settings.getString(WELCOME_SCREEN, "").isEmpty();
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
                hasUpgraded = settings.getBoolean(MigrationAssistant.V7_UPGRADE_KEY, false);
                break;
            case 8:
                hasUpgraded = settings.getBoolean(MigrationAssistant.V8_UPGRADE_KEY, false);
                break;
            case 9:
                hasUpgraded = settings.getBoolean(MigrationAssistant.V9_UPGRADE_KEY, false);
                break;
        }

        return hasUpgraded;
    }

}
