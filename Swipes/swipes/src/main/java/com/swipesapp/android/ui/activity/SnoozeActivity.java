package com.swipesapp.android.ui.activity;

import android.annotation.TargetApi;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.text.format.DateFormat;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.doomonafireball.betterpickers.calendardatepicker.CalendarDatePickerDialog;
import com.doomonafireball.betterpickers.radialtimepicker.RadialTimePickerDialog;
import com.swipesapp.android.BuildConfig;
import com.swipesapp.android.R;
import com.swipesapp.android.analytics.handler.Analytics;
import com.swipesapp.android.analytics.handler.IntercomHandler;
import com.swipesapp.android.analytics.values.Actions;
import com.swipesapp.android.analytics.values.Categories;
import com.swipesapp.android.analytics.values.IntercomEvents;
import com.swipesapp.android.analytics.values.IntercomFields;
import com.swipesapp.android.analytics.values.Labels;
import com.swipesapp.android.app.SwipesApplication;
import com.swipesapp.android.handler.SoundHandler;
import com.swipesapp.android.sync.gson.GsonTask;
import com.swipesapp.android.sync.service.TasksService;
import com.swipesapp.android.ui.view.SwipesTextView;
import com.swipesapp.android.ui.view.TimePreference;
import com.swipesapp.android.util.DateUtils;
import com.swipesapp.android.util.PreferenceUtils;
import com.swipesapp.android.util.ThemeUtils;
import com.swipesapp.android.values.Constants;
import com.swipesapp.android.values.Sections;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import butterknife.OnLongClick;

public class SnoozeActivity extends FragmentActivity {

    @InjectView(R.id.snooze_view)
    LinearLayout mView;

    // Later today.
    @InjectView(R.id.snooze_later_today_icon)
    SwipesTextView mLaterTodayIcon;
    @InjectView(R.id.snooze_later_today_title)
    TextView mLaterTodayTitle;

    // This evening.
    @InjectView(R.id.snooze_this_evening_icon)
    SwipesTextView mThisEveningIcon;
    @InjectView(R.id.snooze_this_evening_title)
    TextView mThisEveningTitle;

    // Tomorrow.
    @InjectView(R.id.snooze_tomorrow_icon)
    SwipesTextView mTomorrowIcon;
    @InjectView(R.id.snooze_tomorrow_title)
    TextView mTomorrowTitle;

    // Two days.
    @InjectView(R.id.snooze_two_days_icon)
    SwipesTextView mTwoDaysIcon;
    @InjectView(R.id.snooze_two_days_title)
    TextView mTwoDaysTitle;

    // This weekend.
    @InjectView(R.id.snooze_this_weekend_icon)
    SwipesTextView mThisWeekendIcon;
    @InjectView(R.id.snooze_this_weekend_title)
    TextView mThisWeekendTitle;

    // Next week.
    @InjectView(R.id.snooze_next_week_icon)
    SwipesTextView mNextWeekIcon;
    @InjectView(R.id.snooze_next_week_title)
    TextView mNextWeekTitle;

    // Unspecified.
    @InjectView(R.id.snooze_unspecified_icon)
    SwipesTextView mUnspecifiedIcon;
    @InjectView(R.id.snooze_unspecified_title)
    TextView mUnspecifiedTitle;

    // At location.
    @InjectView(R.id.snooze_at_location_icon)
    SwipesTextView mAtLocationIcon;
    @InjectView(R.id.snooze_at_location_title)
    TextView mAtLocationTitle;

    // Pick date.
    @InjectView(R.id.snooze_pick_date_icon)
    SwipesTextView mPickDateIcon;
    @InjectView(R.id.snooze_pick_date_title)
    TextView mPickDateTitle;

    @InjectView(R.id.snooze_adjust_hint)
    TextView mAdjustHint;

    private static final String TIME_PICKER_TAG = "SNOOZE_TIME_PICKER";
    private static final String DATE_PICKER_TAG = "SNOOZE_DATE_PICKER";

    private TasksService mTasksService;

    private GsonTask mTask;

    private int mDayStartHour;
    private int mDayStartMinute;

    private int mEveningStartHour;
    private int mEveningStartMinute;

    private int mWeekendDayStartHour;
    private int mWeekendDayStartMinute;

