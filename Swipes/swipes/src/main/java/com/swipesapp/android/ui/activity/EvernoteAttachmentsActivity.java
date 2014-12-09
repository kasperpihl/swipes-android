package com.swipesapp.android.ui.activity;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.ViewTreeObserver;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import com.swipesapp.android.R;
import com.swipesapp.android.sync.gson.GsonTask;
import com.swipesapp.android.sync.service.TasksService;
import com.swipesapp.android.ui.view.ActionEditText;
import com.swipesapp.android.util.Constants;
import com.swipesapp.android.util.ThemeUtils;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;

public class EvernoteAttachmentsActivity extends FragmentActivity {

    @InjectView(R.id.attachments_main_layout)
    LinearLayout mLayout;

    @InjectView(R.id.attachments_view)
    RelativeLayout mView;

    @InjectView(R.id.search_field)
    ActionEditText mSearchField;

    private TasksService mTasksService;

    private GsonTask mTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(ThemeUtils.getThemeResource(this));
        setContentView(R.layout.activity_evernote_attachments);
        ButterKnife.inject(this);

        getActionBar().hide();

        mTasksService = TasksService.getInstance(this);

        Long id = getIntent().getLongExtra(Constants.EXTRA_TASK_ID, 0);

        mTask = mTasksService.loadTask(id);

        customizeViews();

        blurBackground();
    }

    private void customizeViews() {
        mView.setBackgroundColor(getResources().getColor(R.color.evernote_brand_color));

        mSearchField.setTextColor(Color.WHITE);
        mSearchField.setHintTextColor(Color.WHITE);
        mSearchField.setBackgroundResource(R.drawable.edit_text_white_background);
    }

    private void blurBackground() {
        // Make activity window transparent.
        getWindow().setBackgroundDrawable(new ColorDrawable(0));

        // Wait for main layout to be drawn.
        ViewTreeObserver observer = mLayout.getViewTreeObserver();
        if (observer.isAlive()) {
            observer.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    mLayout.getViewTreeObserver().removeGlobalOnLayoutListener(this);

                    // Apply blurred background.
                    mLayout.setBackgroundDrawable(EditTaskActivity.getBlurDrawable());
                }
            });
        }
    }

    @OnClick(R.id.attachments_main_layout)
    protected void cancel() {
        finish();
    }

    @OnClick(R.id.attachments_view)
    protected void ignore() {
        // Do nothing.
    }

}
