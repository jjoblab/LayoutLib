package jo.layoutlib.drawables.parsers;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.io.StringReader;

import jo.layoutlib.drawables.DrawableException;

/**
 * Parser pour <ripple>.
 *
 * @author jo@Dev
 * @since 1.0
 */
public class RippleDrawableParser {

    public static class RippleConfig {
        public String color;
        public String backgroundRef;
        public int radius;
    }

    public RippleConfig parse(String xml) {
        if (xml == null || xml.trim().isEmpty()) {
            throw new DrawableException("XML ripple vide", "ripple");
        }
        try {
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            factory.setNamespaceAware(true);
            XmlPullParser parser = factory.newPullParser();
            parser.setInput(new StringReader(xml));
            return parseDocument(parser);
        } catch (XmlPullParserException | IOException e) {
            throw new DrawableException("Erreur parsing ripple : " + e.getMessage(), "ripple", e);
        }
    }

    private RippleConfig parseDocument(XmlPullParser parser) throws XmlPullParserException, IOException {
        RippleConfig config = new RippleConfig();
        int event = parser.getEventType();
        while (event != XmlPullParser.END_DOCUMENT) {
            if (event == XmlPullParser.START_TAG) {
                String tag = parser.getName();
                if ("ripple".equals(tag)) {
                    config.color = getAttr(parser, "android", "color");
                    String radius = getAttr(parser, "android", "radius");
                    if (radius != null) {
                        try {
                            config.radius = Integer.parseInt(radius.replaceAll("[^0-9-]", ""));
                        } catch (NumberFormatException ignored) {
                        }
                    }
                } else if ("item".equals(tag)) {
                    String drawable = getAttr(parser, "android", "drawable");
                    if (drawable != null && config.backgroundRef == null) {
                        config.backgroundRef = drawable;
                    }
                    // Skip item content
                    int depth = 1;
                    while (depth > 0) {
                        int e = parser.next();
                        if (e == XmlPullParser.START_TAG) depth++;
                        else if (e == XmlPullParser.END_TAG) depth--;
                        else if (e == XmlPullParser.END_DOCUMENT) break;
                    }
                }
            }
            event = parser.next();
        }
        return config;
    }

    private static String getAttr(XmlPullParser parser, String ns, String name) {
        String nsUri = "http://schemas.android.com/apk/res/android";
        String v = parser.getAttributeValue(nsUri, name);
        return v != null ? v : parser.getAttributeValue(null, name);
    }
}
