package com.swipesapp.android.ui.view;

import android.content.Context;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;

import com.swipesapp.android.util.Constants;
import com.swipesapp.android.util.ThemeUtils;

public class SwipesButton extends Button {

    private Context mContext;
    private static Typeface sTypeface;

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
                    sTypeface = Typeface.createFromAsset(mContext.getAssets(), Constants.FONT_NAME);
                }
            }
        }
        this.setTypeface(sTypeface);
        this.setTextColor(ThemeUtils.getTextColor(mContext));
    }

    public void setSelector(final int resource, final int resourceFull) {
        // Create selector based on touch state.
        setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                switch (motionEvent.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        // Change resource to pressed state.
                        setText(mContext.getString(resourceFull));
                        break;
                    case MotionEvent.ACTION_UP:
                        // Change resource to default state.
                        setText(mContext.getString(resource));
                        break;
                }
                return false;
            }
        });
    }

}
