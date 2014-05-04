package com.swipesapp.android.util;

import android.content.Context;
import android.graphics.Color;
import android.util.DisplayMetrics;

import com.swipesapp.android.R;
import com.swipesapp.android.values.Sections;
import com.swipesapp.android.values.Themes;

/**
 * Utilitary class for common operations.
 *
 * @author Felipe Bari
 */
public class Utils {

    /**
     * Color applied in case the appropriate ones are missing.
     */
    private static final int DEFAULT_COLOR = Color.WHITE;

    /**
     * @return Current selected theme.
     */
    public static Themes getCurrentTheme() {
        // TODO: Return a value loaded from preferences.
        return Themes.LIGHT;
    }

    /**
     * @param context Context to use.
     * @return Current theme's background color.
     */
    public static int getCurrentThemeBackgroundColor(Context context) {
        switch (getCurrentTheme()) {
            case LIGHT:
                return context.getResources().getColor(R.color.light_theme_background);
            case DARK:
                return context.getResources().getColor(R.color.dark_theme_background);
            default:
                return context.getResources().getColor(R.color.light_theme_background);
        }
    }

    /**
     * @param context Context to use.
     * @return Current theme's text color.
     */
    public static int getCurrentThemeTextColor(Context context) {
        switch (getCurrentTheme()) {
            case LIGHT:
                return context.getResources().getColor(R.color.light_theme_text);
            case DARK:
                return context.getResources().getColor(R.color.dark_theme_text);
            default:
                return context.getResources().getColor(R.color.light_theme_text);
        }
    }

    /**
     * @param context Context to use.
     * @return Accent color for the chosen section.
     */
    public static int getSectionColor(Sections section, Context context) {
        switch (section) {
            case FOCUS:
                return context.getResources().getColor(R.color.focus_accent_color);
            case LATER:
                return context.getResources().getColor(R.color.later_accent_color);
            case DONE:
                return context.getResources().getColor(R.color.done_accent_color);
            default:
                return DEFAULT_COLOR;
        }
    }

    /**
     * Converts a dpi value to pixels.
     *
     * @param dpi     Dpi value.
     * @param context Context to use.
     * @return Value in pixels.
     */
    public int convertDpiToPixel(float dpi, Context context) {
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        int px = Math.round(dpi * metrics.density);
        return px;
    }

}
