package com.swipesapp.android.ui.adapter;

import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.support.v13.app.FragmentPagerAdapter;
import android.view.ViewGroup;

import com.swipesapp.android.ui.fragments.TasksListFragment;
import com.swipesapp.android.util.DeviceUtils;
import com.swipesapp.android.values.Sections;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A {@link FragmentPagerAdapter} to handle section fragments.
 */
public class SectionsPagerAdapter extends FragmentPagerAdapter {

    private WeakReference<Context> mContext;

    private Map<Integer, String> mFragmentTags;
    private FragmentManager mFragmentManager;

    public SectionsPagerAdapter(FragmentManager fm, Context context) {
        super(fm);

        mContext = new WeakReference<>(context);

        mFragmentManager = fm;
        mFragmentTags = new HashMap<>();
    }

    @Override
    public Fragment getItem(int position) {
        Fragment fragment = getFragment(position);

        if (fragment == null) {
            fragment = TasksListFragment.newInstance(position);
        }

        return fragment;
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        Object object = super.instantiateItem(container, position);

        if (object instanceof Fragment) {
            mFragmentTags.put(position, ((Fragment) object).getTag());
        }

        return object;
    }

    @Override
    public int getCount() {
        return Sections.getSectionsCount();
    }

    @Override
    public float getPageWidth(int position) {
        // Set page width based on orientation.
        if (DeviceUtils.isLandscape(mContext.get())) {
            return 0.33333f;
        } else {
            return 1f;
        }
    }

    private Fragment getFragment(int position) {
        String tag = mFragmentTags.get(position);

        return tag != null ? mFragmentManager.findFragmentByTag(tag) : null;
    }

    public TasksListFragment getFragment(Sections section) {
        int position = section.getSectionNumber();

        return (TasksListFragment) getFragment(position);
    }

    public List<TasksListFragment> getFragments() {
        List<TasksListFragment> fragments = new ArrayList<>();

        for (int i = 0; i < getCount(); i++) {
            fragments.add((TasksListFragment) getFragment(i));
        }

        return fragments;
    }

}
