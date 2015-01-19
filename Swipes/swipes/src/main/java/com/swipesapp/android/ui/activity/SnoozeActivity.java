package com.swipesapp.android.ui.activity;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.text.format.DateFormat;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.doomonafireball.betterpickers.calendardatepicker.CalendarDatePickerDialog;
import com.doomonafireball.betterpickers.radialtimepicker.RadialTimePickerDialog;
import com.swipesapp.android.R;
import com.swipesapp.android.sync.gson.GsonTask;
import com.swipesapp.android.sync.service.TasksService;
import com.swipesapp.android.ui.view.SwipesTextView;
import com.swipesapp.android.util.Constants;
import com.swipesapp.android.util.DateUtils;
import com.swipesapp.android.util.ThemeUtils;
import com.swipesapp.android.values.Sections;
import com.swipesapp.android.values.Themes;

import java.lang.ref.WeakReference;
import java.util.Calendar;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import butterknife.OnLongClick;

public class SnoozeActivity extends FragmentActivity {

    @InjectView(R.id.snooze_main_layout)
    LinearLayout mLayout;

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
//    @InjectView(R.id.snooze_unspecified_icon)
//    SwipesTextView mUnspecifiedIcon;
//    @InjectView(R.id.snooze_unspecified_title)
//    TextView mUnspecifiedTitle;

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

    private WeakReference<Context> mContext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(ThemeUtils.getThemeResource(this));
        setContentView(R.layout.activity_snooze);
        ButterKnife.inject(this);

        mContext = new WeakReference<Context>(this);

        mTasksService = TasksService.getInstance(this);

        Long id = getIntent().getLongExtra(Constants.EXTRA_TASK_ID, 0);

        mTask = mTasksService.loadTask(id);

        customizeViews();

