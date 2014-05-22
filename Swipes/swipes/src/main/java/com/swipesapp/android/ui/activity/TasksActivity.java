package com.swipesapp.android.ui.activity;

import android.animation.ObjectAnimator;
import android.app.ActionBar;
import android.app.Activity;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import com.astuetz.PagerSlidingTabStrip;
import com.swipesapp.android.R;
import com.swipesapp.android.adapter.SectionsPagerAdapter;
import com.swipesapp.android.ui.listener.ListContentsListener;
import com.swipesapp.android.ui.view.NoSwipeViewPager;
import com.swipesapp.android.ui.view.SwipesButton;
import com.swipesapp.android.utils.Constants;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;

public class TasksActivity extends Activity implements ListContentsListener {
    public static final int FOCUS_FRAGMENT_POSITION = 1;
    public static final int SETTINGS_FRAGMENT_POSITION = 3; //last fragment
    SectionsPagerAdapter mSectionsPagerAdapter;

    @InjectView(R.id.pager)
    NoSwipeViewPager mViewPager;

    @InjectView(R.id.tabs)
    PagerSlidingTabStrip mTabs;

    @InjectView(R.id.tasks_activity_container)
    ViewGroup mActivityMainLayout;

    @InjectView(R.id.button_add_task)
    SwipesButton mButtonAddTask;

    @InjectView(R.id.edit_text_add_task_content)
    EditText mEditTaskAddNewTask;

    private static Typeface sTypeface;

    @OnClick(R.id.button_add_task)
    protected void startAddTaskWorkflow() {
        // go to main fragment
        mViewPager.setCurrentItem(FOCUS_FRAGMENT_POSITION);

        // animate button down off screen
        //TODO: animate
        mButtonAddTask.setVisibility(View.GONE);

        // animate edit text into screen
        // TODO: animate
        mEditTaskAddNewTask.setVisibility(View.VISIBLE);
        mEditTaskAddNewTask.setFocusable(true);
        mEditTaskAddNewTask.setFocusableInTouchMode(true);
        mEditTaskAddNewTask.requestFocus();

        //TODO: blur background
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tasks);
        ButterKnife.inject(this);

        mSectionsPagerAdapter = new SectionsPagerAdapter(getFragmentManager(), this);
        mViewPager.setAdapter(mSectionsPagerAdapter);
        mTabs.setViewPager(mViewPager);
        if (sTypeface == null) {
            sTypeface = Typeface.createFromAsset(getAssets(), Constants.FONT_NAME);
        }
        mTabs.setTypeface(sTypeface, 0);
        int dimension = getResources().getDimensionPixelSize(R.dimen.action_bar_icon_size);
        mTabs.setTextSize(dimension);
        mTabs.setIndicatorColor(getResources().getColor(R.color.light_theme_text));
        mTabs.setTextColor(getResources().getColor(R.color.light_theme_text));
        ViewPager.SimpleOnPageChangeListener simpleOnPageChangeListener = new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                int[] textColors = {R.color.later_accent_color, R.color.focus_accent_color, R.color.done_accent_color, R.color.light_theme_text};
                mTabs.setIndicatorColorResource(textColors[position]);
                mTabs.setTextColorResource(R.color.light_theme_text);
                View v = mTabs.getTabView(position);
                if (v instanceof TextView) {
                    TextView tabTextView = (TextView) v;
                    tabTextView.setTextColor(getResources().getColor(textColors[position]));
                }

                if (position == FOCUS_FRAGMENT_POSITION) {
                    setBackgroundImage();
                } else {
                    clearBackgroundImage();
                }

                if (position == SETTINGS_FRAGMENT_POSITION) {
                    mButtonAddTask.setVisibility(View.GONE);
                } else {
                    mButtonAddTask.setVisibility(View.VISIBLE);
                }
            }
        };
        ViewPager.SimpleOnPageChangeListener listener = simpleOnPageChangeListener;
        mTabs.setOnPageChangeListener(listener);

        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }

        // Default to second item, index starts at zero
        mViewPager.setCurrentItem(FOCUS_FRAGMENT_POSITION);
    }

    private void clearBackgroundImage() {
        mActivityMainLayout.setBackgroundResource(0);
    }

    private void setBackgroundImage() {
        mActivityMainLayout.setBackgroundResource(R.drawable.default_background);
    }

    @Override
    protected void onDestroy() {
        ButterKnife.reset(this);
        super.onDestroy();
    }

    // HACK: this is a workaround to change the background entirely
    @Override
    public void onEmpty() {
        setBackgroundImage();
    }

    // HACK: this is a workaround to change the background entirely
    @Override
    public void onNotEmpty() {
        clearBackgroundImage();
    }
}
