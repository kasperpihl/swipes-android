package com.swipesapp.android.ui.view;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.TextView;

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

    public void select() {
        setBackgroundColor(ThemeUtils.getSectionColor(Sections.FOCUS, mContext));
    }

    public void clearSelection() {
        setBackgroundColor(0);
    }

}
