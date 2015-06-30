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

    // User preferences.
    public static final String THEME_KEY = "settings_theme";
    public static final String FIRST_RUN = "app_first_run";
    public static final String WELCOME_DIALOG = "shown_welcome_screen";
    public static final String SYNC_LAST_UPDATE = "sync_last_update";
    public static final String EVERNOTE_SYNC_KEY = "evernote_sync_device";
    public static final String LAST_SYNC_ID = "last_sync_id";

    // Options.
    public static final String BACKGROUND_SYNC_KEY = "settings_enable_background_sync";
    public static final String SCROLL_TO_ADDED_KEY = "settings_scroll_to_added_task";
    public static final String NOTIFICATIONS_KEY = "settings_enable_notifications";
    public static final String VIBRATIONS_KEY = "settings_enable_vibration";
    public static final String DAILY_REMINDER_KEY = "settings_enable_daily_reminder";
    public static final String WEEKLY_REMINDER_KEY = "settings_enable_weekly_reminder";

    // Analytics data.
    public static final String SENT_DIMENSIONS = "sent_user_dimensions";
    public static final String USER_LEVEL = "user_level";
    public static final String EVERNOTE_LEVEL = "evernote_level";
    public static final String ACTIVE_THEME = "active_theme";
    public static final String RECURRING_COUNT = "recurring_tasks_count";
    public static final String TAGS_COUNT = "tags_count";
    public static final String MAILBOX_STATUS = "mailbox_status";
    public static final String INSTALL_DATE = "app_install_date";
    public static final String FIRST_LAUNCH = "app_first_launch";
    public static final String DID_TRY_OUT = "user_did_try_out";

    // Snooze settings.
    public static final String SNOOZE_DAY_START = "settings_day_start";
    public static final String SNOOZE_EVENING_START = "settings_evening_start";
    public static final String SNOOZE_WEEKEND_DAY_START = "settings_weekend_day_start";
    public static final String SNOOZE_WEEK_START = "settings_snoozes_week_start_dow";
    public static final String SNOOZE_WEEKEND_START = "settings_snoozes_weekend_start_dow";
    public static final String SNOOZE_LATER_TODAY = "settings_snoozes_later_today_value";

    /**
     * Saves a boolean preference.
     *
     * @param preference Preference to save.
     * @param value      Value to apply.
     * @param context    Context instance.
     */
    public static void saveBoolean(String preference, boolean value, Context context) {
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
    public static void saveString(String preference, String value, Context context) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        settings.edit().putString(preference, value).apply();
    }

    /**
     * Saves a long preference.
     *
     * @param preference Preference to save.
     * @param value      Value to apply.
     * @param context    Context instance.
     */
    public static void saveLong(String preference, Long value, Context context) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        settings.edit().putLong(preference, value).apply();
    }

    /**
     * Saves an int preference.
     *
     * @param preference Preference to save.
     * @param value      Value to apply.
     * @param context    Context instance.
     */
    public static void saveInt(String preference, int value, Context context) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        settings.edit().putInt(preference, value).apply();
    }

    /**
     * Removes a preference.
     *
     * @param preference Preference to remove.
     * @param context    Context instance.
     */
    public static void remove(String preference, Context context) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        settings.edit().remove(preference).apply();
    }

    /**
     * Reads a string preference.
     *
     * @param context    Context instance.
     * @param preference Preference to read.
     * @return String value.
     */
    public static String readString(String preference, Context context) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        return settings.getString(preference, null);
    }

    /**
     * Reads a long preference.
     *
     * @param context    Context instance.
     * @param preference Preference to read.
     * @return Long value.
     */
    public static long readLong(String preference, Context context) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        return settings.getLong(preference, 0);
    }

    /**
     * Reads an int preference.
     *
     * @param context    Context instance.
     * @param preference Preference to read.
     * @return Int value.
     */
    public static int readInt(String preference, Context context) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        return settings.getInt(preference, 0);
    }

    /**
     * Reads a boolean preference.
     *
     * @param context    Context instance.
     * @param preference Preference to read.
     * @return Boolean value.
     */
    public static boolean readBoolean(String preference, Context context) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        return settings.getBoolean(preference, false);
    }

    /**
     * Determines if a user is running the app for the first time. This is reset when the user logs out.
     *
     * @param context Context instance.
     * @return True if it's the app's first run.
     */
    public static boolean isUserFirstRun(Context context) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        boolean isFirstRun = settings.getString(FIRST_RUN, "").isEmpty();

        if (isFirstRun) {
            settings.edit().putString(FIRST_RUN, "NO").apply();
        }

        return isFirstRun;
    }

    /**
     * Determines if the app is launching for the first time.
     *
     * @param context Context instance.
     * @return True if it's the app's first launch.
     */
    public static boolean isFirstLaunch(Context context) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        boolean isFirstLaunch = settings.getString(FIRST_LAUNCH, "").isEmpty();

        if (isFirstLaunch) {
            settings.edit().putString(FIRST_LAUNCH, "NO").apply();
        }

        return isFirstLaunch;
    }

    /**
     * Determines if the welcome screen has been shown.
     *
     * @param context Context instance.
     * @return True if it has been shown.
     */
    public static boolean hasShownWelcomeScreen(Context context) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        return !settings.getString(WELCOME_DIALOG, "").isEmpty();
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
     * Check if vibration is enabled.
     *
     * @param context Context instance.
     * @return True if it's enabled in the app.
     */
    public static boolean isVibrationEnabled(Context context) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        return settings.getBoolean(VIBRATIONS_KEY, true);
    }

    /**
     * Check if daily reminder is enabled.
     *
     * @param context Context instance.
     * @return True if it's enabled in the app.
     */
    public static boolean isDailyReminderEnabled(Context context) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        return settings.getBoolean(DAILY_REMINDER_KEY, true);
    }

    /**
     * Check if weekly reminder is enabled.
     *
     * @param context Context instance.
     * @return True if it's enabled in the app.
     */
    public static boolean isWeeklyReminderEnabled(Context context) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        return settings.getBoolean(WEEKLY_REMINDER_KEY, true);
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
            case 21:
                hasUpgraded = settings.getBoolean(MigrationAssistant.V21_UPGRADE_KEY, false);
                break;
        }

        return hasUpgraded;
    }

    /**
     * Reads sync last update.
     *
     * @param context Context instance.
     * @return Last update.
     */
    public static String getSyncLastUpdate(Context context) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        return settings.getString(SYNC_LAST_UPDATE, null);
    }

    /**
     * Check if Evernote sync is enabled.
     *
     * @param context Context instance.
     * @return True if it's enabled in the app.
     */
    public static boolean isEvernoteSyncEnabled(Context context) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        return settings.getBoolean(EVERNOTE_SYNC_KEY, true);
    }

    /**
     * Check if background sync is enabled.
     *
     * @param context Context instance.
     * @return True if it's enabled in the app.
     */
    public static boolean isBackgroundSyncEnabled(Context context) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        return settings.getBoolean(BACKGROUND_SYNC_KEY, true);
    }

    /**
     * Check if auto-scroll is enabled.
     *
     * @param context Context instance.
     * @return True if it's enabled in the app.
     */
    public static boolean isAutoScrollEnabled(Context context) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        return settings.getBoolean(SCROLL_TO_ADDED_KEY, true);
    }

    /**
     * Checks if the app has sent initial user dimensions.
     *
     * @param context Context instance.
     * @return True if it has sent.
     */
    public static boolean hasSentUserDimensions(Context context) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        return settings.getBoolean(SENT_DIMENSIONS, false);
    }

    /**
     * Checks if the user has tried out the app.
     *
     * @param context Context instance.
     * @return True if it has tried out.
     */
    public static boolean hasTriedOut(Context context) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        return settings.getBoolean(DID_TRY_OUT, false);
    }

}
