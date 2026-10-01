package jo.layoutlib.resources.api;

import jo.layoutlib.resources.api.ResourceNamespace;

/**
 * Implémentation de ArrayResourceValue.
 *
 * @author jo@Dev
 * @since 1.0
 */
public class ArrayResourceValueImpl extends ResourceValueImpl implements ArrayResourceValue {

    public ArrayResourceValueImpl(ResourceNamespace namespace, ResourceType type, String name, String value) {
        super(namespace, type, name, value);
    }
}
