package jo.layoutlib.drawables;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.Shader;
import android.graphics.drawable.AdaptiveIconDrawable;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.InsetDrawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.RippleDrawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Build;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jo.layoutlib.resources.ColorParser;
import jo.layoutlib.resources.DimensionConverter;
import jo.layoutlib.resources.ResourceException;
import jo.layoutlib.resources.ResourceResolver;

/**
 * Implémentation principale du {@link DrawableResolver}.
 *
 * <p>Cette classe est le cœur du Module 3 (Drawables). Elle sait détecter le
 * type de drawable XML (shape, selector, vector, layer-list, ripple, inset,
 * bitmap, color) et déléguer au parser spécialisé. Elle gère aussi le cache
 * des drawables résolus et la conversion des POJOs de configuration en
 * véritables objets {@link Drawable} Android.</p>
 *
 * <h2>Workflow</h2>
 * <ol>
 *   <li>Détection du type via le tag racine du XML.</li>
 *   <li>Délégation au parser spécialisé (produit un POJO de config).</li>
 *   <li>Conversion du POJO en {@link Drawable} Android via le Context.</li>
 *   <li>Mise en cache du résultat.</li>
 * </ol>
 *
 * <h2>Cache</h2>
 * <p>Les drawables résolus sont mis en cache (Map simple, pas de LRU ici car
 * les drawables sont typiquement peu nombreux). Le cache est invalidé
 * manuellement via {@link #clearCache()}.</p>
 *
 * <p>Référence layoutlib original :
 * {@code com.android.layoutlib.bridge.impl.ResourceHelper.getDrawable()}.</p>
 *
 * @author jo@Dev
 * @since 1.0
 */
public class DrawableResolverImpl implements DrawableResolver {

    /** Tag de journalisation. */
    private static final String TAG = "DrawableResolver";

    /** Cache des drawables parsés, indexé par XML source. */
    private final Map<String, Drawable> drawableCache = new HashMap<>();

    /** Cache des configs parsés (pour tests JVM sans Android). */
    private final Map<String, Object> configCache = new HashMap<>();

    /** Résolveur de resources pour résoudre les @drawable/foo. */
    private final ResourceResolver resourceResolver;

    /** Convertisseur de dimensions. */
    private final DimensionConverter dimensionConverter;

    /** Parser de shapes. */
    private final ShapeParser shapeParser;

    /** Parser de selectors. */
    private final SelectorParser selectorParser;

    /** Parser de vectors. */
    private final VectorParser vectorParser;

    /** Parser de layer-lists. */
    private final LayerListParser layerListParser;

    /** Parser de ripples. */
    private final RippleParser rippleParser;

    /** Parser d'insets. */
    private final InsetParser insetParser;

    /** Parser de bitmaps. */
    private final BitmapParser bitmapParser;

    /** Parser d'icônes adaptatives (ADR 0116 — P4/L6). */
    private final AdaptiveIconParser adaptiveIconParser;

    /** Parser d'animated-vectors (ADR 0116 — P5/L5). */
    private final AnimatedVectorParser animatedVectorParser;

    /**
     * Construit un resolver avec un résolveur de resources et convertisseur.
     *
     * @param resourceResolver  résolveur de resources (peut être {@code null})
     * @param dimensionConverter convertisseur de dimensions (peut être {@code null})
     */
    public DrawableResolverImpl(ResourceResolver resourceResolver,
                                DimensionConverter dimensionConverter) {
        this.resourceResolver = resourceResolver;
        this.dimensionConverter = dimensionConverter;
        this.shapeParser = new ShapeParser(dimensionConverter);
        this.selectorParser = new SelectorParser();
        this.vectorParser = new VectorParser(dimensionConverter);
        this.layerListParser = new LayerListParser(dimensionConverter);
        this.rippleParser = new RippleParser();
        this.insetParser = new InsetParser(dimensionConverter);
        this.bitmapParser = new BitmapParser();
        this.adaptiveIconParser = new AdaptiveIconParser();
        this.animatedVectorParser = new AnimatedVectorParser();
    }

    /**
     * Construit un resolver minimal sans dépendances.
     */
    public DrawableResolverImpl() {
        this(null, null);
    }

