package com.swipesapp.android.ui.view;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Typeface;
import android.os.Build;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.widget.TextView;

import com.swipesapp.android.values.Constants;

public class SwipesTextView extends TextView {

    private static Typeface sTypeface;

    public SwipesTextView(Context context) {
        super(context);
        init();
    }

    public SwipesTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public SwipesTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {
        if (sTypeface == null) {
            synchronized (SwipesTextView.class) {
                if (sTypeface == null) {
                    sTypeface = Typeface.createFromAsset(getContext().getAssets(), Constants.FONT_NAME);
                }
            }
        }
        this.setTypeface(sTypeface);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public void enableTouchFeedback() {
        // Use borderless ripple on Lollipop.
        int resource = Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP ?
                android.R.attr.selectableItemBackgroundBorderless : android.R.attr.selectableItemBackground;

        TypedValue outValue = new TypedValue();
        getContext().getTheme().resolveAttribute(resource, outValue, true);
        setBackgroundResource(outValue.resourceId);
    }

}
