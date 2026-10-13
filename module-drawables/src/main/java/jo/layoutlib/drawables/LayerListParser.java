package jo.layoutlib.drawables;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.io.StringReader;

import jo.layoutlib.resources.DimensionConverter;
import jo.layoutlib.resources.ResourceException;

/**
 * Parser de drawables XML de type {@code <layer-list>}.
 *
 * <p>Transforme un XML LayerDrawable en {@link LayerListConfig} contenant la
 * pile ordonnée des calques. Chaque {@code <item>} est lu avec ses insets
 * ({@code android:left/top/right/bottom}, dimensions converties en pixels) et
 * sa source : référence {@code android:drawable} ou XML imbriqué capturé via
 * {@link DrawableXmlFragments}.</p>
 *
 * <p>Référence layoutlib original :
 * {@code com.android.layoutlib.bridge.impl.ResourceHelper.getDrawable()}.</p>
 *
 * @author jo@Dev
 * @since 3.0
 */
public class LayerListParser {

    /** Convertisseur de dimensions pour parser les dp/sp/px. */
    private final DimensionConverter dimensionConverter;

    /**
     * Construit un parser avec un convertisseur de dimensions.
     *
     * @param dimensionConverter le convertisseur (peut être {@code null} pour
     *                           ne pas convertir les dimensions)
     */
    public LayerListParser(DimensionConverter dimensionConverter) {
        this.dimensionConverter = dimensionConverter;
    }

    /**
     * Parse un XML de layer-list en LayerListConfig.
     *
     * @param xml le XML source
     * @return la configuration parsée (toujours non-null, éventuellement vide)
     * @throws DrawableException si le XML est invalide
     */
    public LayerListConfig parse(String xml) {
        if (xml == null || xml.trim().isEmpty()) {
            throw new DrawableException("XML de layer-list vide", "layer-list");
        }
        try {
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            factory.setNamespaceAware(true);
            XmlPullParser parser = factory.newPullParser();
            parser.setInput(new StringReader(xml));
            return parseDocument(parser);
        } catch (XmlPullParserException | IOException e) {
            throw new DrawableException(
                    "Erreur de parsing layer-list : " + e.getMessage(), "layer-list", e);
        }
    }

    /**
     * Parcourt le document et remplit la LayerListConfig.
     */
    private LayerListConfig parseDocument(XmlPullParser parser)
            throws XmlPullParserException, IOException {
        LayerListConfig config = new LayerListConfig();
        int event = parser.getEventType();
        while (event != XmlPullParser.END_DOCUMENT) {
            if (event == XmlPullParser.START_TAG && "item".equals(parser.getName())) {
                config.addItem(parseItem(parser));
            }
            event = parser.next();
        }
        return config;
    }

    /**
     * Parse un élément {@code <item>} et avance le parser jusqu'à son
     * {@code END_TAG}.
     */
    private LayerListConfig.LayerItem parseItem(XmlPullParser parser)
            throws XmlPullParserException, IOException {
        LayerListConfig.LayerItem item = new LayerListConfig.LayerItem();
        item.setDrawableRef(getAttribute(parser, "android", "drawable"));
        item.setId(getAttribute(parser, "android", "id"));
        item.setInsetLeft(parseDimension(getAttribute(parser, "android", "left")));
        item.setInsetTop(parseDimension(getAttribute(parser, "android", "top")));
        item.setInsetRight(parseDimension(getAttribute(parser, "android", "right")));
        item.setInsetBottom(parseDimension(getAttribute(parser, "android", "bottom")));

        // Enfant inline (shape, selector, vector, color, ripple, inset…) :
        // le prochain événement significatif est soit le END_TAG de l'item
        // (item auto-fermant), soit le START_TAG du drawable imbriqué.
        int event = parser.next();
        while (event == XmlPullParser.TEXT && parser.getText() != null
                && parser.getText().trim().isEmpty()) {
            event = parser.next();
        }
        if (event == XmlPullParser.START_TAG) {
            item.setNestedXml(DrawableXmlFragments.captureElement(parser));
        }
        // Le parser est maintenant sur le END_TAG de l'item (ou de l'enfant,
        // l'END_TAG de l'item sera consommé par la boucle principale).
        return item;
    }

    /**
     * Parse une dimension en utilisant le convertisseur configuré.
     *
     * @param value la valeur (ex. {@code "16dp"}), peut être {@code null}
     * @return la dimension en pixels, ou {@code null} si absente
     */
    private Float parseDimension(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        if (dimensionConverter == null) {
            return parseRawFloat(value);
        }
        try {
            return dimensionConverter.toPixels(value);
        } catch (ResourceException e) {
            // Dimension non reconnue — repli sur la valeur numérique brute
            return parseRawFloat(value);
        }
    }

    /**
     * Repli sans convertisseur : extrait la valeur numérique telle quelle.
     */
    private Float parseRawFloat(String value) {
        try {
            String cleaned = value.replaceAll("[^0-9.\\-]", "").trim();
            return cleaned.isEmpty() ? null : Float.valueOf(cleaned);
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
