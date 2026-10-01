package jo.layoutlib.inflater.bridge.android;

import android.content.Context;

/**
 * Contexte d application simulé, inspiré de l AOSP.
 *
 * @author jo@Dev
 * @since 1.0
 */
public class ApplicationContext {

    private final Context applicationContext;
    private final BridgeContext bridgeContext;

    public ApplicationContext(Context applicationContext) {
        this.applicationContext = applicationContext;
        this.bridgeContext = new BridgeContext(applicationContext);
    }

    public Context getApplicationContext() {
        return applicationContext;
    }

    public BridgeContext getBridgeContext() {
        return bridgeContext;
    }

    public String getPackageName() {
        return applicationContext != null ? applicationContext.getPackageName() : "unknown";
    }
}
