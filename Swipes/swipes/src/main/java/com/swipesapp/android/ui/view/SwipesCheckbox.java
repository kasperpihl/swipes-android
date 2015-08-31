package com.swipesapp.android.ui.view;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Typeface;
import android.os.Build;
import android.util.AttributeSet;
import android.util.TypedValue;
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

        int color = ThemeUtils.isLightTheme(context) ? R.color.checkbox_text_selector_light : R.color.checkbox_text_selector_dark;
        setTextColor(context.getResources().getColorStateList(color));

        setTypeface(sTypeface);
        enableTouchFeedback();
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
