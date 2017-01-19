package com.swipesapp.android.ui.view;

import android.content.Context;
import android.os.Bundle;
import android.preference.ListPreference;
import android.util.AttributeSet;
import android.view.View;

import com.afollestad.materialdialogs.MaterialDialog;
import com.swipesapp.android.R;

public class SwipesListPreference extends ListPreference {

    private Context mContext;

    public SwipesListPreference(Context context) {
        super(context);
        mContext = context;
    }

    public SwipesListPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
    }

    @Override
    protected void showDialog(Bundle state) {
        int selected = 0;
        if (getEntryValues() != null) {
            for (int i = 0; i < getEntryValues().length; i++) {
                String value = getEntryValues()[i].toString();
                if (value.equals(getValue())) selected = i;
            }
        }

        SwipesDialog.Builder builder = (SwipesDialog.Builder) new SwipesDialog.Builder(mContext)
                .actionsColorRes(R.color.neutral_accent)
                .title(getDialogTitle())
                .icon(getDialogIcon())
                .positiveText(R.string.preference_yes)
                .items(getEntries())
                .itemsCallbackSingleChoice(selected, new MaterialDialog.ListCallbackSingleChoice() {
                    @Override
                    public boolean onSelection(MaterialDialog dialog, View itemView, int which, CharSequence text) {
                        if (which >= 0 && getEntryValues() != null) {
                            String value = getEntryValues()[which].toString();
                            if (callChangeListener(value))
                                setValue(value);
                        }
                        return true;
                    }
                });

        final View contentView = onCreateDialogView();
        if (contentView != null) {
            onBindDialogView(contentView);
            builder.customView(contentView, false);
        } else {
            builder.content(getDialogMessage());
        }

        builder.show();
    }

}
