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
                            mLastHour = hourOfDay;
                            mLastMinute = minute;

                            String time = String.valueOf(mLastHour) + ":" + String.valueOf(mLastMinute);

                            if (callChangeListener(time)) {
                                persistString(time);
                            }
                        }
                    };

                    RadialTimePickerDialog dialog = new RadialTimePickerDialog();
                    dialog.setStartTime(mLastHour, mLastMinute);
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
