package com.swipesapp.android.values;

/**
 * Holds the possible service integrations.
 *
 * @author Felipe Bari
 */
public enum Services {

    EVERNOTE("evernote");

    private String mValue;

    Services(String value) {
        mValue = value;
    }

    public String getValue() {
        return mValue;
    }

}
