package com.swipesapp.android.app;

import android.app.Application;
import android.preference.PreferenceManager;

import com.parse.Parse;
import com.parse.ParseFacebookUtils;
import com.swipesapp.android.R;
import com.swipesapp.android.db.DaoMaster;
import com.swipesapp.android.db.DaoSession;
import com.swipesapp.android.db.migration.SwipesHelper;
import com.swipesapp.android.evernote.EvernoteIntegration;

/**
 * Swipes custom application class.
 */
public class SwipesApplication extends Application {

    public static DaoSession sDaoSession;

    private static final String DB_NAME = "swipes-db";

    @Override
    public void onCreate() {
        super.onCreate();

        // Initialize the Parse SDK.
        Parse.initialize(this, getString(R.string.application_id), getString(R.string.client_key));
        ParseFacebookUtils.initialize(getString(R.string.facebook_app_id));
        Parse.setLogLevel(Parse.LOG_LEVEL_DEBUG);

        // Initialize database session.
        SwipesHelper helper = new SwipesHelper(getApplicationContext(), DB_NAME, null);
        DaoMaster daoMaster = new DaoMaster(helper.getWritableDatabase());
        sDaoSession = daoMaster.newSession();

        // Provide initial context for Evernote integration.
        EvernoteIntegration.getInstance().setContext(getApplicationContext());

        // Load default user preferences.
        PreferenceManager.setDefaultValues(getApplicationContext(), R.xml.settings, true);
        PreferenceManager.setDefaultValues(getApplicationContext(), R.xml.snooze_settings, true);
        PreferenceManager.setDefaultValues(getApplicationContext(), R.xml.integrations, true);
    }

    public static DaoSession getDaoSession() {
        return sDaoSession;
    }

}
