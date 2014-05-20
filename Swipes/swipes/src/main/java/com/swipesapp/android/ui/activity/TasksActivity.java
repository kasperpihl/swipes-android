package com.swipesapp.android.ui.activity;

import android.app.ActionBar;
import android.app.Activity;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.astuetz.PagerSlidingTabStrip;
import com.swipesapp.android.R;
import com.swipesapp.android.adapter.SectionsPagerAdapter;
import com.swipesapp.android.ui.listener.ListContentsListener;
import com.swipesapp.android.ui.view.NoSwipeViewPager;
import com.swipesapp.android.utils.Constants;

import butterknife.ButterKnife;
import butterknife.InjectView;

public class TasksActivity extends Activity implements ListContentsListener, ViewPager.OnPageChangeListener {
    SectionsPagerAdapter mSectionsPagerAdapter;

    @InjectView(R.id.pager)
    NoSwipeViewPager mViewPager;

    @InjectView(R.id.tabs)
    PagerSlidingTabStrip mTabs;

    @InjectView(R.id.tasks_activity_container)
    ViewGroup mActivityMainLayout;

    private static Typeface sTypeface;

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
        mTabs.setOnPageChangeListener(this);

        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }

        // Default to second item, index starts at zero
        mViewPager.setCurrentItem(1);
    }

    @Override
    protected void onDestroy() {
        ButterKnife.reset(this);
        super.onDestroy();
    }

    // HACK: this is a workaround to change the background entirely
    @Override
    public void onEmpty() {
        mActivityMainLayout.setBackgroundResource(R.drawable.default_background);
    }

    // HACK: this is a workaround to change the background entirely
    @Override
    public void onNotEmpty() {
        mActivityMainLayout.setBackgroundResource(0);
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        // Needed to comply with interface
    }

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
    }

    @Override
    public void onPageScrollStateChanged(int state) {
        // Needed to comply with interface
    }
}
