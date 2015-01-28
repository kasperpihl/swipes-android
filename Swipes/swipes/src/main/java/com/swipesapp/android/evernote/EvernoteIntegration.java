package com.swipesapp.android.evernote;

/**
 * TODO:
 *  - request and update counters
 *  - caching
 *  - logout stuff
 */

import android.content.Context;
import android.util.Log;

import com.evernote.client.android.AsyncBusinessNoteStoreClient;
import com.evernote.client.android.AsyncNoteStoreClient;
import com.evernote.client.android.ClientFactory;
import com.evernote.client.android.EvernoteSession;
import com.evernote.client.android.InvalidAuthenticationException;
import com.evernote.client.android.OnClientCallback;
import com.evernote.edam.notestore.NoteFilter;
import com.evernote.edam.notestore.NoteList;
import com.evernote.edam.type.LinkedNotebook;
import com.evernote.edam.type.Note;
import com.evernote.edam.type.NoteSortOrder;
import com.evernote.edam.type.Notebook;
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

    private static final String sKeyJson = "json:";
    private static final String sKeyJsonGuid = "guid";
    private static final String sKeyJsonType = "type";
    private static final String sKeyJsonTypePersonal = "personal";
    private static final String sKeyJsonTypeShared = "shared";
    private static final String sKeyJsonTypeBusiness = "business";
    private static final String sKeyJsonLinkedNotebook = "linkedNotebook";
    private static final String sKeyJsonNotebookGuid = "guid";
    private static final String sKeyJsonNotebookNoteStoreUrl = "url";
    private static final String sKeyJsonNotebookShardId = "shardid";
    //private static final String sKeyJsonNotebookSharedNotebookGlobalId = "globalid";

    protected final static EvernoteIntegration sInstance = new EvernoteIntegration();

    protected EvernoteSession mEvernoteSession;

    protected String mSwipesTagGuid;
    protected String mUserNoteStoreGuid;
    protected List<LinkedNotebook> mBusinessNoteStoreNotebooks;
    //protected List<LinkedNotebook> mSharedNoteStoreNotebooks;

    public static EvernoteIntegration getInstance() {
        return sInstance;
    }

    public void asyncJsonFromNote(final Note note, final OnEvernoteCallback<String> callback) {
        final JSONObject json = new JSONObject();
        try {
            json.put(sKeyJsonGuid, note.getGuid());
            getNoteStoreGuids(new OnEvernoteCallback<Void>() {
                public void onSuccess(Void data) {
                    // find out type
                    try {
                        if (note.getNotebookGuid().equalsIgnoreCase(mUserNoteStoreGuid)) {
                            json.put(sKeyJsonType, sKeyJsonTypePersonal);
                        }
                        else {
                            if (mBusinessNoteStoreNotebooks != null) {
                                boolean found = false;
                                for (LinkedNotebook linkedNotebook : mBusinessNoteStoreNotebooks) {
                                    if (note.getNotebookGuid().equalsIgnoreCase(linkedNotebook.getGuid())) {
                                        json.put(sKeyJsonType, sKeyJsonTypeBusiness);
                                        JSONObject jsonLinkedNotebook = new JSONObject();
                                        jsonLinkedNotebook.put(sKeyJsonNotebookGuid, linkedNotebook.getGuid());
                                        jsonLinkedNotebook.put(sKeyJsonNotebookNoteStoreUrl, linkedNotebook.getNoteStoreUrl());
                                        jsonLinkedNotebook.put(sKeyJsonNotebookShardId, linkedNotebook.getShardId());
                                        //jsonLinkedNotebook.put(sKeyJsonNotebookSharedNotebookGlobalId, linkedNotebook.get());
                                        json.put(sKeyJsonLinkedNotebook, jsonLinkedNotebook);
                                        found = true;
                                        break;
                                    }
                                }
                                if (!found) {
                                    json.put(sKeyJsonType, sKeyJsonTypeShared);
                                }
                            }
                        }
                        callback.onSuccess(sKeyJson + json.toString());
                    } catch (JSONException ex) {
                        callback.onException(ex);
                    }
                }

                public void onException(Exception ex) {
                    callback.onException(ex);
                }
            });
        } catch (Exception ex) {
            Log.e(sTag, ex.getMessage(), ex);
            callback.onException(ex);
        }
    }

    public static Note noteFromJson(String jsonString) {
        Note note;
        if (jsonString.startsWith(sKeyJson)) {
            try {
                final JSONObject json = new JSONObject(jsonString.substring(sKeyJson.length(), jsonString.length()));
                note = new Note();
                note.setGuid(json.getString(sKeyJsonGuid));

                String type = json.getString(sKeyJsonType);
                if (null != type && !sKeyJsonTypePersonal.equalsIgnoreCase(type)) {
                    JSONObject jsonLinkedNotebook = json.getJSONObject(sKeyJsonLinkedNotebook);
                    if (null != jsonLinkedNotebook) {
                        note.setNotebookGuid(jsonLinkedNotebook.getString(sKeyJsonNotebookGuid));
                    }
                }

            } catch (Exception ex) {
                note = null;
                Log.e(sTag, ex.getMessage(), ex);
            }
        }
        else {
            try {
                final JSONObject json = new JSONObject(jsonString);
                note = new Note();
                note.setGuid(json.getString(sKeyNoteGuid));
                note.setNotebookGuid(json.optString(sKeyNotebookGuid));
            } catch (Exception e) {
                note = null;
                Log.e(sTag, e.getMessage(), e);
            }
        }
        return note;
    }

    protected EvernoteIntegration() {
        //Set up the Evernote Singleton Session
    }

    public void setContext(final Context context) {
        mEvernoteSession = EvernoteSession.getInstance(context, sConsumerKey, sConsumerSecret, sEvernoteService, sSupportAppLinkedNotebooks);
    }

    synchronized private void getNoteStoreGuids(final OnEvernoteCallback<Void> callback) {
        if ((null != mEvernoteSession) && (null == mUserNoteStoreGuid)) {
            final ClientFactory clientFactory = mEvernoteSession.getClientFactory();
            try {
                final AsyncNoteStoreClient personalNoteStore = clientFactory.createNoteStoreClient();
                personalNoteStore.getDefaultNotebook(new OnClientCallback<Notebook>() {

                    public void onSuccess(Notebook data) {
                        mUserNoteStoreGuid = data.getGuid();
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                final AsyncBusinessNoteStoreClient asyncBusinessNoteStoreClient;
                                try {
                                    asyncBusinessNoteStoreClient = clientFactory.createBusinessNoteStoreClient();
                                    mBusinessNoteStoreNotebooks = asyncBusinessNoteStoreClient.listNotebooks();
                                    callback.onSuccess(null);
                                } catch (Exception ex) {
                                    callback.onException(ex);
                                }
                            }
                        }).start();
                    }

                    public void onException(Exception ex) {
                        callback.onException(ex);
                    }
                });
            } catch (Exception ex) {
                callback.onException(ex);
            }
        }
        else if (null == mEvernoteSession) {
            callback.onException(new Exception("No evernote session"));
        }
        else {
            callback.onSuccess(null);
        }
    }

    public boolean isAuthenticated()
    {
        return mEvernoteSession.isLoggedIn();
    }

    public void authenticateInContext(final Context ctx)
    {
        mEvernoteSession.authenticate(ctx);
    }

    public void getSwipesTagGuid(final OnEvernoteCallback<String> callback) {
        try {
            mEvernoteSession.getClientFactory().createNoteStoreClient().listTags(new OnClientCallback<List<Tag>>() {

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

                                public void onSuccess(Tag data) {
                                    mSwipesTagGuid = data.getGuid();
                                }

                                public void onException(Exception exception) {
                                    // cannot create tag but this is not fatal
                                }
                            });
                        } catch (Exception e) {
                            // cannot create tag but this is not fatal
                        }
                    }
                }

                public void onException(Exception exception) {
                    callback.onException(exception);
                }
            });
        } catch (Exception e) {
            callback.onException(e);
        }
    }

    public void logoutInContext(final Context ctx) {
        try {
            mEvernoteSession.logOut(ctx);
            EvernoteSyncHandler.getInstance().setUpdatedAt(null);
        } catch (InvalidAuthenticationException e) {
            Log.e(sTag, e.getMessage(), e);
        }
    }

    public void findNotes(final String query, final OnEvernoteCallback<List<Note>> callback) {
        final NoteFilter filter = new NoteFilter();
        filter.setOrder(NoteSortOrder.UPDATED.getValue());
        filter.setWords(query);

        try {
            mEvernoteSession.getClientFactory().createNoteStoreClient().findNotes(filter, 0, sMaxNotes, new OnClientCallback<NoteList>() {
                public void onSuccess(NoteList data) {
                    callback.onSuccess(data.getNotes());
                    // TODO use update count and so on
                }

                public void onException(Exception e) {
                    callback.onException(e);
                }
            });
        } catch (Exception e) {
            callback.onException(e);
        }
    }

    private void checkIsBusinessNotebook(final String notebookGuid, final OnEvernoteCallback<LinkedNotebook>callback) {
        getNoteStoreGuids(new OnEvernoteCallback<Void>() {
            public void onSuccess(Void data) {
                if (null != mBusinessNoteStoreNotebooks) {
                    for (LinkedNotebook notebook : mBusinessNoteStoreNotebooks) {
                        if (notebook.getGuid().equalsIgnoreCase(notebookGuid)) {
                            callback.onSuccess(notebook);
                        }
                    }
                }
                callback.onSuccess(null);
            }

            public void onException(Exception e) {
                callback.onException(e);
            }
        });
    }

    void provideAsyncNoteStoreClientForNote(final Note note, final OnEvernoteCallback<AsyncNoteStoreClient>callback) {
        if (null == note.getNotebookGuid() || note.getNotebookGuid().equalsIgnoreCase(mUserNoteStoreGuid)) {
            try {
                callback.onSuccess(mEvernoteSession.getClientFactory().createNoteStoreClient());
            }
            catch (Exception e) {
                callback.onException(e);
            }
        }
        else {
            // business or linked note
            checkIsBusinessNotebook(note.getNotebookGuid(), new OnEvernoteCallback<LinkedNotebook>() {
                public void onSuccess(final LinkedNotebook linkedNotebook) {
//                    if (null != linkedNotebook) {
                        mEvernoteSession.getClientFactory().createBusinessNoteStoreClientAsync(new OnClientCallback<AsyncBusinessNoteStoreClient>() {
                            public void onSuccess(AsyncBusinessNoteStoreClient client) {
                                callback.onSuccess(client.getAsyncClient());
                            }

                            public void onException(Exception e) {
                                callback.onException(e);
                            }
                        });
//                    }
//                    else {
//                        int i = 5;
//                    }
                }

                public void onException(Exception e) {
                    callback.onException(e);
                }
            });
        }
    }

    public void downloadNote(final String noteRefString, final OnEvernoteCallback<Note>callback) {
        final Note note = EvernoteIntegration.noteFromJson(noteRefString);

        provideAsyncNoteStoreClientForNote(note, new OnEvernoteCallback<AsyncNoteStoreClient>() {
            public void onSuccess(AsyncNoteStoreClient client) {
                client.getNote(note.getGuid(), true, false, false, false, new OnClientCallback<Note>() {
                    public void onSuccess(Note data) {
//                        if (null != note.getNotebookGuid() && (!note.getNotebookGuid().equalsIgnoreCase(mUserNoteStoreGuid))) {
//                            data.setNotebookGuid(note.getNotebookGuid()); // daf*ck is it different?
//                        }
                        callback.onSuccess(data);
                    }

                    public void onException(Exception e) {
                        callback.onException(e);
                    }
                });
            }

            public void onException(Exception e) {
                callback.onException(e);
            }
        });
    }

    public void updateNote(final Note note, final OnEvernoteCallback<Note>callback) {

        provideAsyncNoteStoreClientForNote(note, new OnEvernoteCallback<AsyncNoteStoreClient>() {
            public void onSuccess(AsyncNoteStoreClient client) {
                client.updateNote(note, new OnClientCallback<Note>() {
                    public void onSuccess(Note data) {
                        callback.onSuccess(data);
                    }

                    public void onException(Exception e) {
                        callback.onException(e);
                    }
                });
            }

            @Override
            public void onException(Exception e) {
                callback.onException(e);
            }
        });

//        try {
//            mEvernoteSession.getClientFactory().createNoteStoreClient().updateNote(note, new OnClientCallback<Note>() {
//
//                public void onSuccess(Note data) {
//                    callback.onSuccess(data);
//                }
//
//                public void onException(Exception e) {
//                    callback.onException(e);
//                }
//            });
//        } catch (Exception e) {
//            callback.onException(e);
//        }
    }
}
