package com.swipesapp.android.widget;

import android.content.Intent;
import android.widget.RemoteViewsService;

/**
 * Service for the Swipes Now widget.
 */
public class NowWidgetService extends RemoteViewsService {

    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new NowWidgetFactory(getApplicationContext(), intent);
    }

}
