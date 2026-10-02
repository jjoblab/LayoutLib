package jo.layoutlib.attributes.format;

import jo.layoutlib.resources.api.AttributeFormat;

/**
 * Validateur de format d attribut.
 *
 * @author jo@Dev
 * @since 1.0
 */
public class AttributeFormatValidator {

    /**
     * Valide qu une valeur respecte un format.
     *
     * @param value la valeur
     * @param format le format attendu
     * @return true si valide
     */
    public static boolean validate(String value, AttributeFormat format) {
        if (value == null || value.isEmpty() || format == null) {
            return false;
        }
        switch (format) {
            case DIMENSION:
                return value.matches("[0-9.]+(dp|dip|sp|px|mm|in|pt)");
            case COLOR:
                return value.startsWith("#") || value.startsWith("@color/");
            case REFERENCE:
                return value.startsWith("@") || value.startsWith("?");
            case STRING:
                return true;
            case INTEGER:
                return value.matches("-?[0-9]+");
            case FLOAT:
                return value.matches("-?[0-9]+\\.?[0-9]*");
            case BOOLEAN:
                return "true".equals(value) || "false".equals(value)
                        || "1".equals(value) || "0".equals(value);
            case FRACTION:
                return value.matches("[0-9.]+%p?");
            default:
                return false;
        }
    }

    /**
     * Valide qu une valeur respecte un ensemble de formats.
     *
     * @param value la valeur
     * @param formats les formats acceptés
     * @return true si au moins un format est respecté
     */
    public static boolean validateAny(String value, AttributeFormat[] formats) {
        if (formats == null || formats.length == 0) {
            return true;
        }
        for (AttributeFormat f : formats) {
            if (validate(value, f)) {
                return true;
            }
        }
        return false;
    }
}
