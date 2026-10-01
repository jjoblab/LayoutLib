package jo.layoutlib.attributes.format;

import java.util.HashMap;
import java.util.Map;

import jo.layoutlib.resources.ColorParser;
import jo.layoutlib.resources.ResourceException;

/**
 * Formateur d'attributs de type couleur.
 *
 * <p>Une couleur peut être :</p>
 * <ul>
 *   <li>Hex 8 chiffres : {@code #FF6750A4} (ARGB)</li>
 *   <li>Hex 6 chiffres : {@code #6750A4} (RGB)</li>
 *   <li>Hex 4 chiffres : {@code #F675} (ARGB compact)</li>
 *   <li>Hex 3 chiffres : {@code #675} (RGB compact)</li>
 *   <li>Couleur nommée : {@code red}, {@code transparent}, etc.</li>
 *   <li>Référence : {@code @color/foo} (non résolue ici)</li>
 * </ul>
 *
 * @author jo@Dev
 * @since 1.0
 */
public class ColorFormatter {

    /** Map des couleurs nommées. */
    private static final Map<String, Integer> NAMED_COLORS = new HashMap<>();

    static {
        NAMED_COLORS.put("transparent", 0x00000000);
        NAMED_COLORS.put("black", 0xFF000000);
        NAMED_COLORS.put("white", 0xFFFFFFFF);
        NAMED_COLORS.put("red", 0xFFFF0000);
        NAMED_COLORS.put("green", 0xFF00FF00);
        NAMED_COLORS.put("blue", 0xFF0000FF);
        NAMED_COLORS.put("yellow", 0xFFFFFF00);
        NAMED_COLORS.put("cyan", 0xFF00FFFF);
        NAMED_COLORS.put("magenta", 0xFFFF00FF);
        NAMED_COLORS.put("gray", 0xFF888888);
        NAMED_COLORS.put("grey", 0xFF888888);
    }

    /**
     * Construit un formateur de couleurs.
     */
    public ColorFormatter() {
    }

    /**
     * Formate une valeur de couleur en ARGB int.
     *
     * @param value la valeur brute (ex. {@code "#FF6750A4"})
     * @return la valeur ARGB
     * @throws ResourceException si la valeur est invalide
     */
    public int format(String value) {
        return ColorParser.parse(value);
    }

    /**
     * Indique si une valeur est une couleur valide.
     *
     * @param value la valeur à tester
     * @return {@code true} si valide
     */
    public boolean isValid(String value) {
        return ColorParser.isColor(value);
    }

    /**
     * Indique si une valeur est une référence de couleur
     * (commence par {@code @color/}).
     *
     * @param value la valeur à tester
     * @return {@code true} si c'est une référence
     */
    public boolean isReference(String value) {
        return value != null && value.startsWith("@color/");
    }

    /**
     * Extrait le nom de la resource depuis une référence {@code @color/foo}.
     *
     * @param reference la référence
     * @return le nom (ex. {@code "foo"}), ou {@code null} si pas une référence
     */
    public String extractReferenceName(String reference) {
        if (!isReference(reference)) {
            return null;
        }
        return reference.substring("@color/".length());
    }

    /**
     * @return une copie de la map des couleurs nommées
     */
    public Map<String, Integer> getNamedColors() {
        return new HashMap<>(NAMED_COLORS);
    }
}
