package jo.layoutlib.inflater.bridge.android;

/**
 * Représente une valeur de resource non résolue, inspiré de l AOSP.
 *
 * <p>Utilisé quand une référence ne peut pas être résolue immédiatement
 * (par exemple, dépend d un thème qui n est pas encore chargé).</p>
 *
 * @author jo@Dev
 * @since 1.0
 */
public class UnresolvedResourceValue {

    private final String rawValue;
    private final String namespace;
    private final String attributeName;
    private boolean resolved = false;
    private Object resolvedValue;

    public UnresolvedResourceValue(String rawValue, String namespace, String attributeName) {
        this.rawValue = rawValue;
        this.namespace = namespace;
        this.attributeName = attributeName;
    }

    public String getRawValue() {
        return rawValue;
    }

    public String getNamespace() {
        return namespace;
    }

    public String getAttributeName() {
        return attributeName;
    }

    public boolean isResolved() {
        return resolved;
    }

    public Object getResolvedValue() {
        return resolvedValue;
    }

    public void setResolvedValue(Object value) {
        this.resolvedValue = value;
        this.resolved = true;
    }

    @Override
    public String toString() {
        if (resolved) {
            return rawValue + " -> " + resolvedValue;
        }
        return rawValue + " (unresolved)";
    }
}
