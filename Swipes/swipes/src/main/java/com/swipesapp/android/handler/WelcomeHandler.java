package com.swipesapp.android.handler;

import android.content.Context;

import com.swipesapp.android.R;
import com.swipesapp.android.sync.gson.GsonTag;
import com.swipesapp.android.sync.gson.GsonTask;
import com.swipesapp.android.sync.service.SyncService;
import com.swipesapp.android.sync.service.TasksService;
import com.swipesapp.android.values.RepeatOptions;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Date;
import java.util.UUID;

/**
 * Handler to deal with welcome tasks.
 *
 * @author Felipe Bari
 */
public class WelcomeHandler {

    private WeakReference<Context> mContext;

    public WelcomeHandler(Context context) {
        mContext = new WeakReference<Context>(context);
    }

    public void addWelcomeTasks() {
        TasksService tasksService = TasksService.getInstance();
        SyncService syncService = SyncService.getInstance();
        Date currentDate = new Date();

        GsonTask task = GsonTask.gsonForLocal(null, null, null, null, currentDate, currentDate, false, null, null, 0, 0,
                null, currentDate, null, null, RepeatOptions.NEVER.getValue(), null, null, new ArrayList<GsonTag>(), null, 0);

        // Save first task.
        String title = mContext.get().getString(R.string.welcome_task_one);
        String tempId = UUID.randomUUID().toString();
        task.setTitle(title);
        task.setTempId(tempId);

        syncService.saveTaskChangesForSync(task, null);
        tasksService.saveTask(task, false);

        // Save second task.
        title = mContext.get().getString(R.string.welcome_task_two);
        tempId = UUID.randomUUID().toString();
        task.setTitle(title);
        task.setTempId(tempId);

        syncService.saveTaskChangesForSync(task, null);
        tasksService.saveTask(task, false);

        // Save third task.
        title = mContext.get().getString(R.string.welcome_task_three);
        tempId = UUID.randomUUID().toString();
        task.setTitle(title);
        task.setTempId(tempId);

        syncService.saveTaskChangesForSync(task, null);
        tasksService.saveTask(task, false);

        GsonTag tag = GsonTag.gsonForLocal(null, null, null, currentDate, currentDate, null);

        // Save first tag.
        title = mContext.get().getString(R.string.welcome_tag_one);
        tempId = UUID.randomUUID().toString();
        tag.setTitle(title);
        tag.setTempId(tempId);

        syncService.saveTagForSync(tag);
        tasksService.saveTag(tag);

        // Save second tag.
        title = mContext.get().getString(R.string.welcome_tag_two);
        tempId = UUID.randomUUID().toString();
        tag.setTitle(title);
        tag.setTempId(tempId);

        syncService.saveTagForSync(tag);
        tasksService.saveTag(tag);
    }

}
