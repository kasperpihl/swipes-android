package com.swipesapp.android.analytics.handler;

import android.content.Context;
import android.util.Log;

import com.swipesapp.android.BuildConfig;
import com.swipesapp.android.util.PreferenceUtils;

import java.util.HashMap;

import io.intercom.android.sdk.Intercom;
import io.intercom.android.sdk.identity.Registration;

/**
 * Convenience class to handle Intercom calls.
 *
 * @author Felipe Bari
 */
public class IntercomHandler {

    private static final String LOG_TAG = IntercomHandler.class.getSimpleName();

    public static final String API_KEY = "android_sdk-36ef4b52dec031bf012025ff108440e441350295";
    public static final String APP_ID = "yobuz4ff";

    // Custom attributes.
    private static final String ATTR_CUSTOM = "custom_attributes";
    private static final String ATTR_USER_LEVEL = "user_level";
    private static final String ATTR_EVERNOTE_LEVEL = "evernote_user_level";
    private static final String ATTR_ACTIVE_THEME = "active_theme";
    private static final String ATTR_RECURRING = "recurring_tasks";
    private static final String ATTR_TAGS = "number_of_tags";
    private static final String ATTR_MAILBOX_STATUS = "mailbox_installed";

    /**
     * Starts an Intercom session.
     *
     * @param email User email. Pass null for anonymous session.
     */
    public static void beginIntercomSession(String email) {
        if (email != null && !email.isEmpty()) {
            Intercom.client().registerIdentifiedUser(new Registration().withEmail(email));
        } else {
            Intercom.client().registerUnidentifiedUser();
        }
    }

    /**
     * Sends an Intercom event.
     *
     * @param event Event to send.
     * @param data  Additional data.
     */
    public static void sendEvent(String event, HashMap<String, Object> data) {
        if (data != null) {
            Intercom.client().logEvent(event, data);
        } else {
            Intercom.client().logEvent(event);
        }

        logDebugMessage("Sent Intercom event: " + event);
    }

    /**
     * Sends updated Intercom attributes.
     *
     * @param context Context instance.
     */
    public static void sendIntercomAttributes(Context context) {
        HashMap<String, Object> attributes = new HashMap<>();
        HashMap<String, Object> customAttributes = new HashMap<>();

        customAttributes.put(ATTR_USER_LEVEL, PreferenceUtils.readString(PreferenceUtils.USER_LEVEL, context));
        customAttributes.put(ATTR_EVERNOTE_LEVEL, PreferenceUtils.readString(PreferenceUtils.EVERNOTE_LEVEL, context));
        customAttributes.put(ATTR_ACTIVE_THEME, PreferenceUtils.readString(PreferenceUtils.ACTIVE_THEME, context));
        customAttributes.put(ATTR_RECURRING, PreferenceUtils.readString(PreferenceUtils.RECURRING_COUNT, context));
        customAttributes.put(ATTR_TAGS, PreferenceUtils.readString(PreferenceUtils.TAGS_COUNT, context));
        customAttributes.put(ATTR_MAILBOX_STATUS, PreferenceUtils.readString(PreferenceUtils.MAILBOX_STATUS, context));

        attributes.put(ATTR_CUSTOM, customAttributes);
        Intercom.client().updateUser(attributes);

        logDebugMessage("Updated Intercom attributes.");
    }

    /**
     * Sends a log message in debug builds.
     *
     * @param message Message to log.
     */
    private static void logDebugMessage(String message) {
        if (BuildConfig.DEBUG) {
            Log.d(LOG_TAG, message);
        }
    }

}
