package jo.layoutlib.drawables;

import android.view.Gravity;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.io.StringReader;

/**
 * Parser de drawables XML de type {@code <bitmap>}.
 *
 * <p>Transforme un XML BitmapDrawable en {@link BitmapConfig} : source
 * {@code android:src}, tuilage {@code tileMode/tileModeX/tileModeY}, gravité
 * (constantes {@link Gravity} combinables par {@code |}), drapeaux de qualité
 * et alpha/tint.</p>
 *
 * <p>Les modes de tuilage et le tint sont conservés en chaînes brutes :
 * leur conversion en constantes Android ({@code Shader.TileMode},
 * {@code PorterDuff.Mode}) relève de la couche de création du resolver.</p>
 *
 * <p>Référence layoutlib original :
 * {@code com.android.layoutlib.bridge.impl.ResourceHelper.getDrawable()}.</p>
 *
 * @author jo@Dev
 * @since 3.0
 */
public class BitmapParser {

    /**
     * Parse un XML de bitmap en BitmapConfig.
     *
     * @param xml le XML source
     * @return la configuration parsée
     * @throws DrawableException si le XML est invalide
     */
    public BitmapConfig parse(String xml) {
        if (xml == null || xml.trim().isEmpty()) {
            throw new DrawableException("XML de bitmap vide", "bitmap");
        }
        try {
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            factory.setNamespaceAware(true);
            XmlPullParser parser = factory.newPullParser();
            parser.setInput(new StringReader(xml));
            return parseDocument(parser);
        } catch (XmlPullParserException | IOException e) {
            throw new DrawableException(
                    "Erreur de parsing bitmap : " + e.getMessage(), "bitmap", e);
        }
    }

    /**
     * Parcourt le document et remplit la BitmapConfig.
     */
    private BitmapConfig parseDocument(XmlPullParser parser)
            throws XmlPullParserException, IOException {
        BitmapConfig config = new BitmapConfig();
        int event = parser.getEventType();
        while (event != XmlPullParser.END_DOCUMENT) {
            if (event == XmlPullParser.START_TAG && "bitmap".equals(parser.getName())) {
                config.setSrcRef(getAttribute(parser, "android", "src"));
                config.setTileMode(getAttribute(parser, "android", "tileMode"));
                config.setTileModeX(getAttribute(parser, "android", "tileModeX"));
                config.setTileModeY(getAttribute(parser, "android", "tileModeY"));
                config.setGravity(parseGravity(
                        getAttribute(parser, "android", "gravity")));
                config.setAntialias(parseBooleanAttr(
                        getAttribute(parser, "android", "antialias")));
                config.setDither(parseBooleanAttr(
                        getAttribute(parser, "android", "dither")));
                config.setFilter(parseBooleanAttr(
                        getAttribute(parser, "android", "filter")));
                config.setAlpha(parseFloatAttr(
                        getAttribute(parser, "android", "alpha")));
                config.setTint(getAttribute(parser, "android", "tint"));
                config.setTintMode(getAttribute(parser, "android", "tintMode"));
            }
            event = parser.next();
        }
        return config;
    }

    /**
     * Parse une gravité combinable en constantes {@link Gravity}.
     *
     * <p>Les valeurs acceptées sont les mêmes que le framework : séparées par
     * {@code |}, insensibles à la casse. Les constantes sont des
     * {@code static final int} du framework — réelles même en JVM de test
     * (inlining compile-time), ce qui rend cette méthode testable sans
     * device.</p>
     *
     * @param value la gravité (ex. {@code "center|fill_horizontal"}), peut être {@code null}
     * @return la gravité combinée, ou {@code null} si absente ou vide
     */
    public static Integer parseGravity(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        int result = 0;
        boolean any = false;
        for (String token : value.split("\\|")) {
            Integer flag = gravityFlag(token.trim());
            if (flag != null) {
                result |= flag;
                any = true;
            }
        }
        return any ? result : null;
    }

    /**
     * Convertit un jeton de gravité en constante {@link Gravity}.
     *
     * @param token le jeton (ex. {@code "center"})
     * @return la constante, ou {@code null} si inconnu
     */
    private static Integer gravityFlag(String token) {
        if (token.isEmpty()) {
            return null;
        }
        switch (token.toLowerCase()) {
            case "top":             return Gravity.TOP;
            case "bottom":          return Gravity.BOTTOM;
            case "left":            return Gravity.LEFT;
            case "right":           return Gravity.RIGHT;
            case "start":           return Gravity.START;
            case "end":             return Gravity.END;
            case "center_vertical": return Gravity.CENTER_VERTICAL;
            case "fill_vertical":   return Gravity.FILL_VERTICAL;
            case "center_horizontal": return Gravity.CENTER_HORIZONTAL;
            case "fill_horizontal": return Gravity.FILL_HORIZONTAL;
            case "center":          return Gravity.CENTER;
            case "fill":            return Gravity.FILL;
            case "clip_vertical":   return Gravity.CLIP_VERTICAL;
            case "clip_horizontal": return Gravity.CLIP_HORIZONTAL;
            default:                return null;
        }
    }

    /**
     * Parse un attribut booléen.
     *
     * @param value la valeur (ex. {@code "true"}, {@code "false"})
     * @return {@code Boolean.TRUE}, {@code Boolean.FALSE}, ou {@code null}
     */
    private Boolean parseBooleanAttr(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        return "true".equals(value);
    }

    /**
     * Parse un attribut flottant.
     *
     * @param value la valeur (ex. {@code "0.5"})
     * @return le flottant, ou {@code null} si absent ou invalide
     */
    private Float parseFloatAttr(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        try {
            return Float.valueOf(value.trim());
        } catch (NumberFormatException e) {
            return null;
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
