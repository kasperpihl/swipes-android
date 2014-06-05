package com.swipesapp.android.gson;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;

/**
 * Gson mapping class for "objects" request parameter.
 *
 * @author Felipe Bari
 */
public class GsonObjects {

    @SerializedName("ToDo")
    @Expose(deserialize = false)
    private ArrayList<GsonTask> tasks;

    @SerializedName("Tag")
    @Expose(deserialize = false)
    private ArrayList<GsonTag> tags;

    public GsonObjects(ArrayList<GsonTask> tasks, ArrayList<GsonTag> tags) {
        this.tasks = tasks;
        this.tags = tags;
    }

}
