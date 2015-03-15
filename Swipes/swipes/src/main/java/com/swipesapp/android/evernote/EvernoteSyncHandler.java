package com.swipesapp.android.evernote;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.evernote.edam.type.Note;
import com.swipesapp.android.R;
import com.swipesapp.android.analytics.handler.Analytics;
import com.swipesapp.android.analytics.handler.IntercomHandler;
import com.swipesapp.android.analytics.values.Actions;
import com.swipesapp.android.analytics.values.Categories;
import com.swipesapp.android.analytics.values.IntercomEvents;
import com.swipesapp.android.analytics.values.IntercomFields;
import com.swipesapp.android.analytics.values.Labels;
import com.swipesapp.android.sync.gson.GsonAttachment;
import com.swipesapp.android.sync.gson.GsonTask;
import com.swipesapp.android.sync.service.TasksService;
import com.swipesapp.android.util.LevenshteinDistance;
import com.swipesapp.android.util.PreferenceUtils;
import com.swipesapp.android.values.RepeatOptions;
import com.swipesapp.android.values.Services;

import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TimeZone;
import java.util.UUID;

public class EvernoteSyncHandler {

    protected final static String LOG_TAG = EvernoteSyncHandler.class.getSimpleName();
    protected final static String KEY_LAST_UPDATED = "lastUpdated";
    protected final static String KEY_AUTO_IMPORT = "evernote_auto_import";
    protected final static String KEY_EVERNOTE_JSON_CONVERTED = "evernoteJsonConverted";
    protected final static int TITLE_MAX_LENGTH = 255;
    protected final static long FETCH_CHANGES_TIMEOUT = 30000; // 30 seconds

    protected static EvernoteSyncHandler sInstance;

    protected Set<Note> mChangedNotes;
    protected Date mLastUpdated;
    protected WeakReference<Context> mContext;
    protected List<Note> mKnownNotes;
    protected Integer mReturnCount = 0;
    protected int mTotalNoteCount;
    protected int mCurrentNoteCount;
    protected Exception mRunningError;
    protected boolean mConvertedToJson;

    public EvernoteSyncHandler(Context context) {
        // Setup sync handler
        mChangedNotes = new LinkedHashSet<Note>();
        mConvertedToJson = false;
        mContext = new WeakReference<Context>(context);
    }

    public static EvernoteSyncHandler newInstance(Context context) {
        sInstance = new EvernoteSyncHandler(context);
        return sInstance;
    }

    public static EvernoteSyncHandler getInstance() {
        return sInstance;
    }

    public void synchronizeEvernote(OnEvernoteCallback<Void> callback) {
        // ensure authentication
        if (!EvernoteService.getInstance().isAuthenticated()) {
            callback.onException(new Exception("Evernote not authenticated"));
        }

        mKnownNotes = null;
        mChangedNotes.clear();

        if (!mConvertedToJson)
            convertToJSONIdentifiers();

        // retrieve last update time
        if (null == mLastUpdated) {
            long dateLong = PreferenceUtils.readLong(KEY_LAST_UPDATED, mContext.get());
            if (0 < dateLong) {
                mLastUpdated = new Date(dateLong);
            }
        }

        // TODO work with needToClearCache
        boolean hasLocalChanges = checkForLocalChanges();
        if (!hasLocalChanges) {
            // no local changes
            if ((null != mLastUpdated) && (new Date().getTime() - mLastUpdated.getTime() < FETCH_CHANGES_TIMEOUT)) {
                // checking too soon
                callback.onSuccess(null);
                return;
            }
        }

        boolean autoImport = PreferenceUtils.readBoolean(KEY_AUTO_IMPORT, mContext.get());
        if (autoImport) {
            findUpdatedNotesWithTag(EvernoteService.SWIPES_TAG_NAME, callback);
        } else {
            syncEvernote(callback);
        }
    }

    protected boolean checkForLocalChanges() {
        boolean result = false;
        List<GsonTask> objectsWithEvernote = TasksService.getInstance().loadTasksWithEvernote(true);
        for (GsonTask todoWithEvernote : objectsWithEvernote) {
            if (todoWithEvernote.getLocalUpdatedAt() != null && mLastUpdated != null &&
                    todoWithEvernote.getLocalUpdatedAt().after(mLastUpdated)) {
                result = true;
                break;
            }
        }
        return result;
    }

