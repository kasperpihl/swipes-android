package com.swipesapp.android.util;

import com.swipesapp.android.sync.gson.GsonTask;

import org.apache.commons.collections4.comparators.NullComparator;

import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

/**
 * Utilitary class for list operations.
 *
 * @author Felipe Bari
 */
public class ListUtils {

    /**
     * Sort list of scheduled tasks.
     *
     * @param tasks List to sort.
     */
    public static void sortScheduledTasks(List<GsonTask> tasks) {
        // Sort by ascending creation date.
        Collections.sort(tasks, new Comparator<GsonTask>() {
            @Override
            public int compare(GsonTask task1, GsonTask task2) {
                return task1.getLocalCreatedAt().compareTo(task2.getLocalCreatedAt());
            }
        });

        // Sort by ascending schedule.
        Collections.sort(tasks, new Comparator<GsonTask>() {
            @Override
            public int compare(GsonTask task1, GsonTask task2) {
                int order = 0;
                if (task1.getLocalSchedule() != null && task2.getLocalSchedule() != null) {
                    task1.getLocalSchedule().compareTo(task2.getLocalSchedule());
                }
                return order;
            }
        });

        // Move unspecified to the end.
        Collections.sort(tasks, new Comparator<GsonTask>() {
            @Override
            public int compare(GsonTask task1, GsonTask task2) {
                return new NullComparator<Date>(true).compare(task1.getLocalSchedule(), task2.getLocalSchedule());
            }
        });
    }

    /**
     * Sort list of focused tasks.
     *
     * @param tasks List to sort.
     */
    public static void sortFocusedTasks(List<GsonTask> tasks) {
        // Sort by descending creation date.
        Collections.sort(tasks, new Comparator<GsonTask>() {
            @Override
            public int compare(GsonTask task1, GsonTask task2) {
                return task2.getLocalCreatedAt().compareTo(task1.getLocalCreatedAt());
            }
        });

        // Sort by ascending order.
        Collections.sort(tasks, new Comparator<GsonTask>() {
            @Override
            public int compare(GsonTask task1, GsonTask task2) {
                return task1.getOrder().compareTo(task2.getOrder());
            }
        });
    }

    /**
     * Sort list of completed tasks.
     *
     * @param tasks List to sort.
     */
    public static void sortCompletedTasks(List<GsonTask> tasks) {
        // Sort by descending creation date.
        Collections.sort(tasks, new Comparator<GsonTask>() {
            @Override
            public int compare(GsonTask task1, GsonTask task2) {
                return task2.getLocalCompletionDate().compareTo(task1.getLocalCompletionDate());
            }
        });
    }

}
