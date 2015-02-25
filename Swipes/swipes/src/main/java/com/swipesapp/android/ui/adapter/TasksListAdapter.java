package com.swipesapp.android.ui.adapter;

import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.swipesapp.android.R;
import com.swipesapp.android.sync.gson.GsonAttachment;
import com.swipesapp.android.sync.gson.GsonTag;
import com.swipesapp.android.sync.gson.GsonTask;
import com.swipesapp.android.sync.service.TasksService;
import com.swipesapp.android.ui.listener.ListContentsListener;
import com.swipesapp.android.ui.view.SwipesTextView;
import com.swipesapp.android.util.Constants;
import com.swipesapp.android.util.DateUtils;
import com.swipesapp.android.util.ThemeUtils;
import com.swipesapp.android.util.ThreadUtils;
import com.swipesapp.android.values.Sections;
import com.swipesapp.android.values.Services;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Adapter for task lists.
 */
public class TasksListAdapter extends BaseAdapter {

    private static final String TAG_SEPARATOR = ", ";
    private static final String ICON_SEPARATOR = " ";

    private List mData;
    private WeakReference<Context> mContext;
    private int mLayoutResID;
    private Sections mSection;

    private ListContentsListener mListContentsListener;

    private String mEvernoteIcon;
    private String mLocationIcon;
    private String mNotesIcon;
    private String mRepeatIcon;
    private String mTagsIcon;

    private boolean mResetCells;
    private boolean mIsShowingOld;

    // Determines if old tasks will be animated into the screen.
    private boolean mAnimateOld;

    // Determines if cells will be animated after refresh.
    private boolean mAnimateRefresh;

    private int mListViewHeight;
    private int mVisibleAreaHeight;

    private Date mNextDate;
    private Date mPreviousDate;
    private Date mCurrentDate;

    public TasksListAdapter(Context context, int layoutResourceId, Sections section) {
        mData = new ArrayList();
        mContext = new WeakReference<Context>(context);
        mLayoutResID = layoutResourceId;
        mSection = section;

        mLocationIcon = mContext.get().getString(R.string.edit_location);
        mEvernoteIcon = mContext.get().getString(R.string.action_evernote);
        mNotesIcon = mContext.get().getString(R.string.edit_notes);
        mRepeatIcon = mContext.get().getString(R.string.edit_repeat);
        mTagsIcon = mContext.get().getString(R.string.action_tag);
    }

