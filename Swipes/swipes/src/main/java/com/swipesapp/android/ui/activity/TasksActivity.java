package com.swipesapp.android.ui.activity;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.negusoft.holoaccent.activity.AccentActivity;
import com.swipesapp.android.R;
import com.swipesapp.android.sync.gson.GsonTag;
import com.swipesapp.android.sync.gson.GsonTask;
import com.swipesapp.android.sync.receiver.SnoozeReceiver;
import com.swipesapp.android.sync.service.TasksService;
import com.swipesapp.android.ui.adapter.SectionsPagerAdapter;
import com.swipesapp.android.ui.listener.KeyboardBackListener;
import com.swipesapp.android.ui.listener.ListContentsListener;
import com.swipesapp.android.ui.view.ActionEditText;
import com.swipesapp.android.ui.view.BlurBuilder;
import com.swipesapp.android.ui.view.FactorSpeedScroller;
import com.swipesapp.android.ui.view.FlowLayout;
import com.swipesapp.android.ui.view.SwipesButton;
import com.swipesapp.android.util.Constants;
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

public class TasksActivity extends AccentActivity implements ListContentsListener {

    @InjectView(R.id.pager)
    ViewPager mViewPager;

    @InjectView(R.id.button_add_task)
    SwipesButton mButtonAddTask;

    @InjectView(R.id.blur_background)
    ImageView mBlurBackground;

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

    private WeakReference<Context> mContext;

    private TasksService mTasksService;

    private SectionsPagerAdapter mSectionsPagerAdapter;

    private static Sections sCurrentSection;

    private List<GsonTag> mSelectedTags;

    // Used by animator to store tags container position.
    private float mTagsTranslationY;

    public static BitmapDrawable sBlurDrawable;

    private int mCurrentSectionColor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(ThemeUtils.getThemeResource(this));
        setContentView(R.layout.activity_tasks);
        ButterKnife.inject(this);

        getWindow().getDecorView().setBackgroundColor(getResources().getColor(R.color.neutral_background));

        getActionBar().hide();

        mContext = new WeakReference<Context>(this);
        mTasksService = TasksService.getInstance(this);

        createSnoozeAlarm();

        mSectionsPagerAdapter = new SectionsPagerAdapter(getFragmentManager(), this);
        mViewPager.setAdapter(mSectionsPagerAdapter);
        mViewPager.setOnPageChangeListener(mSimpleOnPageChangeListener);

        // Default to second item, index starts at zero.
        mViewPager.setCurrentItem(Sections.FOCUS.getSectionNumber());
        sCurrentSection = Sections.FOCUS;

        // Define a custom duration to the page scroller, providing a more natural feel.
        customizeScroller();

        mSelectedTags = new ArrayList<GsonTag>();

        mTagsTranslationY = mAddTaskTagContainer.getTranslationY();

        // HACK: Flip add task confirm button, so the arrow points to the right.
        mButtonConfirmAddTask.setScaleX(-mButtonConfirmAddTask.getScaleX());

        int hintColor = ThemeUtils.isLightTheme(this) ? R.color.light_text_hint_color : R.color.dark_text_hint_color;
        mEditTextAddNewTask.setHintTextColor(getResources().getColor(hintColor));

        mButtonAddTask.setTextColor(ThemeUtils.getSectionColor(Sections.FOCUS, this));

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

    private void createSnoozeAlarm() {
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(this, SnoozeReceiver.class);
        PendingIntent alarmIntent = PendingIntent.getBroadcast(this, 0, intent, 0);

        alarmManager.setInexactRepeating(AlarmManager.ELAPSED_REALTIME, 60000, 60000, alarmIntent);
    }

    private ViewPager.SimpleOnPageChangeListener mSimpleOnPageChangeListener = new ViewPager.SimpleOnPageChangeListener() {
        @Override
        public void onPageSelected(int position) {
            sCurrentSection = Sections.getSectionByNumber(position);

            // Notify listeners that current tab has changed.
            mTasksService.sendBroadcast(Actions.TAB_CHANGED);

            hideEditBar();

            mCurrentSectionColor = ThemeUtils.getSectionColor(sCurrentSection, mContext.get());
        }
    };

