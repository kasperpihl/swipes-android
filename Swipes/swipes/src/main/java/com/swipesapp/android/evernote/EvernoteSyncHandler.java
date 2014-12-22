package com.swipesapp.android.evernote;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.evernote.edam.type.Note;
import com.swipesapp.android.R;
import com.swipesapp.android.sync.gson.GsonAttachment;
import com.swipesapp.android.sync.gson.GsonTask;
import com.swipesapp.android.sync.service.TasksService;
import com.swipesapp.android.util.LevenshteinDistance;
import com.swipesapp.android.values.RepeatOptions;

import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TimeZone;
import java.util.UUID;

public class EvernoteSyncHandler {
    protected final static String sTag = "EvernoteSyncHandler";
    protected final static String sPrefsName = "EvernoteSyncHandler";
    protected final static String sKeyLastUpdated = "lastUpdated";
    protected final static EvernoteSyncHandler sInstance = new EvernoteSyncHandler();
    protected final static int sTitleMaxLength = 255;
    protected final static long sFetchChangesTimeout = 30 * 1000; // 30 seconds

    protected Set<Note> mChangedNotes;
    protected Date mLastUpdated;
    protected WeakReference<Context> mContext;
    protected List<Note> mKnownNotes;
    protected boolean mIsSyncing;

    public static EvernoteSyncHandler getInstance() {
        return sInstance;
    }

    protected EvernoteSyncHandler() {
        // Setup sync handler
        mChangedNotes = new LinkedHashSet<Note>();
    }

    public void synchronizeEvernote(Context context, OnEvernoteCallback<Void> callback)  {
        if (mIsSyncing) {
            callback.onSuccess(null);
            return;
        }

        // ensure authentication
        if (!EvernoteIntegration.getInstance().isAuthenticated()) {
            callback.onException(new Exception("Evernote not authenticated"));
        }

        mIsSyncing = true;
        mKnownNotes = null;
        mChangedNotes.clear();
        mContext = new WeakReference<Context>(context);
        // retrieve last update time
        if (null == mLastUpdated) {
            SharedPreferences prefs = context.getSharedPreferences(sPrefsName, Context.MODE_PRIVATE);
            long dateLong = prefs.getLong(sKeyLastUpdated, 0);
            if (0 < dateLong) {
                mLastUpdated = new Date(dateLong);
            }
        }

        // TODO work with needToClearCache
        boolean hasLocalChanges = checkForLocalChanges();
        if (!hasLocalChanges) {
            // no local changes
            if ((null != mLastUpdated) && (new Date().getTime() - mLastUpdated.getTime() < sFetchChangesTimeout)) {
                // checking too soon
                callback.onSuccess(null);
                mIsSyncing = false;
                return;
            }
        }

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        if (sharedPreferences.getBoolean("evernote_auto_import", false)) {
            findUpdatedNotesWithTag(EvernoteIntegration.SWIPES_TAG_NAME, callback);
        }
        else {
            syncEvernote(callback);
        }
    }

    protected boolean checkForLocalChanges() {
        boolean result = false;
        List<GsonTask> objectsWithEvernote = TasksService.getInstance(mContext.get()).loadTasksWithEvernote(true);
        for (GsonTask todoWithEvernote : objectsWithEvernote) {
            if (todoWithEvernote.getLocalUpdatedAt() != null && mLastUpdated != null &&
                    todoWithEvernote.getLocalUpdatedAt().after(mLastUpdated)) {
                result = true;
                break;
            }
        }
        return result;
    }