    protected void addAndSyncNewTasksFromNotes(List<Note> notes, final OnEvernoteCallback<Void> callback) {
        mTotalNoteCount = notes.size();
        if (mTotalNoteCount == 0) {
            callback.onSuccess(null);
            return;
        }
        mCurrentNoteCount = 0;
        mRunningError = null;

        for (final Note note : notes) {
            String title = note.getTitle();
            if (null == title) {
                title = mContext.get().getString(R.string.evernote_untitled_note);
            }
            if (TITLE_MAX_LENGTH < title.length()) {
                title = title.substring(0, TITLE_MAX_LENGTH);
            }
            final String fTitle = title;
            // add to DB
            EvernoteService.getInstance().asyncJsonFromNote(note, new OnEvernoteCallback<String>() {
                public void onSuccess(String data) {
                    final GsonAttachment attachment = new GsonAttachment(null, data, Services.EVERNOTE, fTitle, true);

                    final Date currentDate = new Date();
                    final String tempId = UUID.randomUUID().toString();
                    Log.d(LOG_TAG, "Creating task with UUID: " + tempId);
                    GsonTask newTodo = GsonTask.gsonForLocal(null, null, tempId, null, currentDate, currentDate, false,
                            fTitle, null, null, 0, null, currentDate, null, null, RepeatOptions.NEVER,
                            null, null, null, Arrays.asList(attachment), 0);
                    TasksService.getInstance().saveTask(newTodo, true);

                    sendTaskAddedEvent(false, fTitle);

                    sendAttachmentAddedEvent();

                    if (++mCurrentNoteCount >= mTotalNoteCount) {
                        if (null == mRunningError)
                            callback.onSuccess(null);
                        else
                            callback.onException(mRunningError);
                    }
                }

                public void onException(Exception ex) {
                    if (null == mRunningError)
                        mRunningError = ex;
                    if (++mCurrentNoteCount >= mTotalNoteCount) {
                        callback.onException(mRunningError);
                    }
                }
            });
        }
    }

    protected String getEvernoteFormattedDateString(Date since) {
        final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'", Locale.US);
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        return dateFormat.format(since);
    }

