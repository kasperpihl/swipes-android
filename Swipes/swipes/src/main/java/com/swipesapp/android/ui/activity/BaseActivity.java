package com.swipesapp.android.ui.activity;

import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.Window;
import android.view.WindowManager;

import com.readystatesoftware.systembartint.SystemBarTintManager;

/**
 * Standard activity to be extended across the app.
 * <p/>
 * Handles the Status Bar and Toolbar most common operations.
 */
public class BaseActivity extends ActionBarActivity {

    private Window mWindow;
    private SystemBarTintManager mTintManager;

    /**
     * Default constructor. Enables Status Bar tint.
     *
     * @param savedInstanceState Bundle data.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mWindow = getWindow();

        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.KITKAT) {
            // Enable KitKat translucency and tint.
            mWindow.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            mTintManager = new SystemBarTintManager(this);
            mTintManager.setStatusBarTintEnabled(true);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            // Enable Lollipop status bar tint.
            mWindow.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        }
    }

    /**
     * Applies a given color to the Status Bar.
     *
     * @param color Color to apply.
     */
    protected void themeStatusBar(int color) {
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.KITKAT) {
            // Adjust status bar for KitKat.
            mTintManager.setStatusBarTintDrawable(new ColorDrawable(color));
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            // Adjust status bar for Lollipop.
            mWindow.setStatusBarColor(color);
        }
    }

}
