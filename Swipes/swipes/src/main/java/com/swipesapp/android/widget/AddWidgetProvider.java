package com.swipesapp.android.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

import com.swipesapp.android.R;
import com.swipesapp.android.ui.activity.TasksActivity;
import com.swipesapp.android.values.Intents;

/**
 * Provider for the Swipes Quick Add widget.
 *
 * @author Felipe Bari
 */
public class AddWidgetProvider extends AppWidgetProvider {

    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // Perform loop for each widget belonging to this provider.
        for (int appWidgetId : appWidgetIds) {
            // Initialize widget layout.
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.add_widget);

            // Add task intent.
            Intent addIntent = new Intent(context, TasksActivity.class);
            addIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
            addIntent.setAction(Intents.ADD_TASK);
            PendingIntent addPendingIntent = PendingIntent.getActivity(context, 0, addIntent, 0);

            // Attach click listener to button.
            views.setOnClickPendingIntent(R.id.add_widget_button, addPendingIntent);

            // Tell AppWidgetManager to update current widget.
            appWidgetManager.updateAppWidget(appWidgetId, views);
        }
    }

}
