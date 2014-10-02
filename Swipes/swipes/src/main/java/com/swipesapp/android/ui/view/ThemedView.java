package com.swipesapp.android.ui.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

import com.swipesapp.android.util.ThemeUtils;

/**
 * Simple view with background set according to the current theme.
 *
 * @author Felipe Bari
 */
public class ThemedView extends View {

    public ThemedView(Context context) {
        super(context);
        init(context);
    }

    public ThemedView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public ThemedView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    private void init(Context context) {
        setBackgroundColor(ThemeUtils.getTextColor(context));
    }

}
