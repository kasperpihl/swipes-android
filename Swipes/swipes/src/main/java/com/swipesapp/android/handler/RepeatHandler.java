package com.swipesapp.android.handler;

import android.content.Context;

import com.swipesapp.android.sync.gson.GsonTag;
import com.swipesapp.android.sync.gson.GsonTask;
import com.swipesapp.android.sync.service.TasksService;
import com.swipesapp.android.util.DateUtils;
import com.swipesapp.android.values.RepeatOptions;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.UUID;

/**
 * Handler to deal with repeating tasks.
 *
 * @author Felipe Bari
 */
public class RepeatHandler {

    private TasksService mTasksService;

    public RepeatHandler(Context context) {
        mTasksService = TasksService.getInstance();
    }

    public void handleRepeatedTask(GsonTask task) {
        // Create a copy of the original task.
        String tempId = UUID.randomUUID().toString();
        GsonTask copy = GsonTask.gsonForLocal(null, null, tempId, null, task.getLocalCreatedAt(), task.getLocalUpdatedAt(), task.isDeleted(), task.getTitle(), task.getNotes(), task.getOrder(), task.getPriority(), task.getLocalCompletionDate(), task.getLocalSchedule(), task.getLocation(), task.getLocalRepeatDate(), RepeatOptions.NEVER.getValue(), task.getOrigin(), task.getOriginIdentifier(), task.getTags(), null, task.getItemId());

        // Determine next repeat date.
        if (task.getRepeatOption().equals(RepeatOptions.NEVER.getValue())) {
            // Do nothing.
            return;
        } else if (task.getRepeatOption().equals(RepeatOptions.EVERY_DAY.getValue())) {
            // Set interval to next day.
            setInterval(task, 86400000L);
        } else if (task.getRepeatOption().equals(RepeatOptions.MONDAY_TO_FRIDAY.getValue())) {
            // Set interval to next day.
            setInterval(task, 86400000L);
            // Handle weekend.
            handleWeekend(task);
        } else if (task.getRepeatOption().equals(RepeatOptions.EVERY_WEEK.getValue())) {
            // Set interval to next week.
            setInterval(task, 604800000L);
        } else if (task.getRepeatOption().equals(RepeatOptions.EVERY_MONTH.getValue())) {
            // Set interval to next month.
            setMonthlyInterval(task);
        } else if (task.getRepeatOption().equals(RepeatOptions.EVERY_YEAR.getValue())) {
            // Set interval to next year.
            setYearlyInterval(task);
        }

        // Save changes to the database.
        mTasksService.saveTask(task, true);
        mTasksService.saveTask(copy, true);

        // Handle subtasks.
        handleRepeatedSubtasks(task.getTempId(), copy.getTempId());
    }

    private void handleRepeatedSubtasks(String taskId, String copyId) {
        for (GsonTask subtask : mTasksService.loadSubtasksForTask(taskId)) {
            String tempId = UUID.randomUUID().toString();

            // Associate subtask copies with task copy, and uncomplete the originals.
            GsonTask copy = GsonTask.gsonForLocal(null, null, tempId, copyId, subtask.getLocalCreatedAt(), subtask.getLocalUpdatedAt(), false, subtask.getTitle(), null, 0, 0, subtask.getLocalCompletionDate(), subtask.getLocalSchedule(), null, null, RepeatOptions.NEVER.getValue(), null, null, new ArrayList<GsonTag>(), null, 0);
            subtask.setLocalCompletionDate(null);

            mTasksService.saveTask(copy, true);
            mTasksService.saveTask(subtask, true);
        }
    }

    private void setInterval(GsonTask task, long interval) {
        Calendar nextDate = Calendar.getInstance();
        nextDate.setTime(task.getLocalRepeatDate());

        // Add interval until the time set is in the future.
        while (!DateUtils.isNewerThanToday(nextDate.getTime())) {
            long timeInMillis = nextDate.getTimeInMillis();
            nextDate.setTimeInMillis(timeInMillis + interval);
        }

        modifyTask(task, nextDate.getTime());
    }

    private void setMonthlyInterval(GsonTask task) {
        Calendar nextDate = Calendar.getInstance();
        nextDate.setTime(task.getLocalRepeatDate());

        // Add a month until the time set is in the future.
        while (!DateUtils.isNewerThanToday(nextDate.getTime())) {
            nextDate.add(Calendar.MONTH, 1);
        }

        modifyTask(task, nextDate.getTime());
    }

    private void setYearlyInterval(GsonTask task) {
        Calendar nextDate = Calendar.getInstance();
        nextDate.setTime(task.getLocalRepeatDate());

        // Add a year until the time set is in the future.
        while (!DateUtils.isNewerThanToday(nextDate.getTime())) {
            nextDate.add(Calendar.YEAR, 1);
        }

        modifyTask(task, nextDate.getTime());
    }

    private void handleWeekend(GsonTask task) {
        Calendar date = Calendar.getInstance();
        date.setTime(task.getLocalRepeatDate());

        long timeInMillis = date.getTimeInMillis();

        // Set interval to next weekday if needed.
        switch (date.get(Calendar.DAY_OF_WEEK)) {
            case Calendar.SATURDAY:
                date.setTimeInMillis(timeInMillis + 172800000L);
                break;
            case Calendar.SUNDAY:
                date.setTimeInMillis(timeInMillis + 86400000L);
                break;
        }

        modifyTask(task, date.getTime());
    }

    private void modifyTask(GsonTask task, Date date) {
        task.setLocalRepeatDate(date);
        task.setLocalSchedule(date);
        task.setLocalCompletionDate(null);
    }

}
