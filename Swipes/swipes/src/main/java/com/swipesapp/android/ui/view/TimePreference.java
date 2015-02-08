package com.swipesapp.android.ui.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.preference.Preference;
import android.support.v7.app.ActionBarActivity;
import android.text.format.DateFormat;
import android.util.AttributeSet;

import com.doomonafireball.betterpickers.radialtimepicker.RadialTimePickerDialog;
import com.swipesapp.android.R;
import com.swipesapp.android.util.ThemeUtils;

public class TimePreference extends Preference {

    private static final String TIME_PICKER_TAG = "SETTINGS_TIME_PICKER";

    private String mTime;

    public TimePreference(final Context context, AttributeSet attrs) {
        super(context, attrs);

        if (context instanceof ActionBarActivity) {
            setOnPreferenceClickListener(new OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    ActionBarActivity activity = (ActionBarActivity) context;

                    RadialTimePickerDialog.OnTimeSetListener timeSetListener = new RadialTimePickerDialog.OnTimeSetListener() {
                        @Override
                        public void onTimeSet(RadialTimePickerDialog dialog, int hourOfDay, int minute) {
                            mTime = String.valueOf(hourOfDay) + ":" + String.valueOf(minute);

                            if (callChangeListener(mTime)) {
                                persistString(mTime);
                            }
                        }
                    };

                    RadialTimePickerDialog dialog = new RadialTimePickerDialog();
                    dialog.setStartTime(getHour(mTime), getMinute(mTime));
                    dialog.setOnTimeSetListener(timeSetListener);
                    dialog.setDoneText(getContext().getString(R.string.preference_yes));
                    dialog.setThemeDark(!ThemeUtils.isLightTheme(getContext()));
                    dialog.set24HourMode(DateFormat.is24HourFormat(getContext()));
                    dialog.show(activity.getSupportFragmentManager(), TIME_PICKER_TAG);

                    return true;
                }
            });
        }
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        return a.getString(index);
    }

    @Override
    protected void onSetInitialValue(boolean restoreValue, Object defaultValue) {
        if (restoreValue) {
            mTime = getPersistedString(null);
        } else {
            mTime = String.valueOf(defaultValue);

            persistString(mTime);
        }
    }

    public static int getHour(String time) {
        String hour = time.split(":")[0];

        return Integer.parseInt(hour);
    }

    public static int getMinute(String time) {
        String minute = time.split(":")[1];

        return Integer.parseInt(minute);
    }

}
