package com.swipesapp.android.ui.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TimePicker;

import com.swipesapp.android.R;

/**
 * Created by douglasdrumond on 4/27/14.
 */
public class TimePreference extends DialogPreference {
    private int mLastHour;
    private int mLastMinute;
    private TimePicker mTimePicker;

    public static int getHour(String time) {
        String[] pieces = time.split(":");

        return Integer.parseInt(pieces[0]);
    }

    public static int getMinute(String time) {
        String[] pieces = time.split(":");

        return Integer.parseInt(pieces[1]);
    }

    public TimePreference(Context context, AttributeSet attrs) {
        super(context, attrs);

        setPositiveButtonText(context.getString(R.string.set));
        setNegativeButtonText(context.getString(R.string.cancel));
    }

    @Override
    protected View onCreateDialogView() {
        mTimePicker = new TimePicker(getContext());

        return mTimePicker;
    }

    @Override
    protected void onBindDialogView(View v) {
        super.onBindDialogView(v);

        mTimePicker.setCurrentHour(mLastHour);
        mTimePicker.setCurrentMinute(mLastMinute);
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);

        if (positiveResult) {
            mLastHour = mTimePicker.getCurrentHour();
            mLastMinute = mTimePicker.getCurrentMinute();

            String time = String.valueOf(mLastHour) + ":" + String.valueOf(mLastMinute);

            if (callChangeListener(time)) {
                persistString(time);
            }
        }
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        return a.getString(index);
    }

    @Override
    protected void onSetInitialValue(boolean restoreValue, Object defaultValue) {
        String time;

        if (restoreValue) {
            if (defaultValue == null) {
                time = getPersistedString("00:00");
            } else {
                time = getPersistedString(defaultValue.toString());
            }
        } else {
            time = defaultValue.toString();
        }

        mLastHour = getHour(time);
        mLastMinute = getMinute(time);
    }
}
