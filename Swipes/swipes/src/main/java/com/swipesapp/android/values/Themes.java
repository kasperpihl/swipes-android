package com.swipesapp.android.values;

import android.content.Context;
import android.util.Log;

import com.swipesapp.android.R;

/**
 * Holds the possible themes.
 *
 * @author Felipe Bari
 */
public enum Themes {

    LIGHT("light"),
    DARK("dark");

    private static final String TAG = Themes.class.getSimpleName();

    private String mName;

    Themes(String name) {
        mName = name;
    }

    public String getName() {
        return mName;
    }

    public static Themes getThemeByName(String name) {
        if (name.equalsIgnoreCase(LIGHT.getName())) {
            return LIGHT;
        } else if (name.equalsIgnoreCase(DARK.getName())) {
            return DARK;
        } else {
            Log.wtf(TAG, "Theme does not exist.");
            return null;
        }
    }

    public String getDescription(Context context) {
        switch (this) {
            case LIGHT:
                return context.getString(R.string.theme_light);
            case DARK:
                return context.getString(R.string.theme_dark);
            default:
                Log.wtf(TAG, "Theme does not exist.");
                return "";
        }
    }

}
