package jo.layoutlib.resources.values;

import jo.layoutlib.resources.ColorParser;
import jo.layoutlib.resources.ResourceException;
import jo.layoutlib.resources.api.ResourceNamespace;
import jo.layoutlib.resources.api.ResourceType;
import jo.layoutlib.resources.api.ResourceValueImpl;

/**
 * Implémentation d'un ColorResourceValue.
 * Stocke la couleur comme String et la parse à la demande.
 *
 * @author jo@Dev
 * @since 1.0
 */
public class ColorResourceValueImpl extends ResourceValueImpl implements ColorResourceValue {

    private Integer cachedColor;

    public ColorResourceValueImpl(ResourceNamespace namespace, String name, String value) {
        super(namespace, ResourceType.COLOR, name, value);
    }

    @Override
    public Integer getColorValue() {
        if (cachedColor != null) {
            return cachedColor;
        }
        String value = getValue();
        if (value == null || value.isEmpty() || value.startsWith("@") || value.startsWith("?")) {
            return null;
        }
        try {
            cachedColor = ColorParser.parse(value);
            return cachedColor;
        } catch (ResourceException e) {
            return null;
        }
    }
}
