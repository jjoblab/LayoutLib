package jo.layoutlib.attributes.format;

import jo.layoutlib.resources.api.AttributeFormat;
import jo.layoutlib.resources.DimensionConverter;
import jo.layoutlib.resources.ResourceException;

/**
 * Formateur d'attributs de type dimension, inspiré de
 * {@code com.android.ide.common.rendering.api.AttributeFormat} de l'AOSP.
 *
 * <p>Une dimension est une valeur numérique suivie d'une unité Android :
 * {@code dp}, {@code dip}, {@code sp}, {@code px}, {@code mm}, {@code in},
 * {@code pt}.</p>
 *
 * <h2>Exemples valides</h2>
 * <ul>
 *   <li>{@code "16dp"} → 32 pixels (density=2.0)</li>
 *   <li>{@code "14sp"} → 28 pixels (fontScale=1.0)</li>
 *   <li>{@code "100px"} → 100 pixels</li>
 *   <li>{@code "1in"} → 320 pixels (xdpi=320)</li>
 * </ul>
 *
 * @author jo@Dev
 * @since 1.0
 */
public class DimensionFormatter {

    /** Convertisseur de dimensions utilisé. */
    private final DimensionConverter converter;

    /**
     * Construit un formateur avec un convertisseur.
     *
     * @param converter le convertisseur de dimensions
     */
    public DimensionFormatter(DimensionConverter converter) {
        this.converter = converter;
    }

    /**
     * Formate une valeur de dimension en pixels.
     *
     * @param value la valeur brute (ex. {@code "16dp"})
     * @return la valeur en pixels
     * @throws ResourceException si la valeur est invalide
     */
    public float format(String value) {
        if (converter == null) {
            throw new ResourceException(
                    "Aucun DimensionConverter configuré pour formater : " + value, value);
        }
        return converter.toPixels(value);
    }

    /**
     * Formate une valeur en pixels entiers (arrondi).
     *
     * @param value la valeur brute
     * @return la valeur en pixels entiers
     */
    public int formatInt(String value) {
        return Math.round(format(value));
    }

    /**
     * Indique si une valeur est une dimension valide.
     *
     * @param value la valeur à tester
     * @return {@code true} si valide
     */
    public boolean isValid(String value) {
        if (value == null || value.isEmpty()) {
            return false;
        }
        if (converter != null) {
            return converter.isValidDimension(value);
        }
        // Sans converter, on valide juste le format
        return value.matches("[0-9.]+(dp|dip|sp|px|mm|in|pt)");
    }

    /**
     * @return le convertisseur utilisé
     */
    public DimensionConverter getConverter() {
        return converter;
    }
}
