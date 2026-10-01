package jo.layoutlib.attributes.format;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Formateur d'attributs de type enum.
 *
 * <p>Un enum est un ensemble de valeurs nommées associées à des entiers.
 * Exemple typique depuis {@code attrs.xml} :</p>
 *
 * <pre>{@code
 * <attr name="iconGravity" format="enum">
 *     <enum name="textStart" value="1"/>
 *     <enum name="textEnd" value="2"/>
 * </attr>
 * }</pre>
 *
 * @author jo@Dev
 * @since 1.0
 */
public class EnumFormatter {

    /** Map nom → valeur. */
    private final Map<String, Integer> enumValues = new LinkedHashMap<>();

    /**
     * Construit un formateur vide.
     */
    public EnumFormatter() {
    }

    /**
     * Ajoute une valeur d'enum.
     *
     * @param name  le nom de la valeur
     * @param value la valeur entière
     */
    public void addValue(String name, int value) {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("name ne peut pas être vide");
        }
        enumValues.put(name, value);
    }

    /**
     * Formate une valeur d'enum en entier.
     *
     * @param value le nom de l'enum (ex. {@code "textStart"})
     * @return la valeur entière
     * @throws IllegalArgumentException si l'enum est inconnu
     */
    public int format(String value) {
        if (value == null || value.isEmpty()) {
            throw new IllegalArgumentException("Valeur d'enum vide");
        }
        Integer resolved = enumValues.get(value);
        if (resolved == null) {
            throw new IllegalArgumentException("Enum inconnu : " + value
                    + " (valeurs connues : " + enumValues.keySet() + ")");
        }
        return resolved;
    }

    /**
     * Indique si une valeur est un enum valide.
     *
     * @param value la valeur à tester
     * @return {@code true} si valide
     */
    public boolean isValid(String value) {
        return value != null && enumValues.containsKey(value);
    }

    /**
     * @return le nombre de valeurs d'enum
     */
    public int size() {
        return enumValues.size();
    }

    /**
     * @return une copie de la map nom → valeur
     */
    public Map<String, Integer> getValues() {
        return new LinkedHashMap<>(enumValues);
    }

    /**
     * Vide les valeurs d'enum.
     */
    public void clear() {
        enumValues.clear();
    }
}
