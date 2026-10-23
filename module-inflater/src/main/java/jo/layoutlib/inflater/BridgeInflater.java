package jo.layoutlib.inflater;

import android.util.Log;
import android.animation.LayoutTransition;
import android.content.Context;
import android.view.ContextThemeWrapper;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ExpandableListView;
import android.widget.ListView;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import jo.layoutlib.inflater.bridge.MockView;
import jo.layoutlib.inflater.bridge.android.NopAttributeSet;
import jo.layoutlib.inflater.bridge.binding.AdapterHelper;
import jo.layoutlib.inflater.bridge.binding.FakeAdapter;
import jo.layoutlib.inflater.bridge.binding.FakeRecyclerAdapter;
import jo.layoutlib.resources.ResourceResolver;

/**
 * Inflater principal du mini-layoutlib.
 *
 * <p>Inspiré de {@code com.android.layoutlib.bridge.impl.BridgeInflater} de
 * l'AOSP, cette classe est responsable de transformer un XML de layout en un
 * arbre de {@link View} Android. Elle orchestre :</p>
 *
 * <ul>
 *   <li>le pré-traitement du XML (normalisation des attributs
 *       {@code tools:*} et {@code *Start} / {@code *End}) ;</li>
 *   <li>la résolution des noms de tags vers les classes Design ou natives
 *       via {@link ViewTagRegistry} ;</li>
 *   <li>l'instanciation des vues par réflexion via {@link ViewFactory} ;</li>
 *   <li>l'application des attributs de base (id, visibility, padding,
 *       layout_*) — l'application fine est déléguée aux Callers du
 *       module-attributes ;</li>
 *   <li>la gestion des tags spéciaux {@code <include>}, {@code <merge>} et
 *       {@code <ViewStub>} ;</li>
 *   <li>la gestion optionnelle des layouts inclus via un
 *       {@link ResourceResolver}.</li>
 * </ul>
 *
 * <h2>Exemple d'usage</h2>
 * <pre>{@code
 * BridgeInflater inflater = new BridgeInflater(context);
 * inflater.setResourceResolver(resourceResolver);
 * View root = inflater.inflate("<LinearLayout xmlns:android=\"...\">...</LinearLayout>");
 * }</pre>
 *
 * <h2>Différences avec le layoutlib original</h2>
 * <ul>
 *   <li>Pas de support du renderer complet (mesure/layout/draw) — c'est le
 *       travail du module-layout.</li>
 *   <li>Support des adapters (v3.6.0) : ListView/GridView/Spinner et
 *       RecyclerView reçoivent un {@link FakeAdapter} d'aperçu construit
 *       depuis {@code tools:listitem}/{@code tools:itemCount} (miroir
 *       {@code AdapterBinding} de la référence — étude PE14 § 2.4-3).</li>
 *   <li>Les attributs custom {@code app:*} sont appliqués via le
 *       module-attributes.</li>
 * </ul>
 *
 * @author jo@Dev
 * @since 1.0
 */
public class BridgeInflater {

    /** Tag de journalisation. */
    private static final String TAG = "BridgeInflater";

    /** URI de l'espace tools: — convention d'aperçu Android (miroir
     * TOOLS_URI de la référence). */
    private static final String URI_TOOLS =
            "http://schemas.android.com/tools";

    /** Contexte Android. */
    private final Context context;

    /** Registre des tags pour la résolution nom court → classe. */
    private final ViewTagRegistry tagRegistry;

    /** Fabrique de vues par réflexion. */
    private final ViewFactory viewFactory;

    /** Résolveur de ressources optionnel pour {@code <include layout="@layout/foo" />}. */
    private ResourceResolver resourceResolver;

    /** Applier d'attributs (lit XML et applique via setters natifs). */
    private AttributeApplier attributeApplier;

    /** Ids des vues contraintes ConstraintLayout (legacy, conservé pour compat). */
    private final Map<View, String> constraintViewIds = new HashMap<>();

    /** Attributs de contrainte capturés pendant l'inflation (parser sur
     *  START_TAG) : vue → map attribut → valeur. Consommé par le
     *  ConstraintSolver (module-layout) via RenderService. */
    private final Map<View, Map<String, String>> constraintAttributes = new HashMap<>();

    /** Map des vues indexées par id. */
    private final Map<Integer, View> idToView = new HashMap<>();

    /**
     * Map NOM → ID des ids dynamiques (v3.5.1 — S3-étude § 2.3.3) : STABLE
     * pour tout le cycle de vie de l'inflater — deux vues référençant le
     * même {@code @id/x} obtiennent le MÊME id, l'ordre de résolution ne
     * compte plus (ancêtres {@code layout_below}, contraintes, ancres
     * réparés). Miroir {@code bridge/util/DynamicIdMap} de la référence.
     */
    private final jo.layoutlib.inflater.bridge.util.DynamicIdMap dynamicIds =
            new jo.layoutlib.inflater.bridge.util.DynamicIdMap();

    /** Indique si le inflater doit lever une exception sur les attributs inconnus. */
    private boolean strictMode = false;

    /** Hook AOSP d'interception de création de vues (ADR 0116 — P6/L10).
     *  Consulté AVANT la création par défaut ; retour null → ViewFactory. */
    private LayoutInflater.Factory2 factory2;

    /** Nombre de vues fournies par la Factory2 (introspection/tests). */
    private int factoryViewCount = 0;

    /** Nombre de LayoutTransition installés via animateLayoutChanges
     *  (ADR 0116 — P6/L11, introspection/tests). */
    private int layoutTransitionCount = 0;

    /**
     * Le QUALIFICATEUR de direction de rendu (v3.7.0 — étude PE14 § 2.4-5,
     * S8) : {@link DirectionRendu#INDEFINI} par défaut (le framework seul
     * décide), sinon posé sur la RACINE à chaque inflation — les enfants
     * héritent ({@code LAYOUT_DIRECTION_INHERIT}), les marges relatives
     * {@code start}/{@code end} se résolvent par
     * {@link DirectionRendu#appliquer(View, int)}.
     */
    private int layoutDirectionRendu = DirectionRendu.INDEFINI;

    /**
     * Le VIEWCOOKIE de chaque vue inflatée (v3.7.0 — étude PE14 § 2.4-4,
     * S8 — miroir du cookie {@code ViewInfo} de la référence) : l'identité
     * du NŒUD XML SOURCE — « site:ordinal », l'ordinal étant le rang du
     * START_TAG dans la marche du document, le site la CHAÎNE des
     * {@code <include>} traversés. Fiabilise sélection/surlignage : deux
     * vues identiques de nœuds DIFFÉRENTS (même layout inclus DEUX fois)
     * portent des cookies DISTINCTS, et un écart de COMPTE (include
     * résolu nativement, ViewStub, adapter) ne désarme PLUS la suite —
     * l'appariement se fait par IDENTITÉ, pas par position.
     *
     * <p>Vit sur l'inflater RACINE (les enfants de {@code <include>} et de
     * l'aperçu de listes héritent du MÊME registre par
     * {@link #registreViewCookies}) ; {@link java.util.ref.WeakHashMap} —
     * les vues d'un rendu précédent partent avec elles.</p>
     */
    private final java.util.Map<View, String> viewCookies =
            new java.util.WeakHashMap<>();

    /**
     * Le registre PARTAGÉ des cookies (l'inflater RACINE pointe sa propre
     * map ; les inflaters ENFANTS — {@code <include>}, rangées d'aperçu —
     * pointent celle de la RACINE pour que TOUTES les vues d'un rendu
     * vivent au même endroit).
     */
    private java.util.Map<View, String> registreViewCookies = viewCookies;

    /** Le SITE cookie courant (chaîne des {@code <include>} traversés, "" = document principal). */
    private String siteCookies = "";

    /** Le compteur d'ordinaux du document courant (rang du START_TAG, 0-based). */
    private int ordinalNoeud = 0;

    /** Registre d'attributs (module-attributes) pour le mode strict. */
    private jo.layoutlib.attributes.AttributeRegistry attributeRegistry;

    /**
     * Construit un inflater par défaut.
     *
     * <p>v3.7.0 — S8 (étude PE14 § 2.5, hygiène) : le registre par défaut
     * ne cherche PLUS les classes « Design » ({@code jo.layoutlib.design.*})
     * — ce paquet n'a JAMAIS existé dans le dépôt et le mode
     * {@code preferDesignClasses} faisait partir TOUT tag court vers
     * MockView (le piège documenté par l'étude). Les classes VRAIES sont
     * les natives du framework + AndroidX/Material embarqués (v3.4.9).</p>
     *
     * @param context contexte Android
     */
    public BridgeInflater(Context context) {
        this(context, new ViewTagRegistry(false));
    }

    /**
     * Construit un inflater avec un registre de tags personnalisé.
     *
     * @param context     contexte Android
     * @param tagRegistry registre de tags à utiliser
     */
    public BridgeInflater(Context context, ViewTagRegistry tagRegistry) {
        if (context == null) {
            throw new IllegalArgumentException("Le contexte ne peut pas être null");
        }
        this.context = context;
        this.tagRegistry = tagRegistry;
        this.viewFactory = new ViewFactory(context, tagRegistry);
    }

    /**
     * Définit le résolveur de ressources utilisé pour résoudre les
     * {@code <include layout="@layout/foo" />} et les références
     * {@code @color/}, {@code @string/}, etc.
     *
     * <p>Le résolveur est également propagé à l'{@link AttributeApplier}
     * courant (s il existe), afin que la résolution des attributs
     * {@code @color/}/{@code @string/}/{@code @dimen/} ne dépende pas de
     * l ordre d appel des setters.</p>
     *
     * @param resolver le résolveur, ou {@code null} pour désactiver la résolution
     */
    public void setResourceResolver(ResourceResolver resolver) {
        this.resourceResolver = resolver;
        if (attributeApplier != null) {
            attributeApplier.setResourceResolver(resolver);
        }
    }

    /**
     * @return le résolveur de resources configuré
     */
    public ResourceResolver getResourceResolver() {
        return resourceResolver;
    }

    /**
     * Définit l'AttributeApplier à utiliser pour appliquer les attributs XML.
     *
     * @param applier l'applier
     */
    public void setAttributeApplier(AttributeApplier applier) {
        this.attributeApplier = applier;
    }

    /**
     * @return l'AttributeApplier courant (peut être {@code null})
     */
    public AttributeApplier getAttributeApplier() {
        return attributeApplier;
    }

    /**
     * Définit le hook {@link LayoutInflater.Factory2} d'interception de la
     * création des vues (ADR 0116 — P6/L10).
     *
     * <p>Sémantique AOSP : la factory est consultée pour chaque tag de vue
     * standard (hors {@code <include>}/{@code <merge>}/{@code <requestFocus>}/
     * {@code <blink>}) AVANT la création par défaut. Si elle retourne une
     * vue, celle-ci est utilisée telle quelle et suit le pipeline complet
     * (attributs, LayoutParams, enfants) ; si elle retourne {@code null}, la
     * création par défaut ({@link ViewFactory} par réflexion) prend le
     * relais. Une factory qui lève interrompt l'inflation avec une
     * {@link InflateException} (comme l'AOSP).</p>
     *
     * <p><strong>Contrat AOSP conservé</strong> : l'{@code AttributeSet}
     * passé à la factory est {@code null} — le parser XML pull de LayoutLib
     * n'est pas un {@code AttributeSet} Android (cf. l'avertissement
     * XmlBlock.Parser sur Android 14+) ; les attributs XML sont appliqués
     * ensuite par l'{@link AttributeApplier}, comme pour toute vue.</p>
     *
     * <p>Comme dans l'AOSP, une factory ne peut être définie qu'une fois :
     * un second appel lève {@link IllegalStateException}. Le hook est
     * propagé aux inflaters enfants créés pour les {@code <include>}.</p>
     *
     * @param factory la factory, ou {@code null} pour désactiver le hook
     *                (uniquement si aucune factory n'a encore été définie)
     * @throws IllegalStateException si une factory est déjà définie
     */
    public void setFactory2(LayoutInflater.Factory2 factory) {
        if (this.factory2 != null && factory != null) {
            throw new IllegalStateException(
                    "Une Factory2 est déjà définie sur cet inflater "
                            + "(contrat AOSP LayoutInflater.setFactory2)");
        }
        this.factory2 = factory;
    }

    /**
     * @return la Factory2 courante, ou {@code null}
     */
    public LayoutInflater.Factory2 getFactory2() {
        return factory2;
    }

    /**
     * @return le nombre de vues effectivement fournies par la Factory2
     *         lors des inflations (introspection/tests, ADR 0116 — P6/L10)
     */
    public int getFactoryViewCount() {
        return factoryViewCount;
    }

    /**
     * @return le nombre de LayoutTransition installés via
     *         {@code android:animateLayoutChanges="true"}
     *         (introspection/tests, ADR 0116 — P6/L11)
     */
    public int getAppliedLayoutTransitionCount() {
        return layoutTransitionCount;
    }

    /**
     * Active ou désactive le mode strict.
     *
     * <p>En mode strict, tout attribut non reconnu lève une
     * {@link InflateException}. En mode non strict (par défaut), les attributs
     * inconnus sont ignorés silencieusement avec un log.</p>
     *
     * @param strict {@code true} pour activer le mode strict
     */
    public void setStrictMode(boolean strict) {
        this.strictMode = strict;
    }

    /**
     * Définit le registre d'attributs utilisé par le mode strict
     * (intégration du module-attributes).
     *
     * <p>En mode strict, chaque attribut {@code android:*}/{@code app:*} du
     * layout est vérifié contre ce registre : un attribut inconnu lève une
     * {@link InflateException}. Sans registre, le mode strict garde son
     * comportement historique (vérifications ponctuelles).</p>
     *
     * @param attributeRegistry le registre (ex. {@code CompositeAttributeRegistry}
     *                           combinant framework + Material + AndroidX),
     *                           ou {@code null} pour désactiver
     */
    public void setAttributeRegistry(jo.layoutlib.attributes.AttributeRegistry attributeRegistry) {
        this.attributeRegistry = attributeRegistry;
    }

    /**
     * @return le registre d'attributs du mode strict (peut être {@code null})
     */
    public jo.layoutlib.attributes.AttributeRegistry getAttributeRegistry() {
        return attributeRegistry;
    }

