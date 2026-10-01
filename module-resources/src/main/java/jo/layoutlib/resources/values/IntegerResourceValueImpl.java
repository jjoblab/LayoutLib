package jo.layoutlib.resources.values;

import jo.layoutlib.resources.api.ResourceNamespace;
import jo.layoutlib.resources.api.ResourceType;
import jo.layoutlib.resources.api.ResourceValueImpl;

/**
 * Implémentation d'un IntegerResourceValue.
 *
 * @author jo@Dev
 * @since 1.0
 */
public class IntegerResourceValueImpl extends ResourceValueImpl implements IntegerResourceValue {

    private Integer cachedInt;

    public IntegerResourceValueImpl(ResourceNamespace namespace, String name, String value) {
        super(namespace, ResourceType.INTEGER, name, value);
    }

    /**
     * @return la valeur entière, ou null si non résolvable
     */
    public Integer getIntValue() {
        if (cachedInt != null) {
            return cachedInt;
        }
        String value = getValue();
        if (value == null || value.isEmpty()) {
            return null;
        }
        try {
            cachedInt = Integer.parseInt(value.trim());
            return cachedInt;
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
