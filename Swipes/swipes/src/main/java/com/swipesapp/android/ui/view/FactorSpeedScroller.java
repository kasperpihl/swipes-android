package com.swipesapp.android.ui.view;

import android.content.Context;
import android.widget.Scroller;

/**
 * Customizes speed of scroller with a fine tuned duration using a factor, providing
 * a more natural feel to the animation.
 *
 * @author Felipe Bari
 */
public class FactorSpeedScroller extends Scroller {

    public static final int DURATION_MEDIUM = 300;
    public static final int DURATION_SHORT = 180;
    public static final int DURATION_LONG = 400;
    private static final double SCROLL_FACTOR = 2;

    private int mDuration = DURATION_MEDIUM;

    public FactorSpeedScroller(Context context) {
        super(context);
    }

    @Override
    public void startScroll(int startX, int startY, int dx, int dy, int duration) {
        // Ignore received duration, use custom one instead.
        super.startScroll(startX, startY, dx, dy, (int) (mDuration * SCROLL_FACTOR));
    }

    public void setDuration(int duration) {
        mDuration = duration;
    }

}