    @Override
    public Drawable resolve(String reference, Context context) {
        if (reference == null || reference.isEmpty()) {
            return null;
        }
        // Vérifier le cache
        Drawable cached = drawableCache.get(reference);
        if (cached != null) {
            return cached;
        }

        // Si c'est une couleur littérale, retourner un ColorDrawable
        if (ColorParser.isColor(reference)) {
            try {
                int color = ColorParser.parse(reference);
                ColorDrawable drawable = new ColorDrawable(color);
                drawableCache.put(reference, drawable);
                return drawable;
            } catch (ResourceException ignored) {
                // Voulu : couleur nommée inconnue — le fallback natif prend
                // le relais (resolve() continue)
            }
        }

        // Si c'est une référence @drawable/foo, récupérer le XML via le resolver
        String xml = null;
        if (reference.startsWith("@drawable/")) {
            if (resourceResolver == null) {
                return null;
            }
            String path = resourceResolver.getDrawablePath(reference);
            if (path == null) {
                return null;
            }
            // Lecture du fichier XML (si .xml)
            if (path.endsWith(".xml")) {
                xml = readFile(path);
            } else {
                // Fichier image — retourner un BitmapDrawable
                return loadBitmapDrawable(path, context);
            }
        } else if (reference.startsWith("@color/")) {
            // Couleur du projet — ColorDrawable
            if (resourceResolver != null) {
                Integer color = resourceResolver.getColor(reference);
                if (color != null) {
                    return new ColorDrawable(color);
                }
            }
            return null;
        } else if (reference.startsWith("<")) {
            // C'est déjà un XML inline
            xml = reference;
        } else if (reference.startsWith("#") || ColorParser.isColor(reference)) {
            try {
                return new ColorDrawable(ColorParser.parse(reference));
            } catch (ResourceException ignored) {
                // Voulu : couleur nommée inconnue — le fallback natif prend
                // le relais (resolve() continue)
            }
        }

        if (xml == null) {
            return null;
        }
        return parse(xml, context);
    }

    @Override
    public Drawable parse(String xml, Context context) {
        if (xml == null || xml.trim().isEmpty()) {
            return null;
        }
        // Détecter le type de drawable
        String rootTag = detectRootTag(xml);
        if (rootTag == null) {
            return null;
        }
        switch (rootTag) {
            case "shape":
                return createShapeDrawable(xml, context);
            case "selector":
                return createSelectorDrawable(xml, context);
            case "vector":
                return createVectorDrawable(xml, context);
            case "color":
                return createColorDrawable(xml);
            case "layer-list":
                return createLayerListDrawable(xml, context);
            case "ripple":
                return createRippleDrawable(xml, context);
            case "inset":
                return createInsetDrawable(xml, context);
            case "bitmap":
                return createBitmapDrawable(xml, context);
            case "adaptive-icon":
                return createAdaptiveIconDrawable(xml, context);
            case "animated-vector":
                return createAnimatedVectorDrawable(xml, context);
            default:
                return null;
        }
    }

    /**
     * Parse un XML et retourne l'objet de configuration (POJO) correspondant,
     * sans créer de Drawable Android.
     *
     * <p>Cette méthode est utilisée par les tests JVM qui ne peuvent pas
     * instancier de Drawable.</p>
     *
     * @param xml le XML source
     * @return le POJO de configuration ({@link ShapeConfig},
     *         {@link SelectorConfig}, {@link VectorConfig}, etc.)
     */
    public Object parseConfig(String xml) {
        if (xml == null || xml.trim().isEmpty()) {
            return null;
        }
        Object cached = configCache.get(xml);
        if (cached != null) {
            return cached;
        }
        String rootTag = detectRootTag(xml);
        Object config;
        switch (rootTag != null ? rootTag : "") {
            case "shape":
                config = shapeParser.parse(xml);
                break;
            case "selector":
                config = selectorParser.parse(xml);
                break;
            case "vector":
                config = vectorParser.parse(xml);
                break;
            case "layer-list":
                config = layerListParser.parse(xml);
                break;
            case "ripple":
                config = rippleParser.parse(xml);
                break;
            case "inset":
                config = insetParser.parse(xml);
                break;
            case "bitmap":
                config = bitmapParser.parse(xml);
                break;
            case "adaptive-icon":
                config = adaptiveIconParser.parse(xml);
                break;
            case "animated-vector":
                config = animatedVectorParser.parse(xml);
                break;
            default:
                config = null;
        }
        if (config != null) {
            configCache.put(xml, config);
        }
        return config;
    }

    @Override
    public void clearCache() {
        drawableCache.clear();
        configCache.clear();
    }

    /**
     * Détecte le tag racine d'un XML.
     *
     * @param xml le XML à analyser
     * @return le nom du tag racine, ou {@code null} si non trouvé
     */
    private String detectRootTag(String xml) {
        try {
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            factory.setNamespaceAware(true);
            XmlPullParser parser = factory.newPullParser();
            parser.setInput(new StringReader(xml));
            int event = parser.getEventType();
            while (event != XmlPullParser.END_DOCUMENT) {
                if (event == XmlPullParser.START_TAG) {
                    return parser.getName();
                }
                event = parser.next();
            }
        } catch (XmlPullParserException | IOException e) {
            return null;
        }
        return null;
    }

