package jo.layoutlib.drawables.ninepatch;

/**
 * Représentation d un NinePatch, inspiré de com.android.ninepatch.NinePatch.
 *
 * <p>Un NinePatch est une image PNG extensible avec des zones de stretch
 * définies par une bordure noire de 1 pixel.</p>
 *
 * @author jo@Dev
 * @since 1.0
 */
public class NinePatch {

    private final int width;
    private final int height;
    private final int[] xDividers;
    private final int[] yDividers;

    public NinePatch(int width, int height, int[] xDividers, int[] yDividers) {
        this.width = width;
        this.height = height;
        this.xDividers = xDividers != null ? xDividers : new int[0];
        this.yDividers = yDividers != null ? yDividers : new int[0];
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int[] getXDividers() {
        return xDividers.clone();
    }

    public int[] getYDividers() {
        return yDividers.clone();
    }

    /**
     * Calcule les coordonnées de stretch pour une taille cible.
     *
     * @param targetSize la taille cible
     * @param dividers les dividers
     * @param sourceSize la taille source
     * @return les coordonnées ajustées
     */
    public static int[] computeStretch(int targetSize, int[] dividers, int sourceSize) {
        if (dividers == null || dividers.length == 0) {
            return new int[]{0, targetSize};
        }
        int fixedSize = 0;
        for (int i = 0; i < dividers.length; i += 2) {
            fixedSize += dividers[i + 1] - dividers[i];
        }
        int stretchSize = targetSize - (sourceSize - fixedSize);
        return new int[]{stretchSize, fixedSize};
    }
}
