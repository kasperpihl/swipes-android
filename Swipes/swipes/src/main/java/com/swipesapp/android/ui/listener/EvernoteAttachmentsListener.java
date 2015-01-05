package com.swipesapp.android.ui.listener;

import com.evernote.edam.type.Note;

/**
 * Listener to handle Evernote attachments actions.
 *
 * @author Felipe Bari
 */
public interface EvernoteAttachmentsListener {

    /**
     * Attach a note to a task.
     *
     * @param note Note to attach.
     */
    void attachNote(Note note);

}
