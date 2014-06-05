package com.swipesapp.android.gson;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.Date;

/**
 * Gson mapping class for "sync" endpoint.
 *
 * @author Felipe Bari
 */
public class GsonSync {

    // Request fields.
    @Expose(deserialize = false)
    private String sessionToken;

    @Expose(deserialize = false)
    private String platform;

    @Expose(deserialize = false)
    private String version;

    @Expose(deserialize = false)
    private Boolean changesOnly;

    @Expose(deserialize = false)
    private Date lastUpdate;

    @Expose(deserialize = false)
    private GsonObjects objects;

    // Response fields.
    @Expose(serialize = false)
    private Date serverTime;

    @Expose(serialize = false)
    private Date updateTime;

    @SerializedName("ToDo")
    @Expose(serialize = false)
    private ArrayList<GsonTask> tasks;

    @SerializedName("Tag")
    @Expose(serialize = false)
    private ArrayList<GsonTag> tags;

    public GsonSync(String sessionToken, String platform, String version, Boolean changesOnly, Date lastUpdate, GsonObjects objects) {
        this.sessionToken = sessionToken;
        this.platform = platform;
        this.version = version;
        this.changesOnly = changesOnly;
        this.lastUpdate = lastUpdate;
        this.objects = objects;
    }

    public Date getServerTime() {
        return serverTime;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public ArrayList<GsonTask> getTasks() {
        return tasks;
    }

    public ArrayList<GsonTag> getTags() {
        return tags;
    }

}
