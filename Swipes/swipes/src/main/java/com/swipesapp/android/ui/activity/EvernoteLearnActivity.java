package com.swipesapp.android.ui.activity;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.swipesapp.android.R;
import com.swipesapp.android.evernote.EvernoteIntegration;

import java.lang.ref.WeakReference;

import butterknife.ButterKnife;
import butterknife.InjectView;

public class EvernoteLearnActivity extends Activity {

    @InjectView(R.id.evernote_get_started)
    Button mButtonGetStarted;

    private WeakReference<Context> mContext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_evernote_learn);
        ButterKnife.inject(this);

        getActionBar().hide();

        mContext = new WeakReference<Context>(this);

        mButtonGetStarted.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Link Evernote account.
                EvernoteIntegration.getInstance().authenticateInContext(mContext.get());
            }
        });
    }
}
