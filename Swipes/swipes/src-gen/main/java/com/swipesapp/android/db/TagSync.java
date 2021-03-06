package com.swipesapp.android.db;

// THIS CODE IS GENERATED BY greenDAO, EDIT ONLY INSIDE THE "KEEP"-SECTIONS

// KEEP INCLUDES - put your custom includes here
// KEEP INCLUDES END
/**
 * Entity mapped to table TAG_SYNC.
 */
public class TagSync {

    private Long id;
    private String objectId;
    private String tempId;
    private String createdAt;
    private String updatedAt;
    private String title;
    private Boolean deleted;

    // KEEP FIELDS - put your custom fields here
    // KEEP FIELDS END

    public TagSync() {
    }

    public TagSync(Long id) {
        this.id = id;
    }

    public TagSync(Long id, String objectId, String tempId, String createdAt, String updatedAt, String title, Boolean deleted) {
        this.id = id;
        this.objectId = objectId;
        this.tempId = tempId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.title = title;
        this.deleted = deleted;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getObjectId() {
        return objectId;
    }

    public void setObjectId(String objectId) {
        this.objectId = objectId;
    }

    public String getTempId() {
        return tempId;
    }

    public void setTempId(String tempId) {
        this.tempId = tempId;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
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
