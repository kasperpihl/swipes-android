package com.swipesapp.android.ui.activity;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;

import com.swipesapp.android.R;
import com.swipesapp.android.analytics.handler.Analytics;
import com.swipesapp.android.analytics.values.Screens;
import com.swipesapp.android.util.PreferenceUtils;
import com.swipesapp.android.util.ThemeUtils;

import java.lang.ref.WeakReference;

public class OptionsActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(ThemeUtils.getThemeResource(this));
        setContentView(R.layout.activity_options);

        getFragmentManager().beginTransaction().replace(R.id.options_content,
                new SettingsFragment()).commit();

        getWindow().getDecorView().setBackgroundColor(ThemeUtils.getBackgroundColor(this));
    }

    @Override
    protected void onResume() {
        // Send screen view event.
        Analytics.sendScreenView(Screens.SCREEN_OPTIONS);

        super.onResume();
    }

    public static class SettingsFragment extends PreferenceFragment implements OnSharedPreferenceChangeListener {

        private WeakReference<Context> mContext;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            mContext = new WeakReference<Context>(getActivity());

            addPreferencesFromResource(R.xml.options);

            // Enable or disable vibration preference.
            handleVibrationPreference();
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
            if (key.equalsIgnoreCase(PreferenceUtils.NOTIFICATIONS_KEY) ||
                    key.equalsIgnoreCase(PreferenceUtils.DAILY_REMINDER_KEY) ||
                    key.equalsIgnoreCase(PreferenceUtils.WEEKLY_REMINDER_KEY)) {

                // Enable or disable vibration preference.
                handleVibrationPreference();
            }
        }

        private void handleVibrationPreference() {
            Preference preferenceVibration = findPreference("settings_enable_vibration");

            // Check if notifications are disabled.
            if (!PreferenceUtils.areNotificationsEnabled(mContext.get()) &&
                    !PreferenceUtils.isDailyReminderEnabled(mContext.get()) &&
                    !PreferenceUtils.isWeeklyReminderEnabled(mContext.get())) {

                // Disable vibration preference.
                preferenceVibration.setEnabled(false);
            } else {
                // Enable vibration preference.
                preferenceVibration.setEnabled(true);
            }
        }

    }
}