    protected void addAndSyncNewTasksFromNotes(List<Note> notes) {
        for (Note note : notes) {
            String title = note.getTitle();
            if (null == title) {
                title = mContext.get().getString(R.string.evernote_untitled_note);
            }
            if (sTitleMaxLength < title.length()) {
                title = title.substring(0, sTitleMaxLength);
            }
            // add to DB
            GsonAttachment attachment = new GsonAttachment(null, EvernoteIntegration.jsonFromNote(note), EvernoteIntegration.EVERNOTE_SERVICE, title, true);

            Date currentDate = new Date();
            String tempId = UUID.randomUUID().toString();
            GsonTask newTodo = GsonTask.gsonForLocal(null, null, tempId, null, currentDate, currentDate, false,
                    title, null, null, 0, null, currentDate, null, null, RepeatOptions.NEVER.getValue(),
                    null, null, null, Arrays.asList(attachment), 0);
            TasksService.getInstance(mContext.get()).saveTask(newTodo, true);
        }
    }

    protected String getEvernoteFormattedDateString() {
        final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'", Locale.US);
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        return dateFormat.format(new Date());
    }

    protected List<Note> extractKnownNotes()  {
        List<String> evernoteIdentifiers = TasksService.getInstance(mContext.get()).loadIdentifiersWithEvernote(true);
        List<Note> knownNotes = new ArrayList<Note>(evernoteIdentifiers.size());
        for (String s : evernoteIdentifiers) {
            Note note = EvernoteIntegration.noteFromJson(s);
            if (null != note) {
                knownNotes.add(note);
            }
        }
        return knownNotes;
    }

    protected boolean isNoteNew(Note note, List<Note> knownNotes) {
        boolean result = true;
        for (Note n : knownNotes) {
            if (n.getGuid().equalsIgnoreCase(note.getGuid())) {
                if ((n.getNotebookGuid() == null && note.getNotebookGuid() == null) ||
                        (n.getNotebookGuid() != null && n.getNotebookGuid().equalsIgnoreCase(note.getNotebookGuid()))) {
                    result = false;
                    break;
                }
            }
        }
        return result;
    }

    protected void findUpdatedNotesWithTag(String tag, final OnEvernoteCallback<Void> callback) {
        final StringBuilder query = new StringBuilder("tag:" + tag);
        if (null != mLastUpdated) {
            query.append(" updated:");
            query.append(getEvernoteFormattedDateString());
        }

        EvernoteIntegration.getInstance().findNotes(query.toString(), new OnEvernoteCallback<List<Note>>() {
            @Override
            public void onSuccess(List<Note> data) {
                if (null == mKnownNotes) {
                    mKnownNotes = extractKnownNotes();
                }
                ArrayList<Note> newNotes = new ArrayList<Note>();
                for (Note note : data) {
                    if (isNoteNew(note, mKnownNotes)) {
                       newNotes.add(note);
                    }
                    mChangedNotes.add(note);
                }
                addAndSyncNewTasksFromNotes(newNotes);
                fetchEvernoteChanges(callback);
            }

            @Override
            public void onException(Exception e) {
                Log.e(sTag, "findUpdatedNotesWithTag exception", e);
                callback.onException(e);
                mIsSyncing = false;
            }
        });
    }

    protected void setUpdatedAt(Date date) {
        mLastUpdated = date;
        SharedPreferences.Editor editor = mContext.get().getSharedPreferences(sPrefsName, Context.MODE_PRIVATE).edit();
        if (null != date) {
            editor.putLong(sKeyLastUpdated, date.getTime());
        }
        else {
            editor.remove(sKeyLastUpdated);
        }
        editor.commit();
    }

    protected void fetchEvernoteChanges(final OnEvernoteCallback<Void> callback) {
        final String query = (null != mLastUpdated) ? "updated:" + getEvernoteFormattedDateString() : null;

        EvernoteIntegration.getInstance().findNotes(query, new OnEvernoteCallback<List<Note>>() {
            @Override
            public void onSuccess(List<Note> data) {
                if (null == mKnownNotes) {
                    mKnownNotes = extractKnownNotes();
                }
                for (Note note : data) {
                    if (!isNoteNew(note, mKnownNotes))
                        mChangedNotes.add(note);
                }
                syncEvernote(callback);
            }

            @Override
            public void onException(Exception e) {
                Log.e(sTag, "fetchEvernoteChanges exception", e);
                callback.onException(e);
                mIsSyncing = false;
            }
        });
    }

