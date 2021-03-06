package com.swipesapp.android.ui.fragments;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.app.ListFragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.text.Html;
import android.text.InputType;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.view.ViewTreeObserver;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.fortysevendeg.swipelistview.BaseSwipeListViewListener;
import com.fortysevendeg.swipelistview.DynamicListView;
import com.fortysevendeg.swipelistview.SwipeListView;
import com.swipesapp.android.R;
import com.swipesapp.android.analytics.handler.Analytics;
import com.swipesapp.android.analytics.values.Actions;
import com.swipesapp.android.analytics.values.Categories;
import com.swipesapp.android.analytics.values.Labels;
import com.swipesapp.android.handler.RepeatHandler;
import com.swipesapp.android.handler.SoundHandler;
import com.swipesapp.android.sync.gson.GsonTag;
import com.swipesapp.android.sync.gson.GsonTask;
import com.swipesapp.android.sync.service.SyncService;
import com.swipesapp.android.sync.service.TasksService;
import com.swipesapp.android.ui.activity.EditTaskActivity;
import com.swipesapp.android.ui.activity.SnoozeActivity;
import com.swipesapp.android.ui.activity.TasksActivity;
import com.swipesapp.android.ui.adapter.TasksListAdapter;
import com.swipesapp.android.ui.listener.KeyboardBackListener;
import com.swipesapp.android.ui.listener.ListContentsListener;
import com.swipesapp.android.ui.view.ActionEditText;
import com.swipesapp.android.ui.view.FlatButton;
import com.swipesapp.android.ui.view.FlowLayout;
import com.swipesapp.android.ui.view.SwipesDialog;
import com.swipesapp.android.ui.view.SwipesTextView;
import com.swipesapp.android.util.DateUtils;
import com.swipesapp.android.util.DeviceUtils;
import com.swipesapp.android.util.PreferenceUtils;
import com.swipesapp.android.util.ThemeUtils;
import com.swipesapp.android.values.Constants;
import com.swipesapp.android.values.Intents;
import com.swipesapp.android.values.Sections;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Fragment for the list of tasks.
 */
public class TasksListFragment extends ListFragment implements DynamicListView.ListOrderListener, ListContentsListener {

    // The fragment argument representing the section number for this fragment.
    private static final String ARG_SECTION_NUMBER = "section_number";

    // Activity reference.
    private TasksActivity mActivity;

    // Section the fragment belongs to.
    private Sections mSection;

    // List of tasks.
    private List<GsonTask> mTasks;

    // Customized list view to display tasks.
    private DynamicListView mListView;

    // List view height, used for UI calculations.
    private int mListViewHeight;

    // Adapter for tasks.
    private TasksListAdapter mAdapter;

    // Service to perform tasks operations.
    private TasksService mTasksService;

    // Service to perform sync operations.
    private SyncService mSyncService;

    // Handler for repeated tasks.
    private RepeatHandler mRepeatHandler;

    // Selected tasks, tags and filters.
    private static List<GsonTask> sSelectedTasks;
    private static GsonTask sNextTask;
    private List<GsonTag> mAssignedTags;
    private int mAssignedTagsCount;
    private int mUnassignedTagsCount;

    // Empty view.
    private View mEmptyView;
    private boolean mDoneForToday;
    private Boolean mIsShowingEmptyView;

    private TextView mEmptyResultsText;
    private FlatButton mEmptyClearWorkspaceButton;

    // Controls the display of old tasks.
    private static boolean sIsShowingOld;

    // Footer views.
    private TextView mResultsText;
    private FlatButton mClearWorkspaceButton;

    // List position of last added task.
    private Integer mAddedTaskPosition;

    @BindView(android.R.id.empty)
    ViewStub mViewStub;

    @BindView(R.id.header_view)
    LinearLayout mHeaderView;

    @BindView(R.id.list_area)
    LinearLayout mListArea;

    @BindView(R.id.assign_tags_area)
    LinearLayout mTagsArea;

    @BindView(R.id.assign_tags_container)
    FlowLayout mTaskTagsContainer;

    @BindView(R.id.landscape_header)
    RelativeLayout mLandscapeHeader;

    @BindView(R.id.landscape_header_area)
    LinearLayout mLandscapeHeaderArea;

    @BindView(R.id.action_bar_title)
    TextView mLandscapeHeaderTitle;

    @BindView(R.id.action_bar_icon)
    SwipesTextView mLandscapeHeaderIcon;

    public static TasksListFragment newInstance(int sectionNumber) {
        TasksListFragment fragment = new TasksListFragment();

        Bundle args = new Bundle();
        args.putInt(ARG_SECTION_NUMBER, sectionNumber);

        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Bundle args = getArguments();

        mActivity = (TasksActivity) getActivity();

        mTasksService = TasksService.getInstance();
        mSyncService = SyncService.getInstance();

        mRepeatHandler = new RepeatHandler();

        mTasks = new ArrayList<GsonTask>();
        sSelectedTasks = new ArrayList<GsonTask>();

        int sectionNumber = args.getInt(ARG_SECTION_NUMBER, Sections.FOCUS.getSectionNumber());
        mSection = Sections.getSectionByNumber(sectionNumber);

        View rootView = inflater.inflate(R.layout.fragment_tasks_list, container, false);
        ButterKnife.bind(this, rootView);

        // Setup view for current section.
        switch (mSection) {
            case LATER:
                setupView(rootView, R.layout.tasks_later_empty_view);
                configureLaterView(mAdapter);
                configureEmptyView();
                break;
            case FOCUS:
                setupView(rootView, R.layout.tasks_focus_empty_view);
                configureFocusView(mAdapter);
                configureEmptyView();
                updateEmptyView();
                break;
            case DONE:
                setupView(rootView, R.layout.tasks_done_empty_view);
                configureDoneView(mAdapter);
                configureEmptyView();
                break;
        }

        measureListView();

        refreshTaskList(false);

        return rootView;
    }

    @Override
    public void onResume() {
        mTasksService = TasksService.getInstance();

        IntentFilter filter = new IntentFilter();
        filter.addAction(Intents.TAB_CHANGED);
        filter.addAction(Intents.ASSIGN_TAGS);
        filter.addAction(Intents.DELETE_TASKS);
        filter.addAction(Intents.SHARE_TASKS);
        filter.addAction(Intents.BACK_PRESSED);
        filter.addAction(Intents.SELECTION_STARTED);
        filter.addAction(Intents.SELECTION_CLEARED);
        filter.addAction(Intents.FILTER_BY_TAGS);
        filter.addAction(Intents.PERFORM_SEARCH);

        getActivity().registerReceiver(mTasksReceiver, filter);

        handleWelcomeDialog();

        super.onResume();
    }

    @Override
    public void onPause() {
        getActivity().unregisterReceiver(mTasksReceiver);

        super.onPause();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (isCurrentSection()) {
            // Check if request code is the one from snooze task.
            if (requestCode == Constants.SNOOZE_REQUEST_CODE) {
                switch (resultCode) {
                    case Activity.RESULT_OK:
                        // Task has been snoozed. Refresh all task lists.
                        mActivity.refreshSections(true);
                        break;
                    case Activity.RESULT_CANCELED:
                        // Snooze has been canceled. Refresh tasks with animation.
                        refreshTaskList(true);
                        break;
                }
            }
        }
    }

