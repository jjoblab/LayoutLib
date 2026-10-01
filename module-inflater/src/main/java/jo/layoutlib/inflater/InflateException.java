package jo.layoutlib.inflater;

/**
 * Exception de base levée par le mini-layoutlib lorsqu'une erreur survient
 * pendant l'inflation d'un layout XML.
 *
 * <p>Cette exception est unchecked (hérite de {@link RuntimeException}) afin de
 * ne pas polluer les signatures des méthodes appelantes. Elle transporte le
 * numéro de ligne et le nom du fichier XML fautif quand cette information est
 * disponible.</p>
 *
 * @author jo@Dev
 * @since 1.0
 */
public class InflateException extends RuntimeException {

    /** Numéro de ligne XML où l'erreur a été détectée, ou -1 si inconnu. */
    private final int lineNumber;

    /** Nom du fichier XML source, ou {@code null} si inconnu. */
    private final String sourceFile;

    /**
     * Construit une nouvelle exception avec un message descriptif.
     *
     * @param message description de l'erreur
     */
    public InflateException(String message) {
        super(message);
        this.lineNumber = -1;
        this.sourceFile = null;
    }

    /**
     * Construit une nouvelle exception avec un message et la cause racine.
     *
     * @param message description de l'erreur
     * @param cause   exception cause
     */
    public InflateException(String message, Throwable cause) {
        super(message, cause);
        this.lineNumber = -1;
        this.sourceFile = null;
    }

    /**
     * Construit une nouvelle exception avec contexte de position XML.
     *
     * @param message    description de l'erreur
     * @param lineNumber numéro de ligne dans le XML source
     * @param sourceFile nom du fichier XML source
     */
    public InflateException(String message, int lineNumber, String sourceFile) {
        super(message + " (ligne " + lineNumber + " de " + sourceFile + ")");
        this.lineNumber = lineNumber;
        this.sourceFile = sourceFile;
    }

    /**
     * @return le numéro de ligne XML, ou -1 si inconnu
     */
    public int getLineNumber() {
        return lineNumber;
    }

    /**
     * @return le nom du fichier XML source, ou {@code null}
     */
    public String getSourceFile() {
        return sourceFile;
    }
}
