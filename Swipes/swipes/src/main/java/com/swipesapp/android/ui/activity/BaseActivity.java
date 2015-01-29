package com.swipesapp.android.ui.activity;

import android.app.Activity;
import android.app.ActivityManager.TaskDescription;
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
import com.swipesapp.android.util.ThemeUtils;

/**
 * Standard activity to be extended across the app.
 * <p/>
 * Handles the Status Bar and Toolbar most common operations.
 */
public class BaseActivity extends ActionBarActivity {

    private Window mWindow;

    /**
     * Default constructor. Enables Status Bar tint.
     *
     * @param savedInstanceState Bundle data.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mWindow = getWindow();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            // Enable Lollipop status bar tint.
            mWindow.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        }

        themeStatusBar(getResources().getColor(R.color.neutral_accent_dark));
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
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setPopupTheme(ThemeUtils.getToolbarPopupTheme(this));
        setSupportActionBar(toolbar);

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
            ((Activity) this).setTaskDescription(description);
        }
    }

}
