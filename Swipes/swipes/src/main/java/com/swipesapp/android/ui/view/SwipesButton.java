package com.swipesapp.android.ui.view;

import android.content.Context;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.widget.Button;

public class SwipesButton extends Button {
    private Context mContext;
    private static Typeface sTypeface;
    private static final String FONT_NAME = "swipes.ttf";

    public SwipesButton(Context context) {
        super(context);
        init(context);
    }

    public SwipesButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public SwipesButton(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    private void init(Context context) {
        mContext = context;
        if (sTypeface == null) {
            synchronized (SwipesButton.class) {
                if (sTypeface == null) {
                    sTypeface = Typeface.createFromAsset(mContext.getAssets(), FONT_NAME);
                }
            }
        }
        this.setTypeface(sTypeface);
    }
}
