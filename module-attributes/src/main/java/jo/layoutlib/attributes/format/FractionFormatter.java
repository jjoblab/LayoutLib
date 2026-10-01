package jo.layoutlib.attributes.format;

/**
 * Formateur d'attributs de type fraction.
 *
 * <p>Une fraction est un pourcentage avec optionnellement un suffixe
 * {@code p} pour indiquer une fraction du parent. Exemples :</p>
 *
 * <ul>
 *   <li>{@code "50%"} → 0.5 (50% de la dimension de base)</li>
 *   <li>{@code "100%"} → 1.0</li>
 *   <li>{@code "50%p"} → 0.5 (50% du parent)</li>
 *   <li>{@code "120%"} → 1.2</li>
 * </ul>
 *
 * @author jo@Dev
 * @since 1.0
 */
public class FractionFormatter {

    /** Suffixe indiquant une fraction du parent. */
    public static final String PARENT_SUFFIX = "p";

    /**
     * Formate une valeur de fraction.
     *
     * @param value la valeur (ex. {@code "50%"} ou {@code "50%p"})
     * @return un tableau {@code [fraction, isParent]} où fraction est la
     *         valeur entre 0 et 1 (ou plus), et isParent indique le suffixe
     * @throws IllegalArgumentException si invalide
     */
    public float[] format(String value) {
        if (value == null || value.isEmpty()) {
            throw new IllegalArgumentException("Valeur de fraction vide");
        }
        String trimmed = value.trim();
        boolean isParent = false;
        if (trimmed.endsWith("%p")) {
            isParent = true;
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        if (!trimmed.endsWith("%")) {
            throw new IllegalArgumentException("Fraction doit finir par % : " + value);
        }
        String numPart = trimmed.substring(0, trimmed.length() - 1);
        try {
            float percent = Float.parseFloat(numPart);
            return new float[]{percent / 100f, isParent ? 1f : 0f};
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Fraction invalide : " + value, e);
        }
    }

    /**
     * Indique si une valeur est une fraction valide.
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

    /**
     * Indique si une fraction référence le parent.
     *
     * @param value la fraction
     * @return {@code true} si c'est une fraction du parent
     */
    public boolean isParentFraction(String value) {
        return value != null && value.trim().endsWith("%p");
    }
}
