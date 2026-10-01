package jo.layoutlib.attributes.format;

/**
 * Formateur d attributs de type string.
 *
 * @author jo@Dev
 * @since 1.0
 */
public class StringFormatter {

    /**
     * Formate une valeur string (retourne telle quelle).
     *
     * @param value la valeur
     * @return la valeur
     */
    public String format(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Valeur string null");
        }
        return value;
    }

    /**
     * Indique si une valeur est valide (toujours true pour string).
     *
     * @param value la valeur
     * @return true
     */
    public boolean isValid(String value) {
        return value != null;
    }
}
