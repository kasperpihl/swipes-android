package com.swipesapp.android.ui.activity;

import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.swipesapp.android.R;
import com.swipesapp.android.evernote.EvernoteIntegration;

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
                if (!EvernoteIntegration.getInstance().isAuthenticated()) {
                    // Link Evernote account.
                    EvernoteIntegration.getInstance().authenticateInContext(mContext.get());
                } else {
                    finish();
                }
            }
        });
    }

}
