package jo.layoutlib.resources.api;

import android.content.Context;
import android.content.res.Resources;

/**
 * Point d'entrée principal du bridge, inspiré de com.android.layoutlib.bridge.Bridge.
 * Utilise android.content.Context pour l'accès aux resources natives.
 *
 * @author jo@Dev
 * @since 1.0
 */
public class Bridge {

    private static Bridge instance;
    private Context context;
    private boolean initialized = false;

    public static final String VERSION = "1.0.0";

    private Bridge() {
    }

    public static synchronized Bridge getInstance() {
        if (instance == null) {
            instance = new Bridge();
        }
        return instance;
    }

    public boolean init(Context context) {
        this.context = context;
        this.initialized = true;
        return true;
    }

    public void cleanup() {
        this.initialized = false;
        this.context = null;
    }

    public boolean isInitialized() {
        return initialized;
    }

    public Context getContext() {
        return context;
    }

    public Resources getResources() {
        return context != null ? context.getResources() : null;
    }
}
