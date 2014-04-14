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

public class TasksActivity extends Activity implements View.OnClickListener {

    private static final String LOG_TAG = TasksActivity.class.getCanonicalName();

    private Button mButtonLater;
    private Button mButtonFocus;
    private Button mButtonDone;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tasks);

        FocusListFragment focusListFragment = FocusListFragment.newInstance(1);

        FragmentManager fm = getFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();
        ft.add(R.id.tasks_fragment_container, focusListFragment);
        ft.commit();
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
    public void onClick(View target) {
        switch (target.getId()) {
            case R.id.activity_tasks_button_later:
                setupLaterTasksFragment();
                break;
            case R.id.activity_tasks_button_focus:
                setupFocusTasksFragment();
                break;
            case R.id.activity_tasks_button_done:
                setupDoneTasksFragment();
                break;
            default:
                Log.wtf(LOG_TAG, "Clicked some other button");
        }
    }

    private void setupDoneTasksFragment() {
        
    }

    private void setupFocusTasksFragment() {

    }

    private void setupLaterTasksFragment() {

    }
}
