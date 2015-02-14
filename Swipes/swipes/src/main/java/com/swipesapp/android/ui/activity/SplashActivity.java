package com.swipesapp.android.ui.activity;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.crashlytics.android.Crashlytics;
import com.swipesapp.android.R;
import com.swipesapp.android.db.migration.MigrationAssistant;
import com.swipesapp.android.sync.gson.GsonTag;
import com.swipesapp.android.sync.gson.GsonTask;
import com.swipesapp.android.sync.service.SyncService;
import com.swipesapp.android.sync.service.TasksService;
import com.swipesapp.android.ui.view.SwipesButton;
import com.swipesapp.android.util.ColorUtils;
import com.swipesapp.android.util.Constants;
import com.swipesapp.android.util.DeviceUtils;
import com.swipesapp.android.util.PreferenceUtils;
import com.swipesapp.android.util.ThemeUtils;
import com.swipesapp.android.values.RepeatOptions;
import com.swipesapp.android.values.Sections;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Date;
import java.util.UUID;

public class SplashActivity extends BaseActivity {

    private WeakReference<Context> mContext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Crashlytics.start(this);

        setContentView(R.layout.activity_splash);

        mContext = new WeakReference<Context>(this);

        LayoutInflater inflater = LayoutInflater.from(this);
        View actionBarView = inflater.inflate(R.layout.action_bar_custom_view, null);

        getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        getSupportActionBar().setDisplayShowCustomEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        getSupportActionBar().setCustomView(actionBarView);

        setupSystemBars(actionBarView);

        // Perform migrations when needed.
        MigrationAssistant.performUpgrades(mContext.get());

        // Show welcome screen only once.
        if (!PreferenceUtils.hasShownWelcomeScreen(mContext.get())) {
            // Show welcome screen.
            Intent intent = new Intent(SplashActivity.this, WelcomeActivity.class);
            startActivityForResult(intent, Constants.WELCOME_REQUEST_CODE);
        } else {
            if (ThemeUtils.isLightTheme(this)) {
                // Show tasks without transition.
                showTasks();
            } else {
                // Start transition to tasks screen.
                transitionBackground();
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == Constants.WELCOME_REQUEST_CODE) {
            switch (resultCode) {
                case RESULT_OK:
                    // Save welcome tasks if the app is used for the first time.
                    if (PreferenceUtils.isFirstRun(mContext.get())) {
                        addWelcomeTasks();
                    }

                    // Set welcome screen as shown.
                    PreferenceUtils.saveStringPreference(PreferenceUtils.WELCOME_SCREEN, "YES", this);

                    // Show tasks activity.
                    Intent intent = new Intent(SplashActivity.this, TasksActivity.class);
                    startActivity(intent);

                    break;
            }
        }

        finish();
    }

    @Override
    public void finish() {
        super.finish();

        overridePendingTransition(0, 0);
    }

    private void setupSystemBars(View actionBarView) {
        if (DeviceUtils.isLandscape(this)) {
            themeActionBar(getResources().getColor(R.color.neutral_accent));
            themeStatusBar(getResources().getColor(R.color.neutral_accent_dark));

            TextView title = (TextView) actionBarView.findViewById(R.id.action_bar_title);
            title.setText(getString(R.string.overview_title));

            SwipesButton icon = (SwipesButton) actionBarView.findViewById(R.id.action_bar_icon);
            icon.setText(getString(R.string.schedule_logbook));
        } else {
            themeActionBar(ThemeUtils.getSectionColor(Sections.FOCUS, this));
            themeStatusBar(ThemeUtils.getSectionColorDark(Sections.FOCUS, this));
        }
    }

    private void transitionBackground() {
        final int fromColor = getResources().getColor(R.color.light_neutral_background);
        final int toColor = ThemeUtils.getNeutralBackgroundColor(this);

        ValueAnimator anim = ValueAnimator.ofFloat(0, 1);
        anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                // Blend colors according to position.
                float position = animation.getAnimatedFraction();
                int blended = ColorUtils.blendColors(fromColor, toColor, position);

                // Adjust background color.
                getWindow().setBackgroundDrawable(new ColorDrawable(blended));
            }
        });

        anim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                // Show tasks activity.
                showTasks();
            }
        });

        anim.setDuration(Constants.ANIMATION_DURATION_LONG).start();
    }

    private void showTasks() {
        Intent intent = new Intent(SplashActivity.this, TasksActivity.class);
        startActivity(intent);

        finish();
    }

    private void addWelcomeTasks() {
        TasksService tasksService = TasksService.getInstance(this);
        SyncService syncService = SyncService.getInstance(this);
        Date currentDate = new Date();

        GsonTask task = GsonTask.gsonForLocal(null, null, null, null, currentDate, currentDate, false, null, null, 0, 0,
                null, currentDate, null, null, RepeatOptions.NEVER.getValue(), null, null, new ArrayList<GsonTag>(), null, 0);

        // Save first task.
        String title = getString(R.string.welcome_task_one);
        String tempId = UUID.randomUUID().toString();
        task.setTitle(title);
        task.setTempId(tempId);

        syncService.saveTaskChangesForSync(task);
        tasksService.saveTask(task, false);

        // Save second task.
        title = getString(R.string.welcome_task_two);
        tempId = UUID.randomUUID().toString();
        task.setTitle(title);
        task.setTempId(tempId);

        syncService.saveTaskChangesForSync(task);
        tasksService.saveTask(task, false);

        // Save third task.
        title = getString(R.string.welcome_task_three);
        tempId = UUID.randomUUID().toString();
        task.setTitle(title);
        task.setTempId(tempId);

        syncService.saveTaskChangesForSync(task);
        tasksService.saveTask(task, false);

        GsonTag tag = GsonTag.gsonForLocal(null, null, null, currentDate, currentDate, null);

        // Save first tag.
        title = getString(R.string.welcome_tag_one);
        tempId = UUID.randomUUID().toString();
        tag.setTitle(title);
        tag.setTempId(tempId);

        syncService.saveTagForSync(tag);
        tasksService.saveTag(tag);

        // Save second tag.
        title = getString(R.string.welcome_tag_two);
        tempId = UUID.randomUUID().toString();
        tag.setTitle(title);
        tag.setTempId(tempId);

        syncService.saveTagForSync(tag);
        tasksService.saveTag(tag);
    }

}
