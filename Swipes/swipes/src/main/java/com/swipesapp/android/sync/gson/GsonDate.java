package com.swipesapp.android.sync.gson;

import com.google.gson.annotations.Expose;

/**
 * Gson mapping class for date objects.
 * <p/>
 * All objects are compliant with the Parse REST API, following the format
 * {
 * "__type": "Date",
 * "iso": "2014-11-06T12:30:00.000Z"
 * }
 *
 * @author Felipe Bari
 */
public class GsonDate {

    @Expose
    private String __type;

    @Expose
    private String iso;

    /**
     * Returns a GsonDate object with the given date.
     *
     * @param date Date string in the format "2014-11-06T12:30:00.000Z"
     */
    public static GsonDate dateForSync(String date) {
        GsonDate gsonDate = new GsonDate();
        gsonDate.__type = (date == null || date.equals("null")) ? null : "Date";
        gsonDate.iso = date;

        return (date != null && !date.isEmpty()) ? gsonDate : null;
    }

    public String getDate() {
        return iso;
    }

}