    @Override
    public void notifyDataSetChanged() {
        mResetCells = false;
        mNextDate = null;
        mPreviousDate = null;
        mCurrentDate = null;

        super.notifyDataSetChanged();
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
        if (position < 0 || position >= getCount()) {
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

            holder.parentView = (RelativeLayout) row.findViewById(R.id.cell_parent_view);
            holder.containerView = (FrameLayout) row.findViewById(R.id.swipe_container);
            holder.frontView = (LinearLayout) row.findViewById(R.id.swipe_front);
            holder.backView = (RelativeLayout) row.findViewById(R.id.swipe_back);
            holder.rightContainer = (RelativeLayout) row.findViewById(R.id.task_right_container);
            holder.priorityButton = (CheckBox) row.findViewById(R.id.button_task_priority);
            holder.selectedIndicator = row.findViewById(R.id.selected_indicator);
            holder.title = (TextView) row.findViewById(R.id.task_title);
            holder.time = (TextView) row.findViewById(R.id.task_time);
            holder.icons = (SwipesTextView) row.findViewById(R.id.task_icons);
            holder.subtasksCount = (TextView) row.findViewById(R.id.task_subtask_count);
            holder.label = (TextView) row.findViewById(R.id.task_label);
            holder.tags = (TextView) row.findViewById(R.id.task_tags);
            holder.bottomShadow = row.findViewById(R.id.cell_bottom_shadow);
            holder.topShadow = row.findViewById(R.id.cell_top_shadow);
            holder.leftShadow = row.findViewById(R.id.cell_left_shadow);
            holder.rightShadow = row.findViewById(R.id.cell_right_shadow);

            row.setTag(holder);
        } else {
            holder = (TaskHolder) row.getTag();
        }

        customizeView(holder, position);

        animateOldTask(holder, position);

        realignProperties(holder, row);

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
        List<GsonAttachment> attachments = tasks.get(position).getAttachments();
        String notes = tasks.get(position).getNotes();
        Date repeatDate = tasks.get(position).getLocalRepeatDate();
        Integer priority = tasks.get(position).getPriority();
        boolean selected = tasks.get(position).isSelected();
        String taskId = tasks.get(position).getTempId();
        int subtasksCount = TasksService.getInstance().countUncompletedSubtasksForTask(taskId);

        // Set date for dividers.
        if (tasks.size() == 1) {
            setDatesForSection(tasks.get(position), null, null);
        } else if (position == 0) {
            setDatesForSection(tasks.get(position), null, tasks.get(position + 1));
        } else if (position == tasks.size() - 1) {
            setDatesForSection(tasks.get(position), tasks.get(position - 1), null);
        } else {
            setDatesForSection(tasks.get(position), tasks.get(position - 1), tasks.get(position + 1));
        }

        // Reset cell attributes to avoid recycling misbehavior.
        resetCellState(holder);

        // Set task title.
        holder.title.setText(title);

        // Set cell height based on title size.
        if (holder.title.getLineCount() > 1) {
            setCellHeight(holder, R.dimen.list_item_height_large);
        } else {
            setCellHeight(holder, R.dimen.list_item_height);
        }

        // Set priority.
        if (priority != null) holder.priorityButton.setChecked(priority == 1);

        // Set selection.
        if (selected) {
            holder.selectedIndicator.setBackgroundColor(ThemeUtils.getSectionColor(mSection, mContext.get()));
        } else {
            holder.selectedIndicator.setBackgroundColor(0);
        }

        // Specific rules for each section.
        customizeViewForSection(holder, position, tasks);

        // Display Evernote icon.
        if (attachments != null) {
            for (GsonAttachment attachment : attachments) {
                if (attachment.getService().equals(Services.EVERNOTE.getValue())) {
                    holder.icons.setText(holder.icons.getText() + ICON_SEPARATOR + mEvernoteIcon);
                    holder.icons.setVisibility(View.VISIBLE);
                }
            }
        }

        // Display notes icon.
        if (notes != null && !notes.isEmpty()) {
            holder.icons.setText(holder.icons.getText() + ICON_SEPARATOR + mNotesIcon);
            holder.icons.setVisibility(View.VISIBLE);
        }

        // Display repeat icon.
        if (repeatDate != null) {
            holder.icons.setText(holder.icons.getText() + ICON_SEPARATOR + mRepeatIcon);
            holder.icons.setVisibility(View.VISIBLE);
        }

        // Build the formatted tags.
        for (GsonTag tag : tagList) {
            if (tags == null) {
                tags = tag.getTitle();
            } else {
                tags += TAG_SEPARATOR + tag.getTitle();
            }
        }

        // Display formatted tags.
        if (tags != null && !tags.isEmpty()) {
            // Tags icon.
            holder.icons.setText(holder.icons.getText() + ICON_SEPARATOR + mTagsIcon);
            holder.icons.setVisibility(View.VISIBLE);

            // Tags description.
            holder.tags.setVisibility(View.VISIBLE);
            holder.tags.setText(tags);
        }

        // Trim starting space from icons.
        String iconsText = holder.icons.getText().toString();
        holder.icons.setText(iconsText.trim());

        // Display subtasks count.
        if (subtasksCount > 0) {
            holder.subtasksCount.setText(String.valueOf(subtasksCount));
            holder.subtasksCount.setVisibility(View.VISIBLE);
        }

        // Sets colors for cell, matching the current theme.
        holder.title.setTextColor(ThemeUtils.getTextColor(mContext.get()));
        holder.subtasksCount.setTextColor(mContext.get().getResources().getColor(R.color.neutral_gray));
        holder.containerView.setBackgroundColor(ThemeUtils.getBackgroundColor(mContext.get()));

        // Set shadowed background according to position.
        setShadowBackground(holder, position);

        // Set label divider.
        setLabelDivider(holder, position);
    }

