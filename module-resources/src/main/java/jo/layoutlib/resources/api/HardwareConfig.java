package jo.layoutlib.resources.api;

/**
 * Configuration matérielle simulée.
 *
 * @author jo@Dev
 * @since 1.0
 */
public class HardwareConfig {

    private final int screenWidth;
    private final int screenHeight;
    private final int densityDpi;
    private final float xdpi;
    private final float ydpi;
    private final int orientation;

    public HardwareConfig(int screenWidth, int screenHeight, int densityDpi,
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
