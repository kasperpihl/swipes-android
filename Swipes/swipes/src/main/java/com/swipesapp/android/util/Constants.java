package com.swipesapp.android.util;

/**
 * Holds application constants.
 */
public class Constants {

    /**
     * Swipes font name.
     */
    public static final String FONT_NAME = "swipes.ttf";

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
     * Result code for theme changed.
     */
    public static final int THEME_CHANGED_RESULT_CODE = 1;

    /**
     * Result code for account changed.
     */
    public static final int ACCOUNT_CHANGED_RESULT_CODE = 2;

}
