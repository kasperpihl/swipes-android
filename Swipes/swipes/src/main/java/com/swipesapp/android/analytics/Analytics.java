package com.swipesapp.android.analytics;

import android.content.Context;
import android.util.Log;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.parse.ParseUser;
import com.swipesapp.android.BuildConfig;
import com.swipesapp.android.app.SwipesApplication;
import com.swipesapp.android.evernote.EvernoteService;
import com.swipesapp.android.sync.service.TasksService;
import com.swipesapp.android.util.DeviceUtils;
import com.swipesapp.android.util.PreferenceUtils;
import com.swipesapp.android.util.ThemeUtils;

/**
 * Convenience class to handle analytics calls.
 *
 * @author Felipe Bari
 */
public class Analytics {

    private static final String LOG_TAG = Analytics.class.getSimpleName();

    private static final String EVERNOTE_PACKAGE = "com.evernote";
    private static final String MAILBOX_PACKAGE = "com.mailboxapp";

    /**
     * Sends a screen view event.
     *
     * @param screenName Screen name to send.
     */
    public static void sendScreenView(String screenName) {
        Tracker tracker = SwipesApplication.getTracker();
        tracker.setScreenName(screenName);
        tracker.send(getBaseScreenBuilder().build());

        logDebugMessage("Sent screen view: " + screenName);
    }

    /**
     * Sends initial user dimensions only once after the app
     * has been started for the first time.
     *
     * @param context Context instance.
     */
    public static void startUserDimensions(Context context) {
        if (!PreferenceUtils.hasSentUserDimensions(context)) {
            sendUserLevel(context);
            sendActiveTheme(context);
            sendRecurringTasks(context);
            sendNumberOfTags(context);
            sendMailboxStatus(context);

            PreferenceUtils.saveBooleanPreference(PreferenceUtils.SENT_DIMENSIONS, true, context);
        }
    }

    /**
     * Sends updated user level dimension.
     *
     * @param context Context instance.
     */
    public static void sendUserLevel(Context context) {
        String userLevel = Dimensions.VALUE_LEVEL_NONE;

        if (PreferenceUtils.hasShownWelcomeScreen(context)) {
            if (ParseUser.getCurrentUser() != null) {
                userLevel = Dimensions.VALUE_LEVEL_USER;
            } else {
                userLevel = Dimensions.VALUE_LEVEL_TRYOUT;
            }
        }

        Tracker tracker = SwipesApplication.getTracker();
        tracker.setScreenName(null);
        tracker.send(getBaseScreenBuilder()
                .setCustomDimension(Dimensions.DIMEN_USER_LEVEL, userLevel)
                .setNewSession()
                .build());

        logDebugMessage("Sent dimension: User Level - " + userLevel);
    }

    /**
     * Sends updated Evernote user level dimension.
     *
     * @param context Context instance.
     */
    public static void sendEvernoteUserLevel(Context context) {
        String userLevel = Dimensions.VALUE_EVERNOTE_NOT_INSTALLED;
        String previousLevel = PreferenceUtils.readStringPreference(PreferenceUtils.EVERNOTE_LEVEL, context);

        if (EvernoteService.getInstance().isAuthenticated()) {
            // TODO: Get account type from Evernote.
            userLevel = Dimensions.VALUE_EVERNOTE_LINKED;
        } else if (DeviceUtils.isAppInstalled(EVERNOTE_PACKAGE, context)) {
            userLevel = Dimensions.VALUE_EVERNOTE_NOT_LINKED;
        }

        if (!userLevel.equals(previousLevel)) {
            PreferenceUtils.saveStringPreference(PreferenceUtils.EVERNOTE_LEVEL, userLevel, context);

            Tracker tracker = SwipesApplication.getTracker();
            tracker.setScreenName(null);
            tracker.send(getBaseScreenBuilder()
                    .setCustomDimension(Dimensions.DIMEN_EVERNOTE_USER_LEVEL, userLevel)
                    .setNewSession()
                    .build());

            logDebugMessage("Sent dimension: Evernote User Level - " + userLevel);
        }
    }

