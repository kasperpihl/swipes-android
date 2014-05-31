package com.swipesapp.android.ui.activity;

import android.os.Bundle;
import android.view.View;
import android.widget.ScrollView;
import android.widget.TextView;

import com.negusoft.holoaccent.activity.AccentActivity;
import com.swipesapp.android.R;
import com.swipesapp.android.util.ThemeUtils;

import butterknife.ButterKnife;
import butterknife.InjectView;

public class AboutActivity extends AccentActivity {

    @InjectView(R.id.about_view)
    ScrollView mView;

    @InjectView(R.id.about_brought_by)
    TextView mBroughtBy;

    @InjectView(R.id.about_oss_licenses)
    TextView mOssLicenses;

    @InjectView(R.id.about_signature_line)
    View mSignatureLine;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(ThemeUtils.getCurrentThemeResource(this));
        setContentView(R.layout.activity_about);
        ButterKnife.inject(this);

        mView.setBackgroundColor(ThemeUtils.getCurrentThemeBackgroundColor(this));
        mBroughtBy.setTextColor(ThemeUtils.getCurrentThemeTextColor(this));
        mOssLicenses.setTextColor(ThemeUtils.getCurrentThemeTextColor(this));
        mSignatureLine.setBackgroundColor(ThemeUtils.getCurrentThemeDividerColor(this));
    }
}
