package jo.layoutlib.resources;

/**
 * Utilitaire de parsing de couleurs Android.
 *
 * <p>Cette classe transforme une chaîne de couleur Android en valeur ARGB
 * entière (32 bits). Elle supporte tous les formats reconnus par le
 * framework :</p>
 *
 * <ul>
 *   <li>Hexadécimal 8 chiffres : {@code #FF6750A4} (ARGB)</li>
 *   <li>Hexadécimal 6 chiffres : {@code #6750A4} (RGB, alpha = 0xFF)</li>
 *   <li>Hexadécimal 4 chiffres : {@code #F675} (ARGB compact, chaque canal sur 4 bits)</li>
 *   <li>Hexadécimal 3 chiffres : {@code #675} (RGB compact, alpha = 0xF)</li>
 *   <li>Couleurs nommées du framework : {@code red}, {@code blue}, {@code transparent}, etc.</li>
 * </ul>
 *
 * <p>Référence : {@code android.graphics.Color} et
 * {@code com.android.layoutlib.bridge.impl.ResourceHelper.getColor()}.</p>
 *
 * @author jo@Dev
 * @since 1.0
 */
public final class ColorParser {

    /**
     * Constructeur privé : la classe n'est pas instanciable.
     */
    private ColorParser() {
    }

    /**
     * Parse une chaîne de couleur en valeur ARGB entière.
     *
     * @param value la chaîne à parser (ex. {@code "#FF6750A4"} ou {@code "red"})
     * @return la valeur ARGB (32 bits, format 0xAARRGGBB)
     * @throws ResourceException si la valeur n'est pas une couleur valide
     */
    public static int parse(String value) {
        if (value == null || value.isEmpty()) {
            throw new ResourceException("Couleur vide ou null");
        }
        String trimmed = value.trim();

        if (trimmed.startsWith("@color/")) {
            throw new ResourceException(
                    "Référence @color/ non résolue : " + value
                            + " (doit être résolue avant parsing)", value);
        }
        if (trimmed.startsWith("?")) {
            throw new ResourceException(
                    "Référence ?attr/ non résolue : " + value, value);
        }

        if (trimmed.startsWith("#")) {
            return parseHex(trimmed);
        }

        // Couleur nommée du framework
        Integer named = parseNamedColor(trimmed);
        if (named != null) {
            return named;
        }
        throw new ResourceException("Couleur inconnue : " + value, value);
    }

    /**
     * Parse une couleur hexadécimale.
     *
     * @param hex la chaîne hex (ex. {@code "#FF6750A4"})
     * @return la valeur ARGB
     * @throws ResourceException si le format est invalide
     */
    private static int parseHex(String hex) {
        try {
            switch (hex.length()) {
                case 9: // #AARRGGBB
                    return (int) Long.parseLong(hex.substring(1), 16);
                case 7: // #RRGGBB
                    return 0xFF000000 | (int) Long.parseLong(hex.substring(1), 16);
                case 5: // #ARGB (compact)
                    String a = hex.substring(1, 2);
                    String r = hex.substring(2, 3);
                    String g = hex.substring(3, 4);
                    String b = hex.substring(4, 5);
                    return (int) Long.parseLong(a + a + r + r + g + g + b + b, 16);
                case 4: // #RGB (compact)
                    String r2 = hex.substring(1, 2);
                    String g2 = hex.substring(2, 3);
                    String b2 = hex.substring(3, 4);
                    return 0xFF000000
                            | (int) Long.parseLong(r2 + r2 + g2 + g2 + b2 + b2, 16);
                default:
                    throw new ResourceException(
                            "Longueur hex invalide : " + hex, hex);
            }
        } catch (NumberFormatException e) {
            throw new ResourceException("Hex invalide : " + hex, hex, e);
        }
    }

    /**
     * Tente de résoudre une couleur nommée du framework Android.
     *
     * <p>Liste non exhaustive — seules les couleurs les plus courantes sont
     * incluses. Pour une liste complète, voir
     * {@code android.R.color}.</p>
     *
     * @param name le nom de la couleur (ex. {@code "red"}, {@code "transparent"})
     * @return la valeur ARGB, ou {@code null} si le nom n'est pas reconnu
     */
    private static Integer parseNamedColor(String name) {
        switch (name.toLowerCase()) {
            case "transparent": return 0x00000000;
            case "black":       return 0xFF000000;
            case "white":       return 0xFFFFFFFF;
            case "red":         return 0xFFFF0000;
            case "green":       return 0xFF00FF00;
            case "blue":        return 0xFF0000FF;
            case "yellow":      return 0xFFFFFF00;
            case "cyan":        return 0xFF00FFFF;
            case "magenta":     return 0xFFFF00FF;
            case "gray":
            case "grey":        return 0xFF888888;
            case "darkgray":
            case "darkgrey":    return 0xFF444444;
            case "lightgray":
            case "lightgrey":   return 0xFFCCCCCC;
            default:            return null;
        }
    }

    /**
     * Indique si une chaîne est une couleur valide.
     *
     * @param value la chaîne à tester
     * @return {@code true} si la chaîne peut être parsée
     */
    public static boolean isColor(String value) {
        if (value == null || value.isEmpty()) {
            return false;
        }
        try {
            parse(value);
            return true;
        } catch (ResourceException e) {
            return false;
        }
    }
}
