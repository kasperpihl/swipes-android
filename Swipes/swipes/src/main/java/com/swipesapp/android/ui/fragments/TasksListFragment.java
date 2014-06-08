package com.swipesapp.android.ui.fragments;

import android.app.ListFragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.ListView;
import android.widget.Toast;

import com.fortysevendeg.swipelistview.BaseSwipeListViewListener;
import com.fortysevendeg.swipelistview.DynamicListView;
import com.fortysevendeg.swipelistview.SwipeListView;
import com.swipesapp.android.R;
import com.swipesapp.android.sync.gson.GsonTask;
import com.swipesapp.android.sync.service.TasksService;
import com.swipesapp.android.ui.adapter.TasksListAdapter;
import com.swipesapp.android.ui.listener.ListContentsListener;
import com.swipesapp.android.util.ThemeUtils;
import com.swipesapp.android.values.Sections;

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
        mDoneListView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        mDoneListView.setSwipeMode(SwipeListView.SWIPE_MODE_LEFT);
        mDoneListView.setLongSwipeMode(SwipeListView.LONG_SWIPE_MODE_LEFT);
        mDoneListView.setSwipeActionRight(SwipeListView.SWIPE_ACTION_NONE);
        mDoneListView.setSwipeActionLeft(SwipeListView.SWIPE_ACTION_DISMISS);
        mDoneListView.setLongSwipeActionLeft(SwipeListView.LONG_SWIPE_ACTION_REVEAL);
    }

    private BaseSwipeListViewListener mSwipeListener = new BaseSwipeListViewListener() {
        @Override
        public void onFinishedSwipeRight(int position) {
            switch (mCurrentSection) {
                case LATER:
                    // TODO: Move task from Later to Focus.
                    Toast.makeText(getActivity(), "TODO: Move task to Focus.", Toast.LENGTH_SHORT).show();
                    break;
                case FOCUS:
                    // TODO: Move task from Focus to Done.
                    Toast.makeText(getActivity(), "TODO: Move task to Done.", Toast.LENGTH_SHORT).show();
                    break;
            }
        }

        @Override
        public void onFinishedSwipeLeft(int position) {
            switch (mCurrentSection) {
                case LATER:
                    // TODO: Reschedule task.
                    Toast.makeText(getActivity(), "TODO: Reschedule task.", Toast.LENGTH_SHORT).show();
                    break;
                case FOCUS:
                    // TODO: Move task from Focus to Later.
                    Toast.makeText(getActivity(), "TODO: Move task to Later.", Toast.LENGTH_SHORT).show();
                    break;
                case DONE:
                    // TODO: Move task from Done to Focus.
                    Toast.makeText(getActivity(), "TODO: Move task to Focus.", Toast.LENGTH_SHORT).show();
                    break;
            }
        }

        @Override
        public void onFinishedLongSwipeRight(int position) {
            switch (mCurrentSection) {
                case LATER:
                    // TODO: Move task from Later to Done.
                    Toast.makeText(getActivity(), "TODO: Move task to Done.", Toast.LENGTH_SHORT).show();
                    break;
            }
        }

        @Override
        public void onFinishedLongSwipeLeft(int position) {
            switch (mCurrentSection) {
                case DONE:
                    // TODO: Move task from Done to Later.
                    Toast.makeText(getActivity(), "TODO: Move task to Later.", Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };

}
