package com.swipesapp.android.ui.activity;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;

import com.negusoft.holoaccent.activity.AccentActivity;
import com.negusoft.holoaccent.dialog.AccentAlertDialog;
import com.swipesapp.android.R;
import com.swipesapp.android.evernote.EvernoteIntegration;
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
                    // Link Evernote account.
                    EvernoteIntegration.getInstance().authenticateInContext(getActivity());
                    return true;
                }
            });

            Preference evernoteUnlink = findPreference("evernote_unlink");
            evernoteUnlink.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                public boolean onPreferenceClick(Preference preference) {
                    // Show confirmation dialog.
                    new AccentAlertDialog.Builder(getActivity())
                            .setTitle(getString(R.string.evernote_unlink_dialog_title))
                            .setMessage(R.string.evernote_unlink_dialog_message)
                            .setPositiveButton(getString(R.string.dialog_yes), new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    // Unlink Evernote account.
                                    EvernoteIntegration.getInstance().logoutInContext(getActivity());
                                }
                            })
                            .setNegativeButton(getString(R.string.dialog_no), null)
                            .create()
                            .show();

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

            if (EvernoteIntegration.getInstance().isAuthenticated()) {
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
