package com.swipesapp.android.ui.activity;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;

import com.afollestad.materialdialogs.MaterialDialog;
import com.parse.ParseUser;
import com.parse.ui.ParseLoginBuilder;
import com.swipesapp.android.R;
import com.swipesapp.android.sync.gson.GsonTag;
import com.swipesapp.android.sync.gson.GsonTask;
import com.swipesapp.android.sync.service.SyncService;
import com.swipesapp.android.sync.service.TasksService;
import com.swipesapp.android.ui.view.SwipesDialog;
import com.swipesapp.android.util.Constants;
import com.swipesapp.android.util.DateUtils;
import com.swipesapp.android.util.PreferenceUtils;
import com.swipesapp.android.util.ThemeUtils;

public class SettingsActivity extends BaseActivity {

    private static boolean sHasChangedTheme;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(ThemeUtils.getThemeResource(this));
        setContentView(R.layout.activity_settings);

        getFragmentManager().beginTransaction().replace(R.id.settings_content,
                new SettingsFragment()).commit();

        getWindow().getDecorView().setBackgroundColor(ThemeUtils.getBackgroundColor(this));

        if (sHasChangedTheme) {
            // Theme has changed. Set result code.
            setResult(Constants.THEME_CHANGED_RESULT_CODE);
            sHasChangedTheme = false;
        }
    }

    public static class SettingsFragment extends PreferenceFragment implements OnSharedPreferenceChangeListener {

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            addPreferencesFromResource(R.xml.settings);

            PreferenceCategory categoryCloud = (PreferenceCategory) findPreference("group_cloud");
            PreferenceCategory categoryPreferences = (PreferenceCategory) findPreference("group_preferences");
            PreferenceCategory categoryOther = (PreferenceCategory) findPreference("group_other");

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

            final Preference preferenceSync = findPreference("sync");
            preferenceSync.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                public boolean onPreferenceClick(Preference preference) {
                    // Force sync and refresh.
                    SyncService.getInstance(getActivity()).performSync(true);
                    refreshSyncDate(preferenceSync);
                    return true;
                }
            });

            if (ParseUser.getCurrentUser() == null) {
                // Hide logout preference.
                categoryCloud.removePreference(preferenceLogout);

                // Hide manual sync button.
                categoryOther.removePreference(preferenceSync);
            } else {
                // Hide login preference.
                categoryCloud.removePreference(preferenceLogin);

                // Show last sync date.
                refreshSyncDate(preferenceSync);
            }

            // Location is not available yet, so hide the setting.
            Preference preferenceLocation = findPreference("settings_enable_location");
            categoryPreferences.removePreference(preferenceLocation);
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
                // Theme has changed. Save state.
                sHasChangedTheme = true;

                // Reload activity.
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
            new SwipesDialog.Builder(getActivity())
                    .title(R.string.logout_dialog_title)
                    .content(R.string.logout_dialog_message)
                    .positiveText(R.string.logout_dialog_yes)
                    .negativeText(R.string.logout_dialog_no)
                    .actionsColorRes(R.color.neutral_accent)
                    .callback(new MaterialDialog.ButtonCallback() {
                        @Override
                        public void onPositive(MaterialDialog dialog) {
                            // Logout Parse user.
                            ParseUser.logOut();

                            // Reset user preferences.
                            resetPreferences();

                            // Show welcome screen.
                            Intent intent = new Intent(getActivity(), WelcomeActivity.class);
                            startActivityForResult(intent, Constants.WELCOME_REQUEST_CODE);
                        }
                    })
                    .show();
        }

        private void resetPreferences() {
            // Reset user preferences.
            PreferenceManager.getDefaultSharedPreferences(getActivity()).edit().clear().commit();
            PreferenceManager.setDefaultValues(getActivity(), R.xml.settings, true);

            // Set welcome screen as not shown.
            PreferenceUtils.saveStringPreference(PreferenceUtils.WELCOME_SCREEN, "", getActivity());

            // Clear last sync date.
            PreferenceUtils.saveStringPreference(PreferenceUtils.SYNC_LAST_UPDATE, null, getActivity());
        }

        private void askToKeepData() {
            // Display confirmation dialog.
            new SwipesDialog.Builder(getActivity())
                    .title(R.string.keep_data_dialog_title)
                    .content(R.string.keep_data_dialog_message)
                    .positiveText(R.string.keep_data_dialog_yes)
                    .negativeText(R.string.keep_data_dialog_no)
                    .actionsColorRes(R.color.neutral_accent)
                    .cancelable(false)
                    .callback(new MaterialDialog.ButtonCallback() {
                        @Override
                        public void onPositive(MaterialDialog dialog) {
                            // Save data from test period for sync.
                            saveDataForSync();

                            finishLogin();
                        }

                        @Override
                        public void onNegative(MaterialDialog dialog) {
                            // Clear data from test period.
                            TasksService.getInstance(getActivity()).clearAllData();

                            finishLogin();
                        }
                    })
                    .show();
        }

        private void finishLogin() {
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

        private void refreshSyncDate(Preference preferenceSync) {
            String lastUpdate = PreferenceUtils.getSyncLastCall(getActivity());

            if (lastUpdate != null) {
                String syncSummary = DateUtils.formatToRecent(DateUtils.dateFromSync(lastUpdate), getActivity(), false);
                preferenceSync.setSummary(getResources().getString(R.string.settings_sync_summary, syncSummary));
            }
        }

    }
}
