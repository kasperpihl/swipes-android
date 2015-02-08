package com.swipesapp.android.ui.fragments;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Activity;
import android.app.ListFragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
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
import android.widget.ScrollView;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.fortysevendeg.swipelistview.BaseSwipeListViewListener;
import com.fortysevendeg.swipelistview.DynamicListView;
import com.fortysevendeg.swipelistview.SwipeListView;
import com.swipesapp.android.R;
import com.swipesapp.android.handler.RepeatHandler;
import com.swipesapp.android.sync.gson.GsonTag;
import com.swipesapp.android.sync.gson.GsonTask;
import com.swipesapp.android.sync.service.SyncService;
import com.swipesapp.android.sync.service.TasksService;
import com.swipesapp.android.ui.activity.EditDoneTaskActivity;
import com.swipesapp.android.ui.activity.EditLaterTaskActivity;
import com.swipesapp.android.ui.activity.EditTaskActivity;
import com.swipesapp.android.ui.activity.SnoozeActivity;
import com.swipesapp.android.ui.activity.TasksActivity;
import com.swipesapp.android.ui.adapter.TasksListAdapter;
import com.swipesapp.android.ui.listener.KeyboardBackListener;
import com.swipesapp.android.ui.listener.ListContentsListener;
import com.swipesapp.android.ui.view.ActionEditText;
import com.swipesapp.android.ui.view.FlowLayout;
import com.swipesapp.android.ui.view.SwipesButton;
import com.swipesapp.android.ui.view.SwipesDialog;
import com.swipesapp.android.ui.view.SwipesTextView;
import com.swipesapp.android.util.Constants;
import com.swipesapp.android.util.DateUtils;
import com.swipesapp.android.util.DeviceUtils;
import com.swipesapp.android.util.ThemeUtils;
import com.swipesapp.android.values.Actions;
import com.swipesapp.android.values.Sections;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;

/**
 * Fragment for the list of tasks.
 */
public class TasksListFragment extends ListFragment implements DynamicListView.ListOrderListener, ListContentsListener {

    // The fragment argument representing the section number for this fragment.
    private static final String ARG_SECTION_NUMBER = "section_number";

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
    private List<Long> mSelectedFilterTags;

    // Filters containers.
    private LinearLayout mFiltersContainer;
    private LinearLayout mFiltersTagsContainer;
    private LinearLayout mFiltersSearchContainer;
    private FlowLayout mFiltersTagsArea;

    // Filters views.
    private ActionEditText mSearchEditText;
    private SwipesButton mFiltersTagsButton;
    private TextView mFiltersEmptyTags;
    private SwipesButton mCloseSearchButton;

    // Empty view.
    private View mEmptyView;

    // Controls the display of old tasks.
    private static boolean sIsShowingOld;

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
    LinearLayout mLandscapeHeader;

    @InjectView(R.id.action_bar_title)
    TextView mLandscapeHeaderTitle;

    @InjectView(R.id.action_bar_icon)
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

