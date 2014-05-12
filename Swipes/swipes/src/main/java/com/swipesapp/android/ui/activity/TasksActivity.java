package com.swipesapp.android.ui.activity;

import android.app.ActionBar;
import android.app.Activity;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.astuetz.PagerSlidingTabStrip;
import com.swipesapp.android.R;
import com.swipesapp.android.adapter.SectionsPagerAdapter;
import com.swipesapp.android.ui.listener.ListContentsListener;
import com.swipesapp.android.ui.view.NoSwipeViewPager;
import com.swipesapp.android.ui.view.SwipesButton;
import com.swipesapp.android.util.Utils;
import com.swipesapp.android.utils.Constants;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import butterknife.ButterKnife;
import butterknife.InjectView;

public class TasksActivity extends Activity implements ListContentsListener, ActionBar.TabListener, ViewPager.OnPageChangeListener {
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

        getActionBar().hide();

        // Default to second item, index starts at zero
        mViewPager.setCurrentItem(1);
    }

    @Override
    protected void onDestroy() {
        ButterKnife.reset(this);
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.tasks, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            Intent settingsIntent = new Intent(this, SettingsActivity.class);
            startActivity(settingsIntent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // HACK: this is a workaround to change the background entirely
    @Override
    public void onEmpty() {
        mActivityMainLayout.setBackgroundResource(R.drawable.default_background);
        getActionBar().setBackgroundDrawable(new ColorDrawable(android.R.color.transparent));
    }

    // HACK: this is a workaround to change the background entirely
    @Override
    public void onNotEmpty() {
        mActivityMainLayout.setBackgroundResource(0);
        getActionBar().setBackgroundDrawable(new ColorDrawable(android.R.color.white));
    }

    @Override
    public void onTabSelected(ActionBar.Tab tab, FragmentTransaction ft) {
        int[] textColors = {R.color.later_accent_color, R.color.focus_accent_color, R.color.done_accent_color};
        ViewGroup tabView = (ViewGroup) tab.getCustomView();
        TextView tabTextView = (TextView) tabView.findViewById(R.id.tab_swipes_title);
        tabTextView.setTextColor(getResources().getColor(textColors[tab.getPosition()]));
        tab.setCustomView(tabView);

        mViewPager.setCurrentItem(tab.getPosition());
    }

    @Override
    public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction ft) {
        ViewGroup tabView = (ViewGroup) tab.getCustomView();
        TextView tabTextView = (TextView) tabView.findViewById(R.id.tab_swipes_title);
        tabTextView.setTextColor(Utils.getCurrentThemeTextColor(this));
        tab.setCustomView(tabView);
    }

    @Override
    public void onTabReselected(ActionBar.Tab tab, FragmentTransaction ft) {

    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

    }

    @Override
    public void onPageSelected(int position) {
        int[] textColors = {R.color.later_accent_color, R.color.focus_accent_color, R.color.done_accent_color};
        mTabs.setIndicatorColorResource(textColors[position]);
        mTabs.setTextColorResource(android.R.color.black);
        View v = mTabs.getTabView(position);
        if (v instanceof TextView) {
            TextView tabTextView = (TextView)v;
            tabTextView.setTextColor(getResources().getColor(textColors[position]));
        }
    }

    @Override
    public void onPageScrollStateChanged(int state) {

    }
}
