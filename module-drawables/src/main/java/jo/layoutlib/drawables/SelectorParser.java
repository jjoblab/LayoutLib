package jo.layoutlib.drawables;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.io.StringReader;

/**
 * Parser de drawables XML de type {@code <selector>}.
 *
 * <p>Transforme un XML StateListDrawable en {@link SelectorConfig} contenant
 * la liste des items et leurs états.</p>
 *
 * <p>Référence layoutlib original :
 * {@code com.android.layoutlib.bridge.impl.ResourceHelper.getDrawable()}.</p>
 *
 * @author jo@Dev
 * @since 1.0
 */
public class SelectorParser {

    /**
     * Parse un XML de selector en SelectorConfig.
     *
     * @param xml le XML source
     * @return la configuration parsée
     * @throws DrawableException si le XML est invalide
     */
    public SelectorConfig parse(String xml) {
        if (xml == null || xml.trim().isEmpty()) {
            throw new DrawableException("XML de selector vide", "selector");
        }
        try {
            XmlPullParser parser = createParser(xml);
            return parseDocument(parser);
        } catch (XmlPullParserException | IOException e) {
            throw new DrawableException(
                    "Erreur de parsing selector : " + e.getMessage(), "selector", e);
        }
    }

    /**
     * Crée un XmlPullParser.
     */
    private XmlPullParser createParser(String xml) throws XmlPullParserException {
        XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
        factory.setNamespaceAware(true);
        XmlPullParser parser = factory.newPullParser();
        parser.setInput(new StringReader(xml));
        return parser;
    }

    /**
     * Parcourt le document et remplit la SelectorConfig.
     */
    private SelectorConfig parseDocument(XmlPullParser parser)
            throws XmlPullParserException, IOException {
        SelectorConfig config = new SelectorConfig();
        int event = parser.getEventType();
        while (event != XmlPullParser.END_DOCUMENT) {
            if (event == XmlPullParser.START_TAG) {
                String tag = parser.getName();
                if ("item".equals(tag)) {
                    config.addItem(parseItem(parser));
                }
            }
            event = parser.next();
        }
        return config;
    }

    /**
     * Parse un élément {@code <item>}.
     */
    private SelectorConfig.SelectorItem parseItem(XmlPullParser parser) {
        SelectorConfig.SelectorItem item = new SelectorConfig.SelectorItem();
        item.setDrawableRef(getAttribute(parser, "android", "drawable"));
        item.setStatePressed(parseBooleanAttr(getAttribute(parser, "android", "state_pressed")));
        item.setStateEnabled(parseBooleanAttr(getAttribute(parser, "android", "state_enabled")));
        item.setStateFocused(parseBooleanAttr(getAttribute(parser, "android", "state_focused")));
        item.setStateChecked(parseBooleanAttr(getAttribute(parser, "android", "state_checked")));
        item.setStateSelected(parseBooleanAttr(getAttribute(parser, "android", "state_selected")));
        item.setStateWindowFocused(
                parseBooleanAttr(getAttribute(parser, "android", "state_window_focused")));
        item.setStateCheckable(
                parseBooleanAttr(getAttribute(parser, "android", "state_checkable")));
        return item;
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
