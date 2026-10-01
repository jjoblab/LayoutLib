package jo.layoutlib.resources;

/**
 * Convertisseur de dimensions Android vers les pixels.
 *
 * <p>Cette classe transforme une chaîne comme {@code "16dp"}, {@code "12sp"}
 * ou {@code "100px"} en valeur flottante en pixels, en utilisant les
 * métriques d'affichage du contexte Android.</p>
 *
 * <h2>Unités supportées</h2>
 * <ul>
 *   <li>{@code dp} / {@code dip} : density-independent pixel</li>
 *   <li>{@code sp} : scale-independent pixel (affecté par fontScale)</li>
 *   <li>{@code px} : pixel brut</li>
 *   <li>{@code mm} : millimètre (basé sur xdpi)</li>
 *   <li>{@code in} : pouce (basé sur xdpi)</li>
 *   <li>{@code pt} : point (1/72 de pouce)</li>
 * </ul>
 *
 * <p>Si aucune unité n'est spécifiée, la valeur est traitée comme des pixels
 * bruts.</p>
 *
 * @author jo@Dev
 * @since 1.0
 */
public final class DimensionConverter {

    /** Densité d'affichage (ex. 2.0 pour xhdpi). */
    private final float density;

    /** Échelle de police (ex. 1.0 par défaut, 1.3 pour grand). */
    private final float fontScale;

    /** Densité horizontale en dpi (pour mm, in, pt). */
    private final float xdpi;

    /**
     * Construit un convertisseur avec les métriques spécifiées.
     *
     * @param density  densité d'affichage (densityDpi / 160)
     * @param fontScale échelle de police
     * @param xdpi     densité horizontale réelle en dpi
     */
    public DimensionConverter(float density, float fontScale, float xdpi) {
        this.density = density;
        this.fontScale = fontScale;
        this.xdpi = xdpi;
    }

    /**
     * Convertit une dimension Android en pixels.
     *
     * @param value la chaîne à convertir (ex. {@code "16dp"})
     * @return la valeur en pixels
     * @throws ResourceException si la valeur est malformée ou utilise une unité inconnue
     */
    public float toPixels(String value) {
        if (value == null || value.isEmpty()) {
            return 0f;
        }
        String trimmed = value.trim();

        // Détecte l'unité (suffixe alphabétique)
        int unitStart = trimmed.length();
        for (int i = trimmed.length() - 1; i >= 0; i--) {
            char c = trimmed.charAt(i);
            if (Character.isLetter(c)) {
                unitStart = i;
            } else {
                break;
            }
        }
        String unit = trimmed.substring(unitStart);
        String numberPart = trimmed.substring(0, unitStart).trim();

        float number;
        try {
            number = Float.parseFloat(numberPart);
        } catch (NumberFormatException e) {
            throw new ResourceException(
                    "Valeur numérique invalide : " + value, value);
        }

        switch (unit) {
            case "dp":
            case "dip":
                return number * density;
            case "sp":
                return number * density * fontScale;
            case "px":
                return number;
            case "mm":
                return number * xdpi / 25.4f;
            case "in":
                return number * xdpi;
            case "pt":
                return number * xdpi / 72f;
            case "":
                // Pas d'unité = pixels
                return number;
            default:
                throw new ResourceException(
                        "Unité inconnue : " + unit + " (valeur : " + value + ")", value);
        }
    }

    /**
     * Convertit une dimension Android en pixels entiers (arrondi).
     *
     * @param value la chaîne à convertir
     * @return la valeur en pixels entiers
     */
    public int toPixelsInt(String value) {
        return Math.round(toPixels(value));
    }

    /**
     * Indique si une chaîne est une dimension valide.
     *
     * @param value la chaîne à tester
     * @return {@code true} si la chaîne peut être convertie
     */
    public boolean isValidDimension(String value) {
        if (value == null || value.isEmpty()) {
            return false;
        }
        try {
            toPixels(value);
            return true;
        } catch (ResourceException e) {
            return false;
        }
    }

    /**
     * @return la densité utilisée par ce convertisseur
     */
    public float getDensity() {
        return density;
    }

    /**
     * @return l'échelle de police utilisée
     */
    public float getFontScale() {
        return fontScale;
    }
}