    protected List<Note> extractKnownNotes() {
        List<String> evernoteIdentifiers = TasksService.getInstance().loadIdentifiersWithEvernote(true);
        List<Note> knownNotes = new ArrayList<Note>(evernoteIdentifiers.size());
        for (String s : evernoteIdentifiers) {
            Note note = EvernoteService.noteFromJson(s);
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
                if ((n.getNotebookGuid() == null) ||
                        (n.getNotebookGuid().equalsIgnoreCase(note.getNotebookGuid()))) {
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
            query.append(getEvernoteFormattedDateString(mLastUpdated));
        }

        EvernoteService.getInstance().findNotes(query.toString(), new OnEvernoteCallback<List<Note>>() {
            public void onSuccess(List<Note> data) {
                mKnownNotes = extractKnownNotes();
                ArrayList<Note> newNotes = new ArrayList<Note>();
                for (Note note : data) {
                    if (isNoteNew(note, mKnownNotes)) {
                        newNotes.add(note);
                    }
                    mChangedNotes.add(note);
                }
                addAndSyncNewTasksFromNotes(newNotes, new OnEvernoteCallback<Void>() {
                    public void onSuccess(Void data) {
                        fetchEvernoteChanges(callback);
                    }

                    public void onException(Exception ex) {
                        callback.onException(ex);
                    }
                });
            }

            public void onException(Exception e) {
                Log.e(LOG_TAG, "findUpdatedNotesWithTag exception", e);
                callback.onException(e);
            }
        });
    }

    protected void setUpdatedAt(Date date) {
        mLastUpdated = date;
        if (null != date) {
            PreferenceUtils.saveLong(KEY_LAST_UPDATED, date.getTime(), mContext.get());
        } else {
            PreferenceUtils.remove(KEY_LAST_UPDATED, mContext.get());
        }
    }

    protected void fetchEvernoteChanges(final OnEvernoteCallback<Void> callback) {
        final String query = (null != mLastUpdated) ? "updated:" + getEvernoteFormattedDateString(mLastUpdated) : null;

        EvernoteService.getInstance().findNotes(query, new OnEvernoteCallback<List<Note>>() {
            @Override
            public void onSuccess(List<Note> data) {
                mKnownNotes = extractKnownNotes();
                for (Note note : data) {
                    if (!isNoteNew(note, mKnownNotes))
                        mChangedNotes.add(note);
                }
                syncEvernote(callback);
            }

            @Override
            public void onException(Exception e) {
                Log.e(LOG_TAG, "fetchEvernoteChanges exception", e);
                callback.onException(e);
            }
        });
    }

    protected boolean handleEvernoteToDo(final EvernoteToDo evernoteToDo, final GsonTask subtask, final EvernoteToDoProcessor processor, boolean isNew, final TasksService tasksService) {
        boolean updated = false;

        // If subtask is deleted from Swipes - mark completed in Evernote
        if (subtask.isDeleted() && !evernoteToDo.isChecked()) {
            Log.d(LOG_TAG, "completing evernote - subtask was deleted");
            processor.updateToDo(evernoteToDo, true);
            return false;
        }

        boolean subtaskIsCompleted = subtask.getLocalCompletionDate() != null;

        // difference in completion
        if (subtaskIsCompleted != evernoteToDo.isChecked()) {
            // difference in completion
            if (subtaskIsCompleted) {
                // If subtask was completed in Swipes after last sync override evernote
                if (null != mLastUpdated && mLastUpdated.before(subtask.getLocalCompletionDate())) {
                    Log.d(LOG_TAG, "completing evernote");
                    processor.updateToDo(evernoteToDo, subtaskIsCompleted);
                }
                // If not - uncomplete in Swipes
                else {
                    Log.d(LOG_TAG, "uncompleting subtask");
                    subtask.setLocalCompletionDate(null);
                    tasksService.saveTask(subtask, true);
                    updated = true;
                }
            }
            // If task is completed in Evernote, but not in Swipes
            else {
                // If subtask is updated later than last sync override Evernote
                // There could be an error margin here, but I don't see a better solution at the moment
                if (!isNew && null != mLastUpdated && mLastUpdated.before(subtask.getLocalUpdatedAt())) {
                    Log.d(LOG_TAG, "uncompleting evernote");
                    processor.updateToDo(evernoteToDo, false);
                }
                // If not, override in Swipes
                else {
                    Log.d(LOG_TAG, "completing subtask");
                    subtask.setLocalCompletionDate(new Date());
                    tasksService.saveTask(subtask, true);
                    updated = true;
                }
            }
        }

        // difference in name
        if (!subtask.getTitle().equals(subtask.getOriginIdentifier())) {
            if (processor.updateToDo(evernoteToDo, subtask.getTitle())) {
                Log.d(LOG_TAG, "renamed evernote");
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
            if (/*(null == subtask.getOrigin()) || */Services.EVERNOTE.equals(subtask.getOrigin())) {
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
        final TasksService tasksService = TasksService.getInstance();
        List<GsonTask> subtasks = /*filterSubtasksWithEvernote(*/tasksService.loadSubtasksForTask(parentToDo.getTempId())/*)*/;
        List<EvernoteToDo> evernoteToDos = new ArrayList<EvernoteToDo>(processor.getToDos());

        // Creating helper arrays for determining which ones has already been matched
        List<GsonTask> subtasksLeftToBeFound = new ArrayList<GsonTask>(subtasks);
        List<EvernoteToDo> evernoteToDosLeftToBeFound = new ArrayList<EvernoteToDo>(evernoteToDos);

        boolean updated = false;

        // Match and clean all direct matches
        for (EvernoteToDo evernoteToDo : evernoteToDos) {

            for (GsonTask subtask : subtasks) {
                if (evernoteToDo.getTitle().equalsIgnoreCase(subtask.getOriginIdentifier()) || evernoteToDo.getTitle().equalsIgnoreCase(subtask.getTitle())) {
                    subtasksLeftToBeFound.remove(subtask);
                    evernoteToDosLeftToBeFound.remove(evernoteToDo);

                    // subtask exists but not marked as evernote yet
                    if (null == subtask.getOrigin()) {
                        subtask.setOriginIdentifier(evernoteToDo.getTitle());
                        subtask.setOrigin(Services.EVERNOTE);
                        tasksService.saveTask(subtask, true);
                    }
                    if (handleEvernoteToDo(evernoteToDo, subtask, processor, false, tasksService)) {
                        updated = true;
                    }
                    break;
                }
            }

            if (subtasks.size() != subtasksLeftToBeFound.size()) {
                subtasks.clear();
                subtasks.addAll(subtasksLeftToBeFound);
            }
        }

        evernoteToDos.clear();
        evernoteToDos.addAll(evernoteToDosLeftToBeFound);

        // Match and clean all indirect matches
        for (EvernoteToDo evernoteToDo : evernoteToDos) {
            GsonTask matchingSubtask = null;

            int bestScore = Integer.MAX_VALUE;
            GsonTask bestMatch = null;

            for (GsonTask subtask : subtasks) {
                if (null == subtask.getOriginIdentifier())
                    continue;
                int match = LevenshteinDistance.computeEditDistance(evernoteToDo.getTitle(), subtask.getOriginIdentifier());
                if (match < bestScore) {
                    bestScore = match;
                    bestMatch = subtask;
                }
            }

            if (bestScore <= 5)
                matchingSubtask = bestMatch;

            boolean isNew = false;
            if (null == matchingSubtask) {
                Date currentDate = new Date();
                String tempId = UUID.randomUUID().toString();
                matchingSubtask = GsonTask.gsonForLocal(null, null, tempId, parentToDo.getTempId(), currentDate, currentDate, false,
                        evernoteToDo.getTitle(), null, null, 0, evernoteToDo.isChecked() ? currentDate : null, currentDate, null, null,
                        RepeatOptions.NEVER, Services.EVERNOTE, evernoteToDo.getTitle(), null, null, 0);
                tasksService.saveTask(matchingSubtask, true);

                sendTaskAddedEvent(true, evernoteToDo.getTitle());

                updated = true;
                isNew = true;
            } else if (null == matchingSubtask.getOrigin()) {
                // subtask exists but not marked as evernote yet
                matchingSubtask.setOriginIdentifier(evernoteToDo.getTitle());
                matchingSubtask.setOrigin(Services.EVERNOTE);
                tasksService.saveTask(matchingSubtask, true);
            }

            subtasksLeftToBeFound.remove(matchingSubtask);
            subtasks.clear();
            subtasks.addAll(subtasksLeftToBeFound);
            evernoteToDosLeftToBeFound.remove(evernoteToDo);

            if (matchingSubtask != null && handleEvernoteToDo(evernoteToDo, matchingSubtask, processor, isNew, tasksService))
                updated = true;
        }

        // remove evernote subtasks not found in the evernote from our task
        if (subtasks != null && subtasks.size() > 0) {
            ArrayList<GsonTask> tasksToDelete = new ArrayList<GsonTask>();
            for (GsonTask subtask : subtasks) {
                if (null != subtask.getOrigin() && subtask.getOrigin().equals(Services.EVERNOTE)) {
                    updated = true;
                    tasksToDelete.add(subtask);
                }
            }
            if (0 < tasksToDelete.size())
                tasksService.deleteTasks(tasksToDelete);
        }

        // add newly added tasks to evernote
        subtasks = filterSubtasksWithoutOrigin(tasksService.loadSubtasksForTask(parentToDo.getTempId()));
        for (GsonTask subtask : subtasks) {
            if (processor.addToDo(subtask.getTitle())) {
                subtask.setOriginIdentifier(subtask.getTitle());
                subtask.setOrigin(Services.EVERNOTE);
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
        Note searchNote = EvernoteService.noteFromJson(enid);
        for (Note note : mChangedNotes) {
            if (searchNote != null && note.getGuid().equalsIgnoreCase(searchNote.getGuid())) {
                return true;
            }
        }
        return false;
    }

    protected boolean hasLocalChanges(final GsonTask todoWithEvernote) {
        if (null != mLastUpdated) {
            if (todoWithEvernote.getLocalUpdatedAt() != null && todoWithEvernote.getLocalUpdatedAt().after(mLastUpdated)) {
                return true;
            }
            final TasksService tasksService = TasksService.getInstance();
            List<GsonTask> subtasks = filterSubtasksWithEvernote(tasksService.loadSubtasksForTask(todoWithEvernote.getTempId()));
            for (GsonTask subtask : subtasks) {
                if (subtask.getLocalUpdatedAt() != null && subtask.getLocalUpdatedAt().after(mLastUpdated)) {
                    return true;
                }
            }
        }
        return false;
    }

    protected void finalizeSync(final Date date, int targetCount, final Exception ex, final OnEvernoteCallback<Void> callback) {
        mReturnCount++;
        if (mReturnCount == targetCount) {
            if (null != ex) {
                callback.onException(ex);
                return;
            }
            setUpdatedAt(date);
            mChangedNotes.clear();
            callback.onSuccess(null);

            /* TODO ?
            self.block(SyncStatusSuccess, @{@"updated": [self._updatedTasks copy]}, nil);
            [self._updatedTasks removeAllObjects]*/
        }
    }

    protected void syncEvernote(final OnEvernoteCallback<Void> callback) {
        List<GsonTask> objectsWithEvernote = TasksService.getInstance().loadTasksWithEvernote(true);

        final Date date = new Date();
        mReturnCount = 0;
        final int targetCount = objectsWithEvernote.size();
        final Exception[] runningError = {null};

        boolean syncedAnything = false;
        for (final GsonTask todoWithEvernote : objectsWithEvernote) {
            GsonAttachment attachment = todoWithEvernote.getFirstAttachmentForService(Services.EVERNOTE);

            if (attachment != null) {
                final boolean hasLocalChanges = hasLocalChanges(todoWithEvernote);
                final boolean hasChangesFromEvernote = hasChangesFromEvernoteId(attachment.getIdentifier());
                if (!hasLocalChanges && !hasChangesFromEvernote) {
                    finalizeSync(date, targetCount, runningError[0], callback);
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
                                    finalizeSync(date, targetCount, runningError[0], callback);
                                }

                                @Override
                                public void onException(Exception ex) {
                                    // TODO we can handle updated and deleted here at some point
                                    if (null == runningError[0])
                                        runningError[0] = ex;
                                    finalizeSync(date, targetCount, ex, callback);
                                    Log.e(LOG_TAG, ex.getMessage(), ex);
                                }
                            });
                        } else {
                            finalizeSync(date, targetCount, runningError[0], callback);
                        }
                    }

                    @Override
                    public void onException(Exception ex) {
                        // TODO we can handle updated and deleted here at some point
                        if (null == runningError[0])
                            runningError[0] = ex;
                        finalizeSync(date, targetCount, ex, callback);
                        Log.e(LOG_TAG, ex.getMessage(), ex);
                    }
                });
            }
        }

        if (!syncedAnything) {
            callback.onSuccess(null);
        }
    }

