package jo.layoutlib.inflater.bridge.bars;

/**
 * Barre de statut système, utilise l API Android native.
 * Inspiré de com.android.layoutlib.bridge.bars.StatusBar de l AOSP.
 *
 * @author jo@Dev
 * @since 1.0
 */
public class StatusBar {

    public static final int DEFAULT_HEIGHT_DP = 24;

    public StatusBar() {
    }

    public static int getHeightDp() {
        return DEFAULT_HEIGHT_DP;
    }

    public static int getBackgroundColor(boolean nightMode) {
        return nightMode ? 0xFF000000 : 0xFFFFFFFF;
    }
}
