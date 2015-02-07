package com.swipesapp.android.ui.view;

import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.annotation.ArrayRes;
import android.support.annotation.AttrRes;
import android.support.annotation.ColorRes;
import android.support.annotation.DrawableRes;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.view.View;
import android.widget.ListAdapter;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.GravityEnum;
import com.afollestad.materialdialogs.MaterialDialog;
import com.afollestad.materialdialogs.Theme;
import com.afollestad.materialdialogs.util.DialogUtils;
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

        @Override
        public Builder title(@StringRes int titleRes) {
            return (Builder) super.title(titleRes);
        }

        @Override
        public Builder title(@NonNull CharSequence title) {
            return (Builder) super.title(title);
        }

        @Override
        public Builder titleGravity(@NonNull GravityEnum gravity) {
            return (Builder) super.titleGravity(gravity);
        }

        @Override
        public Builder titleColor(int color) {
            return (Builder) super.titleColor(color);
        }

        @Override
        public Builder titleColorRes(@ColorRes int colorRes) {
            return (Builder) super.titleColorRes(colorRes);
        }

        @Override
        public Builder titleColorAttr(@AttrRes int colorAttr) {
            return (Builder) super.titleColorAttr(colorAttr);
        }

        @Override
        public Builder disableDefaultFonts() {
            return (Builder) super.disableDefaultFonts();
        }

        @Override
        public Builder typeface(Typeface medium, Typeface regular) {
            return (Builder) super.typeface(medium, regular);
        }

        @Override
        public Builder typeface(String medium, String regular) {
            return (Builder) super.typeface(medium, regular);
        }

        @Override
        public Builder icon(@NonNull Drawable icon) {
            return (Builder) super.icon(icon);
        }

        @Override
        public Builder iconRes(@DrawableRes int icon) {
            return (Builder) super.iconRes(icon);
        }

        @Override
        public Builder iconAttr(@AttrRes int iconAttr) {
            return (Builder) super.iconAttr(iconAttr);
        }

        @Override
        public Builder contentColor(int color) {
            return (Builder) super.contentColor(color);
        }

        @Override
        public Builder contentColorRes(@ColorRes int colorRes) {
            return (Builder) super.contentColorRes(colorRes);
        }

        @Override
        public Builder contentColorAttr(@AttrRes int colorAttr) {
            return (Builder) super.contentColorAttr(colorAttr);
        }

        @Override
        public Builder content(@StringRes int contentRes) {
            return (Builder) super.content(contentRes);
        }

        @Override
        public Builder content(CharSequence content) {
            return (Builder) super.content(content);
        }

        @Override
        public Builder content(@StringRes int contentRes, Object... formatArgs) {
            return (Builder) super.content(contentRes, formatArgs);
        }

        @Override
        public Builder contentGravity(@NonNull GravityEnum gravity) {
            return (Builder) super.contentGravity(gravity);
        }

        @Override
        public Builder contentLineSpacing(float multiplier) {
            return (Builder) super.contentLineSpacing(multiplier);
        }

        @Override
        public Builder items(@ArrayRes int itemsRes) {
            return (Builder) super.items(itemsRes);
        }

        @Override
        public Builder items(@NonNull CharSequence[] items) {
            return (Builder) super.items(items);
        }

        @Override
        public Builder itemsCallback(@NonNull ListCallback callback) {
            return (Builder) super.itemsCallback(callback);
        }

        @Override
        public Builder itemsCallbackSingleChoice(int selectedIndex, @NonNull ListCallback callback) {
            return (Builder) super.itemsCallbackSingleChoice(selectedIndex, callback);
        }

        @Override
        public Builder alwaysCallSingleChoiceCallback() {
            return (Builder) super.alwaysCallSingleChoiceCallback();
        }

        @Override
        public Builder itemsCallbackMultiChoice(@NonNull Integer[] selectedIndices, @NonNull ListCallbackMulti callback) {
            return (Builder) super.itemsCallbackMultiChoice(selectedIndices, callback);
        }

        @Override
        public Builder alwaysCallMultiChoiceCallback() {
            return (Builder) super.alwaysCallMultiChoiceCallback();
        }

        @Override
        public Builder positiveText(@StringRes int positiveRes) {
            return (Builder) super.positiveText(positiveRes);
        }

        @Override
        public Builder positiveText(@NonNull CharSequence message) {
            return (Builder) super.positiveText(message);
        }

        @Override
        public Builder neutralText(@StringRes int neutralRes) {
            return (Builder) super.neutralText(neutralRes);
        }

        @Override
        public Builder neutralText(@NonNull CharSequence message) {
            return (Builder) super.neutralText(message);
        }

        @Override
        public Builder negativeText(@StringRes int negativeRes) {
            return (Builder) super.negativeText(negativeRes);
        }

        @Override
        public Builder negativeText(@NonNull CharSequence message) {
            return (Builder) super.negativeText(message);
        }

        @Override
        public Builder listSelector(@DrawableRes int selectorRes) {
            return (Builder) super.listSelector(selectorRes);
        }

        @Override
        public Builder btnSelectorStacked(@DrawableRes int selectorRes) {
            return (Builder) super.btnSelectorStacked(selectorRes);
        }

        @Override
        public Builder btnSelector(@DrawableRes int selectorRes) {
            return (Builder) super.btnSelector(selectorRes);
        }

        @Override
        public Builder btnSelector(@DrawableRes int selectorRes, @NonNull DialogAction which) {
            return (Builder) super.btnSelector(selectorRes, which);
        }

        @Override
        public Builder btnStackedGravity(@NonNull GravityEnum gravity) {
            return (Builder) super.btnStackedGravity(gravity);
        }

        @Override
        @Deprecated
        public Builder customView(@LayoutRes int layoutRes) {
            return (Builder) super.customView(layoutRes);
        }

        public Builder customView(@LayoutRes int layoutRes, boolean wrapInScrollView) {
            return (Builder) super.customView(layoutRes, wrapInScrollView);
        }

        @Override
        @Deprecated
        public Builder customView(@NonNull View view) {
            return customView(view, true);
        }

        @Override
        public Builder customView(@NonNull View view, boolean wrapInScrollView) {
            return (Builder) super.customView(view, wrapInScrollView);
        }

        @Override
        public Builder positiveColor(int color) {
            return (Builder) super.positiveColor(color);
        }

        @Override
        public Builder positiveColorRes(@ColorRes int colorRes) {
            return (Builder) super.positiveColorRes(colorRes);
        }

        @Override
        public Builder positiveColorAttr(@AttrRes int colorAttr) {
            return (Builder) super.positiveColorAttr(colorAttr);
        }

        @Override
        public Builder negativeColor(int color) {
            return (Builder) super.negativeColor(color);
        }

        @Override
        public Builder negativeColorRes(@ColorRes int colorRes) {
            return (Builder) super.negativeColorRes(colorRes);
        }

        @Override
        public Builder negativeColorAttr(@AttrRes int colorAttr) {
            return (Builder) super.negativeColorAttr(colorAttr);
        }

        @Override
        public Builder neutralColor(int color) {
            return (Builder) super.neutralColor(color);
        }

        @Override
        public Builder neutralColorRes(@ColorRes int colorRes) {
            return (Builder) super.neutralColorRes(colorRes);
        }

        @Override
        public Builder neutralColorAttr(@AttrRes int colorAttr) {
            return (Builder) super.neutralColorAttr(colorAttr);
        }

        @Override
        public Builder dividerColor(int color) {
            return (Builder) super.dividerColor(color);
        }

        @Override
        public Builder dividerColorRes(@ColorRes int colorRes) {
            return (Builder) super.dividerColorRes(colorRes);
        }

        @Override
        public Builder dividerColorAttr(@AttrRes int colorAttr) {
            return dividerColor(DialogUtils.resolveColor(this.context, colorAttr));
        }

        @Override
        public Builder backgroundColor(int color) {
            return (Builder) super.backgroundColor(color);
        }

        @Override
        public Builder backgroundColorRes(@ColorRes int colorRes) {
            return (Builder) super.backgroundColorRes(colorRes);
        }

        @Override
        public Builder backgroundColorAttr(@AttrRes int colorAttr) {
            return (Builder) super.backgroundColorAttr(colorAttr);
        }

        @Override
        public Builder itemColor(int color) {
            return (Builder) super.itemColor(color);
        }

        @Override
        public Builder itemColorRes(@ColorRes int colorRes) {
            return (Builder) super.itemColorRes(colorRes);
        }

        @Override
        public Builder itemColorAttr(@AttrRes int colorAttr) {
            return (Builder) super.itemColorAttr(colorAttr);
        }

        @Override
        public Builder callback(@NonNull ButtonCallback callback) {
            return (Builder) super.callback(callback);
        }

        @Override
        public Builder theme(@NonNull Theme theme) {
            return (Builder) super.theme(theme);
        }

        @Override
        public Builder cancelable(boolean cancelable) {
            return (Builder) super.cancelable(cancelable);
        }

        @Override
        public Builder autoDismiss(boolean dismiss) {
            return (Builder) super.autoDismiss(dismiss);
        }

        @Override
        public Builder adapter(@NonNull ListAdapter adapter) {
            return (Builder) super.adapter(adapter);
        }

        @Override
        public Builder showListener(@NonNull OnShowListener listener) {
            return (Builder) super.showListener(listener);
        }

        @Override
        public Builder dismissListener(@NonNull OnDismissListener listener) {
            return (Builder) super.dismissListener(listener);
        }

        @Override
        public Builder cancelListener(@NonNull OnCancelListener listener) {
            return (Builder) super.cancelListener(listener);
        }

        @Override
        public Builder keyListener(@NonNull OnKeyListener listener) {
            return (Builder) super.keyListener(listener);
        }

        @Override
        public Builder forceStacking(boolean stacked) {
            return (Builder) super.forceStacking(stacked);
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
