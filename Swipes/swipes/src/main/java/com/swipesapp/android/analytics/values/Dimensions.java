package com.swipesapp.android.analytics.values;

/**
 * Holds the custom dimensions to track in analytics.
 *
 * @author Felipe Bari
 */
public class Dimensions {

    // Dimension indexes.
    public static final int DIMEN_USER_LEVEL = 1;
    public static final int DIMEN_EVERNOTE_USER_LEVEL = 2;
    public static final int DIMEN_ACTIVE_THEME = 3;
    public static final int DIMEN_RECURRING_TASKS = 4;
    public static final int DIMEN_NUMBER_OF_TAGS = 5;
    public static final int DIMEN_MAILBOX_STATUS = 6;
    public static final int DIMEN_PLATFORM = 7;

    // User level values.
    public static final String VALUE_LEVEL_NONE = "None";
    public static final String VALUE_LEVEL_TRYOUT = "Tryout";
    public static final String VALUE_LEVEL_USER = "User";
    public static final String VALUE_LEVEL_PLUS = "Plus";

    // Evernote user level values.
    public static final String VALUE_EVERNOTE_NOT_INSTALLED = "Not Installed";
    public static final String VALUE_EVERNOTE_NOT_LINKED = "Not Linked";
    public static final String VALUE_EVERNOTE_LINKED = "Linked";
    public static final String VALUE_EVERNOTE_STANDARD = "Standard";
    public static final String VALUE_EVERNOTE_PREMIUM = "Premium";
    public static final String VALUE_EVERNOTE_BUSINESS = "Business";

    // Active theme values.
    public static final String VALUE_THEME_DARK = "Dark";
    public static final String VALUE_THEME_LIGHT = "Light";

    // Mailbox status values.
    public static final String VALUE_MAILBOX_INSTALLED = "Installed";
    public static final String VALUE_MAILBOX_NOT_INSTALLED = "Not Installed";

    // Platform value.
    public static final String VALUE_PLATFORM = "Android";

}
