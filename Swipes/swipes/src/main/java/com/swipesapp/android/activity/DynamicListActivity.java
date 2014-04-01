package com.swipesapp.android.activity;

import android.app.ActionBar;
import android.app.Activity;
import android.content.res.Resources;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.widget.ListView;
import android.widget.TextView;

import com.fortysevendeg.swipelistview.BaseSwipeListViewListener;
import com.fortysevendeg.swipelistview.SwipeListView;
import com.swipesapp.android.Cheeses;
import com.swipesapp.android.adapter.StableArrayAdapter;
import com.swipesapp.android.util.Utils;
import com.swipesapp.android.values.Sections;
import com.swipesapp.android.ui.view.DynamicListView;

import java.util.ArrayList;

public class DynamicListActivity extends Activity {

    DynamicListView mListView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setupActionBar();

        ArrayList<String> mCheeseList = new ArrayList<String>();
        for (int i = 0; i < Cheeses.sCheeseStrings.length; ++i) {
            mCheeseList.add(Cheeses.sCheeseStrings[i]);
        }

        StableArrayAdapter adapter = new StableArrayAdapter(this, R.layout.swipeable_cell, mCheeseList);

        mListView = new DynamicListView(this, R.id.swipe_back, R.id.swipe_front);
        mListView.setCheeseList(mCheeseList);
        mListView.setAdapter(adapter);
        mListView.setSwipeListViewListener(mSwipeListener);
        mListView.setBackgroundColor(Utils.getCurrentThemeBackgroundColor(this));
        mListView.setSwipeBackgroundColors(Utils.getSectionColor(Sections.COMPLETED, this), Utils.getSectionColor(Sections.SCHEDULED, this));
        mListView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        mListView.setSwipeActionRight(SwipeListView.SWIPE_ACTION_DISMISS);
        mListView.setSwipeActionLeft(SwipeListView.SWIPE_ACTION_REVEAL);
        /*listView.setOffsetLeft(Utils.convertDpiToPixel(260f)); // left side offset
        listView.setOffsetRight(Utils.convertDpiToPixel(0f)); // right side offset*/

        setContentView(mListView);
    }

    private void setupActionBar() {
        ActionBar actionBar = getActionBar();
        int titleId = Resources.getSystem().getIdentifier("action_bar_title", "id", "android");
        TextView title = (TextView) findViewById(titleId);

        // Changes action bar colors to match the current theme.
        actionBar.setBackgroundDrawable(new ColorDrawable(Utils.getCurrentThemeBackgroundColor(this)));
        title.setTextColor(Utils.getCurrentThemeTextColor(this));
        actionBar.setIcon(getResources().getDrawable(R.drawable.ic_action_bar));
    }

    private BaseSwipeListViewListener mSwipeListener = new BaseSwipeListViewListener() {
        @Override
        public void onOpened(int position, boolean toRight) {
        }

        @Override
        public void onClosed(int position, boolean fromRight) {
        }

        @Override
        public void onDismiss(int[] reverseSortedPositions) {
        }
    };

}
