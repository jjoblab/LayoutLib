package jo.layoutlib.drawables.parsers;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.io.StringReader;

import jo.layoutlib.drawables.DrawableException;

/**
 * Parser pour <clip>, inspiré de l AOSP.
 *
 * @author jo@Dev
 * @since 1.0
 */
public class ClipDrawableParser {

    public static class Config {
        public String drawableRef;
        public int orientation = 0;
        public float gravity = 0;
        public float level = 0;
        public float fromDegrees = 0;
        public float toDegrees = 360;
        public float scaleWidth = 1.0f;
        public float scaleHeight = 1.0f;
    }

    public Config parse(String xml) {
        if (xml == null || xml.trim().isEmpty()) {
            throw new DrawableException("XML vide", "clip");
        }
        try {
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            factory.setNamespaceAware(true);
            XmlPullParser parser = factory.newPullParser();
            parser.setInput(new StringReader(xml));
            Config config = new Config();
            int event = parser.getEventType();
            while (event != XmlPullParser.END_DOCUMENT) {
                if (event == XmlPullParser.START_TAG) {
                    String tag = parser.getName();
                    if ("clip".equals(tag)) {
                        config.drawableRef = getAttr(parser, "android", "drawable");
                    }
                }
                event = parser.next();
            }
            return config;
        } catch (XmlPullParserException | IOException e) {
            throw new DrawableException("Erreur parsing : " + e.getMessage(),
                    "clip", e);
        }
    }

    private static String getAttr(XmlPullParser parser, String ns, String name) {
        String nsUri = "http://schemas.android.com/apk/res/android";
        String v = parser.getAttributeValue(nsUri, name);
        return v != null ? v : parser.getAttributeValue(null, name);
    }
}
