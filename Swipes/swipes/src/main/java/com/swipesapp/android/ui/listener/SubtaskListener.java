package com.swipesapp.android.ui.listener;

import com.swipesapp.android.sync.gson.GsonTask;

/**
 * Listener to handle subtask actions.
 *
 * @author Fernanda Bari
 */
public interface SubtaskListener {

    /**
     * Complete a subtask.
     *
     * @param task Subtask to complete.
     */
    void completeSubtask(GsonTask task);

    /**
     * Undo a subtask completion.
     *
     * @param task Task to undo completion.
     */
    void uncompleteSubtask(GsonTask task);

    /**
     * Delete a subtask.
     *
     * @param task Subtask to delete.
     */
    void deleteSubtask(GsonTask task);

    /**
     * Edit a subtask.
     *
     * @param task Subtask to edit.
     */
    void editSubtask(GsonTask task);

}
