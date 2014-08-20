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
     * @return True if the current theme is the light theme.
     */
    public static boolean isLightTheme(Context context) {
        return getCurrentTheme(context) == Themes.LIGHT;
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
     * @return Current theme's dialog resource file.
     */
    public static int getCurrentDialogThemeResource(Context context) {
        switch (getCurrentTheme(context)) {
            case LIGHT:
                return R.style.Light_Theme_Dialog;
            case DARK:
                return R.style.Dark_Theme_Dialog;
            default:
                return R.style.Light_Theme_Dialog;
        }
    }

    /**
     * @param context Context to use.
     * @return Current theme's picker resource file.
     */
    public static int getCurrentPickerThemeResource(Context context) {
        switch (getCurrentTheme(context)) {
            case LIGHT:
                return R.style.Light_Picker;
            case DARK:
                return R.style.Dark_Picker;
            default:
                return R.style.Light_Picker;
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

    /**
     * @param context Context to use.
     * @return Current theme's tab background resource.
     */
    public static int getCurrentThemeTabBackground(Context context) {
        switch (getCurrentTheme(context)) {
            case LIGHT:
                return R.drawable.background_tab_light;
            case DARK:
                return R.drawable.background_tab_dark;
            default:
                return R.drawable.background_tab_light;
        }
    }

    /**
     * @param context Context to use.
     * @return Current theme's edit text background resource.
     */
    public static int getCurrentThemeEditTextBackground(Context context) {
        switch (getCurrentTheme(context)) {
            case LIGHT:
                return R.drawable.light_theme_edit_text_background;
            case DARK:
                return R.drawable.dark_theme_edit_text_background;
            default:
                return R.drawable.light_theme_edit_text_background;
        }
    }

    /**
     * @param context Context to use.
     * @return Current theme's transition background resource.
     */
    public static int getCurrentThemeTransitionBackground(Context context) {
        switch (getCurrentTheme(context)) {
            case LIGHT:
                return R.drawable.background_transition_light;
            case DARK:
                return R.drawable.background_transition_dark;
            default:
                return R.drawable.background_transition_light;
        }
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

}
