package com.swipesapp.android.ui.fragments;

import android.app.ListFragment;
import android.app.TimePickerDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.ListView;

import com.fortysevendeg.swipelistview.BaseSwipeListViewListener;
import com.fortysevendeg.swipelistview.DynamicListView;
import com.fortysevendeg.swipelistview.SwipeListView;
import com.negusoft.holoaccent.dialog.AccentTimePickerDialog;
import com.swipesapp.android.R;
import com.swipesapp.android.sync.gson.GsonTask;
import com.swipesapp.android.sync.service.TasksService;
import com.swipesapp.android.ui.adapter.TasksListAdapter;
import com.swipesapp.android.ui.listener.ListContentsListener;
import com.swipesapp.android.util.ThemeUtils;
import com.swipesapp.android.values.Sections;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;

/**
 * Fragment for the list of tasks.
 */
public class TasksListFragment extends ListFragment {

    /**
     * The fragment argument representing the section number for this fragment.
     */
    private static final String ARG_SECTION_NUMBER = "section_number";

    /**
     * Current section loaded.
     */
    private Sections mCurrentSection;

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

    @InjectView(android.R.id.empty)
    ViewStub mViewStub;

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

        mTasksService = TasksService.getInstance(getActivity().getApplicationContext());

        int currentSectionNumber = args.getInt(ARG_SECTION_NUMBER, Sections.FOCUS.getSectionNumber());
        mCurrentSection = Sections.getSectionByNumber(currentSectionNumber);

        View rootView = inflater.inflate(R.layout.fragment_tasks_list, container, false);
        ButterKnife.inject(this, rootView);

        // Setup view for current section.
        switch (mCurrentSection) {
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

        return rootView;
    }

    @Override
    public void onDestroyView() {
        ButterKnife.reset(this);
        super.onDestroyView();
    }

    @Override
    public void onResume() {
        updateTaskList();

        IntentFilter filter = new IntentFilter();
        filter.addAction(TasksService.ACTION_TASKS_CHANGED);
        getActivity().registerReceiver(mTasksReceiver, filter);

        super.onResume();
    }

    @Override
    public void onPause() {
        getActivity().unregisterReceiver(mTasksReceiver);

        super.onPause();
    }

    private void setupLaterView(View rootView) {
        // Load tasks.
        List<GsonTask> laterTasks = mTasksService.loadScheduledTasks();

        // Initialize adapter.
        mLaterAdapter = new TasksListAdapter(getActivity(), R.layout.swipeable_cell, laterTasks, mCurrentSection);

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
        mFocusAdapter = new TasksListAdapter(getActivity(), R.layout.swipeable_cell, focusTasks, mCurrentSection);

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
        mDoneAdapter = new TasksListAdapter(getActivity(), R.layout.swipeable_cell, doneTasks, mCurrentSection);

        // Set contents listener.
        if (getActivity() instanceof ListContentsListener) {
            mDoneAdapter.setListContentsListener((ListContentsListener) getActivity());
        }

        // Configure list view.
        mDoneListView = (DynamicListView) rootView.findViewById(android.R.id.list);
        configureDoneListView(mDoneAdapter);

        // Setup empty view.
        mViewStub.setLayoutResource(R.layout.tasks_done_empty_view);
    }

    private void configureLaterListView(TasksListAdapter adapter) {
        mLaterListView.setContentList(adapter.getData());
        mLaterListView.setAdapter(adapter);
        mLaterListView.setSwipeListViewListener(mSwipeListener);
        mLaterListView.setBackgroundColor(ThemeUtils.getCurrentThemeBackgroundColor(getActivity()));
        mLaterListView.setLongSwipeEnabled(true);
        mLaterListView.setSwipeBackgroundColors(ThemeUtils.getSectionColor(Sections.FOCUS, getActivity()), ThemeUtils.getSectionColor(Sections.LATER, getActivity()), ThemeUtils.getCurrentThemeBackgroundColor(getActivity()));
        mLaterListView.setLongSwipeBackgroundColors(ThemeUtils.getSectionColor(Sections.DONE, getActivity()), 0);
        mLaterListView.setBackIconText(R.string.focus_full, R.string.later_full);
        mLaterListView.setLongSwipeBackIconText(R.string.done_full, 0);
        mLaterListView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        mLaterListView.setSwipeMode(SwipeListView.SWIPE_MODE_BOTH);
        mLaterListView.setLongSwipeMode(SwipeListView.LONG_SWIPE_MODE_RIGHT);
        mLaterListView.setSwipeActionRight(SwipeListView.SWIPE_ACTION_DISMISS);
        mLaterListView.setSwipeActionLeft(SwipeListView.SWIPE_ACTION_REVEAL);
        mLaterListView.setLongSwipeActionRight(SwipeListView.LONG_SWIPE_ACTION_DISMISS);
    }

