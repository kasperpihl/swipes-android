package com.swipesapp.android.adapter;

import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.support.v13.app.FragmentPagerAdapter;

import com.swipesapp.android.R;
import com.swipesapp.android.ui.activity.SettingsActivity;
import com.swipesapp.android.ui.fragments.TasksListFragment;

import java.lang.ref.WeakReference;
import java.util.Locale;

/**
 * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
 * one of the sections/tabs/pages.
 */
public class SectionsPagerAdapter extends FragmentPagerAdapter {

    private static final int TOTAL_FRAGMENTS = 4;
    private static final int FRAGMENT_SETTINGS_POSITION = TOTAL_FRAGMENTS - 1; //index starts at 0

    /**
     * Context reference.
     */
    private WeakReference<Context> mContext;

    public SectionsPagerAdapter(FragmentManager fm, Context context) {
        super(fm);
        mContext = new WeakReference<Context>(context);
    }

    @Override
    public Fragment getItem(int position) {
        // getItem is called to instantiate the fragment for the given page.
        // Return a PlaceholderFragment (defined as a static inner class below).
        Fragment result;
        if (position == FRAGMENT_SETTINGS_POSITION) {
            result = new SettingsActivity.SettingsFragment();
        } else {
            result = TasksListFragment.newInstance(position);
        }

        return result;
    }

    @Override
    public int getCount() {
        return TOTAL_FRAGMENTS;
    }

    @Override
    public CharSequence getPageTitle(int position) {
        Locale l = Locale.getDefault();
        String title = null;
        Context context = mContext.get();
        if (context != null) {
            switch (position) {
                case 0:
                    title = context.getString(R.string.title_later).toUpperCase(l);
                    break;
                case 1:
                    title = context.getString(R.string.title_focus).toUpperCase(l);
                    break;
                case 2:
                    title = context.getString(R.string.title_done).toUpperCase(l);
                    break;
                case 3:
                    title = context.getString(R.string.title_settings).toUpperCase(l);
                    break;
            }
        }
        return title;
    }
}
