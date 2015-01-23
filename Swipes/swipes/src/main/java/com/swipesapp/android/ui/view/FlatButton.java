package com.swipesapp.android.ui.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.widget.Button;

import com.swipesapp.android.R;
import com.swipesapp.android.util.ThemeUtils;

public class FlatButton extends Button {

    public FlatButton(Context context) {
        super(context);
    }

    public FlatButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public FlatButton(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        TypedArray array = context.obtainStyledAttributes(attrs, R.styleable.FlatButton);

        int textColor = array.getColor(R.styleable.FlatButton_textColor, Color.BLACK);
        int background = ThemeUtils.isLightTheme(context) ? R.drawable.flat_button_light : R.drawable.flat_button_dark;
        int padding = getResources().getDimensionPixelSize(R.dimen.flat_button_padding);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            background = ThemeUtils.isLightTheme(context) ? R.drawable.flat_button_ripple_light : R.drawable.flat_button_ripple_dark;
        }

        setBackgroundResource(background);
        setPadding(padding, padding, padding, padding);
        setTextAppearance(context, android.R.style.TextAppearance_Small);
        setTypeface(null, Typeface.BOLD);
        setTextColor(textColor);
        setEllipsize(TextUtils.TruncateAt.END);
        setSingleLine(true);
    }

}
