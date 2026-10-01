package jo.layoutlib.drawables.delegates;

import android.graphics.drawable.ColorDrawable;

import jo.layoutlib.resources.ColorParser;
import jo.layoutlib.resources.ResourceException;

/**
 * Delegate pour ColorDrawable, inspiré de l AOSP.
 *
 * @author jo@Dev
 * @since 1.0
 */
public class ColorDrawable_Delegate {

    /**
     * Crée un ColorDrawable depuis une chaîne de couleur.
     *
     * @param colorStr la couleur (ex. "#FF6750A4")
     * @return le ColorDrawable
     */
    public static ColorDrawable createFromColor(String colorStr) {
        if (colorStr == null || colorStr.isEmpty()) {
            return null;
        }
        try {
            int color = ColorParser.parse(colorStr);
            return new ColorDrawable(color);
        } catch (ResourceException e) {
            return null;
        }
    }

    /**
     * Récupère la couleur d un ColorDrawable.
     *
     * @param drawable le drawable
     * @return la couleur ARGB
     */
    public static int getColor(ColorDrawable drawable) {
        if (drawable == null) return 0;
        return drawable.getColor();
    }

    /**
     * Définit la couleur d un ColorDrawable.
     *
     * @param drawable le drawable
     * @param color la couleur ARGB
     */
    public static void setColor(ColorDrawable drawable, int color) {
        if (drawable != null) {
            drawable.setColor(color);
        }
    }
}
