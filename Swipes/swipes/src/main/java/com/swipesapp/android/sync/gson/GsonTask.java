package com.swipesapp.android.sync.gson;

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
    private String parentLocalId;
    @Expose
    private String createdAt;
    @Expose
    private String updatedAt;
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
    private GsonDate completionDate;
    @Expose
    private GsonDate schedule;
    @Expose
    private String location;
    @Expose
    private GsonDate repeatDate;
    @Expose
    private String repeatOption;
    @Expose
    private String origin;
    @Expose
    private String originIdentifier;
    @Expose
    private List<GsonTag> tags;

    // TODO: Expose this when attachments sync is done.
    private List<GsonAttachment> attachments;

    // Local properties.
    private Long id;
    private long itemId;
    private boolean isSelected;

    // Local dates.
    private Date localCreatedAt;
    private Date localUpdatedAt;
    private Date localCompletionDate;
    private Date localSchedule;
    private Date localRepeatDate;

    /**
     * Returns a GsonTask object for use with the local database.
     *
     * @return GsonTask object.
     */
    public static GsonTask gsonForLocal(Long id, String objectId, String tempId, String parentLocalId, Date localCreatedAt, Date localUpdatedAt, Boolean deleted, String title, String notes, Integer order, Integer priority, Date localCompletionDate, Date localSchedule, String location, Date localRepeatDate, String repeatOption, String origin, String originIdentifier, List<GsonTag> tags, List<GsonAttachment> attachments, long itemId) {
        GsonTask task = new GsonTask();
        task.id = id;
        task.objectId = objectId;
        task.tempId = tempId;
        task.parentLocalId = parentLocalId;
        task.localCreatedAt = localCreatedAt;
        task.localUpdatedAt = localUpdatedAt;
        task.deleted = deleted;
        task.title = title;
        task.notes = notes;
        task.order = order;
        task.priority = priority;
        task.localCompletionDate = localCompletionDate;
        task.localSchedule = localSchedule;
        task.location = location;
        task.localRepeatDate = localRepeatDate;
        task.repeatOption = repeatOption;
        task.origin = origin;
        task.originIdentifier = originIdentifier;
        task.tags = tags;
        task.itemId = itemId;
        task.attachments = attachments;

        return task;
    }

    /**
     * Returns a GsonTask object for use with syncing.
     *
     * @return GsonTask object.
     */
    public static GsonTask gsonForSync(String objectId, String tempId, String parentLocalId, String createdAt, String updatedAt, Boolean deleted, String title, String notes, Integer order, Integer priority, String completionDate, String schedule, String location, String repeatDate, String repeatOption, String origin, String originIdentifier, List<GsonTag> tags) {
        GsonTask task = new GsonTask();
        task.objectId = objectId;
        task.tempId = tempId;
        task.parentLocalId = parentLocalId;
        task.createdAt = createdAt;
        task.updatedAt = updatedAt;
        task.deleted = deleted;
        task.title = title;
        task.notes = notes;
        task.order = order;
        task.priority = priority;
        task.completionDate = GsonDate.dateForSync(completionDate);
        task.schedule = GsonDate.dateForSync(schedule);
        task.location = location;
        task.repeatDate = GsonDate.dateForSync(repeatDate);
        task.repeatOption = repeatOption;
        task.origin = origin;
        task.originIdentifier = originIdentifier;
        task.tags = tags;

        // TODO: Include attachments when their sync is done.

        return task;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
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

    public void setParentLocalId(String parentLocalId) {
        this.parentLocalId = parentLocalId;
    }

    public String getParentLocalId() {
        return parentLocalId;
    }

    public void setLocalCreatedAt(Date localCreatedAt) {
        this.localCreatedAt = localCreatedAt;
    }

    public Date getLocalCreatedAt() {
        return localCreatedAt;
    }

    public void setLocalUpdatedAt(Date localUpdatedAt) {
        this.localUpdatedAt = localUpdatedAt;
    }

    public Date getLocalUpdatedAt() {
        return localUpdatedAt;
    }

    public void setDeleted(Boolean deleted) {
        this.deleted = deleted;
    }

    public Boolean isDeleted() {
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

    public void setLocalCompletionDate(Date localCompletionDate) {
        this.localCompletionDate = localCompletionDate;
    }

    public Date getLocalCompletionDate() {
        return localCompletionDate;
    }

    public void setLocalSchedule(Date localSchedule) {
        this.localSchedule = localSchedule;
    }

    public Date getLocalSchedule() {
        return localSchedule;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getLocation() {
        return location;
    }

    public void setLocalRepeatDate(Date localRepeatDate) {
        this.localRepeatDate = localRepeatDate;
    }

    public Date getLocalRepeatDate() {
        return localRepeatDate;
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

    public String getOrigin() {
        return origin;
    }

    public void setOrigin(String origin) {
        this.origin = origin;
    }

    public String getOriginIdentifier() {
        return originIdentifier;
    }

    public void setOriginIdentifier(String originIdentifier) {
        this.originIdentifier = originIdentifier;
    }

    public long getItemId() {
        return itemId;
    }

    public void setItemId(long itemId) {
        this.itemId = itemId;
    }

    public boolean isSelected() {
        return isSelected;
    }

    public void setSelected(boolean isSelected) {
        this.isSelected = isSelected;
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

    public Boolean getDeleted() {
        return deleted;
    }

    public String getCompletionDate() {
        return completionDate != null ? completionDate.getDate() : null;
    }

    public void setCompletionDate(String completionDate) {
        this.completionDate = GsonDate.dateForSync(completionDate);
    }

    public String getSchedule() {
        return schedule != null ? schedule.getDate() : null;
    }

    public void setSchedule(String schedule) {
        this.schedule = GsonDate.dateForSync(schedule);
    }

    public String getRepeatDate() {
        return repeatDate != null ? repeatDate.getDate() : null;
    }

    public void setRepeatDate(String repeatDate) {
        this.repeatDate = GsonDate.dateForSync(repeatDate);
    }

    public List<GsonAttachment> getAttachments() {
        return attachments;
    }

    public void setAttachments(List<GsonAttachment> attachments) {
        this.attachments = attachments;
    }

    public void addAttachment(GsonAttachment attachment) {
        attachments.add(attachment);
    }

    public void removeAttachment(GsonAttachment attachment) {
        if (attachments != null) {
            GsonAttachment match = null;

            for (GsonAttachment gsonAttachment : attachments) {
                Long id = gsonAttachment.getId();
                String identifier = gsonAttachment.getIdentifier();

                if (id.equals(attachment.getId()) || identifier.equals(attachment.getIdentifier())) {
                    match = gsonAttachment;
                }
            }

            attachments.remove(match);
        }
    }

}
