package com.swipesapp.android.ui.activity;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.text.Html;

import com.negusoft.holoaccent.activity.AccentActivity;
import com.swipesapp.android.R;
import com.swipesapp.android.util.PreferenceUtils;
import com.swipesapp.android.util.ThemeUtils;

public class SettingsActivity extends AccentActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        getFragmentManager().beginTransaction().replace(R.id.settings_content,
                new SettingsFragment()).commit();
    }

    public static class SettingsFragment extends PreferenceFragment implements OnSharedPreferenceChangeListener {

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            getActivity().setTheme(ThemeUtils.getCurrentThemeResource(getActivity()));

            addPreferencesFromResource(R.xml.settings);

            Preference preferenceInvite = findPreference("invite");
            preferenceInvite.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                public boolean onPreferenceClick(Preference preference) {
                    // Open invite email.
                    sendInvite();
                    return true;
                }
            });
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
            if (key.equalsIgnoreCase(PreferenceUtils.THEME_KEY)) {
                // Theme has changed. Reload activity.
                getActivity().recreate();
            }
        }

        private void sendInvite() {
            Intent inviteIntent = new Intent(Intent.ACTION_SEND);
            inviteIntent.setType("text/html");
            inviteIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, getString(R.string.invite_subject));

            // Try to open HTML invite directly in Gmail.
            try {
                inviteIntent.setClassName("com.google.android.gm", "com.google.android.gm.ComposeActivityGmail");
                inviteIntent.putExtra(android.content.Intent.EXTRA_TEXT, Html.fromHtml(getString(R.string.invite_html_body)));
                startActivity(inviteIntent);
            } catch (ActivityNotFoundException e) {
                // If Gmail is not available, fallback to non-HTML invite and app selector.
                inviteIntent.putExtra(android.content.Intent.EXTRA_TEXT, getString(R.string.invite_body));
                startActivity(Intent.createChooser(inviteIntent, getString(R.string.invite_chooser_title)));
            }
        }

    }
}
