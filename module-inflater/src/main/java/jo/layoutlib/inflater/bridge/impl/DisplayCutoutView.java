package jo.layoutlib.inflater.bridge.impl;

import android.os.Build;
import android.view.DisplayCutout;
import android.view.WindowInsets;


/**
 * Vue représentant le cutout de l'écran (notch), inspiré de l'AOSP.
 * Utilise android.view.DisplayCutout natif.
 *
 * @author jo@Dev
 * @since 1.0
 */
public class DisplayCutoutView {

    private DisplayCutout cutout;
    private int safeInsetLeft;
    private int safeInsetTop;
    private int safeInsetRight;
    private int safeInsetBottom;

    public DisplayCutoutView() {
    }

    /**
     * Initialise depuis les WindowInsets.
     *
     * <p>{@code getDisplayCutout()} n existe qu à partir de l API 28 :
     * sur API 24-27, l initialisation est simplement ignorée.</p>
     *
     * @param insets les window insets
     */
    public void initFromInsets(WindowInsets insets) {
        if (insets != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            this.cutout = insets.getDisplayCutout();
            if (cutout != null) {
                this.safeInsetLeft = cutout.getSafeInsetLeft();
                this.safeInsetTop = cutout.getSafeInsetTop();
                this.safeInsetRight = cutout.getSafeInsetRight();
                this.safeInsetBottom = cutout.getSafeInsetBottom();
            }
        }
    }

    public int getSafeInsetLeft() { return safeInsetLeft; }
    public int getSafeInsetTop() { return safeInsetTop; }
    public int getSafeInsetRight() { return safeInsetRight; }
    public int getSafeInsetBottom() { return safeInsetBottom; }

    public boolean hasCutout() {
        return cutout != null;
    }

    public DisplayCutout getCutout() {
        return cutout;
    }
}