    /**
     * Vérifie les attributs du START_TAG courant contre le registre
     * (mode strict uniquement).
     *
     * <p>Visibilité package : testable unitairement. Les attributs de
     * l'espace <em>tools</em> (preview de listes v3.6.0) sont IGNORÉS :
     * ils relèvent de la convention d'aperçu, pas du vocabulaire
     * {@code android:*}/{@code app:*} du document.</p>
     *
     * @throws InflateException si un attribut est inconnu du registre
     */
    void checkKnownAttributes(XmlPullParser parser) {
        if (!strictMode || attributeRegistry == null) {
            return;
        }
        for (int i = 0; i < parser.getAttributeCount(); i++) {
            String name = parser.getAttributeName(i);
            if (name == null || name.isEmpty()) {
                continue;
            }
            if (URI_TOOLS.equals(parser.getAttributeNamespace(i))) {
                continue;
            }
            if (!attributeRegistry.isKnownAttribute(name)) {
                throw new InflateException(
                        "Attribut inconnu en mode strict : '" + name
                                + "' (non déclaré dans le registre d'attributs)");
            }
        }
    }

    /**
     * Inflate un layout XML en arbre de vues.
     *
     * @param xml le XML source du layout
     * @return la vue racine, jamais {@code null}
     * @throws InflateException si le XML est malformé ou si l'inflation échoue
     */
    public View inflate(String xml) {
        return inflate(xml, null);
    }

    /**
     * Inflate un layout XML en l'attachant à un parent optionnel.
     *
     * @param xml    le XML source
     * @param parent le parent auquel attacher la vue racine, ou {@code null}
     * @return la vue racine
     * @throws InflateException si l'inflation échoue
     */
    public View inflate(String xml, ViewGroup parent) {
        if (xml == null || xml.trim().isEmpty()) {
            throw new InflateException("Le XML source est vide ou null");
        }

        String normalized = XmlPreprocessor.preprocess(xml);
        XmlPreprocessor.validateWellFormed(normalized);

        try {
            XmlPullParser parser = XmlPreprocessor.createParser(normalized);
            View racine = parseRoot(parser, parent);
            // v3.7.0 — S8 (étude PE14 § 2.4-5) : qualificateur de DIRECTION
            // sur la racine puis RÉSOLUTION des marges relatives de tout
            // l'arbre (la passe que l'attachement d'une fenêtre ferait —
            // le rendu hors fenêtre doit la faire lui-même).
            DirectionRendu.appliquer(racine, layoutDirectionRendu);
            return racine;
        } catch (XmlPullParserException | IOException e) {
            throw new InflateException(
                    "Erreur pendant l'inflation : " + e.getMessage(), e);
        }
    }

    /**
     * Le QUALIFICATEUR de direction de rendu (v3.7.0 — S8, miroir du
     * {@code LayoutDirectionQualifier} de la référence).
     *
     * @param direction {@link View#LAYOUT_DIRECTION_LTR},
     *                   {@link View#LAYOUT_DIRECTION_RTL} ou
     *                   {@link DirectionRendu#INDEFINI} (ne rien poser — le
     *                   framework suit la locale du contexte)
     */
    public void setLayoutDirectionRendu(int direction) {
        this.layoutDirectionRendu = direction;
    }

    /**
     * @return le qualificateur de direction courant
     *         ({@link DirectionRendu#INDEFINI} par défaut)
     */
    public int getLayoutDirectionRendu() {
        return layoutDirectionRendu;
    }

    /**
     * Parse la racine du document XML.
     *
     * <p>Cette méthode avance le parser jusqu'au premier tag de début, puis
     * délègue à {@link #parseElement(XmlPullParser, ViewGroup)}.</p>
     *
     * @param parser le parser positionné au début du document
     * @param parent le parent optionnel
     * @return la vue racine
     * @throws XmlPullParserException en cas d'erreur de parsing
     * @throws IOException            en cas d'erreur d'E/S
     */
    private View parseRoot(XmlPullParser parser, ViewGroup parent)
            throws XmlPullParserException, IOException {
        int event = parser.getEventType();
        while (event != XmlPullParser.START_TAG && event != XmlPullParser.END_DOCUMENT) {
            event = parser.next();
        }
        if (event == XmlPullParser.END_DOCUMENT) {
            throw new InflateException("Document XML vide (aucun tag de début)");
        }
        return parseElement(parser, parent);
    }

    /**
     * Parse un élément XML et renvoie la vue correspondante.
     *
     * <p>Méthode récursive qui gère les tags spéciaux et les tags de vue
     * standards. Pour les tags {@code <include>} et {@code <merge>}, elle
     * délègue aux méthodes dédiées.</p>
     *
     * <p>v3.7.0 — VIEWCOOKIE (étude PE14 § 2.4-4, S8) : chaque appel
     * consomme UN ORDINAL (le rang du START_TAG dans la marche du
     * document) et toute vue PRODUITE reçoit
     * {@link #poserCookie(View, int, String)} — l'identité du nœud XML
     * source, miroir du cookie {@code ViewInfo} de la référence. La vue
     * gonflée d'un {@code <ViewStub>} porte l'ordinal du TAG stub, ses
     * descendants vivent dans un SITE BRANCHÉ (pas de collision avec les
     * frères qui suivent).</p>
     *
     * @param parser le parser positionné sur un START_TAG
     * @param parent le parent (peut être {@code null})
     * @return la vue créée, ou {@code null} pour {@code <merge>}
     */
    private View parseElement(XmlPullParser parser, ViewGroup parent)
            throws XmlPullParserException, IOException {
        int ordinal = ordinalNoeud++;
        String tag = parser.getName();
        View vue = parseElementInterne(parser, parent, ordinal);
        // v3.7.0 — VIEWCOOKIE : la vue créée PAR CET ÉLÉMENT porte son
        // ordinal — SAUF les retours « EMPRUNTÉS » (<include> et <layout>
        // data-binding renvoient la vue d'un AUTRE nœud, déjà cookieée
        // par sa propre marche). Le <ViewStub> pose DEUX fois :
        // « site:ordinal » à l'intérieur, l'ordinal du TAG stub EN DERNIER
        // (sélectionner le tag stub sélectionne la racine gonflée).
        if (vue != null && !"include".equals(tag) && !"layout".equals(tag)) {
            poserCookie(vue, ordinal, siteCookies);
        }
        return vue;
    }

    /**
     * Le corps historique de {@link #parseElement} — SANS le compteur de
     * cookie (v3.7.0 : l'ordinal appartient à l'ÉLÉMENT, pas au corps).
     *
     * @param parser le parser positionné sur un START_TAG
     * @param parent le parent (peut être {@code null})
     * @param ordinal l'ordinal du START_TAG (site branché des tags qui
     *                gonflent un AUTRE document : ViewStub, data-binding)
     * @return la vue créée, ou {@code null}
     */
    private View parseElementInterne(XmlPullParser parser, ViewGroup parent, int ordinal)
            throws XmlPullParserException, IOException {
        String tag = parser.getName();

        if ("include".equals(tag)) {
            return parseInclude(parser, parent, ordinal);
        }
        if ("merge".equals(tag)) {
            parseMerge(parser, parent);
            return null;
        }
        if ("requestFocus".equals(tag)) {
            // Tag vide, on passe au END_TAG
            parser.nextTag();
            return null;
        }
        if ("blink".equals(tag)) {
            // Tag spécial rare, on l'ignore
            skipElement(parser);
            return null;
        }
        // v3.4.0 — G8 : <fragment> n'est pas rendable en preview (les
        // fragments nécessitent un FragmentManager et un cycle de vie
        // d'activité). On l'affiche comme un MockView (v3.4.8 : cadre de
        // remplacement pointillé, hachures et nom de classe) « Fragment
        // (not rendered) » au lieu d'aborter le rendu. Les fragments
        // modernes utilisent <androidx.fragment.app.FragmentContainerView>
        // (qui EST résolvable comme classe), donc ce case ne concerne
        // que le legacy <fragment class="...">.
        if ("fragment".equals(tag)) {
            return parseFragmentPlaceholder(parser, parent);
        }
        // v3.4.0 — G14 : <layout> est le tag racine du data-binding.
        // Il est transparent : on skip la section <data> et on rend
        // l'unique enfant (le vrai contenu). Le codegen data-binding
        // produit un layout plat, mais l'utilisateur peut vouloir
        // preview le layout source.
        if ("layout".equals(tag)) {
            return parseDataBindingLayout(parser, parent);
        }
        // v3.4.0 — G9 : <view class="com.example.Foo"/> est une syntaxe
        // alternative pour les noms de classe pleinement qualifiés
        // (utile pour les classes internes : <view class="Foo$Bar"/>).
        // On lit l'attribut class et on délègue à createViewByName.
        if ("view".equals(tag)) {
            String className = getAttributeValue(parser, null, "class");
            if (className == null) {
                className = getAttributeValue(parser, "android", "class");
            }
            if (className != null) {
                return parseViewTagWithClass(parser, parent, className);
            }
            // <view> sans class= — fallback vers MockView.
            return parseFragmentPlaceholder(parser, parent);
        }
        // v3.4.0 — G13 : <ViewStub> est une vue qui ne se matérialise
        // qu'au runtime (setVisibility(VISIBLE) ou inflate()). En preview,
        // on inflate EAGERMENT le layout référencé par android:layout
        // pour que l'utilisateur voie ce qui apparaîtra — Android Studio
        // fait de même.
        if ("ViewStub".equals(tag)) {
            return parseViewStubEager(parser, parent, ordinal);
        }

        // Cas standard : création d'une vue
        // IMPORTANT : utiliser (Context) seul — pas d'AttributeSet
        // car XmlPullAttributes ne peut pas être casté en XmlBlock.Parser sur Android 14+
        // Les attributs sont appliqués manuellement via AttributeApplier
        // ADR 0116 — P6/L10 : la Factory2 (hook AOSP) est consultée en premier
        // v3.4.0 — G15 : try/catch sur createView, dégradation MockView
        // (v3.4.8 : cadre de remplacement miroir maquette)
        // au lieu d'abandonner tout le rendu (une vue custom qui lève ne
        // doit JAMAIS tuer la preview entière).
        View view = createViewSafely(tag, parent);

        // Mode strict + registre d'attributs (module-attributes) :
        // tout attribut android:/app: inconnu du registre lève avant application
        checkKnownAttributes(parser);

        // Appliquer tous les attributs XML via les setters natifs
        if (attributeApplier != null) {
            attributeApplier.applyAttributes(view, parser);
        } else {
            applyBaseAttributes(view, parser);
        }

        // ADR 0116 — P3/L8 : application des attributs app:* du namespace
        // res-auto via le registre d'attributs (attributs Material/AndroidX
        // connus + repli réflexion pour les vues custom). Les app:layout_constraint*
        // restent au solver Cassowary (P2/L9).
        if (attributeRegistry != null) {
            attributeRegistry.applyCustomAttributes(view, parser, context);
        }

        // ADR 0116 — P6/L11 : android:animateLayoutChanges="true" installe un
        // LayoutTransition (no-op visuel en preview statique, mais l'attribut
        // est consommé fidèlement — jamais d'exception)
        applyLayoutTransitionIfRequested(view, parser);

        // v3.6.0 — étude PE14 § 2.4-3 (S6) : PREVIEW DE LISTES — le
        // parser est ENCORE sur START_TAG, les tools:listitem/itemCount/
        // listheader/listfooter survivent au XmlPreprocessor et se lisent
        // ici (l'adapter d'aperçu remplit ListView/GridView/Spinner ;
        // RecyclerView reçoit en plus son LinearLayoutManager par défaut).
        // Le retour (l'adapter posé) est ignoré du pipeline — il sert aux
        // tests JVM (l'interception des vues android.jar n'y est pas fiable).
        brancherApercuListe(view, parser);

        // Enregistrement dans la map des ids si la vue a un id
        if (view.getId() != View.NO_ID) {
            idToView.put(view.getId(), view);
        }

        // CRITIQUE : lire les LayoutParams MAINTENANT, pendant que le parser
        // est encore sur START_TAG. Création manuelle (pas de generateLayoutParams)
        ViewGroup.LayoutParams lp;
        if (parent != null) {
            lp = produceLayoutParams(parent, parser);
        } else {
            // v3.4.6 — Racine SANS parent : honorer les dimensions DÉCLARÉES
            // sur le tag racine (match_parent / wrap_content / taille fixe),
            // au lieu d'un WRAP_CONTENT en dur. Dès que la racine est
            // ré-attachée à un conteneur hôte (chrome de preview, embedding),
            // ses LayoutParams deviennent effectifs : un WRAP_CONTENT factice
            // la laissait tassée en haut à gauche au lieu de remplir l'espace
            // disponible. Mêmes règles de dimension et même défaut que
            // produceLayoutParams (wrap_content si absent).
            lp = new ViewGroup.LayoutParams(
                    parseLayoutDimension(dimensionDeclareeOuWrap(parser, "layout_width")),
                    parseLayoutDimension(dimensionDeclareeOuWrap(parser, "layout_height")));
        }
        view.setLayoutParams(lp);

        // Capture des contraintes ConstraintLayout MAINTENANT — le parser
        // est encore sur START_TAG (les attributs ne sont plus accessibles
        // après parseChildren). Toute vue portant un attribut
        // layout_constraint* / barrierDirection est enregistrée, avec son
        // id et sa orientation déclarée (Guidelines/Barriers).
        captureConstraintAttributes(view, parser);

        // Traitement récursif des enfants si la vue est un ViewGroup
        if (view instanceof ViewGroup) {
            ViewGroup viewGroup = (ViewGroup) view;
            parseChildren(parser, viewGroup);
        } else {
            skipChildren(parser);
        }

        // Attachement au parent
        if (parent != null) {
            attacherSansAbandonner(parent, view, lp, tag);
        }

        return view;
    }

    /**
     * Attache la vue à son parent SANS faire échouer le rendu ENTIER quand
     * le parent REFUSE l'ajout (v3.5.1 — étude PE14 § 3.3-C, S4 du plan) :
     * {@code AdapterView.addView} lève {@code UnsupportedOperationException}
     * pour TOUT enfant — la référence ISOLE la vue en échec
     * ({@code Bridge.createMockView}) et rend LE RESTE ; ici le nœud fautif
     * reste DÉTACHÉ (un AdapterView n'accepte AUCUN enfant) et JOURNALISÉ —
     * l'éditeur signalant déjà le document par le problème
     * ENFANTS_ADAPTATEUR côté CodeIDE.
     *
     * @param parent le parent visé
     * @param view   la vue à attacher
     * @param lp     les LayoutParams à poser
     * @param nom    le nom de la balise, pour le journal
     */
    private void attacherSansAbandonner(ViewGroup parent, View view,
                                         ViewGroup.LayoutParams lp, String nom) {
        try {
            parent.addView(view, lp);
        } catch (RuntimeException e) {
            Log.w(TAG, "le parent refuse <" + nom + "> (AdapterView ?) : " + e.getMessage());
        }
    }

