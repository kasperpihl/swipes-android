package com.swipesapp.android.ui.view;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.CheckBox;
import android.widget.CompoundButton;

import com.swipesapp.android.R;
import com.swipesapp.android.handler.SoundHandler;

/**
 * Custom checkbox for tags providing sounds.
 *
 * @author Fernanda Bari
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
