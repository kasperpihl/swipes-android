package com.swipesapp.android.ui.activity;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.FragmentActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ListView;

import com.evernote.edam.type.Note;
import com.swipesapp.android.R;
import com.swipesapp.android.db.Attachment;
import com.swipesapp.android.db.Task;
import com.swipesapp.android.evernote.EvernoteService;
import com.swipesapp.android.evernote.OnEvernoteCallback;
import com.swipesapp.android.sync.service.TasksService;
import com.swipesapp.android.ui.adapter.EvernoteAttachmentsAdapter;
import com.swipesapp.android.ui.listener.EvernoteAttachmentsListener;
import com.swipesapp.android.ui.view.ActionEditText;
import com.swipesapp.android.util.ThemeUtils;
import com.swipesapp.android.values.Constants;
import com.swipesapp.android.values.Services;

import java.util.ArrayList;
import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;

public class EvernoteAttachmentsActivity extends FragmentActivity {

    @InjectView(R.id.attachments_view)
    LinearLayout mView;

    @InjectView(R.id.search_field)
    ActionEditText mSearchField;

    @InjectView(R.id.filter_checkbox)
    CheckBox mCheckbox;

    private static final String LOG_TAG = EvernoteAttachmentsActivity.class.getSimpleName();

    private static final String FILTER_PREFIX = "todo:* ";

    private TasksService mTasksService;
    private EvernoteService mEvernoteIntegration;

    private Task mTask;

    private EvernoteAttachmentsAdapter mAdapter;
    private List<Note> mNotes = new ArrayList<Note>();

    private String mQuery = "";

    private Handler mRefreshHandler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(ThemeUtils.getDialogThemeResource(this));
        setContentView(R.layout.activity_evernote_attachments);
        ButterKnife.inject(this);

        getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        mTasksService = TasksService.getInstance();
        mEvernoteIntegration = EvernoteService.getInstance();

        Long id = getIntent().getLongExtra(Constants.EXTRA_TASK_ID, 0);

        mTask = mTasksService.loadTask(id);

        setupListView();

        customizeViews();
    }

    private void setupListView() {
        // Initialize list view.
        ListView listView = (ListView) findViewById(android.R.id.list);

        // Setup adapter.
        mAdapter = new EvernoteAttachmentsAdapter(this, mNotes, mAttachmentsListener);
        listView.setAdapter(mAdapter);

        // Load notes.
        mEvernoteIntegration.findNotes(FILTER_PREFIX + "", mEvernoteCallback);
    }

    private void customizeViews() {
        boolean lightTheme = ThemeUtils.isLightTheme(this);

        int background = lightTheme ? R.drawable.edit_dialog_light : R.drawable.edit_dialog_dark;
        mView.setBackgroundResource(background);

        mSearchField.setTextColor(Color.WHITE);
        mSearchField.setHintTextColor(Color.WHITE);
        mSearchField.addTextChangedListener(mSearchTypeListener);

        int checkbox = lightTheme ? R.drawable.checkbox_square_selector_light : R.drawable.checkbox_square_selector_dark;
        mCheckbox.setBackgroundResource(checkbox);
    }

    private void loadResults() {
        String query = mQuery;

        // user entered text should end with *
        if (query.length() > 0)
            query = query.trim() + "*";

        // Filter notes with tasks by adding prefix.
        if (mCheckbox.isChecked())
            query = FILTER_PREFIX + query;

        // Load search results.
        mEvernoteIntegration.findNotes(query, mEvernoteCallback);
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

                // Clear query.
                mQuery = "";
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
            EvernoteService.getInstance().asyncJsonFromNote(note, new OnEvernoteCallback<String>() {
                @Override
                public void onSuccess(String data) {
                    // Save attachment to task.
                    Attachment attachment = new Attachment(null, data, Services.EVERNOTE, note.getTitle(), true, mTask.getId());
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
