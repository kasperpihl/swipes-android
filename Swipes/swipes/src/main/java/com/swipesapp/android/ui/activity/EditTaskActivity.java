package com.swipesapp.android.ui.activity;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.negusoft.holoaccent.activity.AccentActivity;
import com.negusoft.holoaccent.dialog.AccentAlertDialog;
import com.swipesapp.android.R;
import com.swipesapp.android.sync.gson.GsonTag;
import com.swipesapp.android.sync.gson.GsonTask;
import com.swipesapp.android.sync.service.TasksService;
import com.swipesapp.android.ui.adapter.SubtasksAdapter;
import com.swipesapp.android.ui.listener.KeyboardBackListener;
import com.swipesapp.android.ui.listener.SubtaskListener;
import com.swipesapp.android.ui.view.ActionEditText;
import com.swipesapp.android.ui.view.BlurBuilder;
import com.swipesapp.android.ui.view.FlowLayout;
import com.swipesapp.android.ui.view.RepeatOption;
import com.swipesapp.android.ui.view.SwipesTextView;
import com.swipesapp.android.util.Constants;
import com.swipesapp.android.util.DateUtils;
import com.swipesapp.android.util.ThemeUtils;
import com.swipesapp.android.values.RepeatOptions;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import butterknife.OnLongClick;

public class EditTaskActivity extends AccentActivity {

    @InjectView(R.id.main_layout)
    LinearLayout mLayout;

    @InjectView(R.id.edit_task_container)
    LinearLayout mContainer;

    @InjectView(R.id.properties_view)
    LinearLayout mPropertiesView;

    @InjectView(R.id.button_edit_task_priority)
    CheckBox mPriority;
    @InjectView(R.id.edit_task_title)
    ActionEditText mTitle;

    @InjectView(R.id.edit_task_schedule_icon)
    SwipesTextView mScheduleIcon;
    @InjectView(R.id.edit_task_schedule)
    TextView mSchedule;

    @InjectView(R.id.edit_task_repeat_icon)
    SwipesTextView mRepeatIcon;
    @InjectView(R.id.edit_task_repeat)
    TextView mRepeat;
    @InjectView(R.id.repeat_options)
    LinearLayout mRepeatOptions;

    @InjectView(R.id.repeat_option_never)
    RepeatOption mRepeatNever;
    @InjectView(R.id.repeat_option_day)
    RepeatOption mRepeatDay;
    @InjectView(R.id.repeat_option_mon_fri)
    RepeatOption mRepeatMonFri;
    @InjectView(R.id.repeat_option_week)
    RepeatOption mRepeatWeek;
    @InjectView(R.id.repeat_option_month)
    RepeatOption mRepeatMonth;
    @InjectView(R.id.repeat_option_year)
    RepeatOption mRepeatYear;

    @InjectView(R.id.edit_task_tags_icon)
    SwipesTextView mTagsIcon;
    @InjectView(R.id.edit_task_tags)
    TextView mTags;

    @InjectView(R.id.edit_task_notes_icon)
    SwipesTextView mNotesIcon;
    @InjectView(R.id.edit_task_notes)
    ActionEditText mNotes;

    @InjectView(R.id.assign_tags_area)
    LinearLayout mTagsArea;

    @InjectView(R.id.assign_tags_container)
    FlowLayout mTaskTagsContainer;

    @InjectView(R.id.subtask_footer)
    LinearLayout mSubtaskFooter;

    @InjectView(R.id.subtask_add_circle_container)
    FrameLayout mSubtaskAddCircleContainer;
    @InjectView(R.id.subtask_add_circle)
    View mSubtaskAddCircle;
    @InjectView(R.id.subtask_add_title)
    ActionEditText mSubtaskAddTitle;

    @InjectView(R.id.subtask_visibility_container)
    LinearLayout mSubtaskVisibilityContainer;
    @InjectView(R.id.subtask_visibility_icon)
    SwipesTextView mSubtaskVisibilityIcon;
    @InjectView(R.id.subtask_visibility_caption)
    TextView mSubtaskVisibilityCaption;

