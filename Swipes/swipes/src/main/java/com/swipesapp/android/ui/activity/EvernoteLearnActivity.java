package com.swipesapp.android.ui.activity;

import android.app.Activity;
import android.os.Bundle;

import com.swipesapp.android.R;

import butterknife.ButterKnife;

public class EvernoteLearnActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_evernote_learn);
        ButterKnife.inject(this);

        getActionBar().hide();
    }
}
