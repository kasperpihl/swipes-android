package com.swipesapp.android.ui.activity;

import android.app.TimePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.negusoft.holoaccent.activity.AccentActivity;
import com.negusoft.holoaccent.dialog.AccentAlertDialog;
import com.negusoft.holoaccent.dialog.AccentTimePickerDialog;
import com.swipesapp.android.R;
import com.swipesapp.android.sync.gson.GsonTask;
import com.swipesapp.android.sync.service.TasksService;
import com.swipesapp.android.ui.view.SwipesTextView;
import com.swipesapp.android.util.Constants;
import com.swipesapp.android.util.DateUtils;
import com.swipesapp.android.util.ThemeUtils;

import java.text.ParseException;
import java.util.Arrays;
import java.util.Calendar;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;

public class EditTaskActivity extends AccentActivity {

    @InjectView(R.id.edit_task_view)
    LinearLayout mView;

    @InjectView(R.id.button_edit_task_priority)
    CheckBox mPriority;
    @InjectView(R.id.edit_task_title)
    EditText mTitle;

    @InjectView(R.id.schedule_container)
    RelativeLayout mScheduleContainer;
    @InjectView(R.id.edit_task_schedule_icon)
    SwipesTextView mScheduleIcon;
    @InjectView(R.id.edit_task_schedule)
    TextView mSchedule;

    @InjectView(R.id.repeat_container)
    RelativeLayout mRepeatContainer;
    @InjectView(R.id.edit_task_repeat_icon)
    SwipesTextView mRepeatIcon;
    @InjectView(R.id.edit_task_repeat)
    TextView mRepeat;

    @InjectView(R.id.tags_container)
    RelativeLayout mTagsContainer;
    @InjectView(R.id.edit_task_tags_icon)
    SwipesTextView mTagsIcon;
    @InjectView(R.id.edit_task_tags)
    TextView mTags;

    @InjectView(R.id.notes_container)
    RelativeLayout mNotesContainer;
    @InjectView(R.id.edit_task_notes_icon)
    SwipesTextView mNotesIcon;
    @InjectView(R.id.edit_task_notes)
    TextView mNotes;

    private static final String LOG_TAG = EditTaskActivity.class.getSimpleName();

    private TasksService mTasksService;

    private String mTempId;

    private GsonTask mTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(ThemeUtils.getCurrentThemeResource(this));
        setContentView(R.layout.activity_edit_task);
        ButterKnife.inject(this);

        getActionBar().setDisplayShowTitleEnabled(false);

        mTasksService = TasksService.getInstance(this);

        mTempId = getIntent().getStringExtra(Constants.EXTRA_TASK_TEMP_ID);

        mView.setBackgroundColor(ThemeUtils.getCurrentThemeBackgroundColor(this));
        mView.requestFocus();

        mTitle.setTextColor(ThemeUtils.getCurrentThemeTextColor(this));
        mTitle.setOnEditorActionListener(mEnterListener);

        mScheduleIcon.setTextColor(ThemeUtils.getCurrentThemeTextColor(this));
        mSchedule.setTextColor(ThemeUtils.getCurrentThemeTextColor(this));

        mRepeatIcon.setTextColor(ThemeUtils.getCurrentThemeTextColor(this));
        mRepeat.setTextColor(ThemeUtils.getCurrentThemeTextColor(this));

        mTagsIcon.setTextColor(ThemeUtils.getCurrentThemeTextColor(this));
        mTags.setTextColor(ThemeUtils.getCurrentThemeTextColor(this));

        mNotesIcon.setTextColor(ThemeUtils.getCurrentThemeTextColor(this));
        mNotes.setTextColor(ThemeUtils.getCurrentThemeTextColor(this));

        updateViews();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // TODO: Set icons for current theme.
        menu.add(Menu.NONE, 0, Menu.NONE, "Share").setIcon(android.R.drawable.ic_menu_share).setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        menu.add(Menu.NONE, 1, Menu.NONE, "Delete").setIcon(android.R.drawable.ic_delete).setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case 0:
                // TODO: Change this when share is working.
                Toast.makeText(getApplicationContext(), "Share coming soon", Toast.LENGTH_SHORT).show();
                break;
            case 1:
                deleteTask();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void updateViews() {
        mTask = mTasksService.loadTask(mTempId);

        mTitle.setText(mTask.getTitle());

        mPriority.setChecked(mTask.getPriority() == 1);

        String scheduleDate = "";
        try {
            scheduleDate = DateUtils.formatToTodayOrTomorrow(mTask.getSchedule(), this);
        } catch (ParseException e) {
            Log.e(LOG_TAG, "Error parsing date", e);
        }

        mSchedule.setText(scheduleDate);

        mRepeat.setText(getString(R.string.edit_task_repeat_default_mode));

        mTags.setText(getString(R.string.edit_task_tags_default_text));

        mNotes.setText(getString(R.string.edit_task_notes_default_text));
    }

