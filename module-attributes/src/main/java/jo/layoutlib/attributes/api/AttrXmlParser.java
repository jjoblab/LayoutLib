package jo.layoutlib.attributes.api;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.StringReader;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Parser XML pour attributs custom.
 *
 * @author jo@Dev
 * @since 1.0
 */
public class AttrXmlParser {

    public Map<String, String> parse(String xml) {
        Map<String, String> result = new LinkedHashMap<>();
        if (xml == null || xml.trim().isEmpty()) return result;
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
        } catch (Exception e) {
            // ignore
        }
        return result;
    }
}
