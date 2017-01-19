package com.swipesapp.android.app;

import android.content.Context;
import android.content.res.Configuration;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.multidex.MultiDexApplication;

import com.crashlytics.android.Crashlytics;
import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.Logger.LogLevel;
import com.google.android.gms.analytics.Tracker;
import com.parse.Parse;
import com.parse.ParseFacebookUtils;
import com.parse.ParseInstallation;
import com.parse.ParseUser;
import com.parse.interceptors.ParseLogInterceptor;
import com.swipesapp.android.BuildConfig;
import com.swipesapp.android.R;
import com.swipesapp.android.analytics.handler.Analytics;
import com.swipesapp.android.db.DaoMaster;
import com.swipesapp.android.db.DaoSession;
import com.swipesapp.android.db.migration.SwipesHelper;
import com.swipesapp.android.evernote.EvernoteService;
import com.swipesapp.android.evernote.EvernoteSyncHandler;
import com.swipesapp.android.handler.LanguageHandler;
import com.swipesapp.android.handler.SoundHandler;
import com.swipesapp.android.sync.receiver.NotificationsHelper;
import com.swipesapp.android.sync.receiver.PushReceiver;
import com.swipesapp.android.sync.service.SyncService;
import com.swipesapp.android.sync.service.TasksService;
import com.swipesapp.android.util.PreferenceUtils;

import java.util.List;

import io.fabric.sdk.android.Fabric;

/**
 * Swipes custom application class.
 *
 * @author Felipe Bari
 */
public class SwipesApplication extends MultiDexApplication {

    public static DaoSession sDaoSession;

    private static final String DB_NAME = "swipes-db";

    private static Tracker sTracker;

    private static Handler sHandler;
    private static Runnable sRunnable;

    private static boolean sIsInBackground = true;

    @Override
    public void onCreate() {
        super.onCreate();

        // Initialize Crashlytics.
        Fabric.with(this, new Crashlytics());

        // Load default user preferences.
        loadDefaultPreferences(getApplicationContext());

        // Initialize the Parse SDK.
        Parse.initialize(new Parse.Configuration.Builder(getApplicationContext())
                .applicationId(getString(R.string.application_id))
                .clientKey(getString(R.string.client_key))
                .server(getString(R.string.server))
                .addNetworkInterceptor(new ParseLogInterceptor()).build());
        ParseFacebookUtils.initialize(getApplicationContext());
        Parse.setLogLevel(Parse.LOG_LEVEL_DEBUG);

        // Subscribe to push channels.
        subscribePush(getApplicationContext());

        // Initialize database session.
        startDaoSession(getApplicationContext());

        // Initialize services.
        TasksService.newInstance(getApplicationContext());
        SyncService.newInstance(getApplicationContext());

        // Initialize Evernote service.
        EvernoteService.newInstance(getApplicationContext());
        EvernoteSyncHandler.newInstance(getApplicationContext());

        // Apply user selected language.
        LanguageHandler.applyLanguage(getApplicationContext());

        // Start notification alarms.
        NotificationsHelper.createNotificationsAlarm(getApplicationContext(), null);
        NotificationsHelper.createRemindersAlarm(getApplicationContext());

        // Start Analytics tracker.
        startTracker(getApplicationContext());

        // Send initial user dimensions.
        Analytics.startUserDimensions(getApplicationContext());

        // Start sound handler.
        SoundHandler.load(getApplicationContext());
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        // Configuration changed (possible rotation). Reapply language.
        LanguageHandler.applyLanguage(getApplicationContext());
    }

    public static void startDaoSession(Context context) {
        SwipesHelper helper = new SwipesHelper(context, DB_NAME, null);
        DaoMaster daoMaster = new DaoMaster(helper.getWritableDatabase());
        sDaoSession = daoMaster.newSession();
    }

    public static DaoSession getDaoSession() {
        return sDaoSession;
    }

    public static void startTracker(Context context) {
        GoogleAnalytics analytics = GoogleAnalytics.getInstance(context);
        sTracker = analytics.newTracker(R.xml.app_tracker);

        if (BuildConfig.DEBUG) {
            analytics.getLogger().setLogLevel(LogLevel.VERBOSE);

            // HACK: Force replace tracking ID with the debug one.
            sTracker.set("&tid", "UA-41592802-6");
        }

        ParseUser user = ParseUser.getCurrentUser();
        if (user != null) {
            sTracker.set("&uid", user.getObjectId());
        }
    }

    public static Tracker getTracker() {
        return sTracker;
    }

    public static void loadDefaultPreferences(Context context) {
        PreferenceManager.setDefaultValues(context, R.xml.settings, true);
        PreferenceManager.setDefaultValues(context, R.xml.options, true);
        PreferenceManager.setDefaultValues(context, R.xml.snooze_settings, true);
        PreferenceManager.setDefaultValues(context, R.xml.integrations, true);
    }

    public static void startBackgroundTimer() {
        sHandler = new Handler();
        sRunnable = new Runnable() {
            @Override
            public void run() {
                // Mark application as gone to background.
                sIsInBackground = true;
            }
        };
        sHandler.postDelayed(sRunnable, 2000);
    }

    public static void stopBackgroundTimer() {
        // Cancel background timer.
        if (sHandler != null) sHandler.removeCallbacks(sRunnable);

        // Mark application as in foreground.
        sIsInBackground = false;
    }

    public static boolean isInBackground() {
        return sIsInBackground;
    }

    public static void subscribePush(Context context) {
        if (PreferenceUtils.isBackgroundSyncEnabled(context)) {
            // Background sync is enabled. Try to subscribe.
            ParseUser user = ParseUser.getCurrentUser();
            ParseInstallation installation = ParseInstallation.getCurrentInstallation();

            if (user != null) {
                List<?> channels = installation.getList(PushReceiver.KEY_CHANNELS);

                if (channels == null || !channels.contains(user.getObjectId())) {
                    // User is logged in. Subscribe to push channel.
                    installation.add(PushReceiver.KEY_CHANNELS, user.getObjectId());

                    if (BuildConfig.DEBUG) {
                        // Also subscribe to debug channel when needed.
                        installation.add(PushReceiver.KEY_CHANNELS, PushReceiver.CHANNEL_DEV);
                    }
                }
            }

            installation.saveInBackground();
        }
    }

    public static void unsubscribePush() {
        ParseInstallation installation = ParseInstallation.getCurrentInstallation();

        // Unsubscribe from all channels.
        installation.remove(PushReceiver.KEY_CHANNELS);
        installation.saveInBackground();
    }

}
