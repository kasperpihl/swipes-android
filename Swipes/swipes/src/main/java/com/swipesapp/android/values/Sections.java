package com.swipesapp.android.values;

import android.util.Log;

/**
 * Holds the possible sections.
 *
 * @author Felipe Bari
 */
public enum Sections {

    SETTINGS(0),
    LATER(1),
    FOCUS(2),
    DONE(3),
    FILTERS(4);

    private static final int SECTIONS_COUNT = 5;

    private static final String TAG = Sections.class.getSimpleName();

    private int mSectionNumber;

    Sections(int sectionNumber) {
        mSectionNumber = sectionNumber;
    }

    public int getSectionNumber() {
        return mSectionNumber;
    }

    public static Sections getSectionByNumber(int sectionNumber) {
        switch (sectionNumber) {
            case 0:
                return SETTINGS;
            case 1:
                return LATER;
            case 2:
                return FOCUS;
            case 3:
                return DONE;
            case 4:
                return FILTERS;
            default:
                Log.wtf(TAG, "Section does not exist.");
                return null;
        }
    }

    public static int getSectionsCount() {
        return SECTIONS_COUNT;
    }

}
