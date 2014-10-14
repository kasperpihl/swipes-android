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

/**
 * Handler to deal with repeating tasks.
 *
 * @author Felipe Bari
 */
public class RepeatHandler {

    private TasksService mTasksService;

    public RepeatHandler(Context context) {
        mTasksService = TasksService.getInstance(context);
    }

    public void handleRepeatedTask(GsonTask task) {
        // Create a copy of the original task.
        String tempId = task.getTitle() + new Date();
        GsonTask copy = new GsonTask(null, null, tempId, null, task.getCreatedAt(), task.getUpdatedAt(), task.isDeleted(), task.getTitle(), task.getNotes(), task.getOrder(), task.getPriority(), task.getCompletionDate(), task.getSchedule(), task.getLocation(), task.getRepeatDate(), RepeatOptions.NEVER.getValue(), task.getOrigin(), task.getOriginIdentifier(), task.getTags(), task.getItemId());

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
        mTasksService.saveTask(task);
        mTasksService.saveTask(copy);

        // Handle subtasks.
        handleRepeatedSubtasks(task.getTempId(), copy.getTempId());
    }

    private void handleRepeatedSubtasks(String taskId, String copyId) {
        for (GsonTask subtask : mTasksService.loadSubtasksForTask(taskId)) {
            String tempId = subtask.getTitle() + new Date();

            // Associate subtask copies with task copy, and uncomplete the originals.
            GsonTask copy = new GsonTask(null, null, tempId, copyId, subtask.getCreatedAt(), subtask.getUpdatedAt(), false, subtask.getTitle(), null, 0, 0, subtask.getCompletionDate(), subtask.getSchedule(), null, null, RepeatOptions.NEVER.getValue(), null, null, new ArrayList<GsonTag>(), 0);
            subtask.setCompletionDate(null);

            mTasksService.saveTask(copy);
            mTasksService.saveTask(subtask);
        }
    }

    private void setInterval(GsonTask task, long interval) {
        Calendar nextDate = Calendar.getInstance();
        nextDate.setTime(task.getRepeatDate());

        // Add interval until the time set is in the future.
        while (!DateUtils.isNewerThanToday(nextDate.getTime())) {
            long timeInMillis = nextDate.getTimeInMillis();
            nextDate.setTimeInMillis(timeInMillis + interval);
        }

        modifyTask(task, nextDate.getTime());
    }

    private void setMonthlyInterval(GsonTask task) {
        Calendar nextDate = Calendar.getInstance();
        nextDate.setTime(task.getRepeatDate());

        // Add a month until the time set is in the future.
        while (!DateUtils.isNewerThanToday(nextDate.getTime())) {
            nextDate.add(Calendar.MONTH, 1);
        }

        modifyTask(task, nextDate.getTime());
    }

    private void setYearlyInterval(GsonTask task) {
        Calendar nextDate = Calendar.getInstance();
        nextDate.setTime(task.getRepeatDate());

        // Add a year until the time set is in the future.
        while (!DateUtils.isNewerThanToday(nextDate.getTime())) {
            nextDate.add(Calendar.YEAR, 1);
        }

        modifyTask(task, nextDate.getTime());
    }

    private void handleWeekend(GsonTask task) {
        Calendar date = Calendar.getInstance();
        date.setTime(task.getRepeatDate());

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
        task.setRepeatDate(date);
        task.setSchedule(date);
        task.setCompletionDate(null);
    }

}
