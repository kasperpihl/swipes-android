package com.swipesapp.android.ui.activity;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.Window;

import com.crashlytics.android.Crashlytics;
import com.negusoft.holoaccent.dialog.AccentAlertDialog;
import com.parse.ui.ParseLoginBuilder;
import com.swipesapp.android.R;
import com.swipesapp.android.sync.service.SyncService;
import com.swipesapp.android.sync.service.TasksService;
import com.swipesapp.android.util.Constants;

import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Simple activity to display when the user first launches the app.
 */
public class WelcomeActivity extends Activity {

    private TasksService mTasksService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Crashlytics.start(this);

        requestWindowFeature(Window.FEATURE_NO_TITLE);

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

                        // Perform sync with changesOnly = false.
                        SyncService.getInstance(this).performSync(false);
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
        new AccentAlertDialog.Builder(this)
                .setTitle(getString(R.string.keep_data_dialog_title))
                .setMessage(getString(R.string.keep_data_dialog_message))
                .setPositiveButton(getString(R.string.dialog_yes), new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int which) {
                        showTasks();
                    }
                })
                .setNegativeButton(getString(R.string.dialog_no), new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialogInterface, int i) {
                        // Clear data from test period.
                        mTasksService.clearAllData();

                        showTasks();
                    }
                })
                .create()
                .show();
    }

}
