package jo.layoutlib.inflater.bridge.bars;

/**
 * ActionBar de base du bridge, utilise l API Android native.
 * Inspiré de com.android.layoutlib.bridge.bars.BridgeActionBar de l AOSP.
 *
 * @author jo@Dev
 * @since 1.0
 */
public class BridgeActionBar {

    public static final int DEFAULT_HEIGHT_DP = 56;
    

    public BridgeActionBar() {
    }

    public static int getHeightDp() {
        return DEFAULT_HEIGHT_DP;
    }
}
