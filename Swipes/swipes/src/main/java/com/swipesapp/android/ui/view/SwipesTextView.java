package com.swipesapp.android.ui.view;

import android.content.Context;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;

import com.swipesapp.android.values.Constants;

public class SwipesTextView extends TextView {

    private Context mContext;
    private static Typeface sTypeface;

    public SwipesTextView(Context context) {
        super(context);
        init(context);
    }

    public SwipesTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public SwipesTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    private void init(Context context) {
        mContext = context;
        if (sTypeface == null) {
            synchronized (SwipesButton.class) {
                if (sTypeface == null) {
                    sTypeface = Typeface.createFromAsset(mContext.getAssets(), Constants.FONT_NAME);
                }
            }
        }
        this.setTypeface(sTypeface);
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

}
