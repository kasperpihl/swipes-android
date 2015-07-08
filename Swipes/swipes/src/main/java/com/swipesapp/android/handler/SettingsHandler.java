package com.swipesapp.android.handler;

import android.content.Context;
import android.util.Log;

import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseUser;
import com.parse.RefreshCallback;
import com.swipesapp.android.util.DateUtils;
import com.swipesapp.android.util.PreferenceUtils;

import java.util.Map;

/**
 * Handler to deal with user settings.
 *
 * @author Felipe Bari
 */
public class SettingsHandler {

    private static final String LOG_TAG = SettingsHandler.class.getSimpleName();

    public static void readSettingsFromServer(final Context context) {
        final ParseUser user = ParseUser.getCurrentUser();

        if (user != null) {
            try {
                // Refresh user object.
                user.refreshInBackground(new RefreshCallback() {
                    @Override
                    public void done(ParseObject parseObject, ParseException e) {
                        Map<String, Object> settings = user.getMap("settings");

                        // Save day start setting.
                        Object dayStart = settings.get("SettingWeekStartTime");
                        if (dayStart != null) {
                            String value = hoursAndMinutesFromSeconds((int) dayStart);
                            PreferenceUtils.saveString(PreferenceUtils.SNOOZE_DAY_START, value, context);
                        }

                        // Save evening start setting.
                        Object eveningStart = settings.get("SettingEveningStartTime");
                        if (eveningStart != null) {
                            String value = hoursAndMinutesFromSeconds((int) eveningStart);
                            PreferenceUtils.saveString(PreferenceUtils.SNOOZE_EVENING_START, value, context);
                        }

                        // Save weekend day start setting.
                        Object weekendDayStart = settings.get("SettingWeekendStartTime");
                        if (weekendDayStart != null) {
                            String value = hoursAndMinutesFromSeconds((int) weekendDayStart);
                            PreferenceUtils.saveString(PreferenceUtils.SNOOZE_WEEKEND_DAY_START, value, context);
                        }

                        // Save week start setting.
                        Object weekStart = settings.get("SettingWeekStart");
                        if (weekStart != null) {
                            String value = DateUtils.prefValueFromWeekday((int) weekStart);
                            PreferenceUtils.saveString(PreferenceUtils.SNOOZE_WEEK_START, value, context);
                        }

                        // Save weekend start setting.
                        Object weekendStart = settings.get("SettingWeekendStart");
                        if (weekendStart != null) {
                            String value = DateUtils.prefValueFromWeekday((int) weekendStart);
                            PreferenceUtils.saveString(PreferenceUtils.SNOOZE_WEEKEND_START, value, context);
                        }

                        // Save scroll to added setting.
                        Object laterToday = settings.get("SettingLaterToday");
                        if (laterToday != null) {
                            String value = hoursFromSeconds((int) laterToday);
                            PreferenceUtils.saveString(PreferenceUtils.SNOOZE_LATER_TODAY, value, context);
                        }

                        // Save scroll to added setting.
                        Object scrollToAdded = settings.get("SettingScrollToAdded");
                        if (scrollToAdded != null) {
                            PreferenceUtils.saveBoolean(PreferenceUtils.SCROLL_TO_ADDED_KEY, (boolean) scrollToAdded, context);
                        }
                    }
                });
            } catch (Exception e) {
                Log.e(LOG_TAG, "Error parsing user settings.", e);
            }
        }
    }

    public static void saveSettingsToServer(Context context) {

    }

    private static String hoursFromSeconds(int seconds) {
        // Get integer portion of conversion from seconds.
        return String.valueOf(seconds / 3600);
    }

    private static String hoursAndMinutesFromSeconds(int seconds) {
        int hours = seconds / 3600;
        int minutes = (seconds % 3600) / 60;

        // Format converted seconds to local preference.
        return hours + ":" + minutes;
    }

}
