package jo.layoutlib.inflater.bridge.view;

import android.view.MenuInflater;

/**
 * Delegate pour MenuInflater_Delegate, utilise l API Android native.
 * Inspiré de com.android.layoutlib.bridge.android.view.MenuInflater_Delegate de l AOSP.
 *
 * @author jo@Dev
 * @since 1.0
 */
public class MenuInflater_Delegate {

    public MenuInflater_Delegate() {
    }

    public static void inflate(MenuInflater mi, int menuRes, android.view.Menu menu) {
        mi.inflate(menuRes, menu);
    }
}
