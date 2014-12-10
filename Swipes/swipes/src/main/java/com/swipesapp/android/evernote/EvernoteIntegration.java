package com.swipesapp.android.evernote;

/**
 * TODO:
 *  - request and update counters
 *  - caching
 */

import android.content.Context;
import android.util.Log;

import com.evernote.client.android.EvernoteSession;
import com.evernote.client.android.InvalidAuthenticationException;
import com.evernote.client.android.OnClientCallback;
import com.evernote.edam.notestore.NoteFilter;
import com.evernote.edam.notestore.NoteList;
import com.evernote.edam.type.Note;
import com.evernote.edam.type.NoteSortOrder;
import com.evernote.edam.type.Tag;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

public class EvernoteIntegration {

    public static final String SWIPES_TAG_NAME = "swipes";
    public static final String EVERNOTE_SERVICE = "evernote";

    private static final String sTag = "EvernoteIntegration";

    // Your Evernote API key. See http://dev.evernote.com/documentation/cloud/
    // Please obfuscate your code to help keep these values secret.
    // taken from iOS (for now?)
    private static final String sConsumerKey = "swipes";
    private static final String sConsumerSecret = "e862f0d879e2c2b6";

    // Initial development is done on Evernote's testing service, the sandbox.
    // Change to HOST_PRODUCTION to use the Evernote production service
    // once your code is complete, or HOST_CHINA to use the Yinxiang Biji
    // (Evernote China) production service.
    private static final EvernoteSession.EvernoteService sEvernoteService = EvernoteSession.EvernoteService.PRODUCTION;

    // Set this to true if you want to allow linked notebooks for accounts that can only access a single
    // notebook.
    private static final boolean sSupportAppLinkedNotebooks = true;

    // Maximum number of notes to search
    private static final int sMaxNotes = 100;

    private static final String sKeyNoteGuid = "noteguid";
    private static final String sKeyNotebookGuid = "notebookguid";

    protected final static EvernoteIntegration sInstance = new EvernoteIntegration();

    protected EvernoteSession mEvernoteSession;

    protected String mSwipesTagGuid;

    public static EvernoteIntegration getInstance() {
        return sInstance;
    }

    public static String jsonFromNote(Note note) {
        final JSONObject json = new JSONObject();
        try {
            json.put(sKeyNoteGuid, note.getGuid());
            if (note.isSetNotebookGuid())
                json.put(sKeyNotebookGuid, note.getNotebookGuid());
        } catch (Exception e) {
            Log.e(sTag, e.getMessage(), e);
            return null;
        }
        return json.toString();
    }

    public static Note noteFromJson(String jsonString) {
        Note note = null;
        try {
            final JSONObject json = new JSONObject(jsonString);
            note = new Note();
            note.setGuid(json.getString(sKeyNoteGuid));
            note.setNotebookGuid(json.optString(sKeyNotebookGuid));
        } catch (Exception e) {
            note = null;
            Log.e(sTag, e.getMessage(), e);
        }
        return note;
    }

    protected EvernoteIntegration() {
        //Set up the Evernote Singleton Session
    }

    public void setContext(Context context) {
        mEvernoteSession = EvernoteSession.getInstance(context, sConsumerKey, sConsumerSecret, sEvernoteService, sSupportAppLinkedNotebooks);
    }

    public boolean isAuthenticated()
    {
        return mEvernoteSession.isLoggedIn();
    }

    public void authenticateInContext(Context ctx)
    {
        mEvernoteSession.authenticate(ctx);
    }

    public void getSwipesTagGuid(final OnEvernoteCallback<String> callback) {
        try {
            mEvernoteSession.getClientFactory().createNoteStoreClient().listTags(new OnClientCallback<List<Tag>>() {
                @Override
                public void onSuccess(List<Tag> data) {
                    for (Tag tag : data) {
                        if (tag.getName().equalsIgnoreCase(SWIPES_TAG_NAME)) {
                            mSwipesTagGuid = tag.getGuid();
                            break;
                        }
                    }

                    if (null == mSwipesTagGuid) {
                        Tag tag = new Tag();
                        tag.setName(SWIPES_TAG_NAME);
                        try {
                            mEvernoteSession.getClientFactory().createNoteStoreClient().createTag(tag, new OnClientCallback<Tag>() {
                                @Override
                                public void onSuccess(Tag data) {
                                    mSwipesTagGuid = data.getGuid();
                                }

                                @Override
                                public void onException(Exception exception) {
                                    // cannot create tag but this is not fatal
                                }
                            });
                        } catch (Exception e) {
                            // cannot create tag but this is not fatal
                        }
                    }
                }

                @Override
                public void onException(Exception exception) {
                    callback.onException(exception);
                }
            });
        } catch (Exception e) {
            callback.onException(e);
        }
    }

    public void logoutInContext(Context ctx) {
        try {
            mEvernoteSession.logOut(ctx);
        } catch (InvalidAuthenticationException e) {
            // TODO log exception
        }
    }

    public void findNotes(String query, final OnEvernoteCallback<List<Note>> callback) {
        final NoteFilter filter = new NoteFilter();
        filter.setOrder(NoteSortOrder.UPDATED.getValue());
        filter.setWords(query);

        try {
            mEvernoteSession.getClientFactory().createNoteStoreClient().findNotes(filter, 0, sMaxNotes, new OnClientCallback<NoteList>() {
                @Override
                public void onSuccess(NoteList data) {
                    callback.onSuccess(data.getNotes());
                    // TODO use update count and so on
                }

                @Override
                public void onException(Exception e) {
                    callback.onException(e);
                }
            });
        } catch (Exception e) {
            callback.onException(e);
        }
    }

    public void downloadNote(String noteRefString, final OnEvernoteCallback<Note>callback) {
        try {
            mEvernoteSession.getClientFactory().createNoteStoreClient().getNote(noteRefString, true, false, false, false, new OnClientCallback<Note>() {
                @Override
                public void onSuccess(Note data) {
                    callback.onSuccess(data);
                }

                @Override
                public void onException(Exception e) {
                    callback.onException(e);
                }
            });
        } catch (Exception e) {
            callback.onException(e);
        }
//        Note note = new Note();
//        note.setContent("<en-note><div>There are checks in there:</div><div><br clear=\"none\"/></div><div><en-todo/>Check again!\r\n</div><div><en-todo checked=\"false\"/>Second check</div><div><en-todo checked=\"true\"/>Third check</div><div><en-todo/>Another check<br/></div><div><en-todo/>I'm adding a task<br/></div><div><br clear=\"none\"/></div><div><br clear=\"none\"/><en-media style=\"height: auto;\" type=\"image/jpeg\" hash=\"ab7d9b70e606544a421a0a44daacdf40\"/></div><div><br clear=\"none\"/><en-media border=\"0\" style=\"cursor:pointer;\" type=\"application/xml\" height=\"43\" hash=\"e2ecf58b6a1b62910d76c3aa98c5092a\"/><br clear=\"none\"/><br clear=\"none\"/><br clear=\"none\"/><br clear=\"none\"/></div></en-note>");
//
//        callback.onSuccess(note);
    }

    public void updateNote(Note note, final OnEvernoteCallback<Note>callback) {
        try {
            mEvernoteSession.getClientFactory().createNoteStoreClient().updateNote(note, new OnClientCallback<Note>() {
                @Override
                public void onSuccess(Note data) {
                    callback.onSuccess(data);
                }

                @Override
                public void onException(Exception e) {
                    callback.onException(e);
                }
            });
        } catch (Exception e) {
            callback.onException(e);
        }
    }
}
