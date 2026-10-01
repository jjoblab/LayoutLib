package jo.layoutlib.layout;

/**
 * Exception levée par le Module 6 (Layout).
 *
 * @author jo@Dev
 * @since 1.0
 */
public class LayoutException extends RuntimeException {

    public LayoutException(String message) {
        super(message);
    }

    public LayoutException(String message, Throwable cause) {
        super(message, cause);
    }
}
