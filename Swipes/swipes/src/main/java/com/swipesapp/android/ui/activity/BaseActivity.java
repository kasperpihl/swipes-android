package com.swipesapp.android.ui.activity;

import android.animation.ValueAnimator;
import android.app.ActivityManager.TaskDescription;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.Window;
import android.view.WindowManager;

import com.swipesapp.android.R;
import com.swipesapp.android.analytics.handler.Analytics;
import com.swipesapp.android.app.SwipesApplication;
import com.swipesapp.android.util.ColorUtils;
import com.swipesapp.android.util.DeviceUtils;
import com.swipesapp.android.util.ThemeUtils;
import com.swipesapp.android.values.Constants;

/**
 * Standard activity to be extended across the app.
 * <p/>
 * Handles the Status Bar and Toolbar most common operations.
 */
public class BaseActivity extends ActionBarActivity {

    private Window mWindow;
    protected Toolbar mToolbar;

    /**
     * Default constructor. Enables Status Bar tint.
     *
     * @param savedInstanceState Bundle data.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mWindow = getWindow();

        if (!DeviceUtils.isTablet(this)) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            // Enable Lollipop status bar tint.
            mWindow.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        }

        themeStatusBar(getResources().getColor(R.color.neutral_accent_dark));
    }

    /**
     * Updates relevant analytics dimensions.
     */
    @Override
    public void onResume() {
        // Update Evernote user level dimension.
        Analytics.sendEvernoteUserLevel(this);

        // Send Mailbox status dimension.
        Analytics.sendMailboxStatus(this);

        super.onResume();
    }

    /**
     * Stops background check timer.
     */
    @Override
    public void onStart() {
        SwipesApplication.stopBackgroundTimer();

        super.onStart();
    }

    /**
     * Starts background check timer.
     */
    @Override
    public void onStop() {
        SwipesApplication.startBackgroundTimer();

        super.onStop();
    }

    /**
     * Handles the ActionBar's up navigation.
     *
     * @param item Pressed item.
     * @return True to consume event.
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Respond to the ActionBar's back button.
            case android.R.id.home:
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Inflates the content while also setting the Toolbar as ActionBar.
     *
     * @param layoutResID Resource ID to be inflated.
     */
    @Override
    public void setContentView(int layoutResID) {
        super.setContentView(layoutResID);

        // Set the ToolBar as activity's ActionBar.
        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        mToolbar.setPopupTheme(ThemeUtils.getToolbarPopupTheme(this));
        setSupportActionBar(mToolbar);

        // Enable ActionBar up navigation.
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    /**
     * Applies a given color to the Status Bar.
     *
     * @param color Color to apply.
     */
    protected void themeStatusBar(int color) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            // Adjust status bar for Lollipop.
            mWindow.setStatusBarColor(color);
        }
    }

    /**
     * Applies a given color to the ActionBar.
     *
     * @param color Color to apply.
     */
    protected void themeActionBar(int color) {
        ColorDrawable background = new ColorDrawable(color);
        getSupportActionBar().setBackgroundDrawable(background);
    }

    /**
     * Applies a given color to the header in the recents list.
     *
     * @param color Color to apply.
     */
    protected void themeRecentsHeader(int color) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Bitmap icon = BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher);

            // Adjust header properties.
            TaskDescription description = new TaskDescription(getString(R.string.app_name), icon, color);
            setTaskDescription(description);
        }
    }

    /**
     * Applies a given color to the Status Bar with a smooth transition.
     *
     * @param fromColor Current color.
     * @param toColor   Color to apply.
     */
    protected void transitionStatusBar(final int fromColor, final int toColor) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            ValueAnimator anim = ValueAnimator.ofFloat(0, 1);

            anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    // Blend colors according to position.
                    float position = animation.getAnimatedFraction();
                    int blended = ColorUtils.blendColors(fromColor, toColor, position);

                    // Adjust status bar for Lollipop.
                    themeStatusBar(blended);
                }
            });

            anim.setDuration(Constants.ANIMATION_DURATION_LONG).start();
        }
    }

}
