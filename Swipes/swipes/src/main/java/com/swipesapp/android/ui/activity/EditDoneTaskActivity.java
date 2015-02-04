package com.swipesapp.android.ui.activity;

import android.os.Bundle;

import com.swipesapp.android.values.Sections;

public class EditDoneTaskActivity extends EditTaskActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        mSection = Sections.DONE;

        super.onCreate(savedInstanceState);
    }

}
