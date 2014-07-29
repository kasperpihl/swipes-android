package com.swipesapp.android.ui.adapter;

import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.swipesapp.android.R;
import com.swipesapp.android.sync.gson.GsonTag;
import com.swipesapp.android.sync.gson.GsonTask;
import com.swipesapp.android.ui.listener.ListContentsListener;
import com.swipesapp.android.ui.view.SwipesTextView;
import com.swipesapp.android.util.DateUtils;
import com.swipesapp.android.util.ThemeUtils;
import com.swipesapp.android.util.ThreadUtils;
import com.swipesapp.android.values.Sections;

import java.lang.ref.WeakReference;
import java.util.Date;
import java.util.List;

/**
 * Adapter for task lists.
 */
public class TasksListAdapter extends BaseAdapter {

    private static final String TAG_SEPARATOR = ", ";

    private List mData;
    private WeakReference<Context> mContext;
    private int mLayoutResID;
    private Sections mSection;

    // Controls the display of properties line below task title.
    private boolean mDisplayProperties;

    private ListContentsListener mListContentsListener;

    private boolean mResetCells;

    // When true, cell state resets will be animated.
    private boolean mAnimateReset;

    public TasksListAdapter(Context context, int layoutResourceId, List<GsonTask> data, Sections section) {
        mData = data;
        mContext = new WeakReference<Context>(context);
        mLayoutResID = layoutResourceId;
        mSection = section;
    }

    @Override
    public int getCount() {
        return mData.size();
    }

    @Override
    public Object getItem(int position) {
        return mData.get(position);
    }

    @Override
    public long getItemId(int position) {
        if (position < 0 || position >= mData.size()) {
            return -1;
        }
        return ((GsonTask) getItem(position)).getItemId();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        TaskHolder holder;
        View row = convertView;

        if (row == null) {
            LayoutInflater inflater = ((Activity) mContext.get()).getLayoutInflater();
            row = inflater.inflate(mLayoutResID, parent, false);

            holder = new TaskHolder();

            holder.containerView = (FrameLayout) row.findViewById(R.id.swipe_container);
            holder.frontView = (RelativeLayout) row.findViewById(R.id.swipe_front);
            holder.backView = (RelativeLayout) row.findViewById(R.id.swipe_back);
            holder.priorityContainer = (FrameLayout) row.findViewById(R.id.task_priority_container);
            holder.priorityButton = (CheckBox) row.findViewById(R.id.button_task_priority);
            holder.selectedIndicator = row.findViewById(R.id.selected_indicator);
            holder.title = (TextView) row.findViewById(R.id.task_title);
            holder.time = (TextView) row.findViewById(R.id.task_time);
            holder.propertiesContainer = (RelativeLayout) row.findViewById(R.id.task_properties_container);
            holder.propertiesDivider = (TextView) row.findViewById(R.id.task_properties_divider);
            holder.locationIcon = (SwipesTextView) row.findViewById(R.id.task_location_icon);
            holder.notesIcon = (SwipesTextView) row.findViewById(R.id.task_notes_icon);
            holder.repeatIcon = (SwipesTextView) row.findViewById(R.id.task_repeat_icon);
            holder.tagsDivider = (TextView) row.findViewById(R.id.task_tags_divider);
            holder.tags = (TextView) row.findViewById(R.id.task_tags);

            row.setTag(holder);
        } else {
            holder = (TaskHolder) row.getTag();
        }

        customizeView(holder, position);

        return row;
    }

    private void customizeView(final TaskHolder holder, final int position) {
        // HACK: The DynamicListView can only handle generic lists inside the adapter, so mData is
        // a generic list in order to fix a bug that keeps drag and drop from working as expected.
        // A cast needs to be done here to properly display custom data. This behavior is not ideal,
        // so the DynamicListView should be revised in the future to avoid the need of hacks.
        final List<GsonTask> tasks = (List<GsonTask>) mData;

        // Attributes displayed for all sections.
        String title = tasks.get(position).getTitle();
        List<GsonTag> tagList = tasks.get(position).getTags();
        String tags = null;
        String notes = tasks.get(position).getNotes();
        Date repeatDate = tasks.get(position).getRepeatDate();
        Integer priority = tasks.get(position).getPriority();
        boolean selected = tasks.get(position).isSelected();

        // Reset cell attributes to avoid recycling misbehavior.
        if (mResetCells) resetCellState(holder, position);

        // Set task title.
        holder.title.setText(title);

        // Set cell height based on title size.
        if (holder.title.getLineCount() > 1) {
            setCellHeight(holder, R.dimen.list_item_height_large);
        } else {
            setCellHeight(holder, R.dimen.list_item_height);
        }

        // Set priority.
        holder.priorityButton.setChecked(priority == 1);

        // Set selection.
        if (selected) {
            holder.selectedIndicator.setBackgroundColor(ThemeUtils.getSectionColor(mSection, mContext.get()));
        } else {
            holder.selectedIndicator.setBackgroundColor(0);
        }

        // Build the formatted tags.
        if (tagList != null && !tagList.isEmpty()) {
            for (GsonTag tag : tagList) {
                if (tags == null) {
                    tags = tag.getTitle();
                } else {
                    tags += TAG_SEPARATOR + tag.getTitle();
                }
            }

            // Display formatted tags with divider.
            holder.tagsDivider.setVisibility(View.VISIBLE);
            holder.tags.setVisibility(View.VISIBLE);
            holder.tags.setText(tags);
            mDisplayProperties = true;
        }

        // Display notes icon.
        if (notes != null && !notes.isEmpty()) {
            holder.propertiesDivider.setVisibility(View.VISIBLE);
            holder.notesIcon.setVisibility(View.VISIBLE);
            mDisplayProperties = true;
        }

        // Display repeat icon.
        if (repeatDate != null) {
            holder.propertiesDivider.setVisibility(View.VISIBLE);
            holder.repeatIcon.setVisibility(View.VISIBLE);
            mDisplayProperties = true;
        }

        // Specific rules for each section.
        customizeViewForSection(holder, position, tasks);

        // Display properties line.
        if (mDisplayProperties) {
            holder.propertiesContainer.setVisibility(View.VISIBLE);
        }

        // Sets colors for cell, matching the current theme.
        holder.title.setTextColor(ThemeUtils.getCurrentThemeTextColor(mContext.get()));
        holder.frontView.setBackgroundColor(ThemeUtils.getCurrentThemeBackgroundColor(mContext.get()));
    }

