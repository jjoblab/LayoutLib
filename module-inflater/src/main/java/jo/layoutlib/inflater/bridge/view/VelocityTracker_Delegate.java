package jo.layoutlib.inflater.bridge.view;

import android.view.VelocityTracker;

/**
 * Delegate pour VelocityTracker_Delegate, utilise l API Android native.
 * Inspiré de com.android.layoutlib.bridge.android.view.VelocityTracker_Delegate de l AOSP.
 *
 * @author jo@Dev
 * @since 1.0
 */
public class VelocityTracker_Delegate {

    public VelocityTracker_Delegate() {
    }

    public static VelocityTracker obtain() {
        return VelocityTracker.obtain();
    }

    public static void computeCurrentVelocity(VelocityTracker vt, int units) {
        vt.computeCurrentVelocity(units);
    }

    public static float getXVelocity(VelocityTracker vt) {
        return vt.getXVelocity();
    }

    public static float getYVelocity(VelocityTracker vt) {
        return vt.getYVelocity();
    }

    public static void recycle(VelocityTracker vt) {
        vt.recycle();
    }
}