    // ====================================================================
    // v3.6.0 — Preview de listes (étude PE14 § 2.4-3, S6 du plan)
    // ====================================================================

    /**
     * Branche le CONTENU D'APERÇU des vues à adaptateur (v3.6.0 — étude
     * PE14 § 2.4-3, S6 du plan, miroir {@code AdapterBinding}/
     * {@code setupViewInContext} de la référence).
     *
     * <p>Lecture des attributs {@code tools:listitem},
     * {@code tools:itemCount}, {@code tools:listheader},
     * {@code tools:listfooter} (le XmlPreprocessor les ÉPARGNE depuis
     * v3.6.0) :</p>
     * <ul>
     *   <li>{@code android:entries} GAGNE — si un vrai adapter est déjà
     *       posé (attribut appliqué juste avant par l'applier), rien ne
     *       bouge (précédence d'Android Studio) ;</li>
     *   <li>{@code tools:listitem} résolu → chaque rangée gonfle LE
     *       layout référencé (inflater enfant — résolveurs, registre et
     *       thème projet propagés) ;</li>
     *   <li>sans {@code tools:listitem} → items TEXTE par défaut
     *       « Item N » (Android Studio remplit de même une liste nue —
     *       {@code AdapterHelper.NOMBRE_DEFAUT} borné par les rangées
     *       VISIBLES, la vraie AdapterView ne demande qu'elles) ;</li>
     *   <li>{@code ListView} : entête/pied {@code tools:listheader}/
     *       {@code tools:listfooter} AVANT l'adapter (exigence du
     *       framework) ;</li>
     *   <li>{@code RecyclerView} : {@code LinearLayoutManager} vertical
     *       par défaut (orientation {@code android:orientation}/
     *       {@code app:orientation} honorée) — une RecyclerView SANS
     *       LayoutManager est vide, le défaut rend la tuile vivante
     *       (comportement Android Studio) ;</li>
     *   <li>{@code ExpandableListView} ÉPUISÉ : son setAdapter jette
     *       (ExpandableListConnector exigé) — la tuile reste honnêtement
     *       vide plutôt que de faire échouer le rendu.</li>
     * </ul>
     *
     * <p>Toute défaillance est JOURNALISÉE sans tuer le rendu (miroir de
     * {@link #attacherSansAbandonner}). Visibilité package : testable
     * unitairement — RETOURNE l'adapter posé ({@link FakeAdapter} ou
     * {@link FakeRecyclerAdapter}), ou {@code null} (rien posé) : les
     * tests JVM l'assertent directement, l'interception Mockito des vues
     * android.jar n'étant pas fiable depuis le bytecode de production.</p>
     *
     * @param view   la vue candidate (AdapterView ou RecyclerView)
     * @param parser le parser ENCORE sur son START_TAG
     * @return l'adapter d'aperçu posé, ou {@code null}
     */
    Object brancherApercuListe(View view, XmlPullParser parser) {
        if (view instanceof ExpandableListView) {
            return null;
        }
        if (view instanceof AdapterView<?>) {
            return brancherAdapterView((AdapterView<?>) view, parser);
        }
        if (view instanceof RecyclerView) {
            return brancherRecyclerView((RecyclerView) view, parser);
        }
        return null;
    }

    /**
     * Branche un {@link android.widget.AdapterView} (S6).
     *
     * @return le {@link FakeAdapter} posé, ou {@code null} (adapter déjà
     *         présent — {@code android:entries} — ou défaillance journalisée)
     */
    private FakeAdapter brancherAdapterView(AdapterView<?> adaptateur,
                                             XmlPullParser parser) {
        try {
            if (adaptateur.getAdapter() != null) {
                return null;
            }
            if (adaptateur instanceof ListView) {
                attacherEntetesEtPieds((ListView) adaptateur, parser);
            }
            FakeAdapter adapteurFake =
                    new FakeAdapter(lireNombreItems(parser), creerFournisseur(parser));
            poserFakeAdapter(adaptateur, adapteurFake);
            return adapteurFake;
        } catch (RuntimeException e) {
            Log.w(TAG, "aperçu AdapterView non posé : " + e.getMessage());
            return null;
        }
    }

    /**
     * Branche une {@link RecyclerView} (S6) — LM vertical par défaut.
     *
     * @return le {@link FakeRecyclerAdapter} posé, ou {@code null}
     */
    private FakeRecyclerAdapter brancherRecyclerView(RecyclerView liste,
                                                       XmlPullParser parser) {
        try {
            if (liste.getLayoutManager() == null) {
                liste.setLayoutManager(new LinearLayoutManager(
                        contexteRendu(), lireOrientationListe(parser), false));
            }
            if (liste.getAdapter() != null) {
                return null;
            }
            FakeRecyclerAdapter adapteur = new FakeRecyclerAdapter(
                    lireNombreItems(parser), creerFournisseur(parser));
            liste.setAdapter(adapteur);
            return adapteur;
        } catch (RuntimeException e) {
            Log.w(TAG, "aperçu RecyclerView non posé : " + e.getMessage());
            return null;
        }
    }

    /**
     * Pose le FakeAdapter sur N'IMPORTE QUEL AdapterView (ListView,
     * GridView, Spinner…) : la signature effacée accepte tout
     * {@code ListAdapter} pour chaque sous-classe (Spinner exige un
     * SpinnerAdapter — {@code BaseAdapter} implémente les deux, le
     * FrameworkAdapter brut fait foi).
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    private void poserFakeAdapter(AdapterView brut, FakeAdapter adapter) {
        brut.setAdapter(adapter);
    }

    /**
     * {@code tools:itemCount} bornant les rangées (débit généreux sinon :
     * la vraie AdapterView ne demande que les rangées visibles).
     */
    private int lireNombreItems(XmlPullParser parser) {
        String brut = getAttributeValue(parser, "tools", "itemCount");
        if (brut == null) {
            return AdapterHelper.NOMBRE_DEFAUT;
        }
        try {
            return Math.max(Integer.parseInt(brut.trim()), 0);
        } catch (NumberFormatException e) {
            Log.w(TAG, "tools:itemCount non numérique (« " + brut + " ») — défaut");
            return AdapterHelper.NOMBRE_DEFAUT;
        }
    }

    /** L'orientation de la RecyclerView : horizontal si déclarée. */
    private int lireOrientationListe(XmlPullParser parser) {
        String orientation = getAttributeValue(parser, "android", "orientation");
        if (orientation == null) {
            orientation = getAttributeValue(parser, "app", "orientation");
        }
        return "horizontal".equals(orientation)
                ? LinearLayoutManager.HORIZONTAL : LinearLayoutManager.VERTICAL;
    }

    /**
     * La fabrique de rangées : layout {@code tools:listitem} gonflé par un
     * inflater ENFANT (résolveurs, registre d'attributs, Factory2 et
     * THÈME PROJET propagés — chaque rangée est rendue comme un document) ;
     * items texte « Item N » par défaut.
     */
    private FakeAdapter.FournisseurVueItem creerFournisseur(XmlPullParser parser) {
        String reference = getAttributeValue(parser, "tools", "listitem");
        String xmlItem = reference != null ? resoudreLayoutPourListe(reference) : null;
        if (xmlItem == null) {
            if (reference != null) {
                Log.w(TAG, "tools:listitem non résolu (" + reference
                        + ") — items texte par défaut");
            }
            return AdapterHelper.fournisseurTexteParDefaut(contexteRendu());
        }
        BridgeInflater inflaterItems = creerInflaterEnfant();
        return item -> {
            try {
                return inflaterItems.inflate(xmlItem, null);
            } catch (RuntimeException e) {
                Log.w(TAG, "rangée d'aperçu non gonflée : " + e.getMessage());
                return AdapterHelper.fournisseurTexteParDefaut(contexteRendu())
                        .creerVueItem(item);
            }
        };
    }

    /** Entête/pied de ListView — AVANT l'adapter (exigence framework). */
    private void attacherEntetesEtPieds(ListView liste, XmlPullParser parser) {
        View entete = gonflerAnnexeListe(getAttributeValue(parser, "tools", "listheader"));
        if (entete != null) {
            liste.addHeaderView(entete, null, false);
        }
        View pied = gonflerAnnexeListe(getAttributeValue(parser, "tools", "listfooter"));
        if (pied != null) {
            liste.addFooterView(pied, null, false);
        }
    }

    /** Gonfle l'entête/pied référencé, {@code null} si introuvable. */
    private View gonflerAnnexeListe(String reference) {
        if (reference == null) {
            return null;
        }
        String xml = resoudreLayoutPourListe(reference);
        if (xml == null) {
            Log.w(TAG, "tools:listheader/listfooter non résolu : " + reference);
            return null;
        }
        try {
            return creerInflaterEnfant().inflate(xml, null);
        } catch (RuntimeException e) {
            Log.w(TAG, "entête/pied d'aperçu non gonflé : " + e.getMessage());
            return null;
        }
    }

    /** Résolution miroir de {@code <include>} : résolveur puis natif. */
    private String resoudreLayoutPourListe(String reference) {
        String xml = resolveLayoutReference(reference);
        if (xml == null) {
            xml = resolveLayoutNatively(reference);
        }
        return xml;
    }

    /**
     * Parse les enfants d'un ViewGroup.
     *
     * @param parser    le parser positionné juste après le START_TAG du parent
     * @param viewGroup le ViewGroup parent
     */
    private void parseChildren(XmlPullParser parser, ViewGroup viewGroup)
            throws XmlPullParserException, IOException {
        int event = parser.next();
        while (event != XmlPullParser.END_TAG) {
            if (event == XmlPullParser.START_TAG) {
                parseElement(parser, viewGroup);
            }
            event = parser.next();
        }
    }

    /**
     * Crée une vue pour un tag standard : Factory2 d'abord (ADR 0116 —
     * P6/L10), création par défaut ensuite.
     *
     * <p>Sémantique AOSP {@code LayoutInflater.createViewFromTag()} : la
     * factory reçoit le parent, le nom du tag, le contexte et un
     * {@code AttributeSet} null (les attributs sont appliqués après coup par
     * l'{@link AttributeApplier}, cf. javadoc de
     * {@link #setFactory2(LayoutInflater.Factory2)}). Un retour null laisse
     * la création par défaut ; une factory qui lève interrompt l'inflation.</p>
     *
     * @param tag    le nom du tag XML
     * @param parent le parent (peut être {@code null} pour la racine)
     * @return la vue créée
     * @throws InflateException si aucune création ne réussit, ou si la
     *                          factory lève
     */
    private View onCreateViewFromFactory(String tag, ViewGroup parent) {
        // v3.6.0 — instanciation sous le THÈME PROJET (S7) : les styles
        // par défaut de chaque famille (buttonStyle, editTextStyle…)
        // se résolvent contre le thème prévisualisé.
        return onCreateViewFromFactory(tag, parent, contexteRendu());
    }

    /**
     * Variante du contexte EXPLICITE (v3.4.9 — repli thème Material) :
     * le repli ré-instancie la vue sous un {@link ContextThemeWrapper}
     * Material3 sans toucher au contexte nominal de l'inflater.
     *
     * @param tag    le nom du tag XML
     * @param parent le parent (peut être {@code null} pour la racine)
     * @param ctx    le contexte d'INSTANCIATION (le classloader reste
     *               celui du contexte nominal — {@link ViewFactory} charge
     *               les classes depuis SON contexte, instancie depuis celui-ci)
     * @return la vue créée
     * @throws InflateException si aucune création ne réussit, ou si la
     *                          factory lève
     */
    private View onCreateViewFromFactory(String tag, ViewGroup parent, Context ctx) {
        if (factory2 == null) {
            return viewFactory.createView(tag, ctx);
        }
        View view;
        try {
            // v3.4.1 — G1 : passer un AttributeSet non-null au lieu de null.
            // L'interface AOSP Factory2 déclare attrs comme non-null — passer
            // null provoque une NPE côté Kotlin (paramètre non-nullable).
            // On utilise un NopAttributeSet (vide) — les attributs réels sont
            // appliqués APRÈS par AttributeApplier (pattern LayoutLib).
            AttributeSet attrs = new NopAttributeSet();
            view = factory2.onCreateView(parent, tag, ctx, attrs);
        } catch (RuntimeException e) {
            throw new InflateException(
                    "La Factory2 a échoué pour <" + tag + "> : " + e.getMessage(), e);
        }
        if (view != null) {
            factoryViewCount++;
            return view;
        }
        // Retour null : création par défaut (sémantique AOSP)
        return viewFactory.createView(tag, ctx);
    }

    /**
     * Crée une vue avec dégradation gracieuse — v3.4.0 / G15, étendu v3.4.9.
     *
     * <p>Si {@link #onCreateViewFromFactory} lève (ClassNotFoundException,
     * NoSuchMethodException, InvocationTargetException, theme-enforcement
     * IllegalArgumentException, etc.), on tente D'ABORD le repli thème
     * Material (v3.4.9 — {@link #retenterAvecThemeMaterial}) : une vue
     * {@code com.google.android.material.*} qui lève le theme-enforcement
     * (« The theme on this view is not a MaterialComponents theme ») est
     * ré-instanciée sous un contexte Material3 — la VRAIE vue remplace le
     * MockView. Sinon, on crée un {@link MockView} à la place. Le rendu
     * continue — l'utilisateur voit un rectangle « placeholder » là où la
     * vue custom aurait dû être, au lieu d'une page blanche avec un message
     * d'erreur.</p>
     *
     * <p>Le {@code MockView} porte le nom de la classe originale (via
     * {@link MockView#setOriginalClassName}) pour le diagnostic : la vue UI
     * peut l'afficher dans un tooltip « Classe introuvable :
     * com.example.FooView ».</p>
     *
     * <p><strong>Note</strong> : les attributs ne sont PAS appliqués sur le
     * MockView (la classe originale n'existe pas, les attributs n'ont pas
     * de sens). Les enfants XML sont skippés (la vue est une feuille).</p>
     *
     * @param tag    le nom du tag XML
     * @param parent le parent (peut être {@code null})
     * @return la vue créée (réelle, repli Material ou MockView)
     */
    private View createViewSafely(String tag, ViewGroup parent) {
        try {
            return onCreateViewFromFactory(tag, parent);
        } catch (InflateException e) {
            // v3.4.9 — repli thème Material AVANT le MockView : la classe
            // existe (chip, slider, textfield…) mais le thème de l'hôte
            // n'est pas Material. Retentée sous Material3, la vue réelle
            // rend — c'est le « moins de MockView » du retour propriétaire.
            View vue = retenterAvecThemeMaterial(tag, parent, e);
            if (vue != null) {
                return vue;
            }
            // ClassNotFoundException, NoSuchMethodException, etc. — la classe
            // n'est pas sur le classpath de l'IDE (vue custom projet/library).
            // Dérade vers MockView + journalise pour le diagnostic.
            logViewCreationFailure(tag, e);
            return newMockView(tag);
        } catch (RuntimeException e) {
            // v3.4.9 — même repli pour le theme-enforcement nu (lancé hors
            // InflateException par certains constructeurs Material).
            View vue = retenterAvecThemeMaterial(tag, parent, e);
            if (vue != null) {
                return vue;
            }
            // InvocationTargetException (constructeur qui lève), theme-
            // enforcement IllegalArgumentException, etc. — la classe existe
            // mais son instanciation échoue. Idem : MockView + journalise.
            logViewCreationFailure(tag, e);
            return newMockView(tag);
        }
    }

