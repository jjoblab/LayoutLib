package jo.layoutlib.attributes.api;

/**
 * AttributeParseException.
 *
 * @author jo@Dev
 * @since 1.0
 */
public class AttributeParseException extends RuntimeException {

    public AttributeParseException(String message) {
        super(message);
    }

    public AttributeParseException(String message, Throwable cause) {
        super(message, cause);
    }
}
