package jo.layoutlib.inflater.bridge.impl;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jo.layoutlib.resources.ColorParser;
import jo.layoutlib.resources.ResourceException;

/**
 * Utilitaire central de résolution de resources, inspiré de
 * {@code com.android.layoutlib.bridge.impl.ResourceHelper} de l'AOSP.
 *
 * <p>Cette classe fournit des méthodes statiques pour convertir des valeurs
 * brutes (strings XML) en objets typés Android :
 * couleurs, dimensions, booléens, entiers, etc.</p>
 *
 * <h2>Méthodes principales</h2>
 * <ul>
 *   <li>{@link #getColor(String)} — convertit "#FF6750A4" ou "red" en ARGB int</li>
 *   <li>{@link #getBoolean(String)} — convertit "true"/"false"/"1"/"0" en booléen</li>
 *   <li>{@link #getInteger(String)} — convertit une chaîne en int</li>
 *   <li>{@link #getFloat(String)} — convertit une chaîne en float</li>
 *   <li>{@link #getDrawable(String)} — convertit une couleur en ColorDrawable</li>
 * </ul>
 *
 * @author jo@Dev
 * @since 1.0
 */
public final class ResourceHelper {

    /** Pattern pour valider un entier signé. */
    private static final Pattern INTEGER_PATTERN = Pattern.compile("-?\\d+");

    /** Pattern pour valider un flottant. */
    private static final Pattern FLOAT_PATTERN =
            Pattern.compile("-?\\d+(\\.\\d+)?([eE][+-]?\\d+)?");

    /** Pattern pour valider une couleur hex. */
    private static final Pattern HEX_COLOR_PATTERN =
            Pattern.compile("#([0-9a-fA-F]{3}|[0-9a-fA-F]{4}|[0-9a-fA-F]{6}|[0-9a-fA-F]{8})");