    private void customizeViewForSection(TaskHolder holder, int position, List<GsonTask> tasks) {
        switch (mSection) {
            case LATER:
                // Set priority button color.
                holder.priorityButton.setBackgroundResource(R.drawable.later_circle_selector);

                // Display scheduled time or location icon (never both).
                Date schedule = tasks.get(position).getSchedule();
                String location = tasks.get(position).getLocation();

                if (location != null && !location.isEmpty()) {
                    holder.propertiesDivider.setVisibility(View.GONE);
                    holder.locationIcon.setVisibility(View.VISIBLE);
                } else if (schedule != null) {
                    holder.time.setVisibility(View.VISIBLE);
                    holder.time.setText(DateUtils.getTimeAsString(mContext.get(), schedule));
                    holder.time.setTextColor(ThemeUtils.getSectionColor(Sections.LATER, mContext.get()));
                }

                mDisplayProperties = true;
                break;
            case FOCUS:
                // Set priority button color.
                holder.priorityButton.setBackgroundResource(R.drawable.focus_circle_selector);
                break;
            case DONE:
                // Set priority button color.
                holder.priorityButton.setBackgroundResource(R.drawable.done_circle_selector);

                // Display completion time and hide repeat icon.
                Date completionDate = tasks.get(position).getCompletionDate();

                if (completionDate != null) {
                    holder.time.setVisibility(View.VISIBLE);
                    holder.time.setText(DateUtils.getTimeAsString(mContext.get(), completionDate));
                    holder.time.setTextColor(ThemeUtils.getSectionColor(Sections.DONE, mContext.get()));
                }

                holder.repeatIcon.setVisibility(View.GONE);
                mDisplayProperties = true;
                break;
        }
    }

    private void setCellHeight(TaskHolder holder, int dimension) {
        // Set cell container height.
        ViewGroup.LayoutParams layoutParams = holder.containerView.getLayoutParams();
        layoutParams.height = mContext.get().getResources().getDimensionPixelSize(dimension);
        holder.containerView.setLayoutParams(layoutParams);
    }

    private void resetCellState(TaskHolder holder, int position) {
        // Reset visibility.
        holder.frontView.setVisibility(View.VISIBLE);
        holder.containerView.setVisibility(View.VISIBLE);

        // Reset translation.
        if (mAnimateReset) {
            ObjectAnimator animator = ObjectAnimator.ofFloat(holder.frontView, "translationX", 0);
            animator.start();
        } else {
            holder.frontView.setTranslationX(0);
        }

        // Reset flags when views are done loading.
        if (position == mData.size() - 1) mResetCells = false;
    }

    public void setListContentsListener(ListContentsListener listContentsListener) {
        mListContentsListener = listContentsListener;
    }

    public List<GsonTask> getData() {
        return mData;
    }

    public void update(List<GsonTask> data, boolean resetCells, boolean animateReset) {
        // Check for thread safety.
        ThreadUtils.checkOnMainThread();

        // Update data and flags.
        mData = data;
        mResetCells = resetCells;
        mAnimateReset = animateReset;

        // Refresh adapter.
        notifyDataSetChanged();

        // Check for empty data.
        checkEmpty();
    }

    private void checkEmpty() {
        // HACK: This is a workaround to notify the activity through the fragment.
        if (mListContentsListener != null) {
            if (mData.size() > 0) {
                mListContentsListener.onNotEmpty(mSection);
            } else {
                mListContentsListener.onEmpty(mSection);
            }
        }
    }

    private static class TaskHolder {

        // Containers.
        FrameLayout containerView;
        RelativeLayout frontView;
        RelativeLayout backView;

        // Priority and selection.
        FrameLayout priorityContainer;
        CheckBox priorityButton;
        View selectedIndicator;

        // Main attributes.
        TextView title;
        TextView time;

        // Properties.
        RelativeLayout propertiesContainer;
        TextView propertiesDivider;
        SwipesTextView locationIcon;
        SwipesTextView notesIcon;
        SwipesTextView repeatIcon;

        // Tags.
        TextView tagsDivider;
        TextView tags;
    }

}
