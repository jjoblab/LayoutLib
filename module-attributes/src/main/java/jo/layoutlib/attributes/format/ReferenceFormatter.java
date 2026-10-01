package jo.layoutlib.attributes.format;

import jo.layoutlib.resources.api.ResourceUrl;

/**
 * Formateur d attributs de type reference (@type/name ou ?attr/name).
 *
 * @author jo@Dev
 * @since 1.0
 */
public class ReferenceFormatter {

    /**
     * Formate une référence (retourne telle quelle si valide).
     *
     * @param value la valeur
     * @return la valeur
     * @throws IllegalArgumentException si invalide
     */
    public String format(String value) {
        if (!isValid(value)) {
            throw new IllegalArgumentException("Référence invalide : " + value);
        }
        return value;
    }

    /**
     * Indique si une valeur est une référence valide.
     *
     * @param value la valeur
     * @return true si commence par @ ou ?
     */
    public boolean isValid(String value) {
        if (value == null || value.isEmpty()) {
            return false;
        }
        if (!value.startsWith("@") && !value.startsWith("?")) {
            return false;
        }
        return ResourceUrl.parse(value) != null;
    }

    /**
     * @return true si c est une référence de thème (?attr/...)
     */
    public boolean isThemeReference(String value) {
        return value != null && value.startsWith("?");
    }
}
