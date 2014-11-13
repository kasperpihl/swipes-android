package com.swipesapp.android.sync.gson;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Gson mapping class for "objects" request parameter.
 *
 * @author Felipe Bari
 */
public class GsonObjects {

    @SerializedName("Tag")
    @Expose(deserialize = false)
    private List<GsonTag> tags;

    @SerializedName("ToDo")
    @Expose(deserialize = false)
    private List<GsonTask> tasks;

    public GsonObjects(List<GsonTag> tags, List<GsonTask> tasks) {
        this.tags = tags;
        this.tasks = tasks;
    }

}
