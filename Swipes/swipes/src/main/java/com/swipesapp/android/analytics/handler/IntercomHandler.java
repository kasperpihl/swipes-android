package com.swipesapp.android.analytics.handler;

import android.content.Context;

import com.swipesapp.android.util.PreferenceUtils;

import java.util.HashMap;

import intercom.intercomsdk.Intercom;

/**
 * Convenience class to handle Intercom calls.
 *
 * @author Felipe Bari
 */
public class IntercomHandler {

    public static final String API_KEY = "android_sdk-36ef4b52dec031bf012025ff108440e441350295";
    public static final String APP_ID = "yobuz4ff";

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
            Intercom.beginSessionWithEmail(email, null);
        } else {
            Intercom.beginSessionForAnonymousUser(null);
        }
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

        Intercom.updateUser(attributes);
    }

}
