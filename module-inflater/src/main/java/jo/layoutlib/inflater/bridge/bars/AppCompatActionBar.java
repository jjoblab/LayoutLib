package jo.layoutlib.inflater.bridge.bars;

/**
 * ActionBar AppCompat, utilise l API Android native.
 * Inspiré de com.android.layoutlib.bridge.bars.AppCompatActionBar de l AOSP.
 *
 * @author jo@Dev
 * @since 1.0
 */
public class AppCompatActionBar {

    public static final int DEFAULT_HEIGHT_DP = 56;
    

    public AppCompatActionBar() {
    }

    public static int getHeightDp() {
        return DEFAULT_HEIGHT_DP;
    }
}
