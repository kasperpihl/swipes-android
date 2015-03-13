package com.swipesapp.android.analytics;

/**
 * Holds the event labels to track in analytics.
 *
 * @author Felipe Bari
 */
public class Labels {

    // Session.
    public static final String APP_LAUNCH_DIRECT = "Direct";
    public static final String APP_LAUNCH_LOCAL_NOTIFICATION = "Local Notification";
    public static final String APP_LAUNCH_PUSH_NOTIFICATION = "Push Notification";

    // Onboarding.
    public static final String TRY_OUT_YES = "Yes";
    public static final String TRY_OUT_NO = "No";

    // Tasks - Add.
    public static final String ADDED_FROM_EVERNOTE = "Evernote";
    public static final String ADDED_FROM_INPUT = "Input";
    public static final String ADDED_FROM_SHARE_INTENT = "Share Intent";

    // Tasks - Snooze.
    public static final String SNOOZED_LATER_TODAY = "Later Today";
    public static final String SNOOZED_THIS_EVENING = "This Evening";
    public static final String SNOOZED_TOMORROW = "Tomorrow";
    public static final String SNOOZED_TWO_DAYS = "In 2 Days";
    public static final String SNOOZED_THIS_WEEKEND = "This Weekend";
    public static final String SNOOZED_NEXT_WEEK = "Next Week";
    public static final String SNOOZED_UNSPECIFIED = "Unspecified";
    public static final String SNOOZED_PICK_DATE = "Calendar";
    public static final String SNOOZED_LOCATION = "Location";

    // Tasks - Repeat.
    public static final String RECURRING_NEVER = "never";
    public static final String RECURRING_EVERY_DAY = "every day";
    public static final String RECURRING_MONDAY_TO_FRIDAY = "mon-fri or sat+sun";
    public static final String RECURRING_EVERY_WEEK = "every week";
    public static final String RECURRING_EVERY_MONTH = "every month";
    public static final String RECURRING_EVERY_YEAR = "every year";

    // Tasks - Priority.
    public static final String PRIORITY_ON = "On";
    public static final String PRIORITY_OFF = "Off";

    // Tasks - Attachment.
    public static final String ATTACHMENT_EVERNOTE = "evernote";
    public static final String ATTACHMENT_DROPBOX = "dropbox";

    // Tags.
    public static final String TAGS_FROM_ADD_TASK = "Add Task";
    public static final String TAGS_FROM_EDIT_TASK = "Edit Task";
    public static final String TAGS_FROM_SELECTION = "Select Tasks";
    public static final String TAGS_FROM_FILTER = "Filter";
    public static final String TAGS_FROM_SHARE_INTENT = "Share Intent";

    // General.
    public static final String DONE_FOR_TODAY = "For Today";
    public static final String DONE_FOR_NOW = "For Now";

    // Settings.
    public static final String THEME_DARK = "Dark";
    public static final String THEME_LIGHT = "Light";

    // Integrations.
    public static final String EVERNOTE_STANDARD = "Standard";
    public static final String EVERNOTE_PREMIUM = "Premium";
    public static final String EVERNOTE_BUSINESS = "Business";

    // Notifications.
    public static final String NOTIFICATION_OPENED = "Opened";
    public static final String NOTIFICATION_SNOOZED = "Snoozed";
    public static final String NOTIFICATION_COMPLETED = "Completed";

}
