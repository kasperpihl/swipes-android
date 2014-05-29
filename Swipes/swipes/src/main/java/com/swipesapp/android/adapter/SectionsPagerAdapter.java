package com.swipesapp.android.adapter;

import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.support.v13.app.FragmentPagerAdapter;

import com.swipesapp.android.R;
import com.swipesapp.android.ui.fragments.TasksListFragment;

import java.lang.ref.WeakReference;
import java.util.Locale;

/**
 * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
 * one of the sections/tabs/pages.
 */
public class SectionsPagerAdapter extends FragmentPagerAdapter {

    /** Context reference. */
    private WeakReference<Context> mContext;

    public SectionsPagerAdapter(FragmentManager fm, Context context) {
        super(fm);
        mContext = new WeakReference<Context>(context);
    }

    @Override
    public Fragment getItem(int position) {
        // getItem is called to instantiate the fragment for the given page.
        // Return a PlaceholderFragment (defined as a static inner class below).
        return TasksListFragment.newInstance(position);
    }

    @Override
    public int getCount() {
        // Show 3 total pages.
        return 3;
    }

    @Override
    public CharSequence getPageTitle(int position) {
        Locale l = Locale.getDefault();
        switch (position) {
            case 0:
                return mContext.get().getString(R.string.title_later).toUpperCase(l);
            case 1:
                return mContext.get().getString(R.string.title_focus).toUpperCase(l);
            case 2:
                return mContext.get().getString(R.string.title_done).toUpperCase(l);
        }
        return null;
    }
}
