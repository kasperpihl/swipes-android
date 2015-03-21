package com.swipesapp.android.sync.gson;

import com.google.gson.annotations.Expose;

/**
 * Gson mapping class for attachments.
 *
 * @author Felipe Bari
 */
public class GsonAttachment {

    @Expose
    private String identifier;
    @Expose
    private String service;
    @Expose
    private String title;
    @Expose
    private Integer sync;

    // Local properties.
    private Long id;

    public GsonAttachment(Long id, String identifier, String service, String title, Boolean sync) {
        this.id = id;
        this.identifier = identifier;
        this.service = service;
        this.title = title;
        this.sync = sync ? 1 : 0;
    }

    public Long getId() {
        return id;
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public String getService() {
        return service;
    }

    public void setService(String service) {
        this.service = service;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Boolean getSync() {
        return sync != null && sync == 1;
    }

    public void setSync(Boolean sync) {
        this.sync = sync ? 1 : 0;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj != null && obj instanceof GsonAttachment) {
            if (((GsonAttachment) obj).getIdentifier().equals(identifier)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int hashCode() {
        return identifier.hashCode();
    }

}
