package jo.layoutlib.resources;

/**
 * Exception levée par le Module 2 (Resources) lorsqu'une erreur survient
 * pendant le parsing ou la résolution d'une resource Android.
 *
 * <p>Cette exception est unchecked (hérite de {@link RuntimeException}) afin
 * de garder les signatures de méthodes propres. Elle transporte le nom de la
 * resource fautive quand cette information est disponible.</p>
 *
 * @author jo@Dev
 * @since 1.0
 */
public class ResourceException extends RuntimeException {

    /** Nom de la resource concernée, ou {@code null} si inconnu. */
    private final String resourceName;

    /**
     * Construit une nouvelle exception avec un message descriptif.
     *
     * @param message description de l'erreur
     */
    public ResourceException(String message) {
        super(message);
        this.resourceName = null;
    }

    /**
     * Construit une nouvelle exception avec un message et une cause racine.
     *
     * @param message description de l'erreur
     * @param cause   exception cause
     */
    public ResourceException(String message, Throwable cause) {
        super(message, cause);
        this.resourceName = null;
    }

    /**
     * Construit une nouvelle exception avec un contexte de resource.
     *
     * @param message        description de l'erreur
     * @param resourceName   nom de la resource fautive
     */
    public ResourceException(String message, String resourceName) {
        super(message + " (resource : " + resourceName + ")");
        this.resourceName = resourceName;
    }

    /**
     * Construit une nouvelle exception avec un message, un nom de resource et une cause.
     *
     * @param message        description de l'erreur
     * @param resourceName   nom de la resource fautive
     * @param cause          exception cause
     */
    public ResourceException(String message, String resourceName, Throwable cause) {
        super(message + " (resource : " + resourceName + ")", cause);
        this.resourceName = resourceName;
    }

    /**
     * @return le nom de la resource fautive, ou {@code null}
     */
    public String getResourceName() {
        return resourceName;
    }
}