    private int mWeekStartDay;
    private int mWeekendStartDay;

    private int mLaterTodayDelay;

    private boolean mNewTaskMode;
    private Sections mSection;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(ThemeUtils.getDialogThemeResource(this));
        setContentView(R.layout.activity_snooze);
        ButterKnife.inject(this);

        getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        setVolumeControlStream(AudioManager.STREAM_MUSIC);

        mTasksService = TasksService.getInstance();

        Long id = getIntent().getLongExtra(Constants.EXTRA_TASK_ID, 0);
        mNewTaskMode = getIntent().getBooleanExtra(Constants.EXTRA_NEW_TASK_MODE, false);

        int sectionNumber = getIntent().getIntExtra(Constants.EXTRA_SECTION_NUMBER, -1);
        mSection = Sections.getSectionByNumber(sectionNumber);

        if (!mNewTaskMode) mTask = mTasksService.loadTask(id);

        loadPreferences();

        customizeViews();

        // Play sound.
        SoundHandler.playSound(this, R.raw.snooze_task);
    }

    @Override
    public void finish() {
        super.finish();

        overridePendingTransition(0, 0);
    }

    @Override
    protected void onStart() {
        SwipesApplication.stopBackgroundTimer();

        super.onStart();
    }

    @Override
    protected void onStop() {
        SwipesApplication.startBackgroundTimer();

        super.onStop();
    }

    private void loadPreferences() {
        String prefDayStart = PreferenceUtils.readString(PreferenceUtils.SNOOZE_DAY_START, this);
        mDayStartHour = TimePreference.getHour(prefDayStart);
        mDayStartMinute = TimePreference.getMinute(prefDayStart);

        String prefEveningStart = PreferenceUtils.readString(PreferenceUtils.SNOOZE_EVENING_START, this);
        mEveningStartHour = TimePreference.getHour(prefEveningStart);
        mEveningStartMinute = TimePreference.getMinute(prefEveningStart);

        String prefWeekendDayStart = PreferenceUtils.readString(PreferenceUtils.SNOOZE_WEEKEND_DAY_START, this);
        mWeekendDayStartHour = TimePreference.getHour(prefWeekendDayStart);
        mWeekendDayStartMinute = TimePreference.getMinute(prefWeekendDayStart);

        String prefWeekStart = PreferenceUtils.readString(PreferenceUtils.SNOOZE_WEEK_START, this);
        mWeekStartDay = DateUtils.weekdayFromPrefValue(prefWeekStart);

        String prefWeekendStart = PreferenceUtils.readString(PreferenceUtils.SNOOZE_WEEKEND_START, this);
        mWeekendStartDay = DateUtils.weekdayFromPrefValue(prefWeekendStart);

        String prefLaterToday = PreferenceUtils.readString(PreferenceUtils.SNOOZE_LATER_TODAY, this);
        mLaterTodayDelay = Integer.valueOf(prefLaterToday);
    }

    private void customizeViews() {
        mView.setBackgroundResource(ThemeUtils.getDialogBackground(this));

        if (BuildConfig.DEBUG) mAdjustHint.setVisibility(View.GONE);

        int textColor = ThemeUtils.getSecondaryTextColor(this);
        int iconColor = ThemeUtils.getTextColor(this);

        setSelector(mLaterTodayIcon);
        mLaterTodayIcon.setTextColor(iconColor);
        mLaterTodayTitle.setTextColor(textColor);

        setSelector(mThisEveningIcon);
        mThisEveningIcon.setTextColor(iconColor);
        mThisEveningTitle.setTextColor(textColor);

        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        if (hour >= 19 && hour <= 23) {
            mThisEveningTitle.setText(getString(R.string.snooze_tomorrow_eve));
        }

        setSelector(mTomorrowIcon);
        mTomorrowIcon.setTextColor(iconColor);
        mTomorrowTitle.setTextColor(textColor);

        setSelector(mTwoDaysIcon);
        mTwoDaysIcon.setTextColor(iconColor);
        mTwoDaysTitle.setTextColor(textColor);
        mTwoDaysTitle.setText(getTwoDaysTitle());

        setSelector(mThisWeekendIcon);
        mThisWeekendIcon.setTextColor(iconColor);
        mThisWeekendTitle.setTextColor(textColor);

        setSelector(mNextWeekIcon);
        mNextWeekIcon.setTextColor(iconColor);
        mNextWeekTitle.setTextColor(textColor);

        setSelector(mUnspecifiedIcon);
        mUnspecifiedIcon.setTextColor(iconColor);
        mUnspecifiedTitle.setTextColor(textColor);

        setSelector(mAtLocationIcon);
        mAtLocationIcon.setTextColor(iconColor);
        mAtLocationTitle.setTextColor(textColor);

        setSelector(mPickDateIcon);
        mPickDateIcon.setTextColor(iconColor);
        mPickDateTitle.setTextColor(textColor);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public void setSelector(SwipesTextView icon) {
        // Use borderless ripple on Lollipop.
        int resource = Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP ?
                android.R.attr.selectableItemBackgroundBorderless : android.R.attr.selectableItemBackground;

        TypedValue outValue = new TypedValue();
        getTheme().resolveAttribute(resource, outValue, true);
        ((View) icon.getParent()).setBackgroundResource(outValue.resourceId);
    }

    @OnClick(R.id.snooze_main_layout)
    protected void cancel() {
        finish();
    }

    @OnClick(R.id.snooze_view)
    protected void ignore() {
        // Do nothing.
    }

    @OnClick(R.id.snooze_later_today)
    protected void laterToday() {
        // Set snooze time.
        Calendar snooze = getBaseCalendar();
        int laterToday = snooze.get(Calendar.HOUR_OF_DAY) + mLaterTodayDelay;
        snooze.set(Calendar.HOUR_OF_DAY, laterToday);

        roundMinutes(snooze);

        applyNextDayTreatment(snooze);

        sendSnoozedEvent(Labels.SNOOZED_LATER_TODAY, snooze.getTime(), false);

        performChanges(snooze.getTime());
    }

    @OnLongClick(R.id.snooze_later_today)
    protected boolean laterTodayAdjust() {
        // Set snooze time.
        Calendar snooze = getBaseCalendar();
        roundMinutes(snooze);

        int laterToday = snooze.get(Calendar.HOUR_OF_DAY) + mLaterTodayDelay;
        int currentMinute = snooze.get(Calendar.MINUTE);

        // Show time picker.
        adjustSnoozeTime(snooze, laterToday, currentMinute, Labels.SNOOZED_LATER_TODAY);

        return true;
    }

    @OnClick(R.id.snooze_this_evening)
    protected void thisEvening() {
        // Set snooze time.
        Calendar snooze = getBaseCalendar();
        snooze.set(Calendar.HOUR_OF_DAY, mEveningStartHour);
        snooze.set(Calendar.MINUTE, mEveningStartMinute);

        applyNextDayTreatment(snooze);

        sendSnoozedEvent(Labels.SNOOZED_THIS_EVENING, snooze.getTime(), false);

        performChanges(snooze.getTime());
    }

    @OnLongClick(R.id.snooze_this_evening)
    protected boolean thisEveningAdjust() {
        // Set snooze time.
        Calendar snooze = getBaseCalendar();

        // Show time picker.
        adjustSnoozeTime(snooze, mEveningStartHour, mEveningStartMinute, Labels.SNOOZED_THIS_EVENING);

        return true;
    }

    @OnClick(R.id.snooze_tomorrow)
    protected void tomorrow() {
        // Set snooze time.
        Calendar snooze = getBaseCalendar();
        snooze.setTimeInMillis(snooze.getTimeInMillis() + 86400000L);
        snooze.set(Calendar.HOUR_OF_DAY, mDayStartHour);
        snooze.set(Calendar.MINUTE, mDayStartMinute);

        sendSnoozedEvent(Labels.SNOOZED_TOMORROW, snooze.getTime(), false);

        performChanges(snooze.getTime());
    }

    @OnLongClick(R.id.snooze_tomorrow)
    protected boolean tomorrowAdjust() {
        // Set snooze time.
        Calendar snooze = getBaseCalendar();
        snooze.setTimeInMillis(snooze.getTimeInMillis() + 86400000L);

        // Show time picker.
        adjustSnoozeTime(snooze, mDayStartHour, mDayStartMinute, Labels.SNOOZED_TOMORROW);

        return true;
    }

    @OnClick(R.id.snooze_two_days)
    protected void twoDays() {
        // Set snooze time.
        Calendar snooze = getBaseCalendar();
        snooze.setTimeInMillis(snooze.getTimeInMillis() + 172800000L);
        snooze.set(Calendar.HOUR_OF_DAY, mDayStartHour);
        snooze.set(Calendar.MINUTE, mDayStartMinute);

        sendSnoozedEvent(Labels.SNOOZED_TWO_DAYS, snooze.getTime(), false);

        performChanges(snooze.getTime());
    }

    @OnLongClick(R.id.snooze_two_days)
    protected boolean twoDaysAdjust() {
        // Set snooze time.
        Calendar snooze = getBaseCalendar();
        snooze.setTimeInMillis(snooze.getTimeInMillis() + 172800000L);

        // Show time picker.
        adjustSnoozeTime(snooze, mDayStartHour, mDayStartMinute, Labels.SNOOZED_TWO_DAYS);

        return true;
    }

    @OnClick(R.id.snooze_this_weekend)
    protected void thisWeekend() {
        // Set snooze time.
        Calendar snooze = getBaseCalendar();
        snooze.set(Calendar.DAY_OF_WEEK, mWeekendStartDay);
        snooze.set(Calendar.HOUR_OF_DAY, mWeekendDayStartHour);
        snooze.set(Calendar.MINUTE, mWeekendDayStartMinute);

        applyNextWeekTreatment(snooze);

        sendSnoozedEvent(Labels.SNOOZED_THIS_WEEKEND, snooze.getTime(), false);

        performChanges(snooze.getTime());
    }

    @OnLongClick(R.id.snooze_this_weekend)
    protected boolean thisWeekendAdjust() {
        // Set snooze time.
        Calendar snooze = getBaseCalendar();
        snooze.set(Calendar.DAY_OF_WEEK, mWeekendStartDay);

        applyNextWeekTreatment(snooze);

        // Show time picker.
        adjustSnoozeTime(snooze, mWeekendDayStartHour, mWeekendDayStartMinute, Labels.SNOOZED_THIS_WEEKEND);

        return true;
    }

    @OnClick(R.id.snooze_next_week)
    protected void nextWeek() {
        // Set snooze time.
        Calendar snooze = getBaseCalendar();
        snooze.set(Calendar.DAY_OF_WEEK, mWeekStartDay);
        snooze.set(Calendar.HOUR_OF_DAY, mDayStartHour);
        snooze.set(Calendar.MINUTE, mDayStartMinute);

        applyNextWeekTreatment(snooze);

        sendSnoozedEvent(Labels.SNOOZED_NEXT_WEEK, snooze.getTime(), false);

        performChanges(snooze.getTime());
    }

    @OnLongClick(R.id.snooze_next_week)
    protected boolean nextWeekAdjust() {
        // Set snooze time.
        Calendar snooze = getBaseCalendar();
        snooze.set(Calendar.DAY_OF_WEEK, mWeekStartDay);

        applyNextWeekTreatment(snooze);

        // Show time picker.
        adjustSnoozeTime(snooze, mDayStartHour, mDayStartMinute, Labels.SNOOZED_NEXT_WEEK);

        return true;
    }

    @OnClick(R.id.snooze_unspecified)
    protected void unspecified() {
        // Send analytics event.
        sendSnoozedEvent(Labels.SNOOZED_UNSPECIFIED, null, false);

        // Set date as unspecified.
        performChanges(null);
    }

    @OnLongClick(R.id.snooze_unspecified)
    protected boolean unspecifiedAdjust() {
        // Send analytics event.
        sendSnoozedEvent(Labels.SNOOZED_UNSPECIFIED, null, false);

        // Set date as unspecified.
        performChanges(null);
        return true;
    }

    @OnClick(R.id.snooze_at_location)
    protected void atLocation() {
        // TODO: Location reminders.
    }

    @OnLongClick(R.id.snooze_at_location)
    protected boolean atLocationAdjust() {
        // TODO: Location reminders.
        return true;
    }

    @OnClick(R.id.snooze_pick_date)
    protected void pickDate() {
        // Show date picker.
        pickSnoozeDate();
    }

    @OnLongClick(R.id.snooze_pick_date)
    protected boolean longPickDate() {
        // Show date picker.
        pickSnoozeDate();

        return true;
    }

    private void adjustSnoozeTime(final Calendar snooze, int startHour, int startMinute, final String option) {
        // Hide snooze view.
        getWindow().getDecorView().animate().alpha(0f).setDuration(Constants.ANIMATION_DURATION_MEDIUM);

        // Create time picker listener.
        RadialTimePickerDialog.OnTimeSetListener timeSetListener = new RadialTimePickerDialog.OnTimeSetListener() {
            @Override
            public void onTimeSet(RadialTimePickerDialog dialog, int hourOfDay, int minute) {
                // Set snooze time.
                snooze.set(Calendar.HOUR_OF_DAY, hourOfDay);
                snooze.set(Calendar.MINUTE, minute);

                applyNextDayTreatment(snooze);

                sendSnoozedEvent(option, snooze.getTime(), true);

                performChanges(snooze.getTime());
            }
        };

        // Create dismiss listener.
        RadialTimePickerDialog.OnDialogDismissListener dismissListener = new RadialTimePickerDialog.OnDialogDismissListener() {
            @Override
            public void onDialogDismiss(DialogInterface dialoginterface) {
                // Show snooze view.
                getWindow().getDecorView().animate().alpha(1f).setDuration(Constants.ANIMATION_DURATION_MEDIUM);
            }
        };

        // Make sure start time is valid.
        startHour = apply24HourTreatment(startHour);

        // Show time picker dialog.
        RadialTimePickerDialog dialog = new RadialTimePickerDialog();
        dialog.setStartTime(startHour, startMinute);
        dialog.setOnTimeSetListener(timeSetListener);
        dialog.setOnDismissListener(dismissListener);
        dialog.setDoneText(getString(R.string.snooze_done_text));
        dialog.setThemeDark(!ThemeUtils.isLightTheme(this));
        dialog.set24HourMode(DateFormat.is24HourFormat(this));
        dialog.show(getSupportFragmentManager(), TIME_PICKER_TAG);

        // Mark schedule as not performed.
        setResult(RESULT_CANCELED);
    }

    private void pickSnoozeDate() {
        // Set snooze time.
        final Calendar snooze = getBaseCalendar();
        snooze.set(Calendar.HOUR_OF_DAY, 9);
        snooze.set(Calendar.MINUTE, 0);

        // Set time for already scheduled task.
        if (!mNewTaskMode) {
            Date schedule = mTask.getLocalSchedule();
            if (DateUtils.isNewerThanToday(schedule)) {
                snooze.setTime(schedule);
            }
        }

        // Hide snooze view.
        mView.animate().alpha(0f).setDuration(Constants.ANIMATION_DURATION_MEDIUM);

        // Create date picker listener.
        CalendarDatePickerDialog.OnDateSetListener dateSetListener = new CalendarDatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(CalendarDatePickerDialog dialog, int year, int monthOfYear, int dayOfMonth, boolean adjustTime) {
                // Set snooze date.
                snooze.set(Calendar.YEAR, year);
                snooze.set(Calendar.MONTH, monthOfYear);
                snooze.set(Calendar.DAY_OF_MONTH, dayOfMonth);

                if (adjustTime) {
                    // Call time picker.
                    adjustSnoozeTime(snooze, mDayStartHour, mDayStartMinute, Labels.SNOOZED_PICK_DATE);
                } else {
                    // Snooze task.
                    sendSnoozedEvent(Labels.SNOOZED_PICK_DATE, snooze.getTime(), false);
                    performChanges(snooze.getTime());
                }
            }
        };

        // Create dismiss listener.
        CalendarDatePickerDialog.OnDialogDismissListener dismissListener = new CalendarDatePickerDialog.OnDialogDismissListener() {
            @Override
            public void onDialogDismiss(DialogInterface dialoginterface) {
                // Show snooze view.
                mView.animate().alpha(1f).setDuration(Constants.ANIMATION_DURATION_MEDIUM);
            }
        };

        // Show date picker dialog.
        CalendarDatePickerDialog dialog = new CalendarDatePickerDialog();
        dialog.setOnDateSetListener(dateSetListener);
        dialog.setOnDismissListener(dismissListener);
        dialog.setDoneText(getString(R.string.snooze_done_text));
        dialog.setThemeDark(!ThemeUtils.isLightTheme(this));
        dialog.setStartDate(snooze);
        dialog.show(getSupportFragmentManager(), DATE_PICKER_TAG);

        // Mark schedule as not performed.
        setResult(RESULT_CANCELED);
    }

    private void performChanges(Date schedule) {
        if (mNewTaskMode) {
            // Add snooze time to intent as string.
            String date = DateUtils.dateToSync(schedule);
            Intent data = new Intent();
            data.putExtra(Constants.EXTRA_SNOOZE_TIME, date);

            // Mark schedule as performed and return time.
            setResult(RESULT_OK, data);
        } else {
            // Perform task changes.
            mTask.setLocalSchedule(schedule);
            mTask.setLocalCompletionDate(null);
            mTasksService.saveTask(mTask, true);

            // Mark schedule as performed.
            setResult(RESULT_OK);
        }

        // Play sound.
        if (mSection != Sections.FOCUS || mTasksService.countTasksForNow() > 0) {
            SoundHandler.playSound(this, R.raw.snooze_task);
        }

        finish();
    }

    private void sendSnoozedEvent(String option, Date schedule, boolean timePicker) {
        Date currentSchedule = mNewTaskMode ? new Date() : mTask.getLocalSchedule();
        Long daysAhead = null;
        String usedPicker = timePicker ? Labels.VALUE_YES : Labels.VALUE_NO;

        if (currentSchedule != null && schedule != null) {
            daysAhead = (long) DateUtils.getDateDifference(currentSchedule, schedule, TimeUnit.DAYS);
        }

        // Send analytics event.
        Analytics.sendEvent(Categories.TASKS, Actions.SNOOZED_TASK, option, daysAhead);

        // Prepare Intercom fields.
        HashMap<String, Object> fields = new HashMap<>();
        fields.put(IntercomFields.FROM, option);
        fields.put(IntercomFields.TIME_PICKER, usedPicker);

        if (daysAhead != null) fields.put(IntercomFields.DAYS_AHEAD, daysAhead);

        // Send Intercom events.
        IntercomHandler.sendEvent(IntercomEvents.SNOOZED_TASKS, fields);
    }

    public static void applyNextDayTreatment(Calendar snooze) {
        // Check if the selected time should be in the next day.
        if (snooze.before(Calendar.getInstance())) {
            // Add a day to the snooze time.
            snooze.setTimeInMillis(snooze.getTimeInMillis() + 86400000L);
        }
    }

    private void applyNextWeekTreatment(Calendar snooze) {
        Calendar today = Calendar.getInstance();

        // Check if the selected time should be in the next week.
        if (snooze.before(today) || snooze.get(Calendar.DAY_OF_WEEK) == today.get(Calendar.DAY_OF_WEEK)) {
            // Add a week to the snooze time.
            snooze.setTimeInMillis(snooze.getTimeInMillis() + 604800000L);
        }
    }

    private int apply24HourTreatment(int hourOfDay) {
        // Check if hour is above the 24 limit.
        if (hourOfDay >= 24) {
            // Reduce hour to the proper time in the morning. Time is still today, but
            // the next day treatment will move it to tomorrow when called.
            hourOfDay = hourOfDay - 24;
        }
        return hourOfDay;
    }

    private String getTwoDaysTitle() {
        // Load day of the week two days from now.
        Calendar now = Calendar.getInstance();
        int day = now.get(Calendar.DAY_OF_WEEK) + 2;
        now.set(Calendar.DAY_OF_WEEK, day);

        // Return friendly name for day of the week.
        return DateUtils.formatDayOfWeek(this, now);
    }

    public static void roundMinutes(Calendar snooze) {
        int minutes = snooze.get(Calendar.MINUTE);

        // Calculate round to quarter hour.
        int mod = minutes % 15;
        int add = mod < 8 ? -mod : 15 - mod;

        snooze.add(Calendar.MINUTE, add);
    }

    public static Calendar getBaseCalendar() {
        // Create calendar with seconds reset.
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        return calendar;
    }

}