    /**
     * Le REPLI THÈME MATERIAL (v3.4.9) — moins de MockView.
     *
     * <p>Les vues {@code com.google.android.material.*} vérifient le thème
     * du contexte à la construction ({@code ThemeEnforcement} → « The
     * theme on this view is not a MaterialComponents theme »). Une app
     * hôte au thème AppCompat (l'app de démonstration, un IDE, un outil
     * tiers) voient donc TOUTES ses vues Material partir en MockView alors
     * que les classes existent. Le repli ré-instancie la vue sous un
     * {@link ContextThemeWrapper} Material3 : la vue est RÉELLE (styles
     * Material par défaut, pas ceux du projet — honnête, mais bien plus
     * fidèle qu'un cadre hachuré) et le rendu continue.</p>
     *
     * <p>La décision est testée JVM : classe Material + cause racine
     * {@link IllegalArgumentException} (la signature du theme-enforcement).
     * Toute autre cause (NPE, Resources.NotFound…) retourne {@code null}
     * → MockView, comme avant.</p>
     *
     * @param tag    le nom du tag XML
     * @param parent le parent (peut être {@code null})
     * @param cause  l'échec de la première tentative
     * @return la vue créée sous thème Material3, ou {@code null} (le repli
     *         ne s'applique pas ou échoue à son tour)
     */
    private View retenterAvecThemeMaterial(String tag, ViewGroup parent, RuntimeException cause) {
        String className = tagRegistry.resolveClassName(tag);
        if (!repliThemeMaterialApplicable(className, cause)) {
            return null;
        }
        try {
            View vue = onCreateViewFromFactory(tag, parent, contexteMaterial());
            System.err.println("[BridgeInflater] repli thème Material3 pour <" + tag
                    + "> : le thème hôte n'est pas Material — vraie vue au lieu du MockView");
            return vue;
        } catch (RuntimeException retry) {
            // Le repli échoue aussi : MockView (chemin inchangé).
            return null;
        }
    }

    /**
     * La décision du repli thème Material — PURE (testée JVM) : la classe
     * résolue vit dans {@code com.google.android.material.*} ET la cause
     * racine est une {@link IllegalArgumentException} (theme-enforcement).
     * Toute autre combinaison reste sur le MockView.
     *
     * @param className la classe résolue du tag (peut être {@code null} — tag spécial)
     * @param cause     l'échec de la première instanciation
     * @return {@code true} si le repli s'applique
     */
    static boolean repliThemeMaterialApplicable(String className, Throwable cause) {
        if (className == null || !className.startsWith("com.google.android.material.")) {
            return false;
        }
        Throwable racine = cause;
        while (racine != null) {
            if (racine instanceof IllegalArgumentException) {
                return true;
            }
            racine = racine.getCause();
        }
        return false;
    }

    /** Cache du contexte de repli Material3 (v3.4.9). */
    private Context contexteMaterialCache;

    /**
     * Identifiant du THÈME PROJET résolu (v3.6.0 — étude PE14 § 2.4-2, S7) :
     * l'ancêtre RÉSOLVABLE du thème prévisualisé (Material3,
     * MaterialComponents, AppCompat, framework) rendu par
     * {@code ThemeContexteResolver.resIdResolvable}. {@code 0} = thème
     * hôte inchangé (comportement d'avant v3.6.0).
     */
    private int themeResIdProjet = 0;

    /** Cache du contexte de rendu thématé (v3.6.0). */
    private Context contexteRenduCache;

    /**
     * Le contexte de repli Material3 (paresseux, réutilisé) : un
     * {@link ContextThemeWrapper} posé sur le contexte de l'inflater.
     * Les resource IDS restent ceux de l'HÔTE (seul le thème change),
     * le classloader aussi — la vue charge depuis les mêmes classes.
     */
    private Context contexteMaterial() {
        if (contexteMaterialCache == null) {
            contexteMaterialCache = new ContextThemeWrapper(
                    contexteRendu(), com.google.android.material.R.style.Theme_Material3_DayNight_NoActionBar);
        }
        return contexteMaterialCache;
    }

    /**
     * Pose le THÈME PROJET résolu (v3.6.0 — étude PE14 § 2.4-2, S7 du
     * plan).
     *
     * <p>Le résid vient de
     * {@code jo.layoutlib.themes.ThemeContexteResolver.resIdResolvable} —
     * l'ancêtre RESOLVABLE du thème déclaré par le projet (un thème
     * métier {@code Theme.MonApp} n'existe pas dans les resources de
     * l'hôte, son parent Material3/AppCompat si). Toutes les vues sont
     * ensuite INSTANCIÉES sous le contexte enveloppé : leurs constructeurs
     * ({@code new Button(ctx)} → {@code obtainStyledAttributes(null,
     * buttonStyle)}) résolvent les styles PAR DÉFAUT de la famille contre
     * le thème PRÉVISUALISÉ — l'écart de fidélité n° 2 de l'étude
     * (§ 2.4-2) se referme : un projet MaterialComponents voit enfin ses
     * boutons Material.</p>
     *
     * <p>Appelé AVANT tout rendu (configuration de session — miroir de
     * {@code RenderService.setThemeDuProjet} côté hôte). {@code 0}
     * rétablit le thème hôte. Propagé aux inflaters enfants (include,
     * rangées d'items d'aperçu).</p>
     *
     * @param themeResId le résid du thème (peut être {@code 0})
     */
    public void setThemeProjet(int themeResId) {
        if (themeResId == themeResIdProjet) {
            return;
        }
        this.themeResIdProjet = themeResId;
        this.contexteRenduCache = null;
        this.contexteMaterialCache = null;
    }

    /**
     * @return le résid du thème projet posé (v3.6.0), {@code 0} si aucun
     */
    public int getThemeProjet() {
        return themeResIdProjet;
    }

    /**
     * Le contexte d'INSTANCIATION des vues (v3.6.0) : enveloppé du thème
     * projet si posé, contexte nominal sinon.
     *
     * @return le contexte de rendu effectif
     */
    Context contexteRendu() {
        if (themeResIdProjet == 0) {
            return context;
        }
        if (contexteRenduCache == null) {
            contexteRenduCache = new ContextThemeWrapper(context, themeResIdProjet);
        }
        return contexteRenduCache;
    }

    /**
     * Journalise l'échec de création d'une vue — niveau WARNING, pas FINE,
     * car c'est un problème que l'utilisateur devrait voir (le placeholder
     * de remplacement est silencieux dans le rendu, le log porte le
     * diagnostic).
     */
    private void logViewCreationFailure(String tag, Throwable cause) {
        // La cause racine (cause chain) est plus utile que le message top-level
        // (InflateException emballe souvent le vrai message dans "Classe
        // introuvable : X" — la cause contient le détail).
        Throwable root = cause;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        System.err.println("[BridgeInflater] MockView fallback for <" + tag + ">: "
                + root.getClass().getSimpleName() + ": " + root.getMessage());
    }

    /**
     * Crée un {@link MockView} configuré avec le nom du tag original.
     * Depuis v3.4.8 le MockView dessine le cadre de REMPLACEMENT de la
     * maquette CodeIDE (contour pointillé, hachures, nom de classe) —
     * lisible clair et nuit. Sa taille par défaut est {@code wrap_content} pour ne pas
     * casser le layout.
     */
    private View newMockView(String tag) {
        MockView mock = new MockView(context);
        mock.setOriginalClassName(tag);
        // LayoutParams WRAP_CONTENT pour ne pas perturber le layout parent.
        mock.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        return mock;
    }

    /**
     * Consomme {@code android:animateLayoutChanges} (ADR 0116 — P6/L11).
     *
     * <p>Sur un ViewGroup avec {@code "true"}, installe un
     * {@link LayoutTransition} — fidèle au comportement Android
     * ({@code ViewGroup.setLayoutTransition(new LayoutTransition())}) mais
     * sans effet visuel en preview statique. Sur une vue simple ou à
     * {@code false}, l'attribut est simplement consommé. Jamais d'exception
     * (JVM mockable comprise).</p>
     *
     * @param view  la vue en cours de configuration
     * @param parser le parser encore positionné sur son START_TAG
     */
    private void applyLayoutTransitionIfRequested(View view, XmlPullParser parser) {
        String value = parser.getAttributeValue(
                "http://schemas.android.com/apk/res/android", "animateLayoutChanges");
        if (value == null) {
            value = parser.getAttributeValue(null, "animateLayoutChanges");
        }
        if (value == null || !"true".equalsIgnoreCase(value.trim())) {
            return;
        }
        if (!(view instanceof ViewGroup)) {
            return; // attribute consommé mais sans effet sur une vue simple
        }
        try {
            ((ViewGroup) view).setLayoutTransition(new LayoutTransition());
            layoutTransitionCount++;
        } catch (RuntimeException e) {
            // LayoutTransition indisponible (JVM mockable selon la cible) —
            // attribut consommé silencieusement, philosophie placeholder
        }
    }

    /**
     * Parse un tag {@code <include layout="@layout/foo" />}.
     *
     * <p>Le layout référencé doit être résolvable via le
     * {@link ResourceResolver}. Si aucun résolveur n'est défini, une
     * {@link InflateException} est levée.</p>
     *
     * @param parser le parser positionné sur le START_TAG {@code <include>}
     * @param parent le ViewGroup auquel attacher les enfants inclus
     * @return la première vue incluse, ou {@code null} pour un {@code <merge>}
     */
    private View parseInclude(XmlPullParser parser, ViewGroup parent, int ordinal)
            throws XmlPullParserException, IOException {
        if (parent == null) {
            throw new InflateException(
                    "<include> ne peut pas être utilisé sans parent");
        }

        String layoutRef = parser.getAttributeValue(
                "http://schemas.android.com/apk/res/android", "layout");
        if (layoutRef == null) {
            layoutRef = parser.getAttributeValue(null, "layout");
        }
        if (layoutRef == null) {
            throw new InflateException("<include> sans attribut layout");
        }

        // Tenter de résoudre via ResourceResolver (project resources)
        String includedXml = resolveLayoutReference(layoutRef);

        // Si pas trouvé, tenter via Resources natives (R.layout.*)
        if (includedXml == null) {
            includedXml = resolveLayoutNatively(layoutRef);
        }

        if (includedXml == null) {
            throw new InflateException(
                    "Layout inclus introuvable : " + layoutRef);
        }

        // Sauter le END_TAG du <include>
        parser.nextTag();

        // Inflater récursivement le layout inclus
        BridgeInflater childInflater = creerInflaterEnfant();
        // v3.7.0 — VIEWCOOKIE : le document inclus vit dans un SITE
        // BRANCHÉ — la chaîne des ordinaux d'{@code <include>} — les vues
        // de DEUX inclusions du MÊME fichier portent des cookies DISTINCTS
        // (miroir du {@code MergeCookie} de la référence).
        childInflater.setContexteCookie(siteDe(ordinal), registreViewCookies);
        View firstChild = childInflater.inflate(includedXml, parent);
        // Les compteurs d'introspection (P6/L10-L11) remontent au inflater
        // parent — les <include> ne créent pas de trous de comptage
        this.factoryViewCount += childInflater.getFactoryViewCount();
        this.layoutTransitionCount += childInflater.getAppliedLayoutTransitionCount();
        return firstChild;
    }

    /**
     * Un inflater ENFANT de pleins droits (v3.6.0 — factorisation du bloc
     * {@code <include>}, réutilisée par la preview de listes S6) : résolveur
     * de ressources, applier d'attributs, registre {@code app:*}, Factory2
     * et THÈME PROJET (S7) propagés — le contenu inclus/gonflé est rendu
     * comme le document principal.
     *
     * <p>Le mode strict n'est PAS propagé (sémantique historique du
     * {@code <include>} inchangée — les contrôles stricts restent au
     * document appelant).</p>
     *
     * @return l'inflater enfant prêt à gonfler
     */
    private BridgeInflater creerInflaterEnfant() {
        BridgeInflater enfant = new BridgeInflater(context, tagRegistry);
        enfant.setResourceResolver(resourceResolver);
        if (attributeApplier != null) {
            enfant.setAttributeApplier(attributeApplier);
        }
        // ADR 0116 — P3/L8 : le registre d'attributs (app:*) suit les enfants
        if (attributeRegistry != null) {
            enfant.setAttributeRegistry(attributeRegistry);
        }
        // ADR 0116 — P6/L10 : la Factory2 suit les enfants (l'enfant est
        // neuf, pas de conflit au setFactory2)
        if (factory2 != null) {
            enfant.setFactory2(factory2);
        }
        // v3.6.0 — S7 : le thème projet suit les enfants (rangs d'items
        // d'aperçu comprises)
        enfant.setThemeProjet(themeResIdProjet);
        return enfant;
    }

    /**
     * Parse un tag {@code <merge>}.
     *
     * <p>Le tag {@code <merge>} est utilisé pour éviter un niveau d'imbrication
     * inutile quand on inclut un layout qui sera attaché à un parent déjà
     * existant. Tous les enfants du {@code <merge>} sont directement attachés
     * au {@code parent}.</p>
     *
     * @param parser le parser positionné sur le START_TAG {@code <merge>}
     * @param parent le ViewGroup auquel attacher les enfants
     */
    private void parseMerge(XmlPullParser parser, ViewGroup parent)
            throws XmlPullParserException, IOException {
        if (parent == null) {
            throw new InflateException(
                    "<merge> ne peut pas être la racine sans parent");
        }
        parseChildren(parser, parent);
    }

