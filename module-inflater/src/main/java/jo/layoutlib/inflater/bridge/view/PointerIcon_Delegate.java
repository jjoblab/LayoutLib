package jo.layoutlib.inflater.bridge.view;

import android.view.PointerIcon;
import android.content.Context;

/**
 * Delegate pour PointerIcon_Delegate, utilise l API Android native.
 * Inspiré de com.android.layoutlib.bridge.android.view.PointerIcon_Delegate de l AOSP.
 *
 * @author jo@Dev
 * @since 1.0
 */
public class PointerIcon_Delegate {

    public PointerIcon_Delegate() {
    }

    public static PointerIcon getSystemIcon(android.content.Context ctx, int style) {
        return PointerIcon.getSystemIcon(ctx, style);
    }
}
