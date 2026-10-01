package jo.layoutlib.resources.api;

/**
 * Densité d'affichage Android, inspiré de
 * {@code com.android.resources.Density} de l'AOSP.
 *
 * <p>Représente la densité d'un écran Android en dpi (dots per inch).
 * Plus la densité est élevée, plus il y a de pixels par pouce physique.</p>
 *
 * <h2>Densités courantes</h2>
 * <ul>
 *   <li>{@link #LOW} (ldpi) : 120 dpi</li>
 *   <li>{@link #MEDIUM} (mdpi) : 160 dpi — référence (density = 1.0)</li>
 *   <li>{@link #HIGH} (hdpi) : 240 dpi</li>
 *   <li>{@link #XHIGH} (xhdpi) : 320 dpi</li>
 *   <li>{@link #XXHIGH} (xxhdpi) : 480 dpi</li>
 *   <li>{@link #XXXHIGH} (xxxhdpi) : 640 dpi</li>
 * </ul>
 *
 * @author jo@Dev
 * @since 1.0
 */
public enum Density {

    /** Densité basse (ldpi). */
    LOW(120, "ldpi"),
    /** Densité moyenne (mdpi) — référence. */
    MEDIUM(160, "mdpi"),
    /** Densité haute (hdpi). */
    HIGH(240, "hdpi"),
    /** Densité TV (tvdpi). */
    TV(213, "tvdpi"),
    /** Densité extra-haute (xhdpi). */
    XHIGH(320, "xhdpi"),
    /** Densité extra-extra-haute (xxhdpi). */
    XXHIGH(480, "xxhdpi"),
    /** Densité extra-extra-extra-haute (xxxhdpi). */
    XXXHIGH(640, "xxxhdpi"),
    /** Aucune densité (nodpi). */
    NODPI(0, "nodpi"),
    /** Densité ANY (anydpi). */
    ANYDPI(0xFFFE, "anydpi");

    /** Valeur en dpi. */
    private final int dpi;

    /** Nom court du qualifier. */
    private final String qualifier;

    Density(int dpi, String qualifier) {
        this.dpi = dpi;
        this.qualifier = qualifier;
    }

    /**
     * @return la valeur en dpi
     */
    public int getDpiValue() {
        return dpi;
    }

    /**
     * @return le facteur de densité (dpi / 160)
     */
    public float getDensity() {
        return dpi / 160.0f;
    }

    /**
     * @return le nom du qualifier (ex. {@code "xhdpi"})
     */
    public String getQualifier() {
        return qualifier;
    }

    /**
     * Récupère la densité depuis sa valeur en dpi.
     *
     * @param dpi la valeur en dpi
     * @return l'enum, ou {@link #MEDIUM} si inconnue
     */
    public static Density fromDpi(int dpi) {
        if (dpi <= 0) {
            return NODPI;
        }
        for (Density d : values()) {
            if (d.dpi == dpi) {
                return d;
            }
        }
        // Trouve la densité la plus proche
        Density closest = MEDIUM;
        int minDiff = Math.abs(MEDIUM.dpi - dpi);
        for (Density d : values()) {
            if (d.dpi <= 0) continue;
            int diff = Math.abs(d.dpi - dpi);
            if (diff < minDiff) {
                minDiff = diff;
                closest = d;
            }
        }
        return closest;
    }

    /**
     * Récupère la densité depuis son qualifier.
     *
     * @param qualifier le nom du qualifier (ex. {@code "xhdpi"})
     * @return l'enum, ou {@code null} si inconnu
     */
    public static Density fromQualifier(String qualifier) {
        if (qualifier == null) {
            return null;
        }
        for (Density d : values()) {
            if (d.qualifier.equals(qualifier)) {
                return d;
            }
        }
        return null;
    }

    /**
     * Calcule la densité depuis un facteur de densité.
     *
     * @param density le facteur (1.0 = mdpi, 2.0 = xhdpi, etc.)
     * @return l'enum correspondante
     */
    public static Density fromDensity(float density) {
        return fromDpi(Math.round(density * 160));
    }
}