    /**
     * Saute tous les enfants d'un élément jusqu'à son END_TAG.
     *
     * @param parser le parser positionné juste après le START_TAG
     */
    private void skipChildren(XmlPullParser parser)
            throws XmlPullParserException, IOException {
        int depth = 1;
        while (depth > 0) {
            int event = parser.next();
            if (event == XmlPullParser.START_TAG) {
                depth++;
            } else if (event == XmlPullParser.END_TAG) {
                depth--;
            } else if (event == XmlPullParser.END_DOCUMENT) {
                throw new InflateException("Fin de document inattendue");
            }
        }
    }

    /**
     * Saute complètement un élément (START_TAG ... END_TAG).
     *
     * @param parser le parser positionné sur le START_TAG
     */
    private void skipElement(XmlPullParser parser)
            throws XmlPullParserException, IOException {
        int depth = 1;
        while (depth > 0) {
            int event = parser.next();
            if (event == XmlPullParser.START_TAG) {
                depth++;
            } else if (event == XmlPullParser.END_TAG) {
                depth--;
            } else if (event == XmlPullParser.END_DOCUMENT) {
                throw new InflateException("Fin de document inattendue");
            }
        }
    }

    /**
     * v3.4.0 — G9 : Rendu de {@code <view class="com.example.Foo">}.
     *
     * <p>Le tag {@code <view>} est une syntaxe alternative pour les noms
     * pleinement qualifiés. On lit l'attribut {@code class} et on crée
     * la vue par nom de classe, puis on applique les attributs et on parse
     * les enfants comme pour un tag standard.</p>
     *
     * <p>Si la classe ne peut pas être instanciée (ClassNotFoundException),
     * on dégrade vers MockView (G15) — le rendu continue.</p>
     */
    private View parseViewTagWithClass(XmlPullParser parser, ViewGroup parent,
                                        String className)
            throws XmlPullParserException, IOException {
        View view;
        try {
            view = viewFactory.createViewByName(className);
        } catch (InflateException e) {
            logViewCreationFailure(className, e);
            view = newMockView(className);
        } catch (RuntimeException e) {
            logViewCreationFailure(className, e);
            view = newMockView(className);
        }

        // Appliquer les attributs (même chemin que parseElement standard).
        checkKnownAttributes(parser);
        if (attributeApplier != null) {
            attributeApplier.applyAttributes(view, parser);
        } else {
            applyBaseAttributes(view, parser);
        }
        if (attributeRegistry != null) {
            attributeRegistry.applyCustomAttributes(view, parser, context);
        }
        applyLayoutTransitionIfRequested(view, parser);

        if (view.getId() != View.NO_ID) {
            idToView.put(view.getId(), view);
        }

        ViewGroup.LayoutParams lp;
        if (parent != null) {
            lp = produceLayoutParams(parent, parser);
        } else {
            lp = new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
        }
        view.setLayoutParams(lp);
        captureConstraintAttributes(view, parser);

        if (view instanceof ViewGroup) {
            parseChildren(parser, (ViewGroup) view);
        } else {
            skipChildren(parser);
        }

        if (parent != null) {
            attacherSansAbandonner(parent, view, lp, className);
        }
        return view;
    }

    /**
     * v3.4.0 — G13 : Inflation eager de {@code <ViewStub>}.
     *
     * <p>Un {@code ViewStub} est une vue invisible qui ne se matérialise
     * qu'au runtime via {@code setVisibility(VISIBLE)} ou {@code inflate()}.
     * En preview statique, le stub reste à 0×0 — l'utilisateur ne voit
     * pas ce qui apparaîtra. Android Studio inflate EAGERMENT le layout
     * référencé par {@code android:layout} en preview.</p>
     *
     * <p>On lit {@code android:layout}, on résout le layout (via le
     * {@code layoutProvider} si branché), et on l'inflate récursivement.
     * Si la résolution échoue (pas de provider, layout introuvable), on
     * crée un MockView « ViewStub: @layout/... » pour signaler
     * visuellement la présence du stub.</p>
     */
    private View parseViewStubEager(XmlPullParser parser, ViewGroup parent, int ordinal)
            throws XmlPullParserException, IOException {
        String layoutRef = getAttributeValue(parser, "android", "layout");
        if (layoutRef == null) {
            layoutRef = getAttributeValue(parser, null, "layout");
        }

        // Skip les enfants du ViewStub (il n'en a pas normalement).
        skipElement(parser);

        if (layoutRef == null) {
            // ViewStub sans android:layout — placeholder de remplacement.
            View mock = newMockView("ViewStub (no layout)");
            if (parent != null) {
                parent.addView(mock);
            }
            return mock;
        }

        // Tenter de résoudre le layout via le ResourceResolver.
        String layoutName = layoutRef.startsWith("@layout/")
                ? layoutRef.substring("@layout/".length())
                : layoutRef;
        String layoutXml = resolveLayoutReference("@layout/" + layoutName);
        if (layoutXml == null) {
            // Essai sans le préfixe (le resolver peut accepter le nom brut).
            layoutXml = resolveLayoutReference(layoutRef);
        }

        if (layoutXml != null) {
            // Inflation eager du layout référencé — v3.7.0 VIEWCOOKIE : le
            // document du stub vit dans un SITE BRANCHÉ (les ordinaux de
            // ses nœuds ne COLLISIONNENT pas avec ceux des frères qui
            // suivent le stub) ; la vue RACINE du layout garde l'ordinal
            // DU TAG STUB (posée en dernier par le wrapper — sélectionner
            // le tag stub sélectionne la racine gonflée, miroir AS).
            try {
                XmlPullParser stubParser = XmlPreprocessor.createParser(layoutXml);
                stubParser.nextTag();
                String siteSauve = siteCookies;
                int ordinalSauve = ordinalNoeud;
                siteCookies = siteDe(ordinal);
                ordinalNoeud = 0;
                try {
                    return parseElement(stubParser, parent);
                } finally {
                    siteCookies = siteSauve;
                    ordinalNoeud = ordinalSauve;
                }
            } catch (XmlPullParserException | IOException e) {
                // Layout référencé invalide — MockView de remplacement.
                View mock = newMockView("ViewStub: @" + layoutName + " (invalid)");
                if (parent != null) {
                    parent.addView(mock);
                }
                return mock;
            }
        }

        // Layout non résolvable (pas de provider ou layout introuvable).
        View mock = newMockView("ViewStub: @" + layoutName);
        if (parent != null) {
            parent.addView(mock);
        }
        return mock;
    }

    /**
     * v3.4.0 — G8 : Placeholder pour {@code <fragment>}.
     *
     * <p>Les fragments nécessitent un {@code FragmentManager} et un cycle
     * de vie d'activité — ils ne peuvent pas être rendus en preview statique.
     * Au lieu d'aborter le rendu (comportement v3.3.0), on crée un
     * {@link MockView} et on skip les enfants du tag {@code <fragment>}.</p>
     *
     * <p>Les fragments modernes utilisent
     * {@code <androidx.fragment.app.FragmentContainerView>} qui EST résolvable
     * comme classe (donc passe par le chemin standard). Ce case ne concerne
     * que le legacy {@code <fragment class="...">}.</p>
     */
    private View parseFragmentPlaceholder(XmlPullParser parser, ViewGroup parent)
            throws XmlPullParserException, IOException {
        String className = getAttributeValue(parser, "android", "name");
        if (className == null) {
            className = getAttributeValue(parser, null, "class");
        }
        String label = className != null ? "fragment: " + className : "fragment";
        MockView mock = new MockView(context);
        mock.setOriginalClassName(label);
        mock.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        // Skip les enfants (le fragment peut contenir des vues alternatives).
        skipElement(parser);
        if (parent != null) {
            parent.addView(mock);
        }
        return mock;
    }

    /**
     * v3.4.0 — G14 : Rendu transparent du tag racine data-binding
     * {@code <layout>}.
     *
     * <p>Le data-binding enveloppe le layout réel dans un tag
     * {@code <layout>} contenant une section {@code <data>} (variables et
     * imports) suivie du vrai contenu. Le codegen produit un layout plat,
     * mais l'utilisateur peut vouloir preview le layout source.</p>
     *
     * <p>On skip la section {@code <data>} et on rend l'unique enfant
     * (le vrai contenu). Le {@code <layout>} lui-même est transparent —
     * il ne crée pas de vue.</p>
     */
    private View parseDataBindingLayout(XmlPullParser parser, ViewGroup parent)
            throws XmlPullParserException, IOException {
        View content = null;
        int depth = 1;
        while (depth > 0) {
            int event = parser.next();
            if (event == XmlPullParser.START_TAG) {
                String childTag = parser.getName();
                if ("data".equals(childTag)) {
                    // Section <data> (variables/imports) — skip complète.
                    skipElement(parser);
                } else {
                    // Le vrai contenu — rendre via parseElement.
                    if (content == null) {
                        content = parseElement(parser, parent);
                    } else {
                        // Plusieurs enfants après <data> : skip les extras.
                        skipElement(parser);
                    }
                }
            } else if (event == XmlPullParser.END_TAG) {
                depth--;
            } else if (event == XmlPullParser.END_DOCUMENT) {
                throw new InflateException("Fin de document inattendue dans <layout>");
            }
        }
        return content;
    }

    /**
     * Applique les attributs de base (id, visibility, padding, background,
     * layout_*) à la vue créée.
     *
     * <p>Cette méthode ne couvre que les attributs communs à toutes les vues.
     * Les attributs spécifiques (text, src, etc.) sont gérés par les Callers
     * du module-attributes.</p>
     *
     * @param view   la vue à configurer
     * @param parser le parser positionné sur le START_TAG
     */
    private void applyBaseAttributes(View view, XmlPullParser parser) {
        // id
        String id = getAttributeValue(parser, "android", "id");
        if (id != null) {
            int resolvedId = resolveViewId(id);
            view.setId(resolvedId);
        }

        // visibility
        String visibility = getAttributeValue(parser, "android", "visibility");
        if (visibility != null) {
            switch (visibility) {
                case "gone":
                    view.setVisibility(View.GONE);
                    break;
                case "invisible":
                    view.setVisibility(View.INVISIBLE);
                    break;
                case "visible":
                    view.setVisibility(View.VISIBLE);
                    break;
                default:
                    if (strictMode) {
                        throw new InflateException("Valeur de visibility invalide : " + visibility);
                    }
            }
        }

        // padding
        String paddingLeft = getAttributeValue(parser, "android", "paddingLeft");
        String paddingTop = getAttributeValue(parser, "android", "paddingTop");
        String paddingRight = getAttributeValue(parser, "android", "paddingRight");
        String paddingBottom = getAttributeValue(parser, "android", "paddingBottom");
        if (paddingLeft != null || paddingTop != null
                || paddingRight != null || paddingBottom != null) {
            int l = paddingLeft != null ? parseDimension(paddingLeft) : view.getPaddingLeft();
            int t = paddingTop != null ? parseDimension(paddingTop) : view.getPaddingTop();
            int r = paddingRight != null ? parseDimension(paddingRight) : view.getPaddingRight();
            int b = paddingBottom != null ? parseDimension(paddingBottom) : view.getPaddingBottom();
            view.setPadding(l, t, r, b);
        }

        // Tag user (pour debug)
        String tagValue = getAttributeValue(parser, "android", "tag");
        if (tagValue != null) {
            view.setTag(tagValue);
        }
    }

    /**
     * Valeur déclarée d'un attribut de dimension android: (ex.
     * {@code layout_width}), avec repli {@code "wrap_content"} si l'attribut
     * est absent ou vide — même défaut que {@link #produceLayoutParams}.
     *
     * @param parser parser positionné sur le START_TAG de la vue
     * @param name   nom de l'attribut (sans préfixe de namespace)
     * @return la valeur brute de l'attribut, ou {@code "wrap_content"}
     */
    private String dimensionDeclareeOuWrap(XmlPullParser parser, String name) {
        String valeur = getAttributeValue(parser, "android", name);
        if (valeur == null || valeur.isEmpty()) {
            return "wrap_content";
        }
        return valeur;
    }

