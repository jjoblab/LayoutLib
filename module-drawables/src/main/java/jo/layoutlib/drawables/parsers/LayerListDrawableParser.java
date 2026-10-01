package jo.layoutlib.drawables.parsers;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import jo.layoutlib.drawables.DrawableException;

/**
 * Parser pour <layer-list>.
 *
 * <p>Un layer-list est une liste de layers, chacun avec un drawable et
 * des insets (left, top, right, bottom).</p>
 *
 * @author jo@Dev
 * @since 1.0
 */
public class LayerListDrawableParser {

    /**
     * Représente un layer.
     */
    public static class Layer {
        public String drawableRef;
        public int left;
        public int top;
        public int right;
        public int bottom;
        public int gravity;
    }

    private final List<Layer> layers = new ArrayList<>();

    public List<Layer> parse(String xml) {
        if (xml == null || xml.trim().isEmpty()) {
            throw new DrawableException("XML layer-list vide", "layer-list");
        }
        try {
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            factory.setNamespaceAware(true);
            XmlPullParser parser = factory.newPullParser();
            parser.setInput(new StringReader(xml));
            parseDocument(parser);
            return new ArrayList<>(layers);
        } catch (XmlPullParserException | IOException e) {
            throw new DrawableException("Erreur parsing layer-list : " + e.getMessage(), "layer-list", e);
        }
    }

    private void parseDocument(XmlPullParser parser) throws XmlPullParserException, IOException {
        int event = parser.getEventType();
        while (event != XmlPullParser.END_DOCUMENT) {
            if (event == XmlPullParser.START_TAG) {
                if ("item".equals(parser.getName())) {
                    layers.add(parseItem(parser));
                }
            }
            event = parser.next();
        }
    }

    private Layer parseItem(XmlPullParser parser) throws XmlPullParserException, IOException {
        Layer layer = new Layer();
        layer.drawableRef = getAttr(parser, "android", "drawable");
        String left = getAttr(parser, "android", "left");
        String top = getAttr(parser, "android", "top");
        String right = getAttr(parser, "android", "right");
        String bottom = getAttr(parser, "android", "bottom");
        if (left != null) layer.left = parseInt(left);
        if (top != null) layer.top = parseInt(top);
        if (right != null) layer.right = parseInt(right);
        if (bottom != null) layer.bottom = parseInt(bottom);
        // Consomme le tag
        int depth = 1;
        while (depth > 0) {
            int event = parser.next();
            if (event == XmlPullParser.START_TAG) depth++;
            else if (event == XmlPullParser.END_TAG) depth--;
            else if (event == XmlPullParser.END_DOCUMENT) break;
        }
        return layer;
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
