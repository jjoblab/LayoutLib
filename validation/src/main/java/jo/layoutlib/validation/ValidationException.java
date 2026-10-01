package jo.layoutlib.validation;

/**
 * Exception levée par le module de validation.
 *
 * @author jo@Dev
 * @since 1.0
 */
public class ValidationException extends RuntimeException {

    /** Nom du layout testé, ou {@code null}. */
    private final String layoutName;

    public ValidationException(String message) {
        super(message);
        this.layoutName = null;
    }

    public ValidationException(String message, Throwable cause) {
        super(message, cause);
        this.layoutName = null;
    }

    public ValidationException(String message, String layoutName) {
        super(message + " (layout : " + layoutName + ")");
        this.layoutName = layoutName;
    }

    public String getLayoutName() {
        return layoutName;
    }
}
