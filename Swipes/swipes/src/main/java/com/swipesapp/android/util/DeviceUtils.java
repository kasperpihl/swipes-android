package com.swipesapp.android.util;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Configuration;

import com.swipesapp.android.R;

/**
 * Utilitary class for gathering info on the running device.
 */
public class DeviceUtils {

    /**
     * Checks the device's form factor.
     *
     * @param context Context instance.
     * @return True if it's a tablet.
     */
    public static boolean isTablet(Context context) {
        return context.getResources().getBoolean(R.bool.tablet);
    }

    /**
     * Checks the device's current screen orientation.
     *
     * @param context Context instance.
     * @return True if in landscape mode.
     */
    public static boolean isLandscape(Context context) {
        boolean isLandscape = false;
        if (context != null) {
            Configuration config = context.getResources().getConfiguration();
            isLandscape = config.orientation == Configuration.ORIENTATION_LANDSCAPE;
        }
        return isLandscape;
    }

    /**
     * Checks the device's form factor and screen orientation.
     *
     * @param context Context instance.
     * @return True if it's a tablet in portrait mode.
     */
    public static boolean isTabletPortrait(Context context) {
        return isTablet(context) && !isLandscape(context);
    }

    /**
     * Checks if an app is installed.
     *
     * @param packageName Package name of the app.
     * @param context     Context instance.
     * @return True if it's installed.
     */
    public static boolean isAppInstalled(String packageName, Context context) {
        try {
            PackageManager packageManager = context.getPackageManager();
            packageManager.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

}
