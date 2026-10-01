package jo.layoutlib.resources.values;

import java.util.Collection;

import jo.layoutlib.resources.api.ResourceReference;
import jo.layoutlib.resources.api.ResourceValue;

/**
 * Map de ResourceValue indexée par ResourceReference.
 * Inspiré de com.android.ide.common.resources.ResourceValueMap de l AOSP.
 *
 * @author jo@Dev
 * @since 1.0
 */
public interface ResourceValueMap {

    /**
     * Ajoute une resource.
     *
     * @param value la resource à ajouter
     */
    void put(ResourceValue value);

    /**
     * Récupère une resource par référence.
     *
     * @param ref la référence
     * @return la resource, ou null
     */
    ResourceValue get(ResourceReference ref);

    /**
     * Récupère une resource par nom.
     *
     * @param name le nom
     * @return la resource, ou null
     */
    ResourceValue get(String name);

    /**
     * @return toutes les resources
     */
    Collection<ResourceValue> values();

    /**
     * @return le nombre de resources
     */
    int size();

    /**
     * Indique si une resource existe.
     *
     * @param name le nom
     * @return true si elle existe
     */
    boolean contains(String name);
}
