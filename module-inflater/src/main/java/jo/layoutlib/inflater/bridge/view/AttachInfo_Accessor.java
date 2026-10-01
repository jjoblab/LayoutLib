package jo.layoutlib.inflater.bridge.view;

import android.view.View;
import android.view.View;

/**
 * Delegate pour AttachInfo_Accessor, utilise l API Android native.
 * Inspiré de com.android.layoutlib.bridge.android.view.AttachInfo_Accessor de l AOSP.
 *
 * @author jo@Dev
 * @since 1.0
 */
public class AttachInfo_Accessor {

    public AttachInfo_Accessor() {
    }

    public static android.graphics.Rect getWindowVisibleDisplayFrame(View view) {
        android.graphics.Rect r = new android.graphics.Rect(); view.getWindowVisibleDisplayFrame(r); return r;
    }
}
