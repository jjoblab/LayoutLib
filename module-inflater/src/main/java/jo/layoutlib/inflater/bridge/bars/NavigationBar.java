package jo.layoutlib.inflater.bridge.bars;

/**
 * Barre de navigation système, utilise l API Android native.
 * Inspiré de com.android.layoutlib.bridge.bars.NavigationBar de l AOSP.
 *
 * @author jo@Dev
 * @since 1.0
 */
public class NavigationBar {

    public static final int DEFAULT_HEIGHT_DP = 48;

    public NavigationBar() {
    }

    public static int getHeightDp() {
        return DEFAULT_HEIGHT_DP;
    }

    public static int getBackgroundColor(boolean nightMode) {
        return nightMode ? 0xFF000000 : 0xFFFFFFFF;
    }
}
