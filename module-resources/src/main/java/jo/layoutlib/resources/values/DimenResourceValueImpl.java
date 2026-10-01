package jo.layoutlib.resources.values;

import jo.layoutlib.resources.DimensionConverter;
import jo.layoutlib.resources.ResourceException;
import jo.layoutlib.resources.api.ResourceNamespace;
import jo.layoutlib.resources.api.ResourceType;
import jo.layoutlib.resources.api.ResourceValueImpl;

/**
 * Implémentation d'un DimenResourceValue avec parsing différé.
 *
 * @author jo@Dev
 * @since 1.0
 */
public class DimenResourceValueImpl extends ResourceValueImpl implements DimenResourceValue {

    private final DimensionConverter converter;
    private Float cachedPixels;

    public DimenResourceValueImpl(ResourceNamespace namespace, String name, String value,
                                   DimensionConverter converter) {
        super(namespace, ResourceType.DIMEN, name, value);
        this.converter = converter;
    }

    /**
     * @return la valeur en pixels, ou null si non résolvable
     */
    public Float getPixelValue() {
        if (cachedPixels != null) {
            return cachedPixels;
        }
        String value = getValue();
        if (value == null || value.isEmpty() || value.startsWith("@") || value.startsWith("?")) {
            return null;
        }
        if (converter == null) {
            return null;
        }
        try {
            cachedPixels = converter.toPixels(value);
            return cachedPixels;
        } catch (ResourceException e) {
            return null;
        }
    }
}
