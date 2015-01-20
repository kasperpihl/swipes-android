package com.swipesapp.android.ui.activity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ScrollView;
import android.widget.TextView;

import com.swipesapp.android.BuildConfig;
import com.swipesapp.android.R;
import com.swipesapp.android.util.ThemeUtils;
import com.swipesapp.android.values.Sections;

import butterknife.ButterKnife;
import butterknife.InjectView;

public class AboutActivity extends BaseActivity {

    @InjectView(R.id.about_view)
    ScrollView mView;

    @InjectView(R.id.about_brought_by)
    TextView mBroughtBy;

    @InjectView(R.id.about_oss_licenses)
    TextView mOssLicenses;

    @InjectView(R.id.about_signature_line)
    View mSignatureLine;

    @InjectView(R.id.about_version)
    TextView mVersion;

    @InjectView(R.id.about_policies)
    TextView mPolicies;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(ThemeUtils.getThemeResource(this));
        setContentView(R.layout.activity_about);
        ButterKnife.inject(this);

        getWindow().getDecorView().setBackgroundColor(ThemeUtils.getBackgroundColor(this));

        getSupportActionBar().setDisplayShowTitleEnabled(false);

        themeStatusBar(ThemeUtils.getSectionColorDark(Sections.FOCUS, this));

        mBroughtBy.setTextColor(ThemeUtils.getTextColor(this));
        mOssLicenses.setTextColor(ThemeUtils.getTextColor(this));
        mSignatureLine.setBackgroundColor(ThemeUtils.getDividerColor(this));

        String version = getString(R.string.app_version, BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE);
        mVersion.setText(version);

        mPolicies.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://swipesapp.com/policies.pdf"));
                startActivity(intent);
            }
        });
    }
}
