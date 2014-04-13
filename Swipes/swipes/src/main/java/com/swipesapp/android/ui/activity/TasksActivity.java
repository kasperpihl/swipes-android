package com.swipesapp.android.ui.activity;

import android.app.ActionBar;
import android.app.Activity;
import android.app.FragmentTransaction;
import android.content.res.Resources;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.v13.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.swipesapp.android.R;
import com.swipesapp.android.adapter.SectionsPagerAdapter;
import com.swipesapp.android.ui.view.NoSwipeViewPager;
import com.swipesapp.android.util.Utils;

public class TasksActivity extends Activity implements ActionBar.TabListener {

    /**
     * The {@link android.support.v4.view.PagerAdapter} that will provide
     * fragments for each of the sections. We use a
     * {@link FragmentPagerAdapter} derivative, which will keep every
     * loaded fragment in memory. If this becomes too memory intensive, it
     * may be best to switch to a
     * {@link android.support.v13.app.FragmentStatePagerAdapter}.
     */
    SectionsPagerAdapter mSectionsPagerAdapter;

    /**
     * The {@link ViewPager} that will host the section contents.
     */
    NoSwipeViewPager mViewPager;

    /**
     * Action bar instance.
     */
    ActionBar mActionBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tasks);

        setupActionBar();

        setupTabs();

        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mSectionsPagerAdapter = new SectionsPagerAdapter(getFragmentManager(), this);

        // Set up the ViewPager with the sections adapter.
        mViewPager = (NoSwipeViewPager) findViewById(R.id.pager);
        mViewPager.setAdapter(mSectionsPagerAdapter);

        // When swiping between different sections, select the corresponding
        // tab. We can also use ActionBar.Tab#select() to do this if we have        // a reference to the Tab.

        mViewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                mActionBar.setSelectedNavigationItem(position);
            }
        });

        /*int[] iconResourceIds = {R.drawable.schedule_black, R.drawable.now_highlighted, R.drawable.done_black};
        For each of the sections in the app, add a tab to the action bar.
        for (int i = 0; i < mSectionsPagerAdapter.getCount(); i++) {
            Create a tab with text corresponding to the page title defined by
            the adapter. Also specify this Activity object, which implements
            the TabListener interface, as the callback (listener) for when
            this tab is selected.
            actionBar.addTab(
                    actionBar.newTab()
                            .setIcon(iconResourceIds[i])
                            .setTabListener(this)
            );
        }

        Tab 2 is default, index starts in 0
        actionBar.setSelectedNavigationItem(1);*/
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.tasks, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onTabSelected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
        // When the given tab is selected, switch to the corresponding page in
        // the ViewPager.
        mViewPager.setCurrentItem(tab.getPosition());
    }

    @Override
    public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
    }

    @Override
    public void onTabReselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
    }

    /**
     * Customizes the action bar.
     */
    private void setupActionBar() {
        mActionBar = getActionBar();
        int titleId = Resources.getSystem().getIdentifier("action_bar_title", "id", "android");
        TextView title = (TextView) findViewById(titleId);

        // Changes action bar colors to match the current theme.
        mActionBar.setBackgroundDrawable(new ColorDrawable(Utils.getCurrentThemeBackgroundColor(this)));
        title.setTextColor(Utils.getCurrentThemeTextColor(this));
        mActionBar.setIcon(getResources().getDrawable(R.drawable.ic_action_bar));
    }

    /**
     * Customizes the tabs area.
     */
    private void setupTabs() {
        // Changes colors to match the current theme.
        LinearLayout tabsArea = (LinearLayout) findViewById(R.id.tabs_area);
        tabsArea.setBackgroundColor(Utils.getCurrentThemeBackgroundColor(this));

        // TODO: Find out a clever way to dynamically load icons based on the theme.
    }

}