    /**
     * Returns the current section being displayed.
     *
     * @return Current section.
     */
    public static Sections getCurrentSection() {
        return sCurrentSection;
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

    private void clearEmptyBackground() {
        // Do nothing.
    }

    private void setEmptyBackground() {
        // TODO: Animate empty background.

        mViewPager.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            public void onGlobalLayout() {
                switch (sCurrentSection) {
                    case LATER:
                        // Customize Later empty View.
                        RelativeLayout laterEmptyView = (RelativeLayout) mViewPager.findViewById(R.id.later_empty_view);
                        laterEmptyView.setBackgroundColor(ThemeUtils.getBackgroundColor(mContext.get()));
                        break;
                    case FOCUS:
                        // Customize Focus empty view.
                        ScrollView focusEmptyView = (ScrollView) mViewPager.findViewById(R.id.focus_empty_view);
                        focusEmptyView.setBackgroundColor(ThemeUtils.getBackgroundColor(mContext.get()));

                        TextView allDoneText = (TextView) mViewPager.findViewById(R.id.text_all_done);
                        allDoneText.setTextColor(ThemeUtils.getTextColor(mContext.get()));

                        TextView nextTaskText = (TextView) mViewPager.findViewById(R.id.text_next_task);
                        nextTaskText.setTextColor(ThemeUtils.getTextColor(mContext.get()));

                        TextView allDoneMessage = (TextView) mViewPager.findViewById(R.id.text_all_done_message);
                        allDoneMessage.setTextColor(ThemeUtils.getTextColor(mContext.get()));

                        Button facebookShare = (Button) mViewPager.findViewById(R.id.button_facebook_share);
                        facebookShare.setBackgroundResource(ThemeUtils.isLightTheme(mContext.get()) ?
                                R.drawable.facebook_rounded_button_light : R.drawable.facebook_rounded_button_dark);

                        Button twitterShare = (Button) mViewPager.findViewById(R.id.button_twitter_share);
                        twitterShare.setBackgroundResource(ThemeUtils.isLightTheme(mContext.get()) ?
                                R.drawable.twitter_rounded_button_light : R.drawable.twitter_rounded_button_dark);
                        break;
                    case DONE:
                        // Customize Done empty view.
                        RelativeLayout doneEmptyView = (RelativeLayout) mViewPager.findViewById(R.id.done_empty_view);
                        doneEmptyView.setBackgroundColor(ThemeUtils.getBackgroundColor(mContext.get()));
                        break;
                }
            }
        });

        hideEditBar();
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
            mActionButtonsContainer.setBackgroundColor(mCurrentSectionColor);
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
        // Go to main fragment if needed.
        if (sCurrentSection != Sections.FOCUS) {
            mViewPager.setCurrentItem(Sections.FOCUS.getSectionNumber());
        }

        // Blur background.
        updateBlurDrawable(ThemeUtils.getTasksBlurAlphaColor(this));
        mBlurBackground.setImageDrawable(sBlurDrawable);

        // Fade in the blur background.
        mBlurBackground.setAlpha(0f);
        mBlurBackground.setVisibility(View.VISIBLE);
        mBlurBackground.animate().alpha(1f).setDuration(Constants.ANIMATION_DURATION).setListener(mBlurFadeInListener);

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

    @OnClick(R.id.blur_background)
    protected void blurBackgroundClick() {
        endAddTaskWorkflow(false);
    }

    private void endAddTaskWorkflow(boolean resetFields) {
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

        // Show the main layout.
        mViewPager.setAlpha(1f);
        mViewPager.setVisibility(View.VISIBLE);

        // Fade out the blur background.
        mBlurBackground.animate().alpha(0f).setDuration(Constants.ANIMATION_DURATION).setListener(mBlurFadeOutListener);

        // Broadcast changes.
        mTasksService.sendBroadcast(Actions.TASKS_CHANGED);
    }

    private void animateTags(boolean isHiding) {
        DisplayMetrics displaymetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);

        float fromY = isHiding ? mAddTaskTagContainer.getY() : -displaymetrics.heightPixels;
        float toY = isHiding ? -displaymetrics.heightPixels : mTagsTranslationY;

        ObjectAnimator animator = ObjectAnimator.ofFloat(mAddTaskTagContainer, "translationY", fromY, toY);
        animator.setDuration(Constants.ANIMATION_DURATION).start();
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

    private AnimatorListenerAdapter mBlurFadeInListener = new AnimatorListenerAdapter() {
        @Override
        public void onAnimationEnd(Animator animation) {
            // Hide the main layout.
            mViewPager.setVisibility(View.GONE);
        }
    };

    private AnimatorListenerAdapter mBlurFadeOutListener = new AnimatorListenerAdapter() {
        @Override
        public void onAnimationEnd(Animator animation) {
            // Hide the blur background.
            mBlurBackground.setVisibility(View.GONE);
        }
    };

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

    // HACK: This is a workaround to change the background entirely.
    @Override
    public void onEmpty(Sections section) {
        if (section == sCurrentSection) {
            setEmptyBackground();
        }
    }

    // HACK: This is a workaround to change the background entirely.
    @Override
    public void onNotEmpty(Sections section) {
        if (section == sCurrentSection) {
            clearEmptyBackground();
        }
    }

    public void shareOnFacebook(View v) {
        // TODO: Call sharing flow.
        Toast.makeText(this, "Facebook share coming soon", Toast.LENGTH_SHORT).show();
    }

    public void shareOnTwitter(View v) {
        // TODO: Call sharing flow.
        Toast.makeText(this, "Twitter share coming soon", Toast.LENGTH_SHORT).show();
    }

    public void updateBlurDrawable(int alphaColor) {
        sBlurDrawable = BlurBuilder.blur(this, mViewPager, alphaColor);
    }

    public static BitmapDrawable getBlurDrawable() {
        return sBlurDrawable;
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

}
