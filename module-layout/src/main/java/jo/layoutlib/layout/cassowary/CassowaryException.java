package jo.layoutlib.layout.cassowary;

/**
 * Exception levée par le solver Cassowary.
 *
 * @author jo@Dev
 * @since 1.0
 */
public class CassowaryException extends RuntimeException {

    public CassowaryException(String message) {
        super(message);
    }

    public CassowaryException(String message, Throwable cause) {
        super(message, cause);
    }
}
