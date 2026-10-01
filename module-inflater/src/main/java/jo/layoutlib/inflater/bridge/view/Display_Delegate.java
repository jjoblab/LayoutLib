package jo.layoutlib.inflater.bridge.view;

import android.view.Display;

/**
 * Delegate pour Display_Delegate, utilise l API Android native.
 * Inspiré de com.android.layoutlib.bridge.android.view.Display_Delegate de l AOSP.
 *
 * @author jo@Dev
 * @since 1.0
 */
public class Display_Delegate {

    public Display_Delegate() {
    }

    public static android.graphics.Point getRealSize(Display display) {
        android.graphics.Point p = new android.graphics.Point(); display.getRealSize(p); return p;
    }

    public static int getRotation(Display display) {
        return display.getRotation();
    }
}
