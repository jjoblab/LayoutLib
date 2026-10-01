package jo.layoutlib.inflater.bridge.view;

import android.view.ViewConfiguration;
import android.content.Context;

/**
 * Delegate pour ViewConfiguration_Accessor, utilise l API Android native.
 * Inspiré de com.android.layoutlib.bridge.android.view.ViewConfiguration_Accessor de l AOSP.
 *
 * @author jo@Dev
 * @since 1.0
 */
public class ViewConfiguration_Accessor {

    public ViewConfiguration_Accessor() {
    }

    public static ViewConfiguration get(android.content.Context ctx) {
        return ViewConfiguration.get(ctx);
    }

    public static int getScaledTouchSlop(ViewConfiguration config) {
        return config.getScaledTouchSlop();
    }

    public static int getScaledDoubleTapSlop(ViewConfiguration config) {
        return config.getScaledDoubleTapSlop();
    }
}
