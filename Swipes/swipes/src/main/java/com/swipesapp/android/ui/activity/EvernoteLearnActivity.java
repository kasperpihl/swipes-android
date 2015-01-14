package com.swipesapp.android.ui.activity;

import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;

import com.negusoft.holoaccent.activity.AccentActivity;
import com.readystatesoftware.systembartint.SystemBarTintManager;
import com.swipesapp.android.R;
import com.swipesapp.android.evernote.EvernoteIntegration;
import com.swipesapp.android.util.ThemeUtils;

import java.lang.ref.WeakReference;

import butterknife.ButterKnife;
import butterknife.InjectView;

public class EvernoteLearnActivity extends AccentActivity {

    @InjectView(R.id.evernote_get_started)
    Button mButtonGetStarted;

    private WeakReference<Context> mContext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(ThemeUtils.getThemeResource(this));
        setContentView(R.layout.activity_evernote_learn);
        ButterKnife.inject(this);

        getWindow().getDecorView().setBackgroundColor(getResources().getColor(R.color.evernote_gray_background));

        getActionBar().hide();

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
        SystemBarTintManager tintManager = new SystemBarTintManager(this);
        tintManager.setStatusBarTintEnabled(true);

        int statusBarColor = getResources().getColor(R.color.evernote_brand_color_dark);

        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.KITKAT) {
            // Adjust status bar for KitKat.
            ColorDrawable statusBarBackground = new ColorDrawable(statusBarColor);
            tintManager.setStatusBarTintDrawable(statusBarBackground);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            // Adjust status bar for Lollipop.
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            getWindow().setStatusBarColor(statusBarColor);
        }
    }

}
