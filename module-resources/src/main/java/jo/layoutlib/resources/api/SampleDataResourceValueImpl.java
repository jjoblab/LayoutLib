package jo.layoutlib.resources.api;

import jo.layoutlib.resources.api.ResourceNamespace;

/**
 * Implémentation de SampleDataResourceValue.
 *
 * @author jo@Dev
 * @since 1.0
 */
public class SampleDataResourceValueImpl extends ResourceValueImpl implements SampleDataResourceValue {

    public SampleDataResourceValueImpl(ResourceNamespace namespace, ResourceType type, String name, String value) {
        super(namespace, type, name, value);
    }
}
