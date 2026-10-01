package jo.layoutlib.inflater.bridge.view;

import android.view.View;
import android.view.ViewGroup;

/**
 * WindowManagerGlobal_Delegate - utility class using native Android API.
 *
 * @author jo@Dev
 * @since 1.0
 */
public class WindowManagerGlobal_Delegate {

    public WindowManagerGlobal_Delegate() {
    }

    /**
     * Utility method to check if a view is attached.
     *
     * @param view the view
     * @return true if attached
     */
    public static boolean isAttached(View view) {
        return view != null && view.isAttachedToWindow();
    }
}
