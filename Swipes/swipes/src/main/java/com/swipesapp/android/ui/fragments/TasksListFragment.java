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
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.view.ViewTreeObserver;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.fortysevendeg.swipelistview.BaseSwipeListViewListener;
import com.fortysevendeg.swipelistview.DynamicListView;
import com.fortysevendeg.swipelistview.SwipeListView;
import com.negusoft.holoaccent.dialog.AccentAlertDialog;
import com.swipesapp.android.R;
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
import com.swipesapp.android.ui.view.FlowLayout;
import com.swipesapp.android.ui.view.SwipesButton;
import com.swipesapp.android.ui.view.TransparentButton;
import com.swipesapp.android.util.Constants;
import com.swipesapp.android.util.DateUtils;
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
public class TasksListFragment extends ListFragment implements DynamicListView.ListOrderListener {

    /**
     * The fragment argument representing the section number for this fragment.
     */
    private static final String ARG_SECTION_NUMBER = "section_number";

    /**
     * Section the fragment belongs to.
     */
    private Sections mSection;

    /**
     * Section color for this fragment.
     */
    private int mSectionColor;

    /**
     * Customized list view to display tasks.
     */
    private DynamicListView mListView;

    /**
     * List view height, used for UI calculations.
     */
    private int mListViewHeight;

    /**
     * Adapter for tasks.
     */
    private TasksListAdapter mAdapter;

    /**
     * Service to perform tasks operations.
     */
    private TasksService mTasksService;

    /**
     * Handler for repeated tasks.
     */
    private RepeatHandler mRepeatHandler;

    /**
     * Selected tasks, tags and filters.
     */
    private List<GsonTask> mSelectedTasks;
    private List<GsonTag> mAssignedTags;
    private List<Long> mSelectedFilterTags;

    /**
     * Filters containers.
     */
    private LinearLayout mFiltersContainer;
    private LinearLayout mFiltersTagsContainer;
    private LinearLayout mFiltersSearchContainer;
    private FlowLayout mFiltersTagsArea;

    /**
     * Filters views.
     */
    private ActionEditText mSearchEditText;
    private SwipesButton mFiltersTagsButton;
    private TextView mFiltersEmptyTags;
    private SwipesButton mCloseSearchButton;

    @InjectView(android.R.id.empty)
    ViewStub mViewStub;

    @InjectView(R.id.header_view)
    LinearLayout mHeaderView;

    @InjectView(R.id.button_show_old)
    TransparentButton mButtonShowOld;

    @InjectView(R.id.button_clear_old)
    TransparentButton mButtonClearOld;

    @InjectView(R.id.list_area)
    LinearLayout mListArea;

    @InjectView(R.id.tags_area)
    LinearLayout mTagsArea;

    @InjectView(R.id.tags_container)
    FlowLayout mTaskTagsContainer;

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

        mSelectedTasks = new ArrayList<GsonTask>();

        int sectionNumber = args.getInt(ARG_SECTION_NUMBER, Sections.FOCUS.getSectionNumber());
        mSection = Sections.getSectionByNumber(sectionNumber);

        mSectionColor = ThemeUtils.getSectionColor(mSection, getActivity());

        View rootView = inflater.inflate(R.layout.fragment_tasks_list, container, false);
        ButterKnife.inject(this, rootView);

        // Setup view for current section.
        switch (mSection) {
            case LATER:
                setupView(rootView, mTasksService.loadScheduledTasks(), R.layout.tasks_later_empty_view);
                configureLaterView(mAdapter);
                break;
            case FOCUS:
                setupView(rootView, mTasksService.loadFocusedTasks(), R.layout.tasks_focus_empty_view);
                configureFocusView(mAdapter);
                break;
            case DONE:
                setupView(rootView, mTasksService.loadCompletedTasks(), R.layout.tasks_done_empty_view);
                configureDoneView(mAdapter);
                customizeDoneButtons();
                break;
        }

        measureListView(mListView);

