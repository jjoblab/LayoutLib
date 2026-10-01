package jo.layoutlib.inflater.bridge.android;

import org.xmlpull.v1.XmlPullParser;

/**
 * Wrapper de XmlPullParser avec résolution de références bridge, inspiré de
 * com.android.layoutlib.bridge.android.BridgeXmlBlockParser de l AOSP.
 *
 * @author jo@Dev
 * @since 1.0
 */
public class BridgeXmlBlockParser {

    private final XmlPullParser parser;
    private final BridgeContext context;
    private boolean resolved = false;

    public BridgeXmlBlockParser(XmlPullParser parser, BridgeContext context) {
        this.parser = parser;
        this.context = context;
    }

    public XmlPullParser getParser() {
        return parser;
    }

    public BridgeContext getContext() {
        return context;
    }

    /**
     * Résout une valeur d attribut via le ResourceResolver si nécessaire.
     *
     * @param namespace le namespace
     * @param name le nom de l attribut
     * @return la valeur résolue, ou null
     */
    public String resolveAttributeValue(String namespace, String name) {
        String value = parser.getAttributeValue(namespace, name);
        if (value == null) {
            return null;
        }
        // Si la valeur est une référence @type/name et qu on a un resolver
        if (value.startsWith("@") && context != null && context.getResourceResolver() != null) {
            // La résolution réelle est faite par le caller
            return value;
        }
        return value;
    }

    public boolean isResolved() {
        return resolved;
    }

    public void markResolved() {
        this.resolved = true;
    }
}
