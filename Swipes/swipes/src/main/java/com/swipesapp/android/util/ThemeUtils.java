package com.swipesapp.android.util;

import android.content.Context;
import android.graphics.Color;

import com.swipesapp.android.R;
import com.swipesapp.android.values.Sections;
import com.swipesapp.android.values.Themes;

/**
 * Utilitary class for theme operations.
 *
 * @author Felipe Bari
 */
public class ThemeUtils {

    /**
     * @param context Context to use.
     * @return Current selected theme.
     */
    public static Themes getCurrentTheme(Context context) {
        String theme = PreferenceUtils.readThemeSetting(context);
        if (theme != null) {
            return Themes.getThemeByName(theme);
        } else {
            return Themes.LIGHT;
        }
    }

    /**
     * @param context Context to use.
     * @return Current theme's resource file.
     */
    public static int getCurrentThemeResource(Context context) {
        switch (getCurrentTheme(context)) {
            case LIGHT:
                return R.style.Light_Theme;
            case DARK:
                return R.style.Dark_Theme;
            default:
                return R.style.Light_Theme;
        }
    }

    /**
     * @param context Context to use.
     * @return Current theme's background color.
     */
    public static int getCurrentThemeBackgroundColor(Context context) {
        switch (getCurrentTheme(context)) {
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
        switch (getCurrentTheme(context)) {
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
                return Color.TRANSPARENT;
        }
    }

    /**
     * @param context Context to use.
     * @return Current theme's divider color.
     */
    public static int getCurrentThemeDividerColor(Context context) {
        switch (getCurrentTheme(context)) {
            case LIGHT:
                return context.getResources().getColor(R.color.light_theme_divider);
            case DARK:
                return context.getResources().getColor(R.color.dark_theme_divider);
            default:
                return context.getResources().getColor(R.color.light_theme_divider);
        }
    }

}
