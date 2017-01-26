package com.swipesapp.android.handler;

import android.content.Context;
import android.util.Log;

import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseUser;
import com.parse.RefreshCallback;
import com.swipesapp.android.ui.view.TimePreference;
import com.swipesapp.android.util.DateUtils;
import com.swipesapp.android.util.PreferenceUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * Handler to deal with user settings.
 *
 * @author Fernanda Bari
 */
public class SettingsHandler {

    private static final String LOG_TAG = SettingsHandler.class.getSimpleName();

    private static final String SETTINGS = "settings";

    private static final String DAY_START = "SettingWeekStartTime";
    private static final String EVENING_START = "SettingEveningStartTime";
    private static final String WEEKEND_DAY_START = "SettingWeekendStartTime";
    private static final String WEEK_START = "SettingWeekStart";
    private static final String WEEKEND_START = "SettingWeekendStart";
    private static final String LATER_TODAY = "SettingLaterToday";
    private static final String SCROLL_TO_ADDED = "SettingScrollToAdded";
    private static final String ADD_TO_BOTTOM = "SettingAddToBottom";

    public static void readSettingsFromServer(final Context context) {
        final ParseUser user = ParseUser.getCurrentUser();

        if (user != null) {
            try {
                // Refresh user object.
                user.refreshInBackground(new RefreshCallback() {
                    @Override
                    public void done(ParseObject parseObject, ParseException e) {
                        Map<String, Object> settings = user.getMap(SETTINGS);

                        if (settings != null && !settings.isEmpty()) {
                            // Update day start setting.
                            Object dayStart = settings.get(DAY_START);
                            if (dayStart != null) {
                                String value = hoursAndMinutesPrefFromSeconds((int) dayStart);
                                PreferenceUtils.saveString(PreferenceUtils.SNOOZE_DAY_START, value, context);
                            }

                            // Update evening start setting.
                            Object eveningStart = settings.get(EVENING_START);
                            if (eveningStart != null) {
                                String value = hoursAndMinutesPrefFromSeconds((int) eveningStart);
                                PreferenceUtils.saveString(PreferenceUtils.SNOOZE_EVENING_START, value, context);
                            }

                            // Update weekend day start setting.
                            Object weekendDayStart = settings.get(WEEKEND_DAY_START);
                            if (weekendDayStart != null) {
                                String value = hoursAndMinutesPrefFromSeconds((int) weekendDayStart);
                                PreferenceUtils.saveString(PreferenceUtils.SNOOZE_WEEKEND_DAY_START, value, context);
                            }

                            // Update week start setting.
                            Object weekStart = settings.get(WEEK_START);
                            if (weekStart != null) {
                                String value = DateUtils.prefValueFromWeekday((int) weekStart);
                                PreferenceUtils.saveString(PreferenceUtils.SNOOZE_WEEK_START, value, context);
                            }

                            // Update weekend start setting.
                            Object weekendStart = settings.get(WEEKEND_START);
                            if (weekendStart != null) {
                                String value = DateUtils.prefValueFromWeekday((int) weekendStart);
                                PreferenceUtils.saveString(PreferenceUtils.SNOOZE_WEEKEND_START, value, context);
                            }

                            // Update later today setting.
                            Object laterToday = settings.get(LATER_TODAY);
                            if (laterToday != null) {
                                String value = hoursPrefFromSeconds((int) laterToday);
                                PreferenceUtils.saveString(PreferenceUtils.SNOOZE_LATER_TODAY, value, context);
                            }

                            // Update add to bottom setting.
                            Object addToBottom = settings.get(ADD_TO_BOTTOM);
                            if (addToBottom != null) {
                                PreferenceUtils.saveBoolean(PreferenceUtils.ADD_TO_BOTTOM_KEY, (boolean) addToBottom, context);
                            }

                            // Update scroll to added setting.
                            Object scrollToAdded = settings.get(SCROLL_TO_ADDED);
                            if (scrollToAdded != null) {
                                PreferenceUtils.saveBoolean(PreferenceUtils.SCROLL_TO_ADDED_KEY, (boolean) scrollToAdded, context);
                            }
                        } else {
                            // Settings don't exist yet for the user. Sync the local ones.
                            saveSettingsToServer(context);
                        }
                    }
                });
            } catch (Exception e) {
                Log.e(LOG_TAG, "Error parsing user settings.", e);
            }
        }
    }

    public static void saveSettingsToServer(Context context) {
        final ParseUser user = ParseUser.getCurrentUser();

        if (user != null) {
            Map<String, Object> settings = user.getMap(SETTINGS);
            if (settings == null) settings = new HashMap<>();

            // Save day start setting.
            String dayStart = PreferenceUtils.readString(PreferenceUtils.SNOOZE_DAY_START, context);
            if (dayStart != null) {
                int value = secondsFromHoursAndMinutesPref(dayStart);
                settings.put(DAY_START, value);
            }

            // Save evening start setting.
            String eveningStart = PreferenceUtils.readString(PreferenceUtils.SNOOZE_EVENING_START, context);
            if (eveningStart != null) {
                int value = secondsFromHoursAndMinutesPref(eveningStart);
                settings.put(EVENING_START, value);
            }

            // Save weekend day start setting.
            String weekendDayStart = PreferenceUtils.readString(PreferenceUtils.SNOOZE_WEEKEND_DAY_START, context);
            if (weekendDayStart != null) {
                int value = secondsFromHoursAndMinutesPref(weekendDayStart);
                settings.put(WEEKEND_DAY_START, value);
            }

            // Save week start setting.
            String weekStart = PreferenceUtils.readString(PreferenceUtils.SNOOZE_WEEK_START, context);
            if (weekStart != null) {
                int value = DateUtils.weekdayFromPrefValue(weekStart);
                settings.put(WEEK_START, value);
            }

            // Save weekend start setting.
            String weekendStart = PreferenceUtils.readString(PreferenceUtils.SNOOZE_WEEKEND_START, context);
            if (weekendStart != null) {
                int value = DateUtils.weekdayFromPrefValue(weekendStart);
                settings.put(WEEKEND_START, value);
            }

            // Save later today setting.
            String laterToday = PreferenceUtils.readString(PreferenceUtils.SNOOZE_LATER_TODAY, context);
            if (laterToday != null) {
                int value = secondsFromHoursPref(laterToday);
                settings.put(LATER_TODAY, value);
            }

            // Save add to bottom setting.
            settings.put(ADD_TO_BOTTOM, PreferenceUtils.readBoolean(PreferenceUtils.ADD_TO_BOTTOM_KEY, context));

            // Save scroll to added setting.
            settings.put(SCROLL_TO_ADDED, PreferenceUtils.isAutoScrollEnabled(context));

            // Save updated user object.
            user.put(SETTINGS, settings);
            user.saveInBackground();
        }
    }

    private static String hoursPrefFromSeconds(int seconds) {
        // Get integer portion of conversion from seconds.
        return String.valueOf(seconds / 3600);
    }

    private static String hoursAndMinutesPrefFromSeconds(int seconds) {
        int hours = seconds / 3600;
        int minutes = (seconds % 3600) / 60;

        // Format converted seconds to local preference.
        return hours + ":" + minutes;
    }

    private static int secondsFromHoursPref(String hours) {
        // Convert hours to seconds.
        return Integer.valueOf(hours) * 3600;
    }

    private static int secondsFromHoursAndMinutesPref(String hoursAndMinutes) {
        int hours = TimePreference.getHour(hoursAndMinutes);
        int minutes = TimePreference.getMinute(hoursAndMinutes);

        // Convert local preference to seconds.
        return hours * 3600 + minutes * 60;
    }

}
