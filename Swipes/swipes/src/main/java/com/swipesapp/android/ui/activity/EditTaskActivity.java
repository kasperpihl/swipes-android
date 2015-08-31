package com.swipesapp.android.ui.activity;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.text.InputType;
import android.text.util.Linkify;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.evernote.edam.type.Note;
import com.swipesapp.android.R;
import com.swipesapp.android.analytics.handler.Analytics;
import com.swipesapp.android.analytics.handler.IntercomHandler;
import com.swipesapp.android.analytics.values.Actions;
import com.swipesapp.android.analytics.values.Categories;
import com.swipesapp.android.analytics.values.IntercomEvents;
import com.swipesapp.android.analytics.values.IntercomFields;
import com.swipesapp.android.analytics.values.Labels;
import com.swipesapp.android.analytics.values.Screens;
import com.swipesapp.android.app.SwipesApplication;
import com.swipesapp.android.evernote.EvernoteService;
import com.swipesapp.android.handler.SoundHandler;
import com.swipesapp.android.sync.gson.GsonAttachment;
import com.swipesapp.android.sync.gson.GsonTag;
import com.swipesapp.android.sync.gson.GsonTask;
import com.swipesapp.android.sync.service.SyncService;
import com.swipesapp.android.sync.service.TasksService;
import com.swipesapp.android.ui.adapter.SubtasksAdapter;
import com.swipesapp.android.ui.listener.KeyboardBackListener;
import com.swipesapp.android.ui.listener.SubtaskListener;
import com.swipesapp.android.ui.view.ActionEditText;
import com.swipesapp.android.ui.view.FlowLayout;
import com.swipesapp.android.ui.view.RepeatOption;
import com.swipesapp.android.ui.view.SwipesButton;
import com.swipesapp.android.ui.view.SwipesDialog;
import com.swipesapp.android.ui.view.SwipesTextView;
import com.swipesapp.android.util.DateUtils;
import com.swipesapp.android.util.PreferenceUtils;
import com.swipesapp.android.util.ThemeUtils;
import com.swipesapp.android.values.Constants;
import com.swipesapp.android.values.Intents;
import com.swipesapp.android.values.RepeatOptions;
import com.swipesapp.android.values.Sections;
import com.swipesapp.android.values.Services;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import butterknife.OnLongClick;

public class EditTaskActivity extends FragmentActivity {

    @InjectView(R.id.main_layout)
    LinearLayout mLayout;

    @InjectView(R.id.edit_task_button_header)
    RelativeLayout mButtonHeader;
    @InjectView(R.id.edit_task_evernote_button)
    SwipesButton mEvernoteButton;
    @InjectView(R.id.edit_task_delete_button)
    SwipesButton mDeleteButton;
    @InjectView(R.id.edit_task_share_button)
    SwipesButton mShareButton;

    @InjectView(R.id.properties_view)
    ScrollView mPropertiesView;

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

    @InjectView(R.id.evernote_attachment_container)
    RelativeLayout mEvernoteAttachmentContainer;
    @InjectView(R.id.evernote_attachment_icon)
    SwipesTextView mEvernoteAttachmentIcon;
    @InjectView(R.id.evernote_attachment_title)
    TextView mEvernoteAttachmentTitle;
    @InjectView(R.id.evernote_attached_view)
    TextView mEvernoteAttachedView;

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

    @InjectView(R.id.subtask_add_icon)
    SwipesTextView mSubtaskAddIcon;
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
    ActionEditText mSubtaskFirstTitle;
    @InjectView(R.id.subtask_first_button)
    CheckBox mSubtaskFirstButton;

    private static final String TAG_SEPARATOR = ", ";

    private static final String INTENT_VIEW_NOTE = "com.evernote.action.VIEW_NOTE";
    private static final String EXTRA_EVERNOTE_GUID = "NOTE_GUID";

    private static final String GUID_PREFIX = "guid\":\"";
    private static final String GUID_SUFFIX = "\"";

    private WeakReference<Context> mContext;

    private TasksService mTasksService;
    private SyncService mSyncService;

    private Long mId;

    private GsonTask mTask;
    private GsonAttachment mEvernoteAttachment;

    private List<GsonTag> mAssignedTags;
    private int mAssignedTagsCount;
    private int mUnassignedTagsCount;

    private SubtasksAdapter mAdapter;
    private ListView mListView;
    private List<GsonTask> mSubtasks;

    private Sections mSection;
    private boolean mShowActionSteps;

    private boolean mOpenedFromWidget;
    private boolean mIsShowingDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(ThemeUtils.getDialogThemeResource(this));
        setContentView(R.layout.activity_edit_task);
        ButterKnife.inject(this);

        getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        setVolumeControlStream(AudioManager.STREAM_MUSIC);

        mContext = new WeakReference<Context>(this);

        mTasksService = TasksService.getInstance();
        mSyncService = SyncService.getInstance();

        int sectionNumber = getIntent().getIntExtra(Constants.EXTRA_SECTION_NUMBER, 1);
        mSection = Sections.getSectionByNumber(sectionNumber);

        mId = getIntent().getLongExtra(Constants.EXTRA_TASK_ID, 0);

        mTask = mTasksService.loadTask(mId);

        setupViews();

        setupListView();

        updateViews();

        mShowActionSteps = getIntent().getBooleanExtra(Constants.EXTRA_SHOW_ACTION_STEPS, false);
        if (mShowActionSteps) showSubtasks();

