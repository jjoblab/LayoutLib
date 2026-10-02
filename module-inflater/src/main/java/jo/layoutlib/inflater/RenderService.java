package jo.layoutlib.inflater;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.StringReader;

import jo.layoutlib.layout.LayoutEngineImpl;
import jo.layoutlib.resources.DimensionConverter;
import jo.layoutlib.resources.ResourceResolver;

/**
 * Service de rendu asynchrone avec debounce, inspiré du
 * {@code RenderService} d'Android Studio.
 *
 * <p>Cette classe encapsule toute la logique de rendu du mini-layoutlib :</p>
 * <ul>
 *   <li><strong>Debounce</strong> : attend un délai configurable (400ms par défaut)
 *       après la dernière modification du XML avant de rendre</li>
 *   <li><strong>Validation XML préalable</strong> : si le XML est malformé
 *       (en cours de frappe), le rendu est annulé silencieusement et le
 *       dernier rendu valide est conservé — comme Android Studio</li>
 *   <li><strong>Annulation</strong> : si un nouveau rendu est demandé pendant
 *       qu'un est en cours, le précédent est annulé</li>
 *   <li><strong>Callback</strong> : notifie l'appelant avec le résultat
 *       (vue racine + métriques) ou l'erreur</li>
 * </ul>
 *
 * <h2>Stratégie de validation (comme Android Studio)</h2>
 * <p>Android Studio ne fait pas de rendu si le XML est syntaxiquement invalide.
 * Au lieu de ça, il garde le dernier rendu valide et souligne l'erreur dans
 * l'éditeur. Cette classe reproduit ce comportement :</p>
 * <ol>
 *   <li>Avant chaque rendu, on valide que le XML est bien formé</li>
 *   <li>Si invalide → on annule silencieusement (pas de callback d'erreur)</li>
 *   <li>Si valide mais erreur sémantique (tag inconnu) → callback d'erreur</li>
 *   <li>Si valide et succès → callback de succès</li>
 * </ol>
 *
 * @author jo@Dev
 * @since 1.0
 */
public class RenderService {

    public static final long DEFAULT_DEBOUNCE_MS = 400;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Context context;
    private final BridgeInflater inflater;
    private final LayoutEngineImpl layoutEngine;

    private RenderCallback callback;
    private long debounceMs = DEFAULT_DEBOUNCE_MS;
    private Runnable pendingRender;
    private String pendingXml;
    private int targetWidth = 1080;
    private int targetHeight = 1920;
    private boolean rendering = false;
    private boolean reRenderRequested = false;

    /** Convertisseur de dimensions courant (gardé pour reconstruire l'applier). */
    private DimensionConverter dimensionConverter;

    /** Résolveur de ressources courant (gardé pour reconstruire l'applier). */
    private ResourceResolver resourceResolver;

    /** Indique si au moins un rendu réussi a été fait (pour garder le dernier valide). */
    private boolean hasValidRender = false;

    /**
     * Callback de rendu.
     *
     * @author jo@Dev
     */
    public interface RenderCallback {

        /**
         * Appelé quand le rendu réussit.
         */
        void onRenderSuccess(View rootView, long timeMs, int viewCount,
                              int width, int height);

        /**
         * Appelé quand le rendu échoue (XML valide mais erreur sémantique).
         */
        void onRenderError(String message, Throwable cause);

        /**
         * Appelé quand le XML est invalide (en cours de frappe).
         * Le preview doit garder le dernier rendu valide.
         */
        default void onXmlInvalid(String message) {
            // Ne rien faire par défaut — garder le dernier rendu
        }
    }

    public RenderService(Context context) {
        this.context = context;
        this.inflater = new BridgeInflater(context, new ViewTagRegistry(false));
        this.layoutEngine = new LayoutEngineImpl();

        float density = context.getResources().getDisplayMetrics().density;
        float fontScale = context.getResources().getDisplayMetrics().scaledDensity;
        float xdpi = context.getResources().getDisplayMetrics().xdpi;
        layoutEngine.setDensity(density);
        layoutEngine.setFontScale(fontScale);

        this.dimensionConverter =
                new DimensionConverter(density, fontScale, xdpi);
        rebuildAttributeApplier();
    }