    protected boolean handleEvernoteToDo(final EvernoteToDo evernoteToDo, final GsonTask subtask, final EvernoteToDoProcessor processor, boolean isNew, final TasksService tasksService) {
        boolean updated = false;

        // If subtask is deleted from Swipes - mark completed in Evernote
        if (subtask.isDeleted() && !evernoteToDo.isChecked()) {
            Log.i(sTag, "completing evernote - subtask was deleted");
            processor.updateToDo(evernoteToDo, true);
            return false;
        }

        boolean subtaskIsCompleted = subtask.getLocalCompletionDate() != null;

        // difference in completion
        if (subtaskIsCompleted) {
            // If subtask was completed in Swipes after last sync override evernote
            if (null != mLastUpdated && mLastUpdated.before(subtask.getLocalCompletionDate())) {
                Log.i(sTag, "completing evernote");
                processor.updateToDo(evernoteToDo, subtaskIsCompleted);
            }
            // If not - uncomplete in Swipes
            else {
                Log.i(sTag, "uncompleting subtask");
                subtask.setCompletionDate(null);
                tasksService.saveTask(subtask, true);
                updated = true;
            }
        }
        // If task is completed in Evernote, but not in Swipes
        else {
            // If subtask is updated later than last sync override Evernote
            // There could be an error margin here, but I don't see a better solution at the moment
            if (!isNew && null != mLastUpdated && mLastUpdated.before(subtask.getLocalUpdatedAt())) {
                Log.i(sTag, "uncompleting evernote");
                processor.updateToDo(evernoteToDo, false);
            }
            // If not, override in Swipes
            else{
                Log.i(sTag, "completing subtask");
                subtask.setLocalCompletionDate(new Date());
                tasksService.saveTask(subtask, true);
                updated = true;
            }
        }

        // difference in name
        if (!subtask.getTitle().equals(subtask.getOriginIdentifier())) {
            if (processor.updateToDo(evernoteToDo, subtask.getTitle())) {
                Log.i(sTag, "renamed evernote");
                subtask.setOriginIdentifier(subtask.getTitle());
                tasksService.saveTask(subtask, true);
                updated = true;
            }
        }

        return updated;
    }

    protected List<GsonTask> filterSubtasksWithEvernote(final List<GsonTask> subtasks) {
        final List<GsonTask> evernoteSubtasks = new ArrayList<GsonTask>();
        for (GsonTask subtask : subtasks) {
            // TODO we will add additional checks when we handle updated or deleted
            if (/*(null == subtask.getOrigin()) || */EvernoteIntegration.EVERNOTE_SERVICE.equals(subtask.getOrigin())) {
                evernoteSubtasks.add(subtask);
            }
        }
        return evernoteSubtasks;
    }

    protected List<GsonTask> filterSubtasksWithoutOrigin(final List<GsonTask> subtasks) {
        final List<GsonTask> noOriginSubtasks = new ArrayList<GsonTask>();
        for (GsonTask subtask : subtasks) {
            if ((null == subtask.getOrigin())) {
                noOriginSubtasks.add(subtask);
            }
        }
        return noOriginSubtasks;
    }

