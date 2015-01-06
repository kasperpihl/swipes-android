package com.swipesapp.android.ui.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.Window;

import com.crashlytics.android.Crashlytics;
import com.swipesapp.android.R;
import com.swipesapp.android.db.migration.MigrationAssistant;
import com.swipesapp.android.sync.gson.GsonTag;
import com.swipesapp.android.sync.gson.GsonTask;
import com.swipesapp.android.sync.service.SyncService;
import com.swipesapp.android.sync.service.TasksService;
import com.swipesapp.android.util.Constants;
import com.swipesapp.android.util.PreferenceUtils;
import com.swipesapp.android.values.RepeatOptions;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Date;
import java.util.UUID;

public class SplashActivity extends Activity {

    private static final int SPLASH_TIMEOUT = 100;

    private WeakReference<Context> mContext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Crashlytics.start(this);

        requestWindowFeature(Window.FEATURE_NO_TITLE);

        setContentView(R.layout.activity_splash);

        mContext = new WeakReference<Context>(this);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                // Perform migrations when needed.
                MigrationAssistant.performUpgrades(mContext.get());

                // Show welcome screen only once.
                if (!PreferenceUtils.hasShownWelcomeScreen(mContext.get())) {
                    // Show welcome screen.
                    Intent intent = new Intent(SplashActivity.this, WelcomeActivity.class);
                    startActivityForResult(intent, Constants.WELCOME_REQUEST_CODE);
                } else {
                    // Show tasks activity.
                    Intent intent = new Intent(SplashActivity.this, TasksActivity.class);
                    startActivity(intent);

                    finish();
                }
            }
        }, SPLASH_TIMEOUT);
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

    private void addWelcomeTasks() {
        TasksService tasksService = TasksService.getInstance(this);
        SyncService syncService = SyncService.getInstance(this);
        Date currentDate = new Date();

        GsonTask task = GsonTask.gsonForLocal(null, null, null, null, currentDate, currentDate, false, null, null, 0, 0, null, currentDate, null, null, RepeatOptions.NEVER.getValue(), null, null, new ArrayList<GsonTag>(), null, 0);

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
    }

}