    /**
     * Définit le résolveur de ressources du pipeline de rendu.
     *
     * <p>Le résolveur est propagé au {@link BridgeInflater} (pour les
     * {@code <include>} et références de layout) <em>et</em> à
     * l'{@link AttributeApplier}, afin que {@code @color/}, {@code @string/}
     * et {@code @dimen/} soient résolus par le résolveur quel que soit l ordre
     * des appels {@code setDimensionConverter()} / {@code setResourceResolver()}.</p>
     *
     * @param resolver le résolveur, ou {@code null} pour revenir aux
     *                 {@code Resources} natives
     */
    public void setResourceResolver(ResourceResolver resolver) {
        this.resourceResolver = resolver;
        inflater.setResourceResolver(resolver);
        rebuildAttributeApplier();
    }

    /**
     * @return le résolveur de ressources courant (peut être {@code null})
     */
    public ResourceResolver getResourceResolver() {
        return resourceResolver;
    }

    /**
     * Remplace le convertisseur de dimensions utilisé par l'applier.
     *
     * <p>L'{@link AttributeApplier} est reconstruit avec le convertisseur
     * fourni <em>et</em> le résolveur courant : le résultat est indépendant de
     * l ordre des appels {@code setDimensionConverter()} /
     * {@code setResourceResolver()}.</p>
     *
     * @param converter le nouveau convertisseur
     */
    public void setDimensionConverter(DimensionConverter converter) {
        this.dimensionConverter = converter;
        rebuildAttributeApplier();
    }

    /**
     * @return le convertisseur de dimensions courant
     */
    public DimensionConverter getDimensionConverter() {
        return dimensionConverter;
    }

    /**
     * Reconstruit l'{@link AttributeApplier} avec le convertisseur et le
     * résolveur courants, et l installe dans le inflater.
     */
    private void rebuildAttributeApplier() {
        AttributeApplier applier = new AttributeApplier(context,
                dimensionConverter, resourceResolver);
        inflater.setAttributeApplier(applier);
    }

    public void setRenderCallback(RenderCallback callback) {
        this.callback = callback;
    }

    public void setDebounceMs(long ms) {
        this.debounceMs = ms > 0 ? ms : 0;
    }

    public void setTargetDimensions(int width, int height) {
        this.targetWidth = width;
        this.targetHeight = height;
    }

    /**
     * Demande un rendu du XML avec debounce.
     *
     * <p>Si le XML est invalide (en cours de frappe), le rendu est annulé
     * silencieusement et le dernier rendu valide est conservé.</p>
     *
     * @param xml le XML à rendre
     */
    public void requestRender(String xml) {
        pendingXml = xml;

        if (pendingRender != null) {
            mainHandler.removeCallbacks(pendingRender);
        }

        if (rendering) {
            reRenderRequested = true;
            return;
        }

        pendingRender = this::doRender;
        if (debounceMs > 0) {
            mainHandler.postDelayed(pendingRender, debounceMs);
        } else {
            mainHandler.post(pendingRender);
        }
    }

    /**
     * Demande un rendu immédiat (sans debounce).
     *
     * @param xml le XML à rendre
     */
    public void requestImmediateRender(String xml) {
        pendingXml = xml;
        if (pendingRender != null) {
            mainHandler.removeCallbacks(pendingRender);
        }
        if (rendering) {
            reRenderRequested = true;
            return;
        }
        mainHandler.post(this::doRender);
    }

    /**
     * Annule tous les rendus en attente.
     */
    public void cancelPending() {
        if (pendingRender != null) {
            mainHandler.removeCallbacks(pendingRender);
            pendingRender = null;
        }
        pendingXml = null;
    }

