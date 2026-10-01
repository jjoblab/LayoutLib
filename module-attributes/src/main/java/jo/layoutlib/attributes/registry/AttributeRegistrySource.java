package jo.layoutlib.attributes.registry;

import jo.layoutlib.attributes.api.AttributeDefinitionImpl;

/**
 * Interface commune pour tous les registres d'attributs.
 *
 * @author jo@Dev
 * @since 1.0
 */
public interface AttributeRegistrySource {

    /**
     * Récupère un attribut par nom.
     *
     * @param name le nom
     * @return la définition, ou null
     */
    AttributeDefinitionImpl getAttribute(String name);

    /**
     * Indique si un attribut existe.
     *
     * @param name le nom
     * @return true si existe
     */
    boolean hasAttribute(String name);

    /**
     * @return le nombre d'attributs
     */
    int size();
}
