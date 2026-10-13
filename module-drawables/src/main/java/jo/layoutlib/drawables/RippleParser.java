package jo.layoutlib.drawables;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.io.StringReader;

import jo.layoutlib.resources.ColorParser;
import jo.layoutlib.resources.ResourceException;

/**
 * Parser de drawables XML de type {@code <ripple>}.
 *
 * <p>Transforme un XML RippleDrawable en {@link RippleConfig} : couleur du
 * ripple (attribut {@code android:color}), contenu ({@code <item>} sans
 * {@code android:id/mask}) et masque ({@code <item android:id="@android:id/mask">}).
 * Les enfants inline sont capturés via {@link DrawableXmlFragments}.</p>
 *
 * <p>Référence layoutlib original :
 * {@code com.android.layoutlib.bridge.impl.ResourceHelper.getDrawable()}.</p>
 *
 * @author jo@Dev
 * @since 3.0
 */
public class RippleParser {

    /**
     * Parse un XML de ripple en RippleConfig.
     *
     * @param xml le XML source
     * @return la configuration parsée (couleur {@code null} si absente)
     * @throws DrawableException si le XML est invalide
     */
    public RippleConfig parse(String xml) {
        if (xml == null || xml.trim().isEmpty()) {
            throw new DrawableException("XML de ripple vide", "ripple");
        }
        try {
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            factory.setNamespaceAware(true);
            XmlPullParser parser = factory.newPullParser();
            parser.setInput(new StringReader(xml));
            return parseDocument(parser);
        } catch (XmlPullParserException | IOException e) {
            throw new DrawableException(
                    "Erreur de parsing ripple : " + e.getMessage(), "ripple", e);
        }
    }

    /**
     * Parcourt le document et remplit la RippleConfig.
     */
    private RippleConfig parseDocument(XmlPullParser parser)
            throws XmlPullParserException, IOException {
        RippleConfig config = new RippleConfig();
        int event = parser.getEventType();
        while (event != XmlPullParser.END_DOCUMENT) {
            if (event == XmlPullParser.START_TAG) {
                String tag = parser.getName();
                if ("ripple".equals(tag)) {
                    config.setColor(parseColor(getAttribute(parser, "android", "color")));
                } else if ("item".equals(tag)) {
                    RippleConfig.RippleItem item = parseItem(parser);
                    if (item.isMask()) {
                        config.setMask(item);
                    } else {
                        // Premier item non-masque = contenu ; les suivants
                        // (cas pathological) sont ignorés
                        if (config.getContent() == null) {
                            config.setContent(item);
                        }
                    }
                }
            }
            event = parser.next();
        }
        return config;
    }

    /**
     * Parse un élément {@code <item>} et avance le parser jusqu'à son
     * {@code END_TAG}.
     */
    private RippleConfig.RippleItem parseItem(XmlPullParser parser)
            throws XmlPullParserException, IOException {
        RippleConfig.RippleItem item = new RippleConfig.RippleItem();
        item.setDrawableRef(getAttribute(parser, "android", "drawable"));
        item.setId(getAttribute(parser, "android", "id"));

        int event = parser.next();
        while (event == XmlPullParser.TEXT && parser.getText() != null
                && parser.getText().trim().isEmpty()) {
            event = parser.next();
        }
        if (event == XmlPullParser.START_TAG) {
            item.setNestedXml(DrawableXmlFragments.captureElement(parser));
        }
        return item;
    }

    /**
     * Parse une couleur ARGB, tolérante aux valeurs inconnues.
     *
     * @param value la valeur (ex. {@code "#FF4081"}), peut être {@code null}
     * @return la couleur, ou {@code null} si absente ou non résolvable
     */
    private Integer parseColor(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        try {
            return ColorParser.parse(value);
        } catch (ResourceException e) {
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
