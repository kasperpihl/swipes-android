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

package com.swipesapp.android.adapter;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.swipesapp.android.R;
import com.swipesapp.android.ui.listener.ListContentsListener;
import com.swipesapp.android.util.ThemeUtils;
import com.swipesapp.android.values.Sections;

import java.util.HashMap;
import java.util.List;

// TODO: Refactor adapter for real usage.
public class TasksListAdapter extends ArrayAdapter {

    List data;
    Context context;
    int layoutResID;

    final int INVALID_ID = -1;
    HashMap<String, Integer> mIdMap = new HashMap<String, Integer>();

    private ListContentsListener mListContentsListener;

    Sections mCurrentSection;

    public void setListContentsListener(ListContentsListener listContentsListener) {
        mListContentsListener = listContentsListener;
    }

    public TasksListAdapter(Context context, int layoutResourceId, List data) {
        super(context, layoutResourceId, data);

        this.data = data;
        this.context = context;
        this.layoutResID = layoutResourceId;

        for (int i = 0; i < data.size(); ++i) {
            mIdMap.put(String.valueOf(data.get(i)), i);
        }
    }

    @Override
    public int getCount() {
        int count = super.getCount();
        // HACK: this is a workaround to notify the activity through the fragment
        if (mListContentsListener != null) {
            if (count != 0) {
                mListContentsListener.onNotEmpty();
            } else {
                mListContentsListener.onEmpty(mCurrentSection);
            }
        }
        return count;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        TaskHolder holder = null;
        View row = convertView;

        if (row == null) {
            LayoutInflater inflater = ((Activity) context).getLayoutInflater();
            row = inflater.inflate(layoutResID, parent, false);

            holder = new TaskHolder();

            holder.frontView = (LinearLayout) row.findViewById(R.id.swipe_front);
            holder.backView = (LinearLayout) row.findViewById(R.id.swipe_back);
            holder.frontText = (TextView) row.findViewById(R.id.item_text);

            row.setTag(holder);
        } else {
            holder = (TaskHolder) row.getTag();
        }

        String itemText = String.valueOf(data.get(position));
        holder.frontText.setText(itemText);

        // Sets colors for cell, matching the current theme.
        holder.frontText.setTextColor(ThemeUtils.getCurrentThemeTextColor(getContext()));
        holder.frontView.setBackgroundColor(ThemeUtils.getCurrentThemeBackgroundColor(getContext()));

        return row;
    }

    @Override
    public long getItemId(int position) {
        if (position < 0 || position >= mIdMap.size()) {
            return INVALID_ID;
        }
        String item = String.valueOf(getItem(position));
        return mIdMap.get(item);
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    static class TaskHolder {

        LinearLayout frontView;
        LinearLayout backView;
        TextView frontText;
    }

    public void setCurrentSection(Sections section) {
        mCurrentSection = section;
    }

}
