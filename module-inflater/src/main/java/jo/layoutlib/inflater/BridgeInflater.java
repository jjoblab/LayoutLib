package jo.layoutlib.inflater;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

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
 *   <li>Pas de support des adapters (ListView/RecyclerView) au moment de
 *       l'inflation.</li>
 *   <li>Les attributs custom {@code app:*} sont appliqués via le
 *       module-attributes.</li>
 * </ul>
 *
 * @author jo@Dev
 * @since 1.0
 */
public class BridgeInflater {

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

    /** Contraintes ConstraintLayout collectées pendant l'inflation. */
    private final Map<View, String> constraintViewIds = new HashMap<>();
    private final Map<View, org.xmlpull.v1.XmlPullParser> constraintParsers = new HashMap<>();

    /** Map des vues indexées par id. */
    private final Map<Integer, View> idToView = new HashMap<>();

    /** Compteur d'ids générés pour les vues sans id. */
    private int generatedIdCounter = 0x7f0a0001;

    /** Indique si le inflater doit lever une exception sur les attributs inconnus. */
    private boolean strictMode = false;

    /**
     * Construit un inflater par défaut avec les classes Design préférées.
     *
     * @param context contexte Android
     */
    public BridgeInflater(Context context) {
        this(context, new ViewTagRegistry(true));
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
     * @param resolver le résolveur, ou {@code null} pour désactiver la résolution
     */
    public void setResourceResolver(ResourceResolver resolver) {
        this.resourceResolver = resolver;
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
            return parseRoot(parser, parent);
        } catch (XmlPullParserException | IOException e) {
            throw new InflateException(
                    "Erreur pendant l'inflation : " + e.getMessage(), e);
        }
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
     * @param parser le parser positionné sur un START_TAG
     * @param parent le parent (peut être {@code null})
     * @return la vue créée, ou {@code null} pour {@code <merge>}
     */
    private View parseElement(XmlPullParser parser, ViewGroup parent)
            throws XmlPullParserException, IOException {
        String tag = parser.getName();

        if ("include".equals(tag)) {
            return parseInclude(parser, parent);
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

        // Cas standard : création d'une vue
        // IMPORTANT : utiliser (Context) seul — pas d'AttributeSet
        // car XmlPullAttributes ne peut pas être casté en XmlBlock.Parser sur Android 14+
        // Les attributs sont appliqués manuellement via AttributeApplier
        View view = viewFactory.createView(tag);

        // Appliquer tous les attributs XML via les setters natifs
        if (attributeApplier != null) {
            attributeApplier.applyAttributes(view, parser);
        } else {
            applyBaseAttributes(view, parser);
        }

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
            lp = new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
        }
        view.setLayoutParams(lp);

        // Traitement récursif des enfants si la vue est un ViewGroup
        if (view instanceof ViewGroup) {
            ViewGroup viewGroup = (ViewGroup) view;
            parseChildren(parser, viewGroup);
        } else {
            skipChildren(parser);
        }

        // Attachement au parent
        if (parent != null) {
            parent.addView(view, lp);
        }

        // Détecter si la vue a des contraintes ConstraintLayout
        String constraintAttr = getAttributeValue(parser, "app", "layout_constraintTop_toTopOf");
        if (constraintAttr == null) {
            constraintAttr = getAttributeValue(parser, "app", "layout_constraintStart_toStartOf");
        }
        if (constraintAttr == null) {
            constraintAttr = getAttributeValue(parser, "app", "layout_constraintLeft_toLeftOf");
        }
        if (constraintAttr != null) {
            // Cette vue a des contraintes — stocker pour résolution post-inflation
            String idName = getAttributeValue(parser, "android", "id");
            if (idName != null) {
                if (idName.startsWith("@+id/") || idName.startsWith("@id/")) {
                    idName = idName.substring(idName.indexOf('/') + 1);
                }
                constraintViewIds.put(view, idName);
            }
        }

        return view;
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
    private View parseInclude(XmlPullParser parser, ViewGroup parent)
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
        BridgeInflater childInflater = new BridgeInflater(context, tagRegistry);
        childInflater.setResourceResolver(resourceResolver);
        if (attributeApplier != null) {
            childInflater.setAttributeApplier(attributeApplier);
        }
        View firstChild = childInflater.inflate(includedXml, parent);
        return firstChild;
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
                try { llLp.weight = Float.parseFloat(weight); } catch (NumberFormatException ignored) {}
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
            if (gravity != null) {
                rlLp.addRule(android.widget.RelativeLayout.CENTER_IN_PARENT);
            }
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
                try { trLp.weight = Float.parseFloat(weight); } catch (NumberFormatException ignored) {}
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
                int ml = marginLeft != null ? parseDimension(marginLeft) :
                        (marginStart != null ? parseDimension(marginStart) : mlp.leftMargin);
                int mt = marginTop != null ? parseDimension(marginTop) : mlp.topMargin;
                int mr = marginRight != null ? parseDimension(marginRight) :
                        (marginEnd != null ? parseDimension(marginEnd) : mlp.rightMargin);
                int mb = marginBottom != null ? parseDimension(marginBottom) : mlp.bottomMargin;
                mlp.setMargins(ml, mt, mr, mb);
            }
        }

        return lp;
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
     * @param ns     le préfixe de namespace ({@code android} ou {@code app})
     * @param name   le nom de l'attribut
     * @return la valeur, ou {@code null} si l'attribut n'est pas présent
     */
    private String getAttributeValue(XmlPullParser parser, String ns, String name) {
        String nsUri;
        if ("android".equals(ns)) {
            nsUri = "http://schemas.android.com/apk/res/android";
        } else if ("app".equals(ns)) {
            nsUri = "http://schemas.android.com/apk/res-auto";
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
            // Génère un id unique pour les vues créées dynamiquement
            return generatedIdCounter++;
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
     * Applique les spécifications de GridLayout (row, column, span).
     *
     * @param glLp   les LayoutParams du GridLayout
     * @param parser le parser sur START_TAG
     */
    private void applyGridLayoutSpec(android.widget.GridLayout.LayoutParams glLp,
                                      XmlPullParser parser) {
        String layout_row = getAttributeValue(parser, "android", "layout_row");
        String layout_column = getAttributeValue(parser, "android", "layout_rowSpan");
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
                        try { field.setFloat(lp, Float.parseFloat(value)); } catch (NumberFormatException ignored) {}
                    } else if (field.getType() == String.class) {
                        field.set(lp, value);
                    }
                } catch (NoSuchFieldException ignored) {
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
        // Chercher dans idToView
        for (Map.Entry<Integer, View> entry : idToView.entrySet()) {
            // Pas possible de retrouver le nom depuis l'id sans Resources
            // On utilise generateViewId pour les ids créés
        }
        return View.generateViewId();
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
        try {
            return (int) parseDimensionInternal(value);
        } catch (NumberFormatException e) {
            throw new InflateException("Dimension invalide : " + value, e);
        }
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
     */
    public void reset() {
        idToView.clear();
        generatedIdCounter = 0x7f0a0001;
        viewFactory.clearCache();
        constraintViewIds.clear();
        constraintParsers.clear();
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
}
