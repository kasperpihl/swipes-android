package com.swipesapp.android.values;

import android.content.Context;
import android.util.Log;

import com.swipesapp.android.R;
import com.swipesapp.android.analytics.Screens;

/**
 * Holds the possible sections.
 *
 * @author Felipe Bari
 */
public enum Sections {

    LATER(0),
    FOCUS(1),
    DONE(2);

    private static final int SECTIONS_COUNT = 3;

    private static final String TAG = Sections.class.getSimpleName();

    private int mSectionNumber;

    Sections(int sectionNumber) {
        mSectionNumber = sectionNumber;
    }

    public int getSectionNumber() {
        return mSectionNumber;
    }

    public String getSectionTitle(Context context) {
        switch (mSectionNumber) {
            case 0:
                return context.getString(R.string.later_title);
            case 1:
                return context.getString(R.string.focus_title);
            case 2:
                return context.getString(R.string.done_title);
            default:
                Log.wtf(TAG, "Section does not exist.");
                return null;
        }
    }

    public String getSectionIcon(Context context) {
        switch (mSectionNumber) {
            case 0:
                return context.getString(R.string.later_full);
            case 1:
                return context.getString(R.string.focus_full);
            case 2:
                return context.getString(R.string.done_full);
            default:
                Log.wtf(TAG, "Section does not exist.");
                return null;
        }
    }

    public String getScreenName() {
        switch (mSectionNumber) {
            case 0:
                return Screens.SCREEN_LATER;
            case 1:
                return Screens.SCREEN_FOCUS;
            case 2:
                return Screens.SCREEN_DONE;
            default:
                Log.wtf(TAG, "Section does not exist.");
                return null;
        }
    }

    public static Sections getSectionByNumber(int sectionNumber) {
        switch (sectionNumber) {
            case 0:
                return LATER;
            case 1:
                return FOCUS;
            case 2:
                return DONE;
            default:
                Log.wtf(TAG, "Section does not exist.");
                return null;
        }
    }

    public static int getSectionsCount() {
        return SECTIONS_COUNT;
    }

}
