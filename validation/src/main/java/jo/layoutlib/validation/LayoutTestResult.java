package jo.layoutlib.validation;

/**
 * Résultat d'un test de layout individuel.
 *
 * <p>Cette classe stocke le résultat de l'inflation + mesure + layout d'un
 * XML de test, ainsi que les métriques collectées (temps d'exécution,
 * nombre de vues créées, erreurs éventuelles).</p>
 *
 * @author jo@Dev
 * @since 1.0
 */
public class LayoutTestResult {

    /** Nom du layout testé (ex. {@code "01_textview_simple"}). */
    private final String layoutName;

    /** XML source testé. */
    private final String xml;

    /** Indique si l'inflation a réussi. */
    private final boolean success;

    /** Temps d'inflation en millisecondes. */
    private final long inflationTimeMs;

    /** Temps de measure+layout en millisecondes. */
    private final long layoutTimeMs;

    /** Nombre de vues créées dans l'arbre. */
    private final int viewCount;

    /** Profondeur maximale de la hiérarchie. */
    private final int maxDepth;

    /** Message d'erreur si échec, ou {@code null}. */
    private final String errorMessage;

    /** Largeur mesurée de la vue racine. */
    private final int measuredWidth;

    /** Hauteur mesurée de la vue racine. */
    private final int measuredHeight;

    /**
     * Construit un résultat de test.
     *
     * @param layoutName      nom du layout
     * @param xml             XML source
     * @param success         succès ou échec
     * @param inflationTimeMs temps d'inflation
     * @param layoutTimeMs    temps de layout
     * @param viewCount       nombre de vues
     * @param maxDepth        profondeur max
     * @param errorMessage    message d'erreur, ou {@code null}
     * @param measuredWidth   largeur mesurée
     * @param measuredHeight  hauteur mesurée
     */
    public LayoutTestResult(String layoutName, String xml, boolean success,
                            long inflationTimeMs, long layoutTimeMs,
                            int viewCount, int maxDepth,
                            String errorMessage,
                            int measuredWidth, int measuredHeight) {
        this.layoutName = layoutName;
        this.xml = xml;
        this.success = success;
        this.inflationTimeMs = inflationTimeMs;
        this.layoutTimeMs = layoutTimeMs;
        this.viewCount = viewCount;
        this.maxDepth = maxDepth;
        this.errorMessage = errorMessage;
        this.measuredWidth = measuredWidth;
        this.measuredHeight = measuredHeight;
    }

    /**
     * @return le nom du layout
     */
    public String getLayoutName() {
        return layoutName;
    }

    /**
     * @return le XML source
     */
    public String getXml() {
        return xml;
    }

    /**
     * @return {@code true} si le test a réussi
     */
    public boolean isSuccess() {
        return success;
    }

    /**
     * @return le temps d'inflation en ms
     */
    public long getInflationTimeMs() {
        return inflationTimeMs;
    }

    /**
     * @return le temps de layout en ms
     */
    public long getLayoutTimeMs() {
        return layoutTimeMs;
    }

    /**
     * @return le temps total (inflation + layout) en ms
     */
    public long getTotalTimeMs() {
        return inflationTimeMs + layoutTimeMs;
    }

    /**
     * @return le nombre de vues créées
     */
    public int getViewCount() {
        return viewCount;
    }

    /**
     * @return la profondeur maximale de la hiérarchie
     */
    public int getMaxDepth() {
        return maxDepth;
    }

    /**
     * @return le message d'erreur, ou {@code null}
     */
    public String getErrorMessage() {
        return errorMessage;
    }

    /**
     * @return la largeur mesurée
     */
    public int getMeasuredWidth() {
        return measuredWidth;
    }

    /**
     * @return la hauteur mesurée
     */
    public int getMeasuredHeight() {
        return measuredHeight;
    }

    /**
     * Indique si le test respecte le seuil de performance de 200 ms.
     *
     * @return {@code true} si le temps total est < 200 ms
     */
    public boolean meetsPerformanceTarget() {
        return getTotalTimeMs() < 200;
    }

    @Override
    public String toString() {
        if (success) {
            return String.format("%s: OK (%d vues, %d ms, %dx%d)",
                    layoutName, viewCount, getTotalTimeMs(),
                    measuredWidth, measuredHeight);
        }
        return String.format("%s: ECHEC (%s)", layoutName, errorMessage);
    }
}
