package com.swipesapp.android.ui.adapter;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.swipesapp.android.R;
import com.swipesapp.android.db.Task;
import com.swipesapp.android.ui.listener.KeyboardBackListener;
import com.swipesapp.android.ui.listener.SubtaskListener;
import com.swipesapp.android.ui.view.ActionEditText;
import com.swipesapp.android.util.ThemeUtils;
import com.swipesapp.android.util.ThreadUtils;

import java.lang.ref.WeakReference;
import java.util.List;

/**
 * Adapter for task lists.
 */
public class SubtasksAdapter extends BaseAdapter {

    private List<Task> mData;
    private WeakReference<Context> mContext;
    private SubtaskListener mListener;
    private Resources mResources;
    private View mMainLayout;

    public SubtasksAdapter(Context context, List<Task> data, SubtaskListener listener, View mainLayout) {
        mData = data;
        mContext = new WeakReference<Context>(context);
        mListener = listener;
        mResources = context.getResources();
        mMainLayout = mainLayout;
    }

    @Override
    public int getCount() {
        return mData.size();
    }

    @Override
    public Object getItem(int position) {
        if (position < 0 || position >= getCount()) {
            return null;
        }
        return mData.get(position);
    }

    @Override
    public long getItemId(int position) {
        Task item = (Task) getItem(position);
        return item != null ? item.getId() : -1;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        SubtaskHolder holder;
        Task task = mData.get(position);
        LayoutInflater inflater = ((Activity) mContext.get()).getLayoutInflater();

        convertView = inflater.inflate(R.layout.subtask_cell, parent, false);

        holder = new SubtaskHolder();

        // Since this list is quite small, there's not much need to reuse the holder.
        holder.container = (RelativeLayout) convertView.findViewById(R.id.subtask_container);
        holder.circleContainer = (FrameLayout) convertView.findViewById(R.id.subtask_circle_container);
        holder.buttonContainer = (FrameLayout) convertView.findViewById(R.id.subtask_buttons_container);
        holder.circle = convertView.findViewById(R.id.subtask_circle);
        holder.title = (ActionEditText) convertView.findViewById(R.id.subtask_title);
        holder.button = (CheckBox) convertView.findViewById(R.id.subtask_button);

        convertView.setTag(holder);

        customizeView(holder, task);

        return convertView;
    }

    private void customizeView(final SubtaskHolder holder, final Task task) {
        // Setup properties.
        holder.title.setText(task.getTitle());
        final boolean isCompleted = task.getCompletionDate() != null;
        holder.button.setChecked(isCompleted);

        // Setup action.
        holder.button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (holder.button.isChecked()) {
                    mListener.completeSubtask(task);
                } else {
                    mListener.uncompleteSubtask(task);
                }
            }
        });

        // Setup long click.
        holder.container.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
//                mListener.deleteSubtask(task);
                return true;
            }
        });

        // Setup edit.
        holder.title.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    // If the action is a key-up event on the return key, save changes.
                    if (v.getText().length() > 0) {
                        task.setTitle(v.getText().toString());
                        mListener.editSubtask(task);
                    } else {
                        v.setText(task.getTitle());
                    }

                    hideKeyboard();
                }
                return true;
            }
        });

        holder.title.setListener(new KeyboardBackListener() {
            @Override
            public void onKeyboardBackPressed() {
                hideKeyboard();

                if (holder.title.getText().length() <= 0) {
                    holder.title.setText(task.getTitle());
                }
            }
        });

        // Customize colors.
        int lightGray = ThemeUtils.isLightTheme(mContext.get()) ? R.color.light_hint : R.color.dark_hint;
        holder.title.setTextColor(isCompleted ? mResources.getColor(lightGray) : ThemeUtils.getTextColor(mContext.get()));

        int background = ThemeUtils.isLightTheme(mContext.get()) ? R.drawable.checkbox_selector_light : R.drawable.checkbox_selector_dark;
        holder.button.setBackgroundResource(background);
    }

    public void update(List<Task> data) {
        // Check for thread safety.
        ThreadUtils.checkOnMainThread();

        // Update data and refresh.
        mData = data;
        super.notifyDataSetChanged();
    }

    private void hideKeyboard() {
        InputMethodManager inputManager = (InputMethodManager) mContext.get().getSystemService(Context.INPUT_METHOD_SERVICE);
        inputManager.toggleSoftInput(0, 0);

        mMainLayout.requestFocus();
    }

    private static class SubtaskHolder {

        // Containers.
        RelativeLayout container;
        FrameLayout circleContainer;
        FrameLayout buttonContainer;

        // Views.
        View circle;
        ActionEditText title;
        CheckBox button;
    }

}