    /**
     * Crée un GradientDrawable à partir d'un XML de shape.
     */
    private Drawable createShapeDrawable(String xml, Context context) {
        ShapeConfig config = shapeParser.parse(xml);
        GradientDrawable drawable = new GradientDrawable();

        // Forme
        switch (config.getShapeType()) {
            case RECTANGLE:
                drawable.setShape(GradientDrawable.RECTANGLE);
                break;
            case OVAL:
                drawable.setShape(GradientDrawable.OVAL);
                break;
            case LINE:
                drawable.setShape(GradientDrawable.LINE);
                break;
            case RING:
                drawable.setShape(GradientDrawable.RING);
                break;
        }

        // Couleur unie
        if (config.getSolidColor() != null) {
            drawable.setColor(config.getSolidColor());
        }

        // Gradient
        if (config.hasGradient()) {
            int start = config.getGradientStartColor() != null
                    ? config.getGradientStartColor() : 0;
            int end = config.getGradientEndColor() != null
                    ? config.getGradientEndColor() : 0;
            if (config.getGradientCenterColor() != null) {
                drawable.setColors(new int[]{
                        start, config.getGradientCenterColor(), end});
            } else {
                drawable.setColors(new int[]{start, end});
            }
            drawable.setOrientation(convertGradientAngle(config.getGradientAngle()));
        }

        // Coins
        if (config.hasCorners()) {
            float[] radii = config.getCornerRadii();
            // radii = [topLeft, topRight, bottomRight, bottomLeft]
            // GradientDrawable.setCornerRadii attend [topLeftX, topLeftY, topRightX, topRightY, bottomRightX, bottomRightY, bottomLeftX, bottomLeftY]
            drawable.setCornerRadii(new float[]{
                    radii[0], radii[0], radii[1], radii[1],
                    radii[2], radii[2], radii[3], radii[3]
            });
        }

        // Stroke
        if (config.hasStroke()) {
            drawable.setStroke(
                    (int) config.getStrokeWidth(),
                    config.getStrokeColor() != null ? config.getStrokeColor() : Color.BLACK,
                    config.getStrokeDashWidth(),
                    config.getStrokeDashGap());
        }

        return drawable;
    }

    /**
     * Convertit un angle en degrés vers l'enum GradientDrawable.Orientation.
     */
    private GradientDrawable.Orientation convertGradientAngle(int angle) {
        switch (angle) {
            case 0:   return GradientDrawable.Orientation.LEFT_RIGHT;
            case 45:  return GradientDrawable.Orientation.BL_TR;
            case 90:  return GradientDrawable.Orientation.BOTTOM_TOP;
            case 135: return GradientDrawable.Orientation.BR_TL;
            case 180: return GradientDrawable.Orientation.RIGHT_LEFT;
            case 225: return GradientDrawable.Orientation.TR_BL;
            case 270: return GradientDrawable.Orientation.TOP_BOTTOM;
            case 315: return GradientDrawable.Orientation.TL_BR;
            default:  return GradientDrawable.Orientation.LEFT_RIGHT;
        }
    }

    /**
     * Crée un StateListDrawable à partir d'un XML de selector.
     *
     * <p>Chaque {@code <item>} est résolu : les références {@code @color/}
     * donnent un {@link ColorDrawable}, les références {@code @drawable/}
     * sont résolues récursivement (shapes, selectors imbriqués…), et les
     * couleurs littérales ({@code #RRGGBB}) donnent également un
     * {@link ColorDrawable}. Un item non résolvable est ignoré — jamais
     * d'exception qui casserait le rendu entier.</p>
     */
    private Drawable createSelectorDrawable(String xml, Context context) {
        SelectorConfig config = selectorParser.parse(xml);
        if (config == null || config.getItemCount() == 0) {
            return null;
        }
        StateListDrawable stateList = new StateListDrawable();
        int added = 0;
        for (SelectorConfig.SelectorItem item : config.getItems()) {
            Drawable child = resolveChildDrawable(item.getDrawableRef(), context);
            if (child == null) {
                continue;
            }
            stateList.addState(toStateSet(item), child);
            added++;
        }
        return added > 0 ? stateList : null;
    }

