package com.swipesapp.android.ui.activity;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.view.ViewPager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.fortysevendeg.swipelistview.DynamicViewPager;
import com.melnykov.fab.FloatingActionButton;
import com.parse.ParseUser;
import com.parse.ui.ParseLoginBuilder;
import com.swipesapp.android.R;
import com.swipesapp.android.db.migration.MigrationAssistant;
import com.swipesapp.android.handler.WelcomeHandler;
import com.swipesapp.android.sync.gson.GsonTag;
import com.swipesapp.android.sync.gson.GsonTask;
import com.swipesapp.android.sync.service.SyncService;
import com.swipesapp.android.sync.service.TasksService;
import com.swipesapp.android.ui.adapter.SectionsPagerAdapter;
import com.swipesapp.android.ui.fragments.TasksListFragment;
import com.swipesapp.android.ui.listener.KeyboardBackListener;
import com.swipesapp.android.ui.view.ActionEditText;
import com.swipesapp.android.ui.view.FactorSpeedScroller;
import com.swipesapp.android.ui.view.FlowLayout;
import com.swipesapp.android.ui.view.SwipesButton;
import com.swipesapp.android.ui.view.SwipesDialog;
import com.swipesapp.android.util.ColorUtils;
import com.swipesapp.android.util.DeviceUtils;
import com.swipesapp.android.util.PreferenceUtils;
import com.swipesapp.android.util.ThemeUtils;
import com.swipesapp.android.values.Actions;
import com.swipesapp.android.values.Constants;
import com.swipesapp.android.values.RepeatOptions;
import com.swipesapp.android.values.Sections;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
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

    @InjectView(R.id.toolbar_area)
    FrameLayout mToolbarArea;

    @InjectView(R.id.button_add_task)
    FloatingActionButton mButtonAddTask;

    @InjectView(R.id.add_task_container)
    RelativeLayout mAddTaskContainer;

    @InjectView(R.id.edit_text_add_task_content)
    ActionEditText mEditTextAddNewTask;

    @InjectView(R.id.button_add_task_priority)
    CheckBox mButtonAddTaskPriority;

    @InjectView(R.id.edit_tasks_bar)
    FrameLayout mEditTasksBar;
    @InjectView(R.id.edit_bar_area)
    RelativeLayout mEditBarArea;
    @InjectView(R.id.edit_bar_selection_count)
    TextView mEditBarCount;

    @InjectView(R.id.add_task_tag_container)
    FlowLayout mAddTaskTagContainer;

    @InjectView(R.id.action_buttons_container)
    LinearLayout mActionButtonsContainer;

    @InjectView(R.id.workspaces_view)
    FrameLayout mWorkspacesView;
    @InjectView(R.id.workspaces_area)
    LinearLayout mWorkspacesArea;
    @InjectView(R.id.workspaces_tags)
    FlowLayout mWorkspacesTags;
    @InjectView(R.id.workspaces_empty_tags)
    TextView mWorkspacesEmptyTags;

    @InjectView(R.id.action_bar_search)
    LinearLayout mSearchBar;
    @InjectView(R.id.action_bar_close_search)
    SwipesButton mSearchClose;
    @InjectView(R.id.action_bar_search_field)
    ActionEditText mSearchField;

    @InjectView(R.id.navigation_menu)
    LinearLayout mNavigationMenu;
    @InjectView(R.id.navigation_menu_container)
    RelativeLayout mNavigationMenuContainer;

    @InjectView(R.id.navigation_later_button)
    SwipesButton mNavigationLaterButton;
    @InjectView(R.id.navigation_focus_button)
    SwipesButton mNavigationFocusButton;
    @InjectView(R.id.navigation_done_button)
    SwipesButton mNavigationDoneButton;

    private static final String LOG_TAG = TasksActivity.class.getSimpleName();

    private static final int ACTION_LOGIN = 0;
    private static final int ACTION_MULTI_SELECT = 1;
    private static final int ACTION_SEARCH = 2;
    private static final int ACTION_WORKSPACES = 3;
    private static final int ACTION_SETTINGS = 4;

    private WeakReference<Context> mContext;

    private TasksService mTasksService;
    private SyncService mSyncService;

    private SectionsPagerAdapter mSectionsPagerAdapter;

    private Sections mCurrentSection;

    private List<GsonTag> mSelectedTags;

    // Used by animator to store tags container position.
    private float mTagsTranslationY;

    private View mActionBarView;
    private TextView mActionBarTitle;
    private SwipesButton mActionBarIcon;

    private float mPreviousOffset;

    private boolean mHasChangedTab;

    private boolean mIsAddingTask;

    private String mShareMessage;

    private boolean mWasRestored;

    private String[] mIntentData;

    private boolean mIsSelectionMode;

    private List<GsonTag> mSelectedFilterTags;

    private boolean mIsSearchActive;
    private String mSearchQuery;

    private boolean mShouldSkipSync;
    private boolean mShouldClearData;

    private boolean mIsShowingNavigation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(ThemeUtils.isLightTheme(this) ? R.style.Tasks_Theme_Light : R.style.Tasks_Theme_Dark);
        setContentView(R.layout.activity_tasks);
        ButterKnife.inject(this);

        getWindow().getDecorView().setBackgroundColor(ThemeUtils.getNeutralBackgroundColor(this));

        mContext = new WeakReference<Context>(this);
        mTasksService = TasksService.getInstance();
        mSyncService = SyncService.getInstance();

        setupActionBarCustomView();

        getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        getSupportActionBar().setDisplayShowCustomEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        getSupportActionBar().setCustomView(mActionBarView);

        performInitialSetup();

        if (mCurrentSection == null) mCurrentSection = Sections.FOCUS;

        setupViewPager();

        // Define a custom duration to the page scroller, providing a more natural feel.
        customizeScroller();

        mSelectedTags = new ArrayList<GsonTag>();

        mTagsTranslationY = mAddTaskTagContainer.getTranslationY();

        int hintColor = ThemeUtils.isLightTheme(this) ? R.color.light_hint : R.color.dark_hint;
        mEditTextAddNewTask.setHintTextColor(getResources().getColor(hintColor));

        mEditTextAddNewTask.setListener(mKeyboardBackListener);

        customizeSelectionColors();

        loadSearchBar();

        handleShareIntent();
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
        // Create filter and start receiver.
        IntentFilter filter = new IntentFilter();
        filter.addAction(Actions.TASKS_CHANGED);

        registerReceiver(mTasksReceiver, filter);

        // Start sync if allowed to.
        if (!mShouldSkipSync) {
            startSync();
        }

        if (mWasRestored) {
            // Reset section.
            mViewPager.setCurrentItem(Sections.FOCUS.getSectionNumber());
        }

        // Restore section colors.
        setupSystemBars(mCurrentSection);

        // Clear restoration flag.
        mWasRestored = false;

        super.onResume();
    }

    @Override
    public void onPause() {
        // Stop receiver.
        unregisterReceiver(mTasksReceiver);

        super.onPause();
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        // Mark activity as being restored.
        mWasRestored = true;
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
                case Constants.ACCOUNT_CHANGED_RESULT_CODE:
                    // Change visibility of login menu.
                    invalidateOptionsMenu();

                    // Refresh all lists.
                    refreshSections();
                    break;
            }
        } else if (requestCode == Constants.LOGIN_REQUEST_CODE) {
            switch (resultCode) {
                case Activity.RESULT_OK:
                    // Block sync.
                    mShouldSkipSync = true;

                    // Login successful.
                    if (mShouldClearData) {
                        // Clear data immediately.
                        clearData();
                        performInitialSync();
                    } else {
                        // Ask to keep user data.
                        askToKeepData();
                    }

                    // Change visibility of login menu.
                    invalidateOptionsMenu();
                    break;
                case Activity.RESULT_CANCELED:
                    // Keep data unless told otherwise.
                    mShouldClearData = false;
                    break;
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Login.
        menu.add(Menu.NONE, ACTION_LOGIN, Menu.NONE, getResources().getString(R.string.tasks_list_login_action))
                .setVisible(ParseUser.getCurrentUser() == null).setIcon(R.drawable.ic_account_dark)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);

        // Multi select.
        int multiSelIcon = ThemeUtils.isLightTheme(this) ? R.drawable.ic_multi_select_light : R.drawable.ic_multi_select_dark;
        menu.add(Menu.NONE, ACTION_MULTI_SELECT, Menu.NONE, getResources().getString(R.string.tasks_list_multi_select_action))
                .setIcon(multiSelIcon).setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);

        // Search.
        int searchIcon = ThemeUtils.isLightTheme(this) ? R.drawable.ic_search_light : R.drawable.ic_search_dark;
        menu.add(Menu.NONE, ACTION_SEARCH, Menu.NONE, getResources().getString(R.string.tasks_list_search_action))
                .setIcon(searchIcon).setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);

        // Workspaces.
        int workspacesIcon = ThemeUtils.isLightTheme(this) ? R.drawable.ic_workspaces_light : R.drawable.ic_workspaces_dark;
        menu.add(Menu.NONE, ACTION_WORKSPACES, Menu.NONE, getResources().getString(R.string.tasks_list_workspaces_action))
                .setIcon(workspacesIcon).setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);

        // Settings.
        int settingsIcon = ThemeUtils.isLightTheme(this) ? R.drawable.ic_settings_light : R.drawable.ic_settings_dark;
        menu.add(Menu.NONE, ACTION_SETTINGS, Menu.NONE, getResources().getString(R.string.tasks_list_settings_action))
                .setIcon(settingsIcon).setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onMenuOpened(int featureId, Menu menu) {
        // HACK: Show action icons.
        if (featureId == Window.FEATURE_ACTION_BAR && menu != null) {
            if (menu.getClass().getSimpleName().equals("MenuBuilder")) {
                try {
                    Method m = menu.getClass().getDeclaredMethod("setOptionalIconsVisible", Boolean.TYPE);
                    m.setAccessible(true);
                    m.invoke(menu, true);
                } catch (Exception e) {
                    Log.e(LOG_TAG, "Error setting menu icons.", e);
                }
            }
        }

        return super.onMenuOpened(featureId, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case ACTION_LOGIN:
                // Call login.
                startLogin();
                break;
            case ACTION_MULTI_SELECT:
                // Enable selection UI.
                enableSelection();
                break;
            case ACTION_SEARCH:
                // Show search bar.
                showSearch();
                break;
            case ACTION_WORKSPACES:
                // Open workspaces.
                showWorkspaces();
                break;
            case ACTION_SETTINGS:
                Intent intent = new Intent(this, SettingsActivity.class);
                startActivityForResult(intent, Constants.SETTINGS_REQUEST_CODE);
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    private void startSync() {
        // Sync only changes after initial sync has been performed.
        boolean changesOnly = PreferenceUtils.getSyncLastUpdate(this) != null;
        mSyncService.performSync(changesOnly, 0);
    }

    private void performInitialSetup() {
        // Perform migrations when needed.
        MigrationAssistant.performUpgrades(mContext.get());

        // Show welcome dialog only once.
        if (!PreferenceUtils.hasShownWelcomeScreen(this)) {
            // Welcome user.
            showWelcomeDialog();
        }

        // Save welcome tasks if the app is used for the first time.
        if (PreferenceUtils.isFirstRun(this)) {
            WelcomeHandler welcomeHandler = new WelcomeHandler(this);
            welcomeHandler.addWelcomeTasks();
        }
    }

    private void handleShareIntent() {
        // Handle intent from other apps.
        Intent intent = getIntent();
        String action = intent.getAction();

        if (action != null && (action.equals(Intent.ACTION_SEND) || action.equals(Intent.ACTION_SEND_MULTIPLE))) {

            String title = intent.getStringExtra(Intent.EXTRA_SUBJECT);
            String notes = intent.getStringExtra(Intent.EXTRA_TEXT);

            if (title == null || title.isEmpty()) title = "";

            if (notes != null && !notes.startsWith("http")) {
                notes = notes.replaceAll("http[^ ]+$", "");
            }

            mIntentData = new String[]{title, notes};

            startAddTaskWorkflow();
        }
    }

    private void setupActionBarCustomView() {
        // Inflate custom view.
        LayoutInflater inflater = LayoutInflater.from(this);
        mActionBarView = inflater.inflate(R.layout.action_bar_custom_view, null);
        mActionBarTitle = (TextView) mActionBarView.findViewById(R.id.action_bar_title);
        mActionBarIcon = (SwipesButton) mActionBarView.findViewById(R.id.action_bar_icon);

        // Enable navigation menu on portrait only.
        if (DeviceUtils.isLandscape(this)) {
            mActionBarView.setClickable(false);
        } else {
            mActionBarView.setOnClickListener(mNavigationToggleListener);
        }

        // Apply ripple effect.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mActionBarView.setBackgroundResource(R.drawable.navigation_menu_ripple);
        }
    }

    private void setupSystemBars(Sections section) {
        // Setup toolbar icon.
        mActionBarIcon.disableTouchFeedback();
        if (!mIsShowingNavigation) mActionBarIcon.setTextColor(Color.WHITE);

        // Make ActionBar transparent.
        themeActionBar(Color.TRANSPARENT);

        if (DeviceUtils.isLandscape(this)) {
            // Replace colors on landscape.
            mToolbarArea.setBackgroundColor(getResources().getColor(R.color.neutral_accent));
            themeStatusBar(getResources().getColor(R.color.neutral_accent_dark));

            // Replace title and text.
            mActionBarTitle.setText(getString(R.string.overview_title));
            mActionBarIcon.setText(getString(R.string.schedule_logbook));
        } else {
            // Apply regular colors.
            mToolbarArea.setBackgroundColor(ThemeUtils.getSectionColor(mCurrentSection, this));
            themeStatusBar(ThemeUtils.getSectionColorDark(section, this));

            // Apply regular title and text.
            mActionBarTitle.setText(section.getSectionTitle(this));
            mActionBarIcon.setText(section.getSectionIcon(this));
        }
    }

    private void setupViewPager() {
        if (DeviceUtils.isLandscape(this)) mSimpleOnPageChangeListener = null;

        mSectionsPagerAdapter = new SectionsPagerAdapter(getFragmentManager(), this);
        mViewPager.setAdapter(mSectionsPagerAdapter);
        mViewPager.setOnPageChangeListener(mSimpleOnPageChangeListener);
        mViewPager.setPageMargin(getResources().getDimensionPixelSize(R.dimen.pager_margin_sides));
        mViewPager.setCurrentItem(mCurrentSection.getSectionNumber());
        mViewPager.setOffscreenPageLimit(2);

        if (DeviceUtils.isTablet(this)) mViewPager.setOverScrollMode(View.OVER_SCROLL_NEVER);
    }

    private ViewPager.SimpleOnPageChangeListener mSimpleOnPageChangeListener = new ViewPager.SimpleOnPageChangeListener() {
        @Override
        public void onPageSelected(int position) {
            mCurrentSection = Sections.getSectionByNumber(position);

            themeRecentsHeader(ThemeUtils.getSectionColor(mCurrentSection, mContext.get()));

            updateNavigationButtons();

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
            mToolbarArea.setBackgroundColor(blended);

            // Load dark colors for sections.
            fromColor = ThemeUtils.getSectionColorDark(from, mContext.get());
            toColor = ThemeUtils.getSectionColorDark(to, mContext.get());

            // Blend the colors and adjust the status bar.
            blended = ColorUtils.blendColors(fromColor, toColor, positionOffset);
            if (!mIsAddingTask) themeStatusBar(blended);

            // Adjust navigation area.
            paintNavigationArea(blended);

            // Fade ActionBar content gradually.
            fadeActionBar(positionOffset, from, to);
        }
    };

    private void fadeActionBar(float positionOffset, Sections from, Sections to) {
        if (mPreviousOffset > 0) {
            if (positionOffset > mPreviousOffset) {
                // Swiping to the right of the ViewPager.
                if (positionOffset < 0.5) {
                    // Fade out until half of the way.
                    mActionBarView.setAlpha(1 - positionOffset * 2);
                } else {
                    // Fade in from half to the the end.
                    mActionBarView.setAlpha((positionOffset - 0.5f) * 2);

                    // Set next title and icon.
                    mActionBarTitle.setText(to.getSectionTitle(this));
                    mActionBarIcon.setText(to.getSectionIcon(this));
                }
            } else {
                // Swiping to the left of the ViewPager.
                if (positionOffset > 0.5) {
                    // Fade out until half of the way.
                    mActionBarView.setAlpha(positionOffset / 2);
                } else {
                    // Fade in from half to the the end.
                    mActionBarView.setAlpha((0.5f - positionOffset) * 2);

                    // Set next title and icon.
                    mActionBarTitle.setText(from.getSectionTitle(this));
                    mActionBarIcon.setText(from.getSectionIcon(this));
                }
            }
        }

        mPreviousOffset = positionOffset;
    }

    private BroadcastReceiver mTasksReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Filter intent actions.
            if (intent.getAction().equals(Actions.TASKS_CHANGED)) {
                // Perform refresh of all sections.
                refreshSections();
            }
        }
    };

    public void refreshSections() {
        for (TasksListFragment fragment : mSectionsPagerAdapter.getFragments()) {
            // Refresh list without animation.
            fragment.refreshTaskList(false);
        }
    }

    public Sections getCurrentSection() {
        return mCurrentSection;
    }

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

    public void showEditBar() {
        // Apply container color.
        mEditBarArea.setBackgroundColor(ThemeUtils.getBackgroundColor(this));

        // Animate views only when necessary.
        if (mEditTasksBar.getVisibility() == View.GONE) {
            Animation slideDown = AnimationUtils.loadAnimation(this, R.anim.slide_down_out);
            slideDown.setAnimationListener(mShowEditBarListener);
            mButtonAddTask.startAnimation(slideDown);

            Animation slideUp = AnimationUtils.loadAnimation(this, R.anim.slide_up_in);
            mEditTasksBar.startAnimation(slideUp);
        }
    }

    public void hideEditBar() {
        // Animate views only when necessary.
        if (mEditTasksBar.isShown()) {
            Animation slideDown = AnimationUtils.loadAnimation(this, R.anim.slide_down_out);
            slideDown.setAnimationListener(mHideEditBarListener);
            mEditTasksBar.startAnimation(slideDown);

            Animation slideUp = AnimationUtils.loadAnimation(this, R.anim.slide_up_in);
            mButtonAddTask.startAnimation(slideUp);
        }

        // Hide selection count.
        mEditBarCount.animate().alpha(0f).setDuration(Constants.ANIMATION_DURATION_SHORT);
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

    @OnClick(R.id.add_task_priority_container)
    protected void togglePriority() {
        boolean checked = mButtonAddTaskPriority.isChecked();
        mButtonAddTaskPriority.setChecked(!checked);
    }

    @OnClick(R.id.button_confirm_add_task)
    protected void confirmAddTask() {
        Date currentDate = new Date();
        String title = mEditTextAddNewTask.getText().toString();
        Integer priority = mButtonAddTaskPriority.isChecked() ? 1 : 0;
        String tempId = UUID.randomUUID().toString();
        String notes = null;

        if (mIntentData != null) {
            notes = mIntentData[1];
        }

        if (!title.isEmpty()) {
            GsonTask task = GsonTask.gsonForLocal(null, null, tempId, null, currentDate, currentDate, false, title, notes, 0,
                    priority, null, currentDate, null, null, RepeatOptions.NEVER, null, null, mSelectedTags, null, 0);
            mTasksService.saveTask(task, true);
        }

        if (mIntentData != null) {
            Toast.makeText(this, getString(R.string.share_intent_add_confirm), Toast.LENGTH_SHORT).show();
            PreferenceUtils.saveBooleanPreference(PreferenceUtils.TASKS_ADDED_FROM_INTENT, true, this);
        }

        endAddTaskWorkflow(true);
    }

    @OnClick(R.id.button_add_task)
    protected void startAddTaskWorkflow() {
        // Set flag.
        mIsAddingTask = true;

        // Go to main fragment if needed.
        if (mCurrentSection != Sections.FOCUS) {
            mViewPager.setCurrentItem(Sections.FOCUS.getSectionNumber());
        }

        // Fade out the tasks.
        mTasksArea.animate().alpha(0f).setDuration(Constants.ANIMATION_DURATION_LONG);
        transitionStatusBar(ThemeUtils.getSectionColorDark(mCurrentSection, this), ThemeUtils.getStatusBarColor(this));

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

        // Load title from other apps.
        if (mIntentData != null) {
            String title = mIntentData[0];
            mEditTextAddNewTask.setText(title);
        }
    }

    @OnClick(R.id.add_task_container)
    protected void addTaskAreaClick() {
        if (mIntentData == null) endAddTaskWorkflow(false);
    }

    private void endAddTaskWorkflow(boolean resetFields) {
        // Finish if coming from another app.
        if (mIntentData != null) finish();

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
        transitionStatusBar(ThemeUtils.getStatusBarColor(this), ThemeUtils.getSectionColorDark(Sections.FOCUS, this));

        // Refresh main task list.
        TasksListFragment focusFragment = mSectionsPagerAdapter.getFragment(Sections.FOCUS);
        focusFragment.refreshTaskList(false);
    }

    private void animateTags(boolean isHiding) {
        DisplayMetrics displaymetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);

        float fromY = isHiding ? mAddTaskTagContainer.getY() : -displaymetrics.heightPixels;
        float toY = isHiding ? -displaymetrics.heightPixels : mTagsTranslationY;

        ObjectAnimator animator = ObjectAnimator.ofFloat(mAddTaskTagContainer, "translationY", fromY, toY);
        animator.setDuration(Constants.ANIMATION_DURATION_LONG).start();
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
        Animation slideDown = AnimationUtils.loadAnimation(this, R.anim.slide_down_out);
        slideDown.setAnimationListener(mHideButtonsListener);
        mActionButtonsContainer.startAnimation(slideDown);
    }

    public void showActionButtons() {
        Animation slideUp = AnimationUtils.loadAnimation(this, R.anim.slide_up_in);
        slideUp.setAnimationListener(mShowButtonsListener);
        mActionButtonsContainer.startAnimation(slideUp);
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

    private Animation.AnimationListener mShowButtonsListener = new Animation.AnimationListener() {
        @Override
        public void onAnimationStart(Animation animation) {
            mActionButtonsContainer.setVisibility(View.VISIBLE);
        }

        @Override
        public void onAnimationEnd(Animation animation) {
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
        TasksListFragment focusFragment = mSectionsPagerAdapter.getFragment(Sections.FOCUS);
        if (focusFragment != null) focusFragment.updateEmptyView();
    }

    private void customizeSelectionColors() {
        int background = ThemeUtils.isLightTheme(this) ?
                R.drawable.round_rectangle_light : R.drawable.round_rectangle_dark;
        mEditBarCount.setBackgroundResource(background);

        int textColor = ThemeUtils.isLightTheme(this) ? R.color.dark_text : R.color.light_text;
        mEditBarCount.setTextColor(getResources().getColor(textColor));
    }

    @OnClick(R.id.button_close_selection)
    protected void closeSelection() {
        mTasksService.sendBroadcast(Actions.SELECTION_CLEARED);
    }

    private void enableSelection() {
        mIsSelectionMode = true;

        showEditBar();

        mTasksService.sendBroadcast(Actions.SELECTION_STARTED);
    }

    public void cancelSelection() {
        mIsSelectionMode = false;

        hideEditBar();
    }

    public void updateSelectionCount(int count) {
        mEditBarCount.setText(String.valueOf(count));

        float alpha = count > 0 ? 1f : 0f;
        mEditBarCount.animate().alpha(alpha).setDuration(Constants.ANIMATION_DURATION_SHORT);
    }

    public boolean isSelectionMode() {
        return mIsSelectionMode;
    }

    @OnClick(R.id.button_close_workspaces)
    public void closeWorkspaces() {
        hideWorkspaces();

        // Clear selected tags.
        mSelectedFilterTags.clear();

        // Update lists.
        mTasksService.sendBroadcast(Actions.FILTER_BY_TAGS);
    }

    @OnClick(R.id.button_confirm_workspace)
    protected void confirmWorkspace() {
        hideWorkspaces();
    }

    public void showWorkspaces() {
        // Apply container color.
        mWorkspacesArea.setBackgroundColor(ThemeUtils.getBackgroundColor(this));

        // Animate views only when necessary.
        if (mWorkspacesView.getVisibility() == View.GONE) {
            Animation slideDown = AnimationUtils.loadAnimation(this, R.anim.slide_down_out);
            slideDown.setAnimationListener(mShowWorkspacesListener);
            mButtonAddTask.startAnimation(slideDown);

            Animation slideUp = AnimationUtils.loadAnimation(this, R.anim.slide_up_in);
            mWorkspacesView.startAnimation(slideUp);
        }

        // Load tags.
        loadWorkspacesTags();

        // Disable drag and drop.
        TasksListFragment focusFragment = mSectionsPagerAdapter.getFragment(Sections.FOCUS);
        focusFragment.setDragAndDropEnabled(false);
    }

    public void hideWorkspaces() {
        // Animate views only when necessary.
        if (mWorkspacesView.isShown()) {
            Animation slideDown = AnimationUtils.loadAnimation(this, R.anim.slide_down_out);
            slideDown.setAnimationListener(mHideWorkspacesListener);
            mWorkspacesView.startAnimation(slideDown);

            Animation slideUp = AnimationUtils.loadAnimation(this, R.anim.slide_up_in);
            mButtonAddTask.startAnimation(slideUp);
        }

        // Enable drag and drop.
        TasksListFragment focusFragment = mSectionsPagerAdapter.getFragment(Sections.FOCUS);
        focusFragment.setDragAndDropEnabled(true);
    }

    Animation.AnimationListener mShowWorkspacesListener = new Animation.AnimationListener() {
        @Override
        public void onAnimationStart(Animation animation) {
            mWorkspacesView.setVisibility(View.VISIBLE);
        }

        @Override
        public void onAnimationEnd(Animation animation) {
            mButtonAddTask.setVisibility(View.GONE);
        }

        @Override
        public void onAnimationRepeat(Animation animation) {
        }
    };

    Animation.AnimationListener mHideWorkspacesListener = new Animation.AnimationListener() {
        @Override
        public void onAnimationStart(Animation animation) {
            mButtonAddTask.setVisibility(View.VISIBLE);
        }

        @Override
        public void onAnimationEnd(Animation animation) {
            mWorkspacesView.setVisibility(View.GONE);
        }

        @Override
        public void onAnimationRepeat(Animation animation) {
        }
    };

    private void loadWorkspacesTags() {
        List<GsonTag> tags = mTasksService.loadAllAssignedTags();
        mSelectedFilterTags = new ArrayList<GsonTag>();

        mWorkspacesTags.removeAllViews();
        mWorkspacesTags.setVisibility(View.VISIBLE);
        mWorkspacesEmptyTags.setVisibility(View.GONE);

        // For each tag, add a checkbox as child view.
        for (GsonTag tag : tags) {
            int resource = ThemeUtils.isLightTheme(this) ? R.layout.tag_box_light : R.layout.tag_box_dark;
            CheckBox tagBox = (CheckBox) getLayoutInflater().inflate(resource, null);
            tagBox.setText(tag.getTitle());
            tagBox.setId(tag.getId().intValue());

            // Set listener to apply filter.
            tagBox.setOnClickListener(mFilterTagListener);

            // Add child view.
            mWorkspacesTags.addView(tagBox);
        }

        // If the list is empty, show empty view.
        if (tags.isEmpty()) {
            mWorkspacesTags.setVisibility(View.GONE);
            mWorkspacesEmptyTags.setVisibility(View.VISIBLE);

            int hintColor = ThemeUtils.isLightTheme(this) ? R.color.light_hint : R.color.dark_hint;
            mWorkspacesEmptyTags.setTextColor(getResources().getColor(hintColor));
        }
    }

    private View.OnClickListener mFilterTagListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            GsonTag selectedTag = mTasksService.loadTag((long) view.getId());

            // Add or remove tag from selected filters.
            if (mSelectedFilterTags.contains(selectedTag)) {
                mSelectedFilterTags.remove(selectedTag);
            } else {
                mSelectedFilterTags.add(selectedTag);
            }

            mTasksService.sendBroadcast(Actions.FILTER_BY_TAGS);
        }
    };

    public List<GsonTag> getSelectedFilterTags() {
        return mSelectedFilterTags;
    }

    private void loadSearchBar() {
        mSearchClose.setOnClickListener(mCloseSearchListener);
        mSearchClose.setTextColor(Color.WHITE);

        mSearchField.setOnFocusChangeListener(mSearchFocusListener);
        mSearchField.addTextChangedListener(mSearchTypeListener);
        mSearchField.setOnEditorActionListener(mSearchDoneListener);
    }

    private View.OnClickListener mCloseSearchListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            // Hide search bar.
            hideSearch();
        }
    };

    private View.OnFocusChangeListener mSearchFocusListener = new View.OnFocusChangeListener() {
        @Override
        public void onFocusChange(View view, boolean hasFocus) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (hasFocus) {
                imm.toggleSoftInput(InputMethodManager.SHOW_IMPLICIT, InputMethodManager.HIDE_IMPLICIT_ONLY);
            } else {
                imm.hideSoftInputFromWindow(mSearchField.getWindowToken(), 0);
            }
        }
    };

    private TextWatcher mSearchTypeListener = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {
        }

        @Override
        public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
        }

        @Override
        public void afterTextChanged(Editable editable) {
            mSearchQuery = mSearchField.getText().toString().toLowerCase();

            mTasksService.sendBroadcast(Actions.PERFORM_SEARCH);
        }
    };

    private TextView.OnEditorActionListener mSearchDoneListener =
            new TextView.OnEditorActionListener() {
                @Override
                public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                    if (actionId == EditorInfo.IME_ACTION_DONE) {
                        // Close keyboard on done key pressed.
                        mTasksArea.requestFocus();
                    }
                    return true;
                }
            };

    private void showSearch() {
        int duration = Constants.ANIMATION_DURATION_MEDIUM;

        // Fade in search bar.
        mSearchBar.setVisibility(View.VISIBLE);
        mSearchBar.animate().alpha(1f).setDuration(duration).setListener(null).start();

        // Fade out toolbar.
        mToolbar.animate().alpha(0f).setDuration(duration).setListener(mShowSearchListener).start();
        mButtonAddTask.animate().alpha(0f).setDuration(Constants.ANIMATION_DURATION_SHORT).start();

        // Focus on search field.
        mSearchField.requestFocus();

        // Set flag.
        mIsSearchActive = true;
    }

    private AnimatorListenerAdapter mShowSearchListener = new AnimatorListenerAdapter() {
        @Override
        public void onAnimationEnd(Animator animation) {
            // Hide toolbar.
            mToolbar.setVisibility(View.GONE);
        }
    };

    public void hideSearch() {
        int duration = Constants.ANIMATION_DURATION_MEDIUM;

        // Fade out search bar.
        mSearchBar.animate().alpha(0f).setDuration(duration).setListener(mHideSearchListener).start();

        // Fade in toolbar.
        mToolbar.setVisibility(View.VISIBLE);
        mToolbar.animate().alpha(1f).setDuration(duration).setListener(null).start();
        mButtonAddTask.animate().alpha(1f).setDuration(Constants.ANIMATION_DURATION_SHORT).start();

        // Clear search field.
        mSearchField.setText("");
        mSearchField.clearFocus();

        // Reset flag.
        mIsSearchActive = false;
    }

    private AnimatorListenerAdapter mHideSearchListener = new AnimatorListenerAdapter() {
        @Override
        public void onAnimationEnd(Animator animation) {
            // Hide search bar.
            mSearchBar.setVisibility(View.GONE);
        }
    };

    public boolean isSearchActive() {
        return mIsSearchActive;
    }

    public String getSearchQuery() {
        return mSearchQuery;
    }

    private void startLogin() {
        // Call Parse login activity.
        ParseLoginBuilder builder = new ParseLoginBuilder(this);
        startActivityForResult(builder.build(), Constants.LOGIN_REQUEST_CODE);
    }

    private void askToKeepData() {
        // Display confirmation dialog.
        new SwipesDialog.Builder(this)
                .title(R.string.keep_data_dialog_title)
                .content(R.string.keep_data_dialog_message)
                .positiveText(R.string.keep_data_dialog_yes)
                .negativeText(R.string.keep_data_dialog_no)
                .actionsColorRes(R.color.neutral_accent)
                .cancelable(false)
                .callback(new MaterialDialog.ButtonCallback() {
                    @Override
                    public void onPositive(MaterialDialog dialog) {
                        // Save data from test period for sync.
                        saveDataForSync();
                    }

                    @Override
                    public void onNegative(MaterialDialog dialog) {
                        // Clear data from test period.
                        clearData();
                    }
                })
                .dismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialogInterface) {
                        // Sync user data.
                        performInitialSync();
                    }
                })
                .show();
    }

    private void clearData() {
        // Clear user data.
        mTasksService.clearAllData();

        // Refresh lists.
        refreshSections();
    }

    private void saveDataForSync() {
        // Save all tags for syncing.
        for (GsonTag tag : mTasksService.loadAllTags()) {
            mSyncService.saveTagForSync(tag);
        }

        // Save all tasks for syncing.
        for (GsonTask task : mTasksService.loadAllTasks()) {
            if (!task.getDeleted()) {
                task.setId(null);
                mSyncService.saveTaskChangesForSync(task, null);
            }
        }
    }

    private void performInitialSync() {
        // Unblock sync.
        mShouldSkipSync = false;

        // Perform initial sync.
        startSync();
    }

    private void showWelcomeDialog() {
        // Display welcome dialog.
        new SwipesDialog.Builder(this)
                .title(R.string.welcome_dialog_title)
                .content(R.string.welcome_dialog_message)
                .positiveText(R.string.welcome_dialog_yes)
                .negativeText(R.string.welcome_dialog_no)
                .actionsColorRes(R.color.neutral_accent)
                .callback(new MaterialDialog.ButtonCallback() {
                    @Override
                    public void onPositive(MaterialDialog dialog) {
                        // Returning user. Call login.
                        startLogin();

                        // Clear data before initial sync.
                        mShouldClearData = true;
                    }
                })
                .dismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialogInterface) {
                        // Set dialog as shown.
                        PreferenceUtils.saveStringPreference(PreferenceUtils.WELCOME_DIALOG, "YES", mContext.get());
                    }
                })
                .show();
    }

    private View.OnClickListener mNavigationToggleListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            if (!mNavigationMenu.isShown()) {
                // Change action bar colors.
                transitionNavigationArea(Color.WHITE, ThemeUtils.getSectionColorDark(mCurrentSection, mContext.get()));

                // Animate in navigation menu.
                showNavigationMenu();
            } else {
                // Reset action bar colors.
                transitionNavigationArea(ThemeUtils.getSectionColorDark(mCurrentSection, mContext.get()), Color.WHITE);

                // Animate out navigation menu.
                hideNavigationMenu();
            }
        }
    };

    private void showNavigationMenu() {
        // Apply container color.
        mNavigationMenuContainer.setBackgroundColor(ThemeUtils.getBackgroundColor(this));

        // Apply button colors.
        updateNavigationButtons();

        // Slide menu in from the top.
        Animation slideDown = AnimationUtils.loadAnimation(this, R.anim.slide_down_in);
        mNavigationMenu.setVisibility(View.VISIBLE);
        mNavigationMenu.startAnimation(slideDown);

        // Set menu as shown.
        mIsShowingNavigation = true;
    }

    private void hideNavigationMenu() {
        // Slide menu out to the top.
        Animation slideUp = AnimationUtils.loadAnimation(this, R.anim.slide_up_out);
        slideUp.setAnimationListener(mHideNavigationMenuListener);
        mNavigationMenu.startAnimation(slideUp);

        // Set menu as hidden.
        mIsShowingNavigation = false;
    }

    private Animation.AnimationListener mHideNavigationMenuListener = new Animation.AnimationListener() {
        @Override
        public void onAnimationStart(Animation animation) {
        }

        @Override
        public void onAnimationEnd(Animation animation) {
            mNavigationMenu.setVisibility(View.GONE);
        }

        @Override
        public void onAnimationRepeat(Animation animation) {
        }
    };

    private void transitionNavigationArea(final int fromColor, final int toColor) {
        ValueAnimator anim = ValueAnimator.ofFloat(0, 1);
        anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                // Blend colors according to position.
                float position = animation.getAnimatedFraction();
                int blended = ColorUtils.blendColors(fromColor, toColor, position);

                // Change colors of toolbar custom view.
                mActionBarTitle.setTextColor(blended);
                mActionBarIcon.setTextColor(blended);
            }
        });

        anim.setDuration(Constants.ANIMATION_DURATION_MEDIUM).start();
    }

    private void paintNavigationArea(int color) {
        // Change colors according to state.
        if (mIsShowingNavigation) {
            mActionBarTitle.setTextColor(color);
            mActionBarIcon.setTextColor(color);
        } else {
            mActionBarTitle.setTextColor(Color.WHITE);
            mActionBarIcon.setTextColor(Color.WHITE);
        }
    }

    private void updateNavigationButtons() {
        // Reset button colors.
        mNavigationLaterButton.setTextColor(getResources().getColor(R.color.neutral_gray));
        mNavigationFocusButton.setTextColor(getResources().getColor(R.color.neutral_gray));
        mNavigationDoneButton.setTextColor(getResources().getColor(R.color.neutral_gray));

        // Highlight selected section.
        switch (mCurrentSection) {
            case LATER:
                mNavigationLaterButton.setTextColor(ThemeUtils.getSectionColor(mCurrentSection, this));
                break;
            case FOCUS:
                mNavigationFocusButton.setTextColor(ThemeUtils.getSectionColor(mCurrentSection, this));
                break;
            case DONE:
                mNavigationDoneButton.setTextColor(ThemeUtils.getSectionColor(mCurrentSection, this));
                break;
        }
    }

    @OnClick(R.id.navigation_later_button)
    protected void navigationLaterClick() {
        // Navigate to Later section.
        navigateToSection(Sections.LATER);
    }

    @OnClick(R.id.navigation_focus_button)
    protected void navigationFocusClick() {
        // Navigate to Focus section.
        navigateToSection(Sections.FOCUS);
    }

    @OnClick(R.id.navigation_done_button)
    protected void navigationDoneClick() {
        // Navigate to Done section.
        navigateToSection(Sections.DONE);
    }

    private void navigateToSection(Sections section) {
        // Change section.
        mViewPager.setCurrentItem(section.getSectionNumber());

        // Hide navigation.
        hideNavigationMenu();
    }

}
