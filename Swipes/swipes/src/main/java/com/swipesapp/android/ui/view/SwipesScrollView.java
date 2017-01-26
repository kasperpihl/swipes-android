package com.swipesapp.android.ui.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.Display;
import android.view.WindowManager;
import android.widget.ScrollView;

import java.lang.ref.WeakReference;

/**
 * Custom ScrollView that wraps content, while having a maximum height of x% of the screen.
 *
 * @author Fernanda Bari
 */
public class SwipesScrollView extends ScrollView {

    private WeakReference<Context> mContext;

    public SwipesScrollView(Context context) {
        super(context);
        init(context);
    }

    public SwipesScrollView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public SwipesScrollView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    private void init(Context context) {
        mContext = new WeakReference<>(context);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        WindowManager wm = (WindowManager) mContext.get().getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();

        int maxHeight = (int) Math.round(display.getHeight() * 0.5);
        heightMeasureSpec = MeasureSpec.makeMeasureSpec(maxHeight, MeasureSpec.AT_MOST);

        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

}
