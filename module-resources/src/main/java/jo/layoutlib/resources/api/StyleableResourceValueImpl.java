package jo.layoutlib.resources.api;

import jo.layoutlib.resources.api.ResourceNamespace;

/**
 * Implémentation de StyleableResourceValue.
 *
 * @author jo@Dev
 * @since 1.0
 */
public class StyleableResourceValueImpl extends ResourceValueImpl implements StyleableResourceValue {

    public StyleableResourceValueImpl(ResourceNamespace namespace, ResourceType type, String name, String value) {
        super(namespace, type, name, value);
    }
}
