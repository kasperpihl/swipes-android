package com.swipesapp.android.evernote;

/**
 * TODO:
 *  - request and update counters?
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
import com.evernote.edam.notestore.NoteMetadata;
import com.evernote.edam.notestore.NotesMetadataList;
import com.evernote.edam.notestore.NotesMetadataResultSpec;
import com.evernote.edam.type.LinkedNotebook;
import com.evernote.edam.type.Note;
import com.evernote.edam.type.NoteSortOrder;
import com.evernote.edam.type.Notebook;
import com.evernote.edam.type.SharedNotebook;
import com.evernote.edam.type.Tag;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
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
    private static final long sSearchCacheTimeout = 300 * 1000;

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
    private static final String sKeyJsonNotebookSharedNotebookGlobalId = "globalid";

    protected final static EvernoteIntegration sInstance = new EvernoteIntegration();

    protected EvernoteSession mEvernoteSession;

    protected String mSwipesTagGuid;
    protected String mUserNotebookGuid;
    protected HashMap<String, LinkedNotebook> mLinkedPersonalNotebooks;
    protected HashMap<String, LinkedNotebook> mBusinessNotebooks;
    protected HashMap<String, String> mSharedToBusiness;
    protected HashSet<String> mBusinessNotebookGuids;
    protected HashMap<String, CacheData<List<Note>>> mSearchCache;

    protected List<AsyncNoteStoreClient> mClients;

    public static EvernoteIntegration getInstance() {
        return sInstance;
    }

    private JSONObject getJSONForLinkedNotebook(final LinkedNotebook linkedNotebook) throws JSONException {
        final JSONObject jsonLinkedNotebook = new JSONObject();

        jsonLinkedNotebook.put(sKeyJsonNotebookGuid, linkedNotebook.getGuid());
        jsonLinkedNotebook.put(sKeyJsonNotebookNoteStoreUrl, linkedNotebook.getNoteStoreUrl());
        jsonLinkedNotebook.put(sKeyJsonNotebookShardId, linkedNotebook.getShardId());
        jsonLinkedNotebook.put(sKeyJsonNotebookSharedNotebookGlobalId, linkedNotebook.getShareKey());

        return jsonLinkedNotebook;
    }

    public void asyncJsonFromNote(final Note note, final OnEvernoteCallback<String> callback) {
        final JSONObject json = new JSONObject();
        try {
            json.put(sKeyJsonGuid, note.getGuid());
            getNoteStoreGuids(new OnEvernoteCallback<Void>() {
                public void onSuccess(Void data) {
                    // find out type
                    try {
                        if (note.getNotebookGuid().equalsIgnoreCase(mUserNotebookGuid)) {
                            json.put(sKeyJsonType, sKeyJsonTypePersonal);
                        }
                        else {
                            String guid = mBusinessNotebookGuids.contains(note.getNotebookGuid()) ? note.getNotebookGuid() : null;
                            if (guid != null) {
                                guid = mSharedToBusiness.containsKey(guid) ? mSharedToBusiness.get(guid) : guid;
                                final LinkedNotebook linkedNotebook = mBusinessNotebooks.get(guid);
                                if (linkedNotebook != null) {
                                    json.put(sKeyJsonType, sKeyJsonTypeBusiness);
                                    final JSONObject jsonLinkedNotebook = getJSONForLinkedNotebook(linkedNotebook);
                                    json.put(sKeyJsonLinkedNotebook, jsonLinkedNotebook);
                                }
                                else {
                                    final LinkedNotebook sharedNotebook = mLinkedPersonalNotebooks.get(note.getNotebookGuid());
                                    if (null != sharedNotebook) {
                                        json.put(sKeyJsonType, sKeyJsonTypeShared);
                                        final JSONObject jsonLinkedNotebook = getJSONForLinkedNotebook(sharedNotebook);
                                        json.put(sKeyJsonLinkedNotebook, jsonLinkedNotebook);
                                    }
                                    else {
                                        callback.onException(new Exception("Cannot find LinkedNotebook for guid" + note.getNotebookGuid()));
                                    }
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

    private void getPersonalDefaultNotebookGuid(final AsyncNoteStoreClient personalNoteStore, final OnEvernoteCallback<Void> callback) {
        personalNoteStore.getDefaultNotebook(new OnClientCallback<Notebook>() {
            @Override
            public void onSuccess(Notebook data) {
                mUserNotebookGuid = data.getGuid();
                callback.onSuccess(null);
            }

            @Override
            public void onException(Exception exception) {
                callback.onException(exception);
            }
        });
    }

    private void getPersonalLinkedNotebooks(final AsyncNoteStoreClient personalNoteStore, final OnEvernoteCallback<Void> callback) {
        personalNoteStore.listLinkedNotebooks(new OnClientCallback<List<LinkedNotebook>>() {
            @Override
            public void onSuccess(List<LinkedNotebook> data) {
                mLinkedPersonalNotebooks = new HashMap<String, LinkedNotebook>();
                for (LinkedNotebook linkedNotebook : data) {
                    mLinkedPersonalNotebooks.put(linkedNotebook.getGuid(), linkedNotebook);
                }
                callback.onSuccess(null);
            }

            @Override
            public void onException(Exception exception) {
                callback.onException(exception);
            }
        });
    }

    private void getBusinessSharedNotebooks(final ClientFactory clientFactory, final OnEvernoteCallback<Void> callback) {
        clientFactory.createBusinessNoteStoreClientAsync(new OnClientCallback<AsyncBusinessNoteStoreClient>() {

            @Override
            public void onSuccess(final AsyncBusinessNoteStoreClient client) {
                client.getAsyncClient().listSharedNotebooks(new OnClientCallback<List<SharedNotebook>>() {
                    @Override
                    public void onSuccess(List<SharedNotebook> data) {
                        mBusinessNotebookGuids = new HashSet<String>();
                        mSharedToBusiness = new HashMap<String, String>();
                        mBusinessNotebooks = new HashMap<String, LinkedNotebook>();

                        // prepare a quick hashmap for searching
                        HashMap<String, String> shareKeyMap = new HashMap<String, String>();
                        for (LinkedNotebook linkedNotebook : mLinkedPersonalNotebooks.values()) {
                            shareKeyMap.put(linkedNotebook.getShareKey(), linkedNotebook.getGuid());
                        }

                        // store the guids only
                        for (SharedNotebook sharedNotebook : data) {
                            mBusinessNotebookGuids.add(sharedNotebook.getNotebookGuid());
                            // remove from personal linked if there and create a business to shared key
                            if (shareKeyMap.containsKey(sharedNotebook.getShareKey())) {
                                String linkedGuid = shareKeyMap.get(sharedNotebook.getShareKey());
                                mSharedToBusiness.put(sharedNotebook.getNotebookGuid(), linkedGuid);
                                mBusinessNotebookGuids.add(linkedGuid);
                                mBusinessNotebooks.put(linkedGuid, mLinkedPersonalNotebooks.get(linkedGuid));
                                mLinkedPersonalNotebooks.remove(linkedGuid);
                            }
                        }
                        callback.onSuccess(null);
                    }

                    @Override
                    public void onException(Exception exception) {
                        callback.onException(exception);
                    }
                });

            }

            @Override
            public void onException(Exception exception) {
                callback.onException(exception);
            }
        });

    }

    synchronized private void getNoteStoreGuids(final OnEvernoteCallback<Void> callback) {
        if ((null != mEvernoteSession) && (null == mUserNotebookGuid)) {
            final ClientFactory clientFactory = mEvernoteSession.getClientFactory();
            try {
                final AsyncNoteStoreClient personalNoteStore = clientFactory.createNoteStoreClient();

                getPersonalLinkedNotebooks(personalNoteStore, new OnEvernoteCallback<Void>() {
                    @Override
                    public void onSuccess(Void data) {
                        getPersonalDefaultNotebookGuid(personalNoteStore, new OnEvernoteCallback<Void>() {
                            @Override
                            public void onSuccess(Void data) {
                                getBusinessSharedNotebooks(clientFactory, new OnEvernoteCallback<Void>() {
                                    @Override
                                    public void onSuccess(Void data) {
                                        callback.onSuccess(null);
                                    }

                                    @Override
                                    public void onException(Exception e) {
                                        // it is OK if no business notebooks there
                                        callback.onSuccess(null);
                                    }
                                });
                            }

                            @Override
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

    private void getClients(final OnEvernoteCallback<List<AsyncNoteStoreClient>> callback) {
        if (null != mClients) {
            callback.onSuccess(mClients);
            return;
        }
        final List<AsyncNoteStoreClient> result = new ArrayList<AsyncNoteStoreClient>();
        final ClientFactory factory = mEvernoteSession.getClientFactory();
        try {
            result.add(factory.createNoteStoreClient());
            factory.createBusinessNoteStoreClientAsync(new OnClientCallback<AsyncBusinessNoteStoreClient>() {
                @Override
                public void onSuccess(AsyncBusinessNoteStoreClient client) {
                    result.add(client.getAsyncClient());
                    // TODO add linked notebooks
                    mClients = result;
                    callback.onSuccess(result);
                }

                @Override
                public void onException(Exception exception) {
                    mClients = result;
                    callback.onSuccess(result);
                }
            });
        }
        catch (Exception e) {
            callback.onException(e);
        }

    }

    private void reportFoundNotes(final String query, final List<Note> results, final OnEvernoteCallback<List<Note>> callback, boolean addToCache) {
        // TODO sort outside of main thread?
        Collections.sort(results, new Comparator<Note>() {
            @Override
            public int compare(Note lhs, Note rhs) {
                long diff = rhs.getUpdated() - lhs.getUpdated();
                if (0 < diff)
                    return 1;
                else if (0 > diff)
                    return -1;
                return 0;
            }
        });
        if (addToCache)
            cacheAddSearchResults(query, results);
        callback.onSuccess(results);
    }

    public void findNotes(final String query, final OnEvernoteCallback<List<Note>> callback) {
        final List<Note> cacheResults = cacheGetSearchResult(query);
        if (null != cacheResults) {
            callback.onSuccess(cacheResults);
            return;
        }

        final List<Note> results = new ArrayList<Note>();

        getClients(new OnEvernoteCallback<List<AsyncNoteStoreClient>>() {
            @Override
            public void onSuccess(List<AsyncNoteStoreClient> data) {
                final int totalClients = data.size();
                final int[] askedClients = {0};
                final boolean[] hasErrors = {false};

                for (int i = 0; i < data.size(); i++) {
                    final NoteFilter filter = new NoteFilter();
                    filter.setOrder(NoteSortOrder.UPDATED.getValue());
                    filter.setWords(query);

                    final AsyncNoteStoreClient client = data.get(i);
                    client.findNotes(filter, 0, sMaxNotes, new OnClientCallback<NoteList>() {
                        public void onSuccess(NoteList data) {
                            results.addAll(data.getNotes());
                            if (++askedClients[0] >= totalClients) {
                                reportFoundNotes(query, results, callback, !hasErrors[0]);
                            }
                        }

                        public void onException(Exception e) {
                            if (++askedClients[0] >= totalClients) {
                                hasErrors[0] = true;
                                reportFoundNotes(query, results, callback, !hasErrors[0]);
                            }
                        }
                    });
                }
            }

            @Override
            public void onException(Exception e) {
                callback.onException(e);
            }
        });
    }

    private void checkIsBusinessNotebook(final String notebookGuid, final OnEvernoteCallback<Boolean>callback) {
        getNoteStoreGuids(new OnEvernoteCallback<Void>() {
            public void onSuccess(Void data) {
                callback.onSuccess(mBusinessNotebookGuids.contains(notebookGuid));
            }

            public void onException(Exception e) {
                callback.onException(e);
            }
        });
    }

    void provideAsyncNoteStoreClientForNote(final Note note, final OnEvernoteCallback<AsyncNoteStoreClient>callback) {
        if (null == note.getNotebookGuid() || note.getNotebookGuid().equalsIgnoreCase(mUserNotebookGuid)) {
            try {
                callback.onSuccess(mEvernoteSession.getClientFactory().createNoteStoreClient());
            }
            catch (Exception e) {
                callback.onException(e);
            }
        }
        else {
            // business or linked note
            checkIsBusinessNotebook(note.getNotebookGuid(), new OnEvernoteCallback<Boolean>() {
                public void onSuccess(final Boolean isBusinessNotebook) {
                    if (isBusinessNotebook) {
                        mEvernoteSession.getClientFactory().createBusinessNoteStoreClientAsync(new OnClientCallback<AsyncBusinessNoteStoreClient>() {
                            public void onSuccess(AsyncBusinessNoteStoreClient client) {
                                callback.onSuccess(client.getAsyncClient());
                            }

                            public void onException(Exception e) {
                                callback.onException(e);
                            }
                        });
                    }
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
        if (null == note) {
            callback.onException(new Exception("Invalid EN reference: " + noteRefString));
            return;
        }

        provideAsyncNoteStoreClientForNote(note, new OnEvernoteCallback<AsyncNoteStoreClient>() {
            public void onSuccess(AsyncNoteStoreClient client) {
                client.getNote(note.getGuid(), true, false, false, false, new OnClientCallback<Note>() {
                    public void onSuccess(Note data) {
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
    }

    private List<Note> cacheGetSearchResult(final String query) {
        if (null != mSearchCache) {
            // remove old entries
            final long time = new Date().getTime();
            final HashMap<String, CacheData<List<Note>>> searchCacheCopy = new HashMap<String, CacheData<List<Note>>>(mSearchCache);
            for (String key : searchCacheCopy.keySet()) {
                final CacheData<List<Note>> data = searchCacheCopy.get(key);
                if (data.time + sSearchCacheTimeout < time) {
                    mSearchCache.remove(key);
                }
            }

            final CacheData<List<Note>> cacheData = mSearchCache.get(query);
            if (null != cacheData) {
                return cacheData.data;
            }
        }
        return null;
    }

    private void cacheAddSearchResults(final String query, List<Note> notes) {
        if (null == mSearchCache) {
            mSearchCache = new HashMap<String, CacheData<List<Note>>>();
        }
        mSearchCache.put(query, new CacheData<List<Note>>(notes));
    }

    private class CacheData<T> {
        T data;
        long time;

        private CacheData(T data) {
            this.data = data;
            this.time = new Date().getTime();
        }
    }
}
