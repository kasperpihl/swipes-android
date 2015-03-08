package com.swipesapp.android.sync.gson;

import com.google.gson.annotations.SerializedName;
import com.swipesapp.android.db.Tag;
import com.swipesapp.android.db.Task;

import java.util.List;

/**
 * Gson mapping class for "sync" endpoint.
 *
 * @author Felipe Bari
 */
public class GsonSync {

    // Request fields.
    private String sessionToken;

    private String platform;

    private String version;

    private Boolean changesOnly;

    private String lastUpdate;

    private GsonObjects objects;

    // Response fields.
    private String serverTime;

    private String updateTime;

    @SerializedName("ToDo")
    private List<Task> tasks;

    @SerializedName("Tag")
    private List<Tag> tags;

    public GsonSync(String sessionToken, String platform, String version, Boolean changesOnly, String lastUpdate, GsonObjects objects) {
        this.sessionToken = sessionToken;
        this.platform = platform;
        this.version = version;
        this.changesOnly = changesOnly;
        this.lastUpdate = lastUpdate;
        this.objects = objects;
    }

    public String getServerTime() {
        return serverTime;
    }

    public String getUpdateTime() {
        return updateTime;
    }

    public List<Task> getTasks() {
        return tasks;
    }

    public List<Tag> getTags() {
        return tags;
    }

}
