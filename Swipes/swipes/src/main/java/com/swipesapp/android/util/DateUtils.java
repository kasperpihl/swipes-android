package com.swipesapp.android.util;

import android.content.Context;
import android.text.format.DateFormat;
import android.util.Log;

import com.swipesapp.android.R;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

/**
 * Utilitary class for date operations.
 *
 * @author Felipe Bari
 */
public class DateUtils {

    // Log tag.
    private static final String LOG_TAG = DateUtils.class.getSimpleName();

    // Time format 24-hour.
    public static final String TIME_FORMAT_24 = "HH:mm";

    // Time format AM/PM.
    public static final String TIME_FORMAT_A = "hh:mm a";

    // Date format 24-hour.
    public static final String DATE_FORMAT_24 = "MMM d, HH:mm";

    // Date format AM/PM.
    public static final String DATE_FORMAT_A = "MMM d, hh:mm a";

    // Date format 24-hour with year.
    public static final String DATE_FORMAT_24_YEAR = "MMM d ''yy, hh:mm";

    // Date format AM/PM with year.
    public static final String DATE_FORMAT_A_YEAR = "MMM d ''yy, hh:mm a";

    // Date format for syncing.
    public static final String DATE_FORMAT_SYNC = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";

    // Timezone for syncing.
    public static final String TIMEZONE_SYNC = "UTC";

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
        String time;

        if (DateFormat.is24HourFormat(context)) {
            time = new SimpleDateFormat(TIME_FORMAT_24).format(date);
        } else {
            time = new SimpleDateFormat(TIME_FORMAT_A).format(date);
        }

