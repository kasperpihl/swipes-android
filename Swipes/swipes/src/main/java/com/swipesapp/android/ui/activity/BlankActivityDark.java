package com.swipesapp.android.ui.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Window;

import com.crashlytics.android.Crashlytics;
import com.swipesapp.android.R;

/**
 * Dummy activity for when the app needs to be launched without the splash screen.
 * <p/>
 * The reason why this is needed is to allow customizations on a per theme basis before
 * the app has been fully initialized.
 * <p/>
 * This is the one to be used with the dark theme.
 */
public class BlankActivityDark extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Crashlytics.start(this);

        requestWindowFeature(Window.FEATURE_NO_TITLE);

        setContentView(R.layout.activity_blank);

        Intent i = new Intent(BlankActivityDark.this, TasksActivity.class);
        startActivity(i);

        finish();
    }

}