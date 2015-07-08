package com.swipesapp.android.ui.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.parse.ParseUser;
import com.parse.ui.ParseExtras;
import com.parse.ui.ParseLoginBuilder;
import com.swipesapp.android.R;
import com.swipesapp.android.analytics.handler.Analytics;
import com.swipesapp.android.analytics.handler.IntercomHandler;
import com.swipesapp.android.analytics.values.Actions;
import com.swipesapp.android.analytics.values.Categories;
import com.swipesapp.android.analytics.values.IntercomEvents;
import com.swipesapp.android.analytics.values.IntercomFields;
import com.swipesapp.android.analytics.values.Labels;
import com.swipesapp.android.analytics.values.Screens;
import com.swipesapp.android.app.SwipesApplication;
import com.swipesapp.android.handler.SettingsHandler;
import com.swipesapp.android.sync.gson.GsonTag;
import com.swipesapp.android.sync.gson.GsonTask;
import com.swipesapp.android.sync.listener.SyncListener;
import com.swipesapp.android.sync.service.SyncService;
import com.swipesapp.android.sync.service.TasksService;
import com.swipesapp.android.ui.view.SwipesDialog;
import com.swipesapp.android.util.DateUtils;
import com.swipesapp.android.util.PreferenceUtils;
import com.swipesapp.android.util.ThemeUtils;
import com.swipesapp.android.values.Constants;

import java.lang.ref.WeakReference;
import java.util.HashMap;

import io.intercom.android.sdk.Intercom;

public class SettingsActivity extends BaseActivity {

    private static boolean sHasChangedTheme;
    private static boolean sHasChangedAccount;
    private static boolean sHasLoggedIn;
    private static boolean sHasSignedUp;

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

