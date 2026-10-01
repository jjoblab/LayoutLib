package jo.layoutlib.resources.api;

/**
 * Implémentation de TextResourceValue.
 *
 * @author jo@Dev
 * @since 1.0
 */
public class TextResourceValueImpl extends ResourceValueImpl implements TextResourceValue {

    public TextResourceValueImpl(ResourceNamespace namespace, String name, String value) {
        super(namespace, ResourceType.STRING, name, value);
    }
}
