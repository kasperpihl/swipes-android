package com.swipesapp.android.sync.gson;

import com.google.gson.annotations.Expose;

/**
 * Gson mapping class for deleted objects.
 *
 * @author Felipe Bari
 */
public class GsonDeleted {

    @Expose
    private String className;
    @Expose
    private String objectId;
    @Expose
    private Boolean deleted;

    // Local properties.
    private Long id;

    public GsonDeleted(Long id, String className, String objectId, Boolean deleted) {
        this.id = id;
        this.className = className;
        this.objectId = objectId;
        this.deleted = deleted;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getObjectId() {
        return objectId;
    }

    public void setObjectId(String objectId) {
        this.objectId = objectId;
    }

    public Boolean getDeleted() {
        return deleted;
    }

    public void setDeleted(Boolean deleted) {
        this.deleted = deleted;
    }

}
