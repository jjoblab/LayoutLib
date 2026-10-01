package jo.layoutlib.inflater.bridge.view;

import android.view.View;
import android.view.View;

/**
 * Delegate pour View_Delegate, utilise l API Android native.
 * Inspiré de com.android.layoutlib.bridge.android.view.View_Delegate de l AOSP.
 *
 * @author jo@Dev
 * @since 1.0
 */
public class View_Delegate {

    public View_Delegate() {
    }

    public static void setBackground(View view, int color) {
        view.setBackgroundColor(color);
    }

    public static void setVisibility(View view, int visibility) {
        view.setVisibility(visibility);
    }

    public static void setEnabled(View view, boolean enabled) {
        view.setEnabled(enabled);
    }

    public static void setPadding(View view, int l, int t, int r, int b) {
        view.setPadding(l, t, r, b);
    }

    public static int getMeasuredWidth(View view) {
        return view.getMeasuredWidth();
    }

    public static int getMeasuredHeight(View view) {
        return view.getMeasuredHeight();
    }
}
