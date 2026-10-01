package jo.layoutlib.attributes;

/**
 * Exception levée par le Module 5 (Attributes).
 *
 * @author jo@Dev
 * @since 1.0
 */
public class AttributeException extends RuntimeException {

    /** Nom de l'attribut concerné, ou {@code null}. */
    private final String attributeName;

    public AttributeException(String message) {
        super(message);
        this.attributeName = null;
    }

    public AttributeException(String message, Throwable cause) {
        super(message, cause);
        this.attributeName = null;
    }

    public AttributeException(String message, String attributeName) {
        super(message + " (attribut : " + attributeName + ")");
        this.attributeName = attributeName;
    }

    public String getAttributeName() {
        return attributeName;
    }
}
