package com.swipesapp.android.values;

import android.util.Log;

/**
 * Holds the possible sections.
 *
 * @author Felipe Bari
 */
public enum Sections {

    LATER(0),
    FOCUS(1),
    DONE(2),
    SETTINGS(3);

    private static final int SECTIONS_COUNT = 4;

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
                return LATER;
            case 1:
                return FOCUS;
            case 2:
                return DONE;
            case 3:
                return SETTINGS;
            default:
                Log.wtf(TAG, "Section does not exist.");
                return null;
        }
    }

    public static int getSectionsCount() {
        return SECTIONS_COUNT;
    }

}
