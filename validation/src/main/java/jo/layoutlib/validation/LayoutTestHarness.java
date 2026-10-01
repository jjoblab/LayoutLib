package jo.layoutlib.validation;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

import jo.layoutlib.inflater.BridgeInflater;
import jo.layoutlib.inflater.InflateException;
import jo.layoutlib.inflater.ViewTagRegistry;
import jo.layoutlib.layout.LayoutEngineImpl;
import jo.layoutlib.resources.ResourceResolver;

/**
 * Harness de test qui exécute un layout XML à travers le pipeline complet
 * du mini-layoutlib : inflation → mesure → layout.
 *
 * <p>Cette classe est utilisée par les tests d'intégration pour valider que
 * le mini-layoutlib peut rendre correctement les 50 layouts de test. Elle
 * collecte les métriques de performance (temps d'inflation, temps de layout,
 * nombre de vues, profondeur) et détecte les erreurs.</p>
 *
 * <h2>Métriques collectées</h2>
 * <ul>
 *   <li>Temps d'inflation (ms)</li>
 *   <li>Temps de measure+layout (ms)</li>
 *   <li>Nombre de vues créées</li>
 *   <li>Profondeur maximale de la hiérarchie</li>
 *   <li>Dimensions mesurées de la vue racine</li>
 *   <li>Erreurs éventuelles</li>
 * </ul>
 *
 * <h2>Exemple d'usage</h2>
 * <pre>{@code
 * LayoutTestHarness harness = new LayoutTestHarness(context);
 * LayoutTestResult result = harness.runTest("01_textview", xml, 1080, 1920);
 * if (result.isSuccess()) {
 *     System.out.println("OK : " + result.getViewCount() + " vues en " + result.getTotalTimeMs() + "ms");
 * }
 * }</pre>
 *
 * @author jo@Dev
 * @since 1.0
 */
public class LayoutTestHarness {

    /** Contexte Android. */
    private final Context context;

    /** Inflater utilisé pour les tests. */
    private final BridgeInflater inflater;

    /** Moteur de layout. */
    private final LayoutEngineImpl layoutEngine;

    /** Résolveur de resources optionnel. */
    private ResourceResolver resourceResolver;

    /** Largeur par défaut pour les tests. */
    private int defaultWidth = 1080;

    /** Hauteur par défaut pour les tests. */
    private int defaultHeight = 1920;

    /**
     * Construit un harness avec un contexte.
     *
     * @param context contexte Android
     */
    public LayoutTestHarness(Context context) {
        this.context = context;
        this.inflater = new BridgeInflater(context, new ViewTagRegistry(false));
        this.layoutEngine = new LayoutEngineImpl();
    }

    /**
     * Définit le résolveur de resources à utiliser.
     *
     * @param resolver le résolveur
     */
    public void setResourceResolver(ResourceResolver resolver) {
        this.resourceResolver = resolver;
        this.inflater.setResourceResolver(resolver);
    }

    /**
     * Définit les dimensions par défaut pour les tests.
     *
     * @param width  largeur en pixels
     * @param height hauteur en pixels
     */
    public void setDefaultDimensions(int width, int height) {
        this.defaultWidth = width;
        this.defaultHeight = height;
    }

    /**
     * Exécute un test de layout avec les dimensions par défaut.
     *
     * @param layoutName nom du layout (pour le rapport)
     * @param xml        XML source
     * @return le résultat du test
     */
    public LayoutTestResult runTest(String layoutName, String xml) {
        return runTest(layoutName, xml, defaultWidth, defaultHeight);
    }

    /**
     * Exécute un test de layout avec des dimensions spécifiques.
     *
     * @param layoutName nom du layout
     * @param xml        XML source
     * @param width      largeur en pixels
     * @param height     hauteur en pixels
     * @return le résultat du test
     */
    public LayoutTestResult runTest(String layoutName, String xml, int width, int height) {
        if (xml == null || xml.trim().isEmpty()) {
            return new LayoutTestResult(layoutName, xml, false,
                    0, 0, 0, 0, "XML vide ou null", 0, 0);
        }

        long inflationStart = System.nanoTime();
        View root;
        try {
            root = inflater.inflate(xml);
        } catch (InflateException e) {
            return new LayoutTestResult(layoutName, xml, false,
                    elapsedMs(inflationStart), 0, 0, 0,
                    "Inflation échouée : " + e.getMessage(), 0, 0);
        } catch (Exception e) {
            return new LayoutTestResult(layoutName, xml, false,
                    elapsedMs(inflationStart), 0, 0, 0,
                    "Exception : " + e.getClass().getSimpleName() + " : " + e.getMessage(),
                    0, 0);
        }
        long inflationTime = elapsedMs(inflationStart);

        // Collecte des métriques sur l'arbre
        int viewCount = countViews(root);
        int maxDepth = computeMaxDepth(root);

        // Measure + layout
        long layoutStart = System.nanoTime();
        try {
            layoutEngine.render(root, width, height);
        } catch (Exception e) {
            return new LayoutTestResult(layoutName, xml, false,
                    inflationTime, elapsedMs(layoutStart),
                    viewCount, maxDepth,
                    "Layout échoué : " + e.getMessage(), 0, 0);
        }
        long layoutTime = elapsedMs(layoutStart);

        return new LayoutTestResult(layoutName, xml, true,
                inflationTime, layoutTime,
                viewCount, maxDepth, null,
                root.getMeasuredWidth(), root.getMeasuredHeight());
    }

    /**
     * Exécute une série de tests.
     *
     * @param tests liste de (nom, XML) à tester
     * @return la liste des résultats
     */
    public List<LayoutTestResult> runTests(List<LayoutTestCase> tests) {
        List<LayoutTestResult> results = new ArrayList<>();
        for (LayoutTestCase test : tests) {
            results.add(runTest(test.getName(), test.getXml(),
                    test.getWidth(), test.getHeight()));
        }
        return results;
    }

    /**
     * Compte le nombre de vues dans un arbre.
     *
     * @param view la vue racine
     * @return le nombre total de vues
     */
    private int countViews(View view) {
        if (view == null) {
            return 0;
        }
        int count = 1;
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                count += countViews(group.getChildAt(i));
            }
        }
        return count;
    }

    /**
     * Calcule la profondeur maximale d'un arbre de vues.
     *
     * @param view la vue racine
     * @return la profondeur (1 pour une vue seule)
     */
    private int computeMaxDepth(View view) {
        if (view == null) {
            return 0;
        }
        if (!(view instanceof ViewGroup)) {
            return 1;
        }
        ViewGroup group = (ViewGroup) view;
        int maxChildDepth = 0;
        for (int i = 0; i < group.getChildCount(); i++) {
            maxChildDepth = Math.max(maxChildDepth, computeMaxDepth(group.getChildAt(i)));
        }
        return 1 + maxChildDepth;
    }

    /**
     * Calcule le temps écoulé en millisecondes depuis un timestamp de départ.
     *
     * @param startNanos timestamp de départ en nanosecondes
     * @return temps écoulé en ms
     */
    private long elapsedMs(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000;
    }

    /**
     * @return le contexte Android
     */
    public Context getContext() {
        return context;
    }

    /**
     * @return l'inflater utilisé
     */
    public BridgeInflater getInflater() {
        return inflater;
    }

    /**
     * @return le moteur de layout
     */
    public LayoutEngineImpl getLayoutEngine() {
        return layoutEngine;
    }
}
