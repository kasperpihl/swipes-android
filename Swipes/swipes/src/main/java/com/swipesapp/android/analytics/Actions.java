package com.swipesapp.android.analytics;

/**
 * Holds the event actions to track in analytics.
 *
 * @author Felipe Bari
 */
public class Actions {

    // Session.
    public static final String APP_LAUNCH = "App Launch";

    // Onboarding.
    public static final String INSTALLATION = "Installation";
    public static final String TRYING_OUT = "Trying Out";
    public static final String SIGNED_UP = "Signed Up";
    public static final String LOGGED_IN = "Logged In";

    // Action Steps.
    public static final String ADDED_SUBTASK = "Added";
    public static final String COMPLETED_SUBTASK = "Completed";
    public static final String DELETED_SUBTASK = "Deleted";

    // Tasks.
    public static final String ADDED_TASK = "Added";
    public static final String COMPLETED_TASKS = "Completed";
    public static final String SNOOZED_TASK = "Snoozed";
    public static final String DELETED_TASKS = "Deleted";
    public static final String RECURRING = "Recurring";
    public static final String PRIORITY = "Priority";
    public static final String NOTE = "Note";
    public static final String ATTACHMENT = "Attachment";

    // Tags.
    public static final String ADDED_TAG = "Added";
    public static final String DELETED_TAG = "Deleted";
    public static final String ASSIGNED_TAGS = "Assigned";
    public static final String UNASSIGNED_TAGS = "Unassigned";

    // General.
    public static final String OPEN_EVERNOTE = "Open In Evernote";
    public static final String CLEARED_TASKS = "Cleared Tasks";

    // Settings.
    public static final String CHANGED_THEME = "Changed Theme";

    // Share Task.
    public static final String SHARE_TASK_OPEN = "Opened";
    public static final String SHARE_TASK_SENT = "Sent";

    // Sharing.
    public static final String SHARE_MESSAGE_OPEN = "Opened";
    public static final String SHARE_MESSAGE_SENT = "Sent";

    // Integrations.
    public static final String LINKED_EVERNOTE = "Linked Evernote";
    public static final String LINKED_GMAIL = "Linked Gmail";

    // Notifications.
    public static final String NOTIFICATION_CLICKED = "Notification Clicked";

}
