package com.swipesapp.android.ui.activity;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.annotation.TargetApi;
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
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.util.TypedValue;
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
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.fortysevendeg.swipelistview.DynamicViewPager;
import com.github.amlcurran.showcaseview.OnShowcaseEventListener;
import com.github.amlcurran.showcaseview.ShowcaseView;
import com.github.amlcurran.showcaseview.targets.ViewTarget;
import com.github.clans.fab.FloatingActionButton;
import com.parse.ParseUser;
import com.parse.ui.ParseExtras;
import com.parse.ui.ParseLoginBuilder;
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
import com.swipesapp.android.db.migration.MigrationAssistant;
import com.swipesapp.android.handler.SettingsHandler;
import com.swipesapp.android.handler.SoundHandler;
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
import com.swipesapp.android.ui.view.SwipesTextView;
import com.swipesapp.android.util.ColorUtils;
import com.swipesapp.android.util.DeviceUtils;
import com.swipesapp.android.util.PreferenceUtils;
import com.swipesapp.android.util.ThemeUtils;
import com.swipesapp.android.values.Constants;
import com.swipesapp.android.values.Intents;
import com.swipesapp.android.values.Sections;
import com.swipesapp.android.widget.NowWidgetProvider;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import io.intercom.android.sdk.Intercom;

public class TasksActivity extends BaseActivity {

    @InjectView(R.id.tasks_area)
    RelativeLayout mTasksArea;

    @InjectView(R.id.pager)
    DynamicViewPager mViewPager;

    @InjectView(R.id.toolbar_area)
    FrameLayout mToolbarArea;

    @InjectView(R.id.button_add_task)
    FloatingActionButton mButtonAddTask;

    @InjectView(R.id.edit_tasks_bar)
    FrameLayout mEditTasksBar;
    @InjectView(R.id.edit_bar_area)
    RelativeLayout mEditBarArea;
    @InjectView(R.id.edit_bar_selection_count)
    TextView mEditBarCount;

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
    SwipesTextView mSearchClose;
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
    private FactorSpeedScroller mScroller;

    private Set<GsonTag> mSelectedFilterTags;

    private View mActionBarView;
    private TextView mActionBarTitle;
    private SwipesTextView mActionBarIcon;

    private float mPreviousOffset;
    private boolean mHasChangedTab;
    private boolean mIsSwipingScreens;

    private boolean mCalledAddTask;
    private String mAddedTaskId;
    private boolean mHasAddedSnoozedTask;

    private String mShareMessage;

    private boolean mWasRestored;
    private static boolean sHasPendingRefresh;
    private Handler mRefreshHandler = new Handler();

    private boolean mIsSelectionMode;

    private boolean mIsSearchActive;
    private String mSearchQuery;

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

        sendAppLaunchEvent();

        if (mCurrentSection == null) mCurrentSection = Sections.FOCUS;

        setupViewPager();

        // Define a custom duration to the page scroller, providing a more natural feel.
        customizeScroller();

        mSelectedFilterTags = new LinkedHashSet<>();
        mSearchQuery = "";

        customizeSelectionColors();

