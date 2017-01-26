package com.swipesapp.android.handler;

import android.content.Context;

import com.swipesapp.android.R;
import com.swipesapp.android.analytics.handler.Analytics;
import com.swipesapp.android.analytics.values.Actions;
import com.swipesapp.android.analytics.values.Categories;
import com.swipesapp.android.sync.gson.GsonTag;
import com.swipesapp.android.sync.gson.GsonTask;
import com.swipesapp.android.sync.service.SyncService;
import com.swipesapp.android.sync.service.TasksService;
import com.swipesapp.android.util.DateUtils;
import com.swipesapp.android.util.PreferenceUtils;
import com.swipesapp.android.values.RepeatOptions;

import java.util.ArrayList;
import java.util.Date;
import java.util.UUID;

/**
 * Handler to deal with welcome actions.
 *
 * @author Fernanda Bari
 */
public class WelcomeHandler {

    /**
     * Checks if the app is launching for the first time and saves the date.
     *
     * @param context Context instance.
     */
    public static void checkFirstLaunch(Context context) {
        // Save install date on first launch.
        if (PreferenceUtils.isFirstLaunch(context)) {
            String installDate = DateUtils.dateToSync(new Date());
            PreferenceUtils.saveString(PreferenceUtils.INSTALL_DATE, installDate, context);

            // Send installation event.
            Analytics.sendEvent(Categories.ONBOARDING, Actions.INSTALLATION, null, null);
        }
    }

    /**
     * Adds the welcome tasks at the first run for a user.
     *
     * @param context Context instance.
     */
    public static void addWelcomeTasks(Context context) {
        // Save only at first run for the user.
        if (PreferenceUtils.isUserFirstRun(context)) {
            TasksService tasksService = TasksService.getInstance();
            SyncService syncService = SyncService.getInstance();
            Date currentDate = new Date();

            GsonTask task = GsonTask.gsonForLocal(null, null, null, null, currentDate, currentDate, false, null, null, 0, 0,
                    null, currentDate, null, null, RepeatOptions.NEVER, null, null, new ArrayList<GsonTag>(), null, 0);

            // Save first task.
            String title = context.getString(R.string.welcome_task_one);
            String tempId = UUID.randomUUID().toString();
            task.setTitle(title);
            task.setTempId(tempId);

            syncService.saveTaskChangesForSync(task, null);
            tasksService.saveTask(task, false);

            // Save second task.
            title = context.getString(R.string.welcome_task_two);
            tempId = UUID.randomUUID().toString();
            task.setTitle(title);
            task.setTempId(tempId);

            syncService.saveTaskChangesForSync(task, null);
            tasksService.saveTask(task, false);

            // Save third task.
            title = context.getString(R.string.welcome_task_three);
            tempId = UUID.randomUUID().toString();
            task.setTitle(title);
            task.setTempId(tempId);

            syncService.saveTaskChangesForSync(task, null);
            tasksService.saveTask(task, false);

            GsonTag tag = GsonTag.gsonForLocal(null, null, null, currentDate, currentDate, null);

            // Save first tag.
            title = context.getString(R.string.welcome_tag_one);
            tempId = UUID.randomUUID().toString();
            tag.setTitle(title);
            tag.setTempId(tempId);

            syncService.saveTagForSync(tag);
            tasksService.saveTag(tag);

            // Save second tag.
            title = context.getString(R.string.welcome_tag_two);
            tempId = UUID.randomUUID().toString();
            tag.setTitle(title);
            tag.setTempId(tempId);

            syncService.saveTagForSync(tag);
            tasksService.saveTag(tag);
        }
    }

}
