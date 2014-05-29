package com.swipesapp.android.ui.listener;

import com.swipesapp.android.values.Sections;

/**
 * Created by douglasdrumond on 4/30/14.
 */
public interface ListContentsListener {
    void onEmpty(Sections currentSection);
    void onNotEmpty();
}