    /**
     * Produit les LayoutParams en lisant manuellement layout_width/height.
     * Ne JAMAIS appeler generateLayoutParams(null) ni generateLayoutParams(attrs)
     * car cela déclenche obtainStyledAttributes qui crash sur Android 14+.
     */
    private ViewGroup.LayoutParams produceLayoutParams(ViewGroup parent, XmlPullParser parser) {
        String width = getAttributeValue(parser, "android", "layout_width");
        String height = getAttributeValue(parser, "android", "layout_height");
        if (width == null || width.isEmpty()) width = "wrap_content";
        if (height == null || height.isEmpty()) height = "wrap_content";

        int widthVal = parseLayoutDimension(width);
        int heightVal = parseLayoutDimension(height);

        ViewGroup.LayoutParams lp;
        if (parent instanceof android.widget.LinearLayout) {
            android.widget.LinearLayout.LayoutParams llLp =
                    new android.widget.LinearLayout.LayoutParams(widthVal, heightVal);
            String weight = getAttributeValue(parser, "android", "layout_weight");
            if (weight != null) {
                try { llLp.weight = Float.parseFloat(weight); } catch (NumberFormatException e) { Log.w(TAG, "Valeur numérique invalide, ignorée : " + e.getMessage()); }
            }
            String gravity = getAttributeValue(parser, "android", "layout_gravity");
            if (gravity != null) {
                llLp.gravity = parseGravity(gravity);
            }
            lp = llLp;
        } else if (parent instanceof android.widget.FrameLayout) {
            android.widget.FrameLayout.LayoutParams flLp =
                    new android.widget.FrameLayout.LayoutParams(widthVal, heightVal);
            String gravity = getAttributeValue(parser, "android", "layout_gravity");
            if (gravity != null) {
                flLp.gravity = parseGravity(gravity);
            }
            lp = flLp;
        } else if (parent instanceof android.widget.RelativeLayout) {
            android.widget.RelativeLayout.LayoutParams rlLp =
                    new android.widget.RelativeLayout.LayoutParams(widthVal, heightVal);
            // Attributs de positionnement RelativeLayout
            applyRelativeLayoutRules(rlLp, parser, idToView);
            String gravity = getAttributeValue(parser, "android", "layout_gravity");
            // v3.5.1 — S3-étude § 2.3.2 : la valeur de layout_gravity était
            // IGNORÉE (toujours CENTER_IN_PARENT) ; la gravité demandée est
            // appliquée au champ de LayoutParams.
            applyRelativeLayoutGravity(rlLp, gravity);
            lp = rlLp;
        } else if (parent instanceof android.widget.GridLayout) {
            android.widget.GridLayout.LayoutParams glLp =
                    new android.widget.GridLayout.LayoutParams();
            glLp.width = widthVal;
            glLp.height = heightVal;
            applyGridLayoutSpec(glLp, parser);
            lp = glLp;
        } else if (parent instanceof android.widget.TableLayout) {
            android.widget.TableLayout.LayoutParams tlLp =
                    new android.widget.TableLayout.LayoutParams(widthVal, heightVal);
            lp = tlLp;
        } else if (parent instanceof android.widget.TableRow) {
            android.widget.TableRow.LayoutParams trLp =
                    new android.widget.TableRow.LayoutParams(widthVal, heightVal);
            String weight = getAttributeValue(parser, "android", "layout_weight");
            if (weight != null) {
                try { trLp.weight = Float.parseFloat(weight); } catch (NumberFormatException e) { Log.w(TAG, "Valeur numérique invalide, ignorée : " + e.getMessage()); }
            }
            lp = trLp;
        } else if (isConstraintLayout(parent)) {
            // ConstraintLayout — créer ses LayoutParams natifs avec les contraintes
            lp = createConstraintLayoutParams(widthVal, heightVal, parser);
        } else if (isCoordinatorLayout(parent)) {
            // CoordinatorLayout — créer ses LayoutParams avec behavior
            lp = createCoordinatorLayoutParams(widthVal, heightVal, parser);
        } else {
            lp = new ViewGroup.LayoutParams(widthVal, heightVal);
        }

        // Marges
        if (lp instanceof ViewGroup.MarginLayoutParams) {
            ViewGroup.MarginLayoutParams mlp = (ViewGroup.MarginLayoutParams) lp;
            String margin = getAttributeValue(parser, "android", "layout_margin");
            String marginLeft = getAttributeValue(parser, "android", "layout_marginLeft");
            String marginTop = getAttributeValue(parser, "android", "layout_marginTop");
            String marginRight = getAttributeValue(parser, "android", "layout_marginRight");
            String marginBottom = getAttributeValue(parser, "android", "layout_marginBottom");
            String marginStart = getAttributeValue(parser, "android", "layout_marginStart");
            String marginEnd = getAttributeValue(parser, "android", "layout_marginEnd");

            if (margin != null) {
                int m = parseDimension(margin);
                mlp.setMargins(m, m, m, m);
            } else {
                int ml = marginLeft != null ? parseDimension(marginLeft) : mlp.leftMargin;
                int mt = marginTop != null ? parseDimension(marginTop) : mlp.topMargin;
                int mr = marginRight != null ? parseDimension(marginRight) : mlp.rightMargin;
                int mb = marginBottom != null ? parseDimension(marginBottom) : mlp.bottomMargin;
                mlp.setMargins(ml, mt, mr, mb);
            }
            // v3.7.0 — S8 (étude PE14 § 2.4-5) : les marges RELATIVES
            // survivaient pliées en left/right (destruction RTL du
            // préprocesseur, retirée) ; posées APRÈS les absolues via
            // setMarginStart/setMarginEnd, elles l'emportent sur leur
            // côté (miroir de la lecture AOSP) et DirectionRendu les
            // résoudra sur left/right au layout, selon la direction.
            appliquerMargesRelatives(mlp, marginStart, marginEnd);
        }

        return lp;
    }

    /**
     * Pose les marges RELATIVES {@code layout_marginStart/End} sur les
     * LayoutParams (v3.7.0 — seam TESTABLE : l'android.jar mockable rend
     * {@code setMarginStart} invisible aux tests, les verify Mockito du
     * passe-plat attestent l'application, patron « stubs/verify »).
     *
     * @param lp          les LayoutParams à marge
     * @param marginStart la valeur lue (ou null)
     * @param marginEnd   la valeur lue (ou null)
     */
    void appliquerMargesRelatives(ViewGroup.MarginLayoutParams lp,
                                   String marginStart, String marginEnd) {
        if (marginStart != null) {
            lp.setMarginStart(parseDimension(marginStart));
        }
        if (marginEnd != null) {
            lp.setMarginEnd(parseDimension(marginEnd));
        }
    }

    /**
     * Résout une référence {@code @layout/foo} via le ResourceResolver.
     *
     * @param reference la référence (ex. {@code @layout/login_form})
     * @return le contenu XML du layout, ou {@code null} si introuvable
     */
    private String resolveLayoutReference(String reference) {
        if (resourceResolver == null) {
            return null;
        }
        if (reference == null || !reference.startsWith("@layout/")) {
            return null;
        }
        return resourceResolver.getLayout(reference);
    }

