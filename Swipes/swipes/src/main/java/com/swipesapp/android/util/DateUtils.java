package com.swipesapp.android.util;

import android.content.Context;
import android.text.format.DateFormat;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * Utilitary class for date operations.
 *
 * @author Felipe Bari
 */
public class DateUtils {

    /**
     * Returns a Calendar object for the given date.
     *
     * @param date Desired date.
     * @return Calendar object.
     */
    public static Calendar getCalendarFromDate(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);

        return calendar;
    }

    /**
     * Returns formatted time for a given date. Format will be "HH:mm" or "hh:mm a",
     * depending on the device's 24-hour format setting.
     *
     * @param date Desired date.
     * @return Formatted time.
     */
    public static String getTimeAsString(Context context, Date date) {
        String time = null;

        if (DateFormat.is24HourFormat(context)) {
            time = new SimpleDateFormat("HH:mm").format(date);
        } else {
            time = new SimpleDateFormat("hh:mm a").format(date);
        }

        return time;
    }

}
