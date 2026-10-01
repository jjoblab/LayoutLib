package jo.layoutlib.layout.session;

import android.util.DisplayMetrics;

/**
 * Configuration matérielle, utilise android.util.DisplayMetrics natif.
 * Inspiré de com.android.ide.common.rendering.api.HardwareConfig.
 *
 * @author jo@Dev
 * @since 1.0
 */
public class HardwareConfigImpl {

    private final int screenWidth;
    private final int screenHeight;
    private final int densityDpi;
    private final float xdpi;
    private final float ydpi;
    private final int orientation;

    public HardwareConfigImpl(DisplayMetrics metrics) {
        this.screenWidth = metrics.widthPixels;
        this.screenHeight = metrics.heightPixels;
        this.densityDpi = metrics.densityDpi;
        this.xdpi = metrics.xdpi;
        this.ydpi = metrics.ydpi;
        this.orientation = screenWidth > screenHeight ? 1 : 0; // landscape : portrait
    }

    public HardwareConfigImpl(int screenWidth, int screenHeight, int densityDpi,
                               float xdpi, float ydpi, int orientation) {
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
        this.densityDpi = densityDpi;
        this.xdpi = xdpi;
        this.ydpi = ydpi;
        this.orientation = orientation;
    }

    public int getScreenWidth() { return screenWidth; }
    public int getScreenHeight() { return screenHeight; }
    public int getDensityDpi() { return densityDpi; }
    public float getXdpi() { return xdpi; }
    public float getYdpi() { return ydpi; }
    public int getOrientation() { return orientation; }
    public float getDensity() { return densityDpi / 160.0f; }
}
