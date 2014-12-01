package com.swipesapp.android.ui.activity;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;

import com.negusoft.holoaccent.activity.AccentActivity;
import com.negusoft.holoaccent.dialog.AccentAlertDialog;
import com.parse.ParseUser;
import com.parse.ui.ParseLoginBuilder;
import com.swipesapp.android.R;
import com.swipesapp.android.evernote.EvernoteIntegration;
import com.swipesapp.android.sync.gson.GsonTag;
import com.swipesapp.android.sync.gson.GsonTask;
import com.swipesapp.android.sync.service.SyncService;
import com.swipesapp.android.sync.service.TasksService;
import com.swipesapp.android.util.Constants;
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
            getActivity().setTheme(ThemeUtils.getThemeResource(getActivity()));

            addPreferencesFromResource(R.xml.settings);

            Preference preferenceInvite = findPreference("invite");
            preferenceInvite.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                public boolean onPreferenceClick(Preference preference) {
                    // Open invite email.
                    sendInvite();
                    return true;
                }
            });

            Preference preferenceLogin = findPreference("login");
            preferenceLogin.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                public boolean onPreferenceClick(Preference preference) {
                    // Start login activity.
                    startLogin();
                    return true;
                }
            });

            Preference preferenceLogout = findPreference("logout");
            preferenceLogout.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                public boolean onPreferenceClick(Preference preference) {
                    // Logout current user.
                    performLogout();
                    return true;
                }
            });

            if (ParseUser.getCurrentUser() == null) {
                // Hide logout preference.
                getPreferenceScreen().removePreference(preferenceLogout);
            } else {
                // Hide login preference.
                getPreferenceScreen().removePreference(preferenceLogin);
            }

            final Preference preferenceEvernote = findPreference("evernote");
            final EvernoteIntegration evernoteIntegration = EvernoteIntegration.getInstance();
            evernoteIntegration.setContext(getActivity().getApplicationContext());
            preferenceEvernote.setSummary(evernoteIntegration.isAuthenticated() ? R.string.evernote_integrate_yes : R.string.evernote_integrate_no);
            preferenceEvernote.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                public boolean onPreferenceClick(Preference preference) {
                    // Start login activity.
                    handleEvernote();
                    return true;
                }
            });

            syncDebug();
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

        @Override
        public void onActivityResult(int requestCode, int resultCode, Intent data) {
            if (requestCode == Constants.LOGIN_REQUEST_CODE) {
                switch (resultCode) {
                    case Activity.RESULT_OK:
                        // Login successful. Ask to keep user data.
                        askToKeepData();
                        break;
                }
            } else if (requestCode == Constants.WELCOME_REQUEST_CODE) {
                switch (resultCode) {
                    case RESULT_OK:
                        // Set welcome screen as shown.
                        PreferenceUtils.saveStringPreference(PreferenceUtils.WELCOME_SCREEN, "YES", getActivity());

                        // Show tasks activity.
                        Intent intent = new Intent(getActivity(), TasksActivity.class);
                        startActivity(intent);
                        break;
                }
                getActivity().finish();
            }
        }

        private void sendInvite() {
            Intent inviteIntent = new Intent(Intent.ACTION_SEND);
            inviteIntent.setType("text/html");
            inviteIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, getString(R.string.invite_subject));
            inviteIntent.putExtra(android.content.Intent.EXTRA_TEXT, getString(R.string.invite_body));

            // Try to open invite directly in Gmail.
            try {
                inviteIntent.setClassName("com.google.android.gm", "com.google.android.gm.ComposeActivityGmail");
                startActivity(inviteIntent);
            } catch (ActivityNotFoundException e) {
                // If Gmail is not available, fallback to app selector.
                startActivity(Intent.createChooser(inviteIntent, getString(R.string.invite_chooser_title)));
            }
        }

        private void startLogin() {
            // Call Parse login activity.
            ParseLoginBuilder builder = new ParseLoginBuilder(getActivity());
            startActivityForResult(builder.build(), Constants.LOGIN_REQUEST_CODE);
        }

        private void performLogout() {
            // Display confirmation dialog.
            new AccentAlertDialog.Builder(getActivity())
                    .setTitle(getString(R.string.logout_dialog_title))
                    .setMessage(getString(R.string.logout_dialog_message))
                    .setPositiveButton(getString(R.string.dialog_yes), new DialogInterface.OnClickListener() {

                        public void onClick(DialogInterface dialog, int which) {
                            // Logout Parse user.
                            ParseUser.logOut();

                            // Reset user preferences.
                            resetPreferences();

                            // Show welcome screen.
                            Intent intent = new Intent(getActivity(), WelcomeActivity.class);
                            startActivityForResult(intent, Constants.WELCOME_REQUEST_CODE);
                        }
                    })
                    .setNegativeButton(getString(R.string.dialog_no), null)
                    .create()
                    .show();
        }

        private void resetPreferences() {
            // Reset user preferences.
            PreferenceManager.getDefaultSharedPreferences(getActivity()).edit().clear().commit();
            PreferenceManager.setDefaultValues(getActivity(), R.xml.settings, true);

            // Set welcome screen as not shown.
            PreferenceUtils.saveStringPreference(PreferenceUtils.WELCOME_SCREEN, "", getActivity());
        }

        private void askToKeepData() {
            // Display confirmation dialog.
            new AccentAlertDialog.Builder(getActivity())
                    .setTitle(getString(R.string.keep_data_dialog_title))
                    .setMessage(getString(R.string.keep_data_dialog_message))
                    .setPositiveButton(getString(R.string.dialog_yes), new DialogInterface.OnClickListener() {

                        public void onClick(DialogInterface dialog, int which) {
                            // Save data from test period for sync.
                            saveDataForSync();

                            finishLogin();
                        }
                    })
                    .setNegativeButton(getString(R.string.dialog_no), new DialogInterface.OnClickListener() {

                        public void onClick(DialogInterface dialogInterface, int i) {
                            // Clear data from test period.
                            TasksService.getInstance(getActivity()).clearAllData();

                            finishLogin();
                        }
                    })
                    .create()
                    .show();
        }

        private void finishLogin() {
            // Perform sync with changesOnly = false.
            SyncService.getInstance(getActivity()).performSync(false);

            getActivity().recreate();
        }

        private void saveDataForSync() {
            // Save all tags for syncing.
            for (GsonTag tag : TasksService.getInstance(getActivity()).loadAllTags()) {
                SyncService.getInstance(getActivity()).saveTagForSync(tag);
            }

            // Save all tasks for syncing.
            for (GsonTask task : TasksService.getInstance(getActivity()).loadAllTasks()) {
                if (!task.getDeleted()) {
                    task.setId(null);
                    SyncService.getInstance(getActivity()).saveTaskChangesForSync(task);
                }
            }
        }

        private void syncDebug() {
            Preference preferenceLogout = findPreference("sync");
            preferenceLogout.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                public boolean onPreferenceClick(Preference preference) {
                    // Force sync for debug.
                    SyncService.getInstance(getActivity()).performSync(false);
                    return true;
                }
            });

            // For debugging sync, comment the line below.
            getPreferenceScreen().removePreference(preferenceLogout);
        }

        private void handleEvernote() {
            final EvernoteIntegration evernoteIntegration = EvernoteIntegration.getInstance();
            if (!evernoteIntegration.isAuthenticated()) {
                evernoteIntegration.authenticateInContext(getActivity());
            }
            else {
                // Display dialog to save new tag.
                new AccentAlertDialog.Builder(getActivity())
                        .setTitle(getString(R.string.evernote))
                        .setMessage(R.string.evernote_dialog_msg)
                        .setPositiveButton(getString(R.string.evernote_dialog_dialog_yes), new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                evernoteIntegration.logoutInContext(SettingsFragment.this.getActivity());
                                SettingsFragment.this.findPreference("evernote").setSummary(R.string.evernote_integrate_no);
                            }
                        })
                        .setNegativeButton(getString(R.string.evernote_dialog_cancel), null)
                        .create()
                        .show();
            }
        }

    }
}
