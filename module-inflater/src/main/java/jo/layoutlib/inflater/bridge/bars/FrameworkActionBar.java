package jo.layoutlib.inflater.bridge.bars;

/**
 * ActionBar framework, utilise l API Android native.
 * Inspiré de com.android.layoutlib.bridge.bars.FrameworkActionBar de l AOSP.
 *
 * @author jo@Dev
 * @since 1.0
 */
public class FrameworkActionBar {

    public static final int DEFAULT_HEIGHT_DP = 56;
    

    public FrameworkActionBar() {
    }

    public static int getHeightDp() {
        return DEFAULT_HEIGHT_DP;
    }
}
