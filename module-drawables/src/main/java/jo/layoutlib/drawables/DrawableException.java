package jo.layoutlib.drawables;

/**
 * Exception levée par le Module 3 (Drawables) lorsqu'une erreur survient
 * pendant le parsing ou la création d'un drawable.
 *
 * @author jo@Dev
 * @since 1.0
 */
public class DrawableException extends RuntimeException {

    /** Type de drawable concerné, ou {@code null}. */
    private final String drawableType;

    /**
     * Construit une exception avec un message.
     *
     * @param message description de l'erreur
     */
    public DrawableException(String message) {
        super(message);
        this.drawableType = null;
    }

    /**
     * Construit une exception avec un message et une cause.
     *
     * @param message description
     * @param cause   exception cause
     */
    public DrawableException(String message, Throwable cause) {
        super(message, cause);
        this.drawableType = null;
    }

    /**
     * Construit une exception avec un type de drawable.
     *
     * @param message      description
     * @param drawableType type (shape, selector, vector, ...)
     */
    public DrawableException(String message, String drawableType) {
        super(message + " (type : " + drawableType + ")");
        this.drawableType = drawableType;
    }


    /**
     * Construit une exception avec message, contexte et cause.
     *
     * @param message description
     * @param drawableType contexte (nom du style/drawable/...)
     * @param cause   exception cause
     */
    public DrawableException(String message, String drawableType, Throwable cause) {
        super(message + " (type : " + drawableType + ")", cause);
        this.drawableType = drawableType;
    }

    /**
     * @return le type de drawable, ou {@code null}
     */
    public String getDrawableType() {
        return drawableType;
    }
}
