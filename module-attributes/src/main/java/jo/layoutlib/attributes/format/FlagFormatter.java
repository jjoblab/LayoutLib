package jo.layoutlib.attributes.format;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Formateur d'attributs de type flag.
 *
 * <p>Un flag est un ensemble de valeurs combinables par {@code |} (OU binaire).
 * Exemple typique depuis {@code attrs.xml} :</p>
 *
 * <pre>{@code
 * <attr name="textStyle" format="flag">
 *     <flag name="normal" value="0"/>
 *     <flag name="bold" value="1"/>
 *     <flag name="italic" value="2"/>
 * </attr>
 * }</pre>
 *
 * <p>Une expression comme {@code "bold|italic"} est résolue en
 * {@code 1 | 2 = 3}.</p>
 *
 * @author jo@Dev
 * @since 1.0
 */
public class FlagFormatter {

    /** Map nom → valeur. */
    private final Map<String, Integer> flagValues = new LinkedHashMap<>();

    /**
     * Construit un formateur vide.
     */
    public FlagFormatter() {
    }

    /**
     * Ajoute une valeur de flag.
     *
     * @param name  le nom du flag
     * @param value la valeur entière
     */
    public void addValue(String name, int value) {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("name ne peut pas être vide");
        }
        flagValues.put(name, value);
    }

    /**
     * Formate une expression de flags en entier combiné (OR).
     *
     * @param value l'expression (ex. {@code "bold|italic"})
     * @return la valeur entière combinée
     * @throws IllegalArgumentException si un flag est inconnu
     */
    public int format(String value) {
        if (value == null || value.isEmpty()) {
            throw new IllegalArgumentException("Expression de flag vide");
        }
        String[] parts = value.split("\\|");
        int result = 0;
        for (String part : parts) {
            String trimmed = part.trim();
            Integer flagValue = flagValues.get(trimmed);
            if (flagValue == null) {
                throw new IllegalArgumentException("Flag inconnu : " + trimmed
                        + " (flags connus : " + flagValues.keySet() + ")");
            }
            result |= flagValue;
        }
        return result;
    }

    /**
     * Indique si une expression de flags est valide.
     *
     * @param value l'expression à tester
     * @return {@code true} si tous les flags sont connus
     */
    public boolean isValid(String value) {
        if (value == null || value.isEmpty()) {
            return false;
        }
        String[] parts = value.split("\\|");
        for (String part : parts) {
            if (!flagValues.containsKey(part.trim())) {
                return false;
            }
        }
        return true;
    }

    /**
     * Décompose une valeur entière en liste de noms de flags.
     *
     * <p>Utile pour afficher une valeur debug.</p>
     *
     * @param combinedValue la valeur combinée
     * @return la liste des noms de flags actifs
     */
    public java.util.List<String> decompose(int combinedValue) {
        java.util.List<String> result = new java.util.ArrayList<>();
        for (Map.Entry<String, Integer> entry : flagValues.entrySet()) {
            int flagValue = entry.getValue();
            if (flagValue == 0) {
                // Le flag "normal" (valeur 0) est actif seulement si combinedValue = 0
                if (combinedValue == 0) {
                    result.add(entry.getKey());
                }
            } else if ((combinedValue & flagValue) == flagValue) {
                result.add(entry.getKey());
            }
        }
        return result;
    }

    /**
     * @return le nombre de flags
     */
    public int size() {
        return flagValues.size();
    }

    /**
     * @return une copie de la map nom → valeur
     */
    public Map<String, Integer> getValues() {
        return new LinkedHashMap<>(flagValues);
    }

    /**
     * Vide les flags.
     */
    public void clear() {
        flagValues.clear();
    }
}
