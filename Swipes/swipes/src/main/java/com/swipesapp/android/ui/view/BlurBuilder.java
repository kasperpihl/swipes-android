package com.swipesapp.android.ui.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.PorterDuff;
import android.graphics.drawable.BitmapDrawable;
import android.support.v8.renderscript.Allocation;
import android.support.v8.renderscript.Element;
import android.support.v8.renderscript.RenderScript;
import android.support.v8.renderscript.ScriptIntrinsicBlur;
import android.view.View;
import android.view.Window;

/**
 * Applies blur to a bitmap or view.
 *
 * @author Fernanda Bari
 */
public class BlurBuilder {

    private static final float BITMAP_SCALE = 0.4f;
    private static final float BLUR_RADIUS = 20f;

    public static BitmapDrawable blur(Context context, View view, int alphaColor) {
        // Take screenshot.
        Bitmap blurBitmap = blur(view.getContext(), getScreenshot(context, view));

        // Apply color filter.
        BitmapDrawable blurDrawable = new BitmapDrawable(context.getResources(), blurBitmap);
        blurDrawable.setColorFilter(alphaColor, PorterDuff.Mode.SRC_ATOP);

        return blurDrawable;
    }

    public static Bitmap blur(Context ctx, Bitmap image) {
        int width = Math.round(image.getWidth() * BITMAP_SCALE);
        int height = Math.round(image.getHeight() * BITMAP_SCALE);

        // Create bitmaps.
        Bitmap inputBitmap = Bitmap.createScaledBitmap(image, width, height, false);
        Bitmap outputBitmap = Bitmap.createBitmap(inputBitmap);

        // Apply blur.
        RenderScript rs = RenderScript.create(ctx);
        ScriptIntrinsicBlur theIntrinsic = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs));
        Allocation tmpIn = Allocation.createFromBitmap(rs, inputBitmap);
        Allocation tmpOut = Allocation.createFromBitmap(rs, outputBitmap);
        theIntrinsic.setRadius(BLUR_RADIUS);
        theIntrinsic.setInput(tmpIn);
        theIntrinsic.forEach(tmpOut);
        tmpOut.copyTo(outputBitmap);

        return outputBitmap;
    }

    private static Bitmap getScreenshot(Context context, View view) {
        // Build bitmap from view.
        view.setDrawingCacheEnabled(true);
        Bitmap bitmap = Bitmap.createBitmap(view.getDrawingCache());

        // Remove status bar.
        if (view.findViewById(Window.ID_ANDROID_CONTENT) != null) {
            int top = (int) Math.ceil(25 * context.getResources().getDisplayMetrics().density);
            bitmap = Bitmap.createBitmap(bitmap, 0, top, bitmap.getWidth(), bitmap.getHeight() - top);
        }

        // Clear drawing cache.
        view.setDrawingCacheEnabled(false);

        return bitmap;
    }

}
