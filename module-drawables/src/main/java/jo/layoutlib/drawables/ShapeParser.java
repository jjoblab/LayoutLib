package jo.layoutlib.drawables;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.io.StringReader;

import jo.layoutlib.resources.ColorParser;
import jo.layoutlib.resources.DimensionConverter;
import jo.layoutlib.resources.ResourceException;

/**
 * Parser de drawables XML de type {@code <shape>}.
 *
 * <p>Transforme un XML comme celui-ci en {@link ShapeConfig} :</p>
 *
 * <pre>{@code
 * <shape xmlns:android="http://schemas.android.com/apk/res/android"
 *        android:shape="rectangle">
 *     <solid android:color="#FF6750A4"/>
 *     <gradient android:startColor="#FF0000" android:endColor="#00FF00"
 *               android:angle="90"/>
 *     <corners android:radius="8dp"/>
 *     <stroke android:width="2dp" android:color="#FFFFFFFF"
 *             android:dashWidth="4dp" android:dashGap="2dp"/>
 *     <padding android:left="4dp" android:top="4dp"
 *              android:right="4dp" android:bottom="4dp"/>
 * </shape>
 * }</pre>
 *
 * <p>Référence layoutlib original :
 * {@code com.android.layoutlib.bridge.impl.ResourceHelper.getDrawable()}.</p>
 *
 * @author jo@Dev
 * @since 1.0
 */
public class ShapeParser {

    /** Convertisseur de dimensions pour parser les dp/sp/px. */
    private final DimensionConverter dimensionConverter;

    /**
     * Construit un parser avec un convertisseur de dimensions.
     *
     * @param dimensionConverter le convertisseur (peut être {@code null} pour
     *                           ne pas convertir les dimensions)
     */
    public ShapeParser(DimensionConverter dimensionConverter) {
        this.dimensionConverter = dimensionConverter;
    }

    /**
     * Parse un XML de shape en ShapeConfig.
     *
     * @param xml le XML source
     * @return la configuration parsée
     * @throws DrawableException si le XML est invalide
     */
    public ShapeConfig parse(String xml) {
        if (xml == null || xml.trim().isEmpty()) {
            throw new DrawableException("XML de shape vide", "shape");
        }
        try {
            XmlPullParser parser = createParser(xml);
            return parseDocument(parser);
        } catch (XmlPullParserException | IOException e) {
            throw new DrawableException("Erreur de parsing shape : " + e.getMessage(), "shape", e);
        }
    }

    /**
     * Crée un XmlPullParser pour le XML donné.
     */
    private XmlPullParser createParser(String xml) throws XmlPullParserException {
        XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
        factory.setNamespaceAware(true);
        XmlPullParser parser = factory.newPullParser();
        parser.setInput(new StringReader(xml));
        return parser;
    }

    /**
     * Parcourt le document et remplit la ShapeConfig.
     */
    private ShapeConfig parseDocument(XmlPullParser parser)
            throws XmlPullParserException, IOException {
        ShapeConfig config = new ShapeConfig();
        int event = parser.getEventType();
        while (event != XmlPullParser.END_DOCUMENT) {
            if (event == XmlPullParser.START_TAG) {
                String tag = parser.getName();
                switch (tag) {
                    case "shape":
                        parseShapeAttributes(config, parser);
                        break;
                    case "solid":
                        parseSolid(config, parser);
                        break;
                    case "gradient":
                        parseGradient(config, parser);
                        break;
                    case "corners":
                        parseCorners(config, parser);
                        break;
                    case "stroke":
                        parseStroke(config, parser);
                        break;
                    case "padding":
                        parsePadding(config, parser);
                        break;
                    case "size":
                        parseSize(config, parser);
                        break;
                    default:
                        // Ignorer les tags inconnus
                        break;
                }
            }
            event = parser.next();
        }
        return config;
    }

    /**
     * Parse les attributs du tag racine {@code <shape>}.
     */
    private void parseShapeAttributes(ShapeConfig config, XmlPullParser parser) {
        String shapeType = getAttribute(parser, "android", "shape");
        if (shapeType != null) {
            config.setShapeType(DrawableAttributeParser.parseShapeType(shapeType));
        }
    }

    /**
     * Parse un élément {@code <solid>}.
     */
    private void parseSolid(ShapeConfig config, XmlPullParser parser) {
        String color = getAttribute(parser, "android", "color");
        if (color != null) {
            try {
                config.setSolidColor(ColorParser.parse(color));
            } catch (ResourceException e) {
                throw new DrawableException("Couleur solid invalide : " + color, "shape", e);
            }
        }
    }

