package jo.layoutlib.inflater.bridge.impl;

import android.view.WindowInsets;

/**
 * Overlay système (status bar, nav bar) au-dessus du layout, inspiré de l'AOSP.
 * Utilise android.view.WindowInsets natif.
 *
 * @author jo@Dev
 * @since 1.0
 */
public class SysUiOverlay {

    private int statusBarHeight;
    private int navigationBarHeight;
    private boolean hasStatusBar;
    private boolean hasNavigationBar;

    public SysUiOverlay() {
    }

    public void initFromInsets(WindowInsets insets) {
        if (insets != null) {
            this.statusBarHeight = insets.getSystemWindowInsetTop();
            this.navigationBarHeight = insets.getSystemWindowInsetBottom();
            this.hasStatusBar = statusBarHeight > 0;
            this.hasNavigationBar = navigationBarHeight > 0;
        }
    }

    public int getStatusBarHeight() { return statusBarHeight; }
    public int getNavigationBarHeight() { return navigationBarHeight; }
    public boolean hasStatusBar() { return hasStatusBar; }
    public boolean hasNavigationBar() { return hasNavigationBar; }
    public int getTotalHeight() { return statusBarHeight + navigationBarHeight; }
}
