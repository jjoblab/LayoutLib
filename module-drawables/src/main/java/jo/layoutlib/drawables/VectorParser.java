package jo.layoutlib.drawables;

import android.util.Log;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.io.StringReader;

import jo.layoutlib.resources.ColorParser;
import jo.layoutlib.resources.DimensionConverter;
import jo.layoutlib.resources.ResourceException;

/**
 * Parser de drawables XML de type {@code <vector>}.
 *
 * <p>Transforme un VectorDrawable XML en {@link VectorConfig} contenant
 * les dimensions, viewport, groupes et paths.</p>
 *
 * <p>Référence layoutlib original :
 * {@code com.android.layoutlib.bridge.impl.ResourceHelper.getDrawable()}.</p>
 *
 * @author jo@Dev
 * @since 1.0
 */
public class VectorParser {

    /** Tag de journalisation. */
    private static final String TAG = "VectorParser";

    /** Convertisseur de dimensions. */
    private final DimensionConverter dimensionConverter;

    /**
     * Construit un parser avec convertisseur de dimensions.
     *
     * @param dimensionConverter le convertisseur (peut être {@code null})
     */
    public VectorParser(DimensionConverter dimensionConverter) {
        this.dimensionConverter = dimensionConverter;
    }

    /**
     * Parse un XML de vector en VectorConfig.
     *
     * @param xml le XML source
     * @return la configuration parsée
     * @throws DrawableException si le XML est invalide
     */
    public VectorConfig parse(String xml) {
        if (xml == null || xml.trim().isEmpty()) {
            throw new DrawableException("XML de vector vide", "vector");
        }
        try {
            XmlPullParser parser = createParser(xml);
            return parseDocument(parser);
        } catch (XmlPullParserException | IOException e) {
            throw new DrawableException(
                    "Erreur de parsing vector : " + e.getMessage(), "vector", e);
        }
    }

    private XmlPullParser createParser(String xml) throws XmlPullParserException {
        XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
        factory.setNamespaceAware(true);
        XmlPullParser parser = factory.newPullParser();
        parser.setInput(new StringReader(xml));
        return parser;
    }

    private VectorConfig parseDocument(XmlPullParser parser)
            throws XmlPullParserException, IOException {
        VectorConfig config = new VectorConfig();
        int event = parser.getEventType();
        while (event != XmlPullParser.END_DOCUMENT) {
            if (event == XmlPullParser.START_TAG) {
                String tag = parser.getName();
                if ("vector".equals(tag)) {
                    parseVectorAttributes(config, parser);
                } else if ("group".equals(tag)) {
                    VectorConfig.GroupNode group = new VectorConfig.GroupNode();
                    parseGroupAttributes(group, parser);
                    parseGroupChildren(parser, group);
                    config.addNode(group);
                } else if ("path".equals(tag)) {
                    VectorConfig.PathNode path = new VectorConfig.PathNode();
                    parsePathAttributes(path, parser);
                    config.addNode(path);
                }
            }
            event = parser.next();
        }
        return config;
    }

    /**
     * Parse les attributs du tag racine {@code <vector>}.
     */
    private void parseVectorAttributes(VectorConfig config, XmlPullParser parser) {
        config.setWidth(parseDimension(getAttribute(parser, "android", "width")));
        config.setHeight(parseDimension(getAttribute(parser, "android", "height")));
        config.setViewportWidth(parseFloat(getAttribute(parser, "android", "viewportWidth")));
        config.setViewportHeight(parseFloat(getAttribute(parser, "android", "viewportHeight")));
        config.setAlpha(parseFloat(getAttribute(parser, "android", "alpha"), 1f));
        config.setAutoMirrored("true".equals(getAttribute(parser, "android", "autoMirrored")));

        String tint = getAttribute(parser, "android", "tint");
        if (tint != null) {
            try {
                config.setTint(ColorParser.parse(tint));
            } catch (ResourceException e) {
                // Voulu : couleur/dimension invalide dans le vector — ignorée pour ne pas
                // casser le parsing du drawable entier
                Log.w(TAG, "Valeur invalide ignorée : " + e.getMessage());
            }
        }
        String tintMode = getAttribute(parser, "android", "tintMode");
        if (tintMode != null) {
            config.setTintMode(tintMode);
        }
    }

    /**
     * Parse les attributs d'un {@code <group>}.
     */
    private void parseGroupAttributes(VectorConfig.GroupNode group, XmlPullParser parser) {
        group.setName(getAttribute(parser, "android", "name"));
        group.setRotation(parseFloat(getAttribute(parser, "android", "rotation")));
        group.setPivotX(parseFloat(getAttribute(parser, "android", "pivotX")));
        group.setPivotY(parseFloat(getAttribute(parser, "android", "pivotY")));
        group.setScaleX(parseFloat(getAttribute(parser, "android", "scaleX"), 1f));
        group.setScaleY(parseFloat(getAttribute(parser, "android", "scaleY"), 1f));
        group.setTranslateX(parseFloat(getAttribute(parser, "android", "translateX")));
        group.setTranslateY(parseFloat(getAttribute(parser, "android", "translateY")));
    }

