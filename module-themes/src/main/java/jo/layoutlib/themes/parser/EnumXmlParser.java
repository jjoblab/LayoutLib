package jo.layoutlib.themes.parser;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.StringReader;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Parser XML pour Enum.
 * Utilise XmlPullParser natif Android.
 *
 * @author jo@Dev
 * @since 1.0
 */
public class EnumXmlParser {

    /**
     * Parse un XML et retourne les attributs extraits.
     *
     * @param xml le XML source
     * @return map nom -> valeur
     */
    public Map<String, String> parse(String xml) {
        Map<String, String> result = new LinkedHashMap<>();
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
            // Retourne ce qu on a pu extraire
        }
        return result;
    }
}
