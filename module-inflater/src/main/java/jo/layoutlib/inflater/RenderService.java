package jo.layoutlib.inflater;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.io.StringReader;

import jo.layoutlib.inflater.bridge.util.Debug;
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
 *   <li><strong>Rendu synchrone</strong> : {@code doRender()} s'exécute
 *       intégralement sur le thread appelant (le thread principal) ; il n'y a
 *       pas de rendu concurrent à annuler. Si une demande arrive pendant un
 *       rendu (re-entrée défensive), elle est rejouée juste après via
 *       {@code reRenderRequested}</li>
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
 * <p>La validation s'appuie exclusivement sur {@link XmlPullParser} (pas
 * d'heuristique sur le texte) : un attribut entre apostrophes contenant des
 * guillemets, ou un commentaire avec un nombre impair de guillemets, restent
 * du XML valide.</p>
 *
 * @author jo@Dev
 * @since 1.0
 */
public class RenderService {

    public static final long DEFAULT_DEBOUNCE_MS = 400;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private Context context;
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

    /** Indique si {@link #release()} a été appelé (service non réutilisable). */
    private boolean released = false;

    /** Seuil (ms) au-delà duquel un rendu sur le thread UI est journalisé. */
    private static final long SLOW_RENDER_WARN_MS = 32;

    /**
     * Callback de rendu.
     *
     * <p><strong>Contrat de thread</strong> : toutes les méthodes de cette
     * interface sont invoquées sur le <em>thread principal</em> — le rendu
     * ({@code doRender()}) y est debouncé et exécuté de façon synchrone. Les
     * implémentations n ont donc <strong>pas besoin</strong> de re-poster via
     * {@code runOnUiThread(…)}.</p>
     *
     * @author jo@Dev
     */
    public interface RenderCallback {

        /**
         * Appelé quand le rendu réussit (thread principal).
         */
        void onRenderSuccess(View rootView, long timeMs, int viewCount,
                              int width, int height);

        /**
         * Appelé quand le rendu échoue (XML valide mais erreur sémantique,
         * thread principal).
         */
        void onRenderError(String message, Throwable cause);

        /**
         * Appelé quand le XML est invalide (en cours de frappe,
         * thread principal).
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
        ensureNotReleased();
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
        ensureNotReleased();
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

    /**
     * Libère le service — à appeler depuis {@code Activity.onDestroy()}.
     *
     * <p>Idempotent. Après cet appel :</p>
     * <ul>
     *   <li>les rendus en attente (debounce, re-render) sont annulés ;</li>
     *   <li>le callback est retiré (plus aucune notification vers une
     *       Activity en cours de destruction) ;</li>
     *   <li>la référence vers le {@link Context} est coupée (plus de fuite
     *       de l'Activity par le service) ;</li>
     *   <li>les setters ultérieurs lèvent une {@link IllegalStateException}.</li>
     * </ul>
     */
    public void release() {
        if (released) {
            return;
        }
        released = true;
        cancelPending();
        reRenderRequested = false;
        callback = null;
        inflater.reset();
        context = null;
    }

    /**
     * @return {@code true} si {@link #release()} a été appelé
     */
    public boolean isReleased() {
        return released;
    }

    public void setDebounceMs(long ms) {
        this.debounceMs = ms > 0 ? ms : 0;
    }

    public void setTargetDimensions(int width, int height) {
        this.targetWidth = width;
        this.targetHeight = height;
    }

    public void setRenderCallback(RenderCallback callback) {
        this.callback = callback;
    }

    /**
     * Garantit que le service n a pas été libéré.
     *
     * @throws IllegalStateException si {@link #release()} a été appelé
     */
    private void ensureNotReleased() {
        if (released) {
            throw new IllegalStateException(
                    "RenderService déjà libéré (release()) : créez une nouvelle instance");
        }
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
        if (released) {
            return;
        }
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
        if (released) {
            return;
        }
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
     * Effectue le rendu — synchrone, sur le thread principal.
     */
    private void doRender() {
        if (released) {
            return;
        }
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

            // Télémétrie : le rendu est synchrone sur le thread principal —
            // au-delà de ~2 frames (32 ms), il y a un risque de jank visible.
            // Voir le rapport d'architecture : les View doivent être créées et
            // mesurées sur le thread UI, la solution n'est PAS un thread de
            // rendu, mais une métrique pour décider d'une éventuelle
            // découpe (inflation progressive) sur gros layouts.
            if (elapsed > SLOW_RENDER_WARN_MS) {
                Debug.logWarning("render",
                        "Rendu lent sur le thread UI : " + elapsed + " ms pour "
                                + viewCount + " vues");
            }

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
     * de frappe (ex: {@code android:background="} en cours de saisie).</p>
     *
     * <p>La validation s'appuie <strong>exclusivement</strong> sur
     * {@link XmlPullParser} — aucune heuristique sur le texte (comptage de
     * guillemets, recherche de {@code =}…) qui rejetterait à tort du XML
     * valide, comme {@code <TextView android:text='Dis "bonjour' />}</code>
     * (guillemet dans une valeur entre apostrophes) ou un commentaire
     * contenant un nombre impair de guillemets.</p>
     *
     * <p>Visibilité package : testable unitairement.</p>
     *
     * @param xml le XML à valider
     * @return true si le XML est bien formé
     */
    static boolean isXmlWellFormed(String xml) {
        if (xml == null) {
            return false;
        }
        String trimmed = xml.trim();
        if (trimmed.isEmpty()) {
            return false;
        }
        try {
            XmlPullParser parser = obtainParserFactory().newPullParser();
            parser.setInput(new StringReader(trimmed));
            int event = parser.getEventType();
            while (event != XmlPullParser.END_DOCUMENT) {
                event = parser.next();
            }
            return true;
        } catch (XmlPullParserException | IOException e) {
            // XML malformé — en cours de frappe
            return false;
        } catch (RuntimeException e) {
            // kxml2 lève une RuntimeException (non contrôlée) pour certains
            // XML invalides, ex. « Undefined Prefix » quand xmlns:android
            // n'est pas encore déclaré. Un validateur ne doit jamais lever.
            return false;
        }
    }

    /**
     * Factory {@link XmlPullParserFactory} mise en cache : la recréer à chaque
     * rendu (comme à chaque frappe debouncée) est inutilement coûteux.
     *
     * <p>La factory est thread-safe pour la création de parseurs ; chaque
     * parseur reste local à son appelant.</p>
     */
    private static XmlPullParserFactory parserFactory;

    private static synchronized XmlPullParserFactory obtainParserFactory()
            throws XmlPullParserException {
        if (parserFactory == null) {
            parserFactory = XmlPullParserFactory.newInstance();
            parserFactory.setNamespaceAware(true);
        }
        return parserFactory;
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
