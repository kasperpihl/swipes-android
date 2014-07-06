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

    private static final int SCROLL_DURATION = 300;
    private static final double SCROLL_FACTOR = 2;

    public FactorSpeedScroller(Context context) {
        super(context);
    }

    @Override
    public void startScroll(int startX, int startY, int dx, int dy, int duration) {
        // Ignore received duration, use custom one instead.
        super.startScroll(startX, startY, dx, dy, (int) (SCROLL_DURATION * SCROLL_FACTOR));
    }

}
