package com.swipesapp.android.values;

/**
 * Holds the possible intent actions.
 *
 * @author Felipe Bari
 */
public class Intents {

    /**
     * Notifies a change in the tasks.
     */
    public static final String TASKS_CHANGED = "com.swipesapp.android.ACTION_TASKS_CHANGED";

    /**
     * Notifies a change in the selected tab.
     */
    public static final String TAB_CHANGED = "com.swipesapp.android.ACTION_TAB_CHANGED";

    /**
     * Notifies listeners to assign tags.
     */
    public static final String ASSIGN_TAGS = "com.swipesapp.android.ACTION_ASSIGN_TAGS";

    /**
     * Notifies listeners to delete tasks.
     */
    public static final String DELETE_TASKS = "com.swipesapp.android.ACTION_DELETE_TASKS";

    /**
     * Notifies listeners to share tasks.
     */
    public static final String SHARE_TASKS = "com.swipesapp.android.ACTION_SHARE_TASKS";

    /**
     * Notifies listeners that the device has booted.
     */
    public static final String BOOT_COMPLETED = "android.intent.action.BOOT_COMPLETED";

    /**
     * Notifies listeners to show tasks.
     */
    public static final String SHOW_TASKS = "com.swipesapp.android.ACTION_SHOW_TASKS";

    /**
     * Notifies listeners to snooze tasks.
     */
    public static final String SNOOZE_TASKS = "com.swipesapp.android.ACTION_SNOOZE_TASKS";

    /**
     * Notifies listeners to complete tasks.
     */
    public static final String COMPLETE_TASKS = "com.swipesapp.android.ACTION_COMPLETE_TASKS";

    /**
     * Notifies that hardware back button has been pressed.
     */
    public static final String BACK_PRESSED = "com.swipesapp.android.ACTION_BACK_PRESSED";

    /**
     * Notifies listeners that selection has started.
     */
    public static final String SELECTION_STARTED = "com.swipesapp.android.SELECTION_STARTED";

    /**
     * Notifies listeners that selection has been cleared.
     */
    public static final String SELECTION_CLEARED = "com.swipesapp.android.SELECTION_CLEARED";

    /**
     * Notifies listeners to filter list by tags.
     */
    public static final String FILTER_BY_TAGS = "com.swipesapp.android.FILTER_BY_TAGS";

    /**
     * Notifies listeners to perform search.
     */
    public static final String PERFORM_SEARCH = "com.swipesapp.android.ACTION_PERFORM_SEARCH";

    /**
     * Notifies listeners to add task.
     */
    public static final String ADD_TASK = "com.swipesapp.android.ACTION_ADD_TASK";

}
