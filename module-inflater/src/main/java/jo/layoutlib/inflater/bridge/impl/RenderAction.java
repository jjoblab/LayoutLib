package jo.layoutlib.inflater.bridge.impl;

import android.content.Context;

/**
 * Action de rendu avec lifecycle, inspiré de com.android.layoutlib.bridge.impl.RenderAction.
 *
 * @author jo@Dev
 * @since 1.0
 */
public class RenderAction {

    private final Context context;
    private boolean initialized = false;
    private boolean acquired = false;

    public RenderAction(Context context) {
        this.context = context;
    }

    public boolean init(long timeout) {
        this.initialized = true;
        return true;
    }

    public boolean acquire(long timeout) {
        if (!initialized) {
            throw new IllegalStateException("init() doit être appelé avant acquire()");
        }
        this.acquired = true;
        return true;
    }

    public void release() {
        this.acquired = false;
        this.initialized = false;
    }

    public boolean isInitialized() { return initialized; }
    public boolean isAcquired() { return acquired; }
    public Context getContext() { return context; }
}
