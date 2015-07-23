package com.swipesapp.android.values;

/**
 * Holds application constants.
 */
public class Constants {

    /**
     * Swipes font name.
     */
    public static final String FONT_NAME = "swipes.ttf";

    /**
     * Default sync delay.
     */
    public static final int SYNC_DELAY = 5;

    /**
     * Alpha value for pressed buttons.
     */
    public static final float PRESSED_BUTTON_ALPHA = 0.6f;

    /**
     * Short animation duration.
     */
    public static final int ANIMATION_DURATION_SHORT = 200;

    /**
     * Medium animation duration.
     */
    public static final int ANIMATION_DURATION_MEDIUM = 400;

    /**
     * Default animation duration.
     */
    public static final int ANIMATION_DURATION_LONG = 500;

    /**
     * Intent extra for section number.
     */
    public static final String EXTRA_SECTION_NUMBER = "SECTION_NUMBER";

    /**
     * Intent extra for task ID.
     */
    public static final String EXTRA_TASK_ID = "TASK_ID";

    /**
     * Intent extra to show action steps.
     */
    public static final String EXTRA_SHOW_ACTION_STEPS = "SHOW_ACTION_STEPS";

    /**
     * Intent extra for app launched from notifications.
     */
    public static final String EXTRA_FROM_NOTIFICATIONS = "FROM_NOTIFICATIONS";

    /**
     * Intent extra for launches from widgets.
     */
    public static final String EXTRA_FROM_WIDGET = "FROM_WIDGET";

    /**
     * Intent extra for tag IDs.
     */
    public static final String EXTRA_TAG_IDS = "TAG_IDS";

    /**
     * Intent extra for snooze time.
     */
    public static final String EXTRA_SNOOZE_TIME = "SNOOZE_TIME";

    /**
     * Intent extra for scheduler new task mode.
     */
    public static final String EXTRA_NEW_TASK_MODE = "NEW_TASK_MODE";

    /**
     * Request code for task snooze.
     */
    public static final int SNOOZE_REQUEST_CODE = 1;

    /**
     * Request code for edit task.
     */
    public static final int EDIT_TASK_REQUEST_CODE = 2;

    /**
     * Request code for login.
     */
    public static final int LOGIN_REQUEST_CODE = 3;

    /**
     * Request code for settings screen.
     */
    public static final int SETTINGS_REQUEST_CODE = 4;

    /**
     * Request code for Evernote attachments.
     */
    public static final int EVERNOTE_ATTACHMENTS_REQUEST_CODE = 5;

    /**
     * Request code for Evernote learn more screen.
     */
    public static final int EVERNOTE_LEARN_REQUEST_CODE = 6;

    /**
     * Request code for add task screen.
     */
    public static final int ADD_TASK_REQUEST_CODE = 7;

    /**
     * Result code for theme changed.
     */
    public static final int THEME_CHANGED_RESULT_CODE = 1;

    /**
     * Result code for account changed.
     */
    public static final int ACCOUNT_CHANGED_RESULT_CODE = 2;

    /**
     * Result code for locale changed.
     */
    public static final int LOCALE_CHANGED_RESULT_CODE = 3;

    /**
     * Result code for snoozed task added.
     */
    public static final int ADDED_SNOOZED_TASK_RESULT_CODE = 4;

}
