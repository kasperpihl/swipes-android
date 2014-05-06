package com.swipesapp.android.ui.activity;

import android.app.ActionBar;
import android.app.Activity;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.swipesapp.android.R;
import com.swipesapp.android.adapter.SectionsPagerAdapter;
import com.swipesapp.android.ui.listener.ListContentsListener;
import com.swipesapp.android.ui.view.NoSwipeViewPager;
import com.swipesapp.android.util.Utils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import butterknife.ButterKnife;
import butterknife.InjectView;

public class TasksActivity extends Activity implements ListContentsListener, ActionBar.TabListener {
    SectionsPagerAdapter mSectionsPagerAdapter;

    @InjectView(R.id.pager)
    NoSwipeViewPager mViewPager;

    ActionBar mActionBar;

    @InjectView(R.id.tasks_activity_container)
    ViewGroup mActivityMainLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tasks);
        ButterKnife.inject(this);

        mSectionsPagerAdapter = new SectionsPagerAdapter(getFragmentManager(), this);
        mViewPager.setAdapter(mSectionsPagerAdapter);
        mViewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                mActionBar.setSelectedNavigationItem(position);
            }
        });
        setupActionBar();
        setupTabs();

        // Default to second item, index starts at zero
        mViewPager.setCurrentItem(1);
    }

    private void setupTabs() {
        final Method setHasEmbeddedTabsMethod;
        try {
            setHasEmbeddedTabsMethod = mActionBar.getClass()
                    .getDeclaredMethod("setHasEmbeddedTabs", boolean.class);
            setHasEmbeddedTabsMethod.setAccessible(true);
            setHasEmbeddedTabsMethod.invoke(mActionBar, true);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    private void setupActionBar() {
        mActionBar = getActionBar();
        mActionBar.setBackgroundDrawable(new ColorDrawable(Utils.getCurrentThemeBackgroundColor(this)));

        int titleId = Resources.getSystem().getIdentifier("action_bar_title", "id", "android");
        TextView title = (TextView) findViewById(titleId);
        if (title != null) {
            title.setTextColor(Utils.getCurrentThemeTextColor(this));
        }

        mActionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

        int[] iconTextIds = {R.string.later_light, R.string.focus_light, R.string.done_light};
        int[] tabIndicators = {R.drawable.tab_indicator_ab_later, R.drawable.tab_indicator_ab_focus, R.drawable.tab_indicator_ab_done};

        for (int i = 0; i < mSectionsPagerAdapter.getCount(); i++) {
            View tabView = getLayoutInflater().inflate(R.layout.tab_swipes_layout, null);
            TextView tabTextView = (TextView) tabView.findViewById(R.id.tab_swipes_title);
            tabTextView.setText(iconTextIds[i]);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                tabView.setBackground(getResources().getDrawable(tabIndicators[i]));
            } else {
                tabView.setBackgroundDrawable(getResources().getDrawable(tabIndicators[i]));
            }
            mActionBar.addTab(mActionBar.newTab()
                    .setCustomView(tabView)
                    .setTabListener(this));
        }
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
}