        mOpenedFromWidget = getIntent().getBooleanExtra(Constants.EXTRA_FROM_WIDGET, false);
    }

    @Override
    public void finish() {
        // Save note changes when the user forgets to press the "Done" key.
        if (!mTask.isDeleted()) {
            if (!mNotes.getText().toString().equals(mTask.getNotes())) {
                performChanges(true);

                // Send analytics event.
                sendNoteChangedEvent();
            }
        }

        // Refresh tasks and widget.
        TasksActivity.setPendingRefresh();
        TasksActivity.refreshWidgets(this);

        setResult(Activity.RESULT_OK);

        super.finish();

        overridePendingTransition(0, 0);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == Constants.SNOOZE_REQUEST_CODE) {
            switch (resultCode) {
                case RESULT_OK:
                    // Task has been snoozed. Update views.
                    mSection = Sections.LATER;
                    updateViews();
                    break;
            }
        } else if (requestCode == Constants.EVERNOTE_ATTACHMENTS_REQUEST_CODE) {
            switch (resultCode) {
                case RESULT_OK:
                    // Attachment was added. Update views.
                    updateViews();
                    break;
            }
        }

        // Reset state flag.
        mIsShowingDialog = false;
    }

    @Override
    public void onBackPressed() {
        // Only close activity when subtasks are collapsed and tags are closed.
        if (mListView.getVisibility() == View.VISIBLE && !mShowActionSteps) {
            hideSubtasks();
        } else if (mTagsArea.getVisibility() == View.VISIBLE) {
            closeTags();
        } else {
            finish();
        }
    }

    @Override
    public void onResume() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intents.TASKS_CHANGED);
        registerReceiver(mReceiver, filter);

        // Send screen view event.
        Analytics.sendScreenView(Screens.SCREEN_EDIT_TASK);

        super.onResume();
    }

    @Override
    public void onPause() {
        unregisterReceiver(mReceiver);

        super.onPause();
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

    protected void onUserLeaveHint() {
        // Close when opened from the widget and user presses the home key.
        if (mOpenedFromWidget && !mIsShowingDialog) finish();
    }

    private void setupViews() {
        mLayout.requestFocus();

        boolean lightTheme = ThemeUtils.isLightTheme(this);

        int background = lightTheme ? R.drawable.edit_dialog_light : R.drawable.edit_dialog_dark;
        mLayout.setBackgroundResource(background);

        int headerColor = lightTheme ? R.color.neutral_header_light : R.color.neutral_header_dark;
        mButtonHeader.setBackgroundColor(getResources().getColor(headerColor));

        int secondaryColor = lightTheme ? R.color.light_hint : R.color.dark_hint;
        mEvernoteButton.setTextColor(getResources().getColor(secondaryColor));
        mDeleteButton.setTextColor(getResources().getColor(secondaryColor));
        mShareButton.setTextColor(getResources().getColor(secondaryColor));

        if (!EvernoteService.getInstance().isAuthenticated() || !PreferenceUtils.isEvernoteSyncEnabled(this)) {
            mEvernoteButton.setVisibility(View.GONE);
        }

        mTitle.setTextColor(ThemeUtils.getTextColor(this));
        mTitle.setOnEditorActionListener(mTitleEnterListener);
        mTitle.setListener(mTitleKeyboardBackListener);

        mPropertiesView.setOnTouchListener(mPropertiesTouchListener);

        mScheduleIcon.setTextColor(ThemeUtils.getTextColor(this));
        mSchedule.setTextColor(ThemeUtils.getTextColor(this));

        mRepeatIcon.setTextColor(ThemeUtils.getTextColor(this));
        mRepeat.setTextColor(ThemeUtils.getTextColor(this));
        mRepeat.setHintTextColor(ThemeUtils.getTextColor(this));

        mTagsIcon.setTextColor(ThemeUtils.getTextColor(this));
        mTags.setTextColor(ThemeUtils.getTextColor(this));
        mTags.setHintTextColor(ThemeUtils.getTextColor(this));

        int reverseColor = lightTheme ? R.color.dark_text : R.color.light_text;
        mEvernoteAttachmentIcon.setTextColor(ThemeUtils.getTextColor(this));
        mEvernoteAttachmentTitle.setTextColor(ThemeUtils.getTextColor(this));
        mEvernoteAttachedView.setBackgroundResource(lightTheme ? R.drawable.round_rectangle_light : R.drawable.round_rectangle_dark);
        mEvernoteAttachedView.setTextColor(getResources().getColor(reverseColor));

        mNotesIcon.setTextColor(ThemeUtils.getTextColor(this));
        mNotes.setTextColor(ThemeUtils.getTextColor(this));
        mNotes.setHintTextColor(ThemeUtils.getTextColor(this));
        mNotes.setLinkTextColor(ThemeUtils.getSectionColor(mSection, this));
        mNotes.setOnEditorActionListener(mNotesEnterListener);
        mNotes.setListener(mNotesKeyboardBackListener);
        mNotes.setOnTouchListener(mNotesTouchListener);

        mSubtaskAddIcon.setTextColor(ThemeUtils.getTextColor(this));

        mSubtaskAddTitle.setTextColor(ThemeUtils.getTextColor(this));
        mSubtaskAddTitle.setHintTextColor(ThemeUtils.getTextColor(this));
        mSubtaskAddTitle.setOnEditorActionListener(mSubtaskEnterListener);
        mSubtaskAddTitle.setListener(mTitleKeyboardBackListener);

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
        mAdapter = new SubtasksAdapter(this, mSubtasks, mSubtaskListener, mLayout);
        mListView.setAdapter(mAdapter);
    }

    private void customizeFooter(LinearLayout footer) {
        LinearLayout container = (LinearLayout) footer.findViewById(R.id.subtask_visibility_container);
        container.setOnClickListener(mHideSubtasksClick);

        SwipesTextView addIcon = (SwipesTextView) footer.findViewById(R.id.subtask_add_icon);
        addIcon.setTextColor(ThemeUtils.getTextColor(this));

        ActionEditText addTitle = (ActionEditText) footer.findViewById(R.id.subtask_add_title);
        addTitle.setTextColor(ThemeUtils.getTextColor(this));
        addTitle.setHintTextColor(ThemeUtils.getTextColor(this));
        addTitle.setOnEditorActionListener(mSubtaskEnterListener);
        addTitle.setListener(mTitleKeyboardBackListener);

        SwipesTextView visibilityIcon = (SwipesTextView) footer.findViewById(R.id.subtask_visibility_icon);
        visibilityIcon.setRotation(90f);
        visibilityIcon.setTextColor(ThemeUtils.getTextColor(this));

        TextView visibilityCaption = (TextView) footer.findViewById(R.id.subtask_visibility_caption);
        visibilityCaption.setText(getString(R.string.subtask_hide_caption));
        visibilityCaption.setTextColor(ThemeUtils.getTextColor(this));
    }

    private void updateViews() {
        mTask = mTasksService.loadTask(mId);

        mTitle.setText(mTask.getTitle());

        if (mTask.getPriority() != null) mPriority.setChecked(mTask.getPriority() == 1);

        mSchedule.setText(DateUtils.formatToRecent(mTask.getLocalSchedule(), this, true));

        setSelectedRepeatOption();

        setRepeatDescription();

        mTags.setText(buildFormattedTags());

        loadEvernoteAttachment();

        mNotes.setText(mTask.getNotes());
        Linkify.addLinks(mNotes, Linkify.ALL);

        mSubtaskVisibilityCaption.setText(getString(R.string.subtask_show_caption));

        mSubtaskVisibilityContainer.setVisibility(mSubtasks.isEmpty() ? View.GONE : View.VISIBLE);

        mSubtaskFirstItem.setAlpha(1f);

        loadFirstSubtask();

        updateViewsForSection();
    }

    private void updateViewsForSection() {
        switch (mSection) {
            case LATER:
                mPriority.setBackgroundResource(R.drawable.later_circle_selector);
                break;
            case FOCUS:
                mPriority.setBackgroundResource(R.drawable.focus_circle_selector);
                break;
            case DONE:
                mPriority.setBackgroundResource(R.drawable.done_circle_selector);
                break;
        }
    }

    private void loadEvernoteAttachment() {
        if (mTask.getAttachments() != null) {
            for (GsonAttachment attachment : mTask.getAttachments()) {
                // Check if attachment comes from Evernote.
                if (attachment.getService().equals(Services.EVERNOTE)) {
                    // Attachment found. Update views.
                    mEvernoteAttachment = attachment;
                    mEvernoteAttachmentContainer.setVisibility(View.VISIBLE);
                    mEvernoteAttachmentTitle.setText(attachment.getTitle());
                }
            }
        }
    }

    private void loadFirstSubtask() {
        boolean allCompleted = true;

        for (final GsonTask subtask : mSubtasks) {
            // Display for first uncompleted task.
            if (subtask.getLocalCompletionDate() == null) {
                // Setup properties.
                mSubtaskFirstTitle.setText(subtask.getTitle());
                mSubtaskFirstTitle.setTextColor(ThemeUtils.getTextColor(this));

                mSubtaskFirstButton.setChecked(false);
                int background = ThemeUtils.isLightTheme(this) ? R.drawable.checkbox_selector_light : R.drawable.checkbox_selector_dark;
                mSubtaskFirstButton.setBackgroundResource(background);

                // Edit confirmation listener.
                mSubtaskFirstTitle.setOnEditorActionListener(new TextView.OnEditorActionListener() {
                    @Override
                    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                        if (actionId == EditorInfo.IME_ACTION_DONE) {
                            // If the action is a key-up event on the return key, save changes.
                            if (v.getText().length() > 0) {
                                subtask.setTitle(v.getText().toString());
                                saveSubtask(subtask);
                            } else {
                                v.setText(subtask.getTitle());
                            }

                            hideKeyboard();
                        }
                        return true;
                    }
                });

                // Edit cancel listener.
                mSubtaskFirstTitle.setListener(new KeyboardBackListener() {
                    @Override
                    public void onKeyboardBackPressed() {
                        hideKeyboard();

                        if (mSubtaskFirstTitle.getText().length() <= 0) {
                            mSubtaskFirstTitle.setText(subtask.getTitle());
                        }
                    }
                });

                // Change button visibility.
                mSubtaskVisibilityContainer.setVisibility(mSubtasks.size() == 1 ? View.GONE : View.VISIBLE);

                allCompleted = false;
                break;
            }
        }

        mSubtaskFirstItem.setVisibility(allCompleted ? View.GONE : View.VISIBLE);
    }

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Intents.TASKS_CHANGED)) {
                // Refresh subtasks.
                loadFirstSubtask();
                refreshSubtasks();
            }
        }
    };

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

    private TextView.OnEditorActionListener mTitleEnterListener =
            new TextView.OnEditorActionListener() {
                @Override
                public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                    if (actionId == EditorInfo.IME_ACTION_DONE) {
                        // If the action is a key-up event on the return key, save task changes.
                        if (v.getText().length() > 0) {
                            performChanges(true);
                        } else {
                            v.setText(mTask.getTitle());
                            hideKeyboard();
                        }
                    }
                    return true;
                }
            };

    private KeyboardBackListener mTitleKeyboardBackListener = new KeyboardBackListener() {
        @Override
        public void onKeyboardBackPressed() {
            hideKeyboard();

            if (mTitle.getText().length() <= 0) {
                mTitle.setText(mTask.getTitle());
            }
        }
    };

    @OnClick(R.id.notes_container)
    protected void editNotes() {
        mNotes.requestFocus();
        showKeyboard();
    }

    private TextView.OnEditorActionListener mNotesEnterListener =
            new TextView.OnEditorActionListener() {
                @Override
                public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                    if (actionId == EditorInfo.IME_ACTION_DONE) {
                        // If the action is a key-up event on the return key, save task changes.
                        performChanges(true);

                        // Send analytics event.
                        sendNoteChangedEvent();
                    }
                    return true;
                }
            };

    private KeyboardBackListener mNotesKeyboardBackListener = new KeyboardBackListener() {
        @Override
        public void onKeyboardBackPressed() {
            hideKeyboard();
        }
    };

    private void showKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.toggleSoftInput(0, 0);
    }

    private void hideKeyboard() {
        InputMethodManager inputManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        inputManager.toggleSoftInput(0, 0);

        // Remove focus from text views by focusing on main layout.
        mLayout.requestFocus();
    }

    @OnClick(R.id.main_layout)
    protected void cancel() {
        finish();
    }

    @OnClick(R.id.edit_task_container)
    protected void ignore() {
        // Do nothing.
    }

    @OnClick(R.id.edit_task_priority_container)
    protected void setPriority() {
        boolean checked = mPriority.isChecked();
        mPriority.setChecked(!checked);

        Integer priority = mPriority.isChecked() ? 1 : 0;
        mTask.setPriority(priority);
        mTasksService.saveTask(mTask, true);

        // Send priority changed event.
        String label = mPriority.isChecked() ? Labels.PRIORITY_ON : Labels.PRIORITY_OFF;
        sendTaskPriorityEvent(label);

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
        mPropertiesView.setVisibility(View.GONE);

        // Show tags area with fade animation.
        mTagsArea.setVisibility(View.VISIBLE);
        mTagsArea.setAlpha(0f);
        mTagsArea.animate().alpha(1f).setDuration(Constants.ANIMATION_DURATION_MEDIUM);

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

    @OnClick(R.id.edit_task_delete_button)
    protected void deleteTask() {
        // Display confirmation dialog.
        new SwipesDialog.Builder(this)
                .title(getResources().getString(R.string.delete_single_task))
                .content(R.string.delete_task_dialog_text)
                .positiveText(R.string.delete_task_dialog_yes)
                .negativeText(R.string.delete_task_dialog_no)
                .actionsColor(ThemeUtils.getSectionColor(mSection, this))
                .callback(new MaterialDialog.ButtonCallback() {
                    @Override
                    public void onPositive(MaterialDialog dialog) {
                        // Proceed with delete.
                        mTask.setDeleted(true);
                        mTasksService.deleteTasks(Arrays.asList(mTask));

                        // Send analytics event.
                        sendDeletedTaskEvent();

                        // Play sound.
                        SoundHandler.playSound(mContext.get(), R.raw.action_negative);

                        // Close activity.
                        finish();
                    }
                })
                .show();
    }

    @OnClick(R.id.edit_task_evernote_button)
    protected void attachEvernote() {
        // Call attachments activity.
        Intent intent = new Intent(this, EvernoteAttachmentsActivity.class);
        intent.putExtra(Constants.EXTRA_TASK_ID, mTask.getId());
        startActivityForResult(intent, Constants.EVERNOTE_ATTACHMENTS_REQUEST_CODE);

        // Override animation.
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);

        // Set state flag.
        mIsShowingDialog = true;
    }

    private void openSnoozeSelector() {
        // Call snooze activity.
        Intent intent = new Intent(this, SnoozeActivity.class);
        intent.putExtra(Constants.EXTRA_TASK_ID, mTask.getId());
        startActivityForResult(intent, Constants.SNOOZE_REQUEST_CODE);

        // Set state flag.
        mIsShowingDialog = true;
    }

    private void closeTags() {
        mTagsArea.setVisibility(View.GONE);

        // Show edit task area with fade animation.
        mPropertiesView.setVisibility(View.VISIBLE);
        mPropertiesView.setAlpha(0f);
        mPropertiesView.animate().alpha(1f).setDuration(Constants.ANIMATION_DURATION_MEDIUM);

        updateViews();

        mSyncService.performSync(true, Constants.SYNC_DELAY);

        // Send analytics events.
        sendTagAssignEvents();
    }

    @OnClick(R.id.assign_tags_back_button)
    protected void tagsBack() {
        // Close tags area with animation.
        closeTags();
    }

    @OnClick(R.id.assign_tags_area)
    protected void tagsAreaClick() {
        // Do nothing.
    }

    @OnClick(R.id.assign_tags_add_button)
    protected void addTag() {
        // Create tag title input.
        final ActionEditText input = new ActionEditText(this);
        input.setHint(getString(R.string.add_tag_dialog_hint));
        input.setHintTextColor(ThemeUtils.getHintColor(this));
        input.setTextColor(ThemeUtils.getTextColor(this));
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.requestFocus();

        // Display dialog to save new tag.
        final SwipesDialog dialog = new SwipesDialog.Builder(this)
                .title(R.string.add_tag_dialog_title)
                .positiveText(R.string.add_tag_dialog_yes)
                .negativeText(R.string.add_tag_dialog_no)
                .actionsColor(ThemeUtils.getSectionColor(mSection, this))
                .customView(customizeAddTagInput(input), false)
                .callback(new MaterialDialog.ButtonCallback() {
                    @Override
                    public void onPositive(MaterialDialog dialog) {
                        String title = input.getText().toString();

                        if (!title.isEmpty()) {
                            // Save new tag.
                            confirmAddTag(title);

                            hideKeyboard();
                        }
                    }
                })
                .showListener(new DialogInterface.OnShowListener() {
                    @Override
                    public void onShow(DialogInterface dialogInterface) {
                        // Show keyboard automatically.
                        showKeyboard();
                    }
                })
                .dismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialogInterface) {
                        hideKeyboard();
                    }
                })
                .show();

        // Dismiss dialog on back press.
        input.setListener(new KeyboardBackListener() {
            @Override
            public void onKeyboardBackPressed() {
                dialog.dismiss();
            }
        });

        input.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    // If the action is a key-up event on the return key, save changes.
                    String title = v.getText().toString();

                    if (!title.isEmpty()) {
                        // Save new tag.
                        confirmAddTag(title);
                    }

                    dialog.dismiss();
                }
                return true;
            }
        });
    }

    private void confirmAddTag(String title) {
        // Save new tag to database.
        long id = mTasksService.createTag(title);
        GsonTag tag = mTasksService.loadTag(id);

        // Send analytics event.
        sendTagAddedEvent((long) title.length());

        // Assign to task.
        assignTag(tag);

        // Refresh displayed tags.
        loadTags();

        // Perform sync.
        mSyncService.performSync(true, Constants.SYNC_DELAY);

        // Play sound.
        SoundHandler.playSound(this, R.raw.action_positive);
    }

    private void confirmEditTag(GsonTag selectedTag) {
        // Save tag to database.
        mTasksService.editTag(selectedTag, true);

        // Refresh displayed tags.
        loadTags();

        // Perform sync.
        mSyncService.performSync(true, Constants.SYNC_DELAY);
    }

    private LinearLayout customizeAddTagInput(EditText input) {
        // Create layout with margins.
        LinearLayout layout = new LinearLayout(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        int margin = getResources().getDimensionPixelSize(R.dimen.add_tag_input_margin);
        params.setMargins(margin, 0, margin, 0);

        // Wrap input inside layout.
        layout.addView(input, params);
        return layout;
    }

    private void loadTags() {
        List<GsonTag> tags = mTasksService.loadAllTags();
        mAssignedTags = new ArrayList<GsonTag>();
        mAssignedTagsCount = 0;
        mUnassignedTagsCount = 0;
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

            // Create tag title input.
            final ActionEditText input = new ActionEditText(mContext.get());
            input.setText(selectedTag.getTitle());
            input.setHint(getString(R.string.add_tag_dialog_hint));
            input.setHintTextColor(ThemeUtils.getHintColor(mContext.get()));
            input.setTextColor(ThemeUtils.getTextColor(mContext.get()));
            input.setInputType(InputType.TYPE_CLASS_TEXT);
            input.requestFocus();

            // Display dialog to edit tag.
            final SwipesDialog dialog = new SwipesDialog.Builder(mContext.get())
                    .title(R.string.edit_tag_dialog_title)
                    .positiveText(R.string.add_tag_dialog_yes)
                    .neutralText(R.string.delete_tag_dialog_yes)
                    .negativeText(R.string.add_tag_dialog_no)
                    .actionsColor(ThemeUtils.getSectionColor(Sections.FOCUS, mContext.get()))
                    .customView(customizeAddTagInput(input), false)
                    .callback(new MaterialDialog.ButtonCallback() {
                        @Override
                        public void onPositive(MaterialDialog dialog) {
                            String title = input.getText().toString();

                            if (!title.isEmpty()) {
                                // Save updated tag.
                                selectedTag.setTitle(title);
                                confirmEditTag(selectedTag);
                            }
                        }

                        @Override
                        public void onNeutral(MaterialDialog dialog) {
                            // Ask to delete tag.
                            showTagDeleteDialog(selectedTag);

                            // Dismiss edit dialog.
                            dialog.dismiss();
                        }
                    })
                    .showListener(new DialogInterface.OnShowListener() {
                        @Override
                        public void onShow(DialogInterface dialogInterface) {
                            // Show keyboard automatically.
                            showKeyboard();
                        }
                    })
                    .show();

            // Dismiss dialog on back press.
            input.setListener(new KeyboardBackListener() {
                @Override
                public void onKeyboardBackPressed() {
                    dialog.dismiss();
                }
            });

            input.setOnEditorActionListener(new TextView.OnEditorActionListener() {
                @Override
                public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                    if (actionId == EditorInfo.IME_ACTION_DONE) {
                        // If the action is a key-up event on the return key, save changes.
                        String title = v.getText().toString();

                        if (!title.isEmpty()) {
                            // Save updated tag.
                            selectedTag.setTitle(title);
                            confirmEditTag(selectedTag);
                        }

                        dialog.dismiss();
                    }
                    return true;
                }
            });

            return true;
        }
    };

    private void showTagDeleteDialog(final GsonTag selectedTag) {
        // Display dialog to delete tag.
        new SwipesDialog.Builder(mContext.get())
                .title(getString(R.string.delete_tag_dialog_title, selectedTag.getTitle()))
                .content(R.string.delete_tag_dialog_message)
                .positiveText(R.string.delete_tag_dialog_yes)
                .negativeText(R.string.delete_tag_dialog_no)
                .actionsColor(ThemeUtils.getSectionColor(mSection, mContext.get()))
                .callback(new MaterialDialog.ButtonCallback() {
                    @Override
                    public void onPositive(MaterialDialog dialog) {
                        // Delete tag and unassign it from all tasks.
                        mTasksService.deleteTag(selectedTag.getId());

                        // Send analytics event.
                        sendTagDeletedEvent();

                        // Refresh displayed tags.
                        loadTags();

                        // Perform sync.
                        mSyncService.performSync(true, Constants.SYNC_DELAY);

                        // Play sound.
                        SoundHandler.playSound(mContext.get(), R.raw.action_negative);
                    }
                })
                .show();
    }

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

        mAssignedTagsCount++;
    }

    private void unassignTag(GsonTag tag) {
        // Unassign and update list.
        mTasksService.unassignTag(tag.getId(), mTask.getId());
        mAssignedTags.remove(tag);

        mUnassignedTagsCount++;
    }

    @OnClick(R.id.edit_task_share_button)
    protected void shareTask() {
        String content = getString(R.string.share_message_circle) + mTask.getTitle() + "\n";

        // Append subtask titles.
        for (GsonTask subtask : mTasksService.loadSubtasksForTask(mTask.getTempId())) {
            content += "\t\t" + getString(R.string.share_message_circle) + subtask.getTitle() + "\n";
        }

        // Append notes.
        String notes = mTask.getNotes();
        if (notes != null && !notes.isEmpty()) content += "\n Notes: \n\n" + notes;

        // Append basic footer.
        content += "\n\n" + getString(R.string.share_message_footer_sent_from);

        // Use only basic footer when sharing URLs.
        if (notes == null || !notes.startsWith("http")) {
            content += "\n" + getString(R.string.share_message_footer_get_swipes);
        }

        Intent inviteIntent = new Intent(Intent.ACTION_SEND);
        inviteIntent.setType("text/plain");
        inviteIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, getString(R.string.share_message_subject));
        inviteIntent.putExtra(android.content.Intent.EXTRA_TEXT, content);

        startActivity(Intent.createChooser(inviteIntent, getString(R.string.share_chooser_title)));

        // Send analytics event.
        sendShareTaskEvent();
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
        mRepeatOptions.animate().alpha(0f).setDuration(Constants.ANIMATION_DURATION_MEDIUM)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        mRepeatOptions.setVisibility(View.GONE);
                    }
                });
    }

    @OnClick(R.id.repeat_option_never)
    protected void repeatNever() {
        clearRepeatSelections();
        mRepeatNever.select(mSection);

        mTask.setRepeatOption(RepeatOptions.NEVER);
        mTask.setLocalRepeatDate(null);

        hideRepeatOptions();

        performChanges(false);

        sendRecurringEvent(Labels.RECURRING_NEVER);

        SoundHandler.playSound(this, R.raw.action_negative);
    }

    @OnClick(R.id.repeat_option_day)
    protected void repeatDay() {
        clearRepeatSelections();
        mRepeatDay.select(mSection);

        mTask.setRepeatOption(RepeatOptions.EVERY_DAY);
        mTask.setLocalRepeatDate(mTask.getLocalSchedule());

        hideRepeatOptions();

        performChanges(false);

        sendRecurringEvent(Labels.RECURRING_EVERY_DAY);

        SoundHandler.playSound(this, R.raw.action_positive);
    }

    @OnClick(R.id.repeat_option_mon_fri)
    protected void repeatMonFri() {
        clearRepeatSelections();
        mRepeatMonFri.select(mSection);

        mTask.setRepeatOption(RepeatOptions.MONDAY_TO_FRIDAY);
        mTask.setLocalRepeatDate(mTask.getLocalSchedule());

        hideRepeatOptions();

        performChanges(false);

        sendRecurringEvent(Labels.RECURRING_MONDAY_TO_FRIDAY);

        SoundHandler.playSound(this, R.raw.action_positive);
    }

    @OnClick(R.id.repeat_option_week)
    protected void repeatWeek() {
        clearRepeatSelections();
        mRepeatWeek.select(mSection);

        mTask.setRepeatOption(RepeatOptions.EVERY_WEEK);
        mTask.setLocalRepeatDate(mTask.getLocalSchedule());

        hideRepeatOptions();

        performChanges(false);

        sendRecurringEvent(Labels.RECURRING_EVERY_WEEK);

        SoundHandler.playSound(this, R.raw.action_positive);
    }

    @OnClick(R.id.repeat_option_month)
    protected void repeatMonth() {
        clearRepeatSelections();
        mRepeatMonth.select(mSection);

        mTask.setRepeatOption(RepeatOptions.EVERY_MONTH);
        mTask.setLocalRepeatDate(mTask.getLocalSchedule());

        hideRepeatOptions();

        performChanges(false);

        sendRecurringEvent(Labels.RECURRING_EVERY_MONTH);

        SoundHandler.playSound(this, R.raw.action_positive);
    }

    @OnClick(R.id.repeat_option_year)
    protected void repeatYear() {
        clearRepeatSelections();
        mRepeatYear.select(mSection);

        mTask.setRepeatOption(RepeatOptions.EVERY_YEAR);
        mTask.setLocalRepeatDate(mTask.getLocalSchedule());

        hideRepeatOptions();

        performChanges(false);

        sendRecurringEvent(Labels.RECURRING_EVERY_YEAR);

        SoundHandler.playSound(this, R.raw.action_positive);
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
        if (repeatOption == null || repeatOption.equals(RepeatOptions.NEVER)) {
            mRepeatNever.select(mSection);
        } else if (repeatOption.equals(RepeatOptions.EVERY_DAY)) {
            mRepeatDay.select(mSection);
        } else if (repeatOption.equals(RepeatOptions.MONDAY_TO_FRIDAY)) {
            mRepeatMonFri.select(mSection);
        } else if (repeatOption.equals(RepeatOptions.EVERY_WEEK)) {
            mRepeatWeek.select(mSection);
        } else if (repeatOption.equals(RepeatOptions.EVERY_MONTH)) {
            mRepeatMonth.select(mSection);
        } else if (repeatOption.equals(RepeatOptions.EVERY_YEAR)) {
            mRepeatYear.select(mSection);
        }
    }

    private void setRepeatDescription() {
        String repeatOption = mTask.getRepeatOption();
        Date repeatDate = mTask.getLocalRepeatDate();

        // Set friendly description.
        if (repeatDate == null || repeatOption == null || repeatOption.equals(RepeatOptions.NEVER)) {
            // Option is set to never.
            mRepeat.setText(getString(R.string.repeat_never_description));
        } else {
            Calendar repeatCalendar = Calendar.getInstance();
            repeatCalendar.setTime(repeatDate);

            String time = DateUtils.getTimeAsString(this, repeatDate);
            String dayOfWeek = DateUtils.formatDayOfWeek(this, repeatCalendar);
            String dayOfMonth = DateUtils.formatDayOfMonth(this, repeatCalendar);
            String month = DateUtils.formatMonth(this, repeatCalendar);

            if (repeatOption.equals(RepeatOptions.EVERY_DAY)) {
                // Option is set to every day.
                mRepeat.setText(getString(R.string.repeat_day_description, time));
            } else if (repeatOption.equals(RepeatOptions.MONDAY_TO_FRIDAY)) {
                // Option is set to monday to friday.
                mRepeat.setText(getString(R.string.repeat_mon_fri_description, time));
            } else if (repeatOption.equals(RepeatOptions.EVERY_WEEK)) {
                // Option is set to every week.
                mRepeat.setText(getString(R.string.repeat_week_description, dayOfWeek, time));
            } else if (repeatOption.equals(RepeatOptions.EVERY_MONTH)) {
                // Option is set to every month.
                mRepeat.setText(getString(R.string.repeat_month_description, dayOfMonth, time));
            } else if (repeatOption.equals(RepeatOptions.EVERY_YEAR)) {
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

            // Send analytics event.
            sendSubtaskCompletedEvent();

            // Play sound.
            SoundHandler.playSound(mContext.get(), R.raw.complete_task_1);
        }

        @Override
        public void uncompleteSubtask(GsonTask task) {
            task.setLocalCompletionDate(null);
            saveSubtask(task);

            // Play sound.
            SoundHandler.playSound(mContext.get(), R.raw.focus_task);
        }

        @Override
        public void deleteSubtask(final GsonTask task) {
            // Delete subtask.
            task.setDeleted(true);
            saveSubtask(task);

            if (mSubtasks.isEmpty() && mListView.getVisibility() == View.VISIBLE)
                hideSubtasks();

            // Send analytics event.
            sendSubtaskDeletedEvent();

            // Play sound.
            SoundHandler.playSound(mContext.get(), R.raw.action_negative);
        }

        @Override
        public void editSubtask(GsonTask task) {
            saveSubtask(task);
        }
    };

    private void refreshSubtasks() {
        mSubtasks = mTasksService.loadSubtasksForTask(mTask.getTempId());
        mAdapter.update(mSubtasks);
    }

    public void addSubtask(View view) {
        View title = view.findViewById(R.id.subtask_add_title);
        title.requestFocus();

        showKeyboard();
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
            List<GsonTask> tasksToSave = new ArrayList<>();

            // Create new task.
            GsonTask task = GsonTask.gsonForLocal(null, null, tempId, mTask.getTempId(), currentDate,
                    currentDate, false, title, null, 0, 0, null, currentDate, null, null, RepeatOptions.NEVER,
                    null, null, new ArrayList<GsonTag>(), null, 0);

            // Reorder tasks and save.
            handleOrder(task, tasksToSave);
            mTasksService.saveTasks(tasksToSave, true);

            refreshSubtasks();

            updateViews();

            hideKeyboard();

            if (mListView.getVisibility() == View.GONE) showSubtasks();

            // Send analytics event.
            sendSubtaskAddedEvent((long) title.length());

            // Play sound.
            SoundHandler.playSound(this, R.raw.action_positive);
        }
    }

    private void handleOrder(GsonTask newTask, List<GsonTask> tasksToSave) {
        // Add new task to bottom of the list.
        tasksToSave.addAll(mSubtasks);
        tasksToSave.add(newTask);

        // Reorder tasks.
        for (int i = 0; i < tasksToSave.size(); i++) {
            GsonTask task = tasksToSave.get(i);
            task.setOrder(i);
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

    @OnClick(R.id.subtask_first_buttons_container)
    protected void firstSubtaskButtonClick() {
        mSubtaskFirstButton.setChecked(true);

        mSubtaskFirstItem.animate().alpha(0f).setDuration(Constants.ANIMATION_DURATION_SHORT)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        GsonTask task = getFirstUncompletedSubtask();

                        if (task != null) {
                            task.setLocalCompletionDate(new Date());
                            saveSubtask(task);
                        }

                        mSubtaskFooter.setAlpha(0f);
                        mSubtaskFooter.animate().alpha(1f).setDuration(Constants.ANIMATION_DURATION_SHORT);
                    }
                });

        SoundHandler.playSound(mContext.get(), R.raw.complete_task_1);
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
        mListView.animate().alpha(0f).setDuration(Constants.ANIMATION_DURATION_SHORT)
                .setListener(new AnimatorListenerAdapter() {
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

        mShowActionSteps = false;
    }

    private GsonTask getFirstUncompletedSubtask() {
        for (GsonTask subtask : mSubtasks) {
            if (subtask.getLocalCompletionDate() == null) return subtask;
        }
        return null;
    }

    private View.OnTouchListener mPropertiesTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            mNotes.getParent().requestDisallowInterceptTouchEvent(false);
            return false;
        }
    };

    private View.OnTouchListener mNotesTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            mNotes.getParent().requestDisallowInterceptTouchEvent(true);
            return false;
        }
    };

    @OnClick(R.id.evernote_attachment_container)
    protected void openEvernoteAttachment() {
        // Extract GUID from note.
        String identifier = mEvernoteAttachment.getIdentifier();
        Note note = EvernoteService.noteFromJson(identifier);
        String guid = note != null ? note.getGuid() : null;

        if (guid != null && !guid.isEmpty()) {
            // Open note in Evernote.
            Intent evernoteIntent = new Intent(INTENT_VIEW_NOTE);
            evernoteIntent.putExtra(EXTRA_EVERNOTE_GUID, guid);
            startActivity(evernoteIntent);

            // Send analytics event.
            sendEvernoteOpenEvent();
        }
    }

    private void sendRecurringEvent(String option) {
        // Send analytics event.
        Analytics.sendEvent(Categories.TASKS, Actions.RECURRING, option, null);

        // Update recurring tasks dimension.
        Analytics.sendRecurringTasks(this);

        // Prepare Intercom fields.
        HashMap<String, Object> fields = new HashMap<>();
        fields.put(IntercomFields.REOCURRENCE, option);

        // Send Intercom events.
        IntercomHandler.sendEvent(IntercomEvents.RECURRING_TASK, fields);
    }

    private void sendNoteChangedEvent() {
        long length = (long) mNotes.getText().length();

        // Send analytics event.
        Analytics.sendEvent(Categories.TASKS, Actions.NOTE, null, length);

        // Prepare Intercom fields.
        HashMap<String, Object> fields = new HashMap<>();
        fields.put(IntercomFields.LENGHT, length);

        // Send Intercom events.
        IntercomHandler.sendEvent(IntercomEvents.NOTE, fields);
    }

    private void sendTagAssignEvents() {
        if (mAssignedTagsCount > 0) {
            sendTagAssignEvent();
        }

        if (mUnassignedTagsCount > 0) {
            sendTagUnassignEvent();
        }
    }

    private void sendTagAssignEvent() {
        // Send tags assigned event.
        Analytics.sendEvent(Categories.TAGS, Actions.ASSIGNED_TAGS,
                Labels.TAGS_FROM_EDIT_TASK, (long) mAssignedTagsCount);

        // Prepare Intercom fields.
        HashMap<String, Object> fields = new HashMap<>();
        fields.put(IntercomFields.NUMBER_OF_TASKS, 1);
        fields.put(IntercomFields.NUMBER_OF_TAGS, mAssignedTagsCount);
        fields.put(IntercomFields.FROM, Labels.TAGS_FROM_EDIT_TASK);

        // Send Intercom events.
        IntercomHandler.sendEvent(IntercomEvents.ASSIGN_TAGS, fields);
    }

    private void sendTagUnassignEvent() {
        // Send tags unassigned event.
        Analytics.sendEvent(Categories.TAGS, Actions.UNASSIGNED_TAGS,
                Labels.TAGS_FROM_EDIT_TASK, (long) mUnassignedTagsCount);

        // Prepare Intercom fields.
        HashMap<String, Object> fields = new HashMap<>();
        fields.put(IntercomFields.NUMBER_OF_TASKS, 1);
        fields.put(IntercomFields.NUMBER_OF_TAGS, mUnassignedTagsCount);
        fields.put(IntercomFields.FROM, Labels.TAGS_FROM_EDIT_TASK);

        // Send Intercom events.
        IntercomHandler.sendEvent(IntercomEvents.UNASSIGN_TAGS, fields);
    }

    private void sendDeletedTaskEvent() {
        // Send analytics event.
        Analytics.sendEvent(Categories.TASKS, Actions.DELETED_TASKS, null, 1l);

        // Prepare Intercom fields.
        HashMap<String, Object> fields = new HashMap<>();
        fields.put(IntercomFields.NUMBER_OF_TASKS, 1);

        // Send Intercom events.
        IntercomHandler.sendEvent(IntercomEvents.DELETED_TASKS, fields);
    }

    private void sendSubtaskAddedEvent(long length) {
        // Send analytics event.
        Analytics.sendEvent(Categories.ACTION_STEPS, Actions.ADDED_SUBTASK, Labels.ADDED_FROM_INPUT, length);

        // Prepare Intercom fields.
        HashMap<String, Object> fields = new HashMap<>();
        fields.put(IntercomFields.FROM, Labels.ADDED_FROM_INPUT);
        fields.put(IntercomFields.TASK_ACTION_STEPS, mSubtasks.size());

        // Send Intercom events.
        IntercomHandler.sendEvent(IntercomEvents.ADDED_SUBTASK, fields);
    }

    private void sendSubtaskCompletedEvent() {
        // Send analytics event.
        Analytics.sendEvent(Categories.ACTION_STEPS, Actions.COMPLETED_SUBTASK, null, null);

        // Send Intercom events.
        IntercomHandler.sendEvent(IntercomEvents.COMPLETED_SUBTASK, null);
    }

    private void sendSubtaskDeletedEvent() {
        // Send analytics event.
        Analytics.sendEvent(Categories.ACTION_STEPS, Actions.DELETED_SUBTASK, null, null);

        // Send Intercom events.
        IntercomHandler.sendEvent(IntercomEvents.DELETED_SUBTASK, null);
    }

    private void sendTaskPriorityEvent(String label) {
        // Send analytics event.
        Analytics.sendEvent(Categories.TASKS, Actions.PRIORITY, label, null);

        // Prepare Intercom fields.
        HashMap<String, Object> fields = new HashMap<>();
        fields.put(IntercomFields.ASSIGNED, label);

        // Send Intercom events.
        IntercomHandler.sendEvent(IntercomEvents.PRIORITY, fields);
    }

    private void sendTagAddedEvent(long length) {
        // Send analytics event.
        Analytics.sendEvent(Categories.TAGS, Actions.ADDED_TAG, Labels.TAGS_FROM_EDIT_TASK, length);

        // Prepare Intercom fields.
        HashMap<String, Object> fields = new HashMap<>();
        fields.put(IntercomFields.LENGHT, length);
        fields.put(IntercomFields.FROM, Labels.TAGS_FROM_EDIT_TASK);

        // Send Intercom events.
        IntercomHandler.sendEvent(IntercomEvents.ADDED_TAG, fields);
    }

    private void sendTagDeletedEvent() {
        // Send analytics event.
        Analytics.sendEvent(Categories.TAGS, Actions.DELETED_TAG, Labels.TAGS_FROM_EDIT_TASK, null);

        // Prepare Intercom fields.
        HashMap<String, Object> fields = new HashMap<>();
        fields.put(IntercomFields.FROM, Labels.TAGS_FROM_EDIT_TASK);

        // Send Intercom events.
        IntercomHandler.sendEvent(IntercomEvents.DELETED_TAG, fields);
    }

    private void sendShareTaskEvent() {
        // Send analytics event.
        Analytics.sendEvent(Categories.SHARE_TASK, Actions.SHARE_TASK_OPEN, null, 1l);

        // Prepare Intercom fields.
        HashMap<String, Object> fields = new HashMap<>();
        fields.put(IntercomFields.NUMBER_OF_TASKS, 1);

        // Send Intercom events.
        IntercomHandler.sendEvent(IntercomEvents.SHARE_TASK_OPENED, fields);
    }

    private void sendEvernoteOpenEvent() {
        // Send analytics event.
        Analytics.sendEvent(Categories.ACTIONS, Actions.OPEN_EVERNOTE, null, null);

        // Send Intercom events.
        IntercomHandler.sendEvent(IntercomEvents.OPEN_EVERNOTE, null);
    }

}