    private void customizeViewForSection(TaskHolder holder, int position, List<GsonTask> tasks) {
        switch (mSection) {
            case LATER:
                // Set priority button color.
                holder.priorityButton.setBackgroundResource(R.drawable.later_circle_selector);

                // Display scheduled time or location icon (never both).
                Date schedule = tasks.get(position).getLocalSchedule();
                String location = tasks.get(position).getLocation();

                if (location != null && !location.isEmpty()) {
                    holder.icons.setText(holder.icons.getText() + ICON_SEPARATOR + mLocationIcon);
                    holder.icons.setVisibility(View.VISIBLE);
                } else {
                    holder.time.setVisibility(View.VISIBLE);
                    holder.time.setText(DateUtils.getTimeAsString(mContext.get(), schedule));
                    holder.time.setTextColor(ThemeUtils.getSectionColor(Sections.LATER, mContext.get()));
                }

                break;
            case FOCUS:
                // Set priority button color.
                holder.priorityButton.setBackgroundResource(R.drawable.focus_circle_selector);
                break;
            case DONE:
                // Set priority button color.
                holder.priorityButton.setBackgroundResource(R.drawable.done_circle_selector);

                // Display completion time and hide repeat icon.
                Date completionDate = tasks.get(position).getLocalCompletionDate();

                if (completionDate != null) {
                    holder.time.setVisibility(View.VISIBLE);
                    holder.time.setText(DateUtils.getTimeAsString(mContext.get(), completionDate));
                    holder.time.setTextColor(ThemeUtils.getSectionColor(Sections.DONE, mContext.get()));
                }

                String repeatIcon = mContext.get().getString(R.string.edit_repeat);
                String textIcons = holder.icons.getText().toString();
                textIcons = textIcons.replace(ICON_SEPARATOR + repeatIcon, "");
                holder.icons.setText(textIcons);

                break;
        }
    }

    private void setShadowBackground(TaskHolder holder, int position) {
        Resources res = mContext.get().getResources();

        // Get component heights.
        int cellHeight = holder.title.getLineCount() > 1 ?
                R.dimen.list_item_height_large : R.dimen.list_item_height;
        int shadowSize = R.dimen.list_item_shadow_size;

        if (getCount() == 1 || (mSection != Sections.FOCUS &&
                (!DateUtils.isSameDay(mCurrentDate, mPreviousDate) &&
                        !DateUtils.isSameDay(mCurrentDate, mNextDate)))) {

            // Group has one item. Calculate cell height with full shadow.
            setParentHeight(holder, cellHeight, shadowSize, shadowSize);
        } else if ((mSection == Sections.FOCUS && position == 0) ||
                (mSection != Sections.FOCUS && !DateUtils.isSameDay(mCurrentDate, mPreviousDate))) {

            // First row. Hide bottom shadow.
            holder.bottomShadow.setVisibility(View.GONE);

            // Calculate cell height based on top shadow only.
            setParentHeight(holder, cellHeight, shadowSize, 0);
        } else if ((mSection == Sections.FOCUS && position == getCount() - 1) ||
                (mSection != Sections.FOCUS && !DateUtils.isSameDay(mCurrentDate, mNextDate))) {

            // Last row. Hide top shadow.
            holder.topShadow.setVisibility(View.GONE);

            // Calculate cell height with bottom shadow and margin.
            setParentHeight(holder, cellHeight, 0, shadowSize);
        } else {
            // Middle row. Hide top and bottom shadows.
            holder.bottomShadow.setVisibility(View.GONE);
            holder.topShadow.setVisibility(View.GONE);

            // Use default cell height.
            setParentHeight(holder, cellHeight, 0, 0);
        }

        // Apply side shadow heights.
        ViewGroup.LayoutParams leftParams = holder.leftShadow.getLayoutParams();
        leftParams.height = res.getDimensionPixelSize(cellHeight);
        holder.leftShadow.setLayoutParams(leftParams);

        ViewGroup.LayoutParams rightParams = holder.rightShadow.getLayoutParams();
        rightParams.height = res.getDimensionPixelSize(cellHeight);
        holder.rightShadow.setLayoutParams(rightParams);
    }

    private void setLabelDivider(TaskHolder holder, int position) {
        // Set text and color according to section.
        switch (mSection) {
            case LATER:
                // Set text as schedule day.
                holder.label.setBackgroundResource(R.drawable.cell_label_later);
                holder.label.setText(DateUtils.formatDayToRecent(mCurrentDate, mContext.get()));
                break;
            case FOCUS:
                // Set text as progress for the day.
                int completedToday = TasksService.getInstance().countTasksCompletedToday();
                int tasksToday = TasksService.getInstance().countTasksForToday() + completedToday;
                String labelText = mContext.get().getString(R.string.tasks_cell_label_focus, completedToday, tasksToday);

                holder.label.setBackgroundResource(R.drawable.cell_label_focus);
                holder.label.setText(labelText);
                break;
            case DONE:
                // Set text as completion day.
                holder.label.setBackgroundResource(R.drawable.cell_label_done);
                holder.label.setText(DateUtils.formatDayToRecent(mCurrentDate, mContext.get()));
                break;
        }

        if (getCount() == 1 || (mSection != Sections.FOCUS &&
                (!DateUtils.isSameDay(mCurrentDate, mPreviousDate) &&
                        !DateUtils.isSameDay(mCurrentDate, mNextDate)))) {

            // Group has one item. Show label divider.
            holder.label.setVisibility(View.VISIBLE);
        } else if ((mSection == Sections.FOCUS && position == 0) ||
                (mSection != Sections.FOCUS && !DateUtils.isSameDay(mCurrentDate, mPreviousDate))) {

            // First row. Show label divider.
            holder.label.setVisibility(View.VISIBLE);
        }
    }

