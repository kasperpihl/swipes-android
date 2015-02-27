package com.swipesapp.android.ui.view;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;
import android.widget.TextView;

import com.swipesapp.android.util.ColorUtils;
import com.swipesapp.android.values.Constants;
import com.swipesapp.android.util.ThemeUtils;
import com.swipesapp.android.values.Sections;

/**
 * Text view with background set according to selection.
 *
 * @author Felipe Bari
 */
public class RepeatOption extends TextView {

    private Context mContext;

    public RepeatOption(Context context) {
        super(context);
        init(context);
    }

    public RepeatOption(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public RepeatOption(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    private void init(Context context) {
        mContext = context;
    }

    public void select(Sections section) {
        int sectionColor = ThemeUtils.getSectionColor(section, mContext);
        int backgroundColor = ThemeUtils.getBackgroundColor(mContext);

        transitionBackground(backgroundColor, sectionColor);
    }

    public void clearSelection() {
        setBackgroundColor(0);
        setTextColor(ThemeUtils.getTextColor(mContext));
    }

    private void transitionBackground(final int fromColor, final int toColor) {
        final int fromText = ThemeUtils.getTextColor(mContext);
        final int toText = Color.WHITE;

        ValueAnimator anim = ValueAnimator.ofFloat(0, 1);

        anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                // Blend colors according to position.
                float position = animation.getAnimatedFraction();
                int background = ColorUtils.blendColors(fromColor, toColor, position);
                int textColor = ColorUtils.blendColors(fromText, toText, position);

                // Adjust background and text colors.
                setBackgroundColor(background);
                setTextColor(textColor);
            }
        });

        anim.setDuration(Constants.ANIMATION_DURATION_MEDIUM).start();
    }

}
