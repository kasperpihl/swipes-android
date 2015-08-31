package com.swipesapp.android.ui.view;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Typeface;
import android.os.Build;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.widget.Button;

import com.swipesapp.android.util.ThemeUtils;
import com.swipesapp.android.values.Constants;

public class SwipesButton extends Button {

    private static Typeface sTypeface;

    public SwipesButton(Context context) {
        super(context);
        init();
    }

    public SwipesButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public SwipesButton(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {
        if (sTypeface == null) {
            synchronized (SwipesButton.class) {
                if (sTypeface == null) {
                    sTypeface = Typeface.createFromAsset(getContext().getAssets(), Constants.FONT_NAME);
                }
            }
        }

        setTypeface(sTypeface);
        setTextColor(ThemeUtils.getTextColor(getContext()));
        setSelector();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void setSelector() {
        // Use borderless ripple on Lollipop.
        int resource = Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP ?
                android.R.attr.selectableItemBackgroundBorderless : android.R.attr.selectableItemBackground;

        TypedValue outValue = new TypedValue();
        getContext().getTheme().resolveAttribute(resource, outValue, true);
        setBackgroundResource(outValue.resourceId);
    }

}
