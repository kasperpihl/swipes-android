/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.swipesapp.android.ui.fragments;

import android.app.ListFragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import com.fortysevendeg.swipelistview.BaseSwipeListViewListener;
import com.fortysevendeg.swipelistview.SwipeListView;
import com.swipesapp.android.Cheeses;
import com.swipesapp.android.R;
import com.swipesapp.android.adapter.NowListAdapter;
import com.swipesapp.android.ui.view.DynamicListView;
import com.swipesapp.android.util.Utils;
import com.swipesapp.android.values.Sections;

import java.util.ArrayList;

/**
 * Fragment for the list of tasks in the Now section.
 */
public class NowListFragment extends ListFragment {

    /**
     * The fragment argument representing the section number for this fragment.
     */
    private static final String ARG_SECTION_NUMBER = "section_number";

    /**
     * Customized list view to display tasks.
     */
    DynamicListView mListView;

    public static NowListFragment newInstance(int sectionNumber) {
        NowListFragment fragment = new NowListFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_SECTION_NUMBER, sectionNumber);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // TODO: Remove this and use real data.
        ArrayList<String> mCheeseList = new ArrayList<String>();
        for (int i = 0; i < Cheeses.sCheeseStrings.length; ++i) {
            mCheeseList.add(Cheeses.sCheeseStrings[i]);
        }

        View rootView = inflater.inflate(R.layout.fragment_now_list, container, false);

        NowListAdapter adapter = new NowListAdapter(getActivity(), R.layout.swipeable_cell, mCheeseList);

        mListView = (DynamicListView) rootView.findViewById(android.R.id.list);
        mListView.setCheeseList(mCheeseList);
        mListView.setAdapter(adapter);
        mListView.setSwipeListViewListener(mSwipeListener);
        mListView.setBackgroundColor(Utils.getCurrentThemeBackgroundColor(getActivity()));
        mListView.setSwipeBackgroundColors(Utils.getSectionColor(Sections.COMPLETED, getActivity()), Utils.getSectionColor(Sections.SCHEDULED, getActivity()));
        mListView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        mListView.setSwipeActionRight(SwipeListView.SWIPE_ACTION_DISMISS);
        mListView.setSwipeActionLeft(SwipeListView.SWIPE_ACTION_REVEAL);

        return rootView;
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