        blurBackground();
    }

    private void customizeViews() {
        mView.setBackgroundColor(ThemeUtils.getBackgroundColor(this));

        int hintColor = ThemeUtils.isLightTheme(this) ? R.color.light_text_hint_color : R.color.dark_text_hint_color;
        mAdjustHint.setTextColor(getResources().getColor(hintColor));

        int textColor = ThemeUtils.getTextColor(this);
        int iconColor = ThemeUtils.getSectionColor(Sections.LATER, this);

        setSelector(mLaterTodayIcon);
        mLaterTodayIcon.setTextColor(iconColor);
        mLaterTodayTitle.setTextColor(textColor);

        setSelector(mThisEveningIcon);
        mThisEveningIcon.setTextColor(iconColor);
        mThisEveningTitle.setTextColor(textColor);

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

//        setSelector(mUnspecifiedIcon);
//        mUnspecifiedIcon.setTextColor(iconColor);
//        mUnspecifiedTitle.setTextColor(textColor);

        setSelector(mAtLocationIcon);
        mAtLocationIcon.setTextColor(iconColor);
        mAtLocationTitle.setTextColor(textColor);

        setSelector(mPickDateIcon);
        mPickDateIcon.setTextColor(iconColor);
        mPickDateTitle.setTextColor(textColor);
    }

    private void blurBackground() {
        // Make activity window transparent.
        getWindow().setBackgroundDrawable(new ColorDrawable(0));

        // Wait for main layout to be drawn.
        ViewTreeObserver observer = mLayout.getViewTreeObserver();
        if (observer.isAlive()) {
            observer.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    mLayout.getViewTreeObserver().removeGlobalOnLayoutListener(this);

                    BitmapDrawable blurDrawable = null;
                    String caller = getIntent().getStringExtra(Constants.EXTRA_CALLER_NAME);

                    // Update blur based on caller.
                    if (caller.equals(Constants.CALLER_TASKS_LIST)) {
                        blurDrawable = TasksActivity.getBlurDrawable();
                    } else if (caller.equals(Constants.CALLER_EDIT_TASKS)) {
                        blurDrawable = EditTaskActivity.getBlurDrawable();
                    }

                    // Apply blurred background.
                    mLayout.setBackgroundDrawable(blurDrawable);
                }
            });
        }
    }

    public void setSelector(final SwipesTextView icon) {
        // Create selector based on touch state.
        ((View) icon.getParent()).setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                switch (motionEvent.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        // Change alpha to pressed state.
                        icon.animate().alpha(Constants.PRESSED_BUTTON_ALPHA);
                        break;
                    case MotionEvent.ACTION_UP:
                        // Change alpha to default state.
                        icon.animate().alpha(1.0f);
                        break;
                }
                return false;
            }
        });
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
        Calendar snooze = Calendar.getInstance();
        int laterToday = snooze.get(Calendar.HOUR_OF_DAY) + 3;
        snooze.set(Calendar.HOUR_OF_DAY, laterToday);

        applyNextDayTreatment(snooze);

        performChanges(snooze);
    }

    @OnLongClick(R.id.snooze_later_today)
    protected boolean laterTodayAdjust() {
        // Set snooze time.
        Calendar snooze = Calendar.getInstance();
        int laterToday = snooze.get(Calendar.HOUR_OF_DAY) + 3;
        int currentMinute = snooze.get(Calendar.MINUTE);

        // Show time picker.
        adjustSnoozeTime(snooze, laterToday, currentMinute);

        return true;
    }

    @OnClick(R.id.snooze_this_evening)
    protected void thisEvening() {
        // Set snooze time.
        Calendar snooze = Calendar.getInstance();
        snooze.set(Calendar.HOUR_OF_DAY, 19);
        snooze.set(Calendar.MINUTE, 0);

        applyNextDayTreatment(snooze);

        performChanges(snooze);
    }

    @OnLongClick(R.id.snooze_this_evening)
    protected boolean thisEveningAdjust() {
        // Set snooze time.
        Calendar snooze = Calendar.getInstance();

        // Show time picker.
        adjustSnoozeTime(snooze, 19, 0);

        return true;
    }

    @OnClick(R.id.snooze_tomorrow)
    protected void tomorrow() {
        // Set snooze time.
        Calendar snooze = Calendar.getInstance();
        snooze.setTimeInMillis(snooze.getTimeInMillis() + 86400000L);
        snooze.set(Calendar.HOUR_OF_DAY, 9);
        snooze.set(Calendar.MINUTE, 0);

        performChanges(snooze);
    }

    @OnLongClick(R.id.snooze_tomorrow)
    protected boolean tomorrowAdjust() {
        // Set snooze time.
        Calendar snooze = Calendar.getInstance();
        snooze.setTimeInMillis(snooze.getTimeInMillis() + 86400000L);

        // Show time picker.
        adjustSnoozeTime(snooze, 9, 0);

        return true;
    }

    @OnClick(R.id.snooze_two_days)
    protected void twoDays() {
        // Set snooze time.
        Calendar snooze = Calendar.getInstance();
        snooze.setTimeInMillis(snooze.getTimeInMillis() + 172800000L);
        snooze.set(Calendar.HOUR_OF_DAY, 9);
        snooze.set(Calendar.MINUTE, 0);

        performChanges(snooze);
    }

    @OnLongClick(R.id.snooze_two_days)
    protected boolean twoDaysAdjust() {
        // Set snooze time.
        Calendar snooze = Calendar.getInstance();
        snooze.setTimeInMillis(snooze.getTimeInMillis() + 172800000L);

        // Show time picker.
        adjustSnoozeTime(snooze, 9, 0);

        return true;
    }

    @OnClick(R.id.snooze_this_weekend)
    protected void thisWeekend() {
        // Set snooze time.
        Calendar snooze = Calendar.getInstance();
        snooze.set(Calendar.DAY_OF_WEEK, Calendar.SATURDAY);
        snooze.set(Calendar.HOUR_OF_DAY, 9);
        snooze.set(Calendar.MINUTE, 0);

        applyNextWeekTreatment(snooze);

        performChanges(snooze);
    }

    @OnLongClick(R.id.snooze_this_weekend)
    protected boolean thisWeekendAdjust() {
        // Set snooze time.
        Calendar snooze = Calendar.getInstance();
        snooze.set(Calendar.DAY_OF_WEEK, Calendar.SATURDAY);

        applyNextWeekTreatment(snooze);

        // Show time picker.
        adjustSnoozeTime(snooze, 9, 0);

        return true;
    }

    @OnClick(R.id.snooze_next_week)
    protected void nextWeek() {
        // Set snooze time.
        Calendar snooze = Calendar.getInstance();
        snooze.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
        snooze.set(Calendar.HOUR_OF_DAY, 9);
        snooze.set(Calendar.MINUTE, 0);

        applyNextWeekTreatment(snooze);

        performChanges(snooze);
    }

    @OnLongClick(R.id.snooze_next_week)
    protected boolean nextWeekAdjust() {
        // Set snooze time.
        Calendar snooze = Calendar.getInstance();
        snooze.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);

        applyNextWeekTreatment(snooze);

        // Show time picker.
        adjustSnoozeTime(snooze, 9, 0);

        return true;
    }

