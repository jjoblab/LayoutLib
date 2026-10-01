package jo.layoutlib.drawables.delegates;

import android.graphics.drawable.Drawable;

/**
 * Delegate pour Drawable, inspiré de l AOSP.
 *
 * <p>Le pattern delegate permet de remplacer les méthodes natives de
 * android.graphics.drawable.Drawable par des implémentations JVM.</p>
 *
 * @author jo@Dev
 * @since 1.0
 */
public class Drawable_Delegate {

    /**
     * Delegate pour getOpacity().
     *
     * @param drawable le drawable
     * @return l opacité (PixelFormat.UNKNOWN par défaut)
     */
    public static int getOpacity(Drawable drawable) {
        if (drawable == null) {
            return 0;  // PixelFormat.UNKNOWN
        }
        return drawable.getOpacity();
    }

    /**
     * Delegate pour getIntrinsicWidth().
     *
     * @param drawable le drawable
     * @return la largeur intrinsèque, ou -1
     */
    public static int getIntrinsicWidth(Drawable drawable) {
        if (drawable == null) return -1;
        return drawable.getIntrinsicWidth();
    }

    /**
     * Delegate pour getIntrinsicHeight().
     *
     * @param drawable le drawable
     * @return la hauteur intrinsèque, ou -1
     */
    public static int getIntrinsicHeight(Drawable drawable) {
        if (drawable == null) return -1;
        return drawable.getIntrinsicHeight();
    }

    /**
     * Indique si un drawable est animé.
     *
     * @param drawable le drawable
     * @return true si animé
     */
    public static boolean isStateful(Drawable drawable) {
        if (drawable == null) return false;
        return drawable.isStateful();
    }
}
