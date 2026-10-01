package jo.layoutlib.inflater.bridge.bars;

/**
 * Bar custom générique, inspiré de l AOSP.
 *
 * @author jo@Dev
 * @since 1.0
 */
public class CustomBar {

    private String name;
    private int heightDp;
    private int backgroundColor;
    private boolean visible = true;

    public CustomBar(String name, int heightDp, int backgroundColor) {
        this.name = name;
        this.heightDp = heightDp;
        this.backgroundColor = backgroundColor;
    }

    public String getName() {
        return name;
    }

    public int getHeightDp() {
        return heightDp;
    }

    public void setHeightDp(int heightDp) {
        this.heightDp = heightDp;
    }

    public int getBackgroundColor() {
        return backgroundColor;
    }

    public void setBackgroundColor(int backgroundColor) {
        this.backgroundColor = backgroundColor;
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }
}