    private void configureFocusListView(TasksListAdapter adapter) {
        mFocusListView.setContentList(adapter.getData());
        mFocusListView.setAdapter(adapter);
        mFocusListView.setSwipeListViewListener(mSwipeListener);
        mFocusListView.setBackgroundColor(ThemeUtils.getCurrentThemeBackgroundColor(getActivity()));
        mFocusListView.setSwipeBackgroundColors(ThemeUtils.getSectionColor(Sections.DONE, getActivity()), ThemeUtils.getSectionColor(Sections.LATER, getActivity()), ThemeUtils.getCurrentThemeBackgroundColor(getActivity()));
        mFocusListView.setBackIconText(R.string.done_full, R.string.later_full);
        mFocusListView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        mFocusListView.setSwipeMode(SwipeListView.SWIPE_MODE_BOTH);
        mFocusListView.setSwipeActionRight(SwipeListView.SWIPE_ACTION_DISMISS);
        mFocusListView.setSwipeActionLeft(SwipeListView.SWIPE_ACTION_REVEAL);
    }

    private void configureDoneListView(TasksListAdapter adapter) {
        mDoneListView.setContentList(adapter.getData());
        mDoneListView.setAdapter(adapter);
        mDoneListView.setSwipeListViewListener(mSwipeListener);
        mDoneListView.setBackgroundColor(ThemeUtils.getCurrentThemeBackgroundColor(getActivity()));
        mDoneListView.setLongSwipeEnabled(true);
        mDoneListView.setSwipeBackgroundColors(0, ThemeUtils.getSectionColor(Sections.FOCUS, getActivity()), ThemeUtils.getCurrentThemeBackgroundColor(getActivity()));
        mDoneListView.setLongSwipeBackgroundColors(0, ThemeUtils.getSectionColor(Sections.LATER, getActivity()));
        mDoneListView.setBackIconText(0, R.string.focus_full);
        mDoneListView.setLongSwipeBackIconText(0, R.string.later_full);
        mDoneListView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        mDoneListView.setSwipeMode(SwipeListView.SWIPE_MODE_LEFT);
        mDoneListView.setLongSwipeMode(SwipeListView.LONG_SWIPE_MODE_LEFT);
        mDoneListView.setSwipeActionRight(SwipeListView.SWIPE_ACTION_NONE);
        mDoneListView.setSwipeActionLeft(SwipeListView.SWIPE_ACTION_DISMISS);
        mDoneListView.setLongSwipeActionLeft(SwipeListView.LONG_SWIPE_ACTION_REVEAL);
    }

    // TODO: Find out why this is not working.
    private void updateTaskList() {
        // Update adapter with new data.
        switch (mCurrentSection) {
            case LATER:
                mLaterAdapter.update(mTasksService.loadFocusedTasks());
                break;
            case FOCUS:
                mFocusAdapter.update(mTasksService.loadFocusedTasks());
                break;
            case DONE:
                mDoneAdapter.update(mTasksService.loadFocusedTasks());
                break;
        }
    }

    private GsonTask getTask(int position) {
        switch (mCurrentSection) {
            case LATER:
                return mLaterAdapter.getData().get(position);
            case FOCUS:
                return mFocusAdapter.getData().get(position);
            case DONE:
                return mDoneAdapter.getData().get(position);
        }
        return null;
    }

    // TODO: Find out why onReceive is being called twice.
    public BroadcastReceiver mTasksReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
//            updateTaskList();
        }
    };

    private BaseSwipeListViewListener mSwipeListener = new BaseSwipeListViewListener() {
        @Override
        public void onFinishedSwipeRight(int position) {
            switch (mCurrentSection) {
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
            switch (mCurrentSection) {
                case LATER:
                    // TODO: Call the real snooze flow.
                    fakeSnoozeTask(getTask(position));
                    break;
                case FOCUS:
                    // TODO: Call the real snooze flow.
                    fakeSnoozeTask(getTask(position));
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
            switch (mCurrentSection) {
                case LATER:
                    // Move task from Later to Done.
                    getTask(position).setCompletionDate(new Date());
                    mTasksService.saveTask(getTask(position));
                    break;
            }
        }

        @Override
        public void onFinishedLongSwipeLeft(int position) {
            switch (mCurrentSection) {
                case DONE:
                    // TODO: Call the real snooze flow.
                    fakeSnoozeTask(getTask(position));
                    break;
            }
        }
    };

    private void fakeSnoozeTask(final GsonTask task) {
        // Create time picker listener.
        TimePickerDialog.OnTimeSetListener listener = new TimePickerDialog.OnTimeSetListener() {
            @Override
            public void onTimeSet(android.widget.TimePicker timePicker, int i, int i1) {
                // Set new schedule date.
                Calendar calendar = Calendar.getInstance();
                calendar.set(Calendar.HOUR_OF_DAY, timePicker.getCurrentHour());
                calendar.set(Calendar.MINUTE, timePicker.getCurrentMinute());
                Date schedule = calendar.getTime();

                // Save task changes.
                task.setSchedule(schedule);
                mTasksService.saveTask(task);
            }
        };

        // Get current hour and minutes.
        Calendar calendar = Calendar.getInstance();
        final int currentHour = calendar.get(Calendar.HOUR_OF_DAY);
        final int currentMinute = calendar.get(Calendar.MINUTE);

        // Show time picker dialog.
        AccentTimePickerDialog dialog = new AccentTimePickerDialog(getActivity(), listener, currentHour, currentMinute, DateFormat.is24HourFormat(getActivity()));
        dialog.setTitle("Snooze until");
        dialog.show();
    }

}