    /**
     * Résout le drawable d'un item de selector : {@code @drawable/},
     * {@code @color/} ou couleur littérale.
     *
     * @param ref     la référence de l'item (peut être {@code null})
     * @param context le contexte Android
     * @return le drawable, ou {@code null} si non résolvable
     */
    private Drawable resolveChildDrawable(String ref, Context context) {
        if (ref == null || ref.isEmpty()) {
            return null;
        }
        if (ref.startsWith("@color/") || ref.startsWith("#")) {
            Integer color = null;
            if (ref.startsWith("@color/")) {
                if (resourceResolver != null) {
                    color = resourceResolver.getColor(ref);
                }
            } else {
                try {
                    color = ColorParser.parse(ref);
                } catch (ResourceException ignored) {
                    color = null;
                }
            }
            return color != null ? new ColorDrawable(color) : null;
        }
        // @drawable/foo, XML inline, couleur nommée — délégation à resolve()
        try {
            return resolve(ref, context);
        } catch (RuntimeException e) {
            // Un item cassé ne doit pas casser tout le selector
            return null;
        }
    }

    /**
     * Convertit les états d'un {@link SelectorConfig.SelectorItem} en
     * {@code int[]} pour {@link StateListDrawable#addState(int[], Drawable)}.
     *
     * <p>Un état à {@code true} utilise l'attribut positif, à {@code false}
     * l'attribut négé (préfixé par {@code -}), conformément au protocole
     * {@code Drawable.setState(int[])}.</p>
     */
    private static int[] toStateSet(SelectorConfig.SelectorItem item) {
        List<Integer> states = new ArrayList<>();
        addState(states, android.R.attr.state_pressed, item.getStatePressed());
        addState(states, android.R.attr.state_enabled, item.getStateEnabled());
        addState(states, android.R.attr.state_focused, item.getStateFocused());
        addState(states, android.R.attr.state_checked, item.getStateChecked());
        addState(states, android.R.attr.state_selected, item.getStateSelected());
        addState(states, android.R.attr.state_window_focused,
                item.getStateWindowFocused());
        addState(states, android.R.attr.state_checkable, item.getStateCheckable());
        int[] result = new int[states.size()];
        for (int i = 0; i < result.length; i++) {
            result[i] = states.get(i);
        }
        return result;
    }

    /**
     * Ajoute un état (positif ou négé) à la liste.
     */
    private static void addState(List<Integer> states, int attr, Boolean value) {
        if (value != null) {
            states.add(value ? attr : -attr);
        }
    }

    /**
     * Crée un VectorDrawable à partir d'un XML de vector.
     *
     * <p>Implémentation via l'inflation native du framework
     * ({@link Drawable#createFromXml(Resources, XmlPullParser)}), disponible
     * pour {@code <vector>} depuis l'API 21 (minSdk = 24) — pas besoin de
     * {@code VectorDrawableCompat} (qui imposerait une dépendance AndroidX
     * à ce module). En cas d'échec (XML invalide, JVM sans Resources réel),
     * retourne {@code null} sans lever : l'attribut est simplement ignoré.</p>
     */
    private Drawable createVectorDrawable(String xml, Context context) {
        if (context == null) {
            return null;
        }
        try {
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            factory.setNamespaceAware(true);
            XmlPullParser parser = factory.newPullParser();
            parser.setInput(new StringReader(xml));
            return Drawable.createFromXml(context.getResources(), parser);
        } catch (XmlPullParserException | IOException | RuntimeException e) {
            // VectorDrawable invalide ou Resources indisponible (tests JVM) —
            // fallback silencieux, l'attribut est ignoré
            return null;
        }
    }

    /**
     * Crée un ColorDrawable à partir d'un XML {@code <color>}.
     */
    private Drawable createColorDrawable(String xml) {
        try {
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            factory.setNamespaceAware(true);
            XmlPullParser parser = factory.newPullParser();
            parser.setInput(new StringReader(xml));
            int event = parser.getEventType();
            while (event != XmlPullParser.END_DOCUMENT) {
                if (event == XmlPullParser.START_TAG && "color".equals(parser.getName())) {
                    String colorStr = parser.getAttributeValue(
                            "http://schemas.android.com/apk/res/android", "color");
                    if (colorStr == null) {
                        colorStr = parser.getAttributeValue(null, "color");
                    }
                    if (colorStr != null) {
                        return new ColorDrawable(ColorParser.parse(colorStr));
                    }
                }
                event = parser.next();
            }
        } catch (XmlPullParserException | IOException | ResourceException e) {
            return null;
        }
        return null;
    }

