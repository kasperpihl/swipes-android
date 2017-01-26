package com.swipesapp.android.handler;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.util.DisplayMetrics;

import com.swipesapp.android.util.PreferenceUtils;
import com.swipesapp.android.values.Languages;

import java.util.Locale;

/**
 * Handler to deal with language.
 *
 * @author Fernanda Bari
 */
public class LanguageHandler {

    /**
     * Applies user selected language to the app.
     *
     * @param context Context instance.
     */
    public static void applyLanguage(Context context) {
        Resources res = context.getResources();
        Locale systemLocale = Locale.getDefault();
        String userLocale = PreferenceUtils.readString(PreferenceUtils.LOCALE_KEY, context);

        if (userLocale != null) {
            String[] splitLocale = userLocale.split("_");
            String language = splitLocale[0];
            String country = splitLocale.length > 1 ? splitLocale[1] : null;

            // Apply special treatment for Chinese.
            if (userLocale.equalsIgnoreCase(Languages.CHINESE_SIMPLIFIED.getLocale())) {
                country = systemLocale.getCountry();

                if (!Languages.isChineseSimplifiedCountry(country)) {
                    country = "SG";
                }
            }

            // Change locale in the app.
            DisplayMetrics metrics = res.getDisplayMetrics();
            Configuration config = res.getConfiguration();

            if (country != null) {
                config.locale = new Locale(language, country);
            } else {
                config.locale = new Locale(language);
            }

            res.updateConfiguration(config, metrics);
        } else {
            userLocale = systemLocale.toString();
            String language = systemLocale.getLanguage();
            String country = systemLocale.getCountry();

            // Apply treatment for Chinese and English.
            if (language.equalsIgnoreCase(Languages.ENGLISH.getLocale())) {
                userLocale = language;
            } else if (Languages.isChineseSimplifiedCountry(country)) {
                userLocale = Languages.CHINESE_SIMPLIFIED.getLocale();
            }

            // Save system locale as user locale.
            PreferenceUtils.saveString(PreferenceUtils.LOCALE_KEY, userLocale, context);
        }
    }

}
