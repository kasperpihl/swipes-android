package com.swipesapp.android.values;

import android.content.Context;
import android.util.Log;

import com.swipesapp.android.R;

/**
 * Holds the possible languages.
 *
 * @author Felipe Bari
 */
public enum Languages {

    ENGLISH("en"),
    CZECH("cs_CZ"),
    SPANISH_SPAIN("es_ES"),
    ITALIAN("it_IT"),
    POLISH("pl_PL"),
    PORTUGUESE_BRAZIL("pt_BR"),
    RUSSIAN("ru_RU"),
    TURKISH("tr_TR"),
    CHINESE_SIMPLIFIED("zh_HANS");

    private static final String TAG = Languages.class.getSimpleName();

    private String mLocale;

    Languages(String locale) {
        mLocale = locale;
    }

    public String getLocale() {
        return mLocale;
    }

    public static Languages getLanguageByLocale(String locale) {
        if (locale != null) {
            if (locale.equalsIgnoreCase(ENGLISH.getLocale())) {
                return ENGLISH;
            } else if (locale.equalsIgnoreCase(CZECH.getLocale())) {
                return CZECH;
            } else if (locale.equalsIgnoreCase(SPANISH_SPAIN.getLocale())) {
                return SPANISH_SPAIN;
            } else if (locale.equalsIgnoreCase(ITALIAN.getLocale())) {
                return ITALIAN;
            } else if (locale.equalsIgnoreCase(POLISH.getLocale())) {
                return POLISH;
            } else if (locale.equalsIgnoreCase(PORTUGUESE_BRAZIL.getLocale())) {
                return PORTUGUESE_BRAZIL;
            } else if (locale.equalsIgnoreCase(RUSSIAN.getLocale())) {
                return RUSSIAN;
            } else if (locale.equalsIgnoreCase(TURKISH.getLocale())) {
                return TURKISH;
            } else if (locale.equalsIgnoreCase(CHINESE_SIMPLIFIED.getLocale())) {
                return CHINESE_SIMPLIFIED;
            }
        }

        Log.wtf(TAG, "Locale does not exist.");
        return ENGLISH;
    }

    public String getDescription(Context context) {
        switch (this) {
            case ENGLISH:
                return context.getString(R.string.language_english);
            case CZECH:
                return context.getString(R.string.language_czech);
            case SPANISH_SPAIN:
                return context.getString(R.string.language_spanish_spain);
            case ITALIAN:
                return context.getString(R.string.language_italian);
            case POLISH:
                return context.getString(R.string.language_polish);
            case PORTUGUESE_BRAZIL:
                return context.getString(R.string.language_portuguese_brazil);
            case RUSSIAN:
                return context.getString(R.string.language_russian);
            case TURKISH:
                return context.getString(R.string.language_turkish);
            case CHINESE_SIMPLIFIED:
                return context.getString(R.string.language_chinese_simplified);
            default:
                Log.wtf(TAG, "Language does not exist.");
                return context.getString(R.string.language_english);
        }
    }

    public static boolean isChineseSimplifiedCountry(String country) {
        if (country.equals("CN") || country.equals("MY") || country.equals("SG")) {
            return true;
        } else {
            return false;
        }
    }

}
