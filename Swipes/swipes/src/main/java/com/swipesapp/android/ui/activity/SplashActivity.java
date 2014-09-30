package com.swipesapp.android.ui.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.Window;

import com.crashlytics.android.Crashlytics;
import com.swipesapp.android.R;
import com.swipesapp.android.sync.gson.GsonTag;
import com.swipesapp.android.sync.gson.GsonTask;
import com.swipesapp.android.sync.service.TasksService;
import com.swipesapp.android.util.PreferenceUtils;
import com.swipesapp.android.values.RepeatOptions;

import java.util.ArrayList;
import java.util.Date;

public class SplashActivity extends Activity {

    private static final int SPLASH_TIMEOUT = 500;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Crashlytics.start(this);

        requestWindowFeature(Window.FEATURE_NO_TITLE);

        setContentView(R.layout.activity_splash);

        final boolean isFirstRun = PreferenceUtils.isFirstRun(this);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                // Save welcome tasks if the app is launching for the first time.
                if (isFirstRun) {
                    addWelcomeTasks();
                }

                Intent i = new Intent(SplashActivity.this, TasksActivity.class);
                startActivity(i);

                finish();
            }
        }, SPLASH_TIMEOUT);
    }

    private void addWelcomeTasks() {
        TasksService service = TasksService.getInstance(this);
        Date currentDate = new Date();

        GsonTask task = new GsonTask(null, null, null, null, currentDate, currentDate, false, null, null, 0, 0, null, currentDate, null, null, RepeatOptions.NEVER.getValue(), null, null, new ArrayList<GsonTag>(), 0);

        // Save first task.
        String title = getString(R.string.welcome_task_one);
        String tempId = title + currentDate.getTime();
        task.setTitle(title);
        task.setTempId(tempId);

        service.saveTask(task);

        // Save second task.
        title = getString(R.string.welcome_task_two);
        tempId = title + currentDate.getTime();
        task.setTitle(title);
        task.setTempId(tempId);

        service.saveTask(task);

        // Save third task.
        title = getString(R.string.welcome_task_three);
        tempId = title + currentDate.getTime();
        task.setTitle(title);
        task.setTempId(tempId);

        service.saveTask(task);
    }

}
