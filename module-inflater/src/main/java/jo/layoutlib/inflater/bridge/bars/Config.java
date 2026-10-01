package jo.layoutlib.inflater.bridge.bars;

/**
 * Configuration des bars système (status bar, navigation bar), inspiré de
 * com.android.layoutlib.bridge.bars.Config de l AOSP.
 *
 * @author jo@Dev
 * @since 1.0
 */
public class Config {

    private boolean showStatusBar = true;
    private boolean showNavigationBar = true;
    private boolean showTitleBar = false;
    private boolean darkMode = false;
    private int apiLevel = 34;

    public boolean isShowStatusBar() {
        return showStatusBar;
    }

    public void setShowStatusBar(boolean showStatusBar) {
        this.showStatusBar = showStatusBar;
    }

    public boolean isShowNavigationBar() {
        return showNavigationBar;
    }

    public void setShowNavigationBar(boolean showNavigationBar) {
        this.showNavigationBar = showNavigationBar;
    }

    public boolean isShowTitleBar() {
        return showTitleBar;
    }

    public void setShowTitleBar(boolean showTitleBar) {
        this.showTitleBar = showTitleBar;
    }

    public boolean isDarkMode() {
        return darkMode;
    }

    public void setDarkMode(boolean darkMode) {
        this.darkMode = darkMode;
    }

    public int getApiLevel() {
        return apiLevel;
    }

    public void setApiLevel(int apiLevel) {
        this.apiLevel = apiLevel;
    }

    /**
     * @return la hauteur totale des bars en dp
     */
    public int getTotalBarsHeightDp() {
        int height = 0;
        if (showStatusBar) height += StatusBar.DEFAULT_HEIGHT_DP;
        if (showNavigationBar) height += NavigationBar.DEFAULT_HEIGHT_DP;
        if (showTitleBar) height += TitleBar.DEFAULT_HEIGHT_DP;
        return height;
    }
}