    private boolean isCurrentSection() {
        // Retrieve current section being displayed and compare with this fragment's section.
        return mSection == mActivity.getCurrentSection();
    }

    private void setupView(View rootView, int emptyView) {
        // Initialize adapter.
        mAdapter = new TasksListAdapter(getActivity(), R.layout.swipeable_cell, mSection);
        mAdapter.setListContentsListener(this);

        // Initialize list view.
        mListView = (DynamicListView) rootView.findViewById(android.R.id.list);

        // Load ripple background for theme.
        int background = ThemeUtils.isLightTheme(getActivity()) ?
                R.drawable.list_item_selector_light : R.drawable.list_item_selector_dark;

        // Set colors and components.
        mListView.setContainerBackground(background);
        mListView.setContainerColor(ThemeUtils.getBackgroundColor(getActivity()));
        mListView.setAccentColor(ThemeUtils.getSectionColor(mSection, getActivity()));
        mListView.setFrontDetailText(R.id.task_time);
        mListView.setFrontCounter(R.id.task_subtask_count);

        // Setup filters.
        setupResultsFooter();

        // Setup empty view.
        mViewStub.setLayoutResource(emptyView);
        mEmptyView = mViewStub.inflate();
        mListView.setEmptyView(mEmptyView);

        // Setup tablet mode.
        setupViewForTablets();
    }

    private void setupViewForTablets() {
        // Setup landscape header.
        mLandscapeHeader.setVisibility(DeviceUtils.isLandscape(getActivity()) ? View.VISIBLE : View.GONE);
        mLandscapeHeaderArea.setBackgroundColor(ThemeUtils.getSectionColor(mSection, getActivity()));

        // Setup title and icon.
        mLandscapeHeaderTitle.setText(mSection.getSectionTitle(getActivity()));
        mLandscapeHeaderIcon.setText(mSection.getSectionIcon(getActivity()));
        mLandscapeHeaderIcon.setTextColor(Color.WHITE);

        // Disable edge effect.
        if (DeviceUtils.isTablet(getActivity())) {
            mListView.setOverScrollMode(View.OVER_SCROLL_NEVER);
        }
    }

