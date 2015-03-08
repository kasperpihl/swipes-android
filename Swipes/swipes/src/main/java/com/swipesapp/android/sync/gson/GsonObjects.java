package com.swipesapp.android.sync.gson;

import com.google.gson.annotations.SerializedName;
import com.swipesapp.android.db.Tag;
import com.swipesapp.android.db.Task;

import java.util.List;

/**
 * Gson mapping class for "objects" request parameter.
 *
 * @author Felipe Bari
 */
public class GsonObjects {

    @SerializedName("Tag")
    private List<Tag> tags;

    @SerializedName("ToDo")
    private List<Task> tasks;

    public GsonObjects(List<Tag> tags, List<Task> tasks) {
        this.tags = tags;
        this.tasks = tasks;
    }

}
