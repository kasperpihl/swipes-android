package com.swipesapp.android.gson;

import com.google.gson.annotations.Expose;

import java.util.Date;

/**
 * Gson mapping class for tag fields.
 *
 * @author Felipe Bari
 */
public class GsonTag {

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

    public GsonTag(String objectId, String tempId, Date createdAt, Date updatedAt, String title) {
        this.objectId = objectId;
        this.tempId = tempId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.title = title;
    }

    public void setObjectId(String objectId) {
        this.objectId = objectId;
    }

    public String getObjectId() {
        return objectId;
    }

    public void setTempId(String tempId) {
        this.tempId = tempId;
    }

    public String getTempId() {
        return tempId;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }

}
