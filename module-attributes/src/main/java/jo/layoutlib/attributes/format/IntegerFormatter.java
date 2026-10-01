package jo.layoutlib.attributes.format;

/**
 * Formateur d'attributs de type entier.
 *
 * <p>Accepte les entiers signés et les valeurs hexadécimales (préfixées
 * par {@code 0x}).</p>
 *
 * @author jo@Dev
 * @since 1.0
 */
public class IntegerFormatter {

    /**
     * Formate une valeur en entier.
     *
     * @param value la valeur (ex. {@code "42"}, {@code "0xFF"})
     * @return l'entier
     * @throws IllegalArgumentException si invalide
     */
    public int format(String value) {
        if (value == null || value.isEmpty()) {
            throw new IllegalArgumentException("Valeur entière vide");
        }
        String trimmed = value.trim();
        try {
            if (trimmed.startsWith("0x") || trimmed.startsWith("0X")) {
                return Integer.parseInt(trimmed.substring(2), 16);
            }
            return Integer.parseInt(trimmed);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Entier invalide : " + value, e);
        }
    }

    /**
     * Indique si une valeur est un entier valide.
     *
     * @param value la valeur à tester
     * @return {@code true} si valide
     */
    public boolean isValid(String value) {
        if (value == null || value.isEmpty()) {
            return false;
        }
        try {
            format(value);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