    protected void findAndHandleMatches(final GsonTask parentToDo, final EvernoteToDoProcessor processor) {
        final TasksService tasksService = TasksService.getInstance(mContext.get());
        List<GsonTask> subtasks = filterSubtasksWithEvernote(tasksService.loadSubtasksForTask(parentToDo.getTempId()));
        List<EvernoteToDo> evernoteToDos = new ArrayList<EvernoteToDo>(processor.getToDos());

        // Creating helper arrays for determining which ones has already been matched
        List<GsonTask> subtasksLeftToBeFound = new ArrayList<GsonTask>(subtasks);
        List<EvernoteToDo> evernoteToDosLeftToBeFound = new ArrayList<EvernoteToDo>(evernoteToDos);

        boolean updated = false;

        // Match and clean all direct matches
        for (EvernoteToDo evernoteToDo : processor.getToDos()) {
            GsonTask matchingSubtask = null;

            for (GsonTask subtask : subtasks) {
                if (evernoteToDo.getTitle().equalsIgnoreCase(subtask.getOriginIdentifier())) {
                    matchingSubtask = subtask;
                    subtasksLeftToBeFound.remove(subtask);
                    evernoteToDosLeftToBeFound.remove(evernoteToDo);

                    // subtask exists but not marked as evernote yet
                    if (null == subtask.getOrigin()) {
                        subtask.setOriginIdentifier(evernoteToDo.getTitle());
                        subtask.setOrigin(EvernoteIntegration.EVERNOTE_SERVICE);
                        tasksService.saveTask(subtask, true);
                    }

                    if (handleEvernoteToDo(evernoteToDo, subtask, processor, false, tasksService)) {
                        updated = true;
                    }
                    break;
                }
            }

            subtasks.clear();
            subtasks.addAll(subtasksLeftToBeFound);
        }

        evernoteToDos.clear();
        evernoteToDos.addAll(evernoteToDosLeftToBeFound);

        // Match and clean all indirect matches
        for (EvernoteToDo evernoteToDo : processor.getToDos()) {
            GsonTask matchingSubtask = null;

            int bestScore = 0;
            GsonTask bestMatch = null;

            for (GsonTask subtask : subtasks) {
                if (null == subtask.getOriginIdentifier())
                    continue;
                int match = LevenshteinDistance.computeEditDistance(evernoteToDo.getTitle(), subtask.getOriginIdentifier());
                if (match > bestScore) {
                    bestScore = match;
                    bestMatch = subtask;
                }
            }

            boolean isNew = false;
            if (bestScore >= 120) // TODO check score (might be wrong for java implementation)
                matchingSubtask = bestMatch;

            if (null == matchingSubtask) {
                Date currentDate = new Date();
                String tempId = UUID.randomUUID().toString();
                matchingSubtask = GsonTask.gsonForLocal(null, null, tempId, parentToDo.getTempId(), currentDate, currentDate, false,
                        evernoteToDo.getTitle(), null, null, 0, null, currentDate, null, null, RepeatOptions.NEVER.getValue(),
                        EvernoteIntegration.EVERNOTE_SERVICE, evernoteToDo.getTitle(), null, null, 0);
                tasksService.saveTask(matchingSubtask, true);
                updated = true;
                isNew = true;
            }
            else if (null == matchingSubtask.getOrigin()) {
                // subtask exists but not marked as evernote yet
                matchingSubtask.setOriginIdentifier(evernoteToDo.getTitle());
                matchingSubtask.setOrigin(EvernoteIntegration.EVERNOTE_SERVICE);
                tasksService.saveTask(matchingSubtask, true);
            }

            subtasksLeftToBeFound.remove(matchingSubtask);
            evernoteToDosLeftToBeFound.remove(evernoteToDo);

            if (handleEvernoteToDo(evernoteToDo, matchingSubtask, processor, isNew, tasksService))
                updated = true;

            subtasks.clear();
            subtasks.addAll(subtasksLeftToBeFound);
        }

        // remove evernote subtasks not found in the evernote from our task
        if (subtasks != null && subtasks.size() > 0) {
            updated = true;
            tasksService.deleteTasks(subtasks);
        }

        // add newly added tasks to evernote
        subtasks = filterSubtasksWithoutOrigin(tasksService.loadSubtasksForTask(parentToDo.getTempId()));
        for (GsonTask subtask : subtasks) {
            if (processor.addToDo(subtask.getTitle())) {
                subtask.setOriginIdentifier(subtask.getTitle());
                subtask.setOrigin(EvernoteIntegration.EVERNOTE_SERVICE);
                updated = true;
                tasksService.saveTask(subtask, true);
            }
        }

        /* TODO ?
        if( updated && parentToDo.objectId) {
            [self._updatedTasks addObject:parentToDo.objectId];
        }*/
    }

