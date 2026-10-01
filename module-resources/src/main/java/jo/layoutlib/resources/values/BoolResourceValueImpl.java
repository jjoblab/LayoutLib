package jo.layoutlib.resources.values;

import jo.layoutlib.resources.api.ResourceNamespace;
import jo.layoutlib.resources.api.ResourceType;
import jo.layoutlib.resources.api.ResourceValueImpl;

/**
 * Implémentation d'un BoolResourceValue.
 *
 * @author jo@Dev
 * @since 1.0
 */
public class BoolResourceValueImpl extends ResourceValueImpl implements BoolResourceValue {

    private Boolean cachedBool;

    public BoolResourceValueImpl(ResourceNamespace namespace, String name, String value) {
        super(namespace, ResourceType.BOOL, name, value);
    }

    /**
     * @return la valeur booléenne, ou null si non résolvable
     */
    public Boolean getBoolValue() {
        if (cachedBool != null) {
            return cachedBool;
        }
        String value = getValue();
        if (value == null || value.isEmpty()) {
            return null;
        }
        String lower = value.trim().toLowerCase();
        switch (lower) {
            case "true":
            case "1":
                cachedBool = Boolean.TRUE;
                break;
            case "false":
            case "0":
                cachedBool = Boolean.FALSE;
                break;
            default:
                return null;
        }
        return cachedBool;
    }
}
