package jo.layoutlib.themes.api;

import jo.layoutlib.resources.api.ResourceNamespace;
import jo.layoutlib.resources.api.ResourceReference;
import jo.layoutlib.resources.api.ResourceType;

/**
 * Implémentation de ResourceReference.
 *
 * @author jo@Dev
 * @since 1.0
 */
public class ResourceReferenceImpl extends ResourceReference {

    public ResourceReferenceImpl(ResourceNamespace namespace, ResourceType type, String name) {
        super(namespace, type, name);
    }
}