    /**
     * Résout une référence @layout/foo via les Resources natives Android.
     *
     * <p>Lit le XML du layout depuis {@code R.layout.*} en utilisant
     * {@code Resources.getXml()} et le convertit en chaîne.</p>
     *
     * @param reference la référence (ex. @layout/included_item)
     * @return le contenu XML, ou null si introuvable
     */
    private String resolveLayoutNatively(String reference) {
        if (reference == null) return null;
        String name;
        String pkg = context.getPackageName();
        if (reference.startsWith("@layout/")) {
            name = reference.substring("@layout/".length());
        } else if (reference.startsWith("@android:layout/")) {
            name = reference.substring("@android:layout/".length());
            pkg = "android";
        } else {
            return null;
        }

        int layoutId = context.getResources().getIdentifier(name, "layout", pkg);
        if (layoutId == 0) return null;

        try {
            android.content.res.XmlResourceParser xmlParser =
                    context.getResources().getXml(layoutId);
            StringBuilder sb = new StringBuilder();
            int event = xmlParser.getEventType();
            while (event != XmlPullParser.END_DOCUMENT) {
                if (event == XmlPullParser.START_TAG) {
                    sb.append("<").append(xmlParser.getName());
                    int attrCount = xmlParser.getAttributeCount();
                    for (int i = 0; i < attrCount; i++) {
                        String attrNs = xmlParser.getAttributeNamespace(i);
                        String attrName = xmlParser.getAttributeName(i);
                        String attrValue = xmlParser.getAttributeValue(i);
                        if (attrNs != null && !attrNs.isEmpty()) {
                            // Mapper les URIs vers les préfixes
                            if (attrNs.equals("http://schemas.android.com/apk/res/android")) {
                                sb.append(" android:").append(attrName);
                            } else if (attrNs.equals("http://schemas.android.com/apk/res-auto")) {
                                sb.append(" app:").append(attrName);
                            } else {
                                sb.append(" ").append(attrName);
                            }
                        } else {
                            sb.append(" ").append(attrName);
                        }
                        sb.append("=\"").append(attrValue != null ? attrValue : "").append("\"");
                    }
                    sb.append(">");
                } else if (event == XmlPullParser.END_TAG) {
                    sb.append("</").append(xmlParser.getName()).append(">");
                } else if (event == XmlPullParser.TEXT) {
                    String text = xmlParser.getText();
                    if (text != null) sb.append(text.trim());
                }
                event = xmlParser.next();
            }
            return sb.toString();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Lit la valeur d'un attribut depuis le parser en gérant le namespace.
     *
     * @param parser le parser
     * @param ns     le préfixe de namespace ({@code android}, {@code app}
     *               ou {@code tools} — v3.6.0)
     * @param name   le nom de l'attribut
     * @return la valeur, ou {@code null} si l'attribut n'est pas présent
     */
    private String getAttributeValue(XmlPullParser parser, String ns, String name) {
        String nsUri;
        if ("android".equals(ns)) {
            nsUri = "http://schemas.android.com/apk/res/android";
        } else if ("app".equals(ns)) {
            nsUri = "http://schemas.android.com/apk/res-auto";
        } else if ("tools".equals(ns)) {
            // v3.6.0 — preview de listes : l'espace tools survit au
            // XmlPreprocessor quand des attributs tools:list* restent.
            nsUri = URI_TOOLS;
        } else {
            nsUri = ns;
        }
        String value = parser.getAttributeValue(nsUri, name);
        if (value == null) {
            // Tentative sans namespace
            value = parser.getAttributeValue(null, name);
        }
        return value;
    }

    /**
     * Résout un id de vue. Supporte {@code @+id/foo} (création), {@code @id/foo}
     * (référence) et les ids littéraux.
     *
     * @param idValue la valeur de l'attribut {@code android:id}
     * @return l'id entier résolu
     */
    private int resolveViewId(String idValue) {
        if (idValue == null) {
            return View.NO_ID;
        }
        if (idValue.startsWith("@+id/") || idValue.startsWith("@id/")) {
            String name = idValue.substring(idValue.indexOf('/') + 1);
            int resolved = context.getResources().getIdentifier(
                    name, "id", context.getPackageName());
            if (resolved != 0) {
                return resolved;
            }
            // v3.5.1 — S3-étude § 2.3.3 : id STABLE par nom (miroir
            // DynamicIdMap de la référence) — le compteur régénérait un id
            // DIFFÉRENT à chaque appel pour le même nom.
            return dynamicIds.getId(name);
        }
        if (idValue.startsWith("@android:id/")) {
            String name = idValue.substring("@android:id/".length());
            return android.R.id.class.getDeclaredFields().length > 0
                    ? context.getResources().getIdentifier(name, "id", "android")
                    : View.NO_ID;
        }
        try {
            return Integer.parseInt(idValue);
        } catch (NumberFormatException e) {
            return View.NO_ID;
        }
    }

    /**
     * Parse une dimension au format Android (ex. {@code 16dp}, {@code 100px},
     * {@code 12sp}) et renvoie la valeur en pixels.
     *
     * @param value la chaîne à parser
     * @return la valeur en pixels
     * @throws InflateException si la valeur est malformée
     */
    /**
     * Applique les règles de positionnement RelativeLayout.
     * Supporte tous les attributs layout_above, layout_below, layout_toLeftOf, etc.
     *
     * @param rlLp  les LayoutParams du RelativeLayout
     * @param parser le parser sur START_TAG
     * @param idMap la map des ids déjà créés
     */
    private void applyRelativeLayoutRules(android.widget.RelativeLayout.LayoutParams rlLp,
                                           XmlPullParser parser,
                                           Map<Integer, View> idMap) {
        // Règles booléennes (pas de référence d'id)
        String layout_centerInParent = getAttributeValue(parser, "android", "layout_centerInParent");
        if ("true".equals(layout_centerInParent)) {
            rlLp.addRule(android.widget.RelativeLayout.CENTER_IN_PARENT);
        }
        String layout_centerHorizontal = getAttributeValue(parser, "android", "layout_centerHorizontal");
        if ("true".equals(layout_centerHorizontal)) {
            rlLp.addRule(android.widget.RelativeLayout.CENTER_HORIZONTAL);
        }
        String layout_centerVertical = getAttributeValue(parser, "android", "layout_centerVertical");
        if ("true".equals(layout_centerVertical)) {
            rlLp.addRule(android.widget.RelativeLayout.CENTER_VERTICAL);
        }
        String layout_alignParentTop = getAttributeValue(parser, "android", "layout_alignParentTop");
        if ("true".equals(layout_alignParentTop)) {
            rlLp.addRule(android.widget.RelativeLayout.ALIGN_PARENT_TOP);
        }
        String layout_alignParentBottom = getAttributeValue(parser, "android", "layout_alignParentBottom");
        if ("true".equals(layout_alignParentBottom)) {
            rlLp.addRule(android.widget.RelativeLayout.ALIGN_PARENT_BOTTOM);
        }
        String layout_alignParentLeft = getAttributeValue(parser, "android", "layout_alignParentLeft");
        if ("true".equals(layout_alignParentLeft)) {
            rlLp.addRule(android.widget.RelativeLayout.ALIGN_PARENT_LEFT);
        }
        String layout_alignParentRight = getAttributeValue(parser, "android", "layout_alignParentRight");
        if ("true".equals(layout_alignParentRight)) {
            rlLp.addRule(android.widget.RelativeLayout.ALIGN_PARENT_RIGHT);
        }
        String layout_alignParentStart = getAttributeValue(parser, "android", "layout_alignParentStart");
        if ("true".equals(layout_alignParentStart)) {
            rlLp.addRule(android.widget.RelativeLayout.ALIGN_PARENT_START);
        }
        String layout_alignParentEnd = getAttributeValue(parser, "android", "layout_alignParentEnd");
        if ("true".equals(layout_alignParentEnd)) {
            rlLp.addRule(android.widget.RelativeLayout.ALIGN_PARENT_END);
        }

        // Règles avec référence d'id
        String layout_above = getAttributeValue(parser, "android", "layout_above");
        if (layout_above != null) {
            int id = resolveIdReference(layout_above);
            if (id != 0) rlLp.addRule(android.widget.RelativeLayout.ABOVE, id);
        }
        String layout_below = getAttributeValue(parser, "android", "layout_below");
        if (layout_below != null) {
            int id = resolveIdReference(layout_below);
            if (id != 0) rlLp.addRule(android.widget.RelativeLayout.BELOW, id);
        }
        String layout_toLeftOf = getAttributeValue(parser, "android", "layout_toLeftOf");
        if (layout_toLeftOf != null) {
            int id = resolveIdReference(layout_toLeftOf);
            if (id != 0) rlLp.addRule(android.widget.RelativeLayout.LEFT_OF, id);
        }
        String layout_toRightOf = getAttributeValue(parser, "android", "layout_toRightOf");
        if (layout_toRightOf != null) {
            int id = resolveIdReference(layout_toRightOf);
            if (id != 0) rlLp.addRule(android.widget.RelativeLayout.RIGHT_OF, id);
        }
        String layout_toStartOf = getAttributeValue(parser, "android", "layout_toStartOf");
        if (layout_toStartOf != null) {
            int id = resolveIdReference(layout_toStartOf);
            if (id != 0) rlLp.addRule(android.widget.RelativeLayout.START_OF, id);
        }
        String layout_toEndOf = getAttributeValue(parser, "android", "layout_toEndOf");
        if (layout_toEndOf != null) {
            int id = resolveIdReference(layout_toEndOf);
            if (id != 0) rlLp.addRule(android.widget.RelativeLayout.END_OF, id);
        }
        String layout_alignTop = getAttributeValue(parser, "android", "layout_alignTop");
        if (layout_alignTop != null) {
            int id = resolveIdReference(layout_alignTop);
            if (id != 0) rlLp.addRule(android.widget.RelativeLayout.ALIGN_TOP, id);
        }
        String layout_alignBottom = getAttributeValue(parser, "android", "layout_alignBottom");
        if (layout_alignBottom != null) {
            int id = resolveIdReference(layout_alignBottom);
            if (id != 0) rlLp.addRule(android.widget.RelativeLayout.ALIGN_BOTTOM, id);
        }
        String layout_alignLeft = getAttributeValue(parser, "android", "layout_alignLeft");
        if (layout_alignLeft != null) {
            int id = resolveIdReference(layout_alignLeft);
            if (id != 0) rlLp.addRule(android.widget.RelativeLayout.ALIGN_LEFT, id);
        }
        String layout_alignRight = getAttributeValue(parser, "android", "layout_alignRight");
        if (layout_alignRight != null) {
            int id = resolveIdReference(layout_alignRight);
            if (id != 0) rlLp.addRule(android.widget.RelativeLayout.ALIGN_RIGHT, id);
        }
        String layout_alignStart = getAttributeValue(parser, "android", "layout_alignStart");
        if (layout_alignStart != null) {
            int id = resolveIdReference(layout_alignStart);
            if (id != 0) rlLp.addRule(android.widget.RelativeLayout.ALIGN_START, id);
        }
        String layout_alignEnd = getAttributeValue(parser, "android", "layout_alignEnd");
        if (layout_alignEnd != null) {
            int id = resolveIdReference(layout_alignEnd);
            if (id != 0) rlLp.addRule(android.widget.RelativeLayout.ALIGN_END, id);
        }
        String layout_alignBaseline = getAttributeValue(parser, "android", "layout_alignBaseline");
        if (layout_alignBaseline != null) {
            int id = resolveIdReference(layout_alignBaseline);
            if (id != 0) rlLp.addRule(android.widget.RelativeLayout.ALIGN_BASELINE, id);
        }
    }

    /**
     * Applique la GRAVITÉ d'un enfant de RelativeLayout à ses LayoutParams
     * (v3.5.1 — S3-étude § 2.3.2 : la valeur lue est honorée, plus de
     * CENTER_IN_PARENT imposé).
     *
     * <p>Portée package : testée en JVM (patron BridgeInflaterDimenReferenceTest).
     * La translation elle-même vit dans {@link #reglesDeGraviteRelative(String)}
     * — PURE, observable sous l'android.jar mockable (addRule y est un
     * no-op : on teste la DONNÉE, pas l'appel).</p>
     *
     * @param rlLp    les LayoutParams de l'enfant
     * @param gravity la valeur de {@code layout_gravity} (peut être null)
     */
    void applyRelativeLayoutGravity(android.widget.RelativeLayout.LayoutParams rlLp,
                                    String gravity) {
        for (int regle : reglesDeGraviteRelative(gravity)) {
            rlLp.addRule(regle);
        }
    }

    /**
     * La GRAVITÉ d'un enfant de RelativeLayout traduite en RÈGLES parent
     * (v3.5.1 — S3-étude § 2.3.2) : en RelativeLayout l'alignement
     * s'exprime par des RÈGLES, pas par un champ — {@code end|bottom} →
     * ALIGN_PARENT_END + ALIGN_PARENT_BOTTOM, {@code center} →
     * CENTER_IN_PARENT. La traduction est PAR JETON (miroir du découpage
     * de {@code parseGravity}) : les BITS de gravité se CHEVAUCHENT
     * (END porte le bit de CENTER_HORIZONTAL, BOTTOM celui de
     * CENTER_VERTICAL — un masque {@code & CENTER} confondrait
     * {@code end|bottom} avec {@code center}) ; {@code fill} et variantes
     * n'ont AUCUNE règle équivalente, null/vide → AUCUNE règle.
     *
     * <p>PURE et STATIQUE : les constantes de règles sont des champs
     * {@code static final} (réels même sous l'android.jar mockable).</p>
     *
     * @param gravity la valeur de {@code layout_gravity} (peut être null)
     * @return les règles parent à poser, dans l'ordre des jetons
     */
    static int[] reglesDeGraviteRelative(String gravity) {
        if (gravity == null) {
            return new int[0];
        }
        java.util.List<Integer> regles = new java.util.ArrayList<>();
        for (String brut : gravity.split("\\|")) {
            switch (brut.trim()) {
                case "center":
                    regles.add(android.widget.RelativeLayout.CENTER_IN_PARENT);
                    break;
                case "center_vertical":
                    regles.add(android.widget.RelativeLayout.CENTER_VERTICAL);
                    break;
                case "center_horizontal":
                    regles.add(android.widget.RelativeLayout.CENTER_HORIZONTAL);
                    break;
                case "start":
                    regles.add(android.widget.RelativeLayout.ALIGN_PARENT_START);
                    break;
                case "end":
                    regles.add(android.widget.RelativeLayout.ALIGN_PARENT_END);
                    break;
                case "left":
                    regles.add(android.widget.RelativeLayout.ALIGN_PARENT_LEFT);
                    break;
                case "right":
                    regles.add(android.widget.RelativeLayout.ALIGN_PARENT_RIGHT);
                    break;
                case "top":
                    regles.add(android.widget.RelativeLayout.ALIGN_PARENT_TOP);
                    break;
                case "bottom":
                    regles.add(android.widget.RelativeLayout.ALIGN_PARENT_BOTTOM);
                    break;
                default:
                    // fill, fill_horizontal, fill_vertical… : aucune règle
                    // équivalente — silencieusement ignorés.
                    break;
            }
        }
        int[] sortie = new int[regles.size()];
        for (int i = 0; i < regles.size(); i++) {
            sortie[i] = regles.get(i);
        }
        return sortie;
    }

    /**
     * Applique les spécifications de GridLayout (row, column, span).
     *
     * @param glLp   les LayoutParams du GridLayout
     * @param parser le parser sur START_TAG
     */
    private void applyGridLayoutSpec(android.widget.GridLayout.LayoutParams glLp,
                                      XmlPullParser parser) {
        String layout_row = getAttributeValue(parser, "android", "layout_row");
        // v3.5.1 — S3-étude § 2.3.1 : layout_column était lu depuis
        // layout_rowSpan (copier-coller) — tout GridLayout portant
        // layout_column était mal placé.
        String layout_column = getAttributeValue(parser, "android", "layout_column");
        String layout_rowSpan = getAttributeValue(parser, "android", "layout_rowSpan");
        String layout_columnSpan = getAttributeValue(parser, "android", "layout_columnSpan");
        String layout_gravity = getAttributeValue(parser, "android", "layout_gravity");

        android.widget.GridLayout.Spec rowSpec = android.widget.GridLayout.spec(
                layout_row != null ? Integer.parseInt(layout_row) : android.widget.GridLayout.UNDEFINED,
                layout_rowSpan != null ? Integer.parseInt(layout_rowSpan) : 1);
        android.widget.GridLayout.Spec colSpec = android.widget.GridLayout.spec(
                layout_column != null ? Integer.parseInt(layout_column) : android.widget.GridLayout.UNDEFINED,
                layout_columnSpan != null ? Integer.parseInt(layout_columnSpan) : 1);

        glLp.rowSpec = rowSpec;
        glLp.columnSpec = colSpec;

        if (layout_gravity != null) {
            glLp.setGravity(parseGravity(layout_gravity));
        }
    }

    /**
     * Indique si un parent est un ConstraintLayout (vérification par nom de classe
     * pour éviter une dépendance directe vers androidx.constraintlayout).
     *
     * @param parent le parent à tester
     * @return true si c'est un ConstraintLayout
     */
    private boolean isConstraintLayout(ViewGroup parent) {
        if (parent == null) return false;
        String className = parent.getClass().getName();
        return className.contains("ConstraintLayout") || className.contains("MotionLayout");
    }

    /**
     * Indique si un parent est un CoordinatorLayout.
     *
     * @param parent le parent à tester
     * @return true si c'est un CoordinatorLayout
     */
    private boolean isCoordinatorLayout(ViewGroup parent) {
        if (parent == null) return false;
        return parent.getClass().getName().contains("CoordinatorLayout");
    }

    /**
     * Crée les LayoutParams pour ConstraintLayout en lisant les attributs
     * app:layout_constraint* nativement.
     *
     * <p>ConstraintLayout a son propre solver Cassowary interne qui lit
     * les contraintes depuis ConstraintLayout.LayoutParams. On crée donc
     * ces LayoutParams avec les bonnes valeurs et le solver natif fait le reste.</p>
     *
     * @param width  largeur
     * @param height hauteur
     * @param parser le parser sur START_TAG
     * @return les LayoutParams
     */
    private ViewGroup.LayoutParams createConstraintLayoutParams(int width, int height,
                                                                 XmlPullParser parser) {
        try {
            // Utiliser la réflexion pour créer ConstraintLayout.LayoutParams
            // sans dépendance directe vers la bibliothèque
            Class<?> clClass = Class.forName("androidx.constraintlayout.widget.ConstraintLayout");
            Class<?> lpClass = Class.forName("androidx.constraintlayout.widget.ConstraintLayout$LayoutParams");

            java.lang.reflect.Constructor<?> ctor = lpClass.getConstructor(int.class, int.class);
            Object lp = ctor.newInstance(width, height);

            // Lire et appliquer les contraintes app:layout_constraint*
            String[] constraintAttrs = {
                    "layout_constraintTop_toTopOf", "layout_constraintTop_toBottomOf",
                    "layout_constraintBottom_toTopOf", "layout_constraintBottom_toBottomOf",
                    "layout_constraintStart_toStartOf", "layout_constraintStart_toEndOf",
                    "layout_constraintEnd_toStartOf", "layout_constraintEnd_toEndOf",
                    "layout_constraintLeft_toLeftOf", "layout_constraintLeft_toRightOf",
                    "layout_constraintRight_toLeftOf", "layout_constraintRight_toRightOf",
                    "layout_constraintBaseline_toBaselineOf",
                    "layout_constraintHorizontal_bias", "layout_constraintVertical_bias",
                    "layout_constraintWidth_percent", "layout_constraintHeight_percent",
                    "layout_editor_absoluteX", "layout_editor_absoluteY",
            };

            for (String attr : constraintAttrs) {
                String value = getAttributeValue(parser, "app", attr);
                if (value == null) continue;

                // Mapper le nom d'attribut vers le setter
                // ConstraintLayout.LayoutParams utilise des champs publics
                String fieldName = convertAttrToFieldName(attr);
                try {
                    java.lang.reflect.Field field = lpClass.getField(fieldName);
                    if (field.getType() == int.class) {
                        // Contrainte vers parent ou @id/ref
                        if ("parent".equals(value)) {
                            field.setInt(lp, 0); // PARENT_ID = 0
                        } else if (value.startsWith("@+id/") || value.startsWith("@id/")) {
                            String idName = value.substring(value.indexOf('/') + 1);
                            int id = context.getResources().getIdentifier(idName, "id", context.getPackageName());
                            field.setInt(lp, id != 0 ? id : View.generateViewId());
                        }
                    } else if (field.getType() == float.class) {
                        try { field.setFloat(lp, Float.parseFloat(value)); } catch (NumberFormatException e) { Log.w(TAG, "Valeur numérique invalide, ignorée : " + e.getMessage()); }
                    } else if (field.getType() == String.class) {
                        field.set(lp, value);
                    }
                } catch (NoSuchFieldException ignored) {
                    // Voulu : le LayoutParams du parent n'expose pas ce champ
                    // (réflexion) — attribut ignoré
                }
            }

            return (ViewGroup.LayoutParams) lp;

        } catch (Exception e) {
            // Si ConstraintLayout n'est pas dans le classpath, fallback générique
            return new ViewGroup.MarginLayoutParams(width, height);
        }
    }

    /**
     * Convertit un nom d'attribut app:layout_constraint* en nom de champ
     * de ConstraintLayout.LayoutParams.
     *
     * @param attr le nom d'attribut (ex: layout_constraintTop_toTopOf)
     * @return le nom du champ (ex: topToTop)
     */
    private String convertAttrToFieldName(String attr) {
        // Enlever le préfixe layout_constraint
        String name = attr.replace("layout_constraint", "");
        // Enlever le préfixe layout_editor_
        name = name.replace("layout_editor_", "");
        // Mettre la première lettre en minuscule
        if (name.length() > 0) {
            name = Character.toLowerCase(name.charAt(0)) + name.substring(1);
        }
        return name;
    }

    /**
     * Crée les LayoutParams pour CoordinatorLayout en lisant app:layout_behavior.
     *
     * @param width  largeur
     * @param height hauteur
     * @param parser le parser sur START_TAG
     * @return les LayoutParams
     */
    private ViewGroup.LayoutParams createCoordinatorLayoutParams(int width, int height,
                                                                  XmlPullParser parser) {
        try {
            Class<?> clClass = Class.forName("androidx.coordinatorlayout.widget.CoordinatorLayout");
            Class<?> lpClass = Class.forName("androidx.coordinatorlayout.widget.CoordinatorLayout$LayoutParams");

            java.lang.reflect.Constructor<?> ctor = lpClass.getConstructor(int.class, int.class);
            Object lp = ctor.newInstance(width, height);

            // Lire app:layout_behavior
            String behavior = getAttributeValue(parser, "app", "layout_behavior");
            if (behavior != null && !behavior.isEmpty()) {
                try {
                    // Instancier la classe de Behavior
                    Class<?> behaviorClass = Class.forName(behavior);
                    Object behaviorInstance = behaviorClass.getConstructor().newInstance();
                    java.lang.reflect.Method setBehavior = lpClass.getMethod("setBehavior",
                            Class.forName("androidx.coordinatorlayout.widget.CoordinatorLayout$Behavior"));
                    setBehavior.invoke(lp, behaviorInstance);
                } catch (Exception ignored) {
                    // Behavior non trouvé — pas grave
                }
            }

            // Lire app:layout_anchor et app:layout_anchorGravity
            String anchor = getAttributeValue(parser, "app", "layout_anchor");
            if (anchor != null) {
                if (anchor.startsWith("@+id/") || anchor.startsWith("@id/")) {
                    String idName = anchor.substring(anchor.indexOf('/') + 1);
                    int id = context.getResources().getIdentifier(idName, "id", context.getPackageName());
                    if (id != 0) {
                        java.lang.reflect.Field anchorField = lpClass.getField("anchorId");
                        anchorField.setInt(lp, id);
                    }
                }
            }

            return (ViewGroup.LayoutParams) lp;

        } catch (Exception e) {
            // Voulu : réflexion (ClassNotFoundException, InvocationTargetException,
            // IllegalAccessException…) — si la classe de LayoutParams spécifique
            // n'est pas disponible, fallback MarginLayoutParams générique
            return new ViewGroup.MarginLayoutParams(width, height);
        }
    }

    /**
     * Résout une référence d'id (@id/foo, @+id/foo) en entier.
     *
     * @param ref la référence
     * @return l'id entier, ou 0 si introuvable
     */
    private int resolveIdReference(String ref) {
        if (ref == null) return 0;
        String name;
        if (ref.startsWith("@+id/") || ref.startsWith("@id/")) {
            name = ref.substring(ref.indexOf('/') + 1);
        } else if (ref.startsWith("@android:id/")) {
            name = ref.substring("@android:id/".length());
            return context.getResources().getIdentifier(name, "id", "android");
        } else {
            return 0;
        }
        int id = context.getResources().getIdentifier(name, "id", context.getPackageName());
        if (id != 0) return id;
        // v3.5.1 — S3-étude § 2.3.3 : même map STABLE que les attributs
        // android:id — la référence ne génère JAMAIS deux ids pour un même
        // nom (ancres et contraintes non déterministes sinon).
        return dynamicIds.getId(name);
    }

    /**
     * Parse une valeur de gravité.
     */
    private int parseGravity(String value) {
        int gravity = 0;
        if (value == null) return gravity;
        String[] parts = value.split("\\|");
        for (String part : parts) {
            switch (part.trim()) {
                case "center": gravity |= android.view.Gravity.CENTER; break;
                case "center_horizontal": gravity |= android.view.Gravity.CENTER_HORIZONTAL; break;
                case "center_vertical": gravity |= android.view.Gravity.CENTER_VERTICAL; break;
                case "left": gravity |= android.view.Gravity.LEFT; break;
                case "right": gravity |= android.view.Gravity.RIGHT; break;
                case "top": gravity |= android.view.Gravity.TOP; break;
                case "bottom": gravity |= android.view.Gravity.BOTTOM; break;
                case "start": gravity |= android.view.Gravity.START; break;
                case "end": gravity |= android.view.Gravity.END; break;
                case "fill": gravity |= android.view.Gravity.FILL; break;
                case "fill_horizontal": gravity |= android.view.Gravity.FILL_HORIZONTAL; break;
                case "fill_vertical": gravity |= android.view.Gravity.FILL_VERTICAL; break;
            }
        }
        return gravity;
    }

    private int parseDimension(String value) {
        if (value == null || value.isEmpty()) {
            return 0;
        }
        // v3.4.7 — référence @… : résoudre via le ResourceResolver AVANT
        // l'analyse littérale (miroir du correctif v3.4.4 d'AttributeApplier).
        // Avant, un layout_width/layout_height/margin/padding portant
        // « @dimen/spacing_sm » levait InflateException « Dimension
        // invalide » qui tuait TOUT le rendu — le correctif v3.4.4 ne
        // couvrait que le chemin AttributeApplier, pas les LayoutParams.
        // Non résolue → 0 (dégradation visible, jamais de crash).
        if (value.startsWith("@")) {
            Float resolue = resolveDimensionReference(value);
            return resolue != null ? resolue.intValue() : 0;
        }
        try {
            return (int) parseDimensionInternal(value);
        } catch (NumberFormatException e) {
            throw new InflateException("Dimension invalide : " + value, e);
        }
    }

    /**
     * Résout une référence de dimension {@code @dimen/…} en pixels —
     * d'abord via le [ResourceResolver] (projet + framework, chaînable),
     * puis en repli natif par identifiant (miroir
     * d'AttributeApplier.resolveDimension).
     *
     * @param ref la référence complète ({@code @dimen/spacing_sm})
     * @return la valeur en pixels, ou {@code null} si introuvable
     */
    private Float resolveDimensionReference(String ref) {
        if (resourceResolver != null) {
            Float dim = resourceResolver.getDimension(ref);
            if (dim != null) {
                return dim;
            }
        }
        if (ref.startsWith("@dimen/") && context != null) {
            String name = ref.substring("@dimen/".length());
            int id = context.getResources().getIdentifier(name, "dimen", context.getPackageName());
            if (id != 0) {
                return context.getResources().getDimension(id);
            }
            id = context.getResources().getIdentifier(name, "dimen", "android");
            if (id != 0) {
                return context.getResources().getDimension(id);
            }
        }
        return null;
    }

    /**
     * Parse une dimension de layout (inclut {@code wrap_content} et
     * {@code match_parent}).
     *
     * @param value la chaîne à parser
     * @return la constante {@code MATCH_PARENT}, {@code WRAP_CONTENT} ou la
     *         valeur en pixels
     */
    private int parseLayoutDimension(String value) {
        if ("match_parent".equals(value) || "fill_parent".equals(value)) {
            return ViewGroup.LayoutParams.MATCH_PARENT;
        }
        if ("wrap_content".equals(value)) {
            return ViewGroup.LayoutParams.WRAP_CONTENT;
        }
        return parseDimension(value);
    }

    /**
     * Implémentation interne du parsing de dimension.
     *
     * @param value la chaîne à parser
     * @return la valeur en pixels
     */
    private float parseDimensionInternal(String value) {
        value = value.trim();
        if (value.endsWith("dp") || value.endsWith("dip")) {
            float v = Float.parseFloat(value.replaceAll("[a-zA-Z]+$", ""));
            return v * context.getResources().getDisplayMetrics().density;
        }
        if (value.endsWith("sp")) {
            float v = Float.parseFloat(value.replaceAll("[a-zA-Z]+$", ""));
            return v * context.getResources().getDisplayMetrics().scaledDensity;
        }
        if (value.endsWith("px")) {
            return Float.parseFloat(value.substring(0, value.length() - 2));
        }
        if (value.endsWith("mm")) {
            float v = Float.parseFloat(value.substring(0, value.length() - 2));
            return v * context.getResources().getDisplayMetrics().xdpi / 25.4f;
        }
        if (value.endsWith("in")) {
            float v = Float.parseFloat(value.substring(0, value.length() - 2));
            return v * context.getResources().getDisplayMetrics().xdpi;
        }
        // Pas d'unité : on suppose des pixels
        return Float.parseFloat(value);
    }

    /**
     * Recherche une vue par id après inflation.
     *
     * @param id l'id de la vue recherchée
     * @return la vue, ou {@code null} si aucune vue n'a cet id
     */
    public View findViewById(int id) {
        return idToView.get(id);
    }

    /**
     * @return une copie de la map id → vue indexée pendant la dernière inflation
     */
    public Map<Integer, View> getIndexedViews() {
        return new HashMap<>(idToView);
    }

    /**
     * Remet à zéro l'état interne (index des ids, compteur).
     *
     * <p>v3.4.0 — G12 : on NE vide PLUS le {@code constructorCache} de
     * {@link ViewFactory}. Les clés du cache sont des noms de classes —
     * ils ne changent pas entre deux rendus. Vider le cache forçait
     * {@code Class.forName} + {@code getConstructor} à chaque rendu, ce
     * qui ralentissait les previews sur des layouts à 50+ vues. Le cache
     * grossit avec le nombre de classes distinctes (borné par le nombre
     * de widgets Android — quelques centaines au maximum), donc aucun
     * risque de fuite mémoire.</p>
     */
    public void reset() {
        idToView.clear();
        // v3.5.1 — S3-étude § 2.3.3 : la map des ids dynamiques suit le
        // compteur historique (nouveau document = nouveaux ids).
        dynamicIds.clear();
        // v3.4.0 — G12 : ne PAS vider viewFactory.clearCache() — les
        // constructeurs sont stables entre rendus, le cache accélère
        // les re-rendus de 50-100x sur les gros layouts.
        constraintViewIds.clear();
        constraintAttributes.clear();
        // Compteurs d'introspection P6/L10-L11 : sémantique par rendu,
        // cohérente avec l'état de capture des contraintes
        factoryViewCount = 0;
        layoutTransitionCount = 0;
        // v3.7.0 — VIEWCOOKIE : nouvel inflat = nouveau document. Le
        // REGISTRE ACTIF se vide (celui de la RACINE — les vues du rendu
        // précédent ne fuient pas via la WeakHashMap racine), le site et
        // les ordinaux repartent de zéro.
        registreViewCookies.clear();
        siteCookies = "";
        ordinalNoeud = 0;
    }

    /**
     * Pose le VIEWCOOKIE d'une vue produite (v3.7.0 — étude PE14 § 2.4-4,
     * S8) : « site:ordinal » dans le registre PARTAGÉ (celui de la racine
     * — les inflaters enfants héritent par {@link #setContexteCookie}).
     *
     * @param vue     la vue produite
     * @param ordinal le rang du START_TAG dans la marche du document
     * @param site    la chaîne des {@code <include>} traversés
     */
    private void poserCookie(View vue, int ordinal, String site) {
        if (registreViewCookies != null) {
            registreViewCookies.put(vue, site + ":" + ordinal);
        }
    }

    /**
     * Le SITE d'un {@code <include>} d'ordinal [ordinal] : la chaîne des
     * sites courante étendue du rang de l'{@code <include>} — DEUX sites
     * distincts incluant le MÊME fichier produisent des chaînes distinctes
     * (les vues identiques restent discernables, l'objet du cookie).
     *
     * @param ordinal le rang du START_TAG {@code <include>}
     * @return le site du document inclus
     */
    private String siteDe(int ordinal) {
        return siteCookies.isEmpty() ? String.valueOf(ordinal)
                : siteCookies + ">" + ordinal;
    }

    /**
     * Le CONTEXTE COOKIE d'un inflater ENFANT (v3.7.0 — appelé par
     * {@link #parseInclude} et l'aperçu de listes) : le document de l'enfant
     * vit dans le SITE fourni et ses cookies partagent le REGISTRE de la
     * RACINE (toutes les vues d'un rendu au même endroit).
     *
     * @param site    le site du document enfant
     * @param registre le registre partagé (celui de la racine)
     */
    void setContexteCookie(String site, java.util.Map<View, String> registre) {
        this.siteCookies = site == null ? "" : site;
        this.registreViewCookies = registre != null ? registre : viewCookies;
    }

    /**
     * Les VIEWCOOKIES du DERNIER rendu (v3.7.0 — miroir du cookie
     * {@code ViewInfo} de la référence) : chaque vue inflatée → l'identité
     * de son NŒUD XML SOURCE (« site:ordinal »). Consommé par
     * {@link ViewInfoCollector} pour porter le cookie jusqu'à l'IDE —
     * l'appariement sélection/surlignage se fait par IDENTITÉ (un écart
     * de compte — include natif, ViewStub, rangée d'adapter — ne désarme
     * plus la suite du document).
     *
     * @return le registre VIVANT (WeakHashMap — ne PAS le conserver au-delà
     *         du rendu courant)
     */
    public java.util.Map<View, String> getViewCookies() {
        return registreViewCookies;
    }

    /**
     * @return la map des vues avec contraintes ConstraintLayout (view → id name)
     */
    public Map<View, String> getConstraintViewIds() {
        return new HashMap<>(constraintViewIds);
    }

    /**
     * @return true si l'inflation a collecté des vues ConstraintLayout
     */
    public boolean hasConstraintViews() {
        return !constraintViewIds.isEmpty();
    }

    /**
     * Capture les attributs de contrainte d'une vue pendant que le parser
     * est sur son START_TAG.
     *
     * <p>Enregistre toute vue portant au moins un attribut
     * {@code app:layout_constraint*}, {@code barrierDirection} ou
     * {@code constraint_referenced_ids}, avec :
     * <ul>
     *   <li>son id déclaratif ({@code android:id}, suffixe nu) via
     *       {@link #getConstraintViewIds()}</li>
     *   <li>la map complète de ses attributs de contrainte (ancres, biais,
     *       ratios, chaînes, marges, guideline/barrier, layout_width,
     *       layout_height, orientation) via {@link #getConstraintAttributes()}</li>
     * </ul>
     * Le {@code ConstraintSolver} (module-layout) consomme ces maps après
     * la passe de layout standard.</p>
     *
     * @param view   la vue créée
     * @param parser le parser positionné sur le START_TAG de la vue
     */
    private void captureConstraintAttributes(View view, XmlPullParser parser) {
        Map<String, String> constraintAttrs = new HashMap<>();
        Map<String, String> auxAttrs = new HashMap<>();
        String idName = null;
        int count = parser.getAttributeCount();
        for (int i = 0; i < count; i++) {
            String name = parser.getAttributeName(i);
            String value = parser.getAttributeValue(i);
            if (value == null) {
                continue;
            }
            String ns = parser.getAttributeNamespace(i);
            boolean isApp = "http://schemas.android.com/apk/res-auto".equals(ns);
            if (isApp && (name.startsWith("layout_constraint")
                    || "barrierDirection".equals(name)
                    || "constraint_referenced_ids".equals(name))) {
                constraintAttrs.put(name, value);
            } else if (isApp && "orientation".equals(name)) {
                auxAttrs.put(name, value);
            } else if ((ns == null || ns.isEmpty())
                    && ("layout_width".equals(name) || "layout_height".equals(name)
                    || "orientation".equals(name))) {
                // Sans namespace (kxml2 peut retirer les préfixes) :
                // dimensions déclarées + orientation
                auxAttrs.put(name, value);
            } else if ("http://schemas.android.com/apk/res/android".equals(ns)) {
                if ("id".equals(name)) {
                    idName = stripIdPrefix(value);
                } else if ("layout_width".equals(name) || "layout_height".equals(name)
                        || "orientation".equals(name)) {
                    auxAttrs.put(name, value);
                }
            }
        }
        // N'enregistrer que les vues portant au moins un vrai attribut de
        // contrainte — layout_width/height seuls (racine, vues quelconques)
        // ne suffisent pas.
        if (constraintAttrs.isEmpty()) {
            return;
        }
        // id via la lecture ns-app générale (fallback)
        if (idName == null) {
            String id = getAttributeValue(parser, "android", "id");
            if (id != null) {
                idName = stripIdPrefix(id);
            }
        }
        Map<String, String> attrs = new HashMap<>(constraintAttrs);
        attrs.putAll(auxAttrs);
        constraintAttributes.put(view, attrs);
        if (idName != null) {
            constraintViewIds.put(view, idName);
        }
    }

    /**
     * Retire le préfixe d'une référence d'id ({@code @+id/foo} / {@code @id/foo}).
     */
    private static String stripIdPrefix(String id) {
        if (id == null) {
            return null;
        }
        if (id.startsWith("@+id/") || id.startsWith("@id/")) {
            return id.substring(id.indexOf('/') + 1);
        }
        return id;
    }

    /**
     * @return une copie de la map des attributs de contrainte capturés
     *         (vue → attribut → valeur)
     */
    public Map<View, Map<String, String>> getConstraintAttributes() {
        return new HashMap<>(constraintAttributes);
    }

    /**
     * @return {@code true} si l'inflation a capturé des attributs de
     *         contrainte ConstraintLayout
     */
    public boolean hasConstraintAttributes() {
        return !constraintAttributes.isEmpty();
    }
}
