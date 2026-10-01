package jo.layoutlib.resources.values;

import jo.layoutlib.resources.api.ResourceReference;
import jo.layoutlib.resources.api.ResourceUrl;
import jo.layoutlib.resources.api.ResourceValue;

/**
 * Résolveur de ResourceValue avec support des références chainables.
 * Suit les références @type/name jusqu à une valeur littérale.
 *
 * @author jo@Dev
 * @since 1.0
 */
public class ResourceValueResolver {

    private static final int MAX_DEPTH = 20;

    private final ResourceValueMap map;

    public ResourceValueResolver(ResourceValueMap map) {
        this.map = map;
    }

    /**
     * Résout une ResourceValue en suivant les références.
     *
     * @param value la valeur à résoudre
     * @return la valeur résolue (finale), ou null
     */
    public ResourceValue resolve(ResourceValue value) {
        return resolve(value, 0);
    }

    private ResourceValue resolve(ResourceValue value, int depth) {
        if (value == null || depth > MAX_DEPTH) {
            return null;
        }
        String raw = value.getValue();
        if (raw == null || raw.isEmpty()) {
            return value;
        }
        if (!raw.startsWith("@")) {
            return value;
        }
        ResourceUrl url = ResourceUrl.parse(raw);
        if (url == null || url.isCreate() || url.isThemeAttr()) {
            return value;
        }
        ResourceReference ref = url.toReference();
        ResourceValue next = map.get(ref);
        if (next == null) {
            return null;
        }
        return resolve(next, depth + 1);
    }

    /**
     * Indique si une valeur est une référence chainable.
     *
     * @param value la valeur
     * @return true si c est une référence @type/name
     */
    public static boolean isChainable(ResourceValue value) {
        if (value == null) return false;
        String raw = value.getValue();
        return raw != null && raw.startsWith("@");
    }
}
