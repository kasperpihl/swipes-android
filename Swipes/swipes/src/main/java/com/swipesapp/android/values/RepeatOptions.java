package com.swipesapp.android.values;

/**
 * Holds the possible repeat options.
 *
 * @author Felipe Bari
 */
public enum RepeatOptions {

    NEVER("never"),
    EVERY_DAY("every day"),
    MONDAY_TO_FRIDAY("mon-fri"),
    SATURDAY_TO_SUNDAY("sat-sun"),
    EVERY_WEEK("every week"),
    EVERY_MONTH("every month"),
    EVERY_YEAR("every year");

    private String mValue;

    RepeatOptions(String value) {
        mValue = value;
    }

    public String getValue() {
        return mValue;
    }

}
