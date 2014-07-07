package com.swipesapp.android.util;

import android.content.Context;
import android.text.format.DateFormat;

import com.swipesapp.android.R;

import java.text.ParseException;
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

    /**
     * Returns date formatted as "Today", "Tomorrow" or a regular date.
     *
     * @param rawDate    Date to format.
     * @param context Context instance.
     * @return Formatted date.
     * @throws ParseException When the provided date can't be parsed.
     */
    public static String formatToTodayOrTomorrow(Date rawDate, Context context) throws ParseException {
        String date = new SimpleDateFormat("EEE hh:mma MMM d, yyyy").format(rawDate);
        Date dateTime = new SimpleDateFormat("EEE hh:mma MMM d, yyyy").parse(date);
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(dateTime);
        Calendar today = Calendar.getInstance();
        Calendar tomorrow = Calendar.getInstance();
        tomorrow.add(Calendar.DATE, 1);
        java.text.DateFormat timeFormatter = new SimpleDateFormat("hh:mma");

        if (calendar.get(Calendar.YEAR) == today.get(Calendar.YEAR) && calendar.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)) {
            return context.getString(R.string.date_today) + timeFormatter.format(dateTime);
        } else if (calendar.get(Calendar.YEAR) == tomorrow.get(Calendar.YEAR) && calendar.get(Calendar.DAY_OF_YEAR) == tomorrow.get(Calendar.DAY_OF_YEAR)) {
            return context.getString(R.string.date_tomorrow) + timeFormatter.format(dateTime);
        } else {
            return date;
        }
    }

}
