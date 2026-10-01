package jo.layoutlib.drawables.parsers;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.StringReader;
import java.io.IOException;

import jo.layoutlib.drawables.DrawableException;

/**
 * Parser pour TransitionDrawable, utilise XmlPullParser natif.
 *
 * @author jo@Dev
 * @since 1.0
 */
public class TransitionDrawableParser {

    /**
     * Parse le XML et retourne la configuration extraite.
     *
     * @param xml le XML source
     * @return un tableau d attributs [name, value] alternés
     */
    public java.util.Map<String, String> parse(String xml) {
        java.util.Map<String, String> result = new java.util.LinkedHashMap<>();
        if (xml == null || xml.trim().isEmpty()) {
            return result;
        }
        try {
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            factory.setNamespaceAware(true);
            XmlPullParser parser = factory.newPullParser();
            parser.setInput(new StringReader(xml));
            int event = parser.getEventType();
            while (event != XmlPullParser.END_DOCUMENT) {
                if (event == XmlPullParser.START_TAG) {
                    int count = parser.getAttributeCount();
                    for (int i = 0; i < count; i++) {
                        String name = parser.getAttributeName(i);
                        String value = parser.getAttributeValue(i);
                        if (name != null && value != null) {
                            result.put(name, value);
                        }
                    }
                }
                event = parser.next();
            }
        } catch (XmlPullParserException | IOException e) {
            throw new DrawableException("Erreur parsing: " + e.getMessage(),
                    "transitiondrawable", e);
        }
        return result;
    }
}
