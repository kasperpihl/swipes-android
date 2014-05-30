package com.swipesapp.android.ui.activity;

import android.app.Activity;
import android.os.Bundle;
import android.preference.PreferenceFragment;

import com.swipesapp.android.R;
import com.swipesapp.android.util.ThemeUtils;

public class SnoozeSettingsActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(ThemeUtils.getCurrentThemeResource(this));

        setContentView(R.layout.activity_snooze_settings);

        getFragmentManager().beginTransaction().replace(R.id.snooze_settings_content,
                new SnoozeSettingsFragment()).commit();
    }

    public static class SnoozeSettingsFragment extends PreferenceFragment {

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
//            getActivity().setTheme(ThemeUtils.getCurrentThemeResource(getActivity()));

            addPreferencesFromResource(R.xml.snooze_settings);
        }
    }
}
