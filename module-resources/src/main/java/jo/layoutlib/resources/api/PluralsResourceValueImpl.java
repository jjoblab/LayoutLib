package jo.layoutlib.resources.api;

import jo.layoutlib.resources.api.ResourceNamespace;

/**
 * Implémentation de PluralsResourceValue.
 *
 * @author jo@Dev
 * @since 1.0
 */
public class PluralsResourceValueImpl extends ResourceValueImpl implements PluralsResourceValue {

    public PluralsResourceValueImpl(ResourceNamespace namespace, ResourceType type, String name, String value) {
        super(namespace, type, name, value);
    }
}
