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
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ListView;

import com.fortysevendeg.swipelistview.BaseSwipeListViewListener;
import com.fortysevendeg.swipelistview.DynamicListView;
import com.fortysevendeg.swipelistview.SwipeListView;
import com.negusoft.holoaccent.dialog.AccentAlertDialog;
import com.swipesapp.android.R;
import com.swipesapp.android.sync.gson.GsonTask;
import com.swipesapp.android.sync.service.TasksService;
import com.swipesapp.android.ui.activity.EditTaskActivity;
import com.swipesapp.android.ui.activity.SnoozeActivity;
import com.swipesapp.android.ui.activity.TasksActivity;
import com.swipesapp.android.ui.adapter.TasksListAdapter;
import com.swipesapp.android.ui.listener.ListContentsListener;
import com.swipesapp.android.ui.view.TransparentButton;
import com.swipesapp.android.util.Constants;
import com.swipesapp.android.util.DateUtils;
import com.swipesapp.android.util.ThemeUtils;
import com.swipesapp.android.values.Actions;
import com.swipesapp.android.values.Sections;
import com.swipesapp.android.values.Themes;

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
     * Customized list views to display tasks.
     */
    private DynamicListView mLaterListView;
    private DynamicListView mFocusListView;
    private DynamicListView mDoneListView;

    /**
     * Adapters for each section.
     */
    private TasksListAdapter mLaterAdapter;
    private TasksListAdapter mFocusAdapter;
    private TasksListAdapter mDoneAdapter;

    /**
     * Service to perform tasks operations.
     */
    private TasksService mTasksService;

    /**
     * List of selected tasks.
     */
    private List<GsonTask> mSelectedTasks;

    @InjectView(android.R.id.empty)
    ViewStub mViewStub;

    @InjectView(R.id.footer_view)
    LinearLayout mFooterView;

    @InjectView(R.id.button_show_old)
    TransparentButton mButtonShowOld;

    @InjectView(R.id.button_clear_old)
    TransparentButton mButtonClearOld;

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

        mSelectedTasks = new ArrayList<GsonTask>();

        int sectionNumber = args.getInt(ARG_SECTION_NUMBER, Sections.FOCUS.getSectionNumber());
        mSection = Sections.getSectionByNumber(sectionNumber);

        View rootView = inflater.inflate(R.layout.fragment_tasks_list, container, false);
        ButterKnife.inject(this, rootView);

        // Setup view for current section.
        switch (mSection) {
            case LATER:
                setupLaterView(rootView);
                break;
            case FOCUS:
                setupFocusView(rootView);
                break;
            case DONE:
                setupDoneView(rootView);
                break;
        }

        mTasksRefresher.run();

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
        Sections currentSection = ((TasksActivity) getActivity()).getCurrentSection();
        return mSection == currentSection;
    }

    private void setupLaterView(View rootView) {
        // Load tasks.
        List<GsonTask> laterTasks = mTasksService.loadScheduledTasks();

        // Initialize adapter.
        mLaterAdapter = new TasksListAdapter(getActivity(), R.layout.swipeable_cell, laterTasks, mSection);

        // Set contents listener.
        if (getActivity() instanceof ListContentsListener) {
            mLaterAdapter.setListContentsListener((ListContentsListener) getActivity());
        }

        // Configure list view.
        mLaterListView = (DynamicListView) rootView.findViewById(android.R.id.list);
        configureLaterListView(mLaterAdapter);

        // Setup empty view.
        mViewStub.setLayoutResource(R.layout.tasks_later_empty_view);
    }

    private void setupFocusView(View rootView) {
        // Load tasks.
        List<GsonTask> focusTasks = mTasksService.loadFocusedTasks();

        // Initialize adapter.
        mFocusAdapter = new TasksListAdapter(getActivity(), R.layout.swipeable_cell, focusTasks, mSection);

        // Set contents listener.
        if (getActivity() instanceof ListContentsListener) {
            mFocusAdapter.setListContentsListener((ListContentsListener) getActivity());
        }

        // Configure list view.
        mFocusListView = (DynamicListView) rootView.findViewById(android.R.id.list);
        configureFocusListView(mFocusAdapter);

        // Setup empty view.
        mViewStub.setLayoutResource(R.layout.tasks_focus_empty_view);
    }

    private void setupDoneView(View rootView) {
        // Load tasks.
        List<GsonTask> doneTasks = mTasksService.loadCompletedTasks();

        // Initialize adapter.
        mDoneAdapter = new TasksListAdapter(getActivity(), R.layout.swipeable_cell, doneTasks, mSection);

        // Set contents listener.
        if (getActivity() instanceof ListContentsListener) {
            mDoneAdapter.setListContentsListener((ListContentsListener) getActivity());
        }

        // Configure list view.
        mDoneListView = (DynamicListView) rootView.findViewById(android.R.id.list);
        configureDoneListView(mDoneAdapter);

        // Setup empty view.
        mViewStub.setLayoutResource(R.layout.tasks_done_empty_view);

        // Customize button colors.
        customizeDoneButtons();
    }

    private void customizeDoneButtons() {
        int background = ThemeUtils.getCurrentTheme(getActivity()) == Themes.LIGHT ? R.drawable.transparent_button_selector_light : R.drawable.transparent_button_selector_dark;
        int color = ThemeUtils.getCurrentTheme(getActivity()) == Themes.LIGHT ? R.color.button_text_color_selector_light : R.color.button_text_color_selector_dark;

        mButtonShowOld.setBackgroundResource(background);
        mButtonShowOld.setTextColor(getResources().getColorStateList(color));

        mButtonClearOld.setBackgroundResource(background);
        mButtonClearOld.setTextColor(getResources().getColorStateList(color));
    }

    private void configureLaterListView(TasksListAdapter adapter) {
        // Setup content.
        mLaterListView.setAdapter(adapter);
        mLaterListView.setSwipeListViewListener(mSwipeListener);

        // Setup back view.
        mLaterListView.setBackgroundColor(ThemeUtils.getCurrentThemeBackgroundColor(getActivity()));
        mLaterListView.setLongSwipeEnabled(true);
        mLaterListView.setSwipeBackgroundColors(ThemeUtils.getSectionColor(Sections.FOCUS, getActivity()), ThemeUtils.getSectionColor(Sections.LATER, getActivity()), ThemeUtils.getCurrentThemeBackgroundColor(getActivity()));
        mLaterListView.setLongSwipeBackgroundColors(ThemeUtils.getSectionColor(Sections.DONE, getActivity()), ThemeUtils.getSectionColor(Sections.LATER, getActivity()));
        mLaterListView.setBackIconText(R.string.focus_full, R.string.later_full);
        mLaterListView.setLongSwipeBackIconText(R.string.done_full, R.string.later_full);

        // Setup priority button.
        mLaterListView.setFrontIcon(R.id.button_task_priority);
        mLaterListView.setFrontIconBackgrounds(R.drawable.focus_circle_selector, R.drawable.later_circle_selector, R.drawable.later_circle_selector);
        mLaterListView.setFrontIconLongSwipeBackgrounds(R.drawable.done_circle_selector, R.drawable.later_circle_selector);

        // Setup actions.
        mLaterListView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        mLaterListView.setSwipeMode(SwipeListView.SWIPE_MODE_BOTH);
        mLaterListView.setLongSwipeMode(SwipeListView.LONG_SWIPE_MODE_BOTH);
        mLaterListView.setSwipeActionRight(SwipeListView.SWIPE_ACTION_DISMISS);
        mLaterListView.setSwipeActionLeft(SwipeListView.SWIPE_ACTION_REVEAL);
        mLaterListView.setLongSwipeActionRight(SwipeListView.LONG_SWIPE_ACTION_DISMISS);
        mLaterListView.setLongSwipeActionLeft(SwipeListView.LONG_SWIPE_ACTION_REVEAL);
    }

    private void configureFocusListView(TasksListAdapter adapter) {
        // Setup content.
        mFocusListView.setContentList(adapter.getData());
        mFocusListView.setAdapter(adapter);
        mFocusListView.setSwipeListViewListener(mSwipeListener);
        mFocusListView.setListOrderListener(this);

        // Setup back view.
        mFocusListView.setBackgroundColor(ThemeUtils.getCurrentThemeBackgroundColor(getActivity()));
        mFocusListView.setSwipeBackgroundColors(ThemeUtils.getSectionColor(Sections.DONE, getActivity()), ThemeUtils.getSectionColor(Sections.LATER, getActivity()), ThemeUtils.getCurrentThemeBackgroundColor(getActivity()));
        mFocusListView.setBackIconText(R.string.done_full, R.string.later_full);

        // Setup priority button.
        mFocusListView.setFrontIcon(R.id.button_task_priority);
        mFocusListView.setFrontIconBackgrounds(R.drawable.done_circle_selector, R.drawable.later_circle_selector, R.drawable.focus_circle_selector);

        // Setup actions.
        mFocusListView.setDragAndDropEnabled(true);
        mFocusListView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        mFocusListView.setSwipeMode(SwipeListView.SWIPE_MODE_BOTH);
        mFocusListView.setSwipeActionRight(SwipeListView.SWIPE_ACTION_DISMISS);
        mFocusListView.setSwipeActionLeft(SwipeListView.SWIPE_ACTION_REVEAL);
    }

    private void configureDoneListView(TasksListAdapter adapter) {
        // Setup content.
        mDoneListView.setAdapter(adapter);
        mDoneListView.setSwipeListViewListener(mSwipeListener);

        // Setup back view.
        mDoneListView.setBackgroundColor(ThemeUtils.getCurrentThemeBackgroundColor(getActivity()));
        mDoneListView.setLongSwipeEnabled(true);
        mDoneListView.setSwipeBackgroundColors(0, ThemeUtils.getSectionColor(Sections.FOCUS, getActivity()), ThemeUtils.getCurrentThemeBackgroundColor(getActivity()));
        mDoneListView.setLongSwipeBackgroundColors(0, ThemeUtils.getSectionColor(Sections.LATER, getActivity()));
        mDoneListView.setBackIconText(0, R.string.focus_full);
        mDoneListView.setLongSwipeBackIconText(0, R.string.later_full);

        // Setup priority button.
        mDoneListView.setFrontIcon(R.id.button_task_priority);
        mDoneListView.setFrontIconBackgrounds(0, R.drawable.focus_circle_selector, R.drawable.done_circle_selector);
        mDoneListView.setFrontIconLongSwipeBackgrounds(0, R.drawable.later_circle_selector);

        // Setup actions.
        mDoneListView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        mDoneListView.setSwipeMode(SwipeListView.SWIPE_MODE_LEFT);
        mDoneListView.setLongSwipeMode(SwipeListView.LONG_SWIPE_MODE_LEFT);
        mDoneListView.setSwipeActionRight(SwipeListView.SWIPE_ACTION_NONE);
        mDoneListView.setSwipeActionLeft(SwipeListView.SWIPE_ACTION_DISMISS);
        mDoneListView.setLongSwipeActionLeft(SwipeListView.LONG_SWIPE_ACTION_REVEAL);
    }

    private void refreshTaskList(boolean animateRefresh) {
        List<GsonTask> tasks;

        // Update adapter with new data.
        switch (mSection) {
            case LATER:
                tasks = mTasksService.loadScheduledTasks();
                mLaterAdapter.update(tasks, animateRefresh);
                break;
            case FOCUS:
                tasks = mTasksService.loadFocusedTasks();
                mFocusListView.setContentList(tasks);
                mFocusAdapter.update(tasks, animateRefresh);
                break;
            case DONE:
                tasks = mTasksService.loadCompletedTasks();
                mDoneAdapter.update(tasks, animateRefresh);

                // Hide or show buttons.
                handleDoneButtons();
                break;
        }
    }

    private GsonTask getTask(int position) {
        switch (mSection) {
            case LATER:
                return mLaterAdapter.getData().get(position);
            case FOCUS:
                return mFocusAdapter.getData().get(position);
            case DONE:
                return mDoneAdapter.getData().get(position);
        }
        return null;
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
                    if (mSection == Sections.DONE) mDoneAdapter.hideOld();

                    // Clear selected tasks and perform refresh.
                    mSelectedTasks.clear();
                    refreshTaskList(false);
                } else if (intent.getAction().equals(Actions.EDIT_TASK)) {
                    // Call task edit activity, passing the tempId of the selected task as parameter.
                    Intent editTaskIntent = new Intent(getActivity(), EditTaskActivity.class);
                    editTaskIntent.putExtra(Constants.EXTRA_TASK_TEMP_ID, mSelectedTasks.get(0).getTempId());
                    startActivityForResult(editTaskIntent, Constants.EDIT_TASK_REQUEST_CODE);

                    // Clear selected tasks.
                    mSelectedTasks.clear();
                } else if (intent.getAction().equals(Actions.ASSIGN_TAGS)) {
                    // TODO: Display tag selection screen.
                } else if (intent.getAction().equals(Actions.DELETE_TASKS)) {
                    // Delete tasks.
                    deleteSelectedTasks();
                } else if (intent.getAction().equals(Actions.SHARE_TASKS)) {
                    // TODO: Send intent to share tasks by email.
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
                    getTask(position).setSchedule(new Date());
                    mTasksService.saveTask(getTask(position));
                    break;
                case FOCUS:
                    // Move task from Focus to Done.
                    getTask(position).setCompletionDate(new Date());
                    mTasksService.saveTask(getTask(position));
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
                    getTask(position).setCompletionDate(null);
                    mTasksService.saveTask(getTask(position));
                    break;
            }
        }

        @Override
        public void onFinishedLongSwipeRight(int position) {
            switch (mSection) {
                case LATER:
                    // Move task from Later to Done.
                    Date currentDate = new Date();
                    getTask(position).setCompletionDate(currentDate);
                    getTask(position).setSchedule(currentDate);
                    mTasksService.saveTask(getTask(position));
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

            mTasksService.saveTask(task);
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
            mTasksService.saveTask(task);
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
            Date completionDate = oldestTask != null ? oldestTask.getCompletionDate() : null;

            // Only display buttons in the done section and when the oldest completed task is older than today.
            if (mSection == Sections.DONE && !mDoneAdapter.isShowingOld() && DateUtils.isOlderThanToday(completionDate)) {
                mFooterView.setVisibility(View.VISIBLE);
                mFooterView.setAlpha(1f);
            } else {
                mFooterView.setVisibility(View.GONE);
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
                    }
                })
                .setNegativeButton(getString(R.string.dialog_no), null)
                .create()
                .show();
    }

    @OnClick(R.id.button_show_old)
    protected void showOldTasks() {
        // Animate footer view.
        mFooterView.animate().alpha(0f).setDuration(Constants.ANIMATION_DURATION_SHORT).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                // Hide buttons.
                mFooterView.setVisibility(View.GONE);
                // Show old tasks.
                mDoneAdapter.showOld(mTasksService.loadCompletedTasks());
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
                            if (DateUtils.isOlderThanToday(task.getCompletionDate())) {
                                // Add to the removal list.
                                oldTasks.add(task);
                            }
                        }

                        // Proceed with delete.
                        mTasksService.deleteTasks(oldTasks);
                    }
                })
                .setNegativeButton(getString(R.string.clear_old_dialog_no), null)
                .create()
                .show();
    }

    private void openSnoozeSelector(GsonTask task) {
        Intent intent = new Intent(getActivity(), SnoozeActivity.class);
        intent.putExtra(Constants.EXTRA_TASK_TEMP_ID, task.getTempId());
        startActivityForResult(intent, Constants.SNOOZE_REQUEST_CODE);
    }

    // TODO: Remove this when scheduler service is ready.
    Runnable mTasksRefresher = new Runnable() {
        @Override
        public void run() {
            refreshTaskList(false);
            new Handler().postDelayed(mTasksRefresher, 60000);
        }
    };

}
