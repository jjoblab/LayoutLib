package jo.layoutlib.resources.values;

import jo.layoutlib.resources.api.ResourceNamespace;
import jo.layoutlib.resources.api.ResourceValue;

/**
 * Resource de couleur, inspiré de l'AOSP.
 * Étend ResourceValue avec des méthodes spécifiques aux couleurs.
 *
 * @author jo@Dev
 * @since 1.0
 */
public interface ColorResourceValue extends ResourceValue {

    /**
     * @return la valeur ARGB, ou null si non résolvable
     */
    Integer getColorValue();
}
