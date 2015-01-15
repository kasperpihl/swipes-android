package com.swipesapp.android.ui.activity;

import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;

import com.readystatesoftware.systembartint.SystemBarTintManager;
import com.swipesapp.android.R;
import com.swipesapp.android.evernote.EvernoteIntegration;

import java.lang.ref.WeakReference;

import butterknife.ButterKnife;
import butterknife.InjectView;

public class EvernoteLearnActivity extends ActionBarActivity {

    @InjectView(R.id.evernote_get_started)
    Button mButtonGetStarted;

    private WeakReference<Context> mContext;

    private Window mWindow;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(R.style.Light_Theme);
        setContentView(R.layout.activity_evernote_learn);
        ButterKnife.inject(this);

        mWindow = getWindow();
        mWindow.getDecorView().setBackgroundColor(getResources().getColor(R.color.evernote_gray_background));

        getSupportActionBar().hide();

        themeStatusBar();

        mContext = new WeakReference<Context>(this);

        mButtonGetStarted.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!EvernoteIntegration.getInstance().isAuthenticated()) {
                    // Link Evernote account.
                    EvernoteIntegration.getInstance().authenticateInContext(mContext.get());
                } else {
                    finish();
                }
            }
        });
    }

    private void themeStatusBar() {
        int statusBarColor = getResources().getColor(R.color.evernote_brand_color_dark);

        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.KITKAT) {
            // Enable KitKat translucency and tint.
            mWindow.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            SystemBarTintManager tintManager = new SystemBarTintManager(this);
            tintManager.setStatusBarTintEnabled(true);

            // Adjust status bar for KitKat.
            tintManager.setStatusBarTintDrawable(new ColorDrawable(statusBarColor));
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            // Enable Lollipop status bar tint.
            mWindow.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);

            // Adjust status bar for Lollipop.
            mWindow.setStatusBarColor(statusBarColor);
        }
    }

}
