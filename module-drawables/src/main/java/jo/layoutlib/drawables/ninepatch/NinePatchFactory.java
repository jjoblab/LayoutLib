package jo.layoutlib.drawables.ninepatch;

import android.graphics.Bitmap;

/**
 * Factory pour créer des NinePatch depuis un bitmap, inspiré de l AOSP.
 *
 * @author jo@Dev
 * @since 1.0
 */
public final class NinePatchFactory {

    private NinePatchFactory() {
    }

    /**
     * Crée un NinePatch depuis un bitmap.
     *
     * @param bitmap le bitmap source (avec bordure noire NinePatch)
     * @return le NinePatch, ou null si invalide
     */
    public static NinePatch createNinePatch(Bitmap bitmap) {
        if (bitmap == null || bitmap.getWidth() < 2 || bitmap.getHeight() < 2) {
            return null;
        }
        int[] xDividers = GraphicsUtilities.extractXDividers(bitmap);
        int[] yDividers = GraphicsUtilities.extractYDividers(bitmap);
        return new NinePatch(bitmap.getWidth(), bitmap.getHeight(), xDividers, yDividers);
    }

    /**
     * Indique si un bitmap est un NinePatch (a une bordure noire).
     *
     * @param bitmap le bitmap
     * @return true si c est un NinePatch
     */
    public static boolean isNinePatch(Bitmap bitmap) {
        if (bitmap == null || bitmap.getWidth() < 2 || bitmap.getHeight() < 2) {
            return false;
        }
        // Vérifie la dernière ligne et dernière colonne
        int lastY = bitmap.getHeight() - 1;
        int lastX = bitmap.getWidth() - 1;
        for (int x = 0; x < bitmap.getWidth(); x++) {
            int pixel = bitmap.getPixel(x, lastY);
            if ((pixel & 0xFF000000) == 0xFF000000 && (pixel & 0x00FFFFFF) == 0) {
                return true;
            }
        }
        for (int y = 0; y < bitmap.getHeight(); y++) {
            int pixel = bitmap.getPixel(lastX, y);
            if ((pixel & 0xFF000000) == 0xFF000000 && (pixel & 0x00FFFFFF) == 0) {
                return true;
            }
        }
        return false;
    }
}
