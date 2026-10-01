package jo.layoutlib.attributes;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Définition d'un attribut custom (issu de {@code <declare-styleable>}).
 *
 * <p>Stocke le nom, les formats attendus, et les valeurs d'enum/flag si
 * applicable.</p>
 *
 * @author jo@Dev
 * @since 1.0
 */
public class AttributeDefinition {

    /** Nom de l'attribut (ex. {@code "cornerRadius"}). */
    private final String name;

    /** Formats acceptés (ex. dimension, color, reference|color). */
    private final AttributeFormat[] formats;

    /** Map des valeurs d'enum (nom → valeur entière). */
    private final Map<String, Integer> enumValues = new LinkedHashMap<>();

    /** Map des valeurs de flag (nom → valeur entière). */
    private final Map<String, Integer> flagValues = new LinkedHashMap<>();

    /** Nom du styleable parent (ex. {@code "MaterialButton"}). */
    private String styleableName;

    /**
     * Construit une définition d'attribut.
     *
     * @param name    nom de l'attribut
     * @param formats formats acceptés
     */
    public AttributeDefinition(String name, AttributeFormat[] formats) {
        if (name == null || name.isEmpty()) {
            throw new AttributeException("Nom d'attribut vide", name);
        }
        this.name = name;
        this.formats = formats != null ? formats : new AttributeFormat[0];
    }

    /**
     * Ajoute une valeur d'enum.
     *
     * @param enumName nom de l'enum (ex. {@code "textStart"})
     * @param value    valeur entière
     */
    public void addEnumValue(String enumName, int value) {
        enumValues.put(enumName, value);
    }

    /**
     * Ajoute une valeur de flag.
     *
     * @param flagName nom du flag
     * @param value    valeur entière
     */
    public void addFlagValue(String flagName, int value) {
        flagValues.put(flagName, value);
    }

    /**
     * Indique si l'attribut accepte un format donné.
     *
     * @param format le format à tester
     * @return {@code true} si le format est accepté
     */
    public boolean acceptsFormat(AttributeFormat format) {
        if (format == null) {
            return false;
        }
        for (AttributeFormat f : formats) {
            if (f == format) {
                return true;
            }
        }
        return false;
    }

    /**
     * Valide qu'une valeur respecte les formats de l'attribut.
     *
     * @param value la valeur à valider
     * @return {@code true} si la valeur est compatible
     */
    public boolean isValidValue(String value) {
        if (value == null || value.isEmpty()) {
            return false;
        }
        for (AttributeFormat format : formats) {
            if (matchesFormat(value, format)) {
                return true;
            }
        }
        return formats.length == 0;  // Si pas de format défini, tout passe
    }

    /**
     * Teste si une valeur matche un format spécifique.
     */
    private boolean matchesFormat(String value, AttributeFormat format) {
        switch (format) {
            case DIMENSION:
                return value.matches("[0-9.]+(dp|dip|sp|px|mm|in|pt)");
            case COLOR:
                return value.startsWith("#") || value.startsWith("@color/");
            case REFERENCE:
                return value.startsWith("@") || value.startsWith("?");
            case STRING:
                return true;  // Tout est string
            case INTEGER:
                return value.matches("-?[0-9]+");
            case FLOAT:
                return value.matches("-?[0-9]+\\.?[0-9]*");
            case BOOLEAN:
                return "true".equals(value) || "false".equals(value)
                        || "1".equals(value) || "0".equals(value);
            case FRACTION:
                return value.matches("[0-9]+%p?") || value.matches("[0-9.]+%p?");
            case ENUM:
                return enumValues.containsKey(value);
            case FLAG:
                // Les flags peuvent être combinés par |
                String[] parts = value.split("\\|");
                for (String part : parts) {
                    if (!flagValues.containsKey(part.trim())) {
                        return false;
                    }
                }
                return true;
            default:
                return false;
        }
    }

    /**
     * Résout une valeur d'enum en entier.
     *
     * @param enumName nom de l'enum
     * @return la valeur entière, ou {@code null} si inconnu
     */
    public Integer getEnumValue(String enumName) {
        return enumValues.get(enumName);
    }

    /**
     * Résout une valeur de flag en entier (combinaison OR des flags).
     *
     * @param flagExpr expression de flags séparés par |
     * @return la valeur entière combinée, ou {@code null} si inconnu
     */
    public Integer getFlagValue(String flagExpr) {
        if (flagExpr == null) {
            return null;
        }
        String[] parts = flagExpr.split("\\|");
        int result = 0;
        for (String part : parts) {
            Integer v = flagValues.get(part.trim());
            if (v == null) {
                return null;
            }
            result |= v;
        }
        return result;
    }

    public String getName() {
        return name;
    }

    public AttributeFormat[] getFormats() {
        return formats.clone();
    }

    public Map<String, Integer> getEnumValues() {
        return Collections.unmodifiableMap(enumValues);
    }

    public Map<String, Integer> getFlagValues() {
        return Collections.unmodifiableMap(flagValues);
    }

    public String getStyleableName() {
        return styleableName;
    }

    public void setStyleableName(String name) {
        this.styleableName = name;
    }

    /**
     * @return une chaîne représentant les formats (ex. {@code "dimension|color"})
     */
    public String getFormatsString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < formats.length; i++) {
            if (i > 0) sb.append("|");
            sb.append(formats[i].getXmlName());
        }
        return sb.toString();
    }
}