    /**
     * Sends updated active theme dimension.
     *
     * @param context Context instance.
     */
    public static void sendActiveTheme(Context context) {
        String theme = ThemeUtils.isLightTheme(context) ? Dimensions.VALUE_THEME_LIGHT : Dimensions.VALUE_THEME_DARK;

        Tracker tracker = SwipesApplication.getTracker();
        tracker.setScreenName(null);
        tracker.send(getBaseScreenBuilder()
                .setCustomDimension(Dimensions.DIMEN_ACTIVE_THEME, theme)
                .setNewSession()
                .build());

        logDebugMessage("Sent dimension: Active Theme - " + theme);
    }

    /**
     * Sends updated recurring tasks dimension.
     *
     * @param context Context instance.
     */
    public static void sendRecurringTasks(Context context) {
        String currentCount = String.valueOf(TasksService.getInstance().countRecurringTasks());
        String previousCount = PreferenceUtils.readStringPreference(PreferenceUtils.RECURRING_COUNT, context);

        if (!currentCount.equals(previousCount)) {
            PreferenceUtils.saveStringPreference(PreferenceUtils.RECURRING_COUNT, currentCount, context);

            Tracker tracker = SwipesApplication.getTracker();
            tracker.setScreenName(null);
            tracker.send(getBaseScreenBuilder()
                    .setCustomDimension(Dimensions.DIMEN_RECURRING_TASKS, currentCount)
                    .setNewSession()
                    .build());

            logDebugMessage("Sent dimension: Recurring Tasks - " + currentCount);
        }
    }

    /**
     * Sends updated number of tags dimension.
     *
     * @param context Context instance.
     */
    public static void sendNumberOfTags(Context context) {
        String currentCount = String.valueOf(TasksService.getInstance().countAllTags());
        String previousCount = PreferenceUtils.readStringPreference(PreferenceUtils.TAGS_COUNT, context);

        if (!currentCount.equals(previousCount)) {
            PreferenceUtils.saveStringPreference(PreferenceUtils.TAGS_COUNT, currentCount, context);

            Tracker tracker = SwipesApplication.getTracker();
            tracker.setScreenName(null);
            tracker.send(getBaseScreenBuilder()
                    .setCustomDimension(Dimensions.DIMEN_NUMBER_OF_TAGS, currentCount)
                    .setNewSession()
                    .build());

            logDebugMessage("Sent dimension: Number of Tags - " + currentCount);
        }
    }

    /**
     * Sends updated Mailbox status dimension.
     *
     * @param context Context instance.
     */
    public static void sendMailboxStatus(Context context) {
        boolean isInstalled = DeviceUtils.isAppInstalled(MAILBOX_PACKAGE, context);
        String status = isInstalled ? Dimensions.VALUE_MAILBOX_INSTALLED : Dimensions.VALUE_MAILBOX_NOT_INSTALLED;
        String previousStatus = PreferenceUtils.readStringPreference(PreferenceUtils.MAILBOX_STATUS, context);

        if (!status.equals(previousStatus)) {
            PreferenceUtils.saveStringPreference(PreferenceUtils.MAILBOX_STATUS, status, context);

            Tracker tracker = SwipesApplication.getTracker();
            tracker.setScreenName(null);
            tracker.send(getBaseScreenBuilder()
                    .setCustomDimension(Dimensions.DIMEN_MAILBOX_STATUS, status)
                    .setNewSession()
                    .build());

            logDebugMessage("Sent dimension: Mailbox Status - " + status);
        }
    }

    /**
     * Returns a base ScreenViewBuilder including platform dimension.
     *
     * @return ScreenViewBuilder instance.
     */
    private static HitBuilders.ScreenViewBuilder getBaseScreenBuilder() {
        return new HitBuilders.ScreenViewBuilder()
                .setCustomDimension(Dimensions.DIMEN_PLATFORM, Dimensions.VALUE_PLATFORM);
    }

    /**
     * Sends a log message in debug builds.
     *
     * @param message Message to log.
     */
    private static void logDebugMessage(String message) {
        if (BuildConfig.DEBUG) {
            Log.d(LOG_TAG, message);
        }
    }

}
