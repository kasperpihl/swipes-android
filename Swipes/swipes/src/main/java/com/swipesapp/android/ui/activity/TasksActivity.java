package com.swipesapp.android.ui.activity;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.app.ActionBar;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.TransitionDrawable;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.astuetz.PagerSlidingTabStrip;
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
import com.swipesapp.android.ui.view.NoSwipeViewPager;
import com.swipesapp.android.ui.view.SwipesButton;
import com.swipesapp.android.util.Constants;
import com.swipesapp.android.util.ThemeUtils;
import com.swipesapp.android.values.Actions;
import com.swipesapp.android.values.RepeatOptions;
import com.swipesapp.android.values.Sections;
import com.swipesapp.android.values.Themes;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;

public class TasksActivity extends AccentActivity implements ListContentsListener {

    @InjectView(R.id.pager)
    NoSwipeViewPager mViewPager;

    @InjectView(R.id.tabs)
    PagerSlidingTabStrip mTabs;

    @InjectView(R.id.tasks_activity_container)
    ViewGroup mActivityMainLayout;

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

    @InjectView(R.id.button_assign_tags)
    SwipesButton mButtonAssignTags;

    @InjectView(R.id.button_delete_tasks)
    SwipesButton mButtonDeleteTasks;

    @InjectView(R.id.button_share_tasks)
    SwipesButton mButtonShareTasks;

    @InjectView(R.id.add_task_tag_container)
    FlowLayout mAddTaskTagContainer;

    @InjectView(R.id.button_add_task_tag)
    CheckBox mButtonAddTaskTag;

    @InjectView(R.id.action_buttons_container)
    FrameLayout mActionButtonsContainer;

    @InjectView(R.id.action_buttons_gradient)
    FrameLayout mActionButtonsGradient;

    private static final String LOG_TAG = TasksActivity.class.getSimpleName();

    private WeakReference<Context> mContext;

    private SectionsPagerAdapter mSectionsPagerAdapter;

    private static Sections sCurrentSection;

    private static Typeface sTypeface;

    private TransitionDrawable mBackgroundTransition;

    private boolean mIsEmptyBackground;

    // TODO: Populate list of selected tags.
    private List<GsonTag> mSelectedTags;

    // Used by animator to store tags container position.
    private float mTagsTranslationY;

    public static BitmapDrawable sBlurDrawable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(ThemeUtils.getThemeResource(this));
        setContentView(R.layout.activity_tasks);
        ButterKnife.inject(this);
        mContext = new WeakReference<Context>(this);

        createSnoozeAlarm();

        mSectionsPagerAdapter = new SectionsPagerAdapter(getFragmentManager(), this);
        mViewPager.setAdapter(mSectionsPagerAdapter);
        mTabs.setViewPager(mViewPager);

        if (sTypeface == null) {
            sTypeface = Typeface.createFromAsset(getAssets(), Constants.FONT_NAME);
        }
        mTabs.setTypeface(sTypeface, 0);

