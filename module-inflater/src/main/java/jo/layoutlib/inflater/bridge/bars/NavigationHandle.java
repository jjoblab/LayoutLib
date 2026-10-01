package jo.layoutlib.inflater.bridge.bars;

/**
 * Handle de navigation gestuelle (Android 10+), inspiré de l AOSP.
 *
 * @author jo@Dev
 * @since 1.0
 */
public class NavigationHandle {

    public static final int DEFAULT_WIDTH_DP = 48;
    public static final int DEFAULT_HEIGHT_DP = 4;

    private int widthDp = DEFAULT_WIDTH_DP;
    private int heightDp = DEFAULT_HEIGHT_DP;
    private int color = 0xFF000000;

    public int getWidthDp() {
        return widthDp;
    }

    public void setWidthDp(int widthDp) {
        this.widthDp = widthDp;
    }

    public int getHeightDp() {
        return heightDp;
    }

    public void setHeightDp(int heightDp) {
        this.heightDp = heightDp;
    }

    public int getColor() {
        return color;
    }

    public void setColor(int color) {
        this.color = color;
    }
}
