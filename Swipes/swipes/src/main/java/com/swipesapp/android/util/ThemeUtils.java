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
    public static int getThemeResource(Context context) {
        return isLightTheme(context) ? R.style.Light_Theme : R.style.Dark_Theme;
    }

    /**
     * @param context Context to use.
     * @return Current theme's dialog resource file.
     */
    public static int getDialogThemeResource(Context context) {
        return isLightTheme(context) ? R.style.Dialog_Theme_Light : R.style.Dialog_Theme_Dark;
    }

    /**
     * @param context Context to use.
     * @return Current theme's background color.
     */
    public static int getBackgroundColor(Context context) {
        int color = isLightTheme(context) ? R.color.light_background : R.color.dark_background;
        return context.getResources().getColor(color);
    }

    /**
     * @param context Context to use.
     * @return Current theme's neutral background color.
     */
    public static int getNeutralBackgroundColor(Context context) {
        int color = isLightTheme(context) ? R.color.light_neutral_background : R.color.dark_neutral_background;
        return context.getResources().getColor(color);
    }

    /**
     * @param context Context to use.
     * @return Current theme's text color.
     */
    public static int getTextColor(Context context) {
        int color = isLightTheme(context) ? R.color.light_text : R.color.dark_text;
        return context.getResources().getColor(color);
    }

    /**
     * @param context Context to use.
     * @return Current theme's secondary text color.
     */
    public static int getSecondaryTextColor(Context context) {
        int color = isLightTheme(context) ? R.color.light_secondary_text : R.color.dark_secondary_text;
        return context.getResources().getColor(color);
    }

    /**
     * @param context Context to use.
     * @return Accent color for the chosen section.
     */
    public static int getSectionColor(Sections section, Context context) {
        switch (section) {
            case FOCUS:
                return context.getResources().getColor(R.color.focus_accent);
            case LATER:
                return context.getResources().getColor(R.color.later_accent);
            case DONE:
                return context.getResources().getColor(R.color.done_accent);
            default:
                return Color.TRANSPARENT;
        }
    }

    /**
     * @param context Context to use.
     * @return Dark accent color for the chosen section.
     */
    public static int getSectionColorDark(Sections section, Context context) {
        switch (section) {
            case FOCUS:
                return context.getResources().getColor(R.color.focus_accent_dark);
            case LATER:
                return context.getResources().getColor(R.color.later_accent_dark);
            case DONE:
                return context.getResources().getColor(R.color.done_accent_dark);
            default:
                return Color.TRANSPARENT;
        }
    }

    /**
     * @param context Context to use.
     * @return Current theme's divider color.
     */
    public static int getDividerColor(Context context) {
        int color = isLightTheme(context) ? R.color.light_divider : R.color.dark_divider;
        return context.getResources().getColor(color);
    }

    /**
     * @param context Context to use.
     * @return Current theme's toolbar popup theme.
     */
    public static int getToolbarPopupTheme(Context context) {
        return isLightTheme(context) ? R.style.Light_Theme_Popup_Menu : R.style.Dark_Theme_Popup_Menu;
    }

    /**
     * @param context Context to use.
     * @return Current theme's dialog background resource.
     */
    public static int getDialogBackground(Context context) {
        return isLightTheme(context) ? R.drawable.dialog_light : R.drawable.dialog_dark;
    }

}
