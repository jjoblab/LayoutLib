package jo.layoutlib.drawables.parsers;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.io.StringReader;

import jo.layoutlib.drawables.DrawableException;

/**
 * Parser pour <inset>.
 *
 * @author jo@Dev
 * @since 1.0
 */
public class InsetDrawableParser {

    public static class InsetConfig {
        public String drawableRef;
        public int insetLeft;
        public int insetTop;
        public int insetRight;
        public int insetBottom;
    }

    public InsetConfig parse(String xml) {
        if (xml == null || xml.trim().isEmpty()) {
            throw new DrawableException("XML inset vide", "inset");
        }
        try {
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            factory.setNamespaceAware(true);
            XmlPullParser parser = factory.newPullParser();
            parser.setInput(new StringReader(xml));
            InsetConfig config = new InsetConfig();
            int event = parser.getEventType();
            while (event != XmlPullParser.END_DOCUMENT) {
                if (event == XmlPullParser.START_TAG && "inset".equals(parser.getName())) {
                    config.drawableRef = getAttr(parser, "android", "drawable");
                    String left = getAttr(parser, "android", "insetLeft");
                    String top = getAttr(parser, "android", "insetTop");
                    String right = getAttr(parser, "android", "insetRight");
                    String bottom = getAttr(parser, "android", "insetBottom");
                    if (left != null) config.insetLeft = parseInt(left);
                    if (top != null) config.insetTop = parseInt(top);
                    if (right != null) config.insetRight = parseInt(right);
                    if (bottom != null) config.insetBottom = parseInt(bottom);
                }
                event = parser.next();
            }
            return config;
        } catch (XmlPullParserException | IOException e) {
            throw new DrawableException("Erreur parsing inset : " + e.getMessage(), "inset", e);
        }
    }

    private static String getAttr(XmlPullParser parser, String ns, String name) {
        String nsUri = "http://schemas.android.com/apk/res/android";
        String v = parser.getAttributeValue(nsUri, name);
        return v != null ? v : parser.getAttributeValue(null, name);
    }

    private static int parseInt(String s) {
        try {
            return Integer.parseInt(s.replaceAll("[^0-9-]", ""));
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
