package com.swipesapp.android.gson;

import com.google.gson.annotations.Expose;

import java.util.Date;
import java.util.List;

/**
 * Gson mapping class for task fields.
 *
 * @author Felipe Bari
 */
public class GsonTask {

    @Expose
    private String objectId;
    @Expose
    private String tempId;
    @Expose
    private String parentId;
    @Expose
    private Date createdAt;
    @Expose
    private Date updatedAt;
    @Expose
    private Boolean deleted;
    @Expose
    private String title;
    @Expose
    private String notes;
    @Expose
    private Integer order;
    @Expose
    private Integer priority;
    @Expose
    private Date completionDate;
    @Expose
    private Date schedule;
    @Expose
    private String location;
    @Expose
    private Date repeatDate;
    @Expose
    private String repeatOption;
    @Expose
    private List<GsonTag> tags;

    public GsonTask(String objectId, String tempId, String parentId, Date createdAt, Date updatedAt, Boolean deleted, String title, String notes, Integer order, Integer priority, Date completionDate, Date schedule, String location, Date repeatDate, String repeatOption, List<GsonTag> tags) {
        this.objectId = objectId;
        this.tempId = tempId;
        this.parentId = parentId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.deleted = deleted;
        this.title = title;
        this.notes = notes;
        this.order = order;
        this.priority = priority;
        this.completionDate = completionDate;
        this.schedule = schedule;
        this.location = location;
        this.repeatDate = repeatDate;
        this.repeatOption = repeatOption;
        this.tags = tags;
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

    public void setParentId(String parentId) {
        this.parentId = parentId;
    }

    public String getParentId() {
        return parentId;
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

    public void setDeleted(Boolean deleted) {
        this.deleted = deleted;
    }

    public Boolean getDeleted() {
        return deleted;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getNotes() {
        return notes;
    }

    public void setOrder(Integer order) {
        this.order = order;
    }

    public Integer getOrder() {
        return order;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setCompletionDate(Date completionDate) {
        this.completionDate = completionDate;
    }

    public Date getCompletionDate() {
        return completionDate;
    }

    public void setSchedule(Date schedule) {
        this.schedule = schedule;
    }

    public Date getSchedule() {
        return schedule;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getLocation() {
        return location;
    }

    public void setRepeatDate(Date repeatDate) {
        this.repeatDate = repeatDate;
    }

    public Date getRepeatDate() {
        return repeatDate;
    }

    public void setRepeatOption(String repeatOption) {
        this.repeatOption = repeatOption;
    }

    public String getRepeatOption() {
        return repeatOption;
    }

    public void setTags(List<GsonTag> tags) {
        this.tags = tags;
    }

    public List<GsonTag> getTags() {
        return tags;
    }

}
