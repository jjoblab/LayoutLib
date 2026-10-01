package jo.layoutlib.layout.session;

import android.view.View;

/**
 * Action de rendu, inspiré de l AOSP.
 *
 * @author jo@Dev
 * @since 1.0
 */
public class RenderAction {

    private final String name;
    private View rootView;
    private int width;
    private int height;
    private long durationMs;

    public RenderAction(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public View getRootView() {
        return rootView;
    }

    public void setRootView(View rootView) {
        this.rootView = rootView;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public long getDurationMs() {
        return durationMs;
    }

    public void setDurationMs(long durationMs) {
        this.durationMs = durationMs;
    }
}
