package com.swipesapp.android.ui.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;

import com.swipesapp.android.R;
import com.swipesapp.android.handler.SoundHandler;
import com.swipesapp.android.values.Constants;

/**
 * Custom checkbox for tags providing touch feedback and sounds.
 *
 * @author Felipe Bari
 */
public class TagBox extends CheckBox {

    public TagBox(Context context) {
        super(context, null);
        init(context);
    }

    public TagBox(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public void init(final Context context) {
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

        // Play sound when check changes.
        setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                // Play sound.
                int sound = isChecked ? R.raw.action_positive : R.raw.action_negative;
                SoundHandler.playSound(context, sound);
            }
        });
    }

}