    /**
     * Crée un LayerDrawable à partir d'un XML de layer-list.
     *
     * <p>Chaque {@code <item>} est résolu : les références
     * ({@code @drawable/}, {@code @color/}, littérales) passent par
     * {@link #resolveChildDrawable(String, Context)}, et les drawables
     * inline (shape, selector, vector, color imbriqués) sont re-parsés
     * récursivement. Les insets éventuels
     * ({@code android:left/top/right/bottom}) sont appliqués par calque.
     * Un calque non résolvable est ignoré — jamais d'exception qui casserait
     * le rendu entier (philosophie placeholder neutre) ; si aucun calque ne
     * résout, retourne {@code null}.</p>
     *
     * @param xml     le XML source
     * @param context le contexte Android (peut être {@code null} en JVM)
     * @return le LayerDrawable, ou {@code null}
     */
    private Drawable createLayerListDrawable(String xml, Context context) {
        LayerListConfig config;
        try {
            config = layerListParser.parse(xml);
        } catch (DrawableException e) {
            return null;
        }
        if (config.getItemCount() == 0) {
            return null;
        }
        List<Drawable> layers = new ArrayList<>();
        List<LayerListConfig.LayerItem> appliedItems = new ArrayList<>();
        for (LayerListConfig.LayerItem item : config.getItems()) {
            Drawable child = resolveLayerChild(item.getDrawableRef(),
                    item.getNestedXml(), context);
            if (child == null) {
                continue;
            }
            layers.add(child);
            appliedItems.add(item);
        }
        if (layers.isEmpty()) {
            return null;
        }
        try {
            LayerDrawable layerDrawable =
                    new LayerDrawable(layers.toArray(new Drawable[0]));
            for (int i = 0; i < appliedItems.size(); i++) {
                LayerListConfig.LayerItem item = appliedItems.get(i);
                if (item.hasAnyInset()) {
                    layerDrawable.setLayerInset(i,
                            toPx(item.getInsetLeft()), toPx(item.getInsetTop()),
                            toPx(item.getInsetRight()), toPx(item.getInsetBottom()));
                }
            }
            return layerDrawable;
        } catch (RuntimeException e) {
            // Drawables null dans le tableau (JVM) — repli silencieux
            return null;
        }
    }

    /**
     * Crée le rendu statique d'une icône adaptative (ADR 0116 — P4/L6).
     *
     * <p>Les couches {@code <background>} et {@code <foreground>} sont
     * résolues comme les calques d'un layer-list (référence prioritaire,
     * puis XML inline). Selon le niveau d'API au moment de l'exécution :</p>
     * <ul>
     *   <li><strong>API 33+</strong> : {@link AdaptiveIconDrawable} à trois
     *       couches (monochrome incluse) ;</li>
     *   <li><strong>API 26-32</strong> : {@link AdaptiveIconDrawable} à deux
     *       couches ;</li>
     *   <li><strong>API 24-25 et JVM de test</strong> : repli
     *       {@link LayerDrawable} superposant background puis foreground —
     *       la couche monochrome (icônes thémées du lanceur) n'a pas de sens
     *       en preview statique et n'est ajoutée que si elle est la seule
     *       couche présente.</li>
     * </ul>
     *
     * <p>Philosophie placeholder : une couche non résolvable est ignorée ;
     * si aucune couche ne résout, retourne {@code null} (l'appelant affiche
     * son placeholder neutre).</p>
     *
     * @param xml     le XML source
     * @param context le contexte Android (peut être {@code null} en JVM)
     * @return l'icône adaptative, ou {@code null}
     */
    private Drawable createAdaptiveIconDrawable(String xml, Context context) {
        AdaptiveIconConfig config;
        try {
            config = adaptiveIconParser.parse(xml);
        } catch (DrawableException e) {
            return null;
        }
        if (!config.hasAnyLayer()) {
            return null;
        }
        Drawable background = resolveLayerChild(
                config.getBackgroundRef(), config.getBackgroundXml(), context);
        Drawable foreground = resolveLayerChild(
                config.getForegroundRef(), config.getForegroundXml(), context);
        Drawable monochrome = resolveLayerChild(
                config.getMonochromeRef(), config.getMonochromeXml(), context);

        try {
            if (background != null && foreground != null) {
                if (Build.VERSION.SDK_INT >= 33 && monochrome != null) {
                    return new AdaptiveIconDrawable(background, foreground, monochrome);
                }
                if (Build.VERSION.SDK_INT >= 26) {
                    return new AdaptiveIconDrawable(background, foreground);
                }
                // Repli statique (API 24-25, JVM) : superposition simple
                return new LayerDrawable(
                        new Drawable[]{background, foreground});
            }
            // Une seule couche résolue : l'afficher telle quelle
            List<Drawable> layers = new ArrayList<>();
            if (background != null) {
                layers.add(background);
            }
            if (foreground != null) {
                layers.add(foreground);
            }
            if (layers.isEmpty() && monochrome != null) {
                layers.add(monochrome);
            }
            if (layers.isEmpty()) {
                return null;
            }
            return new LayerDrawable(layers.toArray(new Drawable[0]));
        } catch (RuntimeException e) {
            // Constructeur AdaptiveIconDrawable indisponible (JVM mockable
            // selon la cible) — repli LayerDrawable
            List<Drawable> layers = new ArrayList<>();
            if (background != null) layers.add(background);
            if (foreground != null) layers.add(foreground);
            if (layers.isEmpty()) return null;
            return new LayerDrawable(layers.toArray(new Drawable[0]));
        }
    }

