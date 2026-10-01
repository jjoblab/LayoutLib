package jo.layoutlib.inflater.bridge.bars;

/**
 * Barre de titre, utilise l API Android native.
 * Inspiré de com.android.layoutlib.bridge.bars.TitleBar de l AOSP.
 *
 * @author jo@Dev
 * @since 1.0
 */
public class TitleBar {

    public static final int DEFAULT_HEIGHT_DP = 32;
    

    public TitleBar() {
    }

    public static int getHeightDp() {
        return DEFAULT_HEIGHT_DP;
    }
}
