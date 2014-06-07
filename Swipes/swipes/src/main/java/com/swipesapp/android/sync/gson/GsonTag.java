package com.swipesapp.android.sync.gson;

import com.google.gson.annotations.Expose;

import java.util.Date;

/**
 * Gson mapping class for tag fields.
 *
 * @author Felipe Bari
 */
public class GsonTag {

    private Long tagId;
    @Expose
    private String objectId;
    @Expose
    private String tempId;
    @Expose
    private Date createdAt;
    @Expose
    private Date updatedAt;
    @Expose
    private String title;

    public GsonTag(Long tagId, String objectId, String tempId, Date createdAt, Date updatedAt, String title) {
        this.tagId = tagId;
        this.objectId = objectId;
        this.tempId = tempId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.title = title;
    }

    public Long getTagId() {
        return tagId;
    }

    public String getObjectId() {
        return objectId;
    }

    public String getTempId() {
        return tempId;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public String getTitle() {
        return title;
    }

}