    /**
     * Crée le rendu statique d'un animated-vector : sa <strong>première
     * frame</strong> (ADR 0116 — P5/L5).
     *
     * <p>Deux stratégies, dans l'ordre :</p>
     * <ol>
     *   <li><strong>Inflation native complète</strong> (appareil uniquement) :
     *       {@code Drawable.createFromXml()} sur le XML animated-vector entier —
     *       donne un vrai {@code AnimatedVectorDrawable} posé sur sa frame
     *       initiale, si toutes les références {@code @drawable/}/{@code @anim/}
     *       résolvent dans les {@code Resources} du contexte. En JVM mockable,
     *       {@code createFromXml} retourne null ({@code returnDefaultValues}) ;
     *       toute exception (référence manquante) bascule silencieusement sur
     *       la stratégie 2 ;</li>
     *   <li><strong>Résolution LayoutLib</strong> (pipeline preview) : le
     *       vector sous-jacent est résolu comme un calque de composite —
     *       référence {@code android:drawable} prioritaire (forme API 24+),
     *       puis drawable inline embarqué (forme API 21-23) — puis retourné
     *       tel quel : c'est la première frame.</li>
     * </ol>
     *
     * <p>Les cibles {@code <target>} ({@code @anim/}) ne sont jamais résolues :
     * l'animation n'a pas de sens en preview statique.</p>
     *
     * <p>Philosophie placeholder : un animated-vector sans vector résolvable
     * retourne {@code null} (l'appelant affiche son placeholder neutre),
     * jamais d'exception.</p>
     *
     * @param xml     le XML source
     * @param context le contexte Android (peut être {@code null} en JVM)
     * @return la première frame (drawable du vector sous-jacent), ou {@code null}
     */
    private Drawable createAnimatedVectorDrawable(String xml, Context context) {
        AnimatedVectorConfig config;
        try {
            config = animatedVectorParser.parse(xml);
        } catch (DrawableException e) {
            return null;
        }

        // 1. Inflation native (appareil) — AnimatedVectorDrawable réel
        Drawable nativeDrawable = tryInflateNativeAnimatedVector(xml, context);
        if (nativeDrawable != null) {
            return nativeDrawable;
        }

        // 2. Résolution LayoutLib — première frame = drawable sous-jacent
        if (!config.hasDrawable()) {
            return null;
        }
        try {
            return resolveLayerChild(
                    config.getDrawableRef(), config.getDrawableXml(), context);
        } catch (RuntimeException e) {
            // Vector sous-jacent cassé — placeholder neutre, jamais d'exception
            return null;
        }
    }

    /**
     * Tente l'inflation native d'un XML animated-vector complet via
     * {@code Drawable.createFromXml()}.
     *
     * <p>Ne réussit que sur appareil, avec un contexte dont les
     * {@code Resources} résolvent les références {@code @drawable/} et
     * {@code @anim/} du XML. Retourne {@code null} en toute autre situation
     * (JVM mockable, contexte sans resources, référence manquante) — sans
     * jamais lever.</p>
     */
    private Drawable tryInflateNativeAnimatedVector(String xml, Context context) {
        if (context == null) {
            return null;
        }
        try {
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            factory.setNamespaceAware(true);
            XmlPullParser parser = factory.newPullParser();
            parser.setInput(new StringReader(xml));
            return Drawable.createFromXml(context.getResources(), parser);
        } catch (XmlPullParserException | IOException | RuntimeException e) {
            // Référence @anim/@drawable non résolvable en Resources natives,
            // ou resources indisponibles — repli résolution LayoutLib
            return null;
        }
    }