    @InjectView(R.id.subtask_first_buttons_container)
    FrameLayout mSubtaskFirstButtonsContainer;
    @InjectView(R.id.subtask_first_item)
    RelativeLayout mSubtaskFirstItem;
    @InjectView(R.id.subtask_first_item_title)
    TextView mSubtaskFirstTitle;

    private static final String TAG_SEPARATOR = ", ";

    private WeakReference<Context> mContext;

    private TasksService mTasksService;

    private Long mId;

    private GsonTask mTask;

    private List<GsonTag> mAssignedTags;

    public static BitmapDrawable sBlurDrawable;

    private SubtasksAdapter mAdapter;
    private ListView mListView;
    private List<GsonTask> mSubtasks;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(ThemeUtils.getThemeResource(this));
        setContentView(R.layout.activity_edit_task);
        ButterKnife.inject(this);

        getActionBar().setDisplayShowTitleEnabled(false);

        mContext = new WeakReference<Context>(this);

        mTasksService = TasksService.getInstance(this);

        mId = getIntent().getLongExtra(Constants.EXTRA_TASK_ID, 0);

        mTask = mTasksService.loadTask(mId);

        setupViews();

        setupListView();

        updateViews();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // TODO: Set icons for current theme.
        menu.add(Menu.NONE, 0, Menu.NONE, getResources().getString(R.string.edit_task_share_action)).setIcon(android.R.drawable.ic_menu_share).setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        menu.add(Menu.NONE, 1, Menu.NONE, getResources().getString(R.string.edit_task_delete_action)).setIcon(android.R.drawable.ic_delete).setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case 0:
                shareTask();
                break;
            case 1:
                deleteTask();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Check if the request code the one from snooze task.
        if (requestCode == Constants.SNOOZE_REQUEST_CODE) {
            switch (resultCode) {
                case Activity.RESULT_OK:
                    // Task has been snoozed. Update views.
                    updateViews();
                    break;
            }
        }
    }

    @Override
    public void onBackPressed() {
        // Only close activity when subtasks are collapsed and tags are closed.
        if (mListView.getVisibility() == View.VISIBLE) {
            hideSubtasks();
        } else if (mTagsArea.getVisibility() == View.VISIBLE) {
            closeTags();
        } else {
            performChanges(false);
            super.onBackPressed();
        }
    }

    private void setupViews() {
        mLayout.setBackgroundColor(ThemeUtils.getBackgroundColor(this));
        mLayout.requestFocus();

        mTitle.setTextColor(ThemeUtils.getTextColor(this));
        mTitle.setOnEditorActionListener(mEnterListener);
        mTitle.setListener(mKeyboardBackListener);

        mScheduleIcon.setTextColor(ThemeUtils.getTextColor(this));
        mSchedule.setTextColor(ThemeUtils.getTextColor(this));

        mRepeatIcon.setTextColor(ThemeUtils.getTextColor(this));
        mRepeat.setTextColor(ThemeUtils.getTextColor(this));
        mRepeat.setHintTextColor(ThemeUtils.getTextColor(this));

        mTagsIcon.setTextColor(ThemeUtils.getTextColor(this));
        mTags.setTextColor(ThemeUtils.getTextColor(this));
        mTags.setHintTextColor(ThemeUtils.getTextColor(this));

        mNotesIcon.setTextColor(ThemeUtils.getTextColor(this));
        mNotes.setTextColor(ThemeUtils.getTextColor(this));
        mNotes.setHintTextColor(ThemeUtils.getTextColor(this));
        mNotes.setOnEditorActionListener(mEnterListener);
        mNotes.setListener(mKeyboardBackListener);

        int circle = ThemeUtils.isLightTheme(this) ? R.drawable.black_circle : R.drawable.white_circle;
        mSubtaskAddCircle.setBackgroundResource(circle);

        mSubtaskAddTitle.setTextColor(ThemeUtils.getTextColor(this));
        mSubtaskAddTitle.setHintTextColor(ThemeUtils.getTextColor(this));
        mSubtaskAddTitle.setOnEditorActionListener(mSubtaskEnterListener);
        mSubtaskAddTitle.setListener(mKeyboardBackListener);

        mSubtaskVisibilityIcon.setRotation(270f);
        mSubtaskVisibilityIcon.setTextColor(ThemeUtils.getTextColor(this));

        mSubtaskVisibilityCaption.setTextColor(ThemeUtils.getTextColor(this));
    }

    private void setupListView() {
        // Initialize list view.
        mListView = (ListView) findViewById(android.R.id.list);

        // Setup footer.
        LinearLayout footer = (LinearLayout) getLayoutInflater().inflate(R.layout.subtask_footer, null);
        mListView.addFooterView(footer);

        customizeFooter(footer);

        // Setup adapter.
        mSubtasks = mTasksService.loadSubtasksForTask(mTask.getTempId());
        mAdapter = new SubtasksAdapter(this, mSubtasks, mSubtaskListener);
        mListView.setAdapter(mAdapter);
    }

    private void customizeFooter(LinearLayout footer) {
        LinearLayout container = (LinearLayout) footer.findViewById(R.id.subtask_visibility_container);
        container.setOnClickListener(mHideSubtasksClick);

        FrameLayout circleContainer = (FrameLayout) footer.findViewById(R.id.subtask_add_circle_container);
        changeFooterCircleMargin(circleContainer, true);

        View addCircle = footer.findViewById(R.id.subtask_add_circle);
        int circle = ThemeUtils.isLightTheme(this) ? R.drawable.black_circle : R.drawable.white_circle;
        addCircle.setBackgroundResource(circle);

        ActionEditText addTitle = (ActionEditText) footer.findViewById(R.id.subtask_add_title);
        addTitle.setTextColor(ThemeUtils.getTextColor(this));
        addTitle.setHintTextColor(ThemeUtils.getTextColor(this));
        addTitle.setOnEditorActionListener(mSubtaskEnterListener);
        addTitle.setListener(mKeyboardBackListener);

        SwipesTextView visibilityIcon = (SwipesTextView) footer.findViewById(R.id.subtask_visibility_icon);
        visibilityIcon.setRotation(90f);
        visibilityIcon.setTextColor(ThemeUtils.getTextColor(this));

        TextView visibilityCaption = (TextView) footer.findViewById(R.id.subtask_visibility_caption);
        visibilityCaption.setVisibility(View.GONE);
    }

    private void updateViews() {
        mTask = mTasksService.loadTask(mId);

        mTitle.setText(mTask.getTitle());

        mPriority.setChecked(mTask.getPriority() == 1);

        mSchedule.setText(DateUtils.formatToRecent(mTask.getLocalSchedule(), this));

        setSelectedRepeatOption();

        setRepeatDescription();

        mTags.setText(buildFormattedTags());

        mNotes.setText(mTask.getNotes());

        mSubtaskVisibilityCaption.setText(getResources().getQuantityString(R.plurals.subtask_show_caption, mSubtasks.size(), mSubtasks.size()));

        mSubtaskVisibilityContainer.setVisibility(mSubtasks.isEmpty() ? View.GONE : View.VISIBLE);

        mSubtaskFirstItem.setAlpha(1f);

        loadFirstSubtask();
    }

    private void loadFirstSubtask() {
        boolean allCompleted = true;

        for (GsonTask subtask : mSubtasks) {
            // Display for first uncompleted task.
            if (subtask.getLocalCompletionDate() == null) {
                mSubtaskFirstTitle.setText(subtask.getTitle());

                int gray = ThemeUtils.isLightTheme(this) ? R.color.light_text_hint_color : R.color.dark_text_hint_color;
                mSubtaskFirstTitle.setTextColor(ThemeUtils.getTextColor(this));

                // Change add subtask view margin when needed.
                changeFooterCircleMargin(mSubtaskAddCircleContainer, mSubtasks.size() == 1);

                // Change visibility of buttons.
                mSubtaskFirstButtonsContainer.setVisibility(mSubtasks.size() > 1 ? View.GONE : View.VISIBLE);
                mSubtaskVisibilityContainer.setVisibility(mSubtasks.size() == 1 ? View.GONE : View.VISIBLE);

                allCompleted = false;
                break;
            }
        }

        mSubtaskFirstItem.setVisibility(allCompleted ? View.GONE : View.VISIBLE);
    }

    private String buildFormattedTags() {
        String tags = null;

        for (GsonTag tag : mTask.getTags()) {
            if (tags == null) {
                tags = tag.getTitle();
            } else {
                tags += TAG_SEPARATOR + tag.getTitle();
            }
        }

        return tags;
    }

    private TextView.OnEditorActionListener mEnterListener =
            new TextView.OnEditorActionListener() {
                @Override
                public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                    if (actionId == EditorInfo.IME_ACTION_DONE) {
                        // If the action is a key-up event on the return key, save task changes.
                        performChanges(true);
                    }
                    return true;
                }
            };

    private KeyboardBackListener mKeyboardBackListener = new KeyboardBackListener() {
        @Override
        public void onKeyboardBackPressed() {
            hideKeyboard();
        }
    };

    private void hideKeyboard() {
        InputMethodManager inputManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        inputManager.toggleSoftInput(0, 0);

        // Remove focus from text views by focusing on main layout.
        mLayout.requestFocus();
    }

    @OnClick(R.id.button_edit_task_priority)
    protected void setPriority() {
        Integer priority = mPriority.isChecked() ? 1 : 0;
        mTask.setPriority(priority);
        mTasksService.saveTask(mTask, true);

        updateViews();
    }

    @OnClick(R.id.schedule_container)
    protected void setSchedule() {
        openSnoozeSelector();
    }

    @OnClick(R.id.repeat_container)
    protected void setRepeat() {
        if (mRepeatOptions.getVisibility() == View.GONE) {
            showRepeatOptions();
        } else {
            hideRepeatOptions();
            setRepeatDescription();
        }
    }

    @OnClick(R.id.tags_container)
    protected void setTags() {
        // Apply blur to the tags background.
        int alphaColor = ThemeUtils.getTasksBlurAlphaColor(this);
        updateBlurDrawable(alphaColor);
        mTagsArea.setBackgroundDrawable(getBlurDrawable());

        mContainer.setVisibility(View.GONE);

        // Show tags area with fade animation.
        mTagsArea.setVisibility(View.VISIBLE);
        mTagsArea.setAlpha(0f);
        mTagsArea.animate().alpha(1f).setDuration(Constants.ANIMATION_DURATION_MEDIUM);

        // Hide action bar.
        getActionBar().hide();

        loadTags();
    }

    private void performChanges(boolean hideKeyboard) {
        // Save updated properties.
        mTask.setTitle(mTitle.getText().toString());
        mTask.setNotes(mNotes.getText().toString());
        mTask.setTags(mAssignedTags);
        mTasksService.saveTask(mTask, true);

        if (hideKeyboard) hideKeyboard();

        updateViews();
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

    private void openSnoozeSelector() {
        // Call snooze activity.
        Intent intent = new Intent(this, SnoozeActivity.class);
        intent.putExtra(Constants.EXTRA_TASK_ID, mTask.getId());
        intent.putExtra(Constants.EXTRA_CALLER_NAME, Constants.CALLER_EDIT_TASKS);
        startActivityForResult(intent, Constants.SNOOZE_REQUEST_CODE);

        // Update blurred background and override animation.
        updateBlurDrawable(ThemeUtils.getSnoozeBlurAlphaColor(this));
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
    }

    private void updateBlurDrawable(int alphaColor) {
        sBlurDrawable = BlurBuilder.blur(this, getWindow().getDecorView(), alphaColor);
    }

    public static BitmapDrawable getBlurDrawable() {
        return sBlurDrawable;
    }

    private void closeTags() {
        mTagsArea.setVisibility(View.GONE);

        // Show edit task area with fade animation.
        mContainer.setVisibility(View.VISIBLE);
        mContainer.setAlpha(0f);
        mContainer.animate().alpha(1f).setDuration(Constants.ANIMATION_DURATION_MEDIUM);

        // Hide action bar.
        getActionBar().show();

        updateViews();
    }

    @OnClick(R.id.tags_back_button)
    protected void tagsBack() {
        // Close tags area with animation.
        closeTags();
    }

    @OnClick(R.id.assign_tags_area)
    protected void tagsAreaClick() {
        // Close tags area with animation.
        closeTags();
    }

    @OnClick(R.id.tags_add_button)
    protected void addTag() {
        // Create tag title input.
        final EditText input = new EditText(this);
        input.setHint(getString(R.string.add_tag_dialog_hint));
        input.setInputType(InputType.TYPE_CLASS_TEXT);

        // Display dialog to save new tag.
        new AccentAlertDialog.Builder(this)
                .setTitle(getString(R.string.add_tag_dialog_title))
                .setPositiveButton(getString(R.string.add_tag_dialog_yes), new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int which) {
                        String title = input.getText().toString();
                        if (!title.isEmpty()) {
                            // Save new tag to database.
                            mTasksService.createTag(title);

                            // Refresh displayed tags.
                            loadTags();
                        }
                    }
                })
                .setNegativeButton(getString(R.string.add_tag_dialog_cancel), null)
                .setView(customizeAddTagInput(input))
                .create()
                .show();
    }

    private LinearLayout customizeAddTagInput(EditText input) {
        // Create layout with margins.
        LinearLayout layout = new LinearLayout(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        int margin = getResources().getDimensionPixelSize(R.dimen.add_tag_input_margin);
        params.setMargins(margin, 0, margin, 0);

        // Wrap input inside layout.
        layout.addView(input, params);
        return layout;
    }

    private void loadTags() {
        List<GsonTag> tags = mTasksService.loadAllTags();
        mAssignedTags = new ArrayList<GsonTag>();
        mTaskTagsContainer.removeAllViews();

        // For each tag, add a checkbox as child view.
        for (GsonTag tag : tags) {
            int resource = ThemeUtils.isLightTheme(this) ? R.layout.tag_box_light : R.layout.tag_box_dark;
            CheckBox tagBox = (CheckBox) getLayoutInflater().inflate(resource, null);
            tagBox.setText(tag.getTitle());
            tagBox.setId(tag.getId().intValue());

            // Set listeners to assign and delete.
            tagBox.setOnClickListener(mTagClickListener);
            tagBox.setOnLongClickListener(mTagLongClickListener);

            // Pre-check tag if it's already assigned.
            if (isTagAssigned(tag)) {
                mAssignedTags.add(tag);
                tagBox.setChecked(true);
            }

            // Add child view.
            mTaskTagsContainer.addView(tagBox);
        }
    }

    private View.OnClickListener mTagClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            GsonTag selectedTag = mTasksService.loadTag((long) view.getId());

            // Assign or remove tag from selected tasks.
            if (isTagAssigned(selectedTag)) {
                unassignTag(selectedTag);
            } else {
                assignTag(selectedTag);
            }
        }
    };

    private View.OnLongClickListener mTagLongClickListener = new View.OnLongClickListener() {
        @Override
        public boolean onLongClick(View view) {
            final GsonTag selectedTag = mTasksService.loadTag((long) view.getId());

            // Display dialog to delete tag.
            new AccentAlertDialog.Builder(mContext.get())
                    .setTitle(getString(R.string.delete_tag_dialog_title, selectedTag.getTitle()))
                    .setMessage(getString(R.string.delete_tag_dialog_message))
                    .setPositiveButton(getString(R.string.delete_tag_dialog_yes), new DialogInterface.OnClickListener() {

                        public void onClick(DialogInterface dialog, int which) {
                            // Delete tag and unassign it from all tasks.
                            mTasksService.deleteTag(selectedTag.getId());

                            // Refresh displayed tags.
                            loadTags();
                        }
                    })
                    .setNegativeButton(getString(R.string.delete_tag_dialog_cancel), null)
                    .create()
                    .show();

            return true;
        }
    };

    private boolean isTagAssigned(GsonTag selectedTag) {
        // Check if tag is already assigned to the task.
        for (GsonTag tag : mTask.getTags()) {
            if (tag.getId().equals(selectedTag.getId())) {
                return true;
            }
        }
        return false;
    }

    private void assignTag(GsonTag tag) {
        // Add to list and perform changes.
        mAssignedTags.add(tag);
        performChanges(false);
    }

    private void unassignTag(GsonTag tag) {
        // Unassign and update list.
        mTasksService.unassignTag(tag.getId(), mTask.getId());
        removeSelectedTag(tag);
    }

    private void removeSelectedTag(GsonTag selectedTag) {
        // Find and remove tag from the list of selected.
        List<GsonTag> selected = new ArrayList<GsonTag>(mAssignedTags);
        for (GsonTag tag : selected) {
            if (tag.getId().equals(selectedTag.getId())) {
                mAssignedTags.remove(tag);
            }
        }
    }

    private void shareTask() {
        String content = getString(R.string.share_message_header);
        content += getString(R.string.share_message_circle) + mTask.getTitle() + "\n";

        for (GsonTask subtask : mTasksService.loadSubtasksForTask(mTask.getTempId())) {
            content += "\t\t" + getString(R.string.share_message_circle) + subtask.getTitle() + "\n";
        }

        content += "\n" + getString(R.string.share_message_footer);

        Intent inviteIntent = new Intent(Intent.ACTION_SEND);
        inviteIntent.setType("text/html");
        inviteIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, getString(R.string.share_message_subject));
        inviteIntent.putExtra(android.content.Intent.EXTRA_TEXT, content);

        startActivity(Intent.createChooser(inviteIntent, getString(R.string.share_chooser_title)));
    }

    private void showRepeatOptions() {
        // Animate display of repeat options.
        mRepeatOptions.setVisibility(View.VISIBLE);
        mRepeatOptions.setAlpha(0f);
        mRepeatOptions.animate().alpha(1f).setDuration(Constants.ANIMATION_DURATION_MEDIUM).setListener(null);

        mRepeat.setText(getString(R.string.edit_task_repeat_choice_mode));
    }

    private void hideRepeatOptions() {
        // Animate hide of repeat options.
        mRepeatOptions.animate().alpha(0f).setDuration(Constants.ANIMATION_DURATION_MEDIUM).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mRepeatOptions.setVisibility(View.GONE);
            }
        });
    }

    @OnClick(R.id.repeat_option_never)
    protected void repeatNever() {
        clearRepeatSelections();
        mRepeatNever.select();

        mTask.setRepeatOption(RepeatOptions.NEVER.getValue());
        mTask.setLocalRepeatDate(null);

        hideRepeatOptions();

        performChanges(false);
    }

    @OnClick(R.id.repeat_option_day)
    protected void repeatDay() {
        clearRepeatSelections();
        mRepeatDay.select();

        mTask.setRepeatOption(RepeatOptions.EVERY_DAY.getValue());
        mTask.setLocalRepeatDate(mTask.getLocalSchedule());

        hideRepeatOptions();

        performChanges(false);
    }

    @OnClick(R.id.repeat_option_mon_fri)
    protected void repeatMonFri() {
        clearRepeatSelections();
        mRepeatMonFri.select();

        mTask.setRepeatOption(RepeatOptions.MONDAY_TO_FRIDAY.getValue());
        mTask.setLocalRepeatDate(mTask.getLocalSchedule());

        hideRepeatOptions();

        performChanges(false);
    }

    @OnClick(R.id.repeat_option_week)
    protected void repeatWeek() {
        clearRepeatSelections();
        mRepeatWeek.select();

        mTask.setRepeatOption(RepeatOptions.EVERY_WEEK.getValue());
        mTask.setLocalRepeatDate(mTask.getLocalSchedule());

        hideRepeatOptions();

        performChanges(false);
    }

    @OnClick(R.id.repeat_option_month)
    protected void repeatMonth() {
        clearRepeatSelections();
        mRepeatMonth.select();

        mTask.setRepeatOption(RepeatOptions.EVERY_MONTH.getValue());
        mTask.setLocalRepeatDate(mTask.getLocalSchedule());

        hideRepeatOptions();

        performChanges(false);
    }

    @OnClick(R.id.repeat_option_year)
    protected void repeatYear() {
        clearRepeatSelections();
        mRepeatYear.select();

        mTask.setRepeatOption(RepeatOptions.EVERY_YEAR.getValue());
        mTask.setLocalRepeatDate(mTask.getLocalSchedule());

        hideRepeatOptions();

        performChanges(false);
    }

    private void clearRepeatSelections() {
        for (int i = 0; i < mRepeatOptions.getChildCount(); i++) {
            View view = mRepeatOptions.getChildAt(i);

            if (view instanceof RepeatOption) {
                ((RepeatOption) view).clearSelection();
            }
        }
    }

    private void setSelectedRepeatOption() {
        String repeatOption = mTask.getRepeatOption();

        // Set selected option.
        if (repeatOption.equals(RepeatOptions.NEVER.getValue())) {
            mRepeatNever.select();
        } else if (repeatOption.equals(RepeatOptions.EVERY_DAY.getValue())) {
            mRepeatDay.select();
        } else if (repeatOption.equals(RepeatOptions.MONDAY_TO_FRIDAY.getValue())) {
            mRepeatMonFri.select();
        } else if (repeatOption.equals(RepeatOptions.EVERY_WEEK.getValue())) {
            mRepeatWeek.select();
        } else if (repeatOption.equals(RepeatOptions.EVERY_MONTH.getValue())) {
            mRepeatMonth.select();
        } else if (repeatOption.equals(RepeatOptions.EVERY_YEAR.getValue())) {
            mRepeatYear.select();
        }
    }

    private void setRepeatDescription() {
        String repeatOption = mTask.getRepeatOption();

        // Set friendly description.
        if (repeatOption.equals(RepeatOptions.NEVER.getValue())) {
            // Option is set to never.
            mRepeat.setText(getString(R.string.repeat_never_description));
        } else {
            Calendar repeatDate = Calendar.getInstance();
            repeatDate.setTime(mTask.getLocalRepeatDate());

            String time = DateUtils.getTimeAsString(this, mTask.getLocalRepeatDate());
            String dayOfWeek = DateUtils.formatDayOfWeek(this, repeatDate);
            String dayOfMonth = DateUtils.formatDayOfMonth(this, repeatDate);
            String month = DateUtils.formatMonth(this, repeatDate);

            if (repeatOption.equals(RepeatOptions.EVERY_DAY.getValue())) {
                // Option is set to every day.
                mRepeat.setText(getString(R.string.repeat_day_description, time));
            } else if (repeatOption.equals(RepeatOptions.MONDAY_TO_FRIDAY.getValue())) {
                // Option is set to monday to friday.
                mRepeat.setText(getString(R.string.repeat_mon_fri_description, time));
            } else if (repeatOption.equals(RepeatOptions.EVERY_WEEK.getValue())) {
                // Option is set to every week.
                mRepeat.setText(getString(R.string.repeat_week_description, dayOfWeek, time));
            } else if (repeatOption.equals(RepeatOptions.EVERY_MONTH.getValue())) {
                // Option is set to every month.
                mRepeat.setText(getString(R.string.repeat_month_description, dayOfMonth, time));
            } else if (repeatOption.equals(RepeatOptions.EVERY_YEAR.getValue())) {
                // Option is set to every year.
                mRepeat.setText(getString(R.string.repeat_year_description, month, dayOfMonth, time));
            }
        }
    }

    private SubtaskListener mSubtaskListener = new SubtaskListener() {
        @Override
        public void completeSubtask(GsonTask task) {
            task.setLocalCompletionDate(new Date());
            saveSubtask(task);
        }

        @Override
        public void uncompleteSubtask(GsonTask task) {
            task.setLocalCompletionDate(null);
            saveSubtask(task);
        }

        @Override
        public void deleteSubtask(final GsonTask task) {
            // Display dialog to delete subtask.
            new AccentAlertDialog.Builder(mContext.get())
                    .setTitle(getString(R.string.delete_subtask_dialog_title))
                    .setMessage(getString(R.string.delete_subtask_dialog_message))
                    .setPositiveButton(getString(R.string.delete_subtask_dialog_yes), new DialogInterface.OnClickListener() {

                        public void onClick(DialogInterface dialog, int which) {
                            // Delete subtask.
                            task.setDeleted(true);
                            saveSubtask(task);

                            if (mSubtasks.isEmpty() && mListView.getVisibility() == View.VISIBLE)
                                hideSubtasks();
                        }
                    })
                    .setNegativeButton(getString(R.string.delete_subtask_dialog_cancel), null)
                    .create()
                    .show();
        }
    };

    private void refreshSubtasks() {
        mSubtasks = mTasksService.loadSubtasksForTask(mTask.getTempId());
        mAdapter.update(mSubtasks);
    }

    private TextView.OnEditorActionListener mSubtaskEnterListener =
            new TextView.OnEditorActionListener() {
                @Override
                public boolean onEditorAction(TextView view, int actionId, KeyEvent event) {
                    if (actionId == EditorInfo.IME_ACTION_DONE) {
                        // If the action is a key-up event on the return key, save subtask.
                        createSubtask(view.getText().toString());

                        // Clear text view.
                        view.setText("");
                    }
                    return true;
                }
            };

    private void saveSubtask(GsonTask task) {
        mTasksService.saveTask(task, true);
        refreshSubtasks();
        updateViews();
    }

    private void createSubtask(String title) {
        Date currentDate = new Date();
        String tempId = UUID.randomUUID().toString();

        if (!title.isEmpty()) {
            GsonTask task = GsonTask.gsonForLocal(null, null, tempId, mTask.getTempId(), currentDate, currentDate, false, title, null, 0, 0, null, currentDate, null, null, RepeatOptions.NEVER.getValue(), null, null, new ArrayList<GsonTag>(), 0);
            mTasksService.saveTask(task, true);

            refreshSubtasks();

            updateViews();

            hideKeyboard();

            if (mListView.getVisibility() == View.GONE) showSubtasks();
        }
    }

    @OnClick(R.id.subtask_visibility_container)
    protected void showSubtasksClick() {
        showSubtasks();
    }

    View.OnClickListener mHideSubtasksClick = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            hideSubtasks();
        }
    };

    @OnClick(R.id.subtask_first_item)
    protected void firstSubtaskClick() {
        if (mSubtasks.size() > 1) showSubtasks();
    }

    @OnLongClick(R.id.subtask_first_item)
    protected boolean firstSubtaskLongClick() {
        mSubtaskListener.deleteSubtask(getFirstUncompletedSubtask());
        return true;
    }

    @OnClick(R.id.subtask_first_button)
    protected void firstSubtaskButtonClick() {
        mSubtaskFirstItem.animate().alpha(0f).setDuration(Constants.ANIMATION_DURATION_SHORT).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                GsonTask task = getFirstUncompletedSubtask();

                if (task != null) {
                    task.setLocalCompletionDate(new Date());
                    saveSubtask(task);
                }

                mSubtaskFooter.setAlpha(0f);
                mSubtaskFooter.animate().alpha(1f).setDuration(Constants.ANIMATION_DURATION_SHORT);

                changeFooterCircleMargin(mSubtaskAddCircleContainer, false);
            }
        });
    }

    private void showSubtasks() {
        mPropertiesView.setVisibility(View.GONE);

        // Show list view with fade animation.
        mListView.setVisibility(View.VISIBLE);
        mListView.setAlpha(0f);
        mListView.animate().alpha(1f).setDuration(Constants.ANIMATION_DURATION_MEDIUM).setListener(null);

        mListView.setSelection(0);
    }

    private void hideSubtasks() {
        // Hide list view with fade animation.
        mListView.animate().alpha(0f).setDuration(Constants.ANIMATION_DURATION_SHORT).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mListView.setVisibility(View.GONE);

                // Show edit area with fade animation.
                mPropertiesView.setVisibility(View.VISIBLE);
                mPropertiesView.setAlpha(0f);
                mPropertiesView.animate().alpha(1f).setDuration(Constants.ANIMATION_DURATION_SHORT);
            }
        });

        updateViews();
    }

    private GsonTask getFirstUncompletedSubtask() {
        for (GsonTask subtask : mSubtasks) {
            if (subtask.getLocalCompletionDate() == null) return subtask;
        }
        return null;
    }

    private void changeFooterCircleMargin(FrameLayout container, boolean isLarge) {
        int left = getResources().getDimensionPixelSize(R.dimen.subtask_circle_margin_left);
        int leftLarge = getResources().getDimensionPixelSize(R.dimen.subtask_circle_margin_left_large);
        int right = getResources().getDimensionPixelSize(R.dimen.subtask_circle_margin_right);

        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) container.getLayoutParams();
        params.setMargins(isLarge ? leftLarge : left, 0, right, 0);
        container.setLayoutParams(params);
    }

}
