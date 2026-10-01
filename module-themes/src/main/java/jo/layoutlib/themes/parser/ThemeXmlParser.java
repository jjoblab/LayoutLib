package jo.layoutlib.themes.parser;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

import jo.layoutlib.themes.StyleDefinition;
import jo.layoutlib.themes.ThemeException;

/**
 * Parser de themes.xml, inspiré de l AOSP.
 *
 * @author jo@Dev
 * @since 1.0
 */
public class ThemeXmlParser {

    /**
     * Parse un XML de thèmes.
     *
     * @param xml le XML
     * @return map nom -> StyleDefinition
     */
    public Map<String, StyleDefinition> parse(String xml) {
        Map<String, StyleDefinition> result = new HashMap<>();
        if (xml == null || xml.trim().isEmpty()) {
            return result;
        }
        try {
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            factory.setNamespaceAware(true);
            XmlPullParser parser = factory.newPullParser();
            parser.setInput(new StringReader(xml));
            parseDocument(parser, result);
            return result;
        } catch (XmlPullParserException | IOException e) {
            throw new ThemeException("Erreur parsing themes : " + e.getMessage(), e);
        }
    }

    private void parseDocument(XmlPullParser parser, Map<String, StyleDefinition> result)
            throws XmlPullParserException, IOException {
        int event = parser.getEventType();
        while (event != XmlPullParser.END_DOCUMENT) {
            if (event == XmlPullParser.START_TAG && "style".equals(parser.getName())) {
                StyleDefinition style = parseStyle(parser);
                if (style != null) {
                    result.put(style.getName(), style);
                }
            }
            event = parser.next();
        }
    }

    private StyleDefinition parseStyle(XmlPullParser parser)
            throws XmlPullParserException, IOException {
        String name = parser.getAttributeValue(null, "name");
        if (name == null || name.isEmpty()) return null;
        String parent = parser.getAttributeValue(null, "parent");
        if (parent == null || parent.isEmpty()) {
            parent = new StyleInheritanceResolver().computeImplicitParent(name);
        } else if ("@null".equals(parent)) {
            parent = null;
        }
        StyleDefinition style = new StyleDefinition(name, parent, true);
        int depth = 1;
        while (depth > 0) {
            int event = parser.next();
            if (event == XmlPullParser.START_TAG) {
                if ("item".equals(parser.getName())) {
                    parseItem(style, parser);
                } else {
                    depth++;
                }
            } else if (event == XmlPullParser.END_TAG) {
                depth--;
            } else if (event == XmlPullParser.END_DOCUMENT) {
                break;
            }
        }
        return style;
    }

    private void parseItem(StyleDefinition style, XmlPullParser parser)
            throws XmlPullParserException, IOException {
        String name = parser.getAttributeValue(
                "http://schemas.android.com/apk/res/android", "name");
        if (name == null) {
            name = parser.getAttributeValue(null, "name");
        }
        if (name == null || name.isEmpty()) return;
        StringBuilder sb = new StringBuilder();
        int depth = 1;
        while (depth > 0) {
            int event = parser.next();
            if (event == XmlPullParser.START_TAG) depth++;
            else if (event == XmlPullParser.END_TAG) depth--;
            else if (event == XmlPullParser.TEXT) sb.append(parser.getText());
            else if (event == XmlPullParser.END_DOCUMENT) break;
        }
        style.setAttribute(name, sb.toString().trim());
    }
}
