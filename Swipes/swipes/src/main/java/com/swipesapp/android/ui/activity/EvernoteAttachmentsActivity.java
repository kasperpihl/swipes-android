package com.swipesapp.android.ui.activity;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.FragmentActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.ViewTreeObserver;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ListView;

import com.evernote.edam.type.Note;
import com.swipesapp.android.R;
import com.swipesapp.android.evernote.EvernoteIntegration;
import com.swipesapp.android.evernote.OnEvernoteCallback;
import com.swipesapp.android.sync.gson.GsonAttachment;
import com.swipesapp.android.sync.gson.GsonTask;
import com.swipesapp.android.sync.service.TasksService;
import com.swipesapp.android.ui.adapter.EvernoteAttachmentsAdapter;
import com.swipesapp.android.ui.listener.EvernoteAttachmentsListener;
import com.swipesapp.android.ui.view.ActionEditText;
import com.swipesapp.android.util.Constants;
import com.swipesapp.android.util.ThemeUtils;
import com.swipesapp.android.values.Services;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;

public class EvernoteAttachmentsActivity extends FragmentActivity {

    @InjectView(R.id.attachments_main_layout)
    LinearLayout mLayout;

    @InjectView(R.id.attachments_view)
    LinearLayout mView;

    @InjectView(R.id.search_field)
    ActionEditText mSearchField;

    @InjectView(R.id.filter_checkbox)
    CheckBox mCheckbox;

    private static final String LOG_TAG = EvernoteAttachmentsActivity.class.getSimpleName();

    private static final String FILTER_PREFIX = "todo:* ";

    private WeakReference<Context> mContext;

    private TasksService mTasksService;
    private EvernoteIntegration mEvernoteIntegration;

    private GsonTask mTask;

    private EvernoteAttachmentsAdapter mAdapter;
    private List<Note> mNotes = new ArrayList<Note>();

    private String mQuery = "";

    private Handler mRefreshHandler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(ThemeUtils.getThemeResource(this));
        setContentView(R.layout.activity_evernote_attachments);
        ButterKnife.inject(this);

        getActionBar().hide();

        mContext = new WeakReference<Context>(this);

        mTasksService = TasksService.getInstance(this);
        mEvernoteIntegration = EvernoteIntegration.getInstance();

        Long id = getIntent().getLongExtra(Constants.EXTRA_TASK_ID, 0);

        mTask = mTasksService.loadTask(id);

        customizeViews();

        blurBackground();

        setupListView();
    }

    private void customizeViews() {
        mView.setBackgroundColor(getResources().getColor(R.color.evernote_brand_color));

        mSearchField.setTextColor(Color.WHITE);
        mSearchField.setHintTextColor(Color.WHITE);
        mSearchField.setBackgroundResource(R.drawable.edit_text_white_background);
        mSearchField.addTextChangedListener(mSearchTypeListener);
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

    private void setupListView() {
        // Initialize list view.
        ListView listView = (ListView) findViewById(android.R.id.list);

        // Load notes.
        mEvernoteIntegration.findNotes(FILTER_PREFIX + "", mEvernoteCallback);

        // Setup adapter.
        mAdapter = new EvernoteAttachmentsAdapter(this, mNotes, mAttachmentsListener);
        listView.setAdapter(mAdapter);
    }

    private void loadResults() {
        // Filter notes with tasks by adding prefix.
        if (mCheckbox.isChecked()) mQuery = FILTER_PREFIX + mQuery;

        // Load search results.
        mEvernoteIntegration.findNotes(mQuery, mEvernoteCallback);
    }

    private OnEvernoteCallback<List<Note>> mEvernoteCallback = new OnEvernoteCallback<List<Note>>() {
        @Override
        public void onSuccess(List<Note> data) {
            // Update list of notes.
            if (data != null) {
                mNotes.clear();
                mNotes.addAll(data);

                // Refresh adapter.
                mAdapter.update(mNotes);
            }
        }

        @Override
        public void onException(Exception e) {
            Log.e(LOG_TAG, "Error retrieving search results.", e);
        }
    };

    private TextWatcher mSearchTypeListener = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {
        }

        @Override
        public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
        }

        @Override
        public void afterTextChanged(Editable editable) {
            // Update query after every key press.
            mQuery = mSearchField.getText().toString().toLowerCase();

            // Reset refresh timer.
            mRefreshHandler.removeCallbacks(mResultsRefresher);
            mRefreshHandler.postDelayed(mResultsRefresher, 1000);
        }
    };

    Runnable mResultsRefresher = new Runnable() {
        @Override
        public void run() {
            // Refresh results after timer expires.
            loadResults();
        }
    };

    private EvernoteAttachmentsListener mAttachmentsListener = new EvernoteAttachmentsListener() {
        @Override
        public void attachNote(final Note note) {
            EvernoteIntegration.getInstance().asyncJsonFromNote(note, new OnEvernoteCallback<String>() {
                @Override
                public void onSuccess(String data) {
                    // Save attachment to task.
                    GsonAttachment attachment = new GsonAttachment(null, data, Services.EVERNOTE.getValue(), note.getTitle(), true);
                    mTask.addAttachment(attachment);
                    mTasksService.saveTask(mTask, true);

                    // Send activity result to refresh UI.
                    setResult(RESULT_OK);
                    finish();
                }

                @Override
                public void onException(Exception e) {
                    setResult(RESULT_CANCELED);
                    finish();
                }
            });
        }
    };

    @OnClick(R.id.attachments_main_layout)
    protected void cancel() {
        finish();
    }

    @OnClick(R.id.attachments_view)
    protected void ignore() {
        // Do nothing.
    }

    @OnClick(R.id.filter_checkbox)
    protected void filter() {
        loadResults();
    }

}
