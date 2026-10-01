package jo.layoutlib.drawables.ninepatch;

import android.graphics.Bitmap;

/**
 * Utilitaires graphiques pour NinePatch, inspiré de
 * com.android.ninepatch.GraphicsUtilities de l AOSP.
 *
 * @author jo@Dev
 * @since 1.0
 */
public final class GraphicsUtilities {

    private GraphicsUtilities() {
    }

    /**
     * Extrait les dividers X (horizontaux) d un bitmap NinePatch.
     *
     * @param bitmap le bitmap
     * @return les dividers [start1, end1, start2, end2, ...]
     */
    public static int[] extractXDividers(Bitmap bitmap) {
        if (bitmap == null) return new int[0];
        // La dernière ligne contient les dividers X
        int y = bitmap.getHeight() - 1;
        return extractDividers(bitmap, y, true);
    }

    /**
     * Extrait les dividers Y (verticaux) d un bitmap NinePatch.
     *
     * @param bitmap le bitmap
     * @return les dividers
     */
    public static int[] extractYDividers(Bitmap bitmap) {
        if (bitmap == null) return new int[0];
        // La dernière colonne contient les dividers Y
        int x = bitmap.getWidth() - 1;
        return extractDividers(bitmap, x, false);
    }

    private static int[] extractDividers(Bitmap bitmap, int fixedCoord, boolean horizontal) {
        java.util.List<Integer> dividers = new java.util.ArrayList<>();
        int length = horizontal ? bitmap.getWidth() : bitmap.getHeight();
        boolean inDivider = false;
        int start = 0;

        for (int i = 0; i < length; i++) {
            int pixel = horizontal ? bitmap.getPixel(i, fixedCoord) : bitmap.getPixel(fixedCoord, i);
            boolean isBlack = (pixel & 0xFF000000) == 0xFF000000 && (pixel & 0x00FFFFFF) == 0;
            if (isBlack && !inDivider) {
                inDivider = true;
                start = i;
            } else if (!isBlack && inDivider) {
                inDivider = false;
                dividers.add(start);
                dividers.add(i);
            }
        }
        if (inDivider) {
            dividers.add(start);
            dividers.add(length);
        }

        int[] result = new int[dividers.size()];
        for (int i = 0; i < result.length; i++) {
            result[i] = dividers.get(i);
        }
        return result;
    }
}
