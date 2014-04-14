package com.swipesapp.android.ui.activity;

import android.app.ActionBar;
import android.app.Activity;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.res.Resources;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.v13.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.swipesapp.android.R;
import com.swipesapp.android.adapter.SectionsPagerAdapter;
import com.swipesapp.android.ui.fragments.FocusListFragment;
import com.swipesapp.android.ui.view.NoSwipeViewPager;
import com.swipesapp.android.util.Utils;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;

public class TasksActivity extends Activity {

    @InjectView(R.id.activity_tasks_button_later)
    Button mButtonLater;
    @InjectView(R.id.activity_tasks_button_focus)
    Button mButtonFocus;
    @InjectView(R.id.activity_tasks_button_done)
    Button mButtonDone;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tasks);
        ButterKnife.inject(this);

        FocusListFragment focusListFragment = FocusListFragment.newInstance(1);

        FragmentManager fm = getFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();
        ft.add(R.id.tasks_fragment_container, focusListFragment);
        ft.commit();
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
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @OnClick(R.id.activity_tasks_button_done)
    void setupDoneTasksFragment() {

    }

    @OnClick(R.id.activity_tasks_button_focus)
    void setupFocusTasksFragment() {

    }

    @OnClick(R.id.activity_tasks_button_later)
    void setupLaterTasksFragment() {

    }
}
