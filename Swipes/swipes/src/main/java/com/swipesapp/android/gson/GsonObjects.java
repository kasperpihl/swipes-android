package com.swipesapp.android.gson;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Gson mapping class for "objects" request parameter.
 *
 * @author Felipe Bari
 */
public class GsonObjects {

    @SerializedName("ToDo")
    @Expose(deserialize = false)
    private List<GsonTask> tasks;

    @SerializedName("Tag")
    @Expose(deserialize = false)
    private List<GsonTag> tags;

    public GsonObjects(List<GsonTask> tasks, List<GsonTag> tags) {
        this.tasks = tasks;
        this.tags = tags;
    }

}