    /** Cache des couleurs nommées du framework. */
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
        NAMED_COLORS.put("darkgray", 0xFF444444);
        NAMED_COLORS.put("darkgrey", 0xFF444444);
        NAMED_COLORS.put("lightgray", 0xFFCCCCCC);
        NAMED_COLORS.put("lightgrey", 0xFFCCCCCC);
    }

    /** Constructeur privé. */
    private ResourceHelper() {
    }

    /**
     * Convertit une chaîne en valeur ARGB entière.
     *
     * <p>Formats acceptés :</p>
     * <ul>
     *   <li>Hex 8 chiffres : {@code #FF6750A4} (ARGB complet)</li>
     *   <li>Hex 6 chiffres : {@code #6750A4} (RGB, alpha=0xFF)</li>
     *   <li>Hex 4 chiffres : {@code #F675} (ARGB compact)</li>
     *   <li>Hex 3 chiffres : {@code #675} (RGB compact)</li>
     *   <li>Couleur nommée : {@code red}, {@code transparent}, etc.</li>
     * </ul>
     *
     * @param value la chaîne à convertir
     * @return la valeur ARGB (0xAARRGGBB)
     * @throws ResourceException si la valeur est invalide
     */
    public static int getColor(String value) {
        if (value == null || value.isEmpty()) {
            throw new ResourceException("Valeur de couleur vide");
        }
        String trimmed = value.trim();

        // Couleur nommée
        Integer named = NAMED_COLORS.get(trimmed.toLowerCase());
        if (named != null) {
            return named;
        }

        // Couleur hex
        if (trimmed.startsWith("#")) {
            if (!HEX_COLOR_PATTERN.matcher(trimmed).matches()) {
                throw new ResourceException("Couleur hex invalide : " + value, value);
            }
            return ColorParser.parse(trimmed);
        }

        // Référence @color/ — ne peut pas être résolue ici
        if (trimmed.startsWith("@color/")) {
            throw new ResourceException(
                    "Référence @color/ non résolvable sans ResourceResolver : " + value, value);
        }

        throw new ResourceException("Couleur inconnue : " + value, value);
    }

    /**
     * Convertit une chaîne en booléen.
     *
     * <p>Android accepte {@code "true"} / {@code "false"} (insensible à la casse)
     * ainsi que {@code "1"} / {@code "0"}.</p>
     *
     * @param value la chaîne à convertir
     * @return le booléen
     * @throws ResourceException si la valeur est invalide
     */
    public static boolean getBoolean(String value) {
        if (value == null || value.isEmpty()) {
            throw new ResourceException("Valeur booléenne vide");
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
                throw new ResourceException("Booléen invalide : " + value, value);
        }
    }

    /**
     * Convertit une chaîne en entier.
     *
     * @param value la chaîne (ex. {@code "42"}, {@code "-10"})
     * @return l'entier
     * @throws ResourceException si la valeur est invalide
     */
    public static int getInteger(String value) {
        if (value == null || value.isEmpty()) {
            throw new ResourceException("Valeur entière vide");
        }
        String trimmed = value.trim();
        if (!INTEGER_PATTERN.matcher(trimmed).matches()) {
            throw new ResourceException("Entier invalide : " + value, value);
        }
        try {
            return Integer.parseInt(trimmed);
        } catch (NumberFormatException e) {
            throw new ResourceException("Entier hors plage : " + value, value, e);
        }
    }

    /**
     * Convertit une chaîne en flottant.
     *
     * @param value la chaîne (ex. {@code "3.14"}, {@code "-0.5"}, {@code "1e10"})
     * @return le flottant
     * @throws ResourceException si la valeur est invalide
     */
    public static float getFloat(String value) {
        if (value == null || value.isEmpty()) {
            throw new ResourceException("Valeur flottante vide");
        }
        String trimmed = value.trim();
        if (!FLOAT_PATTERN.matcher(trimmed).matches()) {
            throw new ResourceException("Flottant invalide : " + value, value);
        }
        try {
            return Float.parseFloat(trimmed);
        } catch (NumberFormatException e) {
            throw new ResourceException("Flottant hors plage : " + value, value, e);
        }
    }

    /**
     * Indique si une chaîne est une référence à une resource
     * (commence par {@code @} ou {@code ?}).
     *
     * @param value la valeur à tester
     * @return {@code true} si c'est une référence
     */
    public static boolean isReference(String value) {
        if (value == null || value.isEmpty()) {
            return false;
        }
        char first = value.charAt(0);
        return first == '@' || first == '?';
    }

    /**
     * Crée un Drawable depuis une valeur de couleur littérale.
     *
     * <p>Si la valeur est une couleur hex ou nommée, retourne un
     * {@link ColorDrawable}. Sinon retourne {@code null} (la valeur
     * doit être résolue via un ResourceResolver).</p>
     *
     * @param value la valeur brute
     * @return un ColorDrawable ou {@code null}
     */
    public static Drawable getDrawable(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.startsWith("#") || NAMED_COLORS.containsKey(trimmed.toLowerCase())) {
            try {
                int color = getColor(trimmed);
                return new ColorDrawable(color);
            } catch (ResourceException e) {
                return null;
            }
        }
        return null;
    }

    /**
     * @return une copie de la map des couleurs nommées du framework
     */
    public static Map<String, Integer> getNamedColors() {
        return new HashMap<>(NAMED_COLORS);
    }

    /**
     * Indique si une chaîne est une couleur valide (hex ou nommée).
     *
     * @param value la valeur à tester
     * @return {@code true} si c'est une couleur valide
     */
    public static boolean isColor(String value) {
        if (value == null || value.isEmpty()) {
            return false;
        }
        try {
            getColor(value);
            return true;
        } catch (ResourceException e) {
            return false;
        }
    }

    /**
     * Indique si une chaîne est un entier valide.
     *
     * @param value la valeur à tester
     * @return {@code true} si c'est un entier valide
     */
    public static boolean isInteger(String value) {
        if (value == null || value.isEmpty()) {
            return false;
        }
        return INTEGER_PATTERN.matcher(value.trim()).matches();
    }

    /**
     * Indique si une chaîne est un flottant valide.
     *
     * @param value la valeur à tester
     * @return {@code true} si c'est un flottant valide
     */
    public static boolean isFloat(String value) {
        if (value == null || value.isEmpty()) {
            return false;
        }
        return FLOAT_PATTERN.matcher(value.trim()).matches();
    }
}