    // converts from the old format to the new (universal) JSON format
    synchronized protected void convertToJSONIdentifiers() {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(mContext.get());
        mConvertedToJson = settings.getBoolean(KEY_EVERNOTE_JSON_CONVERTED, false);

        if (!mConvertedToJson) {
            mConvertedToJson = true;
            settings.edit().putBoolean(KEY_EVERNOTE_JSON_CONVERTED, true).apply();
            final List<GsonTask> objectsWithEvernote = TasksService.getInstance().loadTasksWithEvernote(true);

            for (final GsonTask todoWithEvernote : objectsWithEvernote) {
                final GsonAttachment attachment = todoWithEvernote.getFirstAttachmentForService(Services.EVERNOTE);

                if (attachment != null) {
                    final String identifier = attachment.getIdentifier();

                    if (!EvernoteService.isJSONFormat(identifier)) {
                        final Note note = EvernoteService.noteFromJson(identifier);

                        EvernoteService.getInstance().asyncJsonFromNote(note, new OnEvernoteCallback<String>() {
                            @Override
                            public void onSuccess(String data) {
                                todoWithEvernote.removeAttachment(attachment);
                                todoWithEvernote.addAttachment(new GsonAttachment(null, data, Services.EVERNOTE, attachment.getTitle(), true));
                                TasksService.getInstance().saveTask(todoWithEvernote, true);

                                sendAttachmentAddedEvent();
                            }

                            @Override
                            public void onException(Exception e) {
                                Log.e(LOG_TAG, "Error: " + e.getMessage(), e);
                            }
                        });
                    }
                }
            }
        }
    }

