package com.swipesapp.android.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;

import com.negusoft.holoaccent.activity.AccentActivity;
import com.swipesapp.android.R;
import com.swipesapp.android.util.PreferenceUtils;
import com.swipesapp.android.util.ThemeUtils;

public class IntegrationsActivity extends AccentActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(ThemeUtils.getThemeResource(this));
        setContentView(R.layout.activity_integrations);

        getFragmentManager().beginTransaction().replace(R.id.integrations_content,
                new IntegrationsFragment()).commit();

        getActionBar().setDisplayShowTitleEnabled(false);
    }

    public static class IntegrationsFragment extends PreferenceFragment {

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            addPreferencesFromResource(R.xml.integrations);

            Preference evernoteLink = findPreference("evernote_link");
            evernoteLink.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                public boolean onPreferenceClick(Preference preference) {
                    // TODO: Link Evernote account.
                    return true;
                }
            });

            Preference evernoteUnlink = findPreference("evernote_unlink");
            evernoteUnlink.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                public boolean onPreferenceClick(Preference preference) {
                    // TODO: Unlink Evernote account.
                    return true;
                }
            });

            Preference evernoteOpenImporter = findPreference("evernote_open_importer");
            evernoteOpenImporter.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    // TODO: Open importer.
                    return true;
                }
            });

            Preference evernoteLearnMore = findPreference("evernote_learn_more");
            evernoteLearnMore.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    // Show guide.
                    Intent intent = new Intent(getActivity(), EvernoteLearnActivity.class);
                    startActivity(intent);
                    return true;
                }
            });

            Preference evernoteSyncDevice = findPreference("evernote_sync_device");
            Preference evernoteAutoImport = findPreference("evernote_auto_import");
            Preference evernoteSyncPersonal = findPreference("evernote_sync_personal");
            Preference evernoteSyncBusiness = findPreference("evernote_sync_business");

            if (PreferenceUtils.isEvernoteLinked(getActivity())) {
                // Hide Evernote link button.
                getPreferenceScreen().removePreference(evernoteLink);
            } else {
                // Hide all other Evernote preferences.
                getPreferenceScreen().removePreference(evernoteUnlink);
                getPreferenceScreen().removePreference(evernoteSyncDevice);
                getPreferenceScreen().removePreference(evernoteAutoImport);
                getPreferenceScreen().removePreference(evernoteSyncPersonal);
                getPreferenceScreen().removePreference(evernoteSyncBusiness);
                getPreferenceScreen().removePreference(evernoteOpenImporter);
            }
        }
    }
}
