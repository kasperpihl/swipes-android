package com.swipesapp.android.ui.activity;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
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
import com.swipesapp.android.util.Constants;
import com.swipesapp.android.util.ThemeUtils;
import com.swipesapp.android.values.Sections;
import com.swipesapp.android.values.Themes;

import java.lang.ref.WeakReference;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;

public class TasksActivity extends Activity implements ListContentsListener {

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

    private WeakReference<Context> mContext;

    @OnClick(R.id.button_add_task)
    protected void startAddTaskWorkflow() {
        // go to main fragment
        mViewPager.setCurrentItem(Sections.FOCUS.getSectionNumber());

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
        mContext = new WeakReference<Context>(this);

        mSectionsPagerAdapter = new SectionsPagerAdapter(getFragmentManager(), this);
        mViewPager.setAdapter(mSectionsPagerAdapter);
        mTabs.setViewPager(mViewPager);
        if (sTypeface == null) {
            sTypeface = Typeface.createFromAsset(getAssets(), Constants.FONT_NAME);
        }
        mTabs.setTypeface(sTypeface, 0);
        int dimension = getResources().getDimensionPixelSize(R.dimen.action_bar_icon_size);
        mTabs.setTextSize(dimension);
        mTabs.setIndicatorColor(ThemeUtils.getCurrentThemeTextColor(this));
        mTabs.setTextColor(ThemeUtils.getCurrentThemeTextColor(this));
        mTabs.setDividerColor(ThemeUtils.getCurrentThemeDividerColor(this));
        ViewPager.SimpleOnPageChangeListener simpleOnPageChangeListener = new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                int[] textColors = {
                        ThemeUtils.getSectionColor(Sections.LATER, mContext.get()),
                        ThemeUtils.getSectionColor(Sections.FOCUS, mContext.get()),
                        ThemeUtils.getSectionColor(Sections.DONE, mContext.get()),
                        ThemeUtils.getCurrentThemeTextColor(mContext.get())
                };
                mTabs.setIndicatorColor(textColors[position]);
                mTabs.setTextColor(ThemeUtils.getCurrentThemeTextColor(mContext.get()));
                View v = mTabs.getTabView(position);
                if (v instanceof TextView) {
                    TextView tabTextView = (TextView) v;
                    tabTextView.setTextColor(textColors[position]);
                }

                if (position == Sections.FOCUS.getSectionNumber()) {
                    setEmptyBackground(Sections.FOCUS);
                } else {
                    clearEmptyBackground();
                }

                if (position == Sections.SETTINGS.getSectionNumber()) {
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
        mViewPager.setCurrentItem(Sections.FOCUS.getSectionNumber());
    }

    private void clearEmptyBackground() {
        mActivityMainLayout.setBackgroundColor(ThemeUtils.getCurrentThemeBackgroundColor(this));
        resetDividerColor();
    }

    private void setEmptyBackground(Sections currentSection) {
        if (currentSection == Sections.FOCUS) {
            mActivityMainLayout.setBackgroundResource(R.drawable.default_background);
            // Invert divider color, because otherwise it looks misplaced against the image background.
            invertDividerColor();
        }
    }

    private void invertDividerColor() {
        switch (ThemeUtils.getCurrentTheme(this)) {
            case LIGHT:
                mTabs.setDividerColor(ThemeUtils.getThemeDividerColor(Themes.DARK, this));
                break;
            case DARK:
                mTabs.setDividerColor(ThemeUtils.getThemeDividerColor(Themes.LIGHT, this));
                break;
        }
    }

    private void resetDividerColor() {
        mTabs.setDividerColor(ThemeUtils.getCurrentThemeDividerColor(this));
    }

    @Override
    protected void onDestroy() {
        ButterKnife.reset(this);
        super.onDestroy();
    }

    // HACK: this is a workaround to change the background entirely
    @Override
    public void onEmpty(Sections currentSection) {
        setEmptyBackground(currentSection);
    }

    // HACK: this is a workaround to change the background entirely
    @Override
    public void onNotEmpty() {
        clearEmptyBackground();
    }
}
