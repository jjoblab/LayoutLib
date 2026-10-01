package jo.layoutlib.attributes.format;

/**
 * Formateur d'attributs de type flottant.
 *
 * <p>Accepte les flottants standards (ex. {@code "3.14"}, {@code "-0.5"})
 * ainsi que la notation scientifique (ex. {@code "1e10"}).</p>
 *
 * @author jo@Dev
 * @since 1.0
 */
public class FloatFormatter {

    /**
     * Formate une valeur en flottant.
     *
     * @param value la valeur (ex. {@code "3.14"})
     * @return le flottant
     * @throws IllegalArgumentException si invalide
     */
    public float format(String value) {
        if (value == null || value.isEmpty()) {
            throw new IllegalArgumentException("Valeur flottante vide");
        }
        try {
            return Float.parseFloat(value.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Flottant invalide : " + value, e);
        }
    }

    /**
     * Indique si une valeur est un flottant valide.
     *
     * @param value la valeur à tester
     * @return {@code true} si valide
     */
    public boolean isValid(String value) {
        if (value == null || value.isEmpty()) {
            return false;
        }
        try {
            Float.parseFloat(value.trim());
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
