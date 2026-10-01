package jo.layoutlib.layout.cassowary;

/**
 * Force d'une contrainte Cassowary.
 *
 * <p>Le solver Cassowary utilise un système de poids pour prioriser les
 * contraintes. Une contrainte {@link #REQUIRED} ne peut jamais être violée,
 * tandis que les contraintes {@link #STRONG}, {@link #MEDIUM} et
 * {@link #WEAK} peuvent être relaxées si nécessaire pour satisfaire les
 * contraintes plus fortes.</p>
 *
 * <h2>Valeurs numériques</h2>
 * <p>Les valeurs suivent la convention Cassowary :</p>
 * <ul>
 *   <li>REQUIRED = 1 000 000 (le plus fort)</li>
 *   <li>STRONG = 1 000</li>
 *   <li>MEDIUM = 100</li>
 *   <li>WEAK = 1</li>
 * </ul>
 *
 * @author jo@Dev
 * @since 1.0
 */
public final class Strength {

    /** Force requise — ne peut pas être violée. */
    public static final double REQUIRED = 1_000_000.0;

    /** Force forte — peut être violée seulement par REQUIRED. */
    public static final double STRONG = 1_000.0;

    /** Force moyenne. */
    public static final double MEDIUM = 100.0;

    /** Force faible — la plus facile à violer. */
    public static final double WEAK = 1.0;

    /** Constructeur privé. */
    private Strength() {
    }

    /**
     * Crée une force personnalisée.
     *
     * @param value la valeur de la force
     * @return la force
     */
    public static double create(double value) {
        if (value < 0) {
            throw new IllegalArgumentException("La force ne peut pas être négative : " + value);
        }
        return value;
    }

    /**
     * Crée une force en combinant a, b, c.
     *
     * <p>Convention Cassowary : {@code a * 1 000 000 + b * 1 000 + c}</p>
     *
     * @param a poids fort
     * @param b poids moyen
     * @param c poids faible
     * @return la force
     */
    public static double create(double a, double b, double c) {
        return a * REQUIRED + b * STRONG + c * WEAK;
    }
}