            // Send theme change event.
            sendThemeChangedEvent();
        } else if (sHasChangedAccount) {
            // User has logged in or out. Set result code.
            setResult(Constants.ACCOUNT_CHANGED_RESULT_CODE);
            sHasChangedAccount = false;

            // Send login event if needed.
            if (sHasLoggedIn) {
                sendLoginEvent();
                sHasLoggedIn = false;
            }

            // Send signup event if needed.
            if (sHasSignedUp) {
                sendSignupEvent();
                sHasSignedUp = false;
            }

            // Update user level dimension.
            Analytics.sendUserLevel(this);

            // Update recurring tasks and tags dimensions.
            Analytics.sendRecurringTasks(this);
            Analytics.sendNumberOfTags(this);
        }

        // Read user settings.
        SettingsHandler.readSettingsFromServer(this);
    }

    @Override
    protected void onResume() {
        // Send screen view event.
        Analytics.sendScreenView(Screens.SCREEN_SETTINGS);

        super.onResume();
    }

    private void sendLoginEvent() {
        // Check if user tried out the app.
        boolean didTryOut = PreferenceUtils.hasTriedOut(this);
        String label = didTryOut ? Labels.TRY_OUT_YES : Labels.TRY_OUT_NO;

        // Send login event.
        Analytics.sendEvent(Categories.ONBOARDING, Actions.LOGGED_IN, label, null);
    }

    private void sendSignupEvent() {
        // Check if user tried out the app.
        boolean didTryOut = PreferenceUtils.hasTriedOut(this);
        String label = didTryOut ? Labels.TRY_OUT_YES : Labels.TRY_OUT_NO;

        // Send login event.
        Analytics.sendEvent(Categories.ONBOARDING, Actions.SIGNED_UP, label, null);
    }

    private void sendThemeChangedEvent() {
        String label = ThemeUtils.isLightTheme(this) ? Labels.THEME_LIGHT : Labels.THEME_DARK;

        // Send analytics event.
        Analytics.sendEvent(Categories.SETTINGS, Actions.CHANGED_THEME, label, null);

        // Update theme dimension.
        Analytics.sendActiveTheme(this);

        // Prepare Intercom fields.
        HashMap<String, Object> fields = new HashMap<>();
        fields.put(IntercomFields.THEME, label);

        // Send Intercom events.
        IntercomHandler.sendEvent(IntercomEvents.CHANGED_THEME, fields);
    }

    public static class SettingsFragment extends PreferenceFragment implements OnSharedPreferenceChangeListener {

        private WeakReference<Context> mContext;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            mContext = new WeakReference<Context>(getActivity());

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

            Preference preferenceContact = findPreference("contact_us");
            preferenceContact.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                public boolean onPreferenceClick(Preference preference) {
                    // Open Intercom chat.
                    Intercom.client().displayMessageComposer();
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

            Preference preferenceSync = findPreference("sync");
            preferenceSync.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                public boolean onPreferenceClick(Preference preference) {
                    // Start sync immediately.
                    SyncService.getInstance().setListener(mSyncListener);
                    SyncService.getInstance().performSync(true, 0);

                    // Show start message.
                    Toast.makeText(mContext.get(), getString(R.string.sync_started_message), Toast.LENGTH_SHORT).show();
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

                // Refresh widget.
                TasksActivity.refreshWidgets(getActivity());

                // Reload activity.
                getActivity().recreate();
            }
        }

        @Override
        public void onActivityResult(int requestCode, int resultCode, Intent data) {
            if (requestCode == Constants.LOGIN_REQUEST_CODE) {
                switch (resultCode) {
                    case Activity.RESULT_OK:
                        // Login successful. Mark initial setup as performed.
                        PreferenceUtils.saveString(PreferenceUtils.WELCOME_DIALOG, "YES", getActivity());
                        PreferenceUtils.saveString(PreferenceUtils.FIRST_RUN, "NO", getActivity());

                        if (PreferenceUtils.hasTriedOut(getActivity())) {
                            // End anonymous Intercom session.
                            Intercom.client().reset();
                        }

                        if (data != null) {
                            // Mark signup or login as performed.
                            sHasSignedUp = data.getBooleanExtra(ParseExtras.EXTRA_SIGNED_UP, false);
                            sHasLoggedIn = !sHasSignedUp;

                            // Start Intercom session with email.
                            String email = data.getStringExtra(ParseExtras.EXTRA_USER_EMAIL);
                            IntercomHandler.beginIntercomSession(email);
                        }

                        // Subscribe to push channels.
                        SwipesApplication.subscribePush();

                        // Read user settings.
                        SettingsHandler.readSettingsFromServer(getActivity());

                        if (TasksService.getInstance().countAllTasks() > 0) {
                            // Ask to keep user data.
                            askToKeepData();
                        } else {
                            finishAccountChange();
                        }
                        break;
                }
            }
        }

        private void sendInvite() {
            // Create share intent.
            Intent inviteIntent = new Intent(Intent.ACTION_SEND);
            inviteIntent.setType("text/plain");
            inviteIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, getString(R.string.invite_subject));
            inviteIntent.putExtra(android.content.Intent.EXTRA_TEXT, getString(R.string.invite_body));

            // Open app selector.
            startActivity(Intent.createChooser(inviteIntent, getString(R.string.invite_chooser_title)));

            // Send analytics event.
            Analytics.sendEvent(Categories.SHARING, Actions.INVITE_OPEN, null, null);
        }

        private void startLogin() {
            // Call Parse login activity.
            ParseLoginBuilder builder = new ParseLoginBuilder(getActivity());
            startActivityForResult(builder.build(), Constants.LOGIN_REQUEST_CODE);

            // Send screen view event.
            Analytics.sendScreenView(Screens.SCREEN_LOGIN);
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

                            // Unsubscribe from push channels.
                            SwipesApplication.unsubscribePush();

                            // End Intercom session.
                            Intercom.client().reset();

                            // Clear user data.
                            TasksService.getInstance().clearAllData();

                            // Reset user preferences.
                            resetPreferences();

                            finishAccountChange();
                        }
                    })
                    .show();
        }

        private void resetPreferences() {
            // Reset user preferences.
            PreferenceManager.setDefaultValues(getActivity(), R.xml.settings, true);
            PreferenceManager.setDefaultValues(getActivity(), R.xml.options, true);
            PreferenceManager.setDefaultValues(getActivity(), R.xml.snooze_settings, true);
            PreferenceManager.setDefaultValues(getActivity(), R.xml.integrations, true);

            // Clear last sync date.
            PreferenceUtils.remove(PreferenceUtils.SYNC_LAST_UPDATE, getActivity());

            // Clear state of initial setup.
            PreferenceUtils.remove(PreferenceUtils.FIRST_RUN, getActivity());
            PreferenceUtils.remove(PreferenceUtils.WELCOME_DIALOG, getActivity());
            PreferenceUtils.remove(PreferenceUtils.DID_TRY_OUT, getActivity());
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

                            finishAccountChange();
                        }

                        @Override
                        public void onNegative(MaterialDialog dialog) {
                            // Clear data from test period.
                            TasksService.getInstance().clearAllData();

                            finishAccountChange();
                        }
                    })
                    .show();
        }

        private void finishAccountChange() {
            // Account has changed. Save state.
            sHasChangedAccount = true;

            // Reload activity.
            getActivity().recreate();
        }

        private void saveDataForSync() {
            // Save all tags for syncing.
            for (GsonTag tag : TasksService.getInstance().loadAllTags()) {
                SyncService.getInstance().saveTagForSync(tag);
            }

            // Save all tasks for syncing.
            for (GsonTask task : TasksService.getInstance().loadAllTasks()) {
                if (!task.getDeleted()) {
                    task.setId(null);
                    SyncService.getInstance().saveTaskChangesForSync(task, null);
                }
            }
        }

        private void refreshSyncDate(Preference preferenceSync) {
            String lastUpdate = PreferenceUtils.getSyncLastUpdate(getActivity());

            if (lastUpdate != null) {
                String syncSummary = DateUtils.formatToRecent(DateUtils.dateFromSync(lastUpdate), getActivity(), false);
                preferenceSync.setSummary(getResources().getString(R.string.settings_sync_summary, syncSummary));
            }
        }

        private SyncListener mSyncListener = new SyncListener() {
            @Override
            public void onSyncDone() {
                // Refresh sync date.
                Preference preferenceSync = findPreference("sync");
                refreshSyncDate(preferenceSync);

                // Show success message.
                Toast.makeText(mContext.get(), getString(R.string.sync_completed_message), Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onSyncFailed() {
                // Show failure message.
                Toast.makeText(mContext.get(), getString(R.string.sync_failed_message), Toast.LENGTH_SHORT).show();
            }
        };

    }
}
