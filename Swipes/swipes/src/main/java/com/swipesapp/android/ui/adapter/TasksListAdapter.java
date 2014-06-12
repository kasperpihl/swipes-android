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

package com.swipesapp.android.ui.adapter;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.swipesapp.android.R;
import com.swipesapp.android.sync.gson.GsonTask;
import com.swipesapp.android.ui.listener.ListContentsListener;
import com.swipesapp.android.util.ThemeUtils;
import com.swipesapp.android.values.Sections;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.List;

// TODO: Refactor adapter for real usage.
public class TasksListAdapter extends ArrayAdapter {

    private List<GsonTask> mData;
    private WeakReference<Context> mContext;
    private int mLayoutResID;
    private Sections mSection;

    private final int INVALID_ID = -1;
    private HashMap<String, Integer> mIdMap = new HashMap<String, Integer>();

    private ListContentsListener mListContentsListener;

    public TasksListAdapter(Context context, int layoutResourceId, List<GsonTask> data, Sections section) {
        super(context, layoutResourceId, data);

        mData = data;
        mContext = new WeakReference<Context>(context);
        mLayoutResID = layoutResourceId;
        mSection = section;

        updateIdMap();
    }

    @Override
    public int getCount() {
        int count = super.getCount();
        // HACK: this is a workaround to notify the activity through the fragment
        if (mListContentsListener != null) {
            if (count != 0) {
                mListContentsListener.onNotEmpty();
            } else {
                mListContentsListener.onEmpty(Sections.FOCUS);
            }
        }
        return count;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        TaskHolder holder = null;
        View row = convertView;

        if (row == null) {
            LayoutInflater inflater = ((Activity) mContext.get()).getLayoutInflater();
            row = inflater.inflate(mLayoutResID, parent, false);

            holder = new TaskHolder();

            holder.frontView = (RelativeLayout) row.findViewById(R.id.swipe_front);
            holder.backView = (RelativeLayout) row.findViewById(R.id.swipe_back);
            holder.frontText = (TextView) row.findViewById(R.id.task_title);

            row.setTag(holder);
        } else {
            holder = (TaskHolder) row.getTag();
        }

        String itemText = mData.get(position).getTitle();
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
        String key = mData.get(position).getObjectId();
        return mIdMap.get(key);
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    private void updateIdMap() {
        mIdMap.clear();
        for (int i = 0; i < mData.size(); ++i) {
            mIdMap.put(mData.get(i).getObjectId(), i);
        }
    }

    public void setListContentsListener(ListContentsListener listContentsListener) {
        mListContentsListener = listContentsListener;
    }

    public List<GsonTask> getData() {
        return mData;
    }

    public void update(List<GsonTask> data) {
        mData = data;
        updateIdMap();
        notifyDataSetChanged();
    }

    private static class TaskHolder {

        RelativeLayout frontView;
        RelativeLayout backView;
        TextView frontText;
    }

}
