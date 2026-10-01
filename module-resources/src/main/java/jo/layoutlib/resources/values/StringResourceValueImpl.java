package jo.layoutlib.resources.values;

import jo.layoutlib.resources.api.ResourceNamespace;
import jo.layoutlib.resources.api.ResourceType;
import jo.layoutlib.resources.api.ResourceValueImpl;

/**
 * Implémentation d'un StringResourceValue.
 *
 * @author jo@Dev
 * @since 1.0
 */
public class StringResourceValueImpl extends ResourceValueImpl implements StringResourceValue {

    public StringResourceValueImpl(ResourceNamespace namespace, String name, String value) {
        super(namespace, ResourceType.STRING, name, value);
    }
}
