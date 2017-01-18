package com.swipesapp.android.ui.activity;

import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.InputType;
import android.util.DisplayMetrics;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.swipesapp.android.R;
import com.swipesapp.android.analytics.handler.Analytics;
import com.swipesapp.android.analytics.values.Actions;
import com.swipesapp.android.analytics.values.Categories;
import com.swipesapp.android.analytics.values.Labels;
import com.swipesapp.android.handler.SoundHandler;
import com.swipesapp.android.sync.gson.GsonTag;
import com.swipesapp.android.sync.gson.GsonTask;
import com.swipesapp.android.sync.service.SyncService;
import com.swipesapp.android.sync.service.TasksService;
import com.swipesapp.android.ui.listener.KeyboardBackListener;
import com.swipesapp.android.ui.view.ActionEditText;
import com.swipesapp.android.ui.view.FlowLayout;
import com.swipesapp.android.ui.view.SwipesDialog;
import com.swipesapp.android.ui.view.SwipesTextView;
import com.swipesapp.android.util.DateUtils;
import com.swipesapp.android.util.PreferenceUtils;
import com.swipesapp.android.util.ThemeUtils;
import com.swipesapp.android.values.Constants;
import com.swipesapp.android.values.Intents;
import com.swipesapp.android.values.RepeatOptions;
import com.swipesapp.android.values.Sections;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;

public class AddTasksActivity extends BaseActivity {

    @InjectView(R.id.add_task_container)
    RelativeLayout mContainer;

    @InjectView(R.id.add_task_fields_container)
    RelativeLayout mFieldsContainer;

    @InjectView(R.id.add_task_title)
    ActionEditText mEditTextTitle;

    @InjectView(R.id.add_task_priority)
    CheckBox mButtonPriority;

    @InjectView(R.id.add_task_tags_container)
    FlowLayout mTagsContainer;

    @InjectView(R.id.add_task_snooze_checkbox)
    CheckBox mSnoozeCheckbox;

    private WeakReference<Context> mContext;
    private TasksService mTasksService;
    private SyncService mSyncService;

    private boolean mOpenedFromWidget;
    private boolean mIsSnoozing;
    private String[] mIntentData;

    private static Set<GsonTag> sSelectedTags;
    private static String sTitle;
    private static boolean sPriority;
    private static Date sSnoozeTime;
    private static boolean sHasSnoozed;

