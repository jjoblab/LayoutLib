package jo.layoutlib.drawables;

/**
 * Utilitaire de parsing d'attributs de drawables.
 *
 * <p>Cette classe centralise les conversions fréquemment utilisées par les
 * parsers de drawables : dimensions, couleurs, gradients, etc. Elle factorise
 * la logique pour éviter la duplication entre {@link ShapeParser},
 * {@link SelectorParser}, etc.</p>
 *
 * @author jo@Dev
 * @since 1.0
 */
public final class DrawableAttributeParser {

    /** Constructeur privé : classe utilitaire. */
    private DrawableAttributeParser() {
    }

    /**
     * Parse un angle de gradient.
     *
     * <p>Android accepte les multiples de 45 : 0, 45, 90, 135, 180, 225, 270, 315.
     * L'angle est ensuite converti en enum {@link GradientAngle}.</p>
     *
     * @param angleStr l'angle en degrés (ex. {@code "90"})
     * @return l'enum correspondante
     * @throws DrawableException si l'angle n'est pas un multiple de 45
     */
    public static GradientAngle parseGradientAngle(String angleStr) {
        if (angleStr == null || angleStr.isEmpty()) {
            return GradientAngle.ANGLE_0;
        }
        int angle;
        try {
            angle = Integer.parseInt(angleStr.trim());
        } catch (NumberFormatException e) {
            throw new DrawableException(
                    "Angle de gradient invalide : " + angleStr, "gradient", e);
        }
        switch (angle) {
            case 0:   return GradientAngle.ANGLE_0;
            case 45:  return GradientAngle.ANGLE_45;
            case 90:  return GradientAngle.ANGLE_90;
            case 135: return GradientAngle.ANGLE_135;
            case 180: return GradientAngle.ANGLE_180;
            case 225: return GradientAngle.ANGLE_225;
            case 270: return GradientAngle.ANGLE_270;
            case 315: return GradientAngle.ANGLE_315;
            default:
                throw new DrawableException(
                        "Angle doit être un multiple de 45 : " + angle, "gradient");
        }
    }

    /**
     * Parse un type de forme pour {@code <shape>}.
     *
     * @param typeStr le type (rectangle, oval, line, ring)
     * @return l'enum correspondante
     * @throws DrawableException si le type est inconnu
     */
    public static ShapeType parseShapeType(String typeStr) {
        if (typeStr == null || typeStr.isEmpty()) {
            return ShapeType.RECTANGLE;
        }
        switch (typeStr.toLowerCase()) {
            case "rectangle": return ShapeType.RECTANGLE;
            case "oval":      return ShapeType.OVAL;
            case "line":      return ShapeType.LINE;
            case "ring":      return ShapeType.RING;
            default:
                throw new DrawableException(
                        "Type de shape inconnu : " + typeStr, "shape");
        }
    }

    /**
     * Parse une visibilité de gradient (type de dégradé).
     *
     * @param typeStr le type (linear, radial, sweep)
     * @return l'enum correspondante
     */
    public static GradientType parseGradientType(String typeStr) {
        if (typeStr == null || typeStr.isEmpty()) {
            return GradientType.LINEAR;
        }
        switch (typeStr.toLowerCase()) {
            case "linear": return GradientType.LINEAR;
            case "radial": return GradientType.RADIAL;
            case "sweep":  return GradientType.SWEEP;
            default:
                throw new DrawableException(
                        "Type de gradient inconnu : " + typeStr, "gradient");
        }
    }
}