        mTasksService = TasksService.getInstance(getActivity());

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
                handleDoneButtons();
                break;
        }

        measureListView(mListView);

        hideFilters();

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
        mTasksService = TasksService.getInstance(getActivity());

        IntentFilter filter = new IntentFilter();
        filter.addAction(Actions.TAB_CHANGED);
        filter.addAction(Actions.ASSIGN_TAGS);
        filter.addAction(Actions.DELETE_TASKS);
        filter.addAction(Actions.SHARE_TASKS);
        filter.addAction(Actions.BACK_PRESSED);
        filter.addAction(Actions.SELECTION_CLEARED);

        getActivity().registerReceiver(mTasksReceiver, filter);

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
                    ((TasksActivity) getActivity()).refreshSections();
                    break;
                case Activity.RESULT_CANCELED:
                    // Snooze has been canceled. Refresh tasks with animation.
                    refreshTaskList(true);
                    break;
            }
        } else if (requestCode == Constants.EDIT_TASK_REQUEST_CODE) {
            // Refresh all tasks after editing.
            ((TasksActivity) getActivity()).refreshSections();
        }
    }

    private boolean isCurrentSection() {
        // Retrieve current section being displayed and compare with this fragment's section.
        return mSection == TasksActivity.getCurrentSection();
    }

    private void setupView(View rootView, int emptyView) {
        // Initialize adapter.
        mAdapter = new TasksListAdapter(getActivity(), R.layout.swipeable_cell, mSection);
        mAdapter.setListContentsListener(this);

        // Initialize list view.
        mListView = (DynamicListView) rootView.findViewById(android.R.id.list);

        // Setup filters.
        setupFiltersArea();

        // Setup empty view.
        mViewStub.setLayoutResource(emptyView);
        mEmptyView = mViewStub.inflate();
        mListView.setEmptyView(mEmptyView);

        // Setup landscape header.
        mLandscapeHeader.setVisibility(DeviceUtils.isLandscape(getActivity()) ? View.VISIBLE : View.GONE);
        mLandscapeHeader.setBackgroundColor(ThemeUtils.getSectionColor(mSection, getActivity()));
        mLandscapeHeaderTitle.setText(mSection.getSectionTitle(getActivity()));
        mLandscapeHeaderIcon.setText(mSection.getSectionIcon(getActivity()));
        mLandscapeHeaderIcon.setLayoutParams(mLandscapeHeaderTitle.getLayoutParams());
    }

    private void setupFiltersArea() {
        // Add filter views.
        mFiltersContainer = (LinearLayout) getActivity().getLayoutInflater().inflate(R.layout.filters_view, null);
        mFiltersTagsContainer = (LinearLayout) mFiltersContainer.findViewById(R.id.filters_tags_container);
        mFiltersSearchContainer = (LinearLayout) mFiltersContainer.findViewById(R.id.filters_search_container);
        mFiltersTagsArea = (FlowLayout) mFiltersContainer.findViewById(R.id.filters_tags_area);
        mFiltersEmptyTags = (TextView) mFiltersContainer.findViewById(R.id.filter_empty_tags);
        mListView.addHeaderView(mFiltersContainer);

        // Add listeners and customize buttons.
        mFiltersTagsButton = (SwipesButton) mFiltersContainer.findViewById(R.id.filters_tags_button);
        mFiltersTagsButton.setOnClickListener(mShowTagsFilterListener);

        SwipesButton filtersCloseTagsButton = (SwipesButton) mFiltersContainer.findViewById(R.id.filters_close_tags_button);
        filtersCloseTagsButton.setOnClickListener(mCloseTagsFilterListener);

        mSearchEditText = (ActionEditText) mFiltersContainer.findViewById(R.id.filters_search_edit_text);
        mSearchEditText.setOnFocusChangeListener(mSearchFocusListener);
        mSearchEditText.addTextChangedListener(mSearchTypeListener);
        mSearchEditText.setOnEditorActionListener(mSearchDoneListener);
        mSearchEditText.setListener(mKeyboardBackListener);

        mCloseSearchButton = (SwipesButton) mFiltersContainer.findViewById(R.id.filters_close_search_button);
        mCloseSearchButton.setOnClickListener(mSearchCloseListener);

        mFiltersContainer.setBackgroundColor(ThemeUtils.getBackgroundColor(getActivity()));
    }

    private void configureLaterView(TasksListAdapter adapter) {
        // Setup content.
        mListView.setAdapter(adapter);
        mListView.setSwipeListViewListener(mSwipeListener);
        mListView.setViewPager(((TasksActivity) getActivity()).getViewPager());

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
        mListView.setViewPager(((TasksActivity) getActivity()).getViewPager());

        // Setup back view.
        mListView.setSwipeBackgroundColors(ThemeUtils.getSectionColor(Sections.DONE, getActivity()), ThemeUtils.getSectionColor(Sections.LATER, getActivity()), Color.TRANSPARENT);
        mListView.setBackIconText(R.string.done_full, R.string.later_full);

        // Setup priority button.
        mListView.setFrontIcon(R.id.button_task_priority);
        mListView.setFrontIconBackgrounds(R.drawable.done_circle_selector, R.drawable.later_circle_selector, R.drawable.focus_circle_selector);

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
        mListView.setViewPager(((TasksActivity) getActivity()).getViewPager());

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
            List<GsonTask> tasks;

            // Update adapter with new data.
            switch (mSection) {
                case LATER:
                    tasks = mTasksService.loadScheduledTasks();
                    keepSelection(tasks);
                    mAdapter.update(tasks, animateRefresh);

                    // Refresh empty view.
                    sNextTask = !tasks.isEmpty() ? tasks.get(0) : null;
                    ((TasksActivity) getActivity()).updateEmptyView();
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
                    mAdapter.setShowingOld(sIsShowingOld);
                    mAdapter.update(tasks, animateRefresh);
                    break;
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
        // Reduce position by -1 to account for the header.
        return (GsonTask) mAdapter.getItem(position - 1);
    }

    private BroadcastReceiver mTasksReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Filter intent actions.
            if (intent.getAction().equals(Actions.TAB_CHANGED)) {
                // Enable or disable swiping.
                boolean enabled = isCurrentSection() || DeviceUtils.isLandscape(getActivity());
                mListView.setSwipeEnabled(enabled);
            } else if (intent.getAction().equals(Actions.SELECTION_CLEARED)) {
                // Refresh all sections.
                refreshTaskList(false);
            }

            // Filter actions intended only for this section.
            if (isCurrentSection()) {
                if (intent.getAction().equals(Actions.ASSIGN_TAGS)) {
                    // Hide buttons and show tags view.
                    showTags();
                } else if (intent.getAction().equals(Actions.DELETE_TASKS)) {
                    // Delete tasks.
                    deleteSelectedTasks();
                } else if (intent.getAction().equals(Actions.SHARE_TASKS)) {
                    // Send intent to share selected tasks by email.
                    shareTasks();

                    // Hide bar and clear selection.
                    ((TasksActivity) getActivity()).hideEditBar();
                    sSelectedTasks.clear();
                } else if (intent.getAction().equals(Actions.BACK_PRESSED)) {
                    // Don't close the app when assigning tags.
                    if (mTagsArea.getVisibility() == View.VISIBLE) {
                        closeTags();
                    } else if (!sSelectedTasks.isEmpty()) {
                        // Clear selected tasks and hide edit bar.
                        sSelectedTasks.clear();
                        ((TasksActivity) getActivity()).hideEditBar();

                        // Send broadcast to update UI.
                        mTasksService.sendBroadcast(Actions.SELECTION_CLEARED);
                    } else {
                        TasksActivity.clearCurrentSection();
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
            switch (mSection) {
                case LATER:
                    // Move task from Later to Focus.
                    getTask(position).setLocalSchedule(new Date());
                    mTasksService.saveTask(getTask(position), true);
                    refreshTaskList(false);
                    break;
                case FOCUS:
                    // Move task from Focus to Done.
                    getTask(position).setLocalCompletionDate(new Date());
                    mTasksService.saveTask(getTask(position), true);
                    // Handle repeat.
                    mRepeatHandler.handleRepeatedTask(getTask(position));
                    refreshTaskList(false);
                    break;
            }
        }

        @Override
        public void onFinishedSwipeLeft(int position) {
            switch (mSection) {
                case LATER:
                    // Reschedule task.
                    openSnoozeSelector(getTask(position));
                    break;
                case FOCUS:
                    // Move task from Focus to Later.
                    openSnoozeSelector(getTask(position));
                    break;
                case DONE:
                    // Move task from Done to Focus.
                    getTask(position).setLocalCompletionDate(null);
                    mTasksService.saveTask(getTask(position), true);
                    refreshTaskList(false);
                    break;
            }
        }

        @Override
        public void onFinishedLongSwipeRight(int position) {
            switch (mSection) {
                case LATER:
                    // Move task from Later to Done.
                    Date currentDate = new Date();
                    getTask(position).setLocalCompletionDate(currentDate);
                    getTask(position).setLocalSchedule(currentDate);
                    mTasksService.saveTask(getTask(position), true);
                    // Handle repeat.
                    mRepeatHandler.handleRepeatedTask(getTask(position));
                    refreshTaskList(false);
                    break;
            }
        }

        @Override
        public void onFinishedLongSwipeLeft(int position) {
            switch (mSection) {
                case LATER:
                    // Reschedule task.
                    openSnoozeSelector(getTask(position));
                    break;
                case DONE:
                    // Move task from Done to Later.
                    openSnoozeSelector(getTask(position));
                    break;
            }
        }

        @Override
        public void onClickFrontView(View view, int position) {
            GsonTask task = getTask(position);

            startEditTask(task.getId());

//            View selectedIndicator = view.findViewById(R.id.selected_indicator);
//
//            if (task.isSelected()) {
//                // Deselect task.
//                task.setSelected(false);
//                selectedIndicator.setBackgroundColor(0);
//                sSelectedTasks.remove(task);
//            } else {
//                // Select task.
//                task.setSelected(true);
//                selectedIndicator.setBackgroundColor(ThemeUtils.getSectionColor(mSection, getActivity()));
//                sSelectedTasks.add(task);
//            }
//
//            handleEditBar();
        }

        @Override
        public void onClickCheckbox(View view, int position) {
            CheckBox priorityButton = (CheckBox) view;
            boolean checked = !priorityButton.isChecked();
            priorityButton.setChecked(checked);
            Integer priority = checked ? 1 : 0;

            GsonTask task = getTask(position);
            task.setPriority(priority);

            mTasksService.saveTask(task, true);
            refreshTaskList(false);
        }
    };

    @Override
    public void listReordered(List list) {
        if (mSection == Sections.FOCUS) {
            reorderTasks((List<GsonTask>) list);

            // Clear selected tasks and hide edit bar.
            sSelectedTasks.clear();
            ((TasksActivity) getActivity()).hideEditBar();

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

            ((TasksActivity) getActivity()).hideEditBar();
        }
    }

    private void hideEmptyView() {
        if (mSection == Sections.FOCUS) {
            ScrollView focusEmptyView = (ScrollView) mEmptyView.findViewById(R.id.focus_empty_view);
            focusEmptyView.setAlpha(0f);
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
                }
            } else {
                // Show default message.
                allDoneText.setText(getString(R.string.all_done_today));
                nextTaskText.setText(getString(R.string.all_done_next_empty));
                allDoneMessage.setText(getString(R.string.all_done_today_message));
            }

            // Refresh sharing message.
            ((TasksActivity) getActivity()).setShareMessage(allDoneMessage.getText().toString());
        }
    }

    private void handleEditBar() {
        if (!sSelectedTasks.isEmpty()) {
            // Display bar.
            ((TasksActivity) getActivity()).showEditBar();
        } else {
            // Hide bar.
            ((TasksActivity) getActivity()).hideEditBar();
        }
    }

    private void handleDoneButtons() {
        // Load date of the oldest completed task.
        List<GsonTask> completedTasks = mTasksService.loadCompletedTasks();
        GsonTask oldestTask = !completedTasks.isEmpty() ? completedTasks.get(completedTasks.size() - 1) : null;
        Date completionDate = oldestTask != null ? oldestTask.getLocalCompletionDate() : null;

        // Only display buttons in the done section and when the oldest completed task is older than today.
        if (mSection == Sections.DONE && !sIsShowingOld && DateUtils.isOlderThanToday(completionDate)) {
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
                        refreshTaskList(false);
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

    private void startEditTask(Long taskId) {
        // Initialize intent passing the tempId of the selected task as parameter.
        Intent editTaskIntent = new Intent();
        editTaskIntent.putExtra(Constants.EXTRA_TASK_ID, taskId);

        // Decide which variation to call.
        if (mSection == Sections.LATER) {
            editTaskIntent.setClass(getActivity(), EditLaterTaskActivity.class);
        } else if (mSection == Sections.DONE) {
            editTaskIntent.setClass(getActivity(), EditDoneTaskActivity.class);
        } else {
            editTaskIntent.setClass(getActivity(), EditTaskActivity.class);
        }

        // Call activity.
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
        ((TasksActivity) getActivity()).hideActionButtons();

        loadTags();
    }

    private void closeTags() {
        mTagsArea.setVisibility(View.GONE);

        // Show tasks list area with fade animation.
        mListArea.setVisibility(View.VISIBLE);
        mListArea.setAlpha(0f);
        mListArea.animate().alpha(1f).setDuration(Constants.ANIMATION_DURATION_MEDIUM);

        // Show main activity content.
        ((TasksActivity) getActivity()).showActionButtons();

        sSelectedTasks.clear();

        refreshTaskList(false);

        SyncService.getInstance(getActivity()).performSync(true);
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
        final EditText input = new EditText(getActivity());
        input.setHint(getString(R.string.add_tag_dialog_hint));
        input.setInputType(InputType.TYPE_CLASS_TEXT);

        // Display dialog to save new tag.
        new SwipesDialog.Builder(getActivity())
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

                            // Refresh displayed tags.
                            loadTags();
                        }
                    }
                })
                .show();
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
    }

    private void unassignTag(GsonTag tag) {
        // Unassign from all selected tasks.
        for (GsonTask task : sSelectedTasks) {
            mTasksService.unassignTag(tag.getId(), task.getId());
        }

        // Remove from selected tags.
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

    private void hideFilters() {
        // Hide tag filters.
        mFiltersTagsContainer.setVisibility(View.GONE);
        mFiltersSearchContainer.setVisibility(View.VISIBLE);
    }

    private View.OnClickListener mShowTagsFilterListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            mFiltersSearchContainer.setVisibility(View.GONE);

            // Show tag filters with fade animation.
            mFiltersTagsContainer.setVisibility(View.VISIBLE);
            mFiltersTagsContainer.setAlpha(0f);
            mFiltersTagsContainer.animate().alpha(1f).setDuration(Constants.ANIMATION_DURATION_MEDIUM);

            loadFilterTags();
        }
    };

    private View.OnClickListener mCloseTagsFilterListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            mFiltersTagsContainer.setVisibility(View.GONE);

            // Show search view with fade animation.
            mFiltersSearchContainer.setVisibility(View.VISIBLE);
            mFiltersSearchContainer.setAlpha(0f);
            mFiltersSearchContainer.animate().alpha(1f).setDuration(Constants.ANIMATION_DURATION_MEDIUM);

            refreshTaskList(false);
        }
    };

    private void loadFilterTags() {
        List<GsonTag> tags = mTasksService.loadAllAssignedTags();
        mSelectedFilterTags = new ArrayList<Long>();

        mFiltersTagsArea.removeAllViews();
        mFiltersTagsArea.setVisibility(View.VISIBLE);
        mFiltersEmptyTags.setVisibility(View.GONE);

        // For each tag, add a checkbox as child view.
        for (GsonTag tag : tags) {
            int resource = ThemeUtils.isLightTheme(getActivity()) ? R.layout.tag_box_light : R.layout.tag_box_dark;
            CheckBox tagBox = (CheckBox) getActivity().getLayoutInflater().inflate(resource, null);
            tagBox.setText(tag.getTitle());
            tagBox.setId(tag.getId().intValue());

            // Set listener to apply filter.
            tagBox.setOnClickListener(mFilterTagListener);

            // Add child view.
            mFiltersTagsArea.addView(tagBox);
        }

        // If the list is empty, show empty view.
        if (tags.isEmpty()) {
            mFiltersTagsArea.setVisibility(View.GONE);
            mFiltersEmptyTags.setVisibility(View.VISIBLE);

            int hintColor = ThemeUtils.isLightTheme(getActivity()) ? R.color.light_hint : R.color.dark_hint;
            mFiltersEmptyTags.setTextColor(getResources().getColor(hintColor));
        }
    }

    private View.OnClickListener mFilterTagListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            GsonTag selectedTag = mTasksService.loadTag((long) view.getId());

            // Add or remove tag from selected filters.
            if (isFilterTagSelected(selectedTag.getId())) {
                removeSelectedFilterTag(selectedTag.getId());
            } else {
                mSelectedFilterTags.add(selectedTag.getId());
            }

            // Filter by tags or clear results.
            if (!mSelectedFilterTags.isEmpty()) {
                filterByTags();
            } else {
                refreshTaskList(false);
            }
        }
    };

    private boolean isFilterTagSelected(Long selectedTagId) {
        // Check if tag is in the selected filters.
        for (Long tagId : mSelectedFilterTags) {
            if (tagId.equals(selectedTagId)) {
                return true;
            }
        }
        return false;
    }

    private void removeSelectedFilterTag(Long selectedTagId) {
        // Find and remove filter from the list of selected.
        List<Long> selected = new ArrayList<Long>(mSelectedFilterTags);
        for (Long tagId : selected) {
            if (tagId.equals(selectedTagId)) {
                mSelectedFilterTags.remove(tagId);
            }
        }
    }

    private void filterByTags() {
        // Load tasks for each selected tag.
        List<GsonTask> filteredTasks = new ArrayList<GsonTask>();
        for (Long taskId : mSelectedFilterTags) {
            filteredTasks.addAll(mTasksService.loadTasksForTag(taskId, mSection));
        }

        // Refresh list with filtered tasks.
        mListView.setContentList(filteredTasks);
        mAdapter.update(filteredTasks, false);
    }

    private View.OnFocusChangeListener mSearchFocusListener = new View.OnFocusChangeListener() {
        @Override
        public void onFocusChange(View view, boolean hasFocus) {
            if (hasFocus) {
                // Show close search button.
                mCloseSearchButton.setVisibility(View.VISIBLE);
                mFiltersTagsButton.setVisibility(View.GONE);
            }
        }
    };

    private View.OnClickListener mSearchCloseListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            // Hide close search button and clear search query.
            mCloseSearchButton.setVisibility(View.GONE);
            mFiltersTagsButton.setVisibility(View.VISIBLE);
            mSearchEditText.getText().clear();

            refreshTaskList(false);
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
            String query = mSearchEditText.getText().toString().toLowerCase();
            List<GsonTask> results = mTasksService.searchTasks(query, mSection);

            // Refresh list with results.
            if (!results.isEmpty()) {
                mListView.setContentList(results);
                mAdapter.update(results, false);
            }
        }
    };

    private TextView.OnEditorActionListener mSearchDoneListener =
            new TextView.OnEditorActionListener() {
                @Override
                public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                    if (actionId == EditorInfo.IME_ACTION_DONE) {
                        // If the action is a key-up event on the return key, close keyboard.
                        hideKeyboard();
                    }
                    return true;
                }
            };

    private KeyboardBackListener mKeyboardBackListener = new KeyboardBackListener() {
        @Override
        public void onKeyboardBackPressed() {
            // Back button has been pressed, close keyboard.
            hideKeyboard();
        }
    };

    private void hideKeyboard() {
        InputMethodManager inputManager = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        inputManager.toggleSoftInput(0, 0);

        // Remove focus from text views by focusing on list area.
        mListArea.requestFocus();
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

        content += "\n" + getString(R.string.share_message_footer);

        Intent inviteIntent = new Intent(Intent.ACTION_SEND);
        inviteIntent.setType("text/html");
        inviteIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, getString(R.string.share_message_subject));
        inviteIntent.putExtra(android.content.Intent.EXTRA_TEXT, content);

        startActivity(Intent.createChooser(inviteIntent, getString(R.string.share_chooser_title)));
    }

}
