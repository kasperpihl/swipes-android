package com.swipesapp.android.util;

import android.graphics.Color;

/**
 * Utilitary class for color operations.
 *
 * @author Fernanda Bari
 */
public class ColorUtils {

    /**
     * Blends two colors to a given ratio.
     *
     * @param from  Initial color.
     * @param to    Final color.
     * @param ratio Ratio to calculate blend.
     * @return Blended color.
     */
    public static int blendColors(int from, int to, float ratio) {
        final float inverseRatio = 1f - ratio;

        final float r = Color.red(to) * ratio + Color.red(from) * inverseRatio;
        final float g = Color.green(to) * ratio + Color.green(from) * inverseRatio;
        final float b = Color.blue(to) * ratio + Color.blue(from) * inverseRatio;

        return Color.rgb((int) r, (int) g, (int) b);
    }

}
