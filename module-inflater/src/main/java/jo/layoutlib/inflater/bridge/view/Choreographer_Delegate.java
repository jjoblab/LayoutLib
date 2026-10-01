package jo.layoutlib.inflater.bridge.view;

import android.view.Choreographer;

/**
 * Delegate pour Choreographer_Delegate, utilise l API Android native.
 * Inspiré de com.android.layoutlib.bridge.android.view.Choreographer_Delegate de l AOSP.
 *
 * @author jo@Dev
 * @since 1.0
 */
public class Choreographer_Delegate {

    public Choreographer_Delegate() {
    }

    public static Choreographer getInstance() {
        return Choreographer.getInstance();
    }

    public static void postFrameCallback(Choreographer c, android.view.Choreographer.FrameCallback cb) {
        c.postFrameCallback(cb);
    }
}
