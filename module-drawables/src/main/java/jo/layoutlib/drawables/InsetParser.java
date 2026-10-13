package jo.layoutlib.drawables;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.io.StringReader;

import jo.layoutlib.resources.DimensionConverter;
import jo.layoutlib.resources.ResourceException;

/**
 * Parser de drawables XML de type {@code <inset>}.
 *
 * <p>Transforme un XML InsetDrawable en {@link InsetConfig} : référence
 * {@code android:drawable} (ou enfant inline capturé) et les quatre insets
 * ({@code android:insetLeft/insetTop/insetRight/insetBottom}, dimensions
 * converties en pixels).</p>
 *
 * <p>Référence layoutlib original :
 * {@code com.android.layoutlib.bridge.impl.ResourceHelper.getDrawable()}.</p>
 *
 * @author jo@Dev
 * @since 3.0
 */
public class InsetParser {

    /** Convertisseur de dimensions pour parser les dp/sp/px. */
    private final DimensionConverter dimensionConverter;

    /**
     * Construit un parser avec un convertisseur de dimensions.
     *
     * @param dimensionConverter le convertisseur (peut être {@code null} pour
     *                           ne pas convertir les dimensions)
     */
    public InsetParser(DimensionConverter dimensionConverter) {
        this.dimensionConverter = dimensionConverter;
    }

    /**
     * Parse un XML d'inset en InsetConfig.
     *
     * @param xml le XML source
     * @return la configuration parsée
     * @throws DrawableException si le XML est invalide
     */
    public InsetConfig parse(String xml) {
        if (xml == null || xml.trim().isEmpty()) {
            throw new DrawableException("XML d'inset vide", "inset");
        }
        try {
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            factory.setNamespaceAware(true);
            XmlPullParser parser = factory.newPullParser();
            parser.setInput(new StringReader(xml));
            return parseDocument(parser);
        } catch (XmlPullParserException | IOException e) {
            throw new DrawableException(
                    "Erreur de parsing inset : " + e.getMessage(), "inset", e);
        }
    }

    /**
     * Parcourt le document et remplit la InsetConfig.
     *
     * <p>Le premier élément {@code <inset>} rencontré est la racine : ses
     * attributs sont lus, puis l'événement significatif suivant est soit son
     * {@code END_TAG} (inset auto-fermant, drawable par référence), soit le
     * {@code START_TAG} d'un drawable enfant inline (capturé).</p>
     */
    private InsetConfig parseDocument(XmlPullParser parser)
            throws XmlPullParserException, IOException {
        InsetConfig config = new InsetConfig();
        boolean rootSeen = false;
        int event = parser.getEventType();
        while (event != XmlPullParser.END_DOCUMENT) {
            if (event == XmlPullParser.START_TAG && !rootSeen
                    && "inset".equals(parser.getName())) {
                rootSeen = true;
                config.setDrawableRef(getAttribute(parser, "android", "drawable"));
                config.setInsetLeft(parseDimension(
                        getAttribute(parser, "android", "insetLeft")));
                config.setInsetTop(parseDimension(
                        getAttribute(parser, "android", "insetTop")));
                config.setInsetRight(parseDimension(
                        getAttribute(parser, "android", "insetRight")));
                config.setInsetBottom(parseDimension(
                        getAttribute(parser, "android", "insetBottom")));

                // Enfant inline éventuel (shape, selector, vector, color…)
                event = parser.next();
                while (event == XmlPullParser.TEXT && parser.getText() != null
                        && parser.getText().trim().isEmpty()) {
                    event = parser.next();
                }
                if (event == XmlPullParser.START_TAG) {
                    config.setNestedXml(DrawableXmlFragments.captureElement(parser));
                }
                continue;
            }
            event = parser.next();
        }
        return config;
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
