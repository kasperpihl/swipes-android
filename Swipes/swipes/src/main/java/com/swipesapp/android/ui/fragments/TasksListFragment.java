package com.swipesapp.android.ui.fragments;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Activity;
import android.app.ListFragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
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
import android.widget.ScrollView;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.fortysevendeg.swipelistview.BaseSwipeListViewListener;
import com.fortysevendeg.swipelistview.DynamicListView;
import com.fortysevendeg.swipelistview.SwipeListView;
import com.swipesapp.android.R;
import com.swipesapp.android.analytics.handler.Analytics;
import com.swipesapp.android.analytics.handler.IntercomHandler;
import com.swipesapp.android.analytics.values.Actions;
import com.swipesapp.android.analytics.values.Categories;
import com.swipesapp.android.analytics.values.IntercomEvents;
import com.swipesapp.android.analytics.values.IntercomFields;
import com.swipesapp.android.analytics.values.Labels;
import com.swipesapp.android.handler.RepeatHandler;
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
import com.swipesapp.android.ui.view.SwipesButton;
import com.swipesapp.android.ui.view.SwipesDialog;
import com.swipesapp.android.util.DateUtils;
import com.swipesapp.android.util.DeviceUtils;
import com.swipesapp.android.util.PreferenceUtils;
import com.swipesapp.android.util.ThemeUtils;
import com.swipesapp.android.values.Constants;
import com.swipesapp.android.values.Intents;
import com.swipesapp.android.values.Sections;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import butterknife.ButterKnife;
import butterknife.InjectView;
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

    // Customized list view to display tasks.
    private DynamicListView mListView;

    // List view height, used for UI calculations.
    private int mListViewHeight;

    // Adapter for tasks.
    private TasksListAdapter mAdapter;

    // Service to perform tasks operations.
    private TasksService mTasksService;

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

    // Controls the display of old tasks.
    private static boolean sIsShowingOld;

    // Footer views.
    private LinearLayout mResultsView;
    private TextView mResultsText;
    private FlatButton mClearWorkspaceButton;

    @InjectView(android.R.id.empty)
    ViewStub mViewStub;

    @InjectView(R.id.header_view)
    LinearLayout mHeaderView;

    @InjectView(R.id.list_area)
    LinearLayout mListArea;

    @InjectView(R.id.assign_tags_area)
    LinearLayout mTagsArea;

    @InjectView(R.id.assign_tags_container)
    FlowLayout mTaskTagsContainer;

    @InjectView(R.id.landscape_header)
    RelativeLayout mLandscapeHeader;

    @InjectView(R.id.landscape_header_area)
    LinearLayout mLandscapeHeaderArea;

    @InjectView(R.id.action_bar_title)
    TextView mLandscapeHeaderTitle;

    @InjectView(R.id.action_bar_icon)
    SwipesButton mLandscapeHeaderIcon;

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

        mRepeatHandler = new RepeatHandler(getActivity());

        sSelectedTasks = new ArrayList<GsonTask>();

        int sectionNumber = args.getInt(ARG_SECTION_NUMBER, Sections.FOCUS.getSectionNumber());
        mSection = Sections.getSectionByNumber(sectionNumber);

        View rootView = inflater.inflate(R.layout.fragment_tasks_list, container, false);
        ButterKnife.inject(this, rootView);

        // Setup view for current section.
        switch (mSection) {
            case LATER:
                setupView(rootView, R.layout.tasks_later_empty_view);
                configureLaterView(mAdapter);
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
                break;
        }

        measureListView(mListView);

        refreshTaskList(false);

        return rootView;
    }

    @Override
    public void onDestroyView() {
        ButterKnife.reset(this);
        super.onDestroyView();
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

        // Refresh if tasks were added from intent.
        if (mSection == Sections.FOCUS && PreferenceUtils.hasAddedTasksFromIntent(getActivity())) {
            refreshTaskList(false);

            // Reset preference.
            PreferenceUtils.saveBoolean(PreferenceUtils.TASKS_ADDED_FROM_INTENT, false, getActivity());
        }

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
        // Check if request code is the one from snooze task.
        if (requestCode == Constants.SNOOZE_REQUEST_CODE) {
            switch (resultCode) {
                case Activity.RESULT_OK:
                    // Task has been snoozed. Refresh all task lists.
                    mActivity.refreshSections();
                    break;
                case Activity.RESULT_CANCELED:
                    // Snooze has been canceled. Refresh tasks with animation.
                    refreshTaskList(true);
                    break;
            }
        } else if (requestCode == Constants.EDIT_TASK_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                // Refresh all tasks after editing.
                mActivity.refreshSections();
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

        // Set colors and components.
        mListView.setContainerBackgroundColor(ThemeUtils.getBackgroundColor(getActivity()));
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
        mLandscapeHeaderIcon.disableTouchFeedback();

        // Disable edge effect.
        if (DeviceUtils.isTablet(getActivity())) {
            mListView.setOverScrollMode(View.OVER_SCROLL_NEVER);
        }
    }

    private void setupResultsFooter() {
        // Add filter views.
        mResultsView = (LinearLayout) getActivity().getLayoutInflater().inflate(R.layout.results_footer, null);
        mResultsText = (TextView) mResultsView.findViewById(R.id.workspace_results);
        mListView.addFooterView(mResultsView);

        // Add clear listener.
        mClearWorkspaceButton = (FlatButton) mResultsView.findViewById(R.id.clear_workspace_button);
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

            Button facebookShare = (Button) mEmptyView.findViewById(R.id.button_facebook_share);
            facebookShare.setBackgroundResource(ThemeUtils.isLightTheme(getActivity()) ?
                    R.drawable.ic_facebook_light : R.drawable.ic_facebook_dark);
            setButtonSelector(facebookShare);

            Button twitterShare = (Button) mEmptyView.findViewById(R.id.button_twitter_share);
            twitterShare.setBackgroundResource(ThemeUtils.isLightTheme(getActivity()) ?
                    R.drawable.ic_twitter_light : R.drawable.ic_twitter_dark);
            setButtonSelector(twitterShare);
        }
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

    private void measureListView(final DynamicListView listView) {
        if (listView != null) {
            listView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                public void onGlobalLayout() {
                    // Save list view height for later calculations.
                    mListViewHeight = listView.getHeight();
                }
            });
        }
    }

    public void refreshTaskList(boolean animateRefresh) {
        // Block refresh while swiping.
        if (!mListView.isSwiping()) {

            // Only refresh as usual when workspace is inactive.
            if (mActivity.getSelectedFilterTags().isEmpty()) {
                List<GsonTask> tasks;

                // Update adapter with new data.
                switch (mSection) {
                    case LATER:
                        tasks = mTasksService.loadScheduledTasks();
                        keepSelection(tasks);
                        mAdapter.update(tasks, animateRefresh);

                        // Refresh empty view.
                        sNextTask = !tasks.isEmpty() ? tasks.get(0) : null;
                        mActivity.updateEmptyView();
                        break;
                    case FOCUS:
                        tasks = mTasksService.loadFocusedTasks();
                        keepSelection(tasks);
                        mListView.setContentList(tasks);
                        mAdapter.update(tasks, animateRefresh);
                        break;
                    case DONE:
                        tasks = mTasksService.loadCompletedTasks();
                        keepSelection(tasks);
                        handleDoneButtons(tasks);
                        mAdapter.setShowingOld(sIsShowingOld);
                        mAdapter.update(tasks, animateRefresh);
                        break;
                }
            } else {
                // Workspace is active. Reload filter.
                filterByTags();
            }
        }
    }

    private void keepSelection(List<GsonTask> tasks) {
        for (GsonTask selected : sSelectedTasks) {
            for (GsonTask task : tasks) {
                if (selected.getTempId().equals(task.getTempId())) {
                    task.setSelected(true);
                    break;
                }
            }
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
                if (!mActivity.isSelectionMode()) {
                    // Enable or disable swiping.
                    boolean enabled = isCurrentSection() || DeviceUtils.isLandscape(getActivity());
                    mListView.setSwipeEnabled(enabled);
                }
            } else if (action.equals(Intents.SELECTION_STARTED)) {
                // TODO: Disable swiping.
//                mListView.setSwipeEnabled(false);
            } else if (action.equals(Intents.SELECTION_CLEARED)) {
                // Clear selected tasks and stop selection mode.
                sSelectedTasks.clear();
                mActivity.cancelSelection();

                // TODO: Enable swiping.
//                mListView.setSwipeEnabled(true);

                // Refresh all sections.
                refreshTaskList(false);
            } else if (action.equals(Intents.FILTER_BY_TAGS)) {
                // Filter by tags or clear results.
                if (!mActivity.getSelectedFilterTags().isEmpty()) {
                    filterByTags();

                    // Send analytics event.
                    long value = (long) mActivity.getSelectedFilterTags().size();
                    sendFilterByTagsEvent(value);
                } else {
                    // Hide old tasks before refreshing.
                    if (mSection == Sections.DONE) {
                        mHeaderView.setVisibility(View.VISIBLE);
                        sIsShowingOld = false;
                    }

                    // Hide results footer.
                    hideWorkspaceResults();

                    refreshTaskList(false);
                }
            } else if (action.equals(Intents.PERFORM_SEARCH)) {
                // Update search results.
                performSearch();
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
                        // Move task from Later to Focus.
                        task.setLocalSchedule(new Date());
                        mTasksService.saveTask(task, true);
                        // Refresh all lists.
                        mActivity.refreshSections();
                        break;
                    case FOCUS:
                        // Move task from Focus to Done.
                        task.setLocalCompletionDate(new Date());
                        mTasksService.saveTask(task, true);
                        // Handle repeat.
                        mRepeatHandler.handleRepeatedTask(task);
                        // Refresh all lists.
                        mActivity.refreshSections();
                        // Send analytics event.
                        sendTaskCompletedEvent();
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
                        mActivity.refreshSections();
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
                        mActivity.refreshSections();
                        // Send analytics event.
                        sendTaskCompletedEvent();
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
                sendTaskPriorityEvent(label);
            }
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
    }

    @Override
    public void listReordered(List list) {
        if (mSection == Sections.FOCUS) {
            reorderTasks((List<GsonTask>) list);

            // Clear selected tasks and hide edit bar.
            sSelectedTasks.clear();
            mActivity.cancelSelection();

            refreshTaskList(false);
        }
    }

    private void reorderTasks(List<GsonTask> tasks) {
        // Save task order as its position on the list.
        for (int i = 0; i < tasks.size(); i++) {
            GsonTask task = tasks.get(i);
            task.setOrder(i);
            mTasksService.saveTask(task, true);
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
        if (mSection == Sections.FOCUS) {
            ScrollView focusEmptyView = (ScrollView) mEmptyView.findViewById(R.id.focus_empty_view);

            updateEmptyView();

            // Animate empty view.
            if (focusEmptyView.getAlpha() == 0f) {
                focusEmptyView.animate().alpha(1f).setDuration(Constants.ANIMATION_DURATION_LONG).start();
            }

            // Send cleared tasks event.
            sendClearedTasksEvent();
        }

        if (DeviceUtils.isLandscape(getActivity())) {
            // Hide landscape header.
            mLandscapeHeader.setVisibility(View.GONE);
        }
    }

    private void hideEmptyView() {
        if (mSection == Sections.FOCUS) {
            ScrollView focusEmptyView = (ScrollView) mEmptyView.findViewById(R.id.focus_empty_view);
            focusEmptyView.setAlpha(0f);

            mDoneForToday = false;
        }

        if (DeviceUtils.isLandscape(getActivity())) {
            // Show landscape header.
            mLandscapeHeader.setVisibility(View.VISIBLE);
        }
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
        GsonTask oldestTask = !completedTasks.isEmpty() ? completedTasks.get(completedTasks.size() - 1) : null;
        Date completionDate = oldestTask != null ? oldestTask.getLocalCompletionDate() : null;

        // Only display buttons in the done section and when the oldest completed task is older than today.
        if (!sIsShowingOld && DateUtils.isOlderThanToday(completionDate)) {
            mHeaderView.setVisibility(View.VISIBLE);
            mHeaderView.setAlpha(1f);
        }
    }

    private void deleteSelectedTasks() {
        // Display confirmation dialog.
        new SwipesDialog.Builder(getActivity())
                .title(getResources().getQuantityString(R.plurals.delete_task_dialog_title, sSelectedTasks.size(), sSelectedTasks.size()))
                .content(R.string.delete_task_dialog_text)
                .positiveText(R.string.delete_task_dialog_yes)
                .negativeText(R.string.delete_task_dialog_no)
                .actionsColor(ThemeUtils.getSectionColor(mSection, getActivity()))
                .callback(new MaterialDialog.ButtonCallback() {
                    @Override
                    public void onPositive(MaterialDialog dialog) {
                        // Proceed with delete.
                        mTasksService.deleteTasks(sSelectedTasks);

                        // Send analytics event.
                        sendDeletedTasksEvent();

                        // Clear selection.
                        sSelectedTasks.clear();
                        mActivity.updateSelectionCount(sSelectedTasks.size());

                        // Refresh all task lists.
                        mActivity.refreshSections();
                    }
                })
                .show();
    }

    @OnClick(R.id.button_show_old)
    protected void showOldTasks() {
        // Animate footer view.
        mHeaderView.animate().alpha(0f).setDuration(Constants.ANIMATION_DURATION_SHORT).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                // Hide buttons.
                mHeaderView.setVisibility(View.GONE);

                // Show header in landscape.
                if (DeviceUtils.isLandscape(getActivity())) {
                    mLandscapeHeader.setVisibility(View.VISIBLE);
                }

                // Show old tasks.
                List<GsonTask> tasks = mTasksService.loadCompletedTasks();
                keepSelection(tasks);
                mAdapter.showOld(tasks, mListViewHeight);

                // Set old tasks as shown.
                sIsShowingOld = true;
            }
        });
    }

    @OnClick(R.id.button_clear_old)
    protected void clearOldTasks() {
        // Display confirmation dialog.
        new SwipesDialog.Builder(getActivity())
                .title(R.string.clear_old_dialog_title)
                .content(R.string.clear_old_dialog_text)
                .positiveText(R.string.clear_old_dialog_yes)
                .negativeText(R.string.clear_old_dialog_no)
                .actionsColor(ThemeUtils.getSectionColor(mSection, getActivity()))
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
                })
                .show();
    }

    private void openSnoozeSelector(GsonTask task) {
        // Call snooze activity.
        Intent intent = new Intent(getActivity(), SnoozeActivity.class);
        intent.putExtra(Constants.EXTRA_TASK_ID, task.getId());
        startActivityForResult(intent, Constants.SNOOZE_REQUEST_CODE);
    }

    private void startEditTask(Long taskId, boolean showActionSteps) {
        // Call edit task activity, passing the tempId of the selected task as parameter.
        Intent editTaskIntent = new Intent(getActivity(), EditTaskActivity.class);
        editTaskIntent.putExtra(Constants.EXTRA_TASK_ID, taskId);
        editTaskIntent.putExtra(Constants.EXTRA_SECTION_NUMBER, mSection.getSectionNumber());
        editTaskIntent.putExtra(Constants.EXTRA_SHOW_ACTION_STEPS, showActionSteps);

        startActivityForResult(editTaskIntent, Constants.EDIT_TASK_REQUEST_CODE);
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
        mActivity.refreshSections();

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
        final SwipesDialog dialog = new SwipesDialog.Builder(getActivity())
                .title(R.string.add_tag_dialog_title)
                .positiveText(R.string.add_tag_dialog_yes)
                .negativeText(R.string.add_tag_dialog_no)
                .actionsColor(ThemeUtils.getSectionColor(mSection, getActivity()))
                .customView(customizeAddTagInput(input), false)
                .callback(new MaterialDialog.ButtonCallback() {
                    @Override
                    public void onPositive(MaterialDialog dialog) {
                        String title = input.getText().toString();

                        if (!title.isEmpty()) {
                            // Save new tag to database.
                            mTasksService.createTag(title);

                            // Send analytics event.
                            sendTagAddedEvent((long) title.length());

                            // Refresh displayed tags.
                            loadTags();

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
                        // Save new tag to database.
                        mTasksService.createTag(title);

                        // Refresh displayed tags.
                        loadTags();
                    }

                    dialog.dismiss();
                }
                return true;
            }
        });
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

            // Display dialog to delete tag.
            new SwipesDialog.Builder(getActivity())
                    .title(getString(R.string.delete_tag_dialog_title, selectedTag.getTitle()))
                    .content(R.string.delete_tag_dialog_message)
                    .positiveText(R.string.delete_tag_dialog_yes)
                    .negativeText(R.string.delete_tag_dialog_no)
                    .actionsColor(ThemeUtils.getSectionColor(mSection, getActivity()))
                    .callback(new MaterialDialog.ButtonCallback() {
                        @Override
                        public void onPositive(MaterialDialog dialog) {
                            // Delete tag and unassign it from all tasks.
                            mTasksService.deleteTag(selectedTag.getId());

                            // Send analytics event.
                            sendTagDeletedEvent();

                            // Refresh displayed tags.
                            loadTags();
                        }
                    })
                    .show();

            return true;
        }
    };

    private boolean isTagAssigned(GsonTag selectedTag) {
        int assigns = 0;
        // Using a counter, check if the tag is assigned to all selected tasks.
        for (GsonTask task : sSelectedTasks) {
            // Increase counter if tag is already assigned to the task.
            for (GsonTag tag : task.getTags()) {
                if (tag.getId().equals(selectedTag.getId())) {
                    assigns++;
                }
            }
        }
        return assigns == sSelectedTasks.size();
    }

    private void assignTag(GsonTag tag) {
        // Assign to all selected tasks.
        for (GsonTask task : sSelectedTasks) {
            mAssignedTags.add(tag);
            task.setTags(mAssignedTags);
            mTasksService.saveTask(task, true);
        }

        mAssignedTagsCount++;
    }

    private void unassignTag(GsonTag tag) {
        // Unassign from all selected tasks.
        for (GsonTask task : sSelectedTasks) {
            mTasksService.unassignTag(tag.getId(), task.getId());
        }

        // Remove from selected tags.
        mAssignedTags.remove(tag);

        mUnassignedTagsCount++;
    }

    private void filterByTags() {
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

        // Make sure old tasks are shown.
        if (mSection == Sections.DONE) {
            mHeaderView.setVisibility(View.GONE);
            mAdapter.setShowingOld(true);
            sIsShowingOld = true;
        }

        // Show results footer.
        showWorkspaceResults();

        // Update results count.
        updateResultsDescription(filteredTasks.size());

        // Refresh list with filtered tasks.
        List<GsonTask> list = new ArrayList<>(filteredTasks);
        mListView.setContentList(list);
        mAdapter.update(list, false);
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
        String session = "";

        switch (mSection) {
            case LATER:
                session = getString(R.string.later_results_description);
                break;
            case FOCUS:
                session = getString(R.string.focus_results_description);
                break;
            case DONE:
                session = getString(R.string.done_results_description);
                break;
        }

        String tags = null;
        for (GsonTag tag : mActivity.getSelectedFilterTags()) {
            if (tags == null) {
                tags = tag.getTitle();
            } else {
                tags += ", " + tag.getTitle();
            }
        }

        String description = getResources().getQuantityString(R.plurals.workspace_results, count, String.valueOf(count), session, tags);
        CharSequence styledDescription = Html.fromHtml(description);

        mResultsText.setText(styledDescription);
    }

    private void showKeyboard() {
        InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.toggleSoftInput(0, 0);
    }

    private void hideKeyboard() {
        InputMethodManager inputManager = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        inputManager.toggleSoftInput(0, 0);

        // Remove focus from text views by focusing on list area.
        mListArea.requestFocus();
    }

    private void performSearch() {
        String query = mActivity.getSearchQuery();
        List<GsonTask> results = mTasksService.searchTasks(query, mSection);

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
        mListView.setContentList(results);
        mAdapter.update(results, false);
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
        sendShareTaskEvent();
    }

    public boolean isDoneForToday() {
        return mDoneForToday;
    }

    public void fadeOutTasksList() {
        mListView.animate().alpha(0f).setDuration(Constants.ANIMATION_DURATION_MEDIUM);
    }

    public void fadeInTasksList() {
        mListView.animate().alpha(1f).setDuration(Constants.ANIMATION_DURATION_MEDIUM);
    }

    public void setDragAndDropEnabled(boolean enabled) {
        if (mSection == Sections.FOCUS) {
            mListView.setDragAndDropEnabled(enabled);
        }
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
                Labels.TAGS_FROM_SELECTION, (long) mAssignedTagsCount);

        // Prepare Intercom fields.
        HashMap<String, Object> fields = new HashMap<>();
        fields.put(IntercomFields.NUMBER_OF_TASKS, sSelectedTasks.size());
        fields.put(IntercomFields.NUMBER_OF_TAGS, mAssignedTagsCount);
        fields.put(IntercomFields.FROM, Labels.TAGS_FROM_SELECTION);

        // Send Intercom events.
        IntercomHandler.sendEvent(IntercomEvents.ASSIGN_TAGS, fields);
    }

    private void sendTagUnassignEvent() {
        // Send tags unassigned event.
        Analytics.sendEvent(Categories.TAGS, Actions.UNASSIGNED_TAGS,
                Labels.TAGS_FROM_SELECTION, (long) mUnassignedTagsCount);

        // Prepare Intercom fields.
        HashMap<String, Object> fields = new HashMap<>();
        fields.put(IntercomFields.NUMBER_OF_TASKS, sSelectedTasks.size());
        fields.put(IntercomFields.NUMBER_OF_TAGS, mUnassignedTagsCount);
        fields.put(IntercomFields.FROM, Labels.TAGS_FROM_SELECTION);

        // Send Intercom events.
        IntercomHandler.sendEvent(IntercomEvents.UNASSIGN_TAGS, fields);
    }

    private void sendClearedTasksEvent() {
        String label = mDoneForToday ? Labels.DONE_FOR_TODAY : Labels.DONE_FOR_NOW;
        String labelIntercom = mDoneForToday ? Labels.DONE_TODAY : Labels.DONE_NOW;

        // Send analytics event.
        Analytics.sendEvent(Categories.ACTIONS, Actions.CLEARED_TASKS, label, null);

        // Prepare Intercom fields.
        HashMap<String, Object> fields = new HashMap<>();
        fields.put(IntercomFields.DONE_FOR_TODAY, labelIntercom);

        // Send Intercom events.
        IntercomHandler.sendEvent(IntercomEvents.CLEARED_TASKS, fields);
    }

    private void sendTaskCompletedEvent() {
        // Send analytics event.
        Analytics.sendEvent(Categories.TASKS, Actions.COMPLETED_TASKS, null, 1l);

        // Prepare Intercom fields.
        HashMap<String, Object> fields = new HashMap<>();
        fields.put(IntercomFields.NUMBER_OF_TASKS, 1);

        // Send Intercom events.
        IntercomHandler.sendEvent(IntercomEvents.COMPLETED_TASKS, fields);
    }

    private void sendDeletedTasksEvent() {
        long numberOfTasks = (long) sSelectedTasks.size();

        // Send analytics event.
        Analytics.sendEvent(Categories.TASKS, Actions.DELETED_TASKS, null, numberOfTasks);

        // Prepare Intercom fields.
        HashMap<String, Object> fields = new HashMap<>();
        fields.put(IntercomFields.NUMBER_OF_TASKS, numberOfTasks);

        // Send Intercom events.
        IntercomHandler.sendEvent(IntercomEvents.DELETED_TASKS, fields);
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

    private void handleWelcomeDialog() {
        // Hide list view when showing welcome dialog.
        if (!PreferenceUtils.hasShownWelcomeScreen(getActivity()) && mSection == Sections.FOCUS) {
            mListView.setAlpha(0f);
        }
    }

    private void sendTagAddedEvent(long length) {
        // Send analytics event.
        Analytics.sendEvent(Categories.TAGS, Actions.ADDED_TAG, Labels.TAGS_FROM_SELECTION, length);

        // Prepare Intercom fields.
        HashMap<String, Object> fields = new HashMap<>();
        fields.put(IntercomFields.LENGHT, length);
        fields.put(IntercomFields.FROM, Labels.TAGS_FROM_SELECTION);

        // Send Intercom events.
        IntercomHandler.sendEvent(IntercomEvents.ADDED_TAG, fields);
    }

    private void sendTagDeletedEvent() {
        // Send analytics event.
        Analytics.sendEvent(Categories.TAGS, Actions.DELETED_TAG, Labels.TAGS_FROM_SELECTION, null);

        // Prepare Intercom fields.
        HashMap<String, Object> fields = new HashMap<>();
        fields.put(IntercomFields.FROM, Labels.TAGS_FROM_SELECTION);

        // Send Intercom events.
        IntercomHandler.sendEvent(IntercomEvents.DELETED_TAG, fields);
    }

    private void sendFilterByTagsEvent(long value) {
        // Send analytics event.
        Analytics.sendEvent(Categories.WORKSPACES, Actions.FILTER_TAGS, null, value);

        // Prepare Intercom fields.
        HashMap<String, Object> fields = new HashMap<>();
        fields.put(IntercomFields.NUMBER_OF_TAGS, value);

        // Send Intercom events.
        IntercomHandler.sendEvent(IntercomEvents.FILTER_TAGS, fields);
    }

    private void sendShareTaskEvent() {
        long value = (long) sSelectedTasks.size();

        // Send analytics event.
        Analytics.sendEvent(Categories.SHARE_TASK, Actions.SHARE_TASK_OPEN, null, value);

        // Prepare Intercom fields.
        HashMap<String, Object> fields = new HashMap<>();
        fields.put(IntercomFields.NUMBER_OF_TASKS, value);

        // Send Intercom events.
        IntercomHandler.sendEvent(IntercomEvents.SHARE_TASK_OPENED, fields);
    }

}
