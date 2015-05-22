package com.swipesapp.android.ui.view;

import android.content.Context;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.CheckBox;

import com.swipesapp.android.R;
import com.swipesapp.android.util.ThemeUtils;
import com.swipesapp.android.values.Constants;

public class SwipesCheckbox extends CheckBox {

    private static Typeface sTypeface;

    public SwipesCheckbox(Context context) {
        super(context);
        init(context);
    }

    public SwipesCheckbox(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public SwipesCheckbox(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    private void init(Context context) {
        if (sTypeface == null) {
            synchronized (SwipesCheckbox.class) {
                if (sTypeface == null) {
                    sTypeface = Typeface.createFromAsset(context.getAssets(), Constants.FONT_NAME);
                }
            }
        }

        int color = ThemeUtils.isLightTheme(context) ? R.color.checkbox_text_selector_light : R.color.checkbox_text_selector_light;
        setTextColor(context.getResources().getColorStateList(color));

        setTypeface(sTypeface);
        enableTouchFeedback();
    }

    public void enableTouchFeedback() {
        // Create selector based on touch state.
        setOnTouchListener(new OnTouchListener() {
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
