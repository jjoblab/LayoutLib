package jo.layoutlib.drawables.ninepatch;

/**
 * Chunk de NinePatch (métadonnées de stretch), inspiré de l AOSP.
 *
 * @author jo@Dev
 * @since 1.0
 */
public class NinePatchChunk {

    private int[] xDividers;
    private int[] yDividers;
    private int[] colors;
    private boolean paddingAvailable;
    private int paddingLeft;
    private int paddingRight;
    private int paddingTop;
    private int paddingBottom;

    public NinePatchChunk() {
    }

    public int[] getXDividers() {
        return xDividers;
    }

    public void setXDividers(int[] xDividers) {
        this.xDividers = xDividers;
    }

    public int[] getYDividers() {
        return yDividers;
    }

    public void setYDividers(int[] yDividers) {
        this.yDividers = yDividers;
    }

    public int[] getColors() {
        return colors;
    }

    public void setColors(int[] colors) {
        this.colors = colors;
    }

    public boolean isPaddingAvailable() {
        return paddingAvailable;
    }

    public void setPaddingAvailable(boolean paddingAvailable) {
        this.paddingAvailable = paddingAvailable;
    }

    public int getPaddingLeft() {
        return paddingLeft;
    }

    public void setPaddingLeft(int paddingLeft) {
        this.paddingLeft = paddingLeft;
    }

    public int getPaddingRight() {
        return paddingRight;
    }

    public void setPaddingRight(int paddingRight) {
        this.paddingRight = paddingRight;
    }

    public int getPaddingTop() {
        return paddingTop;
    }

    public void setPaddingTop(int paddingTop) {
        this.paddingTop = paddingTop;
    }

    public int getPaddingBottom() {
        return paddingBottom;
    }

    public void setPaddingBottom(int paddingBottom) {
        this.paddingBottom = paddingBottom;
    }
}