    private TextView.OnEditorActionListener mEnterListener =
            new TextView.OnEditorActionListener() {
                @Override
                public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                    if (actionId == EditorInfo.IME_ACTION_DONE) {
                        // If the action is a key-up event on the return key, save task changes.
                        mTask.setTitle(v.getText().toString());
                        mTasksService.saveTask(mTask);

                        hideKeyboard();

                        updateViews();
                    }
                    return true;
                }
            };

    private void hideKeyboard() {
        InputMethodManager inputManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        inputManager.toggleSoftInput(0, 0);

        // Remove focus from text views by focusing on main layout.
        mView.requestFocus();
    }

    @OnClick(R.id.button_edit_task_priority)
    protected void setPriority() {
        Integer priority = mPriority.isChecked() ? 1 : 0;
        mTask.setPriority(priority);
        mTasksService.saveTask(mTask);

        updateViews();
    }

    @OnClick(R.id.schedule_container)
    protected void setSchedule() {
        fakeSnoozeTask();
    }

    @OnClick(R.id.repeat_container)
    protected void setRepeat() {
        Toast.makeText(getApplicationContext(), "Repeat coming soon", Toast.LENGTH_SHORT).show();
    }

    @OnClick(R.id.tags_container)
    protected void setTags() {
        Toast.makeText(getApplicationContext(), "Tags coming soon", Toast.LENGTH_SHORT).show();
    }

    @OnClick(R.id.notes_container)
    protected void setNotes() {
        Toast.makeText(getApplicationContext(), "Notes coming soon", Toast.LENGTH_SHORT).show();
    }

    private void deleteTask() {
        // Display confirmation dialog.
        new AccentAlertDialog.Builder(this)
                .setTitle(getResources().getQuantityString(R.plurals.delete_task_dialog_title, 1, 1))
                .setMessage(getResources().getQuantityString(R.plurals.delete_task_dialog_text, 1))
                .setPositiveButton(getString(R.string.dialog_yes), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // Proceed with delete.
                        mTasksService.deleteTasks(Arrays.asList(mTask));
                        // Close activity.
                        finish();
                    }
                })
                .setNegativeButton(getString(R.string.dialog_no), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // Do nothing.
                    }
                })
                .create()
                .show();
    }

    private void fakeSnoozeTask() {
        // Create time picker listener.
        TimePickerDialog.OnTimeSetListener timeSetListener = new TimePickerDialog.OnTimeSetListener() {
            // HACK: There is a bug on Android that makes onTimeSet() be called twice, and also
            // be called when dismissing the dialog. This call count is used to prevent that.
            int callCount = 0;

            @Override
            public void onTimeSet(android.widget.TimePicker timePicker, int i, int i1) {
                // Refer to the "HACK" note above. Avoid execution when call count is zero.
                if (callCount == 1) {
                    // Set snooze date.
                    Calendar snooze = Calendar.getInstance();
                    snooze.set(Calendar.HOUR_OF_DAY, timePicker.getCurrentHour());
                    snooze.set(Calendar.MINUTE, timePicker.getCurrentMinute());

                    // Check if the selected time should be in the next day.
                    if (snooze.before(Calendar.getInstance())) {
                        // Add a day to the snooze time.
                        snooze.setTimeInMillis(snooze.getTimeInMillis() + 86400000L);
                    }

                    // Save task changes.
                    mTask.setSchedule(snooze.getTime());
                    mTask.setCompletionDate(null);
                    mTasksService.saveTask(mTask);

                    updateViews();
                }
                callCount++;
            }
        };

        // Get current hour and minutes.
        Calendar calendar = Calendar.getInstance();
        final int currentHour = calendar.get(Calendar.HOUR_OF_DAY);
        final int currentMinute = calendar.get(Calendar.MINUTE);
        final int laterToday = currentHour + 3;

        // Show time picker dialog.
        AccentTimePickerDialog dialog = new AccentTimePickerDialog(this, timeSetListener, laterToday, currentMinute, DateFormat.is24HourFormat(this));
        dialog.setTitle("Snooze until");
        dialog.show();
    }

}