    /**
     * Crée un RippleDrawable à partir d'un XML de ripple.
     *
     * <p>La couleur {@code android:color} est obligatoire. Le contenu et le
     * masque ({@code <item android:id="@android:id/mask">}) sont optionnels
     * et résolus comme les calques d'un layer-list. Sans couleur, retourne
     * {@code null} (un ripple sans couleur n'a aucun effet visible).</p>
     *
     * @param xml     le XML source
     * @param context le contexte Android (peut être {@code null} en JVM)
     * @return le RippleDrawable, ou {@code null}
     */
    private Drawable createRippleDrawable(String xml, Context context) {
        RippleConfig config;
        try {
            config = rippleParser.parse(xml);
        } catch (DrawableException e) {
            return null;
        }
        if (!config.hasColor()) {
            return null;
        }
        Drawable content = resolveLayerChild(
                config.getContent() != null ? config.getContent().getDrawableRef() : null,
                config.getContent() != null ? config.getContent().getNestedXml() : null,
                context);
        Drawable mask = resolveLayerChild(
                config.getMask() != null ? config.getMask().getDrawableRef() : null,
                config.getMask() != null ? config.getMask().getNestedXml() : null,
                context);
        try {
            return new RippleDrawable(
                    ColorStateList.valueOf(config.getColor()), content, mask);
        } catch (RuntimeException e) {
            // ColorStateList null en JVM de test — repli silencieux
            return null;
        }
    }

    /**
     * Crée un InsetDrawable à partir d'un XML d'inset.
     *
     * <p>Le drawable enfant ({@code android:drawable} ou inline) est
     * obligatoire : sans lui, l'inset n'encadre rien et retourne
     * {@code null}. Les quatre insets sont appliqués en pixels.</p>
     *
     * @param xml     le XML source
     * @param context le contexte Android (peut être {@code null} en JVM)
     * @return le InsetDrawable, ou {@code null}
     */
    private Drawable createInsetDrawable(String xml, Context context) {
        InsetConfig config;
        try {
            config = insetParser.parse(xml);
        } catch (DrawableException e) {
            return null;
        }
        Drawable child = resolveLayerChild(config.getDrawableRef(),
                config.getNestedXml(), context);
        if (child == null) {
            return null;
        }
        try {
            return new InsetDrawable(child,
                    toPx(config.getInsetLeft()), toPx(config.getInsetTop()),
                    toPx(config.getInsetRight()), toPx(config.getInsetBottom()));
        } catch (RuntimeException e) {
            return null;
        }
    }

    /**
     * Crée un BitmapDrawable (ou ColorDrawable pour une source couleur) à
     * partir d'un XML de bitmap.
     *
     * <p>La source {@code android:src} est résolue : fichier image →
     * {@link BitmapDrawable} avec tuilage ({@code tileMode/tileModeX/Y}),
     * gravité et drapeaux de qualité appliqués ; couleur
     * ({@code @color/} ou littérale) → {@link ColorDrawable} avec alpha et
     * tint. En JVM de test (pas de Resources réel), le décodage d'image
     * échoue silencieusement → {@code null}.</p>
     *
     * @param xml     le XML source
     * @param context le contexte Android (peut être {@code null} en JVM)
     * @return le drawable, ou {@code null}
     */
    private Drawable createBitmapDrawable(String xml, Context context) {
        BitmapConfig config;
        try {
            config = bitmapParser.parse(xml);
        } catch (DrawableException e) {
            return null;
        }
        if (!config.hasSource()) {
            return null;
        }
        String srcRef = config.getSrcRef();
        Drawable drawable = null;

        if (srcRef.startsWith("#") || srcRef.startsWith("@color/")) {
            Integer color = null;
            if (srcRef.startsWith("#")) {
                try {
                    color = ColorParser.parse(srcRef);
                } catch (ResourceException ignored) {
                    color = null;
                }
            } else if (resourceResolver != null) {
                color = resourceResolver.getColor(srcRef);
            }
            if (color != null) {
                drawable = new ColorDrawable(color);
            }
        } else if (srcRef.startsWith("@drawable/") && resourceResolver != null) {
            String path = resourceResolver.getDrawablePath(srcRef);
            if (path != null && !path.endsWith(".xml")) {
                drawable = loadBitmapDrawable(path, context);
            } else if (path != null) {
                // Source pointant vers un drawable XML — délégation récursive
                drawable = resolveLayerChild(srcRef, null, context);
            }
        }
        if (drawable == null) {
            return null;
        }

        if (drawable instanceof BitmapDrawable) {
            BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
            Shader.TileMode tileX = toTileMode(firstNonNull(
                    config.getTileModeX(), config.getTileMode()));
            Shader.TileMode tileY = toTileMode(firstNonNull(
                    config.getTileModeY(), config.getTileMode()));
            if (tileX != null || tileY != null) {
                bitmapDrawable.setTileModeXY(
                        tileX != null ? tileX : Shader.TileMode.CLAMP,
                        tileY != null ? tileY : Shader.TileMode.CLAMP);
            }
            if (config.getGravity() != null) {
                bitmapDrawable.setGravity(config.getGravity());
            }
            if (config.getAntialias() != null) {
                bitmapDrawable.setAntiAlias(config.getAntialias());
            }
            if (config.getDither() != null) {
                bitmapDrawable.setDither(config.getDither());
            }
            if (config.getFilter() != null) {
                bitmapDrawable.setFilterBitmap(config.getFilter());
            }
        }
        if (config.getAlpha() != null) {
            drawable.setAlpha(Math.round(config.getAlpha() * 255f));
        }
        Integer tint = parseTintColor(config.getTint());
        if (tint != null) {
            drawable.setTint(tint);
            PorterDuff.Mode mode = toTintMode(config.getTintMode());
            if (mode != null) {
                drawable.setTintMode(mode);
            }
        }
        return drawable;
    }

