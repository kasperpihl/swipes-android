package com.swipesapp.android.ui.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.CheckBox;

import com.swipesapp.android.values.Constants;

/**
 * Custom checkbox for tags providing touch feedback.
 *
 * @author Felipe Bari
 */
public class TagBox extends CheckBox {

    public TagBox(Context context) {
        super(context, null);
        enableTouchFeedback();
    }

    public TagBox(Context context, AttributeSet attrs) {
        super(context, attrs);
        enableTouchFeedback();
    }

    public void enableTouchFeedback() {
        // Create selector based on touch state.
        setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                switch (motionEvent.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        // Change alpha to pressed state.
                        animate().alpha(Constants.PRESSED_BUTTON_ALPHA);
                        break;
                    case MotionEvent.ACTION_UP:
                        // Change alpha to default state.
                        animate().alpha(1.0f);
                        break;
                }
                return false;
            }
        });
    }

    public void disableTouchFeedback() {
        // Remove touch listener.
        setOnTouchListener(null);
    }

}