    /**
     * Effectue le rendu.
     */
    private void doRender() {
        final String xml = pendingXml;
        if (xml == null || xml.trim().isEmpty()) {
            // XML vide — ne pas afficher d'erreur, juste ignorer
            return;
        }

        // ÉTAPE 1 : Validation XML syntaxique (comme Android Studio)
        // Si le XML est malformé (en cours de frappe), on annule silencieusement
        if (!isXmlWellFormed(xml)) {
            // XML invalide — appeler onXmlInvalid (garde le dernier rendu)
            if (callback != null) {
                callback.onXmlInvalid("XML en cours de frappe");
            }
            return;
        }

        rendering = true;

        try {
            long start = System.nanoTime();

            // ÉTAPE 2 : Inflation
            inflater.reset();
            View root = inflater.inflate(xml);

            // Assurer des LayoutParams
            if (root.getLayoutParams() == null) {
                root.setLayoutParams(new ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT));
            }

            // ÉTAPE 3 : Mesure + layout
            int width = targetWidth > 0 ? targetWidth : 1080;
            int height = targetHeight > 0 ? targetHeight : 1920;
            layoutEngine.render(root, width, height);

            // ÉTAPE 4 : Compter les vues
            int viewCount = countViews(root);

            long elapsed = (System.nanoTime() - start) / 1_000_000;

            hasValidRender = true;

            // Callback de succès
            if (callback != null) {
                callback.onRenderSuccess(root, elapsed, viewCount,
                        root.getMeasuredWidth(), root.getMeasuredHeight());
            }

        } catch (Exception e) {
            // XML valide mais erreur sémantique (tag inconnu, classe introuvable, etc.)
            String msg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            if (callback != null) {
                callback.onRenderError(msg, e);
            }
        } finally {
            rendering = false;

            if (reRenderRequested) {
                reRenderRequested = false;
                mainHandler.post(this::doRender);
            }
        }
    }

    /**
     * Valide qu'un XML est bien formé (syntaxiquement correct).
     *
     * <p>Cette méthode ne valide pas le schéma Android, uniquement la
     * conformité XML. Elle est utilisée pour filtrer les XML en cours
     * de frappe (ex: "android:background=" en cours de saisie).</p>
     *
     * <p>Vérifications :</p>
     * <ul>
     *   <li>Le XML commence par {@code <}</li>
     *   <li>Tous les tags ouverts sont fermés</li>
     *   <li>Les attributs ont des valeurs entre guillemets</li>
     *   <li>Pas de caractères illégaux</li>
     * </ul>
     *
     * @param xml le XML à valider
     * @return true si le XML est bien formé
     */
    private boolean isXmlWellFormed(String xml) {
        if (xml == null || xml.trim().isEmpty()) {
            return false;
        }

        // Vérification rapide : doit commencer par <
        String trimmed = xml.trim();
        if (!trimmed.startsWith("<")) {
            return false;
        }

        // Vérification rapide : attribut sans valeur (ex: android:text= sans ")
        // Détecte les patterns comme = sans guillemet suivant
        if (trimmed.contains("=\"") == false && trimmed.contains("='") == false) {
            // Pas d'attribut avec valeur — OK si c'est juste un tag simple
            // Mais si on a un = sans guillemet, c'est invalide
            if (trimmed.contains("=") && !trimmed.contains("=\"") && !trimmed.contains("='")) {
                return false;
            }
        }

        // Vérification : attribut incomplet (ex: android:text=" sans fermer le guillemet)
        // Compte les guillemets non échappés
        int doubleQuotes = 0;
        boolean inString = false;
        for (int i = 0; i < trimmed.length(); i++) {
            char c = trimmed.charAt(i);
            if (c == '"' && (i == 0 || trimmed.charAt(i - 1) != '\\')) {
                doubleQuotes++;
            }
        }
        if (doubleQuotes % 2 != 0) {
            // Nombre impair de guillemets → attribut incomplet
            return false;
        }

        // Validation complète avec XmlPullParser
        try {
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            factory.setNamespaceAware(true);
            XmlPullParser parser = factory.newPullParser();
            parser.setInput(new StringReader(trimmed));

            int event = parser.getEventType();
            while (event != XmlPullParser.END_DOCUMENT) {
                event = parser.next();
            }
            return true;
        } catch (XmlPullParserException e) {
            // XML malformé — en cours de frappe
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Compte le nombre de vues dans un arbre.
     *
     * @param view la vue racine
     * @return le nombre total
     */
    private int countViews(View view) {
        if (view == null) return 0;
        int count = 1;
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                count += countViews(group.getChildAt(i));
            }
        }
        return count;
    }

    public BridgeInflater getInflater() {
        return inflater;
    }

    public LayoutEngineImpl getLayoutEngine() {
        return layoutEngine;
    }

    public boolean isRendering() {
        return rendering;
    }

    public boolean hasValidRender() {
        return hasValidRender;
    }
}
