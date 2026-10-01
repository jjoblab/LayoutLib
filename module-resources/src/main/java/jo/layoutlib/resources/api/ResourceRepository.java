package jo.layoutlib.resources.api;

/**
 * Interface pour un repository de resources, inspiré de l'AOSP.
 *
 * @author jo@Dev
 * @since 1.0
 */
public interface ResourceRepository {

    /**
     * Récupère une resource par référence.
     *
     * @param ref la référence
     * @return la valeur, ou null
     */
    ResourceValue getResourceValue(ResourceReference ref);

    /**
     * @return le namespace du repository
     */
    ResourceNamespace getNamespace();
}
