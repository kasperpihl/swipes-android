package com.swipesapp.android.app;

import android.app.Application;
import android.preference.PreferenceManager;

import com.crashlytics.android.Crashlytics;
import com.parse.Parse;
import com.parse.ParseFacebookUtils;
import com.swipesapp.android.R;
import com.swipesapp.android.db.DaoMaster;
import com.swipesapp.android.db.DaoSession;
import com.swipesapp.android.db.migration.SwipesHelper;
import com.swipesapp.android.evernote.EvernoteService;
import com.swipesapp.android.evernote.EvernoteSyncHandler;
import com.swipesapp.android.sync.receiver.SnoozeHelper;
import com.swipesapp.android.sync.service.SyncService;
import com.swipesapp.android.sync.service.TasksService;

/**
 * Swipes custom application class.
 */
public class SwipesApplication extends Application {

    public static DaoSession sDaoSession;

    private static final String DB_NAME = "swipes-db";

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
        SwipesHelper helper = new SwipesHelper(getApplicationContext(), DB_NAME, null);
        DaoMaster daoMaster = new DaoMaster(helper.getWritableDatabase());
        sDaoSession = daoMaster.newSession();

        // Initialize services.
        TasksService.newInstance(getApplicationContext());
        SyncService.newInstance(getApplicationContext());

        // Initialize Evernote service.
        EvernoteService.newInstance(getApplicationContext());
        EvernoteSyncHandler.newInstance(getApplicationContext());

        // Start snooze alarm.
        SnoozeHelper.createSnoozeAlarm(getApplicationContext());

        // Load default user preferences.
        PreferenceManager.setDefaultValues(getApplicationContext(), R.xml.settings, true);
        PreferenceManager.setDefaultValues(getApplicationContext(), R.xml.snooze_settings, true);
        PreferenceManager.setDefaultValues(getApplicationContext(), R.xml.integrations, true);
    }

    public static DaoSession getDaoSession() {
        return sDaoSession;
    }

}
