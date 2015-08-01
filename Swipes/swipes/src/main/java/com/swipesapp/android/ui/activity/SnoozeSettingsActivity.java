package com.swipesapp.android.ui.activity;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;

import com.swipesapp.android.R;
import com.swipesapp.android.analytics.handler.Analytics;
import com.swipesapp.android.analytics.values.Screens;
import com.swipesapp.android.handler.SettingsHandler;
import com.swipesapp.android.sync.receiver.NotificationsHelper;
import com.swipesapp.android.ui.view.TimePreference;
import com.swipesapp.android.util.DateUtils;
import com.swipesapp.android.util.PreferenceUtils;
import com.swipesapp.android.util.ThemeUtils;

import java.util.Calendar;

public class SnoozeSettingsActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(ThemeUtils.getThemeResource(this));
        setContentView(R.layout.activity_snooze_settings);

        getFragmentManager().beginTransaction().replace(R.id.snooze_settings_content,
                new SnoozeSettingsFragment()).commit();

        getWindow().getDecorView().setBackgroundColor(ThemeUtils.getBackgroundColor(this));

        getSupportActionBar().setTitle(getString(R.string.title_activity_snooze_settings));
    }

    @Override
    protected void onResume() {
        // Send screen view event.
        Analytics.sendScreenView(Screens.SCREEN_SNOOZES);

        super.onResume();
    }

    public static class SnoozeSettingsFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            addPreferencesFromResource(R.xml.snooze_settings);

            // Display current values.
            displayValues();
        }

        @Override
        public void onResume() {
            super.onResume();
            getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
        }

        @Override
        public void onPause() {
            getPreferenceManager().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
            super.onPause();
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            // Update displayed values.
            displayValues();

            // Save user settings.
            SettingsHandler.saveSettingsToServer(getActivity());

            if (key.equalsIgnoreCase(PreferenceUtils.SNOOZE_WEEKEND_DAY_START)) {
                // Reschedule daily reminder.
                NotificationsHelper.createDailyReminderAlarm(getActivity());

            } else if (key.equalsIgnoreCase(PreferenceUtils.SNOOZE_EVENING_START)) {
                // Reschedule evening and weekly reminders.
                NotificationsHelper.createEveningReminderAlarm(getActivity());
                NotificationsHelper.createWeeklyReminderAlarm(getActivity());

            } else if (key.equalsIgnoreCase(PreferenceUtils.SNOOZE_WEEK_START)) {
                // Reschedule weekly reminder.
                NotificationsHelper.createWeeklyReminderAlarm(getActivity());
            }
        }

        private void displayValues() {
            // Display day start value.
            String dayStartValue = PreferenceUtils.readString(PreferenceUtils.SNOOZE_DAY_START, getActivity());
            Preference preferenceDayStart = findPreference("settings_day_start");
            preferenceDayStart.setSummary(formatTime(dayStartValue));

            // Display evening start value.
            String eveningStartValue = PreferenceUtils.readString(PreferenceUtils.SNOOZE_EVENING_START, getActivity());
            Preference preferenceEveningStart = findPreference("settings_evening_start");
            preferenceEveningStart.setSummary(formatTime(eveningStartValue));

            // Display weekend day start value.
            String weekendDayStartValue = PreferenceUtils.readString(PreferenceUtils.SNOOZE_WEEKEND_DAY_START, getActivity());
            Preference preferenceWeekendDayStart = findPreference("settings_weekend_day_start");
            preferenceWeekendDayStart.setSummary(formatTime(weekendDayStartValue));

            // Display week start value.
            String weekStartValue = PreferenceUtils.readString(PreferenceUtils.SNOOZE_WEEK_START, getActivity());
            Calendar calendarWeekStart = Calendar.getInstance();
            calendarWeekStart.set(Calendar.DAY_OF_WEEK, DateUtils.weekdayFromPrefValue(weekStartValue));
            String formattedWeekStart = DateUtils.formatDayOfWeek(getActivity(), calendarWeekStart);
            Preference preferenceWeekStart = findPreference("settings_snoozes_week_start_dow");
            preferenceWeekStart.setSummary(formattedWeekStart);

            // Display weekend start value.
            String weekendStartValue = PreferenceUtils.readString(PreferenceUtils.SNOOZE_WEEKEND_START, getActivity());
            Calendar calendarWeekendStart = Calendar.getInstance();
            calendarWeekendStart.set(Calendar.DAY_OF_WEEK, DateUtils.weekdayFromPrefValue(weekendStartValue));
            String formattedWeekendStart = DateUtils.formatDayOfWeek(getActivity(), calendarWeekendStart);
            Preference preferenceWeekendStart = findPreference("settings_snoozes_weekend_start_dow");
            preferenceWeekendStart.setSummary(formattedWeekendStart);

            // Display later today value.
            String laterTodayValue = PreferenceUtils.readString(PreferenceUtils.SNOOZE_LATER_TODAY, getActivity());
            Preference preferenceLaterToday = findPreference("settings_snoozes_later_today_value");
            preferenceLaterToday.setSummary(formatLaterToday(laterTodayValue));
        }

        private String formatTime(String time) {
            int hour = TimePreference.getHour(time);
            int minute = TimePreference.getMinute(time);

            Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.HOUR_OF_DAY, hour);
            calendar.set(Calendar.MINUTE, minute);

            return DateUtils.getTimeAsString(getActivity(), calendar.getTime());
        }

        private String formatLaterToday(String value) {
            // Load resource string for later today.
            switch (value) {
                case "1":
                    return getString(R.string.later_today_1h);
                case "2":
                    return getString(R.string.later_today_2h);
                case "3":
                    return getString(R.string.later_today_3h);
                case "4":
                    return getString(R.string.later_today_4h);
                case "5":
                    return getString(R.string.later_today_5h);
                case "6":
                    return getString(R.string.later_today_6h);
                default:
                    return "";
            }
        }

    }
}