        int dimension = getResources().getDimensionPixelSize(R.dimen.action_bar_icon_size);
        mTabs.setTextSize(dimension);
        mTabs.setIndicatorColor(ThemeUtils.getTextColor(this));
        mTabs.setTextColor(ThemeUtils.getTextColor(this));
        mTabs.setDividerColor(ThemeUtils.getDividerColor(this));
        mTabs.setTabBackground(ThemeUtils.getTabBackground(this));
        mTabs.setOnPageChangeListener(mSimpleOnPageChangeListener);

        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }

        mSelectedTags = new ArrayList<GsonTag>();

        mTagsTranslationY = mAddTaskTagContainer.getTranslationY();

        // Default to second item, index starts at zero
        mViewPager.setCurrentItem(Sections.FOCUS.getSectionNumber());
        sCurrentSection = Sections.FOCUS;

        // HACK: Flip add task confirm button, so the arrow points to the right.
        mButtonConfirmAddTask.setScaleX(-mButtonConfirmAddTask.getScaleX());

        // Define a custom duration to the page scroller, providing a more natural feel.
        customizeScroller();

        // Customize tag button to match current theme.
        customizeTagButton();

        // Setup background.
        mActivityMainLayout.setBackgroundResource(ThemeUtils.getTransitionBackground(this));
        mBackgroundTransition = (TransitionDrawable) mActivityMainLayout.getBackground();

        // Set button selectors.
        mButtonAddTask.setSelector(R.string.round_add, R.string.round_add_full);

        int hintColor = ThemeUtils.isLightTheme(this) ? R.color.light_text_hint_color : R.color.dark_text_hint_color;
        mEditTextAddNewTask.setHintTextColor(getResources().getColor(hintColor));

        mEditTextAddNewTask.setListener(mKeyboardBackListener);

        // TODO: Remove this when tagging is working. The container is hidden for the first beta.
        mAddTaskTagContainer.setEnabled(false);
        mAddTaskTagContainer.setAlpha(0f);
    }

    @Override
    protected void onDestroy() {
        ButterKnife.reset(this);
        super.onDestroy();
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
            if (position != Sections.FOCUS.getSectionNumber()) {
                clearEmptyBackground();
                mIsEmptyBackground = false;
            }

            customizeTabColors(ThemeUtils.getTextColor(mContext.get()), ThemeUtils.getDividerColor(mContext.get()), position);

            if (position == Sections.SETTINGS.getSectionNumber()) {
                mButtonAddTask.setVisibility(View.GONE);
                hideGradient();
            } else {
                mButtonAddTask.setVisibility(View.VISIBLE);
                showGradient();

                if (position != Sections.DONE.getSectionNumber()) collapseGradient();
            }

            sCurrentSection = Sections.getSectionByNumber(position);

            // Notify listeners that current tab has changed.
            TasksService.getInstance(mContext.get()).sendBroadcast(Actions.TAB_CHANGED);

            hideEditBar();
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

    private void customizeTagButton() {
        int background = ThemeUtils.getCurrentTheme(this) == Themes.LIGHT ? R.drawable.tag_selector_light : R.drawable.tag_selector_dark;
        int color = ThemeUtils.getCurrentTheme(this) == Themes.LIGHT ? R.color.tag_text_color_selector_light : R.color.tag_text_color_selector_dark;
        mButtonAddTaskTag.setBackgroundResource(background);
        mButtonAddTaskTag.setTextColor(getResources().getColorStateList(color));
    }

    private void customizeTabColors(int textColor, int dividerColor, int position) {
        int[] textColors = {
                ThemeUtils.getSectionColor(Sections.LATER, mContext.get()),
                ThemeUtils.getSectionColor(Sections.FOCUS, mContext.get()),
                ThemeUtils.getSectionColor(Sections.DONE, mContext.get()),
                textColor
        };

        mTabs.setIndicatorColor(textColors[position]);
        mTabs.setTextColor(textColor);
        mTabs.setDividerColor(dividerColor);

        View view = mTabs.getTabView(position);

        if (view != null && view instanceof TextView) {
            TextView tabTextView = (TextView) view;
            tabTextView.setTextColor(textColors[position]);
        }
    }

    public void showGradient() {
        mActionButtonsContainer.setBackgroundColor(ThemeUtils.getBackgroundColor(this));
        mActionButtonsGradient.setBackgroundDrawable(ThemeUtils.getGradientDrawable(this));
    }

    public void hideGradient() {
        mActionButtonsContainer.setBackgroundColor(Color.TRANSPARENT);
        mActionButtonsGradient.setBackgroundColor(Color.TRANSPARENT);
    }

    public void expandGradient() {
        // Increase container height and make it transparent, so the gradient is on top of the done buttons.
        setViewHeight(mActionButtonsContainer, R.dimen.button_container_expanded_height);
        mActionButtonsContainer.setBackgroundColor(Color.TRANSPARENT);
    }

    public void collapseGradient() {
        // Restore original container height and background.
        setViewHeight(mActionButtonsContainer, R.dimen.button_container_height);
        mActionButtonsContainer.setBackgroundColor(ThemeUtils.getBackgroundColor(this));
    }

    private void clearEmptyBackground() {
        // Reset background and divider color.
        mBackgroundTransition.resetTransition();

        // Load text color.
        int textColor = ThemeUtils.getTextColor(this);

        // Reset tab colors.
        customizeTabColors(textColor, ThemeUtils.getDividerColor(this), sCurrentSection.getSectionNumber());

        // Reset buttons and text colors.
        mButtonAddTask.setTextColor(textColor);

        showGradient();
    }

    private void setEmptyBackground() {
        // Change background.
        mBackgroundTransition.startTransition(Constants.ANIMATION_DURATION);

        // Change tab colors, otherwise they look misplaced against the image background.
        customizeTabColors(Color.WHITE, getResources().getColor(R.color.empty_divider), sCurrentSection.getSectionNumber());

        // Change buttons and text colors to improve visibility.
        mButtonAddTask.setTextColor(Color.WHITE);

        hideEditBar();

        hideGradient();
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
        // TODO: What should the temp ID be?
        String tempId = title + currentDate.getTime();

        if (!title.isEmpty()) {
            GsonTask task = new GsonTask(null, tempId, null, currentDate, currentDate, false, title, null, 0, priority, null, currentDate, null, null, RepeatOptions.NEVER.getValue(), null, null, mSelectedTags, 0);
            TasksService.getInstance(this).saveTask(task);
        }

        endAddTaskWorkflow();
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
    }

    @OnClick(R.id.blur_background)
    protected void endAddTaskWorkflow() {
        // Remove focus and hide text view.
        mEditTextAddNewTask.clearFocus();
        mEditTextAddNewTask.setVisibility(View.GONE);

        // Reset fields.
        mEditTextAddNewTask.setText("");
        mButtonAddTaskPriority.setChecked(false);

        // Hide add task area.
        mAddTaskContainer.setVisibility(View.GONE);

        // Hide tags area.
        animateTags(true);

        // Show the main layout.
        mActivityMainLayout.setAlpha(1f);
        mActivityMainLayout.setVisibility(View.VISIBLE);

        // Fade out the blur background.
        mBlurBackground.animate().alpha(0f).setDuration(Constants.ANIMATION_DURATION).setListener(mBlurFadeOutListener);
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
        TasksService.getInstance(this).sendBroadcast(Actions.EDIT_TASK);
        // Close edit bar.
        hideEditBar();
    }

    @OnClick(R.id.button_assign_tags)
    protected void assignTags() {
        // Send a broadcast to assign tags to the selected tasks. The fragment should handle it.
        TasksService.getInstance(this).sendBroadcast(Actions.ASSIGN_TAGS);

        // TODO: Remove this when tagging is working.
        Toast.makeText(this, "Tags coming soon", Toast.LENGTH_SHORT).show();
    }

    @OnClick(R.id.button_delete_tasks)
    protected void deleteTasks() {
        // Send a broadcast to delete tasks. The fragment should handle it, since it contains the list.
        TasksService.getInstance(this).sendBroadcast(Actions.DELETE_TASKS);
    }

    @OnClick(R.id.button_share_tasks)
    protected void shareTasks() {
        // Send a broadcast to share selected tasks. The fragment should handle it.
        TasksService.getInstance(this).sendBroadcast(Actions.SHARE_TASKS);

        // TODO: Remove this when sharing is working.
        Toast.makeText(this, "Sharing coming soon", Toast.LENGTH_SHORT).show();
    }

    private AnimatorListenerAdapter mBlurFadeInListener = new AnimatorListenerAdapter() {
        @Override
        public void onAnimationEnd(Animator animation) {
            // Hide the main layout.
            mActivityMainLayout.setVisibility(View.GONE);
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
            endAddTaskWorkflow();
        }
    };

    // HACK: This is a workaround to change the background entirely.
    @Override
    public void onEmpty(Sections section) {
        if (section == sCurrentSection && section == Sections.FOCUS && !mIsEmptyBackground) {
            setEmptyBackground();
            mIsEmptyBackground = true;
        }
    }

    // HACK: This is a workaround to change the background entirely.
    @Override
    public void onNotEmpty(Sections section) {
        if (section == sCurrentSection && section == Sections.FOCUS && mIsEmptyBackground) {
            clearEmptyBackground();
            mIsEmptyBackground = false;
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
        sBlurDrawable = BlurBuilder.blur(this, mActivityMainLayout, alphaColor);
    }

    public static BitmapDrawable getBlurDrawable() {
        return sBlurDrawable;
    }

    private void setViewHeight(View view, int dimen) {
        ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
        layoutParams.height = getResources().getDimensionPixelSize(dimen);
        view.setLayoutParams(layoutParams);
    }

}
