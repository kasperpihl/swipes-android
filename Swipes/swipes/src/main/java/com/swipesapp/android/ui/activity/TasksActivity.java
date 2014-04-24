package com.swipesapp.android.ui.activity;

import android.app.Activity;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;

import com.swipesapp.android.R;
import com.swipesapp.android.ui.fragments.FocusListFragment;

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
        ft.replace(R.id.tasks_fragment_container, focusListFragment);
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
}
