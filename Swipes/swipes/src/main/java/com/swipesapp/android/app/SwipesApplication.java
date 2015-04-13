package com.swipesapp.android.app;

import android.app.Application;
import android.content.Context;
import android.preference.PreferenceManager;

import com.crashlytics.android.Crashlytics;
import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.Logger.LogLevel;
import com.google.android.gms.analytics.Tracker;
import com.parse.Parse;
import com.parse.ParseFacebookUtils;
import com.parse.ParseUser;
import com.swipesapp.android.BuildConfig;
import com.swipesapp.android.R;
import com.swipesapp.android.analytics.handler.Analytics;
import com.swipesapp.android.analytics.handler.IntercomHandler;
import com.swipesapp.android.db.DaoMaster;
import com.swipesapp.android.db.DaoSession;
import com.swipesapp.android.db.migration.SwipesHelper;
import com.swipesapp.android.evernote.EvernoteService;
import com.swipesapp.android.evernote.EvernoteSyncHandler;
import com.swipesapp.android.sync.receiver.SnoozeHelper;
import com.swipesapp.android.sync.service.SyncService;
import com.swipesapp.android.sync.service.TasksService;

import io.intercom.android.sdk.Intercom;

/**
 * Swipes custom application class.
 *
 * @author Felipe Bari
 */
public class SwipesApplication extends Application {

    public static DaoSession sDaoSession;

    private static final String DB_NAME = "swipes-db";

    private static Tracker sTracker;

    @Override
    public void onCreate() {
        super.onCreate();

        // Initialize Crashlytics.
        Crashlytics.start(this);

        // Initialize the Parse SDK.
        Parse.initialize(this, getString(R.string.application_id), getString(R.string.client_key));
        ParseFacebookUtils.initialize(getString(R.string.facebook_app_id));
        Parse.setLogLevel(Parse.LOG_LEVEL_DEBUG);

        // Initialize database session.
        startDaoSession(getApplicationContext());

        // Initialize services.
        TasksService.newInstance(getApplicationContext());
        SyncService.newInstance(getApplicationContext());

        // Initialize Evernote service.
        EvernoteService.newInstance(getApplicationContext());
        EvernoteSyncHandler.newInstance(getApplicationContext());

        // Start snooze alarm.
        SnoozeHelper.createSnoozeAlarm(getApplicationContext());

        // Start Analytics tracker.
        startTracker(getApplicationContext());

        // Initialize Intercom.
        Intercom.initialize(this, IntercomHandler.API_KEY, IntercomHandler.APP_ID);

        // Load default user preferences.
        PreferenceManager.setDefaultValues(getApplicationContext(), R.xml.settings, true);
        PreferenceManager.setDefaultValues(getApplicationContext(), R.xml.snooze_settings, true);
        PreferenceManager.setDefaultValues(getApplicationContext(), R.xml.integrations, true);

        // Send initial user dimensions.
        Analytics.startUserDimensions(getApplicationContext());
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

}