    /**
     * Résout un enfant de drawable composite : référence d'abord, XML
     * imbriqué ensuite.
     *
     * @param ref      la référence ({@code @drawable/…}, {@code @color/…},
     *                 littérale), ou {@code null}
     * @param nestedXml le XML inline capturé, ou {@code null}
     * @param context  le contexte Android
     * @return le drawable enfant, ou {@code null} si non résolvable
     */
    private Drawable resolveLayerChild(String ref, String nestedXml, Context context) {
        if (ref != null && !ref.isEmpty()) {
            return resolveChildDrawable(ref, context);
        }
        if (nestedXml != null && !nestedXml.isEmpty()) {
            try {
                return parse(nestedXml, context);
            } catch (RuntimeException e) {
                // Un enfant cassé ne doit pas casser le composite entier
                return null;
            }
        }
        return null;
    }

    /**
     * Convertit un Float éventuellement null en pixels int.
     */
    private static int toPx(Float value) {
        return value != null ? Math.round(value) : 0;
    }

    /**
     * Retourne le premier non-null des deux valeurs.
     */
    private static <T> T firstNonNull(T first, T second) {
        return first != null ? first : second;
    }

    /**
     * Convertit une valeur de tileMode en {@link Shader.TileMode}.
     *
     * @param value la valeur brute ({@code clamp}, {@code repeat},
     *              {@code mirror}, {@code disabled})
     * @return le mode, ou {@code null} pour absent/disabled
     */
    private static Shader.TileMode toTileMode(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        switch (value.toLowerCase()) {
            case "clamp":   return Shader.TileMode.CLAMP;
            case "repeat":  return Shader.TileMode.REPEAT;
            case "mirror":  return Shader.TileMode.MIRROR;
            default:        return null; // "disabled" et inconnus : pas de tuilage
        }
    }

    /**
     * Convertit une valeur de tintMode en {@link PorterDuff.Mode}.
     *
     * @param value la valeur brute ({@code src_over}, {@code multiply}, …)
     * @return le mode, ou {@code null} si absent/inconnu
     */
    private static PorterDuff.Mode toTintMode(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        switch (value.toLowerCase()) {
            case "src_over": return PorterDuff.Mode.SRC_OVER;
            case "src_in":   return PorterDuff.Mode.SRC_IN;
            case "src_atop": return PorterDuff.Mode.SRC_ATOP;
            case "multiply": return PorterDuff.Mode.MULTIPLY;
            case "screen":   return PorterDuff.Mode.SCREEN;
            case "add":      return PorterDuff.Mode.ADD;
            case "clear":    return PorterDuff.Mode.CLEAR;
            default:         return null;
        }
    }

    /**
     * Parse une couleur de tint (littérale ou {@code @color/}).
     *
     * @param value la valeur brute
     * @return la couleur, ou {@code null} si non résolvable
     */
    private Integer parseTintColor(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        if (value.startsWith("@color/") && resourceResolver != null) {
            return resourceResolver.getColor(value);
        }
        try {
            return ColorParser.parse(value);
        } catch (ResourceException e) {
            return null;
        }
    }

    /**
     * Charge un BitmapDrawable depuis un fichier image.
     */
    private Drawable loadBitmapDrawable(String path, Context context) {
        try {
            android.graphics.BitmapFactory.Options opts = new android.graphics.BitmapFactory.Options();
            opts.inDensity = context.getResources().getDisplayMetrics().densityDpi;
            android.graphics.Bitmap bm = android.graphics.BitmapFactory.decodeFile(path, opts);
            if (bm != null) {
                return new android.graphics.drawable.BitmapDrawable(
                        context.getResources(), bm);
            }
        } catch (Exception e) {
            return null;
        }
        return null;
    }

    /**
     * Lit le contenu d'un fichier en UTF-8.
     */
    private String readFile(String path) {
        try (java.io.FileInputStream fis = new java.io.FileInputStream(path)) {
            byte[] data = new byte[fis.available()];
            fis.read(data);
            return new String(data, "UTF-8");
        } catch (IOException e) {
            return null;
        }
    }
}
