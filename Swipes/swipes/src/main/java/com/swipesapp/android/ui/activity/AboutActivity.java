package com.swipesapp.android.ui.activity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.swipesapp.android.BuildConfig;
import com.swipesapp.android.R;
import com.swipesapp.android.analytics.handler.Analytics;
import com.swipesapp.android.analytics.values.Screens;
import com.swipesapp.android.ui.view.SwipesTextView;
import com.swipesapp.android.util.ThemeUtils;
import com.swipesapp.android.values.Constants;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;

public class AboutActivity extends BaseActivity {

    @InjectView(R.id.about_brought_by)
    TextView mBroughtBy;

    @InjectView(R.id.about_swipes_signature)
    SwipesTextView mSwipesSignature;

    @InjectView(R.id.about_signature_line)
    View mSignatureLine;

    @InjectView(R.id.about_version)
    TextView mVersion;

    @InjectView(R.id.about_version_number)
    TextView mVersionNumber;

    @InjectView(R.id.about_policies_container)
    LinearLayout mPoliciesContainer;

    @InjectView(R.id.about_policies)
    TextView mPolicies;

    @InjectView(R.id.about_policies_detail)
    TextView mPoliciesDetail;

    @InjectView(R.id.about_oss)
    TextView mOss;

    @InjectView(R.id.about_oss_licenses)
    TextView mOssLicenses;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(ThemeUtils.getThemeResource(this));
        setContentView(R.layout.activity_about);
        ButterKnife.inject(this);

        getWindow().getDecorView().setBackgroundColor(ThemeUtils.getBackgroundColor(this));

        getSupportActionBar().setTitle(getString(R.string.title_activity_about));

        mBroughtBy.setTextColor(ThemeUtils.getTextColor(this));
        mSwipesSignature.setTextColor(ThemeUtils.getSecondaryTextColor(this));
        mSignatureLine.setBackgroundColor(ThemeUtils.getDividerColor(this));

        mVersion.setTextColor(ThemeUtils.getTextColor(this));
        mVersionNumber.setTextColor(ThemeUtils.getSecondaryTextColor(this));

        String version = getString(R.string.app_version, BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE);
        mVersionNumber.setText(version);

        mPolicies.setTextColor(ThemeUtils.getTextColor(this));
        mPoliciesDetail.setTextColor(ThemeUtils.getSecondaryTextColor(this));
        mPoliciesContainer.setOnTouchListener(mPoliciesListener);

        mOss.setTextColor(ThemeUtils.getTextColor(this));
        mOssLicenses.setTextColor(ThemeUtils.getSecondaryTextColor(this));
    }

    @Override
    protected void onResume() {
        // Send screen view event.
        Analytics.sendScreenView(Screens.SCREEN_ABOUT);

        super.onResume();
    }

    @OnClick(R.id.about_policies_container)
    protected void downloadPolicies() {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://swipesapp.com/policies.pdf"));
        startActivity(intent);
    }

    private View.OnTouchListener mPoliciesListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            switch (motionEvent.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    mPoliciesContainer.animate().alpha(Constants.PRESSED_BUTTON_ALPHA);
                    break;
                case MotionEvent.ACTION_UP:
                    mPoliciesContainer.animate().alpha(1.0f);
                    break;
            }
            return false;
        }
    };

}
