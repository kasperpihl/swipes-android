package com.swipesapp.android.util;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.swipesapp.android.app.SwipesApplication;

/**
 * Convenience class to handle analytics calls.
 */
public class Analytics {

    public static final String SCREEN_LATER = "Later Tab";
    public static final String SCREEN_FOCUS = "Today Tab";
    public static final String SCREEN_DONE = "Done Tab";

    /**
     * Sends a screen view event.
     *
     * @param screenName Screen name to send.
     */
    public static void sendScreenView(String screenName) {
        Tracker tracker = SwipesApplication.getTracker();
        tracker.setScreenName(screenName);
        tracker.send(new HitBuilders.AppViewBuilder().build());
    }

}
