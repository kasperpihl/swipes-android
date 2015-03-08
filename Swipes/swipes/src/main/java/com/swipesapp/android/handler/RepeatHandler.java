package com.swipesapp.android.handler;

import android.content.Context;

import com.swipesapp.android.db.Task;
import com.swipesapp.android.sync.service.TasksService;
import com.swipesapp.android.util.DateUtils;
import com.swipesapp.android.values.RepeatOptions;

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

    public void handleRepeatedTask(Task task) {
        // Create a copy of the original task.
        String tempId = UUID.randomUUID().toString();
        Task copy = new Task(null, null, tempId, null, task.getCreatedAt(), task.getUpdatedAt(),
                task.isDeleted(), task.getTitle(), task.getNotes(), task.getOrder(), task.getPriority(), task.getCompletionDate(),
                task.getSchedule(), task.getLocation(), task.getRepeatDate(), RepeatOptions.NEVER, task.getOrigin(),
                task.getOriginIdentifier());

        // Copy tags as well.
        copy.setTags(task.getTags());

        String repeatOption = task.getRepeatOption();

        // Determine next repeat date.
        if (repeatOption == null || repeatOption.equals(RepeatOptions.NEVER)) {
            // Do nothing.
            return;
        } else if (repeatOption.equals(RepeatOptions.EVERY_DAY)) {
            // Set interval to next day.
            setInterval(task, 86400000L);
        } else if (repeatOption.equals(RepeatOptions.MONDAY_TO_FRIDAY)) {
            // Set interval to next day.
            setInterval(task, 86400000L);
            // Handle weekend.
            handleWeekend(task);
        } else if (repeatOption.equals(RepeatOptions.EVERY_WEEK)) {
            // Set interval to next week.
            setInterval(task, 604800000L);
        } else if (repeatOption.equals(RepeatOptions.EVERY_MONTH)) {
            // Set interval to next month.
            setMonthlyInterval(task);
        } else if (repeatOption.equals(RepeatOptions.EVERY_YEAR)) {
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
        for (Task subtask : mTasksService.loadSubtasksForTask(taskId)) {
            String tempId = UUID.randomUUID().toString();

            // Associate subtask copies with task copy, and uncomplete the originals.
            Task copy = new Task(null, null, tempId, copyId, subtask.getCreatedAt(),
                    subtask.getUpdatedAt(), false, subtask.getTitle(), null, 0, 0, subtask.getCompletionDate(),
                    subtask.getSchedule(), null, null, RepeatOptions.NEVER, null, null);

            subtask.setCompletionDate(null);

            mTasksService.saveTask(copy, true);
            mTasksService.saveTask(subtask, true);
        }
    }

    private void setInterval(Task task, long interval) {
        Calendar nextDate = Calendar.getInstance();
        nextDate.setTime(task.getRepeatDate());

        // Add interval until the time set is in the future.
        while (!DateUtils.isNewerThanToday(nextDate.getTime())) {
            long timeInMillis = nextDate.getTimeInMillis();
            nextDate.setTimeInMillis(timeInMillis + interval);
        }

        modifyTask(task, nextDate.getTime());
    }

    private void setMonthlyInterval(Task task) {
        Calendar nextDate = Calendar.getInstance();
        nextDate.setTime(task.getRepeatDate());

        // Add a month until the time set is in the future.
        while (!DateUtils.isNewerThanToday(nextDate.getTime())) {
            nextDate.add(Calendar.MONTH, 1);
        }

        modifyTask(task, nextDate.getTime());
    }

    private void setYearlyInterval(Task task) {
        Calendar nextDate = Calendar.getInstance();
        nextDate.setTime(task.getRepeatDate());

        // Add a year until the time set is in the future.
        while (!DateUtils.isNewerThanToday(nextDate.getTime())) {
            nextDate.add(Calendar.YEAR, 1);
        }

        modifyTask(task, nextDate.getTime());
    }

    private void handleWeekend(Task task) {
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

    private void modifyTask(Task task, Date date) {
        task.setRepeatDate(date);
        task.setSchedule(date);
        task.setCompletionDate(null);
    }

}
