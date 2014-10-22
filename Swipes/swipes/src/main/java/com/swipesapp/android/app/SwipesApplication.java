package com.swipesapp.android.app;

import android.app.Application;

import com.parse.Parse;
import com.parse.ParseFacebookUtils;
import com.swipesapp.android.R;

/**
 * Swipes custom application class.
 */
public class SwipesApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        // Initialize the Parse SDK.
        Parse.initialize(this, getString(R.string.application_id), getString(R.string.client_key));
        ParseFacebookUtils.initialize(getString(R.string.facebook_app_id));

        Parse.setLogLevel(Parse.LOG_LEVEL_DEBUG);
    }

}
