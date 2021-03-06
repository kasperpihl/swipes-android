package com.swipesapp.android.analytics.handler;

import android.content.Context;
import android.util.Log;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.parse.ParseUser;
import com.swipesapp.android.BuildConfig;
import com.swipesapp.android.analytics.values.Dimensions;
import com.swipesapp.android.app.SwipesApplication;
import com.swipesapp.android.evernote.EvernoteService;
import com.swipesapp.android.sync.service.TasksService;
import com.swipesapp.android.util.DateUtils;
import com.swipesapp.android.util.DeviceUtils;
import com.swipesapp.android.util.PreferenceUtils;
import com.swipesapp.android.util.ThemeUtils;

import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * Convenience class to handle analytics calls.
 *
 * @author Fernanda Bari
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
     * Sends an action event.
     *
     * @param category Event category.
     * @param action   Event action.
     * @param label    Event label.
     * @param value    Event value.
     */
    public static void sendEvent(String category, String action, String label, Long value) {
        HitBuilders.EventBuilder builder = getBaseEventBuilder()
                .setCategory(category)
                .setAction(action);

        if (label != null) builder.setLabel(label);
        if (value != null) builder.setValue(value);

        Tracker tracker = SwipesApplication.getTracker();
        tracker.send(builder.build());

        logDebugMessage("Sent event: " + category + " - " + action + " - " + label + " - " + value);
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
            sendEvernoteUserLevel(context);
            sendActiveTheme(context);
            sendRecurringTasks(context);
            sendNumberOfTags(context);
            sendMailboxStatus(context);

            PreferenceUtils.saveBoolean(PreferenceUtils.SENT_DIMENSIONS, true, context);
        }
    }

    /**
     * Sends updated user level dimension.
     *
     * @param context Context instance.
     */
    public static void sendUserLevel(Context context) {
        String userLevel = Dimensions.VALUE_LEVEL_NONE;
        String previousLevel = PreferenceUtils.readString(PreferenceUtils.USER_LEVEL, context);

        if (PreferenceUtils.hasShownWelcomeScreen(context)) {
            if (ParseUser.getCurrentUser() != null) {
                userLevel = Dimensions.VALUE_LEVEL_USER;
            } else {
                userLevel = Dimensions.VALUE_LEVEL_TRYOUT;
            }
        }

        if (!userLevel.equals(previousLevel)) {
            PreferenceUtils.saveString(PreferenceUtils.USER_LEVEL, userLevel, context);

            Tracker tracker = SwipesApplication.getTracker();
            tracker.setScreenName(null);
            tracker.send(getBaseScreenBuilder()
                    .setCustomDimension(Dimensions.DIMEN_USER_LEVEL, userLevel)
                    .setNewSession()
                    .build());

            logDebugMessage("Sent dimension: User Level - " + userLevel);
        }
    }

    /**
     * Sends updated Evernote user level dimension.
     *
     * @param context Context instance.
     */
    public static void sendEvernoteUserLevel(Context context) {
        String userLevel = Dimensions.VALUE_EVERNOTE_NOT_INSTALLED;
        String previousLevel = PreferenceUtils.readString(PreferenceUtils.EVERNOTE_LEVEL, context);

        if (EvernoteService.getInstance().isAuthenticated()) {
            // TODO: Get account type from Evernote.
            userLevel = Dimensions.VALUE_EVERNOTE_LINKED;
        } else if (DeviceUtils.isAppInstalled(EVERNOTE_PACKAGE, context)) {
            userLevel = Dimensions.VALUE_EVERNOTE_NOT_LINKED;
        }

        if (!userLevel.equals(previousLevel)) {
            PreferenceUtils.saveString(PreferenceUtils.EVERNOTE_LEVEL, userLevel, context);

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
        String previousTheme = PreferenceUtils.readString(PreferenceUtils.ACTIVE_THEME, context);

        if (!theme.equals(previousTheme)) {
            PreferenceUtils.saveString(PreferenceUtils.ACTIVE_THEME, theme, context);

            Tracker tracker = SwipesApplication.getTracker();
            tracker.setScreenName(null);
            tracker.send(getBaseScreenBuilder()
                    .setCustomDimension(Dimensions.DIMEN_ACTIVE_THEME, theme)
                    .setNewSession()
                    .build());

            logDebugMessage("Sent dimension: Active Theme - " + theme);
        }
    }

    /**
     * Sends updated recurring tasks dimension.
     *
     * @param context Context instance.
     */
    public static void sendRecurringTasks(Context context) {
        String currentCount = String.valueOf(TasksService.getInstance().countRecurringTasks());
        String previousCount = PreferenceUtils.readString(PreferenceUtils.RECURRING_COUNT, context);

        if (!currentCount.equals(previousCount)) {
            PreferenceUtils.saveString(PreferenceUtils.RECURRING_COUNT, currentCount, context);

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
        String previousCount = PreferenceUtils.readString(PreferenceUtils.TAGS_COUNT, context);

        if (!currentCount.equals(previousCount)) {
            PreferenceUtils.saveString(PreferenceUtils.TAGS_COUNT, currentCount, context);

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
        String previousStatus = PreferenceUtils.readString(PreferenceUtils.MAILBOX_STATUS, context);

        if (!status.equals(previousStatus)) {
            PreferenceUtils.saveString(PreferenceUtils.MAILBOX_STATUS, status, context);

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
     * Returns a base EventBuilder including platform dimension.
     *
     * @return EventBuilder instance.
     */
    private static HitBuilders.EventBuilder getBaseEventBuilder() {
        return new HitBuilders.EventBuilder()
                .setCustomDimension(Dimensions.DIMEN_PLATFORM, Dimensions.VALUE_PLATFORM);
    }

    /**
     * Calculates the number of days since the app was installed.
     *
     * @param context Context instance.
     * @return Number of days.
     */
    public static Long getDaysSinceInstall(Context context) {
        long daysDifference = 0;

        String installDateString = PreferenceUtils.readString(PreferenceUtils.INSTALL_DATE, context);
        Date installDate = DateUtils.dateFromSync(installDateString);

        if (installDate != null) {
            daysDifference = DateUtils.getDateDifference(installDate, new Date(), TimeUnit.DAYS);
        }

        return daysDifference;
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