    private void setupResultsFooter() {
        // Add filter views.
        View resultsView = getActivity().getLayoutInflater().inflate(R.layout.results_footer, null);
        mResultsText = (TextView) resultsView.findViewById(R.id.workspace_results);
        mListView.addFooterView(resultsView);

        // Add clear listener.
        mClearWorkspaceButton = (FlatButton) resultsView.findViewById(R.id.clear_workspace_button);
        mClearWorkspaceButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Clear workspace.
                mActivity.closeWorkspaces();
            }
        });

        // Hide footer initially.
        hideWorkspaceResults();
    }

    private void configureLaterView(TasksListAdapter adapter) {
        // Setup content.
        mListView.setAdapter(adapter);
        mListView.setSwipeListViewListener(mSwipeListener);
        mListView.setViewPager(mActivity.getViewPager());

        // Setup back view.
        mListView.setLongSwipeEnabled(true);
        mListView.setSwipeBackgroundColors(ThemeUtils.getSectionColor(Sections.FOCUS, getActivity()), ThemeUtils.getSectionColor(Sections.LATER, getActivity()), Color.TRANSPARENT);
        mListView.setLongSwipeBackgroundColors(ThemeUtils.getSectionColor(Sections.DONE, getActivity()), ThemeUtils.getSectionColor(Sections.LATER, getActivity()));
        mListView.setBackIconText(R.string.focus_full, R.string.later_full);
        mListView.setLongSwipeBackIconText(R.string.done_full, R.string.later_full);

        // Setup priority button.
        mListView.setFrontIcon(R.id.button_task_priority);
        mListView.setFrontIconBackgrounds(R.drawable.focus_circle_selector, R.drawable.later_circle_selector, R.drawable.later_circle_selector);
        mListView.setFrontIconLongSwipeBackgrounds(R.drawable.done_circle_selector, R.drawable.later_circle_selector);

        // Setup label.
        mListView.setFrontLabel(R.id.task_label);
        mListView.setFrontLabelBackgrounds(R.drawable.cell_label_focus, R.drawable.cell_label_later, R.drawable.cell_label_later);
        mListView.setFrontLabelLongSwipeBackgrounds(R.drawable.cell_label_done, R.drawable.cell_label_later);

        // Setup actions.
        mListView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        mListView.setSwipeMode(SwipeListView.SWIPE_MODE_BOTH);
        mListView.setLongSwipeMode(SwipeListView.LONG_SWIPE_MODE_BOTH);
        mListView.setSwipeActionRight(SwipeListView.SWIPE_ACTION_DISMISS);
        mListView.setSwipeActionLeft(SwipeListView.SWIPE_ACTION_REVEAL);
        mListView.setLongSwipeActionRight(SwipeListView.LONG_SWIPE_ACTION_DISMISS);
        mListView.setLongSwipeActionLeft(SwipeListView.LONG_SWIPE_ACTION_REVEAL);

        // Setup landscape padding.
        if (DeviceUtils.isLandscape(getActivity())) {
            int bottomPadding = mListView.getPaddingBottom();
            int topPadding = mListView.getPaddingTop();
            int sidePadding = mListView.getPaddingLeft();

            mListView.setPadding(sidePadding, topPadding, Math.round(sidePadding / 2), bottomPadding);

            // Setup landscape margin.
            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) mLandscapeHeader.getLayoutParams();
            params.setMargins(sidePadding, params.topMargin, Math.round(sidePadding / 2), 0);
            mLandscapeHeader.setLayoutParams(params);
        }
    }

    private void configureFocusView(TasksListAdapter adapter) {
        // Setup content.
        mListView.setContentList(adapter.getData());
        mListView.setAdapter(adapter);
        mListView.setSwipeListViewListener(mSwipeListener);
        mListView.setListOrderListener(this);
        mListView.setViewPager(mActivity.getViewPager());

        // Setup back view.
        mListView.setSwipeBackgroundColors(ThemeUtils.getSectionColor(Sections.DONE, getActivity()), ThemeUtils.getSectionColor(Sections.LATER, getActivity()), Color.TRANSPARENT);
        mListView.setBackIconText(R.string.done_full, R.string.later_full);

        // Setup priority button.
        mListView.setFrontIcon(R.id.button_task_priority);
        mListView.setFrontIconBackgrounds(R.drawable.done_circle_selector, R.drawable.later_circle_selector, R.drawable.focus_circle_selector);

        // Setup label.
        mListView.setFrontLabel(R.id.task_label);
        mListView.setFrontLabelBackgrounds(R.drawable.cell_label_done, R.drawable.cell_label_later, R.drawable.cell_label_focus);

        // Setup actions.
        mListView.setDragAndDropEnabled(true);
        mListView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        mListView.setSwipeMode(SwipeListView.SWIPE_MODE_BOTH);
        mListView.setSwipeActionRight(SwipeListView.SWIPE_ACTION_DISMISS);
        mListView.setSwipeActionLeft(SwipeListView.SWIPE_ACTION_REVEAL);

        // Setup landscape padding.
        if (DeviceUtils.isLandscape(getActivity())) {
            int bottomPadding = mListView.getPaddingBottom();
            int topPadding = mListView.getPaddingTop();
            int sidePadding = Math.round(mListView.getPaddingLeft() / 2);

            mListView.setPadding(sidePadding, topPadding, sidePadding, bottomPadding);

            // Setup landscape margin.
            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) mLandscapeHeader.getLayoutParams();
            params.setMargins(sidePadding, params.topMargin, sidePadding, 0);
            mLandscapeHeader.setLayoutParams(params);
        }
    }

    private void configureDoneView(TasksListAdapter adapter) {
        // Setup content.
        mListView.setAdapter(adapter);
        mListView.setSwipeListViewListener(mSwipeListener);
        mListView.setViewPager(mActivity.getViewPager());

        // Setup back view.
        mListView.setLongSwipeEnabled(true);
        mListView.setSwipeBackgroundColors(0, ThemeUtils.getSectionColor(Sections.FOCUS, getActivity()), Color.TRANSPARENT);
        mListView.setLongSwipeBackgroundColors(0, ThemeUtils.getSectionColor(Sections.LATER, getActivity()));
        mListView.setBackIconText(0, R.string.focus_full);
        mListView.setLongSwipeBackIconText(0, R.string.later_full);

        // Setup priority button.
        mListView.setFrontIcon(R.id.button_task_priority);
        mListView.setFrontIconBackgrounds(0, R.drawable.focus_circle_selector, R.drawable.done_circle_selector);
        mListView.setFrontIconLongSwipeBackgrounds(0, R.drawable.later_circle_selector);

        // Setup label.
        mListView.setFrontLabel(R.id.task_label);
        mListView.setFrontLabelBackgrounds(0, R.drawable.cell_label_focus, R.drawable.cell_label_done);
        mListView.setFrontLabelLongSwipeBackgrounds(0, R.drawable.cell_label_later);

        // Setup actions.
        mListView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        mListView.setSwipeMode(SwipeListView.SWIPE_MODE_LEFT);
        mListView.setLongSwipeMode(SwipeListView.LONG_SWIPE_MODE_LEFT);
        mListView.setSwipeActionRight(SwipeListView.SWIPE_ACTION_NONE);
        mListView.setSwipeActionLeft(SwipeListView.SWIPE_ACTION_DISMISS);
        mListView.setLongSwipeActionLeft(SwipeListView.LONG_SWIPE_ACTION_REVEAL);

        // Setup landscape padding.
        if (DeviceUtils.isLandscape(getActivity())) {
            int bottomPadding = mListView.getPaddingBottom();
            int topPadding = mListView.getPaddingTop();
            int sidePadding = mListView.getPaddingLeft();

            mListView.setPadding(Math.round(sidePadding / 2), topPadding, sidePadding, bottomPadding);

            // Setup landscape margin.
            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) mLandscapeHeader.getLayoutParams();
            params.setMargins(Math.round(sidePadding / 2), params.topMargin, sidePadding, 0);
            mLandscapeHeader.setLayoutParams(params);
        }
    }

    private void configureEmptyView() {
        // Customize Focus empty view.
        if (mSection == Sections.FOCUS) {
            TextView allDoneText = (TextView) mEmptyView.findViewById(R.id.text_all_done);
            allDoneText.setTextColor(ThemeUtils.getSecondaryTextColor(getActivity()));

            TextView nextTaskText = (TextView) mEmptyView.findViewById(R.id.text_next_task);
            nextTaskText.setTextColor(ThemeUtils.getSecondaryTextColor(getActivity()));

            TextView allDoneMessage = (TextView) mEmptyView.findViewById(R.id.text_all_done_message);
            allDoneMessage.setTextColor(ThemeUtils.getSecondaryTextColor(getActivity()));

            Button share = (Button) mEmptyView.findViewById(R.id.button_share);
            share.setBackgroundResource(ThemeUtils.isLightTheme(getActivity()) ?
                    R.drawable.ic_share_light : R.drawable.ic_share_dark);
            setButtonSelector(share);
        }

        // Setup results footer.
        View emptyResultsView = mEmptyView.findViewById(R.id.results_footer);
        mEmptyResultsText = (TextView) emptyResultsView.findViewById(R.id.workspace_results);

        // Add clear listener.
        mEmptyClearWorkspaceButton = (FlatButton) emptyResultsView.findViewById(R.id.clear_workspace_button);
        mEmptyClearWorkspaceButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Clear workspace.
                mActivity.closeWorkspaces();
            }
        });

        // Hide results initially.
        hideEmptyResults();
    }

    private void setButtonSelector(final View button) {
        // Create selector based on touch state.
        button.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                switch (motionEvent.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        // Change alpha to pressed state.
                        button.animate().alpha(Constants.PRESSED_BUTTON_ALPHA);
                        break;
                    case MotionEvent.ACTION_UP:
                        // Change alpha to default state.
                        button.animate().alpha(1.0f);
                        break;
                }
                return false;
            }
        });
    }

    private void measureListView() {
        if (mListArea != null) {
            mListArea.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                public void onGlobalLayout() {
                    // Save list view height for later calculations.
                    mListViewHeight = mListArea.getHeight();

                    // Consider header height in the done section.
                    if (mSection == Sections.DONE) {
                        mListViewHeight = mListViewHeight - mHeaderView.getHeight();
                    }
                }
            });
        }
    }

    public void refreshTaskList(boolean animateRefresh) {
        // Block refresh while swiping.
        if (!mListView.isSwiping()) {

            // Find out current refresh type.
            boolean isFilter = !mActivity.getSelectedFilterTags().isEmpty();
            boolean isSearch = !mActivity.getSearchQuery().isEmpty();

            // Only refresh as usual when workspace is inactive.
            if (!isFilter && !isSearch) {
                // Refresh asynchronously.
                new RefreshTask().execute(animateRefresh);
            } else if (isFilter) {
                // Workspace is active. Reload filter.
                new FilterTask().execute();
            } else {
                // Search is active. Reload results.
                new SearchTask().execute();
            }
        }
    }

    private class RefreshTask extends AsyncTask<Boolean, Void, Boolean> {
        @Override
        protected Boolean doInBackground(Boolean... params) {
            // Load new data.
            switch (mSection) {
                case LATER:
                    mTasks = mTasksService.loadScheduledTasks();
                    break;
                case FOCUS:
                    mTasks = mTasksService.loadFocusedTasks();
                    break;
                case DONE:
                    mTasks = mTasksService.loadCompletedTasks();
                    break;
            }

            // Keep tasks selected after refresh.
            keepSelection();

            // Find last added task for scrolling.
            if ((mSection == Sections.LATER || mSection == Sections.FOCUS) && mActivity.hasAddedTask()) {
                mAddedTaskPosition = findLastAddedTask();
            }

            return params[0];
        }

        @Override
        protected void onPostExecute(Boolean animateRefresh) {
            // Avoid updating while swiping screens or when a refresh is pending.
            if (!mActivity.isSwipingScreens() && !TasksActivity.hasPendingRefresh()) {
                // Update adapter with new data.
                updateAdapter(animateRefresh);
            } else {
                // Mark update as pending.
                TasksActivity.setPendingRefresh();
            }
        }
    }

    public void updateAdapter(boolean animateRefresh) {
        switch (mSection) {
            case LATER:
                mAdapter.update(mTasks, animateRefresh);

                // Refresh empty view.
                sNextTask = !mTasks.isEmpty() ? mTasks.get(0) : null;
                mActivity.updateEmptyView();

                // Scroll to last added task.
                scrollToLastAdded();
                break;
            case FOCUS:
                mListView.setContentList(mTasks);
                mAdapter.update(mTasks, animateRefresh);

                // Scroll to last added task.
                scrollToLastAdded();
                break;
            case DONE:
                handleDoneButtons(mTasks);
                mAdapter.setShowingOld(sIsShowingOld);
                mAdapter.update(mTasks, animateRefresh);
                break;
        }
    }

    private void keepSelection() {
        for (GsonTask selected : sSelectedTasks) {
            for (GsonTask task : mTasks) {
                if (selected.getTempId().equals(task.getTempId())) {
                    task.setSelected(true);
                    break;
                }
            }
        }
    }

    private Integer findLastAddedTask() {
        Integer position = null;
        for (int x = 0; x < mTasks.size(); x++) {
            GsonTask task = mTasks.get(x);
            if (task.getTempId().equals(mActivity.getAddedTaskId())) {
                // Return list position of last added task.
                position = x;
                break;
            }
        }
        return position;
    }

    private void scrollToLastAdded() {
        if (mActivity.hasAddedTask() && mAddedTaskPosition != null) {
            if (PreferenceUtils.isAutoScrollEnabled(getActivity())) {
                // Scroll to position.
                if (mSection == Sections.FOCUS) {
                    final int position = mAddedTaskPosition;
                    // Wait for fade animation to complete.
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            mListView.smoothScrollToPosition(position);
                        }
                    }, Constants.ANIMATION_DURATION_MEDIUM);
                } else if (mSection == Sections.LATER) {
                    mListView.setSelection(mAddedTaskPosition);
                }
            }

            // Clear scrolling flags.
            mActivity.clearAddedTask();
            mAddedTaskPosition = null;
        }
    }

    private GsonTask getTask(int position) {
        return (GsonTask) mAdapter.getItem(position);
    }

    private BroadcastReceiver mTasksReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            // Filter intent actions.
            if (action.equals(Intents.TAB_CHANGED)) {
                // Enable or disable swiping.
                boolean enabled = isCurrentSection() || DeviceUtils.isLandscape(getActivity());
                mListView.setSwipeEnabled(enabled);
            } else if (action.equals(Intents.SELECTION_STARTED)) {
                // TODO: Enable multi-swiping (after it's implemented).
            } else if (action.equals(Intents.SELECTION_CLEARED)) {
                // Play sound.
                if (!sSelectedTasks.isEmpty()) {
                    SoundHandler.playSound(getActivity(), R.raw.action_negative);
                }

                // Clear selected tasks and stop selection mode.
                sSelectedTasks.clear();
                mActivity.cancelSelection();

                // TODO: Disable multi-swiping.

                // Refresh all sections.
                refreshTaskList(false);
            } else if (action.equals(Intents.FILTER_BY_TAGS)) {
                // Filter by tags or clear results.
                if (!mActivity.getSelectedFilterTags().isEmpty()) {
                    new FilterTask().execute();

                    // Send analytics event.
                    long value = (long) mActivity.getSelectedFilterTags().size();
                    Analytics.sendEvent(Categories.WORKSPACES, Actions.FILTER_TAGS, null, value);
                } else {
                    // Hide old tasks before refreshing.
                    if (mSection == Sections.DONE) {
                        mHeaderView.setVisibility(View.VISIBLE);
                        sIsShowingOld = false;
                    }

                    // Hide results footers.
                    hideWorkspaceResults();
                    hideEmptyResults();

                    refreshTaskList(false);
                }
            } else if (action.equals(Intents.PERFORM_SEARCH)) {
                // Update search results.
                new SearchTask().execute();
            }

            // Filter actions intended only for this section.
            if (isCurrentSection()) {
                if (action.equals(Intents.ASSIGN_TAGS)) {
                    if (!sSelectedTasks.isEmpty()) {
                        // Hide buttons and show tags view.
                        showTags();
                    }
                } else if (action.equals(Intents.DELETE_TASKS)) {
                    if (!sSelectedTasks.isEmpty()) {
                        // Delete tasks.
                        deleteSelectedTasks();
                    }
                } else if (action.equals(Intents.SHARE_TASKS)) {
                    // Send intent to share selected tasks by email.
                    if (!sSelectedTasks.isEmpty()) {
                        shareTasks();
                    }
                } else if (action.equals(Intents.BACK_PRESSED)) {
                    // Don't close the app when assigning tags.
                    if (mTagsArea.getVisibility() == View.VISIBLE) {
                        closeTags();
                    } else if (mActivity.isSelectionMode()) {
                        // Send broadcast to update selection UI.
                        mTasksService.sendBroadcast(Intents.SELECTION_CLEARED);
                    } else if (mActivity.isSearchActive()) {
                        // Close search and refresh list.
                        mActivity.hideSearch();
                        refreshTaskList(false);
                    } else {
                        sIsShowingOld = false;
                        getActivity().finish();
                    }
                }
            }
        }
    };

    private BaseSwipeListViewListener mSwipeListener = new BaseSwipeListViewListener() {
        @Override
        public void onFinishedSwipeRight(int position) {
            GsonTask task = getTask(position);
            if (task != null) {
                switch (mSection) {
                    case LATER:
                        List<GsonTask> tasksToSave = new ArrayList<>();
                        // Move task from Later to Focus.
                        task.setLocalSchedule(new Date());
                        // Reorder tasks and save.
                        handleOrder(task, tasksToSave);
                        mTasksService.saveTasks(tasksToSave, true);
                        // Refresh all lists.
                        mActivity.refreshSections(true);
                        // Play sound.
                        SoundHandler.playSound(getActivity(), R.raw.focus_task);
                        break;
                    case FOCUS:
                        // Move task from Focus to Done.
                        task.setLocalCompletionDate(new Date());
                        mTasksService.saveTask(task, true);
                        // Handle repeat.
                        mRepeatHandler.handleRepeatedTask(task);
                        // Refresh all lists.
                        mActivity.refreshSections(true);
                        // Send analytics event.
                        Analytics.sendEvent(Categories.TASKS, Actions.COMPLETED_TASKS, null, 1l);
                        // Play sound.
                        if (mTasksService.countTasksForNow() > 0) {
                            SoundHandler.playSound(getActivity(), R.raw.complete_task_1);
                        }
                        break;
                }
            }
        }

        @Override
        public void onFinishedSwipeLeft(int position) {
            GsonTask task = getTask(position);
            if (task != null) {
                switch (mSection) {
                    case LATER:
                        // Reschedule task.
                        openSnoozeSelector(task);
                        break;
                    case FOCUS:
                        // Move task from Focus to Later.
                        openSnoozeSelector(task);
                        break;
                    case DONE:
                        // Move task from Done to Focus.
                        task.setLocalCompletionDate(null);
                        mTasksService.saveTask(task, true);
                        // Refresh all lists.
                        mActivity.refreshSections(true);
                        // Play sound.
                        SoundHandler.playSound(getActivity(), R.raw.focus_task);
                        break;
                }
            }
        }

        @Override
        public void onFinishedLongSwipeRight(int position) {
            GsonTask task = getTask(position);
            if (task != null) {
                switch (mSection) {
                    case LATER:
                        // Move task from Later to Done.
                        Date currentDate = new Date();
                        task.setLocalCompletionDate(currentDate);
                        task.setLocalSchedule(currentDate);
                        mTasksService.saveTask(task, true);
                        // Handle repeat.
                        mRepeatHandler.handleRepeatedTask(task);
                        // Refresh all lists.
                        mActivity.refreshSections(true);
                        // Send analytics event.
                        Analytics.sendEvent(Categories.TASKS, Actions.COMPLETED_TASKS, null, 1l);
                        // Play sound.
                        SoundHandler.playSound(getActivity(), R.raw.complete_task_1);
                        break;
                }
            }
        }

        @Override
        public void onFinishedLongSwipeLeft(int position) {
            GsonTask task = getTask(position);
            if (task != null) {
                switch (mSection) {
                    case LATER:
                        // Reschedule task.
                        openSnoozeSelector(task);
                        break;
                    case DONE:
                        // Move task from Done to Later.
                        openSnoozeSelector(task);
                        break;
                }
            }
        }

        @Override
        public void onClickFrontView(View view, int position) {
            GsonTask task = getTask(position);
            if (task != null) {
                // Start selection or edit task.
                if (mActivity.isSelectionMode()) {
                    selectTask(view, task);
                } else {
                    startEditTask(task.getId(), false);
                }
            }
        }

        @Override
        public void onClickCheckbox(View view, int position) {
            CheckBox priorityButton = (CheckBox) view;
            boolean checked = !priorityButton.isChecked();
            priorityButton.setChecked(checked);
            Integer priority = checked ? 1 : 0;

            GsonTask task = getTask(position);
            if (task != null) {
                task.setPriority(priority);
                mTasksService.saveTask(task, true);

                // Send analytics event.
                String label = checked ? Labels.PRIORITY_ON : Labels.PRIORITY_OFF;
                Analytics.sendEvent(Categories.TASKS, Actions.PRIORITY, label, null);

                // Refresh widget.
                TasksActivity.refreshWidgets(getActivity());
            }

            // Play sound.
            int sound = checked ? R.raw.action_positive : R.raw.action_negative;
            SoundHandler.playSound(getActivity(), sound);
        }

        @Override
        public void onClickNumber(View view, int position) {
            GsonTask task = getTask(position);
            if (task != null) {
                // Start selection or show action steps.
                if (mActivity.isSelectionMode()) {
                    selectTask(view, task);
                } else {
                    startEditTask(task.getId(), true);
                }
            }
        }
    };

    private void selectTask(View view, GsonTask task) {
        View selectedIndicator = view.findViewById(R.id.selected_indicator);

        if (task.isSelected()) {
            // Deselect task.
            task.setSelected(false);
            selectedIndicator.setBackgroundColor(0);
            sSelectedTasks.remove(task);
        } else {
            // Select task.
            task.setSelected(true);
            selectedIndicator.setBackgroundColor(ThemeUtils.getSectionColor(mSection, getActivity()));
            sSelectedTasks.add(task);
        }

        mActivity.updateSelectionCount(sSelectedTasks.size());

        // Play sound.
        int sound = task.isSelected() ? R.raw.action_positive : R.raw.action_negative;
        SoundHandler.playSound(getActivity(), sound);
    }

    @Override
    public void listReordered(List list) {
        if (mSection == Sections.FOCUS) {
            reorderTasks((List<GsonTask>) list);

            // Clear selected tasks and hide edit bar.
            sSelectedTasks.clear();
            mActivity.cancelSelection();

            refreshTaskList(false);

            // Refresh widget.
            TasksActivity.refreshWidgets(getActivity());

            // Play sound.
            SoundHandler.playSound(getActivity(), R.raw.action_positive);
        }
    }

    private void reorderTasks(List<GsonTask> tasks) {
        // Save task order as its position on the list.
        for (int i = 0; i < tasks.size(); i++) {
            GsonTask task = tasks.get(i);
            task.setOrder(i);
        }

        mTasksService.saveTasks(tasks, true);
    }

    private void handleOrder(GsonTask newTask, List<GsonTask> tasksToSave) {
        boolean addToBottom = PreferenceUtils.readBoolean(PreferenceUtils.ADD_TO_BOTTOM_KEY, getActivity());
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

    @Override
    public void onEmpty() {
        showEmptyView();
    }

    @Override
    public void onNotEmpty() {
        hideEmptyView();
    }

    private void showEmptyView() {
        if (mIsShowingEmptyView == null || !mIsShowingEmptyView) {
            boolean isFilter = !mActivity.getSelectedFilterTags().isEmpty();
            boolean isSearch = !mActivity.getSearchQuery().isEmpty();

            View emptyViewContainer = mEmptyView.findViewById(R.id.empty_view_container);

            if (mSection == Sections.FOCUS) {
                RelativeLayout emptyMainArea = (RelativeLayout) mEmptyView.findViewById(R.id.all_done_main_area);
                RelativeLayout emptySocialArea = (RelativeLayout) mEmptyView.findViewById(R.id.all_done_social_area);
                SwipesTextView emptyViewIcon = (SwipesTextView) mEmptyView.findViewById(R.id.tasks_empty_view_icon);

                // Set visibility of sharing and icon.
                emptyMainArea.setVisibility(isSearch || isFilter ? View.GONE : View.VISIBLE);
                emptySocialArea.setVisibility(isSearch || isFilter ? View.GONE : View.VISIBLE);
                emptyViewIcon.setVisibility(isSearch || isFilter ? View.VISIBLE : View.GONE);

                updateEmptyView();

                if (!isFilter && !isSearch && mIsShowingEmptyView != null) {
                    // Send cleared tasks event.
                    sendClearedTasksEvent();

                    // Play sound.
                    SoundHandler.playSound(getActivity(), R.raw.all_done_today);
                }
            }

            // Animate empty view.
            if (emptyViewContainer.getAlpha() == 0f) {
                emptyViewContainer.animate().alpha(1f).setDuration(Constants.ANIMATION_DURATION_LONG).start();

                fadeOutTasksList();
            }

            if (DeviceUtils.isLandscape(getActivity())) {
                // Hide landscape header.
                mLandscapeHeader.setVisibility(View.GONE);
            }

            if (isFilter) {
                // Show workspace result.
                mEmptyResultsText.setVisibility(View.VISIBLE);
                mEmptyClearWorkspaceButton.setVisibility(View.VISIBLE);
            } else if (isSearch) {
                // Show search result.
                updateResultsDescription(0);
                mEmptyResultsText.setVisibility(View.VISIBLE);
                mEmptyClearWorkspaceButton.setVisibility(View.GONE);
            } else {
                // Hide results.
                hideEmptyResults();
            }
        }

        mIsShowingEmptyView = true;
    }

    private void hideEmptyView() {
        if (mIsShowingEmptyView == null || mIsShowingEmptyView) {
            View emptyViewContainer = mEmptyView.findViewById(R.id.empty_view_container);

            // Animate empty view.
            if (emptyViewContainer.getAlpha() == 1f) {
                emptyViewContainer.animate().alpha(0f).setDuration(Constants.ANIMATION_DURATION_LONG).start();

                fadeInTasksList();
            }

            if (DeviceUtils.isLandscape(getActivity())) {
                // Show landscape header.
                mLandscapeHeader.setVisibility(View.VISIBLE);
            }

            if (mSection == Sections.FOCUS) {
                mDoneForToday = false;
            }
        }

        mIsShowingEmptyView = false;
    }

    public void hideEmptyResults() {
        mEmptyResultsText.setVisibility(View.GONE);
        mEmptyClearWorkspaceButton.setVisibility(View.GONE);
    }

    public void updateEmptyView() {
        if (mSection == Sections.FOCUS) {
            TextView allDoneText = (TextView) mEmptyView.findViewById(R.id.text_all_done);
            TextView nextTaskText = (TextView) mEmptyView.findViewById(R.id.text_next_task);
            TextView allDoneMessage = (TextView) mEmptyView.findViewById(R.id.text_all_done_message);

            if (sNextTask != null && sNextTask.getLocalSchedule() != null) {
                Date nextSchedule = sNextTask.getLocalSchedule();

                // Set text according to the next scheduled task.
                if (DateUtils.isToday(nextSchedule)) {
                    allDoneText.setText(getString(R.string.all_done_now));
                    String nextDate = DateUtils.getTimeAsString(getActivity(), nextSchedule);
                    nextTaskText.setText(getString(R.string.all_done_now_next, nextDate));
                    allDoneMessage.setText(getString(R.string.all_done_now_message));
                } else {
                    allDoneText.setText(getString(R.string.all_done_today));
                    String nextDate = DateUtils.formatToRecent(nextSchedule, getActivity(), false);
                    nextTaskText.setText(getString(R.string.all_done_today_next, nextDate));
                    allDoneMessage.setText(getString(R.string.all_done_today_message));
                    mDoneForToday = true;
                }
            } else {
                // Show default message.
                allDoneText.setText(getString(R.string.all_done_today));
                nextTaskText.setText(getString(R.string.all_done_next_empty));
                allDoneMessage.setText(getString(R.string.all_done_today_message));
                mDoneForToday = true;
            }

            // Refresh sharing message.
            mActivity.setShareMessage(allDoneMessage.getText().toString());
        }
    }

    private void handleDoneButtons(List<GsonTask> completedTasks) {
        // Load date of the oldest completed task.
        GsonTask oldestTask = completedTasks != null && !completedTasks.isEmpty() ?
                completedTasks.get(completedTasks.size() - 1) : null;
        Date completionDate = oldestTask != null ? oldestTask.getLocalCompletionDate() : null;

        // Only display buttons in the done section and when the oldest completed task is older than today.
        if (!sIsShowingOld && DateUtils.isOlderThanToday(completionDate)) {
            if (mHeaderView != null) {
                mHeaderView.setVisibility(View.VISIBLE);
                mHeaderView.setAlpha(1f);
            }
        }
    }

    private void deleteSelectedTasks() {
        // Display confirmation dialog.
        SwipesDialog.show(new SwipesDialog.Builder(getActivity())
                .actionsColor(ThemeUtils.getSectionColor(mSection, getActivity()))
                .title(getResources().getQuantityString(R.plurals.delete_task_dialog_title, sSelectedTasks.size(), sSelectedTasks.size()))
                .content(R.string.delete_task_dialog_text)
                .positiveText(R.string.delete_task_dialog_yes)
                .negativeText(R.string.delete_task_dialog_no)
                .callback(new MaterialDialog.ButtonCallback() {
                    @Override
                    public void onPositive(MaterialDialog dialog) {
                        // Proceed with delete.
                        mTasksService.deleteTasks(sSelectedTasks);

                        // Send analytics event.
                        Analytics.sendEvent(Categories.TASKS, Actions.DELETED_TASKS, null, (long) sSelectedTasks.size());

                        // Clear selection.
                        sSelectedTasks.clear();
                        mActivity.updateSelectionCount(sSelectedTasks.size());

                        // Refresh all task lists.
                        mActivity.refreshSections(true);

                        // Play sound.
                        SoundHandler.playSound(getActivity(), R.raw.action_negative);
                    }
                }));
    }

    @OnClick(R.id.button_show_old)
    protected void showOldTasks() {
        // Animate footer view.
        mHeaderView.animate().alpha(0f).setDuration(Constants.ANIMATION_DURATION_SHORT).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                // Hide buttons.
                mHeaderView.setVisibility(View.GONE);

                // Animate list view to new position.
                float fromY = mListView.getTranslationY() + mHeaderView.getHeight();
                float toY = mListView.getTranslationY();

                ObjectAnimator animator = ObjectAnimator.ofFloat(mListView, "translationY", fromY, toY);
                animator.setDuration(Constants.ANIMATION_DURATION_MEDIUM).start();

                // Show header in landscape.
                if (DeviceUtils.isLandscape(getActivity())) {
                    mLandscapeHeader.setVisibility(View.VISIBLE);
                }

                // Set old tasks as shown.
                sIsShowingOld = true;

                // Show old tasks.
                mTasks = mTasksService.loadCompletedTasks();
                keepSelection();
                mAdapter.showOld(mTasks, mListViewHeight);
            }
        });
    }

    @OnClick(R.id.button_clear_old)
    protected void clearOldTasks() {
        // Display confirmation dialog.
        SwipesDialog.show(new SwipesDialog.Builder(getActivity())
                .actionsColor(ThemeUtils.getSectionColor(mSection, getActivity()))
                .title(R.string.clear_old_dialog_title)
                .content(R.string.clear_old_dialog_text)
                .positiveText(R.string.clear_old_dialog_yes)
                .negativeText(R.string.clear_old_dialog_no)
                .callback(new MaterialDialog.ButtonCallback() {
                    @Override
                    public void onPositive(MaterialDialog dialog) {
                        // List of old tasks to delete.
                        List<GsonTask> oldTasks = new ArrayList<GsonTask>();

                        for (GsonTask task : mTasksService.loadCompletedTasks()) {
                            // Check if it's an old task.
                            if (DateUtils.isOlderThanToday(task.getLocalCompletionDate())) {
                                // Add to the removal list.
                                oldTasks.add(task);
                            }
                        }

                        // Proceed with delete.
                        mTasksService.deleteTasks(oldTasks);
                        refreshTaskList(false);

                        // Hide buttons.
                        mHeaderView.setVisibility(View.GONE);
                    }
                }));
    }

    private void openSnoozeSelector(GsonTask task) {
        // Call snooze activity.
        Intent intent = new Intent(getActivity(), SnoozeActivity.class);
        intent.putExtra(Constants.EXTRA_TASK_ID, task.getId());
        intent.putExtra(Constants.EXTRA_SECTION_NUMBER, mSection.getSectionNumber());
        startActivityForResult(intent, Constants.SNOOZE_REQUEST_CODE);
    }

    private void startEditTask(Long taskId, boolean showActionSteps) {
        // Call edit task activity, passing the tempId of the selected task as parameter.
        Intent editTaskIntent = new Intent(getActivity(), EditTaskActivity.class);
        editTaskIntent.putExtra(Constants.EXTRA_TASK_ID, taskId);
        editTaskIntent.putExtra(Constants.EXTRA_SECTION_NUMBER, mSection.getSectionNumber());
        editTaskIntent.putExtra(Constants.EXTRA_SHOW_ACTION_STEPS, showActionSteps);

        startActivity(editTaskIntent);
    }

    private void showTags() {
        mTagsArea.setBackgroundColor(ThemeUtils.getBackgroundColor(getActivity()));
        mListArea.setVisibility(View.GONE);

        // Show tags area with fade animation.
        mTagsArea.setVisibility(View.VISIBLE);
        mTagsArea.setAlpha(0f);
        mTagsArea.animate().alpha(1f).setDuration(Constants.ANIMATION_DURATION_MEDIUM);

        // Hide main activity content.
        mActivity.hideActionButtons();

        // Disable pager swiping.
        mActivity.getViewPager().setSwipeable(false);

        loadTags();
    }

    private void closeTags() {
        mTagsArea.setVisibility(View.GONE);

        // Show tasks list area with fade animation.
        mListArea.setVisibility(View.VISIBLE);
        mListArea.setAlpha(0f);
        mListArea.animate().alpha(1f).setDuration(Constants.ANIMATION_DURATION_MEDIUM);

        // Show main activity content.
        mActivity.showActionButtons();

        // Enable pager swiping.
        mActivity.getViewPager().setSwipeable(true);

        // Refresh all task lists.
        mActivity.refreshSections(true);

        SyncService.getInstance().performSync(true, Constants.SYNC_DELAY);

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
        // Close tags area with animation.
        closeTags();
    }

    @OnClick(R.id.assign_tags_add_button)
    protected void addTag() {
        // Create tag title input.
        final ActionEditText input = new ActionEditText(getActivity());
        input.setHint(getString(R.string.add_tag_dialog_hint));
        input.setHintTextColor(ThemeUtils.getHintColor(getActivity()));
        input.setTextColor(ThemeUtils.getTextColor(getActivity()));
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.requestFocus();

        // Display dialog to save new tag.
        final SwipesDialog dialog = SwipesDialog.show(new SwipesDialog.Builder(getActivity())
                .actionsColor(ThemeUtils.getSectionColor(mSection, getActivity()))
                .title(R.string.add_tag_dialog_title)
                .positiveText(R.string.add_tag_dialog_yes)
                .negativeText(R.string.add_tag_dialog_no)
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
                }));

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
        Analytics.sendEvent(Categories.TAGS, Actions.ADDED_TAG, Labels.TAGS_FROM_SELECTION, (long) title.length());

        // Assign to selected tasks.
        assignTag(tag);

        // Refresh displayed tags.
        loadTags();

        // Perform sync.
        mSyncService.performSync(true, Constants.SYNC_DELAY);

        // Play sound.
        SoundHandler.playSound(getActivity(), R.raw.action_positive);
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
        LinearLayout layout = new LinearLayout(getActivity());
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        int margin = getActivity().getResources().getDimensionPixelSize(R.dimen.add_tag_input_margin);
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
            int resource = ThemeUtils.isLightTheme(getActivity()) ? R.layout.tag_box_light : R.layout.tag_box_dark;
            CheckBox tagBox = (CheckBox) getActivity().getLayoutInflater().inflate(resource, null);
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
            final ActionEditText input = new ActionEditText(getActivity());
            input.setText(selectedTag.getTitle());
            input.setHint(getString(R.string.add_tag_dialog_hint));
            input.setHintTextColor(ThemeUtils.getHintColor(getActivity()));
            input.setTextColor(ThemeUtils.getTextColor(getActivity()));
            input.setInputType(InputType.TYPE_CLASS_TEXT);
            input.requestFocus();

            // Display dialog to edit tag.
            final SwipesDialog dialog = SwipesDialog.show(new SwipesDialog.Builder(getActivity())
                    .actionsColor(ThemeUtils.getSectionColor(Sections.FOCUS, getActivity()))
                    .title(R.string.edit_tag_dialog_title)
                    .positiveText(R.string.add_tag_dialog_yes)
                    .neutralText(R.string.delete_tag_dialog_yes)
                    .negativeText(R.string.add_tag_dialog_no)
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
                    }));

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
        SwipesDialog.show(new SwipesDialog.Builder(getActivity())
                .actionsColor(ThemeUtils.getSectionColor(mSection, getActivity()))
                .title(getString(R.string.delete_tag_dialog_title, selectedTag.getTitle()))
                .content(R.string.delete_tag_dialog_message)
                .positiveText(R.string.delete_tag_dialog_yes)
                .negativeText(R.string.delete_tag_dialog_no)
                .callback(new MaterialDialog.ButtonCallback() {
                    @Override
                    public void onPositive(MaterialDialog dialog) {
                        // Delete tag and unassign it from all tasks.
                        mTasksService.deleteTag(selectedTag.getId());

                        // Send analytics event.
                        Analytics.sendEvent(Categories.TAGS, Actions.DELETED_TAG, Labels.TAGS_FROM_SELECTION, null);

                        // Refresh displayed tags.
                        loadTags();

                        // Perform sync.
                        mSyncService.performSync(true, Constants.SYNC_DELAY);

                        // Play sound.
                        SoundHandler.playSound(getActivity(), R.raw.action_negative);
                    }
                }));
    }

    private boolean isTagAssigned(GsonTag selectedTag) {
        int assigns = 0;
        // Using a counter, check if the tag is assigned to all selected tasks.
        for (GsonTask task : sSelectedTasks) {
            // Increase counter if tag is already assigned to the task.
            for (GsonTag tag : task.getTags()) {
                if (tag.getId().equals(selectedTag.getId())) {
                    assigns++;
                    break;
                }
            }
        }
        return assigns == sSelectedTasks.size();
    }

    private void assignTag(GsonTag tag) {
        mAssignedTags.add(tag);

        // Assign to all selected tasks.
        for (GsonTask task : sSelectedTasks) {
            task.setTags(mAssignedTags);
        }

        // Save selected tasks.
        mTasksService.saveTasks(sSelectedTasks, true);

        mAssignedTagsCount++;
    }

    private void unassignTag(GsonTag tag) {
        // Unassign from all selected tasks.
        mTasksService.unassignTag(tag.getId(), sSelectedTasks);

        // Remove from selected tags.
        mAssignedTags.remove(tag);

        mUnassignedTagsCount++;
    }

    private class FilterTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... params) {
            // Load tasks for each selected tag ("OR" filter).
            Set<GsonTask> filteredTasks = new LinkedHashSet<>();
            for (GsonTag tag : mActivity.getSelectedFilterTags()) {
                filteredTasks.addAll(mTasksService.loadTasksForTag(tag.getId(), mSection));
            }

            // Find tasks not containing all selected tags.
            Set<GsonTask> tasksToRemove = new LinkedHashSet<>();
            for (GsonTask task : filteredTasks) {
                if (!task.getTags().containsAll(mActivity.getSelectedFilterTags())) {
                    tasksToRemove.add(task);
                }
            }

            // Apply "AND" filter by default (remove tasks not matching).
            filteredTasks.removeAll(tasksToRemove);

            // Update list of tasks.
            mTasks = new ArrayList<>(filteredTasks);

            // Keep tasks selected after refresh.
            keepSelection();

            // Find last added task for scrolling.
            if ((mSection == Sections.LATER || mSection == Sections.FOCUS) && mActivity.hasAddedTask()) {
                mAddedTaskPosition = findLastAddedTask();
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            // Avoid updating while swiping screens or when a refresh is pending.
            if (!mActivity.isSwipingScreens() && !TasksActivity.hasPendingRefresh()) {
                // Update adapter with new data.
                updateFilterAdapter();
            } else {
                // Mark update as pending.
                TasksActivity.setPendingRefresh();
            }
        }
    }

    public void updateFilterAdapter() {
        // Make sure old tasks are shown.
        if (mSection == Sections.DONE) {
            mHeaderView.setVisibility(View.GONE);
            mAdapter.setShowingOld(true);
            sIsShowingOld = true;
        }

        // Show results footer.
        if (!mTasks.isEmpty()) showWorkspaceResults();

        // Update results count.
        updateResultsDescription(mTasks.size());

        // Refresh list with filtered tasks.
        mListView.setContentList(mTasks);
        mAdapter.update(mTasks, false);

        if (mSection == Sections.LATER || mSection == Sections.FOCUS) {
            // Scroll to last added task.
            scrollToLastAdded();
        }
    }

    private void hideWorkspaceResults() {
        mResultsText.setVisibility(View.GONE);
        mClearWorkspaceButton.setVisibility(View.GONE);
    }

    private void showWorkspaceResults() {
        mResultsText.setVisibility(View.VISIBLE);
        mClearWorkspaceButton.setVisibility(View.VISIBLE);
    }

    private void updateResultsDescription(int count) {
        String tags = null;

        for (GsonTag tag : mActivity.getSelectedFilterTags()) {
            if (tags == null) {
                tags = tag.getTitle();
            } else {
                tags += ", " + tag.getTitle();
            }
        }

        int descriptionId = 0;
        int searchDescriptionId = 0;
        int filterDescriptionId = 0;

        switch (mSection) {
            case LATER:
                descriptionId = R.plurals.later_workspace_results;
                searchDescriptionId = R.string.later_empty_search;
                filterDescriptionId = R.string.later_empty_workspace;
                break;
            case FOCUS:
                descriptionId = R.plurals.focus_workspace_results;
                searchDescriptionId = R.string.focus_empty_search;
                filterDescriptionId = R.string.focus_empty_workspace;
                break;
            case DONE:
                descriptionId = R.plurals.done_workspace_results;
                searchDescriptionId = R.string.done_empty_search;
                filterDescriptionId = R.string.done_empty_workspace;
                break;
        }

        String description = getResources().getQuantityString(descriptionId, count, String.valueOf(count), tags);

        if (count == 0) {
            boolean isSearch = !mActivity.getSearchQuery().isEmpty();

            if (isSearch) {
                description = getString(searchDescriptionId, mActivity.getSearchQuery());
            } else {
                description = getString(filterDescriptionId, tags);
            }
        }

        CharSequence styledDescription = Html.fromHtml(description);

        mResultsText.setText(styledDescription);
        mEmptyResultsText.setText(styledDescription);
    }

    private void showKeyboard() {
        InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.toggleSoftInput(0, 0);
    }

    private class SearchTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... params) {
            // Update list of tasks from query.
            String query = mActivity.getSearchQuery();
            mTasks = mTasksService.searchTasks(query, mSection);

            // Keep tasks selected after refresh.
            keepSelection();

            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            // Avoid updating while swiping screens or when a refresh is pending.
            if (!mActivity.isSwipingScreens() && !TasksActivity.hasPendingRefresh()) {
                // Update adapter with new data.
                updateSearchAdapter();
            } else {
                // Mark update as pending.
                TasksActivity.setPendingRefresh();
            }
        }
    }

    public void updateSearchAdapter() {
        String query = mActivity.getSearchQuery();

        if (mSection == Sections.DONE) {
            if (!query.isEmpty()) {
                // Make sure old tasks are shown.
                mHeaderView.setVisibility(View.GONE);
                mAdapter.setShowingOld(true);
            } else {
                // Hide old tasks before clearing search.
                mHeaderView.setVisibility(View.VISIBLE);
                mAdapter.setShowingOld(false);
            }
        }

        // Refresh list with results.
        mListView.setContentList(mTasks);
        mListView.smoothScrollToPosition(0);
        mAdapter.update(mTasks, false);
    }

    private void shareTasks() {
        String content = getString(R.string.share_message_header);

        // Append task titles.
        for (GsonTask task : sSelectedTasks) {
            content += getString(R.string.share_message_circle) + task.getTitle() + "\n";

            for (GsonTask subtask : mTasksService.loadSubtasksForTask(task.getTempId())) {
                content += "\t\t" + getString(R.string.share_message_circle) + subtask.getTitle() + "\n";
            }
        }

        content += "\n" + getString(R.string.share_message_footer_sent_from);
        content += "\n" + getString(R.string.share_message_footer_get_swipes);

        Intent inviteIntent = new Intent(Intent.ACTION_SEND);
        inviteIntent.setType("text/plain");
        inviteIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, getString(R.string.share_message_subject));
        inviteIntent.putExtra(android.content.Intent.EXTRA_TEXT, content);

        startActivity(Intent.createChooser(inviteIntent, getString(R.string.share_chooser_title)));

        // Send analytics event.
        Analytics.sendEvent(Categories.SHARE_TASK, Actions.SHARE_TASK_OPEN, null, (long) sSelectedTasks.size());
    }

    public boolean isDoneForToday() {
        return mDoneForToday;
    }

    public void fadeOutTasksList() {
        mListView.setAlpha(1f);
        mListView.animate().alpha(0f).setDuration(Constants.ANIMATION_DURATION_MEDIUM);
    }

    public void fadeInTasksList() {
        if (mSection == Sections.DONE && sIsShowingOld) {
            mListView.setAlpha(1f);
        } else {
            mListView.setAlpha(0f);
            mListView.animate().alpha(1f).setDuration(Constants.ANIMATION_DURATION_MEDIUM);
        }
    }

    public void setDragAndDropEnabled(boolean enabled) {
        if (mSection == Sections.FOCUS) {
            mListView.setDragAndDropEnabled(enabled);
        }
    }

    private void sendTagAssignEvents() {
        if (mAssignedTagsCount > 0) {
            Analytics.sendEvent(Categories.TAGS, Actions.ASSIGNED_TAGS, Labels.TAGS_FROM_SELECTION, (long) mAssignedTagsCount);
        }

        if (mUnassignedTagsCount > 0) {
            Analytics.sendEvent(Categories.TAGS, Actions.UNASSIGNED_TAGS, Labels.TAGS_FROM_SELECTION, (long) mUnassignedTagsCount);
        }
    }

    private void sendClearedTasksEvent() {
        String label = mDoneForToday ? Labels.DONE_FOR_TODAY : Labels.DONE_FOR_NOW;

        // Send analytics event.
        Analytics.sendEvent(Categories.ACTIONS, Actions.CLEARED_TASKS, label, null);
    }

    private void handleWelcomeDialog() {
        // Hide list view when showing welcome dialog.
        if (!PreferenceUtils.hasShownWelcomeScreen(getActivity()) && mSection == Sections.FOCUS) {
            mListView.setAlpha(0f);
        }
    }

}
