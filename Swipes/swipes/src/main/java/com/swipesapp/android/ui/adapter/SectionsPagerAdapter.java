package com.swipesapp.android.ui.adapter;

import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.support.v13.app.FragmentPagerAdapter;

import com.swipesapp.android.ui.fragments.TasksListFragment;
import com.swipesapp.android.values.Sections;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

/**
 * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
 * one of the sections/tabs/pages.
 */
public class SectionsPagerAdapter extends FragmentPagerAdapter {

    private List<TasksListFragment> mFragments = new ArrayList<TasksListFragment>();

    private WeakReference<Context> mContext;

    public SectionsPagerAdapter(FragmentManager fm, Context context) {
        super(fm);
        mContext = new WeakReference<Context>(context);

        // Pre-load fragments.
        for (int x = 0; x < getCount(); x++) {
            mFragments.add(TasksListFragment.newInstance(x));
        }
    }

    @Override
    public Fragment getItem(int position) {
        return mFragments.get(position);
    }

    @Override
    public int getCount() {
        return Sections.getSectionsCount();
    }

}
