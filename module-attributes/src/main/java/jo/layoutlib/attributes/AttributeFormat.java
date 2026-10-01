package jo.layoutlib.attributes;

/**
 * Formats d'attributs custom supportés par le mini-layoutlib.
 *
 * <p>Ces formats correspondent à l'attribut {@code format} des éléments
 * {@code <attr>} dans {@code attrs.xml}. Un attribut peut avoir plusieurs
 * formats séparés par {@code |} (ex. {@code "reference|color"}).</p>
 *
 * @author jo@Dev
 * @since 1.0
 */
public enum AttributeFormat {

    /** Dimension (ex. {@code "16dp"}, {@code "100px"}). */
    DIMENSION("dimension"),

    /** Couleur (ex. {@code "#FF6750A4"}, {@code "@color/foo"}). */
    COLOR("color"),

    /** Référence vers une resource (ex. {@code "@drawable/foo"}). */
    REFERENCE("reference"),

    /** Chaîne de caractères. */
    STRING("string"),

    /** Entier. */
    INTEGER("integer"),

    /** Flottant. */
    FLOAT("float"),

    /** Booléen. */
    BOOLEAN("boolean"),

    /** Fraction (ex. {@code "50%"}, {@code "80%p"}). */
    FRACTION("fraction"),

    /** Énumération (valeurs nommées définies via {@code <enum>}). */
    ENUM("enum"),

    /** Flags (valeurs combinables par {@code |}). */
    FLAG("flag");

    /** Nom du format tel qu'il apparaît dans attrs.xml. */
    private final String xmlName;

    AttributeFormat(String xmlName) {
        this.xmlName = xmlName;
    }

    /**
     * @return le nom XML du format
     */
    public String getXmlName() {
        return xmlName;
    }

    /**
     * Parse un nom de format en enum.
     *
     * @param name le nom (ex. {@code "dimension"})
     * @return l'enum, ou {@code null} si inconnu
     */
    public static AttributeFormat fromXmlName(String name) {
        if (name == null || name.isEmpty()) {
            return null;
        }
        for (AttributeFormat format : values()) {
            if (format.xmlName.equals(name)) {
                return format;
            }
        }
        return null;
    }

    /**
     * Parse une chaîne de formats séparés par {@code |}.
     *
     * @param formatsStr la chaîne (ex. {@code "reference|color"})
     * @return un tableau d'enums
     */
    public static AttributeFormat[] parseFormats(String formatsStr) {
        if (formatsStr == null || formatsStr.isEmpty()) {
            return new AttributeFormat[0];
        }
        String[] parts = formatsStr.split("\\|");
        AttributeFormat[] result = new AttributeFormat[parts.length];
        for (int i = 0; i < parts.length; i++) {
            result[i] = fromXmlName(parts[i].trim());
        }
        return result;
    }
}
