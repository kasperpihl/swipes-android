package com.swipesapp.android.ui.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.net.Uri;
import android.os.Build;
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
import com.swipesapp.android.BuildConfig;
import com.swipesapp.android.R;
import com.swipesapp.android.analytics.handler.Analytics;
import com.swipesapp.android.analytics.values.Actions;
import com.swipesapp.android.analytics.values.Categories;
import com.swipesapp.android.analytics.values.Labels;
import com.swipesapp.android.analytics.values.Screens;
import com.swipesapp.android.app.SwipesApplication;
import com.swipesapp.android.evernote.EvernoteService;
import com.swipesapp.android.handler.LanguageHandler;
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
import com.swipesapp.android.values.Languages;
import com.swipesapp.android.values.Themes;
import com.swipesapp.android.widget.AddWidgetProvider;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class SettingsActivity extends BaseActivity {

    private static boolean sHasChangedTheme;
    private static boolean sHasChangedAccount;
    private static boolean sHasLoggedIn;
    private static boolean sHasSignedUp;
    private static boolean sHasChangedLocale;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(ThemeUtils.getThemeResource(this));
        setContentView(R.layout.activity_settings);

        getFragmentManager().beginTransaction().replace(R.id.settings_content,
                new SettingsFragment()).commit();

        getWindow().getDecorView().setBackgroundColor(ThemeUtils.getBackgroundColor(this));

        getSupportActionBar().setTitle(getString(R.string.title_activity_settings));

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
        } else if (sHasChangedLocale) {
            // Locale has changed. Set result code.
            setResult(Constants.LOCALE_CHANGED_RESULT_CODE);
            sHasChangedLocale = false;
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
    }

    public static class SettingsFragment extends PreferenceFragment implements OnSharedPreferenceChangeListener {

        private WeakReference<Context> mContext;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            mContext = new WeakReference<Context>(getActivity());

            addPreferencesFromResource(R.xml.settings);

            PreferenceCategory categoryCloud = (PreferenceCategory) findPreference("group_cloud");
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
                    // Open support email.
                    contactUs();
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
            if (key.equalsIgnoreCase(PreferenceUtils.THEME_KEY)) {
                // Theme has changed. Save state.
                sHasChangedTheme = true;

                // Refresh widget.
                TasksActivity.refreshWidgets(getActivity());

                // Reload activity.
                getActivity().recreate();
            } else if (key.equalsIgnoreCase(PreferenceUtils.LOCALE_KEY)) {
                // Locale has changed. Save state.
                sHasChangedLocale = true;

                // Apply selected language.
                LanguageHandler.applyLanguage(getActivity().getApplicationContext());

                // Refresh widgets.
                TasksActivity.refreshWidgets(getActivity().getApplicationContext());
                AddWidgetProvider.refreshWidget(getActivity().getApplicationContext());

                // Reload activity.
                getActivity().recreate();
            }

            // Update displayed values.
            displayValues();
        }

        @Override
        public void onActivityResult(int requestCode, int resultCode, Intent data) {
            if (requestCode == Constants.LOGIN_REQUEST_CODE) {
                switch (resultCode) {
                    case Activity.RESULT_OK:
                        // Login successful. Mark initial setup as performed.
                        PreferenceUtils.saveString(PreferenceUtils.WELCOME_DIALOG, "YES", getActivity());
                        PreferenceUtils.saveString(PreferenceUtils.FIRST_RUN, "NO", getActivity());

                        if (data != null) {
                            // Mark signup or login as performed.
                            sHasSignedUp = data.getBooleanExtra(ParseExtras.EXTRA_SIGNED_UP, false);
                            sHasLoggedIn = !sHasSignedUp;
                        }

                        // Subscribe to push channels.
                        SwipesApplication.subscribePush(getActivity());

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

        private void contactUs() {
            // Load device info.
            String hint = getString(R.string.help_email_text_hint);
            String appVersion = "\n\n\n" + "App version: " + getString(R.string.app_version,
                    BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE);
            String deviceData = "\n" + "Device model: " + Build.MANUFACTURER + " " + Build.MODEL;
            deviceData += "\n" + "Android version: " + Build.VERSION.RELEASE;

            // Create email intent.
            Intent contactIntent = new Intent(Intent.ACTION_SENDTO, Uri.fromParts(
                    "mailto", getString(R.string.help_email_receiver), null));
            contactIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.help_email_subject));
            contactIntent.putExtra(Intent.EXTRA_TEXT, hint + appVersion + deviceData);

            // Open app selector.
            startActivity(Intent.createChooser(contactIntent, getString(R.string.help_email_chooser_title)));
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
            SwipesDialog.show(new SwipesDialog.Builder(getActivity())
                    .actionsColorRes(R.color.neutral_accent)
                    .title(R.string.logout_dialog_title)
                    .content(R.string.logout_dialog_message)
                    .positiveText(R.string.logout_dialog_yes)
                    .negativeText(R.string.logout_dialog_no)
                    .callback(new MaterialDialog.ButtonCallback() {
                        @Override
                        public void onPositive(MaterialDialog dialog) {
                            // Logout Parse user.
                            ParseUser.logOut();

                            // Unlink Evernote account.
                            EvernoteService.getInstance().logout();

                            // Unsubscribe from push channels.
                            SwipesApplication.unsubscribePush();

                            // Clear user data.
                            TasksService.getInstance().clearAllData();

                            // Reset user preferences.
                            resetPreferences();

                            finishAccountChange();
                        }
                    }));
        }

        private void resetPreferences() {
            // Preserve current language and theme.
            String theme = PreferenceUtils.readString(PreferenceUtils.THEME_KEY, getActivity());
            String locale = PreferenceUtils.readString(PreferenceUtils.LOCALE_KEY, getActivity());

            // Reset user preferences.
            PreferenceManager.getDefaultSharedPreferences(getActivity()).edit().clear().commit();
            SwipesApplication.loadDefaultPreferences(getActivity());

            // Reapply language and theme.
            PreferenceUtils.saveString(PreferenceUtils.THEME_KEY, theme, getActivity());
            PreferenceUtils.saveString(PreferenceUtils.LOCALE_KEY, locale, getActivity());
        }

        private void askToKeepData() {
            // Display confirmation dialog.
            SwipesDialog.show(new SwipesDialog.Builder(getActivity())
                    .actionsColorRes(R.color.neutral_accent)
                    .title(R.string.keep_data_dialog_title)
                    .content(R.string.keep_data_dialog_message)
                    .positiveText(R.string.keep_data_dialog_yes)
                    .negativeText(R.string.keep_data_dialog_no)
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
                    }));
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

            // Load all non-deleted tasks.
            List<GsonTask> tasksToSave = new ArrayList<>();
            for (GsonTask task : TasksService.getInstance().loadAllTasks()) {
                if (!task.getDeleted()) {
                    task.setId(null);
                    tasksToSave.add(task);
                }
            }

            // Save tasks for syncing.
            SyncService.getInstance().saveTasksForSync(tasksToSave);
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

        private void displayValues() {
            // Display selected theme.
            Themes theme = ThemeUtils.getCurrentTheme(getActivity());
            Preference preferenceTheme = findPreference("settings_theme");
            preferenceTheme.setSummary(theme.getDescription(getActivity()));

            // Display selected language.
            String userLocale = PreferenceUtils.readString(PreferenceUtils.LOCALE_KEY, getActivity());
            Languages language = Languages.getLanguageByLocale(userLocale);
            Preference preferenceLanguage = findPreference("settings_locale");
            preferenceLanguage.setSummary(language.getDescription(getActivity()));
        }

    }
}