    protected boolean hasChangesFromEvernoteId(String enid) {
        Note searchNote = EvernoteIntegration.noteFromJson(enid);
        for (Note note : mChangedNotes) {
            if (note.getGuid().equalsIgnoreCase(searchNote.getGuid()) && note.getNotebookGuid().equalsIgnoreCase(searchNote.getNotebookGuid())) {
                return true;
            }
        }
        return false;
    }

    protected void finalizeSync(final Date date, Integer returnCount, int targetCount, final Exception ex, final OnEvernoteCallback<Void> callback) {
        returnCount++;
        if (returnCount == targetCount) {
            if (null != ex) {
                callback.onException(ex);
                mIsSyncing = false;
                return;
            }
            setUpdatedAt(date);
            mChangedNotes.clear();
            callback.onSuccess(null);
            mIsSyncing = false;

            /* TODO ?
            self.block(SyncStatusSuccess, @{@"updated": [self._updatedTasks copy]}, nil);
            [self._updatedTasks removeAllObjects]*/
        }
    }

    protected void syncEvernote(final OnEvernoteCallback<Void> callback)  {
        List<GsonTask> objectsWithEvernote = TasksService.getInstance(mContext.get()).loadTasksWithEvernote(true);

        final Date date = new Date();
        final Integer returnCount = new Integer(0);
        final int targetCount = objectsWithEvernote.size();
        final Exception[] runningError = {null};

        boolean syncedAnything = false;
        for (final GsonTask todoWithEvernote : objectsWithEvernote) {
            GsonAttachment attachment = todoWithEvernote.getFirstAttachmentForService(EvernoteIntegration.EVERNOTE_SERVICE);

            final boolean hasLocalChanges = (todoWithEvernote.getLocalUpdatedAt() != null && mLastUpdated != null && todoWithEvernote.getLocalUpdatedAt().after(mLastUpdated));
            final boolean hasChangesFromEvernote = hasChangesFromEvernoteId(attachment.getIdentifier());
            if (!hasLocalChanges && !hasChangesFromEvernote) {
                finalizeSync(date, returnCount, targetCount, runningError[0], callback);
                continue;
            }
            syncedAnything = true;

            EvernoteToDoProcessor.createInstance(attachment.getIdentifier(), new OnEvernoteCallback<EvernoteToDoProcessor>() {
                @Override
                public void onSuccess(final EvernoteToDoProcessor processor) {
                    EvernoteSyncHandler.this.findAndHandleMatches(todoWithEvernote, processor);
                    if (processor.getNeedUpdate()) {
                        processor.saveToEvernote(new OnEvernoteCallback<Boolean>() {
                            @Override
                            public void onSuccess(Boolean data) {
                                finalizeSync(date, returnCount, targetCount, runningError[0], callback);
                            }

                            @Override
                            public void onException(Exception ex) {
                                // TODO we can handle updated and deleted here at some point
                                if (null == runningError[0])
                                    runningError[0] = ex;
                                finalizeSync(date, returnCount, targetCount, ex, callback);
                                Log.e(sTag, ex.getMessage(), ex);
                            }
                        });
                    }
                    else {
                        finalizeSync(date, returnCount, targetCount, runningError[0], callback);
                    }
                }

                @Override
                public void onException(Exception ex) {
                    // TODO we can handle updated and deleted here at some point
                    if (null == runningError[0])
                        runningError[0] = ex;
                    finalizeSync(date, returnCount, targetCount, ex, callback);
                    Log.e(sTag, ex.getMessage(), ex);
                }
            });
        }
        
        if (!syncedAnything) {
            callback.onSuccess(null);
            mIsSyncing = false;
        }
    }
}