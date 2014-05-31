package com.swipesapp.android.ui.view;

import android.app.TimePickerDialog;
import android.content.Context;
import android.content.res.TypedArray;
import android.preference.Preference;
import android.text.format.DateFormat;
import android.util.AttributeSet;

import com.negusoft.holoaccent.dialog.AccentTimePickerDialog;

/**
 * Created by douglasdrumond on 4/27/14.
 */
public class TimePreference extends Preference {
    private int mLastHour;
    private int mLastMinute;

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

        setOnPreferenceClickListener(new OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                TimePickerDialog.OnTimeSetListener listener = new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(android.widget.TimePicker timePicker, int i, int i1) {
                        mLastHour = timePicker.getCurrentHour();
                        mLastMinute = timePicker.getCurrentMinute();

                        String time = String.valueOf(mLastHour) + ":" + String.valueOf(mLastMinute);

                        if (callChangeListener(time)) {
                            persistString(time);
                        }
                    }
                };

                AccentTimePickerDialog dialog = new AccentTimePickerDialog(getContext(), listener, mLastHour, mLastMinute,
                        DateFormat.is24HourFormat(getContext()));
                dialog.setTitle(getTitle());
                dialog.show();

                return true;
            }
        });
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
