package com.swipesapp.android.ui.view;

import android.content.Context;
import android.os.Build;
import android.support.annotation.NonNull;

import com.afollestad.materialdialogs.MaterialDialog;
import com.afollestad.materialdialogs.Theme;
import com.swipesapp.android.R;
import com.swipesapp.android.util.ThemeUtils;

/**
 * Custom dialog extending {@link com.afollestad.materialdialogs.MaterialDialog}.
 * <p/>
 * Automatically sets theme, background dim amount and drawable.
 *
 * @author Felipe Bari
 */
public class SwipesDialog extends MaterialDialog {

    protected SwipesDialog(Builder builder) {
        super(builder);
    }

    @Override
    public void onStart() {
        super.onStart();

        boolean lightTheme = ThemeUtils.isLightTheme(getContext());

        getWindow().getAttributes().dimAmount = lightTheme ? 0.2f : 0.4f;
        getWindow().setWindowAnimations(R.style.Dialog_Animation);
    }

    public static SwipesDialog build(MaterialDialog.Builder swipesDialogBuilder) {
        checkInstance(swipesDialogBuilder);
        return ((Builder) swipesDialogBuilder).build();
    }

    public static SwipesDialog show(MaterialDialog.Builder swipesDialogBuilder) {
        checkInstance(swipesDialogBuilder);
        return ((Builder) swipesDialogBuilder).show();
    }

    private static void checkInstance(MaterialDialog.Builder swipesDialogBuilder) {
        if (!(swipesDialogBuilder instanceof Builder))
            throw new AssertionError(String.format("swipesDialogBuilder must be an instance of %s",
                    Builder.class.getCanonicalName()));
    }

    /**
     * Custom builder extending {@link com.afollestad.materialdialogs.MaterialDialog.Builder}.
     * <p/>
     * Keeps the original methods intact while returning an instance of {@link SwipesDialog}
     * with the current theme automatically set.
     */
    public static class Builder extends MaterialDialog.Builder {

        public Builder(@NonNull Context context) {
            super(context);

            boolean lightTheme = ThemeUtils.isLightTheme(context);
            int selector = lightTheme ? R.drawable.flat_button_light : R.drawable.flat_button_dark;
            int background = ThemeUtils.getNeutralBackgroundColor(context);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                selector = lightTheme ? R.drawable.flat_button_ripple_light : R.drawable.flat_button_ripple_dark;
            }

            theme(lightTheme ? Theme.LIGHT : Theme.DARK);
            btnSelector(selector);
            backgroundColor(background);
        }

        public Builder actionsColor(int color) {
            positiveColor(color);
            negativeColor(color);
            neutralColor(color);
            return this;
        }

        public Builder actionsColorRes(int colorRes) {
            positiveColorRes(colorRes);
            negativeColorRes(colorRes);
            neutralColorRes(colorRes);
            return this;
        }

        @Override
        public SwipesDialog build() {
            return new SwipesDialog(this);
        }

        @Override
        public SwipesDialog show() {
            SwipesDialog dialog = build();
            dialog.show();
            return dialog;
        }

    }

}
