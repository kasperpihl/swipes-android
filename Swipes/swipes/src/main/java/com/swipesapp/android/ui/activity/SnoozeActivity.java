package com.swipesapp.android.ui.activity;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.doomonafireball.betterpickers.radialtimepicker.RadialTimePickerDialog;
import com.swipesapp.android.R;
import com.swipesapp.android.sync.gson.GsonTask;
import com.swipesapp.android.sync.service.TasksService;
import com.swipesapp.android.ui.view.SwipesTextView;
import com.swipesapp.android.util.Constants;
import com.swipesapp.android.util.ThemeUtils;
import com.swipesapp.android.values.Sections;
import com.swipesapp.android.values.Themes;

import java.util.Calendar;

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

    private static final String TIME_PICKER_TAG = "SNOOZE_TIME_PICKER";

    private TasksService mTasksService;

    private String mTempId;

    private GsonTask mTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(ThemeUtils.getCurrentDialogThemeResource(this));
        setContentView(R.layout.activity_snooze);
        ButterKnife.inject(this);

        getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        mTasksService = TasksService.getInstance(this);

        mTempId = getIntent().getStringExtra(Constants.EXTRA_TASK_TEMP_ID);

        mTask = mTasksService.loadTask(mTempId);

        customizeViews();
    }

    private void customizeViews() {
        mView.setBackgroundColor(ThemeUtils.getCurrentThemeBackgroundColor(this));

        setSelector(mLaterTodayIcon, R.string.schedule_coffee, R.string.schedule_coffee_full);
        mLaterTodayIcon.setTextColor(ThemeUtils.getSectionColor(Sections.LATER, this));
        mLaterTodayTitle.setTextColor(ThemeUtils.getCurrentThemeTextColor(this));

        setSelector(mThisEveningIcon, R.string.schedule_moon, R.string.schedule_moon_full);
        mThisEveningIcon.setTextColor(ThemeUtils.getSectionColor(Sections.LATER, this));
        mThisEveningTitle.setTextColor(ThemeUtils.getCurrentThemeTextColor(this));

        setSelector(mTomorrowIcon, R.string.schedule_sun, R.string.schedule_sun_full);
        mTomorrowIcon.setTextColor(ThemeUtils.getSectionColor(Sections.LATER, this));
        mTomorrowTitle.setTextColor(ThemeUtils.getCurrentThemeTextColor(this));

        setSelector(mTwoDaysIcon, R.string.schedule_logbook, R.string.schedule_logbook_full);
        mTwoDaysIcon.setTextColor(ThemeUtils.getSectionColor(Sections.LATER, this));
        mTwoDaysTitle.setTextColor(ThemeUtils.getCurrentThemeTextColor(this));

        setSelector(mThisWeekendIcon, R.string.schedule_glass, R.string.schedule_glass_full);
        mThisWeekendIcon.setTextColor(ThemeUtils.getSectionColor(Sections.LATER, this));
        mThisWeekendTitle.setTextColor(ThemeUtils.getCurrentThemeTextColor(this));

        setSelector(mNextWeekIcon, R.string.schedule_circle, R.string.schedule_circle_full);
        mNextWeekIcon.setTextColor(ThemeUtils.getSectionColor(Sections.LATER, this));
        mNextWeekTitle.setTextColor(ThemeUtils.getCurrentThemeTextColor(this));

//        setSelector(mUnspecifiedIcon, R.string.schedule_cloud, R.string.schedule_cloud_full);
//        mUnspecifiedIcon.setTextColor(ThemeUtils.getSectionColor(Sections.LATER, this));
//        mUnspecifiedTitle.setTextColor(ThemeUtils.getCurrentThemeTextColor(this));

        setSelector(mAtLocationIcon, R.string.schedule_location, R.string.schedule_location_full);
        mAtLocationIcon.setTextColor(ThemeUtils.getSectionColor(Sections.LATER, this));
        mAtLocationTitle.setTextColor(ThemeUtils.getCurrentThemeTextColor(this));

        setSelector(mPickDateIcon, R.string.schedule_calendar, R.string.schedule_calendar_full);
        mPickDateIcon.setTextColor(ThemeUtils.getSectionColor(Sections.LATER, this));
        mPickDateTitle.setTextColor(ThemeUtils.getCurrentThemeTextColor(this));
    }

    public void setSelector(final SwipesTextView icon, final int resource, final int resourceFull) {
        // Create selector based on touch state.
        ((View) icon.getParent()).setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                switch (motionEvent.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        // Change resource to pressed state.
                        icon.setText(getString(resourceFull));
                        break;
                    case MotionEvent.ACTION_UP:
                        // Change resource to default state.
                        icon.setText(getString(resource));
                        break;
                }
                return false;
            }
        });
    }

    @OnClick(R.id.snooze_later_today)
    protected void laterToday() {
        // Set snooze time.
        Calendar snooze = Calendar.getInstance();
        final int laterToday = snooze.get(Calendar.HOUR_OF_DAY) + 3;
        snooze.set(Calendar.HOUR_OF_DAY, laterToday);

        applyNextDayTreatment(snooze);

        performChanges(snooze);
    }

    @OnLongClick(R.id.snooze_later_today)
    protected boolean laterTodayAdjust() {
        fakeSnoozeTask();
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
        fakeSnoozeTask();
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
        fakeSnoozeTask();
        return true;
    }

    @OnClick(R.id.snooze_two_days)
    protected void twoDays() {
        Toast.makeText(getApplicationContext(), "Snooze option coming soon", Toast.LENGTH_SHORT).show();
    }

    @OnLongClick(R.id.snooze_two_days)
    protected boolean twoDaysAdjust() {
        Toast.makeText(getApplicationContext(), "Snooze option coming soon", Toast.LENGTH_SHORT).show();
        return true;
    }

    @OnClick(R.id.snooze_this_weekend)
    protected void thisWeekend() {
        Toast.makeText(getApplicationContext(), "Snooze option coming soon", Toast.LENGTH_SHORT).show();
    }

    @OnLongClick(R.id.snooze_this_weekend)
    protected boolean thisWeekendAdjust() {
        Toast.makeText(getApplicationContext(), "Snooze option coming soon", Toast.LENGTH_SHORT).show();
        return true;
    }

    @OnClick(R.id.snooze_next_week)
    protected void nextWeek() {
        Toast.makeText(getApplicationContext(), "Snooze option coming soon", Toast.LENGTH_SHORT).show();
    }

    @OnLongClick(R.id.snooze_next_week)
    protected boolean nextWeekAdjust() {
        Toast.makeText(getApplicationContext(), "Snooze option coming soon", Toast.LENGTH_SHORT).show();
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
        Toast.makeText(getApplicationContext(), "Snooze option coming soon", Toast.LENGTH_SHORT).show();
    }

    @OnLongClick(R.id.snooze_pick_date)
    protected boolean pickDateAdjust() {
        Toast.makeText(getApplicationContext(), "Snooze option coming soon", Toast.LENGTH_SHORT).show();
        return true;
    }

    private void fakeSnoozeTask() {
        // Create time picker listener.
        RadialTimePickerDialog.OnTimeSetListener timeSetListener = new RadialTimePickerDialog.OnTimeSetListener() {
            @Override
            public void onTimeSet(RadialTimePickerDialog dialog, int hourOfDay, int minute) {
                // Set snooze date.
                Calendar snooze = Calendar.getInstance();
                snooze.set(Calendar.HOUR_OF_DAY, hourOfDay);
                snooze.set(Calendar.MINUTE, minute);

                applyNextDayTreatment(snooze);

                performChanges(snooze);
            }
        };

        // Get current hour and minutes.
        Calendar calendar = Calendar.getInstance();
        final int currentHour = calendar.get(Calendar.HOUR_OF_DAY);
        final int currentMinute = calendar.get(Calendar.MINUTE);
        final int laterToday = currentHour + 3;

        // Show time picker dialog.
        RadialTimePickerDialog dialog = new RadialTimePickerDialog();
        dialog.setStartTime(laterToday, currentMinute);
        dialog.setOnTimeSetListener(timeSetListener);
        dialog.setDoneText("Snooze");
        dialog.setThemeDark(ThemeUtils.getCurrentTheme(this) != Themes.LIGHT);
        dialog.show(getSupportFragmentManager(), TIME_PICKER_TAG);

        // Mark schedule as not performed.
        setResult(RESULT_CANCELED);
    }

    private void performChanges(Calendar snooze) {
        // Perform task changes.
        mTask.setSchedule(snooze.getTime());
        mTask.setCompletionDate(null);
        mTasksService.saveTask(mTask);

        // Mark schedule as performed.
        setResult(RESULT_OK);

        finish();
    }

    private void applyNextDayTreatment(Calendar snooze) {
        // Check if the selected time should be in the next day.
        if (snooze.before(Calendar.getInstance())) {
            // Add a day to the snooze time.
            snooze.setTimeInMillis(snooze.getTimeInMillis() + 86400000L);
        }
    }

}