        loadSearchBar();
    }

    @Override
    protected void onDestroy() {
        ButterKnife.reset(this);
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        if (mWorkspacesView.isShown()) {
            // Close workspaces and refresh.
            closeWorkspaces();
        } else {
            // Forward call to listeners.
            mTasksService.sendBroadcast(Intents.BACK_PRESSED);
        }
    }

    @Override
    protected void onResume() {
        // Create filter and start receiver.
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intents.TASKS_CHANGED);

        registerReceiver(mTasksReceiver, filter);

        if (mWasRestored) {
            // Reset section.
            mViewPager.setCurrentItem(Sections.FOCUS.getSectionNumber());
        }

        // Refresh lists if needed.
        if (sHasPendingRefresh) {
            refreshSections(false);
        }

        // Restore section colors.
        setupSystemBars(mCurrentSection);

        // Clear activity state flags.
        mWasRestored = false;

        // Send screen view event.
        Analytics.sendScreenView(mCurrentSection.getScreenName());

        super.onResume();
    }

    @Override
    protected void onStart() {
        // Start sync when coming from the background.
        if (SwipesApplication.isInBackground()) {
            startSync();

            // Read user settings.
            SettingsHandler.readSettingsFromServer(this);

            // Start sound handler.
            SoundHandler.load(this);
        }

        super.onStart();
    }

    @Override
    protected void onPause() {
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
                    // Perform initial setup again.
                    performInitialSetup();

                    // Reset section.
                    mViewPager.setCurrentItem(Sections.FOCUS.getSectionNumber());

                    // Change visibility of login menu.
                    invalidateOptionsMenu();

                    // Refresh all lists.
                    refreshSections(true);

                    // Start syncing.
                    startSync();
                    break;
                case Constants.LOCALE_CHANGED_RESULT_CODE:
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            // Locale has changed. Reload activity.
                            recreate();
                        }
                    }, 1);
                    break;
            }
        } else if (requestCode == Constants.LOGIN_REQUEST_CODE) {
            switch (resultCode) {
                case Activity.RESULT_OK:
                    // Login successful.
                    if (mShouldClearData) {
                        // Clear data immediately.
                        clearData();
                        performInitialSync();
                    } else if (mTasksService.countAllTasks() > 0) {
                        // Ask to keep user data.
                        askToKeepData();
                    }

                    // Reset flag.
                    mShouldClearData = false;

                    if (PreferenceUtils.hasTriedOut(this)) {
                        // End anonymous Intercom session.
                        Intercom.client().reset();
                    }

                    if (data != null) {
                        boolean signedUp = data.getBooleanExtra(ParseExtras.EXTRA_SIGNED_UP, false);
                        String email = data.getStringExtra(ParseExtras.EXTRA_USER_EMAIL);

                        if (signedUp) {
                            // Send signup event to analytics.
                            sendSignupEvent();
                        } else {
                            // Send login event to analytics.
                            sendLoginEvent();
                        }

                        // Start Intercom session with email.
                        IntercomHandler.beginIntercomSession(email);
                    }

                    // Update user level dimension.
                    Analytics.sendUserLevel(this);

                    // Subscribe to push channels.
                    SwipesApplication.subscribePush(this);

                    // Read user settings.
                    SettingsHandler.readSettingsFromServer(this);

                    // Change visibility of login menu.
                    invalidateOptionsMenu();
                    break;
                case Activity.RESULT_CANCELED:
                    // Keep data unless told otherwise.
                    mShouldClearData = false;
                    break;
            }

            // Fade in tasks list if needed.
            TasksListFragment focusFragment = mSectionsPagerAdapter.getFragment(Sections.FOCUS);
            focusFragment.fadeInTasksList();

        } else if (requestCode == Constants.ADD_TASK_REQUEST_CODE) {
            if (resultCode != RESULT_CANCELED) {
                // Load added task ID.
                mAddedTaskId = data.getStringExtra(Constants.EXTRA_TASK_ID);

                // Check if added task was snoozed.
                if (resultCode == Constants.ADDED_SNOOZED_TASK_RESULT_CODE) {
                    if (PreferenceUtils.isAutoScrollEnabled(this)) {
                        // Auto-scroll by default when workspace is inactive.
                        boolean shouldScroll = mSelectedFilterTags.isEmpty();

                        if (!shouldScroll) {
                            GsonTask task = mTasksService.loadTask(mAddedTaskId);

                            // When a workspace is active, make sure the added task is visible (i.e. has one
                            // of the selected tags) before auto-scrolling.
                            for (GsonTag filterTag : mSelectedFilterTags) {
                                for (GsonTag tag : task.getTags()) {
                                    if (tag.getTempId().equals(filterTag.getTempId())) {
                                        // Tag found. Task is visible, auto-scroll can happen.
                                        shouldScroll = true;
                                    }
                                }
                            }
                        }

                        if (shouldScroll) {
                            // Wait for fade animation to complete.
                            new Handler().postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    // Move to later section.
                                    mScroller.setDuration(FactorSpeedScroller.DURATION_LONG);
                                    mViewPager.setCurrentItem(Sections.LATER.getSectionNumber());
                                }
                            }, Constants.ANIMATION_DURATION_MEDIUM);

                        }

                        // Mark added task as snoozed.
                        mHasAddedSnoozedTask = true;
                    }
                }
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
    public boolean onPrepareOptionsMenu(Menu menu) {
        // HACK: Show action icons.
        if (menu != null) {
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

        return super.onPrepareOptionsMenu(menu);
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
        // Save install date on first launch.
        WelcomeHandler.checkFirstLaunch(this);

        // Perform migrations when needed.
        MigrationAssistant.performUpgrades(mContext.get());

        // Show welcome dialog only once.
        if (!PreferenceUtils.hasShownWelcomeScreen(this)) {
            // Welcome user.
            showWelcomeDialog();
        }

        // Save welcome tasks on first run for the user.
        WelcomeHandler.addWelcomeTasks(this);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void setupActionBarCustomView() {
        // Inflate custom view.
        LayoutInflater inflater = LayoutInflater.from(this);
        mActionBarView = inflater.inflate(R.layout.action_bar_custom_view, null);
        mActionBarTitle = (TextView) mActionBarView.findViewById(R.id.action_bar_title);
        mActionBarIcon = (SwipesTextView) mActionBarView.findViewById(R.id.action_bar_icon);

        // Enable navigation menu on portrait only.
        if (DeviceUtils.isLandscape(this)) {
            mActionBarView.setClickable(false);
        } else {
            mActionBarView.setOnClickListener(mNavigationToggleListener);
        }

        // Apply borderless ripple on Lollipop.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            TypedValue outValue = new TypedValue();
            getTheme().resolveAttribute(android.R.attr.selectableItemBackgroundBorderless, outValue, true);
            mActionBarView.setBackgroundResource(outValue.resourceId);
        }
    }

    private void setupSystemBars(Sections section) {
        // Setup toolbar icon.
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
                    mTasksService.sendBroadcast(Intents.TAB_CHANGED);
                    mHasChangedTab = false;

                    // Send screen view event.
                    Analytics.sendScreenView(mCurrentSection.getScreenName());
                }

                mActionBarView.setAlpha(1f);

                if (sHasPendingRefresh) {
                    Runnable refreshRunnable = new Runnable() {
                        @Override
                        public void run() {
                            // Finish refreshing lists.
                            refreshAdapters();
                        }
                    };

                    // Wait a little before refreshing.
                    mRefreshHandler.postDelayed(refreshRunnable, 1000);
                }

                if (mCalledAddTask) {
                    // Call add task screen.
                    callAddTask();

                    // Reset flag.
                    mCalledAddTask = false;
                }

                // Reset scroller speed.
                mScroller.setDuration(FactorSpeedScroller.DURATION_MEDIUM);

                mIsSwipingScreens = false;
            } else if (state == ViewPager.SCROLL_STATE_DRAGGING) {
                mIsSwipingScreens = true;

                // User is swiping again. Cancel refresh.
                mRefreshHandler.removeCallbacksAndMessages(null);
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
            themeStatusBar(ColorUtils.blendColors(fromColor, toColor, positionOffset));

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

    public boolean isSwipingScreens() {
        return mIsSwipingScreens;
    }

    private BroadcastReceiver mTasksReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Filter intent actions.
            if (intent.getAction().equals(Intents.TASKS_CHANGED)) {
                // Perform refresh of all sections.
                refreshSections(true);
            }
        }
    };

    public void refreshSections(boolean refreshWidgets) {
        // Clear pending refresh flag.
        sHasPendingRefresh = false;

        // Refresh lists without animation.
        for (TasksListFragment fragment : mSectionsPagerAdapter.getFragments()) {
            if (fragment != null) fragment.refreshTaskList(false);
        }

        // Refresh app widgets.
        if (refreshWidgets) refreshWidgets(this);
    }

    public static void refreshWidgets(Context context) {
        // Refresh main widget.
        NowWidgetProvider.refreshWidget(context);
    }

    private void refreshAdapters() {
        // Clear pending refresh flag.
        sHasPendingRefresh = false;

        // Find out current refresh type.
        boolean isFilter = !mSelectedFilterTags.isEmpty();
        boolean isSearch = !mSearchQuery.isEmpty();

        // Update adapters with lists already loaded.
        if (!isFilter && !isSearch) {
            // Perform regular update.
            for (TasksListFragment fragment : mSectionsPagerAdapter.getFragments()) {
                if (fragment != null) fragment.updateAdapter(false);
            }
        } else if (isFilter) {
            // Perform filters update.
            for (TasksListFragment fragment : mSectionsPagerAdapter.getFragments()) {
                if (fragment != null) fragment.updateFilterAdapter();
            }
        } else {
            // Perform search update.
            for (TasksListFragment fragment : mSectionsPagerAdapter.getFragments()) {
                if (fragment != null) fragment.updateSearchAdapter();
            }
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
            mScroller = new FactorSpeedScroller(this);
            scroller.set(mViewPager, mScroller);
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

    @OnClick(R.id.button_add_task)
    protected void startAddTaskWorkflow() {
        // Go to main fragment if needed.
        if (mCurrentSection != Sections.FOCUS) {
            // Use short scroll duration for faster input.
            mScroller.setDuration(FactorSpeedScroller.DURATION_SHORT);
            mViewPager.setCurrentItem(Sections.FOCUS.getSectionNumber());

            // Set flag to call add task after scrolling ends.
            mCalledAddTask = true;
        } else {
            callAddTask();
        }
    }

    private void callAddTask() {
        // Prepare selected filter tags.
        ArrayList<Integer> tagIds = new ArrayList<>();
        for (GsonTag tag : mSelectedFilterTags) {
            tagIds.add(tag.getId().intValue());
        }

        // Call add task activity.
        Intent intent = new Intent(this, AddTasksActivity.class);
        intent.putIntegerArrayListExtra(Constants.EXTRA_TAG_IDS, tagIds);

        startActivityForResult(intent, Constants.ADD_TASK_REQUEST_CODE);
    }

    @OnClick(R.id.button_assign_tags)
    protected void assignTags() {
        // Send a broadcast to assign tags to the selected tasks. The fragment should handle it.
        mTasksService.sendBroadcast(Intents.ASSIGN_TAGS);
    }

    @OnClick(R.id.button_delete_tasks)
    protected void deleteTasks() {
        // Send a broadcast to delete tasks. The fragment should handle it, since it contains the list.
        mTasksService.sendBroadcast(Intents.DELETE_TASKS);
    }

    @OnClick(R.id.button_share_tasks)
    protected void shareTasks() {
        // Send a broadcast to share selected tasks. The fragment should handle it.
        mTasksService.sendBroadcast(Intents.SHARE_TASKS);
    }

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

        // Send analytics event.
        sendSharingMessageEvent();
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
                    .actionsColor(ThemeUtils.getSectionColor(mCurrentSection, mContext.get()))
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
        sendTagAddedEvent((long) title.length(), Labels.TAGS_FROM_FILTER);

        mSelectedFilterTags.add(tag);
        loadWorkspacesTags();

        // Refresh workspace results.
        mTasksService.sendBroadcast(Intents.FILTER_BY_TAGS);

        // Perform sync.
        mSyncService.performSync(true, Constants.SYNC_DELAY);

        // Play sound.
        SoundHandler.playSound(this, R.raw.action_positive);
    }

    private void confirmEditTag(GsonTag selectedTag) {
        // Save tag to database.
        mTasksService.editTag(selectedTag, true);

        // Refresh displayed tags.
        loadWorkspacesTags();

        // Refresh workspace results.
        mTasksService.sendBroadcast(Intents.FILTER_BY_TAGS);

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
        mTasksService.sendBroadcast(Intents.SELECTION_CLEARED);
    }

    private void enableSelection() {
        mIsSelectionMode = true;

        showEditBar();

        mTasksService.sendBroadcast(Intents.SELECTION_STARTED);
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

        if (!mSelectedFilterTags.isEmpty()) {
            // Play sound.
            SoundHandler.playSound(this, R.raw.action_negative);
        }

        // Clear selected tags.
        mSelectedFilterTags.clear();
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
        } else {
            // Update lists.
            mTasksService.sendBroadcast(Intents.FILTER_BY_TAGS);
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

            // Update lists.
            mTasksService.sendBroadcast(Intents.FILTER_BY_TAGS);
        }

        @Override
        public void onAnimationRepeat(Animation animation) {
        }
    };

    private void loadWorkspacesTags() {
        List<GsonTag> tags = mTasksService.loadAllTags();

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
            tagBox.setOnLongClickListener(mFilterTagEditListener);

            // Pre-select tag if needed.
            if (mSelectedFilterTags.contains(tag)) tagBox.setChecked(true);

            // Add child view.
            mWorkspacesTags.addView(tagBox);
        }

        // Create add tag button.
        SwipesTextView button = (SwipesTextView) getLayoutInflater().inflate(R.layout.tag_add_button, null);
        button.setOnClickListener(mAddTagListener);
        button.enableTouchFeedback();

        // Add child view.
        mWorkspacesTags.addView(button);

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

            // Update results.
            mTasksService.sendBroadcast(Intents.FILTER_BY_TAGS);
        }
    };

    private View.OnLongClickListener mFilterTagEditListener = new View.OnLongClickListener() {
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
                .actionsColor(ThemeUtils.getSectionColor(mCurrentSection, mContext.get()))
                .callback(new MaterialDialog.ButtonCallback() {
                    @Override
                    public void onPositive(MaterialDialog dialog) {
                        // Delete tag and unassign it from all tasks.
                        mTasksService.deleteTag(selectedTag.getId());

                        // Send analytics event.
                        sendTagDeletedEvent(Labels.TAGS_FROM_FILTER);

                        // Refresh displayed tags.
                        loadWorkspacesTags();

                        // Update results.
                        mTasksService.sendBroadcast(Intents.FILTER_BY_TAGS);

                        // Perform sync.
                        mSyncService.performSync(true, Constants.SYNC_DELAY);

                        // Play sound.
                        SoundHandler.playSound(mContext.get(), R.raw.action_negative);
                    }
                })
                .show();
    }

    public Set<GsonTag> getSelectedFilterTags() {
        return mSelectedFilterTags;
    }

    private void loadSearchBar() {
        mSearchClose.setOnClickListener(mCloseSearchListener);
        mSearchClose.setTextColor(Color.WHITE);
        mSearchClose.enableTouchFeedback();

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

            mTasksService.sendBroadcast(Intents.PERFORM_SEARCH);
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

        // Send screen view event.
        Analytics.sendScreenView(Screens.SCREEN_LOGIN);
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
        refreshSections(true);
    }

    private void saveDataForSync() {
        // Save all tags for syncing.
        for (GsonTag tag : mTasksService.loadAllTags()) {
            mSyncService.saveTagForSync(tag);
        }

        // Load all non-deleted tasks.
        List<GsonTask> tasksToSave = new ArrayList<>();
        for (GsonTask task : mTasksService.loadAllTasks()) {
            if (!task.getDeleted()) {
                task.setId(null);
                tasksToSave.add(task);
            }
        }

        // Save tasks for syncing.
        mSyncService.saveTasksForSync(tasksToSave);
    }

    private void performInitialSync() {
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
                        PreferenceUtils.saveString(PreferenceUtils.WELCOME_DIALOG, "YES", mContext.get());

                        // Update user level dimension.
                        Analytics.sendUserLevel(mContext.get());

                        if (!mShouldClearData) {
                            // User chose to try out. Send event.
                            Analytics.sendEvent(Categories.ONBOARDING, Actions.TRYING_OUT,
                                    null, Analytics.getDaysSinceInstall(mContext.get()));

                            // Save try out state.
                            PreferenceUtils.saveBoolean(PreferenceUtils.DID_TRY_OUT, true, mContext.get());

                            // Show navigation menu tutorial.
                            showNavigationTutorial();

                            // Start anonymous Intercom session.
                            IntercomHandler.beginIntercomSession(null);
                        }
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

    private void resetNavigationArea() {
        // Reset action bar colors.
        transitionNavigationArea(ThemeUtils.getSectionColorDark(mCurrentSection, mContext.get()), Color.WHITE);

        // Hide navigation.
        hideNavigationMenu();
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
        if (mCurrentSection != Sections.LATER) {
            // Navigate to Later section.
            navigateToSection(Sections.LATER);
        } else {
            resetNavigationArea();
        }
    }

    @OnClick(R.id.navigation_focus_button)
    protected void navigationFocusClick() {
        if (mCurrentSection != Sections.FOCUS) {
            // Navigate to Focus section.
            navigateToSection(Sections.FOCUS);
        } else {
            resetNavigationArea();
        }
    }

    @OnClick(R.id.navigation_done_button)
    protected void navigationDoneClick() {
        if (mCurrentSection != Sections.DONE) {
            // Navigate to Done section.
            navigateToSection(Sections.DONE);
        } else {
            resetNavigationArea();
        }
    }

    private void navigateToSection(Sections section) {
        // Change section.
        mViewPager.setCurrentItem(section.getSectionNumber());

        // Hide navigation.
        hideNavigationMenu();
    }

    private boolean isDoneForToday() {
        TasksListFragment focusFragment = mSectionsPagerAdapter.getFragment(Sections.FOCUS);
        return focusFragment.isDoneForToday();
    }

    private void showNavigationTutorial() {
        String tutorialText = getString(R.string.navigation_tutorial_text);

        // Show tutorial.
        displayShowcaseView(mActionBarView, tutorialText, mNavigationTutorialListener);
    }

    private OnShowcaseEventListener mNavigationTutorialListener = new OnShowcaseEventListener() {
        @Override
        public void onShowcaseViewHide(ShowcaseView showcaseView) {
            // Fade in tasks list.
            TasksListFragment focusFragment = mSectionsPagerAdapter.getFragment(Sections.FOCUS);
            focusFragment.fadeInTasksList();

            // Fade in floating button.
            mButtonAddTask.animate().alpha(1f).setDuration(Constants.ANIMATION_DURATION_MEDIUM);

            // Hide navigation menu if needed.
            if (mIsShowingNavigation) resetNavigationArea();

            // Reset navigation to main section.
            mViewPager.setCurrentItem(Sections.FOCUS.getSectionNumber());
        }

        @Override
        public void onShowcaseViewDidHide(ShowcaseView showcaseView) {
        }

        @Override
        public void onShowcaseViewShow(ShowcaseView showcaseView) {
            // Fade out tasks list.
            TasksListFragment focusFragment = mSectionsPagerAdapter.getFragment(Sections.FOCUS);
            focusFragment.fadeOutTasksList();

            // Fade out floating button.
            mButtonAddTask.animate().alpha(0f).setDuration(Constants.ANIMATION_DURATION_MEDIUM);
        }
    };

    private void displayShowcaseView(View target, String text, OnShowcaseEventListener listener) {
        // Load showcase button parameters.
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        int margin = getResources().getDimensionPixelSize(R.dimen.showcase_button_margin);
        params.setMargins(margin, margin, margin, margin);

        // Display showcase.
        ViewTarget viewTarget = new ViewTarget(target);
        ShowcaseView showcase = new ShowcaseView.Builder(this, true)
                .setTarget(viewTarget)
                .setContentText(text)
                .setStyle(R.style.Showcase_Theme)
                .setShowcaseEventListener(listener)
                .build();
        showcase.setButtonPosition(params);

        // Apply Lollipop padding fix.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            int paddingBottom = getResources().getDimensionPixelSize(R.dimen.showcase_padding_bottom);
            showcase.setPadding(0, 0, 0, paddingBottom);
        }
    }

    public static void setPendingRefresh() {
        // Set flag to refresh lists.
        sHasPendingRefresh = true;
    }

    public static boolean hasPendingRefresh() {
        return sHasPendingRefresh;
    }

    public String getAddedTaskId() {
        // ID of the last added task. Can be null very often.
        return mAddedTaskId;
    }

    public boolean hasAddedSnoozedTask() {
        return mHasAddedSnoozedTask;
    }

    public void clearAddedSnozedTask() {
        // Clear flag after adding a snoozed task.
        mHasAddedSnoozedTask = false;
    }

    private void sendAppLaunchEvent() {
        String label = Labels.APP_LAUNCH_DIRECT;
        long value = Analytics.getDaysSinceInstall(this);

        boolean fromNotifications = getIntent().getBooleanExtra(Constants.EXTRA_FROM_NOTIFICATIONS, false);
        if (fromNotifications) label = Labels.APP_LAUNCH_LOCAL_NOTIFICATION;

        // Send app launch event.
        Analytics.sendEvent(Categories.SESSION, Actions.APP_LAUNCH, label, value);
    }

    private void sendLoginEvent() {
        // Check if user tried out the app.
        boolean didTryOut = PreferenceUtils.hasTriedOut(this);
        String label = didTryOut ? Labels.TRY_OUT_YES : Labels.TRY_OUT_NO;

        // Send login event.
        Analytics.sendEvent(Categories.ONBOARDING, Actions.LOGGED_IN, label, null);
    }

    private void sendSignupEvent() {
        // Check if user tried out the app.
        boolean didTryOut = PreferenceUtils.hasTriedOut(this);
        String label = didTryOut ? Labels.TRY_OUT_YES : Labels.TRY_OUT_NO;

        // Send login event.
        Analytics.sendEvent(Categories.ONBOARDING, Actions.SIGNED_UP, label, null);
    }

    private void sendSharingMessageEvent() {
        long value = isDoneForToday() ? 1 : 0;
        String valueIntercom = isDoneForToday() ? Labels.DONE_TODAY : Labels.DONE_NOW;

        // Send analytics event.
        Analytics.sendEvent(Categories.SHARING, Actions.SHARE_MESSAGE_OPEN, mShareMessage, value);

        // Prepare Intercom fields.
        HashMap<String, Object> fields = new HashMap<>();
        fields.put(IntercomFields.DONE_FOR_TODAY, valueIntercom);

        // Send Intercom events.
        IntercomHandler.sendEvent(IntercomEvents.SHARE_MESSAGE_OPENED, fields);
    }

    public static void sendTagDeletedEvent(String from) {
        // Send analytics event.
        Analytics.sendEvent(Categories.TAGS, Actions.DELETED_TAG, from, null);

        // Prepare Intercom fields.
        HashMap<String, Object> fields = new HashMap<>();
        fields.put(IntercomFields.FROM, from);

        // Send Intercom events.
        IntercomHandler.sendEvent(IntercomEvents.DELETED_TAG, fields);
    }

    public static void sendTagAddedEvent(long length, String from) {
        // Send analytics event.
        Analytics.sendEvent(Categories.TAGS, Actions.ADDED_TAG, from, length);

        // Prepare Intercom fields.
        HashMap<String, Object> fields = new HashMap<>();
        fields.put(IntercomFields.LENGHT, length);
        fields.put(IntercomFields.FROM, from);

        // Send Intercom events.
        IntercomHandler.sendEvent(IntercomEvents.ADDED_TAG, fields);
    }

}
