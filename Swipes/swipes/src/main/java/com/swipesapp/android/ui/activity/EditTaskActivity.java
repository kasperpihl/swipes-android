package com.swipesapp.android.ui.activity;

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
import android.widget.TextView;
import android.widget.Toast;

import com.negusoft.holoaccent.activity.AccentActivity;
import com.negusoft.holoaccent.dialog.AccentAlertDialog;
import com.swipesapp.android.R;
import com.swipesapp.android.sync.gson.GsonTag;
import com.swipesapp.android.sync.gson.GsonTask;
import com.swipesapp.android.sync.service.TasksService;
import com.swipesapp.android.ui.listener.KeyboardBackListener;
import com.swipesapp.android.ui.view.ActionEditText;
import com.swipesapp.android.ui.view.BlurBuilder;
import com.swipesapp.android.ui.view.FlowLayout;
import com.swipesapp.android.ui.view.SwipesTextView;
import com.swipesapp.android.util.Constants;
import com.swipesapp.android.util.DateUtils;
import com.swipesapp.android.util.ThemeUtils;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;

public class EditTaskActivity extends AccentActivity {

    @InjectView(R.id.edit_task_container)
    FrameLayout mContainer;

    @InjectView(R.id.edit_task_view)
    LinearLayout mView;

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

    private static final String TAG_SEPARATOR = ", ";

    private WeakReference<Context> mContext;

    private TasksService mTasksService;

    private Long mId;

    private GsonTask mTask;

    private List<GsonTag> mAssignedTags;

    public static BitmapDrawable sBlurDrawable;

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

        mContainer.setBackgroundColor(ThemeUtils.getBackgroundColor(this));
        mContainer.requestFocus();

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

    private void updateViews() {
        mTask = mTasksService.loadTask(mId);

        mTitle.setText(mTask.getTitle());

        mPriority.setChecked(mTask.getPriority() == 1);

        mSchedule.setText(DateUtils.formatToRecent(mTask.getSchedule(), this));

        mRepeat.setText(getString(R.string.edit_task_repeat_default_mode));

        mTags.setText(buildFormattedTags());

        mNotes.setText(mTask.getNotes());
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
                        performChanges();
                    }
                    return true;
                }
            };

    private KeyboardBackListener mKeyboardBackListener = new KeyboardBackListener() {
        @Override
        public void onKeyboardBackPressed() {
            discardChanges();
        }
    };

    private void hideKeyboard() {
        InputMethodManager inputManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        inputManager.toggleSoftInput(0, 0);

        // Remove focus from text views by focusing on main layout.
        mContainer.requestFocus();
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
        openSnoozeSelector();
    }

    @OnClick(R.id.repeat_container)
    protected void setRepeat() {
        Toast.makeText(getApplicationContext(), "Repeat coming soon", Toast.LENGTH_SHORT).show();
    }

    @OnClick(R.id.tags_container)
    protected void setTags() {
        // Apply blur to the tags background.
        int alphaColor = ThemeUtils.getTasksBlurAlphaColor(this);
        updateBlurDrawable(alphaColor);
        mTagsArea.setBackgroundDrawable(getBlurDrawable());

        mView.setVisibility(View.GONE);

        // Show tags area with fade animation.
        mTagsArea.setVisibility(View.VISIBLE);
        mTagsArea.setAlpha(0f);
        mTagsArea.animate().alpha(1f).setDuration(Constants.ANIMATION_DURATION_MEDIUM);

        // Hide action bar.
        getActionBar().hide();

        loadTags();
    }

    private void performChanges() {
        // Save updated properties.
        mTask.setTitle(mTitle.getText().toString());
        mTask.setNotes(mNotes.getText().toString());
        mTask.setTags(mAssignedTags);
        mTasksService.saveTask(mTask);

        hideKeyboard();

        updateViews();
    }

    private void discardChanges() {
        hideKeyboard();
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
        mView.setVisibility(View.VISIBLE);
        mView.setAlpha(0f);
        mView.animate().alpha(1f).setDuration(Constants.ANIMATION_DURATION_MEDIUM);

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
        mTask.setTags(mAssignedTags);
        mTasksService.saveTask(mTask);
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
        content += "\n" + getString(R.string.share_message_footer);

        Intent inviteIntent = new Intent(Intent.ACTION_SEND);
        inviteIntent.setType("text/html");
        inviteIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, getString(R.string.share_message_subject));
        inviteIntent.putExtra(android.content.Intent.EXTRA_TEXT, content);

        startActivity(Intent.createChooser(inviteIntent, getString(R.string.invite_chooser_title)));
    }

}
