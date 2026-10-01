package jo.layoutlib.attributes.format;

import java.util.HashMap;
import java.util.Map;

import jo.layoutlib.attributes.AttributeFormat;
import jo.layoutlib.resources.DimensionConverter;

/**
 * Registre central des formateurs d'attributs.
 *
 * <p>Cette classe maintient une map des formateurs disponibles par format
 * Android (dimension, color, integer, float, boolean, fraction, enum, flag).
 * Elle est utilisée par l'AttributeRegistry pour appliquer les valeurs
 * d'attributs custom.</p>
 *
 * @author jo@Dev
 * @since 1.0
 */
public class FormatRegistry {

    /** Map des formateurs par format. */
    private final Map<AttributeFormat, Object> formatters = new HashMap<>();

    /** Convertisseur de dimensions partagé. */
    private final DimensionConverter dimensionConverter;

    /**
     * Construit un registre avec un convertisseur de dimensions.
     *
     * @param dimensionConverter le convertisseur (peut être {@code null})
     */
    public FormatRegistry(DimensionConverter dimensionConverter) {
        this.dimensionConverter = dimensionConverter;
        registerDefaults();
    }

    /**
     * Enregistre les formateurs par défaut.
     */
    private void registerDefaults() {
        formatters.put(AttributeFormat.DIMENSION,
                new DimensionFormatter(dimensionConverter));
        formatters.put(AttributeFormat.COLOR, new ColorFormatter());
        formatters.put(AttributeFormat.INTEGER, new IntegerFormatter());
        formatters.put(AttributeFormat.FLOAT, new FloatFormatter());
        formatters.put(AttributeFormat.BOOLEAN, new BooleanFormatter());
        formatters.put(AttributeFormat.FRACTION, new FractionFormatter());
        // ENUM et FLAG nécessitent des définitions spécifiques par attribut
    }

    /**
     * Récupère le formateur d'un format.
     *
     * @param format le format
     * @return le formateur, ou {@code null} si non enregistré
     */
    public Object getFormatter(AttributeFormat format) {
        return formatters.get(format);
    }

    /**
     * Enregistre un formateur pour un format.
     *
     * @param format    le format
     * @param formatter le formateur
     */
    public void register(AttributeFormat format, Object formatter) {
        if (format != null && formatter != null) {
            formatters.put(format, formatter);
        }
    }

    /**
     * Crée un formateur d'enum avec les valeurs données.
     *
     * @param enumValues map nom → valeur
     * @return le formateur configuré
     */
    public EnumFormatter createEnumFormatter(Map<String, Integer> enumValues) {
        EnumFormatter formatter = new EnumFormatter();
        if (enumValues != null) {
            for (Map.Entry<String, Integer> entry : enumValues.entrySet()) {
                formatter.addValue(entry.getKey(), entry.getValue());
            }
        }
        return formatter;
    }

    /**
     * Crée un formateur de flags avec les valeurs données.
     *
     * @param flagValues map nom → valeur
     * @return le formateur configuré
     */
    public FlagFormatter createFlagFormatter(Map<String, Integer> flagValues) {
        FlagFormatter formatter = new FlagFormatter();
        if (flagValues != null) {
            for (Map.Entry<String, Integer> entry : flagValues.entrySet()) {
                formatter.addValue(entry.getKey(), entry.getValue());
            }
        }
        return formatter;
    }

    /**
     * Indique si un format est supporté.
     *
     * @param format le format
     * @return {@code true} si un formateur existe
     */
    public boolean isSupported(AttributeFormat format) {
        return formatters.containsKey(format);
    }

    /**
     * @return le convertisseur de dimensions
     */
    public DimensionConverter getDimensionConverter() {
        return dimensionConverter;
    }
}