    private void setDatesForSection(GsonTask task, GsonTask previousTask, GsonTask nextTask) {
        switch (mSection) {
            case LATER:
                // Set date as schedule.
                mCurrentDate = task.getLocalSchedule();
                mPreviousDate = previousTask != null ? previousTask.getLocalSchedule() : null;
                mNextDate = nextTask != null ? nextTask.getLocalSchedule() : null;
                break;
            case DONE:
                // Set date as completion.
                mCurrentDate = task.getLocalCompletionDate();
                mPreviousDate = previousTask != null ? previousTask.getLocalCompletionDate() : null;
                mNextDate = nextTask != null ? nextTask.getLocalCompletionDate() : null;
                break;
        }
    }

    private void setCellHeight(TaskHolder holder, int dimension) {
        // Set cell container height.
        ViewGroup.LayoutParams containerParams = holder.containerView.getLayoutParams();
        containerParams.height = mContext.get().getResources().getDimensionPixelSize(dimension);
        holder.containerView.setLayoutParams(containerParams);
    }

    private void setParentHeight(TaskHolder holder, int cellDimen, int topShadowDimen, int bottomShadowDimen) {
        Resources res = mContext.get().getResources();

        // Load child heights.
        int cellHeight = cellDimen != 0 ? res.getDimensionPixelSize(cellDimen) : 0;
        int topShadowHeight = topShadowDimen != 0 ? res.getDimensionPixelSize(topShadowDimen) : 0;
        int bottomShadowHeight = bottomShadowDimen != 0 ? res.getDimensionPixelSize(bottomShadowDimen) : 0;
        int marginHeight = bottomShadowDimen != 0 ? res.getDimensionPixelSize(R.dimen.list_item_margin_bottom) : 0;

        // Set parent height.
        ViewGroup.LayoutParams parentParams = holder.parentView.getLayoutParams();
        parentParams.height = cellHeight + topShadowHeight + bottomShadowHeight + marginHeight;
        holder.parentView.setLayoutParams(parentParams);
    }

