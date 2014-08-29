package com.swipesapp.android.values;

/**
 * Holds the possible intent actions.
 *
 * @author Felipe Bari
 */
public class Actions {

    /**
     * Notifies a change in the tasks.
     */
    public static final String TASKS_CHANGED = "com.swipesapp.android.ACTION_TASKS_CHANGED";

    /**
     * Notifies a change in the selected tab.
     */
    public static final String TAB_CHANGED = "com.swipesapp.android.ACTION_TAB_CHANGED";

    /**
     * Notifies listeners to edit task.
     */
    public static final String EDIT_TASK = "com.swipesapp.android.ACTION_EDIT_TASK";

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
     * Notifies listeners to snooze tasks.
     */
    public static final String SNOOZE_TASKS = "com.swipesapp.android.ACTION_SNOOZE_TASKS";

    /**
     * Notifies listeners to complete tasks.
     */
    public static final String COMPLETE_TASKS = "com.swipesapp.android.ACTION_COMPLETE_TASKS";

}