        hideFilters();

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
        filter.addAction(Actions.TASKS_CHANGED);
        filter.addAction(Actions.TAB_CHANGED);
        filter.addAction(Actions.EDIT_TASK);
        filter.addAction(Actions.ASSIGN_TAGS);
        filter.addAction(Actions.DELETE_TASKS);
        filter.addAction(Actions.SHARE_TASKS);
        filter.addAction(Actions.BACK_PRESSED);

        getActivity().registerReceiver(mTasksReceiver, filter);

        refreshTaskList(false);

        super.onResume();
    }

    @Override
    public void onPause() {
        getActivity().unregisterReceiver(mTasksReceiver);

        super.onPause();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Check if the request code the one from snooze task.
        if (requestCode == Constants.SNOOZE_REQUEST_CODE) {
            switch (resultCode) {
                case Activity.RESULT_OK:
                    // Task has been snoozed. Refresh tasks list.
                    refreshTaskList(false);
                    break;
                case Activity.RESULT_CANCELED:
                    // Snooze has been canceled. Refresh tasks with animation.
                    refreshTaskList(true);
                    break;
            }
        } else if (requestCode == Constants.EDIT_TASK_REQUEST_CODE) {
            // Refresh tasks after editing.
            refreshTaskList(false);
        }
    }

    private boolean isCurrentSection() {
        // Retrieve current section being displayed and compare with this fragment's section.
        return mSection == TasksActivity.getCurrentSection();
    }

    private void setupView(View rootView, List<GsonTask> tasks, int emptyView) {
        // Initialize adapter.
        mAdapter = new TasksListAdapter(getActivity(), R.layout.swipeable_cell, tasks, mSection);

        // Set contents listener.
        if (getActivity() instanceof ListContentsListener) {
            mAdapter.setListContentsListener((ListContentsListener) getActivity());
        }

        // Initialize list view.
        mListView = (DynamicListView) rootView.findViewById(android.R.id.list);

        // Setup filters.
        setupFiltersArea();

        // Setup empty view.
        mViewStub.setLayoutResource(emptyView);
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

    private void customizeDoneButtons() {
        mButtonShowOld.setBackgroundResource(R.drawable.transparent_button_selector_dark);
        mButtonShowOld.setTextColor(getResources().getColorStateList(R.color.button_text_color_selector_dark));

        mButtonClearOld.setBackgroundResource(R.drawable.transparent_button_selector_dark);
        mButtonClearOld.setTextColor(getResources().getColorStateList(R.color.button_text_color_selector_dark));
    }

    private void configureLaterView(TasksListAdapter adapter) {
        // Setup content.
        mListView.setAdapter(adapter);
        mListView.setSwipeListViewListener(mSwipeListener);

        // Setup back view.
        mListView.setLongSwipeEnabled(true);
        mListView.setSwipeBackgroundColors(ThemeUtils.getSectionColor(Sections.FOCUS, getActivity()), ThemeUtils.getSectionColor(Sections.LATER, getActivity()), ThemeUtils.getBackgroundColor(getActivity()));
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
    }

    private void configureFocusView(TasksListAdapter adapter) {
        // Setup content.
        mListView.setContentList(adapter.getData());
        mListView.setAdapter(adapter);
        mListView.setSwipeListViewListener(mSwipeListener);
        mListView.setListOrderListener(this);

        // Setup back view.
        mListView.setSwipeBackgroundColors(ThemeUtils.getSectionColor(Sections.DONE, getActivity()), ThemeUtils.getSectionColor(Sections.LATER, getActivity()), ThemeUtils.getBackgroundColor(getActivity()));
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
    }

    private void configureDoneView(TasksListAdapter adapter) {
        // Setup content.
        mListView.setAdapter(adapter);
        mListView.setSwipeListViewListener(mSwipeListener);

        // Setup back view.
        mListView.setLongSwipeEnabled(true);
        mListView.setSwipeBackgroundColors(0, ThemeUtils.getSectionColor(Sections.FOCUS, getActivity()), ThemeUtils.getBackgroundColor(getActivity()));
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

    private void refreshTaskList(boolean animateRefresh) {
        List<GsonTask> tasks;

        // Update adapter with new data.
        switch (mSection) {
            case LATER:
                tasks = mTasksService.loadScheduledTasks();
                mAdapter.update(tasks, animateRefresh);
                break;
            case FOCUS:
                tasks = mTasksService.loadFocusedTasks();
                mListView.setContentList(tasks);
                mAdapter.update(tasks, animateRefresh);
                break;
            case DONE:
                tasks = mTasksService.loadCompletedTasks();
                mAdapter.update(tasks, animateRefresh);

                // Hide or show buttons.
                handleDoneButtons();
                break;
        }
    }

    private GsonTask getTask(int position) {
        // Reduce position by -1 to account for the header.
        return (GsonTask) mAdapter.getItem(position - 1);
    }

    private BroadcastReceiver mTasksReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Listen to broadcasts intended for this section.
            if (isCurrentSection()) {
                // Filter intent actions.
                if (intent.getAction().equals(Actions.TASKS_CHANGED)) {
                    // Perform refresh.
                    refreshTaskList(false);
                } else if (intent.getAction().equals(Actions.TAB_CHANGED)) {
                    // Hide old tasks in the done section.
                    if (mSection == Sections.DONE) mAdapter.hideOld();

                    // Clear selected tasks and perform refresh.
                    mSelectedTasks.clear();
                    refreshTaskList(false);

                    // Hide search and tags.
                    hideFilters();
                } else if (intent.getAction().equals(Actions.EDIT_TASK)) {
                    // Call task edit activity, passing the tempId of the selected task as parameter.
                    Intent editTaskIntent = new Intent(getActivity(), EditTaskActivity.class);
                    editTaskIntent.putExtra(Constants.EXTRA_TASK_ID, mSelectedTasks.get(0).getId());
                    startActivityForResult(editTaskIntent, Constants.EDIT_TASK_REQUEST_CODE);

                    // Clear selected tasks.
                    mSelectedTasks.clear();
                } else if (intent.getAction().equals(Actions.ASSIGN_TAGS)) {
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
                    mSelectedTasks.clear();
                } else if (intent.getAction().equals(Actions.BACK_PRESSED)) {
                    // Don't close the app when assigning tags.
                    if (mTagsArea.getVisibility() == View.VISIBLE) {
                        closeTags();
                    } else {
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
            View selectedIndicator = view.findViewById(R.id.selected_indicator);

            if (task.isSelected()) {
                // Deselect task.
                task.setSelected(false);
                selectedIndicator.setBackgroundColor(0);
                mSelectedTasks.remove(task);
            } else {
                // Select task.
                task.setSelected(true);
                selectedIndicator.setBackgroundColor(ThemeUtils.getSectionColor(mSection, getActivity()));
                mSelectedTasks.add(task);
            }

            handleEditBar();
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
            mSelectedTasks.clear();
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

    private void handleEditBar() {
        if (!mSelectedTasks.isEmpty()) {
            // Display bar.
            ((TasksActivity) getActivity()).showEditBar(mSelectedTasks.size() > 1);
        } else {
            // Hide bar.
            ((TasksActivity) getActivity()).hideEditBar();
        }
    }

    private void handleDoneButtons() {
        if (isCurrentSection()) {
            // Load date of the oldest completed task.
            List<GsonTask> completedTasks = mTasksService.loadCompletedTasks();
            GsonTask oldestTask = !completedTasks.isEmpty() ? completedTasks.get(completedTasks.size() - 1) : null;
            Date completionDate = oldestTask != null ? oldestTask.getLocalCompletionDate() : null;

            // Only display buttons in the done section and when the oldest completed task is older than today.
            if (mSection == Sections.DONE && !mAdapter.isShowingOld() && DateUtils.isOlderThanToday(completionDate)) {
                mHeaderView.setVisibility(View.VISIBLE);
                mHeaderView.setAlpha(1f);
            } else {
                mHeaderView.setVisibility(View.GONE);
            }
        }
    }

    private void deleteSelectedTasks() {
        // Display confirmation dialog.
        new AccentAlertDialog.Builder(getActivity())
                .setTitle(getResources().getQuantityString(R.plurals.delete_task_dialog_title, mSelectedTasks.size(), mSelectedTasks.size()))
                .setMessage(getResources().getQuantityString(R.plurals.delete_task_dialog_text, mSelectedTasks.size()))
                .setPositiveButton(getString(R.string.dialog_yes), new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int which) {
                        // Proceed with delete.
                        mTasksService.deleteTasks(mSelectedTasks);
                        refreshTaskList(false);
                    }
                })
                .setNegativeButton(getString(R.string.dialog_no), null)
                .create()
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
                mAdapter.showOld(mTasksService.loadCompletedTasks(), mListViewHeight);
            }
        });
    }

    @OnClick(R.id.button_clear_old)
    protected void clearOldTasks() {
        // Display confirmation dialog.
        new AccentAlertDialog.Builder(getActivity())
                .setTitle(getString(R.string.clear_old_dialog_title))
                .setMessage(getString(R.string.clear_old_dialog_text))
                .setPositiveButton(getString(R.string.clear_old_dialog_yes), new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int which) {
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
                    }
                })
                .setNegativeButton(getString(R.string.clear_old_dialog_no), null)
                .create()
                .show();
    }

    private void openSnoozeSelector(GsonTask task) {
        // Call snooze activity.
        Intent intent = new Intent(getActivity(), SnoozeActivity.class);
        intent.putExtra(Constants.EXTRA_TASK_ID, task.getId());
        intent.putExtra(Constants.EXTRA_CALLER_NAME, Constants.CALLER_TASKS_LIST);
        startActivityForResult(intent, Constants.SNOOZE_REQUEST_CODE);

        // Update blurred background and override animation.
        int alphaColor = ThemeUtils.getSnoozeBlurAlphaColor(getActivity());
        ((TasksActivity) getActivity()).updateBlurDrawable(alphaColor);
        getActivity().overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
    }

    private void showTags() {
        // Apply blur to the tags background.
        int alphaColor = ThemeUtils.getTasksBlurAlphaColor(getActivity());
        ((TasksActivity) getActivity()).updateBlurDrawable(alphaColor);
        mTagsArea.setBackgroundDrawable(TasksActivity.getBlurDrawable());

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

        mSelectedTasks.clear();

        refreshTaskList(false);

        SyncService.getInstance(getActivity()).performSync(true);
    }

    @OnClick(R.id.tags_back_button)
    protected void tagsBack() {
        // Close tags area with animation.
        closeTags();
    }

    @OnClick(R.id.tags_area)
    protected void tagsAreaClick() {
        // Close tags area with animation.
        closeTags();
    }

    @OnClick(R.id.tags_add_button)
    protected void addTag() {
        // Create tag title input.
        final EditText input = new EditText(getActivity());
        input.setHint(getString(R.string.add_tag_dialog_hint));
        input.setInputType(InputType.TYPE_CLASS_TEXT);

        // Display dialog to save new tag.
        new AccentAlertDialog.Builder(getActivity())
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
            new AccentAlertDialog.Builder(getActivity())
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
        int assigns = 0;
        // Using a counter, check if the tag is assigned to all selected tasks.
        for (GsonTask task : mSelectedTasks) {
            // Increase counter if tag is already assigned to the task.
            for (GsonTag tag : task.getTags()) {
                if (tag.getId().equals(selectedTag.getId())) {
                    assigns++;
                }
            }
        }
        return assigns == mSelectedTasks.size();
    }

    private void assignTag(GsonTag tag) {
        // Assign to all selected tasks.
        for (GsonTask task : mSelectedTasks) {
            mAssignedTags.add(tag);
            task.setTags(mAssignedTags);
            mTasksService.saveTask(task, true);
        }
    }

    private void unassignTag(GsonTag tag) {
        // Unassign from all selected tasks.
        for (GsonTask task : mSelectedTasks) {
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
        // Scroll list to first position, hiding the search.
        mListView.post(new Runnable() {
            @Override
            public void run() {
                mListView.setSelection(1);
            }
        });

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

            int hintColor = ThemeUtils.isLightTheme(getActivity()) ? R.color.light_text_hint_color : R.color.dark_text_hint_color;
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
        for (GsonTask task : mSelectedTasks) {
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