        return time;
    }

    /**
     * Returns a formatted date. Format will be "MMM d, hh:mm" or "MMM d, hh:mm a",
     * depending on the device's 24-hour format setting.
     * <p/>
     * When the date is in a different year, the format will include it as "''yy".
     *
     * @param date Desired date.
     * @return Formatted date.
     */
    public static String getDateAsString(Context context, Date date) {
        String time;
        String format;

        if (DateFormat.is24HourFormat(context)) {
            format = isSameYear(date) ? DATE_FORMAT_24 : DATE_FORMAT_24_YEAR;
            time = new SimpleDateFormat(format).format(date);
        } else {
            format = isSameYear(date) ? DATE_FORMAT_A : DATE_FORMAT_A_YEAR;
            time = new SimpleDateFormat(format).format(date);
        }

        return time;
    }

    /**
     * Returns date formatted as "Today", "Tomorrow", "Yesterday" or a regular date.
     *
     * @param rawDate    Date to format.
     * @param context    Context instance.
     * @param capitalize True to capitalize first letter.
     * @return Formatted date.
     */
    public static String formatToRecent(Date rawDate, Context context, boolean capitalize) {
        // Check if date is unspecified.
        if (rawDate == null) return context.getString(R.string.date_unspecified);

        // Prepare given date.
        String date = getDateAsString(context, rawDate);
        Calendar providedDate = Calendar.getInstance();
        providedDate.setTime(rawDate);

        // Calendars for today, tomorrow and yesterday.
        Calendar today = Calendar.getInstance();
        Calendar tomorrow = Calendar.getInstance();
        tomorrow.add(Calendar.DATE, 1);
        Calendar yesterday = Calendar.getInstance();
        yesterday.add(Calendar.DATE, -1);

        if (providedDate.get(Calendar.YEAR) == today.get(Calendar.YEAR) && providedDate.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)) {
            // Date is today.
            date = context.getString(R.string.date_today) + getTimeAsString(context, rawDate);
        } else if (providedDate.get(Calendar.YEAR) == tomorrow.get(Calendar.YEAR) && providedDate.get(Calendar.DAY_OF_YEAR) == tomorrow.get(Calendar.DAY_OF_YEAR)) {
            // Date is tomorrow.
            date = context.getString(R.string.date_tomorrow) + getTimeAsString(context, rawDate);
        } else if (providedDate.get(Calendar.YEAR) == yesterday.get(Calendar.YEAR) && providedDate.get(Calendar.DAY_OF_YEAR) == yesterday.get(Calendar.DAY_OF_YEAR)) {
            // Date was yesterday.
            date = context.getString(R.string.date_yesterday) + getTimeAsString(context, rawDate);
        } else if (isWithinWeek(providedDate.getTime())) {
            // Date is within a week.
            date = formatDayOfWeek(context, providedDate) + ", " + getTimeAsString(context, rawDate);
        } else {
            // Date is some other day. Always capitalize first letter.
            return Character.toUpperCase(date.charAt(0)) + date.substring(1);
        }

        // Capitalize first letter if needed.
        return capitalize ? Character.toUpperCase(date.charAt(0)) + date.substring(1) : date;
    }

    /**
     * Checks if the provided date is today.
     *
     * @param date Date to check.
     * @return True if date is today.
     */
    public static boolean isToday(Date date) {
        Calendar providedDate = Calendar.getInstance();
        providedDate.setTime(date);
        Calendar currentDate = Calendar.getInstance();

        int providedYear = providedDate.get(Calendar.YEAR);
        int currentYear = providedDate.get(Calendar.YEAR);
        int providedDay = providedDate.get(Calendar.DAY_OF_YEAR);
        int currentDay = currentDate.get(Calendar.DAY_OF_YEAR);

        return providedYear == currentYear && providedDay == currentDay;
    }

    /**
     * Checks if the provided date is older than today.
     *
     * @param date Date to check.
     * @return True if date is older than today.
     */
    public static boolean isOlderThanToday(Date date) {
        if (date == null) {
            return false;
        }

        Calendar providedDate = Calendar.getInstance();
        providedDate.setTime(date);
        Calendar currentDate = Calendar.getInstance();

        int providedYear = providedDate.get(Calendar.YEAR);
        int currentYear = currentDate.get(Calendar.YEAR);
        int providedDay = providedDate.get(Calendar.DAY_OF_YEAR);
        int currentDay = currentDate.get(Calendar.DAY_OF_YEAR);

        return providedYear <= currentYear && (providedDay < currentDay || providedYear < currentYear);
    }

    /**
     * Checks if the provided date is newer than today.
     *
     * @param date Date to check.
     * @return True if date is newer than today.
     */
    public static boolean isNewerThanToday(Date date) {
        if (date == null) {
            return false;
        }

        Calendar providedDate = Calendar.getInstance();
        providedDate.setTime(date);
        Calendar currentDate = Calendar.getInstance();

        int providedYear = providedDate.get(Calendar.YEAR);
        int currentYear = currentDate.get(Calendar.YEAR);
        int providedDay = providedDate.get(Calendar.DAY_OF_YEAR);
        int currentDay = currentDate.get(Calendar.DAY_OF_YEAR);

        return providedYear >= currentYear && (providedDay > currentDay || providedYear > currentYear);
    }

    /**
     * Checks if the provided date is within a week from today.
     *
     * @param date Date to check.
     * @return True if date is within a week.
     */
    public static boolean isWithinWeek(Date date) {
        if (date == null) {
            return false;
        }

        Calendar providedDate = Calendar.getInstance();
        providedDate.setTime(date);
        Calendar currentDate = Calendar.getInstance();

        long providedMillis = providedDate.getTimeInMillis();
        long currentMillis = currentDate.getTimeInMillis();
        int providedDayOfWeek = providedDate.get(Calendar.DAY_OF_WEEK);
        int currentDayOfWeek = currentDate.get(Calendar.DAY_OF_WEEK);

        boolean isWithinWeek = providedMillis > currentMillis && providedMillis - currentMillis < 604800000L;
        boolean isSameDayNextWeek = providedMillis > currentMillis && providedDayOfWeek == currentDayOfWeek;

        return isWithinWeek || isSameDayNextWeek;
    }

    /**
     * Checks if the provided date is in the same year as today.
     *
     * @param date Date to check.
     * @return True if it's in the same year.
     */
    public static boolean isSameYear(Date date) {
        if (date == null) {
            return false;
        }

        Calendar providedDate = Calendar.getInstance();
        providedDate.setTime(date);
        Calendar currentDate = Calendar.getInstance();

        int providedYear = providedDate.get(Calendar.YEAR);
        int currentYear = currentDate.get(Calendar.YEAR);

        return providedYear == currentYear;
    }

    /**
     * Checks if the provided dates are in the same day.
     *
     * @param firstDate  First date to compare.
     * @param secondDate Second date to compare.
     * @return True if they are in the same day.
     */
    public static boolean isSameDay(Date firstDate, Date secondDate) {
        if (firstDate == null || secondDate == null) {
            return false;
        }

        Calendar first = Calendar.getInstance();
        first.setTime(firstDate);
        Calendar second = Calendar.getInstance();
        second.setTime(secondDate);

        boolean isSameYear = first.get(Calendar.YEAR) == second.get(Calendar.YEAR);
        boolean isSameDay = first.get(Calendar.DAY_OF_YEAR) == second.get(Calendar.DAY_OF_YEAR);

        return isSameYear && isSameDay;
    }

    /**
     * Formats day of the week to a friendly name.
     *
     * @param context Context instance.
     * @param date    Date containing day of the week.
     * @return Formatted string.
     */
    public static String formatDayOfWeek(Context context, Calendar date) {
        // Load resource string for day of the week.
        switch (date.get(Calendar.DAY_OF_WEEK)) {
            case 1:
                return context.getString(R.string.sunday_full);
            case 2:
                return context.getString(R.string.monday_full);
            case 3:
                return context.getString(R.string.tuesday_full);
            case 4:
                return context.getString(R.string.wednesday_full);
            case 5:
                return context.getString(R.string.thursday_full);
            case 6:
                return context.getString(R.string.friday_full);
            case 7:
                return context.getString(R.string.saturday_full);
            default:
                return "";
        }
    }

    /**
     * Formats month to a friendly name.
     *
     * @param context Context instance.
     * @param date    Date containing month.
     * @return Formatted string.
     */
    public static String formatMonth(Context context, Calendar date) {
        // Load resource string for month.
        switch (date.get(Calendar.MONTH)) {
            case 0:
                return context.getString(R.string.january_full);
            case 1:
                return context.getString(R.string.february_full);
            case 2:
                return context.getString(R.string.march_full);
            case 3:
                return context.getString(R.string.april_full);
            case 4:
                return context.getString(R.string.may_full);
            case 5:
                return context.getString(R.string.june_full);
            case 6:
                return context.getString(R.string.july_full);
            case 7:
                return context.getString(R.string.august_full);
            case 8:
                return context.getString(R.string.september_full);
            case 9:
                return context.getString(R.string.october_full);
            case 10:
                return context.getString(R.string.november_full);
            case 11:
                return context.getString(R.string.december_full);
            default:
                return "";
        }
    }

    /**
     * Formats day of the month to a friendly name, with suffix (st, nd, rd, th).
     *
     * @param context Context instance.
     * @param date    Date containing day of the month.
     * @return Formatted string.
     */
    public static String formatDayOfMonth(Context context, Calendar date) {
        int dayOfMonth = date.get(Calendar.DAY_OF_MONTH);

        // Load resource string for day of month with suffix.
        switch (dayOfMonth) {
            case 1:
                return context.getString(R.string.month_first_suffix, dayOfMonth);
            case 2:
                return context.getString(R.string.month_second_suffix, dayOfMonth);
            case 3:
                return context.getString(R.string.month_third_suffix, dayOfMonth);
            case 21:
                return context.getString(R.string.month_first_suffix, dayOfMonth);
            case 22:
                return context.getString(R.string.month_second_suffix, dayOfMonth);
            case 23:
                return context.getString(R.string.month_third_suffix, dayOfMonth);
            case 31:
                return context.getString(R.string.month_first_suffix, dayOfMonth);
            default:
                return context.getString(R.string.month_default_suffix, dayOfMonth);
        }
    }

    /**
     * Returns a date string formatted for syncing.
     *
     * @param date Date to format.
     * @return Formatted string.
     */
    public static String dateToSync(Date date) {
        SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT_SYNC);
        dateFormat.setTimeZone(TimeZone.getTimeZone(TIMEZONE_SYNC));

        return date != null ? dateFormat.format(date) : "null";
    }

    /**
     * Returns a date from a sync date string.
     *
     * @param date Sync date to format.
     * @return Date to use locally.
     */
    public static Date dateFromSync(String date) {
        SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT_SYNC);
        dateFormat.setTimeZone(TimeZone.getTimeZone(TIMEZONE_SYNC));

        try {
            return date != null && !date.equals("null") ? dateFormat.parse(date) : null;
        } catch (ParseException e) {
            Log.e(LOG_TAG, e.getMessage(), e);
        }

        return null;
    }

    /**
     * Returns a Date object from a time in milliseconds.
     *
     * @param dateInMillis Time in millis.
     * @return Date object.
     */
    public static Date dateFromMillis(long dateInMillis) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(dateInMillis);

        return calendar.getTime();
    }

}
