package jo.layoutlib.drawables;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
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
        } else if (reference.startsWith("<")) {
            // C'est déjà un XML inline
            xml = reference;
        } else if (reference.startsWith("#") || ColorParser.isColor(reference)) {
            try {
                return new ColorDrawable(ColorParser.parse(reference));
            } catch (ResourceException ignored) {
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
                // Pas implémenté en v1 — retourne null
                return null;
            case "ripple":
                // Pas implémenté en v1 — retourne null
                return null;
            case "inset":
                // Pas implémenté en v1 — retourne null
                return null;
            case "bitmap":
                // Pas implémenté en v1 — retourne null
                return null;
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
     * <p>Note : la création des drawables enfants nécessite un
     * {@link ResourceResolver} configuré. Sans resolver, seule la config
     * est retournée (via {@link #parseConfig(String)}).</p>
     */
    private Drawable createSelectorDrawable(String xml, Context context) {
        // La création d'un StateListDrawable avec drawables enfants nécessite
        // la résolution de @drawable/foo. Cette implémentation retourne null
        // pour l'instant — l'API parseConfig peut être utilisée pour inspecter.
        if (resourceResolver == null) {
            return null;
        }
        // TODO: implémenter la création de StateListDrawable avec résolution
        // des drawables enfants via resourceResolver.getDrawablePath()
        return null;
    }

    /**
     * Crée un VectorDrawable à partir d'un XML de vector.
     */
    private Drawable createVectorDrawable(String xml, Context context) {
        // La création d'un VectorDrawable nécessite l'API VectorDrawableCompat
        // ou l'utilisation de XmlResourceParser. Cette implémentation
        // retourne null pour l'instant.
        // TODO: implémenter via VectorDrawableCompat.createFromXml()
        return null;
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
