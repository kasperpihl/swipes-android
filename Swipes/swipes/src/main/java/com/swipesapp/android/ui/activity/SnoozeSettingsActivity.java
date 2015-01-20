package com.swipesapp.android.ui.activity;

import android.os.Bundle;
import android.preference.PreferenceFragment;

import com.swipesapp.android.R;
import com.swipesapp.android.util.ThemeUtils;
import com.swipesapp.android.values.Sections;

public class SnoozeSettingsActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(ThemeUtils.getThemeResource(this));
        setContentView(R.layout.activity_snooze_settings);

        getFragmentManager().beginTransaction().replace(R.id.snooze_settings_content,
                new SnoozeSettingsFragment()).commit();

        getWindow().getDecorView().setBackgroundColor(ThemeUtils.getBackgroundColor(this));

        getSupportActionBar().setDisplayShowTitleEnabled(false);

        themeStatusBar(ThemeUtils.getSectionColorDark(Sections.FOCUS, this));
    }

    public static class SnoozeSettingsFragment extends PreferenceFragment {

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            addPreferencesFromResource(R.xml.snooze_settings);
        }
    }
}
