package com.swipesapp.android.db;

// THIS CODE IS GENERATED BY greenDAO, EDIT ONLY INSIDE THE "KEEP"-SECTIONS

// KEEP INCLUDES - put your custom includes here
// KEEP INCLUDES END
/**
 * Entity mapped to table DELETED.
 */
public class Deleted {

    private Long id;
    private String className;
    private String objectId;
    private Boolean deleted;

    // KEEP FIELDS - put your custom fields here
    // KEEP FIELDS END

    public Deleted() {
    }

    public Deleted(Long id) {
        this.id = id;
    }

    public Deleted(Long id, String className, String objectId, Boolean deleted) {
        this.id = id;
        this.className = className;
        this.objectId = objectId;
        this.deleted = deleted;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    // KEEP METHODS - put your custom methods here
    // KEEP METHODS END

}