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
    private Boolean sync;

    // Local properties.
    private Long id;

    public GsonAttachment(Long id, String identifier, String service, String title, Boolean sync) {
        this.id = id;
        this.identifier = identifier;
        this.service = service;
        this.title = title;
        this.sync = sync;
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
        return sync;
    }

    public void setSync(Boolean sync) {
        this.sync = sync;
    }

}
