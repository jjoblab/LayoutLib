package jo.layoutlib.drawables.delegates;

import android.graphics.drawable.GradientDrawable;

/**
 * Delegate pour GradientDrawable, inspiré de l AOSP.
 *
 * @author jo@Dev
 * @since 1.0
 */
public class GradientDrawable_Delegate {

    /**
     * Configure la forme d un GradientDrawable.
     *
     * @param drawable le drawable
     * @param shape la forme (RECTANGLE, OVAL, LINE, RING)
     */
    public static void setShape(GradientDrawable drawable, int shape) {
        if (drawable != null) {
            drawable.setShape(shape);
        }
    }

    /**
     * Configure la couleur unie.
     *
     * @param drawable le drawable
     * @param color la couleur ARGB
     */
    public static void setSolidColor(GradientDrawable drawable, int color) {
        if (drawable != null) {
            drawable.setColor(color);
        }
    }

    /**
     * Configure le stroke.
     *
     * @param drawable le drawable
     * @param width épaisseur en pixels
     * @param color couleur du trait
     */
    public static void setStroke(GradientDrawable drawable, int width, int color) {
        if (drawable != null) {
            drawable.setStroke(width, color);
        }
    }

    /**
     * Configure les coins arrondis.
     *
     * @param drawable le drawable
     * @param radius rayon en pixels
     */
    public static void setCornerRadius(GradientDrawable drawable, float radius) {
        if (drawable != null) {
            drawable.setCornerRadius(radius);
        }
    }
}
