package com.swipesapp.android.ui.adapter;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.swipesapp.android.R;
import com.swipesapp.android.sync.gson.GsonTask;
import com.swipesapp.android.ui.view.SwipesTextView;
import com.swipesapp.android.util.Constants;
import com.swipesapp.android.util.ThemeUtils;
import com.swipesapp.android.util.ThreadUtils;

import java.lang.ref.WeakReference;
import java.util.List;

/**
 * Adapter for task lists.
 */
public class SubtasksAdapter extends BaseAdapter {

    private List<GsonTask> mData;
    private WeakReference<Context> mContext;
    private SubtaskListener mListener;
    private Resources mResources;

    public SubtasksAdapter(Context context, List<GsonTask> data, SubtaskListener listener) {
        mData = data;
        mContext = new WeakReference<Context>(context);
        mListener = listener;
        mResources = context.getResources();
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
        SubtaskHolder holder;
        View row = convertView;
        GsonTask task = mData.get(position);
        LayoutInflater inflater = ((Activity) mContext.get()).getLayoutInflater();

        // Inflate layout according to completion.
        if (task.getCompletionDate() != null) {
            row = inflater.inflate(R.layout.subtask_completed, parent, false);
        } else {
            row = inflater.inflate(R.layout.subtask_default, parent, false);
        }

        holder = new SubtaskHolder();

        // Since this list is quite small, there's not much need to reuse the holder.
        holder.container = (RelativeLayout) row.findViewById(R.id.subtask_container);
        holder.circleContainer = (FrameLayout) row.findViewById(R.id.subtask_circle_container);
        holder.buttonContainer = (FrameLayout) row.findViewById(R.id.subtask_buttons_container);
        holder.circle = row.findViewById(R.id.subtask_circle);
        holder.title = (TextView) row.findViewById(R.id.subtask_title);
        holder.button = (SwipesTextView) row.findViewById(R.id.subtask_button);

        row.setTag(holder);

        customizeView(holder, task);

        return row;
    }

    private void customizeView(final SubtaskHolder holder, final GsonTask task) {
        // Setup properties.
        holder.title.setText(task.getTitle());
        final boolean isCompleted = task.getCompletionDate() != null;

        // Setup action.
        holder.button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Quickly fade cell then trigger action.
                holder.container.animate().alpha(0f).setDuration(Constants.ANIMATION_DURATION_SHORT).setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        if (!isCompleted) {
                            mListener.completeSubtask(task);
                        } else {
                            mListener.uncompleteSubtask(task);
                        }
                    }
                });
            }
        });

        // Setup long click.
        holder.container.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                mListener.deleteSubtask(task);
                return true;
            }
        });

        // Customize colors.
        int lightGray = ThemeUtils.isLightTheme(mContext.get()) ? R.color.light_text_hint_color : R.color.dark_text_hint_color;
        holder.button.setTextColor(mResources.getColor(lightGray));
        holder.title.setTextColor(isCompleted ? mResources.getColor(lightGray) : ThemeUtils.getTextColor(mContext.get()));

        // TODO: Set button selector.
    }

    public void update(List<GsonTask> data) {
        // Check for thread safety.
        ThreadUtils.checkOnMainThread();

        // Update data and refresh.
        mData = data;
        super.notifyDataSetChanged();
    }

    private static class SubtaskHolder {

        // Containers.
        RelativeLayout container;
        FrameLayout circleContainer;
        FrameLayout buttonContainer;

        // Views.
        View circle;
        TextView title;
        SwipesTextView button;
    }

    public interface SubtaskListener {
        void completeSubtask(GsonTask task);

        void uncompleteSubtask(GsonTask task);

        void deleteSubtask(GsonTask task);
    }

}