    private void sendTaskAddedEvent(boolean isSubtask, String title) {
        String action = isSubtask ? Actions.ADDED_SUBTASK : Actions.ADDED_TASK;
        String intercomAction = isSubtask ? IntercomEvents.ADDED_SUBTASK : IntercomEvents.ADDED_TASK;

        // Send analytics event.
        Analytics.sendEvent(Categories.TASKS, action, Labels.ADDED_FROM_EVERNOTE, (long) title.length());

        // Prepare Intercom fields.
        HashMap<String, Object> fields = new HashMap<>();
        fields.put(IntercomFields.LENGHT, (long) title.length());
        fields.put(IntercomFields.FROM, Labels.ADDED_FROM_EVERNOTE);

        // Send Intercom events.
        IntercomHandler.sendEvent(intercomAction, fields);
    }

    private void sendAttachmentAddedEvent() {
        // Send analytics event.
        Analytics.sendEvent(Categories.TASKS, Actions.ATTACHMENT, Labels.ATTACHMENT_EVERNOTE, null);

        // Prepare Intercom fields.
        HashMap<String, Object> fields = new HashMap<>();
        fields.put(IntercomFields.SERVICE, Labels.ATTACHMENT_EVERNOTE);
        fields.put(IntercomFields.FROM, Labels.ATTACHMENT_FROM_SWIPES_TAG);

        // Send Intercom events.
        IntercomHandler.sendEvent(IntercomEvents.ATTACHMENT, fields);
    }

}
