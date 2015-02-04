package com.swipesapp.android.ui.activity;

import android.os.Bundle;

import com.swipesapp.android.values.Sections;

public class EditLaterTaskActivity extends EditTaskActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        mSection = Sections.LATER;

        super.onCreate(savedInstanceState);
    }

}
