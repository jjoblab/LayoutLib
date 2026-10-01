package jo.layoutlib.themes;

/**
 * Exception levée par le Module 4 (Themes) lorsqu'une erreur survient
 * pendant le parsing des thèmes/styles ou la résolution d'un attribut.
 *
 * @author jo@Dev
 * @since 1.0
 */
public class ThemeException extends RuntimeException {

    /** Nom du style ou attribut concerné, ou {@code null}. */
    private final String styleName;

    /**
     * Construit une exception avec un message.
     *
     * @param message description
     */
    public ThemeException(String message) {
        super(message);
        this.styleName = null;
    }

    /**
     * Construit une exception avec message + cause.
     *
     * @param message description
     * @param cause   exception cause
     */
    public ThemeException(String message, Throwable cause) {
        super(message, cause);
        this.styleName = null;
    }

    /**
     * Construit une exception avec contexte de style.
     *
     * @param message   description
     * @param styleName nom du style concerné
     */
    public ThemeException(String message, String styleName) {
        super(message + " (style : " + styleName + ")");
        this.styleName = styleName;
    }


    /**
     * Construit une exception avec message, contexte et cause.
     *
     * @param message description
     * @param styleName contexte (nom du style/drawable/...)
     * @param cause   exception cause
     */
    public ThemeException(String message, String styleName, Throwable cause) {
        super(message + " (style : " + styleName + ")", cause);
        this.styleName = styleName;
    }

    /**
     * @return le nom du style, ou {@code null}
     */
    public String getStyleName() {
        return styleName;
    }
}
