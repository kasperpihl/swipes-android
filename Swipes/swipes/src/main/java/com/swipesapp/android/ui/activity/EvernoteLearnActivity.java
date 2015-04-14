package com.swipesapp.android.ui.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.evernote.client.android.EvernoteSession;
import com.swipesapp.android.R;
import com.swipesapp.android.analytics.handler.Analytics;
import com.swipesapp.android.analytics.values.Screens;
import com.swipesapp.android.evernote.EvernoteService;

import java.lang.ref.WeakReference;

import butterknife.ButterKnife;
import butterknife.InjectView;

public class EvernoteLearnActivity extends BaseActivity {

    @InjectView(R.id.evernote_get_started)
    Button mButtonGetStarted;

    private WeakReference<Context> mContext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(R.style.Light_Theme);
        setContentView(R.layout.activity_evernote_learn);
        ButterKnife.inject(this);

        mContext = new WeakReference<Context>(this);

        getWindow().getDecorView().setBackgroundColor(getResources().getColor(R.color.evernote_gray_background));

        getSupportActionBar().hide();

        themeStatusBar(getResources().getColor(R.color.evernote_brand_dark));

        mContext = new WeakReference<Context>(this);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mButtonGetStarted.setBackgroundResource(R.drawable.red_button_ripple);
        }

        mButtonGetStarted.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!EvernoteService.getInstance().isAuthenticated()) {
                    // Link Evernote account.
                    EvernoteService.getInstance().authenticate(mContext.get());
                } else {
                    finish();
                }
            }
        });
    }

    @Override
    protected void onResume() {
        // Send screen view event.
        Analytics.sendScreenView(Screens.SCREEN_EVERNOTE_LEARN);

        super.onResume();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case EvernoteSession.REQUEST_CODE_OAUTH:
                if (resultCode == Activity.RESULT_OK) {
                    // Close this and refresh settings UI.
                    setResult(Activity.RESULT_OK);
                    finish();
                } else {
                    // Do nothing after finishing.
                    setResult(Activity.RESULT_CANCELED);
                }
                break;
        }
    }

}
