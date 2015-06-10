package com.swipesapp.android.sync.receiver;

import android.app.Notification;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.parse.ParsePushBroadcastReceiver;
import com.swipesapp.android.sync.service.SyncService;
import com.swipesapp.android.util.PreferenceUtils;
import com.swipesapp.android.values.Constants;

/**
 * Custom receiver to handle push messages.
 *
 * @author Felipe Bari
 */
public class PushReceiver extends ParsePushBroadcastReceiver {

    private static final String LOG_TAG = PushReceiver.class.getSimpleName();

    private static final String EXTRA_CONTENT = "content-available";
    private static final String EXTRA_SYNC_ID = "syncId";

    private static SyncService sSyncService = SyncService.getInstance();

    @Override
    protected void onPushReceive(Context context, Intent intent) {
        super.onPushReceive(context, intent);

        // Initialize service if needed.
        if (sSyncService == null) sSyncService = SyncService.newInstance(context);

        // Read payload data in the intent.
        String contentAvailable = intent.getStringExtra(EXTRA_CONTENT);
        String syncId = intent.getStringExtra(EXTRA_SYNC_ID);

        // Determine sync conditions.
        boolean hasContent = contentAvailable != null && contentAvailable.equals("1");
        String lastSyncId = PreferenceUtils.readString(PreferenceUtils.LAST_SYNC_ID, context);
        boolean isDifferentDevice = (syncId != null && !syncId.isEmpty()) && lastSyncId != null && !syncId.equals(lastSyncId);

        if (hasContent && isDifferentDevice) {
            // Perform background sync.
            sSyncService.performSync(true, Constants.SYNC_DELAY);
        }

        Log.d(LOG_TAG, "Push received");
    }

    @Override
    protected Notification getNotification(Context context, Intent intent) {
        // We don't want the push to trigger a notification.
        return null;
    }

}