//    @OnClick(R.id.snooze_unspecified)
//    protected void unspecified() {
//        Toast.makeText(getApplicationContext(), "Snooze option coming soon", Toast.LENGTH_SHORT).show();
//    }

//    @OnLongClick(R.id.snooze_unspecified)
//    protected boolean unspecifiedAdjust() {
//        Toast.makeText(getApplicationContext(), "Snooze option coming soon", Toast.LENGTH_SHORT).show();
//        return true;
//    }

    @OnClick(R.id.snooze_at_location)
    protected void atLocation() {
        Toast.makeText(getApplicationContext(), "Location reminders coming soon", Toast.LENGTH_SHORT).show();
    }

    @OnLongClick(R.id.snooze_at_location)
    protected boolean atLocationAdjust() {
        Toast.makeText(getApplicationContext(), "Snooze option coming soon", Toast.LENGTH_SHORT).show();
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

    private void adjustSnoozeTime(final Calendar snooze, int startHour, int startMinute) {
        // Hide snooze view.
        mView.animate().alpha(0f).setDuration(Constants.ANIMATION_DURATION_MEDIUM);

        // Create time picker listener.
        RadialTimePickerDialog.OnTimeSetListener timeSetListener = new RadialTimePickerDialog.OnTimeSetListener() {
            @Override
            public void onTimeSet(RadialTimePickerDialog dialog, int hourOfDay, int minute) {
                // Set snooze time.
                snooze.set(Calendar.HOUR_OF_DAY, hourOfDay);
                snooze.set(Calendar.MINUTE, minute);

                applyNextDayTreatment(snooze);

                performChanges(snooze);
            }
        };

        // Create dismiss listener.
        RadialTimePickerDialog.OnDialogDismissListener dismissListener = new RadialTimePickerDialog.OnDialogDismissListener() {
            @Override
            public void onDialogDismiss(DialogInterface dialoginterface) {
                // Show snooze view.
                mView.animate().alpha(1f).setDuration(Constants.ANIMATION_DURATION_MEDIUM);
            }
        };

        // Show time picker dialog.
        RadialTimePickerDialog dialog = new RadialTimePickerDialog();
        dialog.setStartTime(startHour, startMinute);
        dialog.setOnTimeSetListener(timeSetListener);
        dialog.setOnDismissListener(dismissListener);
        dialog.setDoneText(getString(R.string.snooze_done_text));
        dialog.setThemeDark(ThemeUtils.getCurrentTheme(this) != Themes.LIGHT);
        dialog.set24HourMode(DateFormat.is24HourFormat(this));
        dialog.show(getSupportFragmentManager(), TIME_PICKER_TAG);

        // Mark schedule as not performed.
        setResult(RESULT_CANCELED);
    }

    private void pickSnoozeDate() {
        // Set snooze time.
        final Calendar snooze = Calendar.getInstance();
        snooze.set(Calendar.HOUR_OF_DAY, 9);
        snooze.set(Calendar.MINUTE, 0);

        // Hide snooze view.
        mView.animate().alpha(0f).setDuration(Constants.ANIMATION_DURATION_MEDIUM);

        // Create date picker listener.
        CalendarDatePickerDialog.OnDateSetListener dateSetListener = new CalendarDatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(CalendarDatePickerDialog dialog, int year, int monthOfYear, int dayOfMonth) {
                // Set snooze date.
                snooze.set(Calendar.YEAR, year);
                snooze.set(Calendar.MONTH, monthOfYear);
                snooze.set(Calendar.DAY_OF_MONTH, dayOfMonth);

                performChanges(snooze);
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
        dialog.setThemeDark(ThemeUtils.getCurrentTheme(this) != Themes.LIGHT);
        dialog.show(getSupportFragmentManager(), DATE_PICKER_TAG);

        // Mark schedule as not performed.
        setResult(RESULT_CANCELED);
    }

    private void performChanges(Calendar snooze) {
        // Perform task changes.
        mTask.setLocalSchedule(snooze.getTime());
        mTask.setLocalCompletionDate(null);
        mTasksService.saveTask(mTask, true);

        // Mark schedule as performed.
        setResult(RESULT_OK);

        finish();
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

    private String getTwoDaysTitle() {
        // Load day of the week two days from now.
        Calendar now = Calendar.getInstance();
        int day = now.get(Calendar.DAY_OF_WEEK) + 2;
        now.set(Calendar.DAY_OF_WEEK, day);

        // Return friendly name for day of the week.
        return DateUtils.formatDayOfWeek(this, now);
    }

}
