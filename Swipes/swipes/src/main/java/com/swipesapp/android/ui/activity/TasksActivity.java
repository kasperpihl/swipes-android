package com.swipesapp.android.ui.activity;

import android.animation.ObjectAnimator;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.view.ViewPager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.fortysevendeg.swipelistview.DynamicViewPager;
import com.melnykov.fab.FloatingActionButton;
import com.swipesapp.android.R;
import com.swipesapp.android.sync.gson.GsonTag;
import com.swipesapp.android.sync.gson.GsonTask;
import com.swipesapp.android.sync.receiver.SnoozeReceiver;
import com.swipesapp.android.sync.service.SyncService;
import com.swipesapp.android.sync.service.TasksService;
import com.swipesapp.android.ui.adapter.SectionsPagerAdapter;
import com.swipesapp.android.ui.fragments.TasksListFragment;
import com.swipesapp.android.ui.listener.KeyboardBackListener;
import com.swipesapp.android.ui.view.ActionEditText;
import com.swipesapp.android.ui.view.FactorSpeedScroller;
import com.swipesapp.android.ui.view.FlowLayout;
import com.swipesapp.android.ui.view.SwipesButton;
import com.swipesapp.android.util.ColorUtils;
import com.swipesapp.android.util.Constants;
import com.swipesapp.android.util.DeviceUtils;
import com.swipesapp.android.util.PreferenceUtils;
import com.swipesapp.android.util.ThemeUtils;
import com.swipesapp.android.values.Actions;
import com.swipesapp.android.values.RepeatOptions;
import com.swipesapp.android.values.Sections;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;

public class TasksActivity extends BaseActivity {

    @InjectView(R.id.tasks_area)
    RelativeLayout mTasksArea;

    @InjectView(R.id.pager)
    DynamicViewPager mViewPager;

    @InjectView(R.id.button_add_task)
    FloatingActionButton mButtonAddTask;

    @InjectView(R.id.add_task_container)
    RelativeLayout mAddTaskContainer;

    @InjectView(R.id.edit_text_add_task_content)
    ActionEditText mEditTextAddNewTask;

    @InjectView(R.id.button_confirm_add_task)
    SwipesButton mButtonConfirmAddTask;

    @InjectView(R.id.button_add_task_priority)
    CheckBox mButtonAddTaskPriority;

    @InjectView(R.id.edit_tasks_bar)
    LinearLayout mEditTasksBar;

    @InjectView(R.id.button_edit_task)
    SwipesButton mButtonEditTask;

    @InjectView(R.id.add_task_tag_container)
    FlowLayout mAddTaskTagContainer;

    @InjectView(R.id.action_buttons_container)
    LinearLayout mActionButtonsContainer;

    private static final String LOG_TAG = TasksActivity.class.getSimpleName();

    private static final int ACTION_MULTI_SELECT = 0;
    private static final int ACTION_SEARCH = 1;
    private static final int ACTION_WORKSPACES = 2;
    private static final int ACTION_SETTINGS = 3;

    private WeakReference<Context> mContext;

    private TasksService mTasksService;

    private SectionsPagerAdapter mSectionsPagerAdapter;

    private static Sections sCurrentSection;

    private List<GsonTag> mSelectedTags;

    // Used by animator to store tags container position.
    private float mTagsTranslationY;

    private View mActionBarView;

    private float mPreviousOffset;

    private boolean mHasChangedTab;

    private boolean mIsAddingTask;

    private String mShareMessage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(ThemeUtils.getThemeResource(this));
        setContentView(R.layout.activity_tasks);
        ButterKnife.inject(this);

        getWindow().getDecorView().setBackgroundColor(ThemeUtils.getNeutralBackgroundColor(this));

        mContext = new WeakReference<Context>(this);
        mTasksService = TasksService.getInstance(this);

        LayoutInflater inflater = LayoutInflater.from(this);
        mActionBarView = inflater.inflate(R.layout.action_bar_custom_view, null);

        getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        getSupportActionBar().setDisplayShowCustomEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        getSupportActionBar().setCustomView(mActionBarView);

        createSnoozeAlarm();

        if (sCurrentSection == null) sCurrentSection = Sections.FOCUS;

        setupViewPager();

        setupSystemBars(sCurrentSection);

        // Define a custom duration to the page scroller, providing a more natural feel.
        customizeScroller();

        mSelectedTags = new ArrayList<GsonTag>();

        mTagsTranslationY = mAddTaskTagContainer.getTranslationY();

        // HACK: Flip add task confirm button, so the arrow points to the right.
        mButtonConfirmAddTask.setScaleX(-mButtonConfirmAddTask.getScaleX());

        int hintColor = ThemeUtils.isLightTheme(this) ? R.color.light_hint : R.color.dark_hint;
        mEditTextAddNewTask.setHintTextColor(getResources().getColor(hintColor));

        mEditTextAddNewTask.setListener(mKeyboardBackListener);
    }

    @Override
    protected void onDestroy() {
        ButterKnife.reset(this);
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        // Forward call to listeners.
        mTasksService.sendBroadcast(Actions.BACK_PRESSED);
    }

    @Override
    public void onResume() {
        // Sync only changes after initial sync has been performed.
        boolean changesOnly = PreferenceUtils.getSyncLastUpdate(this) != null;
        SyncService.getInstance(this).performSync(changesOnly);

        super.onResume();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == Constants.SETTINGS_REQUEST_CODE) {
            switch (resultCode) {
                case Constants.THEME_CHANGED_RESULT_CODE:
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            // Theme has changed. Reload activity.
                            recreate();
                        }
                    }, 1);
                    break;
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // TODO: Show standard actions.
        menu.add(Menu.NONE, ACTION_MULTI_SELECT, Menu.NONE, getResources().getString(R.string.tasks_list_multi_select_action))
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        menu.add(Menu.NONE, ACTION_SEARCH, Menu.NONE, getResources().getString(R.string.tasks_list_search_action))
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        menu.add(Menu.NONE, ACTION_WORKSPACES, Menu.NONE, getResources().getString(R.string.tasks_list_workspaces_action))
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        menu.add(Menu.NONE, ACTION_SETTINGS, Menu.NONE, getResources().getString(R.string.tasks_list_settings_action))
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);

        // TODO: Show icons.

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case ACTION_MULTI_SELECT:
                // TODO: New selection UI.
                break;
            case ACTION_SEARCH:
                // TODO: New search UI.
                break;
            case ACTION_WORKSPACES:
                // TODO: Implement workspaces.
                break;
            case ACTION_SETTINGS:
                Intent intent = new Intent(this, SettingsActivity.class);
                startActivityForResult(intent, Constants.SETTINGS_REQUEST_CODE);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void setupSystemBars(Sections section) {
        // Apply colors.
        themeActionBar(ThemeUtils.getSectionColor(section, this));
        themeStatusBar(ThemeUtils.getSectionColorDark(section, this));

        // TODO: Set section title and icon.
    }

    private void createSnoozeAlarm() {
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(this, SnoozeReceiver.class);
        PendingIntent alarmIntent = PendingIntent.getBroadcast(this, 0, intent, 0);

        alarmManager.setInexactRepeating(AlarmManager.ELAPSED_REALTIME, 60000, 60000, alarmIntent);
    }

    private void setupViewPager() {
        if (DeviceUtils.isLandscape(this)) mSimpleOnPageChangeListener = null;

        mSectionsPagerAdapter = new SectionsPagerAdapter(getFragmentManager(), this);
        mViewPager.setAdapter(mSectionsPagerAdapter);
        mViewPager.setOnPageChangeListener(mSimpleOnPageChangeListener);
        mViewPager.setOffscreenPageLimit(Sections.getSectionsCount());
        mViewPager.setPageMargin(getResources().getDimensionPixelSize(R.dimen.pager_margin_sides));
        mViewPager.setCurrentItem(sCurrentSection.getSectionNumber());

        // TODO: Find out why over scroll is buggy on tablets and turn it back on.
        if (DeviceUtils.isTablet(this)) mViewPager.setOverScrollMode(View.OVER_SCROLL_NEVER);
    }

    private ViewPager.SimpleOnPageChangeListener mSimpleOnPageChangeListener = new ViewPager.SimpleOnPageChangeListener() {
        @Override
        public void onPageSelected(int position) {
            sCurrentSection = Sections.getSectionByNumber(position);

            themeRecentsHeader(ThemeUtils.getSectionColor(sCurrentSection, mContext.get()));

            mHasChangedTab = true;
        }

        @Override
        public void onPageScrollStateChanged(int state) {
            if (state == ViewPager.SCROLL_STATE_IDLE) {
                if (mHasChangedTab) {
                    // Notify listeners that current tab has changed.
                    mTasksService.sendBroadcast(Actions.TAB_CHANGED);
                    mHasChangedTab = false;
                }

                mActionBarView.setAlpha(1f);
            }
        }

        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            // Protect against index out of bound.
            if (position >= mSectionsPagerAdapter.getCount() - 1) {
                return;
            }

            // Retrieve the current and next sections.
            Sections from = Sections.getSectionByNumber(position);
            Sections to = Sections.getSectionByNumber(position + 1);

            // Load colors for sections.
            int fromColor = ThemeUtils.getSectionColor(from, mContext.get());
            int toColor = ThemeUtils.getSectionColor(to, mContext.get());

            // Blend the colors and adjust the ActionBar.
            int blended = ColorUtils.blendColors(fromColor, toColor, positionOffset);
            themeActionBar(blended);

            // Load dark colors for sections.
            fromColor = ThemeUtils.getSectionColorDark(from, mContext.get());
            toColor = ThemeUtils.getSectionColorDark(to, mContext.get());

            // Blend the colors and adjust the status bar.
            blended = ColorUtils.blendColors(fromColor, toColor, positionOffset);
            if (!mIsAddingTask) themeStatusBar(blended);

            // Fade ActionBar content gradually.
            fadeActionBar(positionOffset);
        }
    };

    private void fadeActionBar(float positionOffset) {
        // TODO: Set text and icons properly.
//        TextView title = (TextView) mActionBarView.findViewById(R.id.action_bar_title);
//        title.setText(from.getSectionNumber() != 1 ? "LATER" : "TODAY");

        if (mPreviousOffset > 0) {
            if (positionOffset > mPreviousOffset) {
                // Swiping to the right of the ViewPager.
                if (positionOffset < 0.5) {
                    // Fade out until half of the way.
                    mActionBarView.setAlpha(1 - positionOffset * 2);
                } else {
                    // Fade in from half to the the end.
                    mActionBarView.setAlpha((positionOffset - 0.5f) * 2);
                }
            } else {
                // Swiping to the left of the ViewPager.
                if (positionOffset > 0.5) {
                    // Fade out until half of the way.
                    mActionBarView.setAlpha(positionOffset / 2);
                } else {
                    // Fade in from half to the the end.
                    mActionBarView.setAlpha((0.5f - positionOffset) * 2);
                }
            }
        }

        mPreviousOffset = positionOffset;
    }

    /**
     * Returns the current section being displayed.
     *
     * @return Current section.
     */
    public static Sections getCurrentSection() {
        return sCurrentSection;
    }

    /**
     * Clears the current section being displayed.
     */
    public static void clearCurrentSection() {
        sCurrentSection = null;
    }

    /**
     * @return The ViewPager being used.
     */
    public DynamicViewPager getViewPager() {
        return mViewPager;
    }

    private void customizeScroller() {
        try {
            // HACK: Use reflection to access the scroller and customize it.
            Field scroller = ViewPager.class.getDeclaredField("mScroller");
            scroller.setAccessible(true);
            scroller.set(mViewPager, new FactorSpeedScroller(this));
        } catch (Exception e) {
            Log.e(LOG_TAG, "Something went wrong accessing field \"mScroller\" inside ViewPager class", e);
        }
    }

    /**
     * Shows the task edit bar.
     *
     * @param isBatchOperation True when multiple tasks are selected.
     */
    public void showEditBar(boolean isBatchOperation) {
        // Animate views only when necessary.
        if (mEditTasksBar.getVisibility() == View.GONE) {
            Animation slideDown = AnimationUtils.loadAnimation(this, R.anim.slide_down);
            slideDown.setAnimationListener(mShowEditBarListener);
            mButtonAddTask.startAnimation(slideDown);

            Animation slideUp = AnimationUtils.loadAnimation(this, R.anim.slide_up);
            mEditTasksBar.startAnimation(slideUp);
        }

        // The edit button shouldn't be used for multiple tasks at once.
        if (isBatchOperation && mButtonEditTask.isEnabled()) {
            // Animate view.
            Animation slideDown = AnimationUtils.loadAnimation(this, R.anim.slide_down);
            slideDown.setFillAfter(true);
            slideDown.setDuration(Constants.ANIMATION_DURATION_SHORT);
            mButtonEditTask.startAnimation(slideDown);

            // Disable button.
            mButtonEditTask.setEnabled(false);
        } else if (!isBatchOperation && !mButtonEditTask.isEnabled()) {
            // Animate view.
            Animation slideUp = AnimationUtils.loadAnimation(this, R.anim.slide_up);
            slideUp.setFillAfter(true);
            slideUp.setDuration(Constants.ANIMATION_DURATION_SHORT);
            mButtonEditTask.startAnimation(slideUp);

            // Enable button.
            mButtonEditTask.setEnabled(true);
        }
    }

    public void hideEditBar() {
        // Clear container color.
        mActionButtonsContainer.setBackgroundColor(Color.TRANSPARENT);

        // Animate views only when necessary.
        if (mEditTasksBar.isShown()) {
            Animation slideDown = AnimationUtils.loadAnimation(this, R.anim.slide_down);
            slideDown.setAnimationListener(mHideEditBarListener);
            mEditTasksBar.startAnimation(slideDown);

            Animation slideUp = AnimationUtils.loadAnimation(this, R.anim.slide_up);
            mButtonAddTask.startAnimation(slideUp);
        }
    }

    Animation.AnimationListener mShowEditBarListener = new Animation.AnimationListener() {
        @Override
        public void onAnimationStart(Animation animation) {
            mEditTasksBar.setVisibility(View.VISIBLE);
        }

        @Override
        public void onAnimationEnd(Animation animation) {
            mButtonAddTask.setVisibility(View.GONE);

            // Apply container color.
            mActionButtonsContainer.setBackgroundColor(ThemeUtils.getBackgroundColor(mContext.get()));
        }

        @Override
        public void onAnimationRepeat(Animation animation) {
        }
    };

    Animation.AnimationListener mHideEditBarListener = new Animation.AnimationListener() {
        @Override
        public void onAnimationStart(Animation animation) {
            mButtonAddTask.setVisibility(View.VISIBLE);
        }

        @Override
        public void onAnimationEnd(Animation animation) {
            mEditTasksBar.setVisibility(View.GONE);
        }

        @Override
        public void onAnimationRepeat(Animation animation) {
        }
    };

    @OnClick(R.id.button_confirm_add_task)
    protected void confirmAddTask() {
        Date currentDate = new Date();
        String title = mEditTextAddNewTask.getText().toString();
        Integer priority = mButtonAddTaskPriority.isChecked() ? 1 : 0;
        String tempId = UUID.randomUUID().toString();

        if (!title.isEmpty()) {
            GsonTask task = GsonTask.gsonForLocal(null, null, tempId, null, currentDate, currentDate, false, title, null, 0, priority, null, currentDate, null, null, RepeatOptions.NEVER.getValue(), null, null, mSelectedTags, null, 0);
            mTasksService.saveTask(task, true);
        }

        endAddTaskWorkflow(true);
    }

    @OnClick(R.id.button_add_task)
    protected void startAddTaskWorkflow() {
        // Set flag.
        mIsAddingTask = true;

        // Go to main fragment if needed.
        if (sCurrentSection != Sections.FOCUS) {
            mViewPager.setCurrentItem(Sections.FOCUS.getSectionNumber());
        }

        // Fade out the tasks.
        mTasksArea.animate().alpha(0f).setDuration(Constants.ANIMATION_DURATION_LONG);
        transitionStatusBar(ThemeUtils.getStatusBarColor(this));

        // Show and hide keyboard automatically.
        mEditTextAddNewTask.setOnFocusChangeListener(mFocusListener);
        mEditTextAddNewTask.setOnEditorActionListener(mEnterListener);

        // Display edit text.
        mEditTextAddNewTask.setVisibility(View.VISIBLE);
        mEditTextAddNewTask.setFocusable(true);
        mEditTextAddNewTask.setFocusableInTouchMode(true);
        mEditTextAddNewTask.requestFocus();
        mEditTextAddNewTask.bringToFront();

        // Display add task area.
        mAddTaskContainer.setVisibility(View.VISIBLE);

        // Display tags area.
        animateTags(false);
        loadTags();
    }

    @OnClick(R.id.add_task_container)
    protected void addTaskAreaClick() {
        endAddTaskWorkflow(false);
    }

    private void endAddTaskWorkflow(boolean resetFields) {
        // Reset flag.
        mIsAddingTask = false;

        // Remove focus and hide text view.
        mEditTextAddNewTask.clearFocus();
        mEditTextAddNewTask.setVisibility(View.GONE);

        // Reset fields.
        if (resetFields) {
            mEditTextAddNewTask.setText("");
            mButtonAddTaskPriority.setChecked(false);
            mSelectedTags.clear();
        }

        // Hide add task area.
        mAddTaskContainer.setVisibility(View.GONE);

        // Hide tags area.
        animateTags(true);

        // Fade in the tasks.
        mTasksArea.animate().alpha(1f).setDuration(Constants.ANIMATION_DURATION_LONG);
        transitionStatusBar(ThemeUtils.getSectionColorDark(Sections.FOCUS, this));

        // Broadcast changes.
        mTasksService.sendBroadcast(Actions.TASKS_CHANGED);
    }

    private void animateTags(boolean isHiding) {
        DisplayMetrics displaymetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);

        float fromY = isHiding ? mAddTaskTagContainer.getY() : -displaymetrics.heightPixels;
        float toY = isHiding ? -displaymetrics.heightPixels : mTagsTranslationY;

        ObjectAnimator animator = ObjectAnimator.ofFloat(mAddTaskTagContainer, "translationY", fromY, toY);
        animator.setDuration(Constants.ANIMATION_DURATION_LONG).start();
    }

    @OnClick(R.id.button_edit_task)
    protected void editTask() {
        // Send a broadcast to edit the currently selected task. The fragment should handle it.
        mTasksService.sendBroadcast(Actions.EDIT_TASK);
        // Close edit bar.
        hideEditBar();
    }

    @OnClick(R.id.button_assign_tags)
    protected void assignTags() {
        // Send a broadcast to assign tags to the selected tasks. The fragment should handle it.
        mTasksService.sendBroadcast(Actions.ASSIGN_TAGS);
    }

    @OnClick(R.id.button_delete_tasks)
    protected void deleteTasks() {
        // Send a broadcast to delete tasks. The fragment should handle it, since it contains the list.
        mTasksService.sendBroadcast(Actions.DELETE_TASKS);
    }

    @OnClick(R.id.button_share_tasks)
    protected void shareTasks() {
        // Send a broadcast to share selected tasks. The fragment should handle it.
        mTasksService.sendBroadcast(Actions.SHARE_TASKS);
    }

    private View.OnFocusChangeListener mFocusListener = new View.OnFocusChangeListener() {
        @Override
        public void onFocusChange(View v, boolean hasFocus) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (hasFocus) {
                imm.showSoftInput(mEditTextAddNewTask, InputMethodManager.SHOW_IMPLICIT);
            } else {
                imm.hideSoftInputFromWindow(mEditTextAddNewTask.getWindowToken(), 0);
            }
        }
    };

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
            // Back button has been pressed. Get back to the list.
            endAddTaskWorkflow(false);
        }
    };

    public void shareOnFacebook(View v) {
        // TODO: Call sharing flow.
    }

    public void shareOnTwitter(View v) {
        // TODO: Call sharing flow.
    }

    public void shareAll(View v) {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, mShareMessage + " // " +
                getString(R.string.all_done_share_url));
        startActivity(Intent.createChooser(intent, getString(R.string.share_chooser_title)));
    }

    public void setShareMessage(String message) {
        mShareMessage = message;
    }

    private void setViewHeight(View view, int dimen) {
        ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
        layoutParams.height = getResources().getDimensionPixelSize(dimen);
        view.setLayoutParams(layoutParams);
    }

    public void hideActionButtons() {
        Animation slideDown = AnimationUtils.loadAnimation(this, R.anim.slide_down);
        slideDown.setAnimationListener(mHideButtonsListener);
        mActionButtonsContainer.startAnimation(slideDown);
    }

    public void showActionButtons() {
        mActionButtonsContainer.setVisibility(View.VISIBLE);
        mButtonAddTask.setVisibility(View.VISIBLE);
        mEditTasksBar.setVisibility(View.GONE);
    }

    private Animation.AnimationListener mHideButtonsListener = new Animation.AnimationListener() {
        @Override
        public void onAnimationStart(Animation animation) {
        }

        @Override
        public void onAnimationEnd(Animation animation) {
            mActionButtonsContainer.setVisibility(View.GONE);
        }

        @Override
        public void onAnimationRepeat(Animation animation) {
        }
    };

    private void loadTags() {
        List<GsonTag> tags = mTasksService.loadAllTags();
        mAddTaskTagContainer.removeAllViews();

        // For each tag, add a checkbox as child view.
        for (GsonTag tag : tags) {
            int resource = ThemeUtils.isLightTheme(this) ? R.layout.tag_box_light : R.layout.tag_box_dark;
            CheckBox tagBox = (CheckBox) getLayoutInflater().inflate(resource, null);
            tagBox.setText(tag.getTitle());
            tagBox.setId(tag.getId().intValue());

            // Set listener to assign tag.
            tagBox.setOnClickListener(mTagClickListener);

            // Add child view.
            mAddTaskTagContainer.addView(tagBox);
        }
    }

    private View.OnClickListener mTagClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            GsonTag selectedTag = mTasksService.loadTag((long) view.getId());

            // Add or remove from list of selected tags.
            if (isTagSelected(selectedTag)) {
                removeSelectedTag(selectedTag);
            } else {
                mSelectedTags.add(selectedTag);
            }
        }
    };

    private boolean isTagSelected(GsonTag selectedTag) {
        // Check if tag already exists in the list of selected.
        for (GsonTag tag : mSelectedTags) {
            if (tag.getId().equals(selectedTag.getId())) {
                return true;
            }
        }
        return false;
    }

    private void removeSelectedTag(GsonTag selectedTag) {
        // Find and remove tag from the list of selected.
        List<GsonTag> selected = new ArrayList<GsonTag>(mSelectedTags);
        for (GsonTag tag : selected) {
            if (tag.getId().equals(selectedTag.getId())) {
                mSelectedTags.remove(tag);
            }
        }
    }

    // HACK: Use activity to notify the middle fragment.
    public void updateEmptyView() {
        int focusIndex = Sections.FOCUS.getSectionNumber();
        TasksListFragment focusFragment = (TasksListFragment) mSectionsPagerAdapter.getItem(focusIndex);
        focusFragment.updateEmptyView();
    }

}
