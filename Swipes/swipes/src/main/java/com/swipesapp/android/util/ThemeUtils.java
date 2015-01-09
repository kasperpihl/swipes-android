package com.swipesapp.android.util;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;

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
     * @return True if the current theme is the light theme.
     */
    public static boolean isLightTheme(Context context) {
        return getCurrentTheme(context) == Themes.LIGHT;
    }

    /**
     * @param context Context to use.
     * @return Current theme's resource file.
     */
    public static int getThemeResource(Context context) {
        return isLightTheme(context) ? R.style.Light_Theme : R.style.Dark_Theme;
    }

    /**
     * @param context Context to use.
     * @return Current theme's dialog resource file.
     */
    public static int getDialogThemeResource(Context context) {
        return isLightTheme(context) ? R.style.Light_Theme_Dialog : R.style.Dark_Theme_Dialog;
    }

    /**
     * @param context Context to use.
     * @return Current theme's picker resource file.
     */
    public static int getPickerThemeResource(Context context) {
        return isLightTheme(context) ? R.style.Light_Picker : R.style.Dark_Picker;
    }

    /**
     * @param context Context to use.
     * @return Current theme's background color.
     */
    public static int getBackgroundColor(Context context) {
        int color = isLightTheme(context) ? R.color.light_theme_background : R.color.dark_theme_background;
        return context.getResources().getColor(color);
    }

    /**
     * @param context Context to use.
     * @return Current theme's text color.
     */
    public static int getTextColor(Context context) {
        int color = isLightTheme(context) ? R.color.light_theme_text : R.color.dark_theme_text;
        return context.getResources().getColor(color);
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
     * @return Accent color resource for the chosen section.
     */
    public static int getSectionColorResource(Sections section) {
        switch (section) {
            case FOCUS:
                return R.color.focus_accent_color;
            case LATER:
                return R.color.later_accent_color;
            case DONE:
                return R.color.done_accent_color;
            default:
                return Color.TRANSPARENT;
        }
    }

    /**
     * @param context Context to use.
     * @return Current theme's divider color.
     */
    public static int getDividerColor(Context context) {
        int color = isLightTheme(context) ? R.color.light_theme_divider : R.color.dark_theme_divider;
        return context.getResources().getColor(color);
    }

    /**
     * @param context Context to use.
     * @return Current theme's tab background resource.
     */
    public static int getTabBackground(Context context) {
        return isLightTheme(context) ? R.drawable.background_tab_light : R.drawable.background_tab_dark;
    }

    /**
     * @param context Context to use.
     * @return Current theme's transition background resource.
     */
    public static int getTransitionBackground(Context context) {
        return isLightTheme(context) ? R.drawable.background_transition_light : R.drawable.background_transition_dark;
    }

    /**
     * @param context Context to use.
     * @return Current theme's snooze blur alpha color.
     */
    public static int getSnoozeBlurAlphaColor(Context context) {
        int color = isLightTheme(context) ? R.color.light_snooze_blur_alpha_color : R.color.dark_snooze_blur_alpha_color;
        return context.getResources().getColor(color);
    }

    /**
     * @param context Context to use.
     * @return Current theme's tasks blur alpha color.
     */
    public static int getTasksBlurAlphaColor(Context context) {
        int color = isLightTheme(context) ? R.color.light_tasks_blur_alpha_color : R.color.dark_tasks_blur_alpha_color;
        return context.getResources().getColor(color);
    }

    /**
     * @param context Context to use.
     * @return Current theme's gradient drawable.
     */
    public static Drawable getGradientDrawable(Context context) {
        int drawable = isLightTheme(context) ? R.drawable.light_gradient : R.drawable.dark_gradient;
        return context.getResources().getDrawable(drawable);
    }

}
