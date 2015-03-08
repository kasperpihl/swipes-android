package com.swipesapp.android.ui.listener;

import com.swipesapp.android.db.Task;

/**
 * Listener to handle subtask actions.
 *
 * @author Felipe Bari
 */
public interface SubtaskListener {

    /**
     * Complete a subtask.
     *
     * @param task Subtask to complete.
     */
    void completeSubtask(Task task);

    /**
     * Undo a subtask completion.
     *
     * @param task Task to undo completion.
     */
    void uncompleteSubtask(Task task);

    /**
     * Delete a subtask.
     *
     * @param task Subtask to delete.
     */
    void deleteSubtask(Task task);

    /**
     * Edit a subtask.
     *
     * @param task Subtask to edit.
     */
    void editSubtask(Task task);

}