    /**
     * Parse les enfants d'un {@code <group>}.
     */
    private void parseGroupChildren(XmlPullParser parser, VectorConfig.GroupNode group)
            throws XmlPullParserException, IOException {
        int depth = 1;
        while (depth > 0) {
            int event = parser.next();
            if (event == XmlPullParser.START_TAG) {
                String tag = parser.getName();
                if ("path".equals(tag)) {
                    VectorConfig.PathNode path = new VectorConfig.PathNode();
                    parsePathAttributes(path, parser);
                    group.addChild(path);
                } else if ("group".equals(tag)) {
                    VectorConfig.GroupNode childGroup = new VectorConfig.GroupNode();
                    parseGroupAttributes(childGroup, parser);
                    parseGroupChildren(parser, childGroup);
                    group.addChild(childGroup);
                    depth--;  // le childGroup a déjà consommé son END_TAG
                }
            } else if (event == XmlPullParser.END_TAG) {
                depth--;
            } else if (event == XmlPullParser.END_DOCUMENT) {
                break;
            }
        }
    }

    /**
     * Parse les attributs d'un {@code <path>}.
     */
    private void parsePathAttributes(VectorConfig.PathNode path, XmlPullParser parser) {
        path.setPathData(getAttribute(parser, "android", "pathData"));

        String fillColor = getAttribute(parser, "android", "fillColor");
        if (fillColor != null) {
            try {
                path.setFillColor(ColorParser.parse(fillColor));
            } catch (ResourceException e) {
                // Voulu : couleur/dimension invalide dans le vector — ignorée pour ne pas
                // casser le parsing du drawable entier
                Log.w(TAG, "Valeur invalide ignorée : " + e.getMessage());
            }
        }
        String strokeColor = getAttribute(parser, "android", "strokeColor");
        if (strokeColor != null) {
            try {
                path.setStrokeColor(ColorParser.parse(strokeColor));
            } catch (ResourceException e) {
                // Voulu : couleur/dimension invalide dans le vector — ignorée pour ne pas
                // casser le parsing du drawable entier
                Log.w(TAG, "Valeur invalide ignorée : " + e.getMessage());
            }
        }
        path.setStrokeWidth(parseDimension(getAttribute(parser, "android", "strokeWidth")));
        path.setFillAlpha(parseFloat(getAttribute(parser, "android", "fillAlpha"), 1f));
        path.setStrokeAlpha(parseFloat(getAttribute(parser, "android", "strokeAlpha"), 1f));

        String cap = getAttribute(parser, "android", "strokeLineCap");
        if (cap != null) {
            path.setStrokeLineCap(cap);
        }
        String join = getAttribute(parser, "android", "strokeLineJoin");
        if (join != null) {
            path.setStrokeLineJoin(join);
        }
        path.setStrokeMiterLimit(parseFloat(getAttribute(parser, "android", "strokeMiterLimit"), 4f));

        String fillType = getAttribute(parser, "android", "fillType");
        if (fillType != null) {
            path.setFillType(fillType);
        }
    }

    /**
     * Parse une dimension en pixels.
     */
    private float parseDimension(String value) {
        if (value == null || value.isEmpty()) {
            return 0f;
        }
        if (dimensionConverter == null) {
            try {
                return Float.parseFloat(value.replaceAll("[^0-9.\\-]", ""));
            } catch (NumberFormatException e) {
                return 0f;
            }
        }
        try {
            return dimensionConverter.toPixels(value);
        } catch (ResourceException e) {
            return 0f;
        }
    }

    /**
     * Parse un float depuis une chaîne.
     *
     * @param value la chaîne
     * @return la valeur, ou 0 si invalide
     */
    private float parseFloat(String value) {
        return parseFloat(value, 0f);
    }

    /**
     * Parse un float avec valeur par défaut.
     */
    private float parseFloat(String value, float defaultValue) {
        if (value == null || value.isEmpty()) {
            return defaultValue;
        }
        try {
            return Float.parseFloat(value.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * Récupère un attribut par namespace et nom.
     */
    private String getAttribute(XmlPullParser parser, String ns, String name) {
        String nsUri;
        if ("android".equals(ns)) {
            nsUri = "http://schemas.android.com/apk/res/android";
        } else {
            nsUri = ns;
        }
        String value = parser.getAttributeValue(nsUri, name);
        if (value == null) {
            value = parser.getAttributeValue(null, name);
        }
        return value;
    }
}
