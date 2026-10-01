package jo.layoutlib.inflater.bridge.view;

import android.view.View;
import android.view.ViewGroup;

/**
 * LayoutlibRenderer - utility class using native Android API.
 *
 * @author jo@Dev
 * @since 1.0
 */
public class LayoutlibRenderer {

    public LayoutlibRenderer() {
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
