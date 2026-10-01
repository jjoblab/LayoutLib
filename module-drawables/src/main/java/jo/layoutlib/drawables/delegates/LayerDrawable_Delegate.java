package jo.layoutlib.drawables.delegates;

import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;

/**
 * Delegate pour LayerDrawable, inspiré de l AOSP.
 *
 * @author jo@Dev
 * @since 1.0
 */
public class LayerDrawable_Delegate {

    /**
     * Ajoute un layer à un LayerDrawable.
     *
     * @param drawable le LayerDrawable
     * @param layer le drawable à ajouter
     * @param left inset gauche
     * @param top inset haut
     * @param right inset droit
     * @param bottom inset bas
     */
    public static void addLayer(LayerDrawable drawable, Drawable layer,
                                 int left, int top, int right, int bottom) {
        if (drawable == null || layer == null) return;
        // Pas d API directe pour ajouter un layer après construction
        // Il faudrait reconstruire le LayerDrawable
    }

    /**
     * Récupère le nombre de layers.
     *
     * @param drawable le LayerDrawable
     * @return le nombre de layers
     */
    public static int getLayerCount(LayerDrawable drawable) {
        if (drawable == null) return 0;
        return drawable.getNumberOfLayers();
    }

    /**
     * Récupère un layer par index.
     *
     * @param drawable le LayerDrawable
     * @param index l index
     * @return le drawable du layer
     */
    public static Drawable getLayer(LayerDrawable drawable, int index) {
        if (drawable == null || index < 0 || index >= drawable.getNumberOfLayers()) {
            return null;
        }
        return drawable.getDrawable(index);
    }
}
