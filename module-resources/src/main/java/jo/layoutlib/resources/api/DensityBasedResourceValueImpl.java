package jo.layoutlib.resources.api;

import jo.layoutlib.resources.api.ResourceNamespace;

/**
 * Implémentation de DensityBasedResourceValue.
 *
 * @author jo@Dev
 * @since 1.0
 */
public class DensityBasedResourceValueImpl extends ResourceValueImpl implements DensityBasedResourceValue {

    public DensityBasedResourceValueImpl(ResourceNamespace namespace, ResourceType type, String name, String value) {
        super(namespace, type, name, value);
    }
}
