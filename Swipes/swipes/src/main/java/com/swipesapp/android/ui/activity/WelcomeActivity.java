package com.swipesapp.android.ui.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;

import com.afollestad.materialdialogs.MaterialDialog;
import com.crashlytics.android.Crashlytics;
import com.parse.ui.ParseLoginBuilder;
import com.swipesapp.android.R;
import com.swipesapp.android.sync.gson.GsonTag;
import com.swipesapp.android.sync.gson.GsonTask;
import com.swipesapp.android.sync.service.SyncService;
import com.swipesapp.android.sync.service.TasksService;
import com.swipesapp.android.ui.view.SwipesDialog;
import com.swipesapp.android.util.Constants;
import com.swipesapp.android.util.ThemeUtils;

import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Simple activity to display when the user first launches the app.
 */
public class WelcomeActivity extends ActionBarActivity {

    private TasksService mTasksService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(ThemeUtils.getThemeResource(this));
        Crashlytics.start(this);

        setContentView(R.layout.activity_welcome);

        ButterKnife.inject(this);

        mTasksService = TasksService.getInstance(this);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == Constants.LOGIN_REQUEST_CODE) {
            switch (resultCode) {
                case Activity.RESULT_OK:
                    // Login successful. Ask to keep user data.
                    if (!mTasksService.loadAllTasks().isEmpty()) {
                        askToKeepData();
                    } else {
                        showTasks();
                    }
                    break;
            }
        }
    }

    @Override
    public void onBackPressed() {
        setResult(RESULT_CANCELED);

        super.onBackPressed();
    }

    @OnClick(R.id.welcome_login_button)
    protected void startLogin() {
        // Call Parse login activity.
        ParseLoginBuilder builder = new ParseLoginBuilder(this);
        startActivityForResult(builder.build(), Constants.LOGIN_REQUEST_CODE);
    }

    @OnClick(R.id.welcome_try_button)
    protected void showTasks() {
        setResult(RESULT_OK);
        finish();
    }

    private void askToKeepData() {
        // Display confirmation dialog.
        new SwipesDialog.Builder(this)
                .title(R.string.keep_data_dialog_title)
                .content(R.string.keep_data_dialog_message)
                .positiveText(R.string.keep_data_dialog_yes)
                .negativeText(R.string.keep_data_dialog_no)
                .actionsColorRes(R.color.neutral_accent)
                .cancelable(false)
                .callback(new MaterialDialog.ButtonCallback() {
                    @Override
                    public void onPositive(MaterialDialog dialog) {
                        // Save data from test period for sync.
                        saveDataForSync();

                        showTasks();
                    }

                    @Override
                    public void onNegative(MaterialDialog dialog) {
                        // Clear data from test period.
                        mTasksService.clearAllData();

                        showTasks();
                    }
                })
                .show();
    }

    private void saveDataForSync() {
        // Save all tags for syncing.
        for (GsonTag tag : TasksService.getInstance(this).loadAllTags()) {
            SyncService.getInstance(this).saveTagForSync(tag);
        }

        // Save all tasks for syncing.
        for (GsonTask task : TasksService.getInstance(this).loadAllTasks()) {
            if (!task.getDeleted()) {
                task.setId(null);
                SyncService.getInstance(this).saveTaskChangesForSync(task);
            }
        }
    }

}