    private float mFieldsTranslationY;
    private boolean mHasStartedTimer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(ThemeUtils.isLightTheme(this) ? R.style.Tasks_Theme_Light : R.style.Tasks_Theme_Dark);
        setContentView(R.layout.activity_add_tasks);
        ButterKnife.inject(this);

        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);

        getWindow().getDecorView().setBackgroundColor(ThemeUtils.getNeutralBackgroundColor(this));

        mContext = new WeakReference<Context>(this);
        mTasksService = TasksService.getInstance();
        mSyncService = SyncService.getInstance();

        getSupportActionBar().hide();

        themeStatusBar(ThemeUtils.getStatusBarColor(this));

        if (sSelectedTags == null || sSelectedTags.isEmpty()) {
            sSelectedTags = new LinkedHashSet<>();

            List<Integer> tagIds = getIntent().getIntegerArrayListExtra(Constants.EXTRA_TAG_IDS);

            // Load tags from filter.
            if (tagIds != null) {
                for (Integer id : tagIds) {
                    GsonTag tag = mTasksService.loadTag(id.longValue());
                    if (tag != null) sSelectedTags.add(tag);
                }
            }
        }

        mOpenedFromWidget = getIntent().getBooleanExtra(Constants.EXTRA_FROM_WIDGET, false);

        mFieldsTranslationY = mFieldsContainer.getTranslationY();

        handleShareIntent();

        prepareFields();

        loadTags();

        showViews();
    }

    @Override
    public void finish() {
        super.finish();

        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
    }

    @Override
    protected void onUserLeaveHint() {
        // Close when opened from the widget and user presses the home key.
        if (mOpenedFromWidget && !mIsSnoozing) cancelAddTask();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == Constants.SNOOZE_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                // Set snooze time.
                if (data != null) {
                    String date = data.getStringExtra(Constants.EXTRA_SNOOZE_TIME);
                    sSnoozeTime = DateUtils.dateFromSync(date);

                    // Set success flag.
                    sHasSnoozed = true;
                }
            } else {
                // Uncheck snooze checkbox.
                mSnoozeCheckbox.setChecked(false);
            }

            // Reset flag.
            mIsSnoozing = false;
        }
    }

    private void handleShareIntent() {
        // Handle intent from other apps.
        Intent intent = getIntent();
        String action = intent.getAction();

        if (action != null && (action.equals(Intent.ACTION_SEND) ||
                action.equals(Intent.ACTION_SEND_MULTIPLE) || action.equals(Intents.ADD_TASK))) {

            String title = intent.getStringExtra(Intent.EXTRA_SUBJECT);
            String notes = intent.getStringExtra(Intent.EXTRA_TEXT);

            if (title == null || title.isEmpty()) title = "";

            if (notes != null && !notes.startsWith("http")) {
                notes = notes.replaceAll("http[^ ]+$", "");
            }

            mIntentData = new String[]{title, notes};
        }
    }

    private void prepareFields() {
        // Customize colors.
        int hintColor = ThemeUtils.isLightTheme(this) ? R.color.light_hint : R.color.dark_hint;
        mEditTextTitle.setHintTextColor(getResources().getColor(hintColor));

        // Listeners for input field.
        mEditTextTitle.setOnEditorActionListener(mEnterListener);
        mEditTextTitle.setListener(mKeyboardBackListener);

        // Focus on input field.
        mEditTextTitle.requestFocus();

        // Restore state of fields.
        if (sTitle != null) mEditTextTitle.setText(sTitle);
        mButtonPriority.setChecked(sPriority);
        mSnoozeCheckbox.setChecked(sHasSnoozed);

        // Load title from other apps.
        if (mIntentData != null) {
            String title = mIntentData[0];
            mEditTextTitle.setText(title);
        }
    }

    @OnClick(R.id.add_task_priority_container)
    protected void togglePriority() {
        boolean checked = mButtonPriority.isChecked();
        mButtonPriority.setChecked(!checked);
    }

    @OnClick(R.id.button_confirm_add_task)
    protected void confirmAddTask() {
        Date currentDate = new Date();
        String title = mEditTextTitle.getText().toString();
        Integer priority = mButtonPriority.isChecked() ? 1 : 0;
        String tempId = UUID.randomUUID().toString();
        String notes = null;
        List<GsonTag> tags = new ArrayList<>(sSelectedTags);

        // Load note from intent.
        if (mIntentData != null) {
            notes = mIntentData[1];
        }

        // Set snooze time if not manually selected.
        if (!sHasSnoozed) sSnoozeTime = currentDate;

        if (!title.isEmpty()) {
            List<GsonTask> tasksToSave = new ArrayList<>();

            // Create new task.
            GsonTask task = GsonTask.gsonForLocal(null, null, tempId, null, currentDate, currentDate, false, title, notes, 0,
                    priority, null, sSnoozeTime, null, null, RepeatOptions.NEVER, null, null, tags, null, 0);

            // Reorder tasks and save.
            handleOrder(task, tasksToSave);
            mTasksService.saveTasks(tasksToSave, true);

            // Show success message when needed.
            if (mIntentData != null || mOpenedFromWidget) {
                Toast.makeText(this, getString(R.string.share_intent_add_confirm), Toast.LENGTH_SHORT).show();
            }

            // Send analytics event.
            sendTaskAddedEvent();

            // Reset state of fields.
            sTitle = null;
            sPriority = false;
            sSelectedTags.clear();
            sSnoozeTime = null;
            sHasSnoozed = false;

            // Refresh widget and tasks.
            TasksActivity.refreshWidgets(this);
            TasksActivity.setPendingRefresh();

            // Set result after adding task.
            Intent data = new Intent();
            data.putExtra(Constants.EXTRA_TASK_ID, tempId);

            if (mSnoozeCheckbox.isChecked()) {
                setResult(Constants.ADDED_SNOOZED_TASK_RESULT_CODE, data);
            } else {
                setResult(RESULT_OK, data);
            }

            // Play sound.
            SoundHandler.playSound(this, R.raw.action_positive);

            // Fade out views.
            hideViews();

            finish();
        } else {
            cancelAddTask();
        }
    }

    private void handleOrder(GsonTask newTask, List<GsonTask> tasksToSave) {
        boolean addToBottom = PreferenceUtils.readBoolean(PreferenceUtils.ADD_TO_BOTTOM_KEY, this);
        List<GsonTask> focusedTasks = mTasksService.loadFocusedTasks();

        if (addToBottom) {
            // Add new task to bottom of the list.
            tasksToSave.addAll(focusedTasks);
            tasksToSave.add(newTask);
        } else {
            // Add new task to top of the list.
            tasksToSave.add(newTask);
            tasksToSave.addAll(focusedTasks);
        }

        // Reorder tasks.
        for (int i = 0; i < tasksToSave.size(); i++) {
            GsonTask task = tasksToSave.get(i);
            task.setOrder(i);
        }
    }

    private void cancelAddTask() {
        // Save state of fields.
        sTitle = mEditTextTitle.getText().toString();

        if (!sTitle.isEmpty()) {
            sPriority = mButtonPriority.isChecked();
        } else {
            sPriority = false;
            sSelectedTags.clear();
            sSnoozeTime = null;
            sHasSnoozed = false;
        }

        // Fade out views.
        hideViews();

        finish();
    }

    private TextView.OnEditorActionListener mEnterListener =
            new TextView.OnEditorActionListener() {
                @Override
                public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                    if (actionId == EditorInfo.IME_ACTION_DONE) {
                        // If the action is a key-up event on the return key, save new task.
                        confirmAddTask();
                    }
                    return true;
                }
            };

    private KeyboardBackListener mKeyboardBackListener = new KeyboardBackListener() {
        @Override
        public void onKeyboardBackPressed() {
            // Back button has been pressed. Stop adding task.
            cancelAddTask();
        }
    };

    private void showViews() {
        // HACK: Fallback to a timer if there isn't a soft keyboard.
        final Handler handler = new Handler();
        final Runnable runnable = new Runnable() {
            @Override
            public void run() {
                // Animate tags from top to bottom.
                animateView(mTagsContainer, 0, false);

                // Animate fields from bottom to top.
                animateView(mFieldsContainer, mFieldsTranslationY, true);

                // Flag timer as finished.
                mHasStartedTimer = false;
            }
        };

        // HACK: Wait for keyboard to appear to avoid breaking animation.
        mContainer.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                int heightDiff = mContainer.getRootView().getHeight() - mContainer.getHeight();
                int minKeyboardHeight = mContext.get().getResources().getDimensionPixelSize(R.dimen.keyboard_minimum_height);

                // Use layout height difference to detect keyboard.
                if (heightDiff > minKeyboardHeight) {
                    // Animate tags from top to bottom.
                    animateView(mTagsContainer, 0, false);

                    // Animate fields from bottom to top.
                    animateView(mFieldsContainer, mFieldsTranslationY, true);

                    // Animation was triggered. Cancel timer.
                    handler.removeCallbacks(runnable);
                    mHasStartedTimer = false;

                    // Remove layout listener.
                    mContainer.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                } else if (!mHasStartedTimer) {
                    // No keyboard yet. Start fallback timer.
                    handler.postDelayed(runnable, 400);

                    // Flag timer as started.
                    mHasStartedTimer = true;
                }
            }
        });
    }

    private void hideViews() {
        // Fade out tags and fields.
        mTagsContainer.animate().alpha(0f).setDuration(Constants.ANIMATION_DURATION_SHORT).start();
        mFieldsContainer.animate().alpha(0f).setDuration(Constants.ANIMATION_DURATION_SHORT).start();
    }

    private void animateView(View view, float toY, boolean isReverse) {
        DisplayMetrics displaymetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);

        // Determine initial position.
        float fromY = isReverse ? displaymetrics.heightPixels : -displaymetrics.heightPixels;

        // Show container.
        view.setVisibility(View.VISIBLE);

        // Animate translation.
        ObjectAnimator animator = ObjectAnimator.ofFloat(view, "translationY", fromY, toY);
        animator.setDuration(Constants.ANIMATION_DURATION_LONG).start();
    }

    private void loadTags() {
        List<GsonTag> tags = mTasksService.loadAllTags();
        mTagsContainer.removeAllViews();

        // For each tag, add a checkbox as child view.
        for (GsonTag tag : tags) {
            int resource = ThemeUtils.isLightTheme(this) ? R.layout.tag_box_light : R.layout.tag_box_dark;
            CheckBox tagBox = (CheckBox) getLayoutInflater().inflate(resource, null);
            tagBox.setText(tag.getTitle());
            tagBox.setId(tag.getId().intValue());

            // Set listener to assign tag.
            tagBox.setOnClickListener(mTagClickListener);
            tagBox.setOnLongClickListener(mEditTagListener);

            // Pre-select tag if needed.
            if (sSelectedTags.contains(tag)) tagBox.setChecked(true);

            // Add child view.
            mTagsContainer.addView(tagBox);
        }

        // Create add tag button.
        SwipesTextView button = (SwipesTextView) getLayoutInflater().inflate(R.layout.tag_add_button, null);
        button.setOnClickListener(mAddTagListener);
        button.enableTouchFeedback();

        // Add child view.
        mTagsContainer.addView(button);
    }

    private View.OnClickListener mTagClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            GsonTag selectedTag = mTasksService.loadTag((long) view.getId());

            // Add or remove from list of selected tags.
            if (isTagSelected(selectedTag)) {
                removeSelectedTag(selectedTag);
            } else {
                sSelectedTags.add(selectedTag);
            }
        }
    };

    private View.OnLongClickListener mEditTagListener = new View.OnLongClickListener() {
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
                .actionsColor(ThemeUtils.getSectionColor(Sections.FOCUS, mContext.get()))
                .callback(new MaterialDialog.ButtonCallback() {
                    @Override
                    public void onPositive(MaterialDialog dialog) {
                        // Delete tag and unassign it from all tasks.
                        mTasksService.deleteTag(selectedTag.getId());

                        // Send analytics event.
                        Analytics.sendEvent(Categories.TAGS, Actions.DELETED_TAG, Labels.TAGS_FROM_ADD_TASK, null);

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

    View.OnClickListener mAddTagListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            // Create tag title input.
            final ActionEditText input = new ActionEditText(mContext.get());
            input.setHint(getString(R.string.add_tag_dialog_hint));
            input.setHintTextColor(ThemeUtils.getHintColor(mContext.get()));
            input.setTextColor(ThemeUtils.getTextColor(mContext.get()));
            input.setInputType(InputType.TYPE_CLASS_TEXT);
            input.requestFocus();

            // Display dialog to save new tag.
            final SwipesDialog dialog = new SwipesDialog.Builder(mContext.get())
                    .title(R.string.add_tag_dialog_title)
                    .positiveText(R.string.add_tag_dialog_yes)
                    .negativeText(R.string.add_tag_dialog_no)
                    .actionsColor(ThemeUtils.getSectionColor(Sections.FOCUS, mContext.get()))
                    .customView(customizeAddTagInput(input), false)
                    .callback(new MaterialDialog.ButtonCallback() {
                        @Override
                        public void onPositive(MaterialDialog dialog) {
                            String title = input.getText().toString();

                            if (!title.isEmpty()) {
                                // Save new tag.
                                confirmAddTag(title);
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
    };

    private void confirmAddTag(String title) {
        // Save new tag to database.
        long id = mTasksService.createTag(title);
        GsonTag tag = mTasksService.loadTag(id);

        // Send analytics event.
        Analytics.sendEvent(Categories.TAGS, Actions.ADDED_TAG, Labels.TAGS_FROM_ADD_TASK, (long) title.length());

        // Refresh displayed tags.
        sSelectedTags.add(tag);
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
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        int margin = getResources().getDimensionPixelSize(R.dimen.add_tag_input_margin);
        params.setMargins(margin, 0, margin, 0);

        // Wrap input inside layout.
        layout.addView(input, params);
        return layout;
    }

    private void showKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.toggleSoftInput(0, 0);
    }

    private boolean isTagSelected(GsonTag selectedTag) {
        // Check if tag already exists in the list of selected.
        for (GsonTag tag : sSelectedTags) {
            if (tag.getId().equals(selectedTag.getId())) {
                return true;
            }
        }
        return false;
    }

    private void removeSelectedTag(GsonTag selectedTag) {
        // Find and remove tag from the list of selected.
        List<GsonTag> selected = new ArrayList<GsonTag>(sSelectedTags);
        for (GsonTag tag : selected) {
            if (tag.getId().equals(selectedTag.getId())) {
                sSelectedTags.remove(tag);
            }
        }
    }

    @OnClick(R.id.add_task_snooze_checkbox)
    protected void setSnooze() {
        if (!sHasSnoozed) {
            // Call snooze activity.
            Intent intent = new Intent(this, SnoozeActivity.class);
            intent.putExtra(Constants.EXTRA_TASK_ID, 0L);
            intent.putExtra(Constants.EXTRA_NEW_TASK_MODE, true);
            startActivityForResult(intent, Constants.SNOOZE_REQUEST_CODE);

            // Set flag to prevent closing.
            mIsSnoozing = true;
        } else {
            // Remove snooze.
            sSnoozeTime = null;
            sHasSnoozed = false;
        }
    }

    private void sendTaskAddedEvent() {
        String label = mIntentData != null ? Labels.ADDED_FROM_SHARE_INTENT : Labels.ADDED_FROM_INPUT;
        long value = mEditTextTitle.getText().length();

        // Send task added event.
        Analytics.sendEvent(Categories.TASKS, Actions.ADDED_TASK, label, value);
    }

}
