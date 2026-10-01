package jo.layoutlib.inflater.bridge.impl;

import android.view.DisplayCutout;
import android.view.View;
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
     * @param insets les window insets
     */
    public void initFromInsets(WindowInsets insets) {
        if (insets != null) {
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
