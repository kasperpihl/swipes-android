package com.swipesapp.android.ui.activity;

import android.app.ActionBar;
import android.app.Activity;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.swipesapp.android.R;
import com.swipesapp.android.adapter.SectionsPagerAdapter;
import com.swipesapp.android.ui.fragments.FocusListFragment;
import com.swipesapp.android.ui.listener.ListContentsListener;
import com.swipesapp.android.ui.view.NoSwipeViewPager;
import com.swipesapp.android.util.Utils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;

public class TasksActivity extends Activity implements ListContentsListener, ActionBar.TabListener {
    SectionsPagerAdapter mSectionsPagerAdapter;

    @InjectView(R.id.pager)
    NoSwipeViewPager mViewPager;

    ActionBar mActionBar;

    @InjectView(R.id.activity_tasks_button_later)
    Button mButtonLater;
    @InjectView(R.id.activity_tasks_button_focus)
    Button mButtonFocus;
    @InjectView(R.id.activity_tasks_button_done)
    Button mButtonDone;
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

//        FocusListFragment focusListFragment = FocusListFragment.newInstance(1);
//        FragmentManager fm = getFragmentManager();
//        FragmentTransaction ft = fm.beginTransaction();
//        ft.add(R.id.tasks_fragment_container, focusListFragment);
//        ft.commit();
    }

    private void setupTabs() {
        LinearLayout tabsArea = (LinearLayout) findViewById(R.id.tabs_area);
        tabsArea.setBackgroundColor(Utils.getCurrentThemeBackgroundColor(this));

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

        int[] iconResourceIds = {R.drawable.schedule_black, R.drawable.focus_highlighted, R.drawable.done_black};
        int[] iconTextIds = {R.string.later_light, R.string.focus_light, R.string.done_light};
        for (int i = 0; i < mSectionsPagerAdapter.getCount(); i++) {
            View tabView = getLayoutInflater().inflate(R.layout.tab_swipes_layout, null);
            TextView tabTextView = (TextView) tabView.findViewById(R.id.tab_swipes_title);
            tabTextView.setText(iconTextIds[i]);
            mActionBar.addTab(mActionBar.newTab()
                    .setCustomView(tabTextView)
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

    private void setButtonsColors(int buttonLaterColorResourceId, int buttonFocusColorResourceId, int buttonDoneColorResourceId) {
        Resources res = getResources();
        mButtonLater.setTextColor(res.getColor(buttonLaterColorResourceId));
        mButtonFocus.setTextColor(res.getColor(buttonFocusColorResourceId));
        mButtonDone.setTextColor(res.getColor(buttonDoneColorResourceId));
    }

    private void transitionToFragment(int fragmentIndex) {
        FocusListFragment focusListFragment = FocusListFragment.newInstance(fragmentIndex);

        FragmentManager fm = getFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();
//        ft.replace(R.id.tasks_fragment_container, focusListFragment);
        ft.commit();
    }

    @OnClick(R.id.activity_tasks_button_later)
    void setupLaterTasksFragment() {
        setButtonsColors(R.color.later_accent_color, R.color.light_theme_text, R.color.light_theme_text);
        transitionToFragment(0);
    }

    @OnClick(R.id.activity_tasks_button_focus)
    void setupFocusTasksFragment() {
        setButtonsColors(R.color.light_theme_text, R.color.focus_accent_color, R.color.light_theme_text);
        transitionToFragment(1);
    }

    @OnClick(R.id.activity_tasks_button_done)
    void setupDoneTasksFragment() {
        setButtonsColors(R.color.light_theme_text, R.color.light_theme_text, R.color.done_accent_color);
        transitionToFragment(2);
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
        TextView tabTextView = (TextView) tab.getCustomView();
        tabTextView.setTextColor(getResources().getColor(textColors[tab.getPosition()]));
        tab.setCustomView(tabTextView);

        mViewPager.setCurrentItem(tab.getPosition());
    }

    @Override
    public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction ft) {
        TextView tabTextView = (TextView) tab.getCustomView();
        tabTextView.setTextColor(Utils.getCurrentThemeTextColor(this));
        tab.setCustomView(tabTextView);
    }

    @Override
    public void onTabReselected(ActionBar.Tab tab, FragmentTransaction ft) {

    }
}
