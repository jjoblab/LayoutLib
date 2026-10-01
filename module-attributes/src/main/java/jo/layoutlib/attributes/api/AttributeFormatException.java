package jo.layoutlib.attributes.api;

/**
 * AttributeFormatException.
 *
 * @author jo@Dev
 * @since 1.0
 */
public class AttributeFormatException extends RuntimeException {

    public AttributeFormatException(String message) {
        super(message);
    }

    public AttributeFormatException(String message, Throwable cause) {
        super(message, cause);
    }
}
