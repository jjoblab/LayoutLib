package jo.layoutlib.drawables;

/**
 * Type de forme pour {@code <shape>}.
 *
 * @author jo@Dev
 * @since 1.0
 */
public enum ShapeType {
    /** Rectangle (valeur par défaut). */
    RECTANGLE,
    /** Ovale / cercle. */
    OVAL,
    /** Ligne (nécessite un stroke). */
    LINE,
    /** Anneau. */
    RING
}

/**
 * Type de gradient pour {@code <gradient>}.
 *
 * @author jo@Dev
 * @since 1.0
 */
enum GradientType {
    /** Dégradé linéaire (défaut). */
    LINEAR,
    /** Dégradé radial (nécessite gradientRadius). */
    RADIAL,
    /** Dégradé en balayage (sweep). */
    SWEEP
}

/**
 * Angle de gradient (multiples de 45 degrés).
 *
 * @author jo@Dev
 * @since 1.0
 */
enum GradientAngle {
    ANGLE_0, ANGLE_45, ANGLE_90, ANGLE_135,
    ANGLE_180, ANGLE_225, ANGLE_270, ANGLE_315
}
