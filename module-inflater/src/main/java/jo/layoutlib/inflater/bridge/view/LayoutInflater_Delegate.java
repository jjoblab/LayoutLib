package jo.layoutlib.inflater.bridge.view;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * Delegate pour LayoutInflater, utilise l API Android native.
 *
 * @author jo@Dev
 * @since 1.0
 */
public class LayoutInflater_Delegate {

    public LayoutInflater_Delegate() {
    }

    public static LayoutInflater from(Context ctx) {
        return LayoutInflater.from(ctx);
    }

    public static View inflate(LayoutInflater li, int resource, ViewGroup root) {
        return li.inflate(resource, root);
    }
}
