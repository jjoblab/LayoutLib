package jo.layoutlib.attributes.format;

/**
 * Formateur d'attributs de type booléen.
 *
 * <p>Android accepte {@code "true"} / {@code "false"} (insensible à la casse)
 * ainsi que {@code "1"} / {@code "0"}.</p>
 *
 * @author jo@Dev
 * @since 1.0
 */
public class BooleanFormatter {

    /**
     * Formate une valeur booléenne.
     *
     * @param value la valeur (ex. {@code "true"}, {@code "1"})
     * @return le booléen
     * @throws IllegalArgumentException si la valeur est invalide
     */
    public boolean format(String value) {
        if (value == null || value.isEmpty()) {
            throw new IllegalArgumentException("Valeur booléenne vide");
        }
        String lower = value.trim().toLowerCase();
        switch (lower) {
            case "true":
            case "1":
                return true;
            case "false":
            case "0":
                return false;
            default:
                throw new IllegalArgumentException("Booléen invalide : " + value);
        }
    }

    /**
     * Indique si une valeur est un booléen valide.
     *
     * @param value la valeur à tester
     * @return {@code true} si valide
     */
    public boolean isValid(String value) {
        if (value == null || value.isEmpty()) {
            return false;
        }
        String lower = value.trim().toLowerCase();
        return "true".equals(lower) || "false".equals(lower)
                || "1".equals(lower) || "0".equals(lower);
    }
}
