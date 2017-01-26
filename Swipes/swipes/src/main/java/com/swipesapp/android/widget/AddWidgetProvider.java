package com.swipesapp.android.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

import com.swipesapp.android.R;
import com.swipesapp.android.ui.activity.AddTasksActivity;
import com.swipesapp.android.values.Constants;
import com.swipesapp.android.values.Intents;

/**
 * Provider for the Add to Swipes widget.
 *
 * @author Fernanda Bari
 */
public class AddWidgetProvider extends AppWidgetProvider {

    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // Perform loop for each widget belonging to this provider.
        for (int appWidgetId : appWidgetIds) {
            // Initialize widget layout.
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.add_widget);

            // Add task intent.
            Intent addIntent = new Intent(context, AddTasksActivity.class);
            addIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
            addIntent.setAction(Intents.ADD_TASK);
            addIntent.putExtra(Constants.EXTRA_FROM_WIDGET, true);
            PendingIntent addPendingIntent = PendingIntent.getActivity(context, 0, addIntent, 0);

            // Attach click listener to button.
            views.setOnClickPendingIntent(R.id.add_widget_button, addPendingIntent);

            // Set text for description.
            views.setTextViewText(R.id.add_widget_button_description, context.getString(R.string.add_widget_description));

            // Tell AppWidgetManager to update current widget.
            appWidgetManager.updateAppWidget(appWidgetId, views);
        }
    }

    public static void refreshWidget(Context context) {
        // Load manager and IDs.
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        int[] ids = manager.getAppWidgetIds(new ComponentName(context, AddWidgetProvider.class));

        // Update widget intent.
        Intent intent = new Intent(context, AddWidgetProvider.class);
        intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);

        // Send update broadcast.
        context.sendBroadcast(intent);
    }

}
