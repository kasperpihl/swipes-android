package com.swipesapp.android.ui.adapter;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.evernote.edam.type.Note;
import com.swipesapp.android.R;
import com.swipesapp.android.ui.listener.EvernoteAttachmentsListener;
import com.swipesapp.android.util.DateUtils;
import com.swipesapp.android.util.ThreadUtils;

import java.lang.ref.WeakReference;
import java.util.Date;
import java.util.List;

/**
 * Adapter for task lists.
 */
public class EvernoteAttachmentsAdapter extends BaseAdapter {

    private List<Note> mData;
    private WeakReference<Context> mContext;
    private EvernoteAttachmentsListener mListener;

    public EvernoteAttachmentsAdapter(Context context, List<Note> data, EvernoteAttachmentsListener listener) {
        mData = data;
        mContext = new WeakReference<Context>(context);
        mListener = listener;
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
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        EvernoteAttachmentsHolder holder;
        View row = convertView;
        Note note = mData.get(position);

        if (row == null) {
            LayoutInflater inflater = ((Activity) mContext.get()).getLayoutInflater();
            row = inflater.inflate(R.layout.evernote_attachment_cell, parent, false);

            holder = new EvernoteAttachmentsHolder();

            holder.container = row.findViewById(R.id.attachment_container);
            holder.title = (TextView) row.findViewById(R.id.attachment_title);
            holder.time = (TextView) row.findViewById(R.id.attachment_time);

            row.setTag(holder);
        } else {
            holder = (EvernoteAttachmentsHolder) row.getTag();
        }

        customizeView(holder, note);

        return row;
    }

    private void customizeView(EvernoteAttachmentsHolder holder, final Note note) {
        // Setup properties.
        holder.title.setText(note.getTitle());
        Date time = DateUtils.dateFromMillis(note.getUpdated());
        holder.time.setText(DateUtils.formatToRecent(time, mContext.get()));

        // Setup action.
        holder.container.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Attach note.
                mListener.attachNote(note);
            }
        });
    }

    public void update(List<Note> data) {
        // Check for thread safety.
        ThreadUtils.checkOnMainThread();

        // Update data and refresh.
        mData = data;
        super.notifyDataSetChanged();
    }

    private static class EvernoteAttachmentsHolder {
        // Views.
        View container;
        TextView title;
        TextView time;
    }

}