    /**
     * Parse un élément {@code <gradient>}.
     */
    private void parseGradient(ShapeConfig config, XmlPullParser parser) {
        String startColor = getAttribute(parser, "android", "startColor");
        String centerColor = getAttribute(parser, "android", "centerColor");
        String endColor = getAttribute(parser, "android", "endColor");
        String angle = getAttribute(parser, "android", "angle");
        String type = getAttribute(parser, "android", "type");
        String radius = getAttribute(parser, "android", "gradientRadius");

        try {
            if (startColor != null) {
                config.setGradientStartColor(ColorParser.parse(startColor));
            }
            if (centerColor != null) {
                config.setGradientCenterColor(ColorParser.parse(centerColor));
            }
            if (endColor != null) {
                config.setGradientEndColor(ColorParser.parse(endColor));
            }
        } catch (ResourceException e) {
            throw new DrawableException("Couleur de gradient invalide", "shape", e);
        }
        if (angle != null) {
            try {
                config.setGradientAngle(Integer.parseInt(angle));
            } catch (NumberFormatException ignored) {
            }
        }
        if (type != null) {
            config.setGradientType(DrawableAttributeParser.parseGradientType(type));
        }
        if (radius != null) {
            config.setGradientRadius(parseDimension(radius));
        }
    }

    /**
     * Parse un élément {@code <corners>}.
     */
    private void parseCorners(ShapeConfig config, XmlPullParser parser) {
        config.setCornerRadius(parseDimension(getAttribute(parser, "android", "radius")));
        config.setTopLeftRadius(parseDimension(getAttribute(parser, "android", "topLeftRadius")));
        config.setTopRightRadius(parseDimension(getAttribute(parser, "android", "topRightRadius")));
        config.setBottomLeftRadius(parseDimension(getAttribute(parser, "android", "bottomLeftRadius")));
        config.setBottomRightRadius(parseDimension(getAttribute(parser, "android", "bottomRightRadius")));
    }

    /**
     * Parse un élément {@code <stroke>}.
     */
    private void parseStroke(ShapeConfig config, XmlPullParser parser) {
        String width = getAttribute(parser, "android", "width");
        String color = getAttribute(parser, "android", "color");
        String dashWidth = getAttribute(parser, "android", "dashWidth");
        String dashGap = getAttribute(parser, "android", "dashGap");

        if (width != null) {
            config.setStrokeWidth(parseDimension(width));
        }
        if (color != null) {
            try {
                config.setStrokeColor(ColorParser.parse(color));
            } catch (ResourceException e) {
                throw new DrawableException("Couleur de stroke invalide : " + color, "shape", e);
            }
        }
        if (dashWidth != null) {
            config.setStrokeDashWidth(parseDimension(dashWidth));
        }
        if (dashGap != null) {
            config.setStrokeDashGap(parseDimension(dashGap));
        }
    }

    /**
     * Parse un élément {@code <padding>}.
     */
    private void parsePadding(ShapeConfig config, XmlPullParser parser) {
        config.setPaddingLeft(parseDimension(getAttribute(parser, "android", "left")));
        config.setPaddingTop(parseDimension(getAttribute(parser, "android", "top")));
        config.setPaddingRight(parseDimension(getAttribute(parser, "android", "right")));
        config.setPaddingBottom(parseDimension(getAttribute(parser, "android", "bottom")));
    }

    /**
     * Parse un élément {@code <size>}.
     *
     * <p>Note : la taille n'est pas utilisée dans la ShapeConfig car elle est
     * déterminée par le conteneur parent. Cette méthode existe pour la
     * complétude du parser.</p>
     */
    private void parseSize(ShapeConfig config, XmlPullParser parser) {
        // Taille non stockée — gérée par le conteneur parent
    }

    /**
     * Parse une dimension en utilisant le convertisseur configuré.
     *
     * @param value la valeur (ex. {@code "16dp"})
     * @return la valeur en pixels, ou 0 si {@code null}
     */
    private float parseDimension(String value) {
        if (value == null || value.isEmpty()) {
            return 0f;
        }
        if (dimensionConverter == null) {
            // Sans convertisseur, on tente de parser comme float brut
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
     * Récupère la valeur d'un attribut par namespace et nom.
     */
    private String getAttribute(XmlPullParser parser, String ns, String name) {
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
            value = parser.getAttributeValue(null, name);
        }
        return value;
    }
}