    private void realignProperties(final TaskHolder holder, View row) {
        // Align things manually when both the label and counter are visible.
        if (holder.label.getVisibility() == View.VISIBLE && holder.subtasksCount.getVisibility() == View.VISIBLE) {
            // Avoid recycling the container.
            holder.rightContainer = (RelativeLayout) row.findViewById(R.id.task_right_container);

            // Wait until container can be measured.
            final ViewTreeObserver observer = holder.rightContainer.getViewTreeObserver();
            observer.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    // Align subtasks counter to the right.
                    RelativeLayout.LayoutParams countParams = (RelativeLayout.LayoutParams) holder.subtasksCount.getLayoutParams();
                    countParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
                    holder.subtasksCount.setLayoutParams(countParams);

                    // Set a fixed container width based on its first measure.
                    ViewGroup.LayoutParams containerParams = holder.rightContainer.getLayoutParams();
                    containerParams.width = holder.rightContainer.getWidth();
                    holder.rightContainer.setLayoutParams(containerParams);

                    // Remove listener.
                    if (observer.isAlive()) observer.removeGlobalOnLayoutListener(this);
                }
            });
        }
    }

    private void resetCellState(TaskHolder holder) {
        // Reset visibility.
        if (mResetCells) {
            // Reset cell.
            holder.frontView.setBackgroundColor(Color.TRANSPARENT);
            holder.frontView.setVisibility(View.VISIBLE);
            holder.backView.setVisibility(View.GONE);
            holder.parentView.setVisibility(View.VISIBLE);

            // Reset shadows.
            holder.bottomShadow.setVisibility(View.VISIBLE);
            holder.topShadow.setVisibility(View.VISIBLE);
        }

        // Reset properties.
        holder.tags.setVisibility(View.GONE);
        holder.icons.setText("");
        holder.icons.setVisibility(View.GONE);
        holder.subtasksCount.setVisibility(View.GONE);
        holder.label.setVisibility(View.GONE);

        // Reset translation.
        if (mResetCells) {
            if (mAnimateRefresh) {
                ObjectAnimator animator = ObjectAnimator.ofFloat(holder.frontView, "translationX", 0);
                animator.start();
            } else {
                holder.frontView.setTranslationX(0);
            }
        }
    }

    private void animateOldTask(TaskHolder holder, int position) {
        // Animate display of old tasks only when needed.
        if (mAnimateOld && mIsShowingOld && position >= getFirstOldPosition()) {
            DisplayMetrics displaymetrics = new DisplayMetrics();
            ((Activity) mContext.get()).getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);

            float fromY = displaymetrics.heightPixels;
            float toY = holder.containerView.getTranslationY();

            ObjectAnimator animator = ObjectAnimator.ofFloat(holder.containerView, "translationY", fromY, toY);
            animator.setDuration(Constants.ANIMATION_DURATION_MEDIUM).start();
        }

        // Reset flag when all tasks have been animated.
        if (isVisibleAreaFull(holder)) {
            mAnimateOld = false;
        }
    }

    private boolean isVisibleAreaFull(TaskHolder holder) {
        // Calculate max list height.
        ViewGroup.LayoutParams layoutParams = holder.containerView.getLayoutParams();
        int maxListHeight = mListViewHeight + layoutParams.height;

        // Update current visible area.
        mVisibleAreaHeight += layoutParams.height;

        // Determine if visible area is full.
        return mVisibleAreaHeight >= maxListHeight;
    }

    public void setListContentsListener(ListContentsListener listContentsListener) {
        mListContentsListener = listContentsListener;
    }

    public List<GsonTask> getData() {
        return mData;
    }

    public void update(List<GsonTask> data, boolean animateRefresh) {
        // Check for thread safety.
        ThreadUtils.checkOnMainThread();

        // Update data and flags.
        mData = data;
        mAnimateRefresh = animateRefresh;
        mAnimateOld = false;
        mResetCells = true;
        mNextDate = null;
        mPreviousDate = null;
        mCurrentDate = null;

        // Remove old tasks if needed.
        handleOldTasks();

        // Refresh adapter.
        super.notifyDataSetChanged();

        // Check for empty data.
        checkEmpty();
    }

    private void checkEmpty() {
        if (mListContentsListener != null) {
            if (mData.isEmpty()) {
                mListContentsListener.onEmpty();
            } else {
                mListContentsListener.onNotEmpty();
            }
        }
    }

    public void showOld(List<GsonTask> data, int listViewHeight) {
        // Check for thread safety.
        ThreadUtils.checkOnMainThread();

        // Update data and flags.
        mData = data;
        mIsShowingOld = true;
        mAnimateOld = true;
        mResetCells = true;
        mListViewHeight = listViewHeight;
        mVisibleAreaHeight = 0;
        mNextDate = null;
        mPreviousDate = null;
        mCurrentDate = null;

        // Refresh adapter.
        super.notifyDataSetChanged();
    }

    public void setShowingOld(boolean isShowing) {
        mIsShowingOld = isShowing;
    }

    private void handleOldTasks() {
        // Check if old tasks should be displayed.
        if (!mIsShowingOld) {
            List<GsonTask> oldTasks = new ArrayList<GsonTask>();

            for (GsonTask task : (List<GsonTask>) mData) {
                // Check if it's an old task.
                if (DateUtils.isOlderThanToday(task.getLocalCompletionDate())) {
                    // Add it to the removal list.
                    oldTasks.add(task);
                }
            }

            // Remove old tasks from view.
            ((List<GsonTask>) mData).removeAll(oldTasks);
        }
    }

    private int getFirstOldPosition() {
        int position;
        for (position = 0; position < mData.size(); position++) {
            GsonTask task = ((List<GsonTask>) mData).get(position);
            // Check if completion date is older than today.
            if (DateUtils.isOlderThanToday(task.getLocalCompletionDate())) {
                // First old task position found.
                break;
            }
        }
        return position;
    }

    private static class TaskHolder {

        // Containers.
        RelativeLayout parentView;
        FrameLayout containerView;
        LinearLayout frontView;
        RelativeLayout backView;
        RelativeLayout rightContainer;

        // Priority and selection.
        CheckBox priorityButton;
        View selectedIndicator;

        // Main attributes.
        TextView title;
        TextView time;

        // Properties.
        SwipesTextView icons;
        TextView subtasksCount;
        TextView label;

        // Tags.
        TextView tags;

        // Shadows.
        View topShadow;
        View bottomShadow;
        View leftShadow;
        View rightShadow;
    }

}
